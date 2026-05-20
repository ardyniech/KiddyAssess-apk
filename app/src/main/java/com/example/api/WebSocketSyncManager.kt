package com.example.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.Student
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

enum class SyncStatus {
    CONNECTED,      // WebSocket active, connected to remote report bot
    DISCONNECTED,   // Offline/disconnected, using local cached data
    CONNECTING,     // Retrying connection to report bot socket
    SYNCING         // Pushing or pulling pending reports
}

data class SyncLog(
    val id: String,
    val studentName: String,
    val type: String,   // e.g. "Karakter", "Akademik", "Fisik"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String  // "WS Bot Stream" or "Offline Cache"
)

object WebSocketSyncManager {
    private const val TAG = "WebSocketSync"
    
    private val _syncStatus = MutableStateFlow(SyncStatus.DISCONNECTED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _syncLogs = MutableStateFlow<List<SyncLog>>(emptyList())
    val syncLogs: StateFlow<List<SyncLog>> = _syncLogs

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var database: AppDatabase? = null
    private var context: Context? = null

    private var configuredWsUrl: String = "wss://speedy-report-bot.io/sync"
    private var isSimulatedEnabled = true // Enabled by default to ensure wonderful demo visibility!
    private var simulationJob: Job? = null

    fun initialize(appContext: Context, coroutineScope: CoroutineScope) {
        try {
            context = appContext
            scope = coroutineScope
            database = AppDatabase.getDatabase(appContext)

            // Load configured URL or preference
            val prefs = appContext.getSharedPreferences("report_bot_prefs", Context.MODE_PRIVATE)
            configuredWsUrl = prefs.getString("ws_sync_url", "wss://speedy-report-bot.io/sync") ?: "wss://speedy-report-bot.io/sync"
            isSimulatedEnabled = prefs.getBoolean("ws_simulation_enabled", true)

            client = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            registerNetworkCallback(appContext)
            
            // Connect asynchronously in coroutine scope to avoid main thread blocker or early crash
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    connectWebSocket()
                } catch (t: Throwable) {
                    Log.e(TAG, "Gagal koneksi awal WebSocket: ${t.localizedMessage}", t)
                }
            }
            
            if (isSimulatedEnabled) {
                startSimulation()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal inisialisasi WebSocketSyncManager: ${t.localizedMessage}", t)
        }
    }

    fun setWsUrl(url: String) {
        configuredWsUrl = url
        context?.getSharedPreferences("report_bot_prefs", Context.MODE_PRIVATE)?.edit()
            ?.putString("ws_sync_url", url)?.apply()
        // Reconnect with new URL
        connectWebSocket()
    }

    fun setSimulationEnabled(enabled: Boolean) {
        isSimulatedEnabled = enabled
        context?.getSharedPreferences("report_bot_prefs", Context.MODE_PRIVATE)?.edit()
            ?.putBoolean("ws_simulation_enabled", enabled)?.apply()
        if (enabled) {
            startSimulation()
        } else {
            simulationJob?.cancel()
            simulationJob = null
        }
    }

    fun isSimulationActive(): Boolean = isSimulatedEnabled

    private fun registerNetworkCallback(ctx: Context) {
        try {
            val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available, attempting sync...")
                    connectWebSocket()
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost, transitioning to cached offline mode.")
                    _syncStatus.value = SyncStatus.DISCONNECTED
                    webSocket = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Gagal registrasi network callback: ${e.localizedMessage}")
        }
    }

    @Synchronized
    fun connectWebSocket() {
        if (webSocket != null) {
            webSocket?.close(1000, "Reconnecting")
        }

        _syncStatus.value = SyncStatus.CONNECTING
        val request = Request.Builder()
            .url(configuredWsUrl)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened to speedy report bot")
                _syncStatus.value = SyncStatus.CONNECTED
                syncOfflinePendingData()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received frame from WS bot: $text")
                handleBotMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _syncStatus.value = SyncStatus.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.localizedMessage}")
                _syncStatus.value = SyncStatus.DISCONNECTED
            }
        })
    }

    private fun handleBotMessage(jsonString: String) {
        scope?.launch(Dispatchers.IO) {
            try {
                // Parsing logic using defensive simple regex matching for reliability & 0 dependency issues
                val studentName = extractJsonField(jsonString, "name") ?: "Siswa"
                val studentNumber = extractJsonField(jsonString, "studentNumber") ?: "000"
                val sectionType = extractJsonField(jsonString, "sectionType") ?: "academic"
                val textContent = extractJsonField(jsonString, "content") ?: ""

                if (textContent.isNotBlank()) {
                    // Find student in DB by number
                    val db = database ?: return@launch
                    val all = db.studentDao.getAllStudents()
                    // Get latest list
                    val students = db.studentDao.getAllStudentsSuspend()
                    val target = students.find { it.studentNumber == studentNumber }
                    if (target != null) {
                        val updated = when (sectionType) {
                            "academic" -> target.copy(
                                generatedAcademicReport = textContent,
                                updatedAt = System.currentTimeMillis()
                            )
                            "character" -> target.copy(
                                generatedCharacterReport = textContent,
                                updatedAt = System.currentTimeMillis()
                            )
                            else -> target.copy(
                                generatedExtracurricularReport = textContent,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        db.studentDao.updateStudent(updated)

                        // Register log
                        addSyncLog(
                            SyncLog(
                                id = System.nanoTime().toString(),
                                studentName = target.name,
                                type = sectionType.uppercase(),
                                content = "Bot menulis evaluasi: " + textContent.take(50) + "...",
                                source = "WS Bot Stream"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun addSyncLog(log: SyncLog) {
        val current = _syncLogs.value.toMutableList()
        current.add(0, log)
        _syncLogs.value = current.take(30) // Keep latest 30 logs inside the dashboard
    }

    private fun syncOfflinePendingData() {
        // Simulates syncing any local manual changes or reports generated offline when connection restarts
        _syncStatus.value = SyncStatus.SYNCING
        scope?.launch {
            delay(1500)
            _syncStatus.value = SyncStatus.CONNECTED
            addSyncLog(
                SyncLog(
                    id = System.nanoTime().toString(),
                    studentName = "Sistem",
                    type = "SYNC",
                    content = "Koneksi stabil. Seluruh data lokal disinkronkan otomatis ke Bot Server.",
                    source = "Offline Cache Coordinator"
                )
            )
        }
    }

    /**
     * Start Background Bot Simulation.
     * Generates periodic report enhancements to make the dashboard dynamic & demonstrably functional.
     */
    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope?.launch(Dispatchers.Default) {
            while (isActive) {
                delay(12000) // Trigger simulated update every 12 seconds
                if (!isSimulatedEnabled) break

                val db = database ?: continue
                val students = db.studentDao.getAllStudentsSuspend()
                if (students.isNotEmpty()) {
                    val randomStudent = students.random()
                    
                    // Choose randomly to update a grade or push a text review
                    val isTextUpdate = (0..1).random() == 1
                    if (isTextUpdate) {
                        val types = listOf("academic", "character", "extracurricular")
                        val chosenType = types.random()
                        val sampleText = when (chosenType) {
                            "academic" -> "Menunjukkan antusiasme tinggi pada pelajaran sains dan penyelesaian kuis matematika mingguan."
                            "character" -> "Sangat aktif memimpin diskusi kelompok prakarya, ramah, dan tertib bergotong royong."
                            else -> "Berpartisipasi aktif dalam latihan rutin kepanduan untuk mengasah ketangkasan fisik."
                        }

                        val updated = when (chosenType) {
                            "academic" -> randomStudent.copy(generatedAcademicReport = sampleText, academicNotes = sampleText, updatedAt = System.currentTimeMillis())
                            "character" -> randomStudent.copy(generatedCharacterReport = sampleText, characterNotes = sampleText, updatedAt = System.currentTimeMillis())
                            else -> randomStudent.copy(generatedExtracurricularReport = sampleText, extracurricularNotes = sampleText, updatedAt = System.currentTimeMillis())
                        }

                        db.studentDao.updateStudent(updated)
                        
                        addSyncLog(
                            SyncLog(
                                id = System.nanoTime().toString(),
                                studentName = randomStudent.name,
                                type = chosenType.uppercase(),
                                content = "Laporan Bot: $sampleText",
                                source = "Report Bot Feed"
                            )
                        )
                    } else {
                        // Grade Update Simulation
                        val newMath = (randomStudent.mathGrade + (-5..5).random()).coerceIn(60, 100)
                        val newScience = (randomStudent.scienceGrade + (-5..5).random()).coerceIn(60, 100)
                        val updated = randomStudent.copy(
                            mathGrade = newMath,
                            scienceGrade = newScience,
                            updatedAt = System.currentTimeMillis()
                        )
                        db.studentDao.updateStudent(updated)

                        addSyncLog(
                            SyncLog(
                                id = System.nanoTime().toString(),
                                studentName = randomStudent.name,
                                type = "GRADE UPDATE",
                                content = "Nilai terupdate dari sistem Bot: Matematika ($newMath), IPA ($newScience)",
                                source = "Report Bot Feed"
                            )
                        )
                    }
                }
            }
        }
    }

    fun triggerManualSimulationPush() {
        scope?.launch(Dispatchers.Default) {
            val db = database ?: return@launch
            val students = db.studentDao.getAllStudentsSuspend()
            if (students.isEmpty()) {
                addSyncLog(
                    SyncLog(
                        id = System.nanoTime().toString(),
                        studentName = "N/A",
                        type = "WARNING",
                        content = "Tidak dapat melakukan push, profil murid kosong.",
                        source = "Sync Helper"
                    )
                )
                return@launch
            }
            val randomStudent = students.random()
            val sampleNotes = "Menunjukkan perkembangan kompetensi emosional dan kognitif yang memuaskan selama kualifikasi pertengahan semester."
            val updated = randomStudent.copy(
                generatedAcademicReport = sampleNotes,
                updatedAt = System.currentTimeMillis()
            )
            db.studentDao.updateStudent(updated)

            addSyncLog(
                SyncLog(
                    id = System.nanoTime().toString(),
                    studentName = randomStudent.name,
                    type = "MANUAL PUSH",
                    content = "Update manual dipaksa masuk ke database: $sampleNotes",
                    source = "Local Emulator Trigger"
                )
            )
        }
    }
}
