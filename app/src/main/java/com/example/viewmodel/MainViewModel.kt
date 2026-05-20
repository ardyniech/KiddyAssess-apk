package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Student
import com.example.data.StudentRepository
import com.example.data.User
import com.example.api.*
import com.example.util.ReportPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: StudentRepository? = null
    private val _allStudents = MutableStateFlow<List<Student>>(emptyList())
    val allStudents: StateFlow<List<Student>> = _allStudents.asStateFlow()

    // Selected student for detailed editing or viewing
    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent

    // API Settings
    private val _customApiKeyState = MutableStateFlow("")
    val customApiKeyState: StateFlow<String> = _customApiKeyState

    // API Calling States
    private val _isGeneratingAcademic = MutableStateFlow(false)
    val isGeneratingAcademic: StateFlow<Boolean> = _isGeneratingAcademic

    private val _isGeneratingCharacter = MutableStateFlow(false)
    val isGeneratingCharacter: StateFlow<Boolean> = _isGeneratingCharacter

    private val _isGeneratingExtracurricular = MutableStateFlow(false)
    val isGeneratingExtracurricular: StateFlow<Boolean> = _isGeneratingExtracurricular

    // PDF States
    private val _pdfGenerationState = MutableStateFlow<File?>(null)
    val pdfGenerationState: StateFlow<File?> = _pdfGenerationState

    // --- AUTHENTICATION & SYNC FLOW EXPOSURE ---
    val currentUser: StateFlow<User?> = AuthManager.currentUser
    val syncStatus: StateFlow<SyncStatus> = WebSocketSyncManager.syncStatus
    val syncLogs: StateFlow<List<SyncLog>> = WebSocketSyncManager.syncLogs

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    init {
        try {
            val database = AppDatabase.getDatabase(application)
            val repo = StudentRepository(database.studentDao)
            repository = repo

            viewModelScope.launch {
                try {
                    repo.allStudents.collect { list ->
                        _allStudents.value = list
                    }
                } catch (t: Throwable) {
                    Log.e("MainViewModel", "Gagal mengumpulkan data siswa dari Room: ${t.localizedMessage}", t)
                }
            }
        } catch (t: Throwable) {
            Log.e("MainViewModel", "Kritis: Gagal inisialisasi database Room: ${t.localizedMessage}", t)
        }

        // Load saved Api Key from SharedPreferences if existing
        val prefs = application.getSharedPreferences("report_bot_prefs", Context.MODE_PRIVATE)
        _customApiKeyState.value = prefs.getString("gemini_api_key", "") ?: ""

        // Initialize Global Auth & WebSocket Sync engines
        try {
            AuthManager.initialize(application)
        } catch (t: Throwable) {
            Log.e("MainViewModel", "Gagal inisialisasi AuthManager pada ViewModel init: ${t.localizedMessage}")
        }

        try {
            WebSocketSyncManager.initialize(application, viewModelScope)
        } catch (t: Throwable) {
            Log.e("MainViewModel", "Gagal inisialisasi WebSocketSyncManager pada ViewModel init: ${t.localizedMessage}")
        }
    }

    fun selectStudent(student: Student?) {
        _selectedStudent.value = student
    }

    fun saveApiKey(key: String) {
        _customApiKeyState.value = key
        val prefs = getApplication<Application>().getSharedPreferences("report_bot_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    // --- AUTH FLOWS ---
    fun clearAuthError() {
        _authError.value = null
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _authError.value = null
            when (val result = AuthManager.signUp(email, password, fullName)) {
                is AuthResult.Success -> { /* Solved */ }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            when (val result = AuthManager.signIn(email, password)) {
                is AuthResult.Success -> { /* Solved */ }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun login(email: String, password: String) {
        signIn(email, password)
    }

    fun signInWithGoogle(email: String, fullName: String) {
        viewModelScope.launch {
            _authError.value = null
            when (val result = AuthManager.signInWithGoogle(email, fullName)) {
                is AuthResult.Success -> { /* Solved */ }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun loginWithGoogle() {
        signInWithGoogle("guru.demobot@gmail.com", "Guru Demo Bot Speedy")
    }

    fun signInWithFacebook(email: String, fullName: String) {
        viewModelScope.launch {
            _authError.value = null
            when (val result = AuthManager.signInWithFacebook(email, fullName)) {
                is AuthResult.Success -> { /* Solved */ }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun loginWithFacebook() {
        signInWithFacebook("guru.facebook@gmail.com", "Guru Demo Facebook")
    }

    fun resetPassword(email: String, newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            when (val result = AuthManager.resetPassword(email, newPassword)) {
                is AuthResult.Success -> {
                    onComplete(true)
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                    onComplete(false)
                }
            }
        }
    }

    fun signOut() {
        AuthManager.signOut()
    }

    // --- SYNC ACTIONS ---
    fun setWsUrl(url: String) {
        WebSocketSyncManager.setWsUrl(url)
    }

    fun setSimulationEnabled(enabled: Boolean) {
        WebSocketSyncManager.setSimulationEnabled(enabled)
    }

    fun isSimulationActive(): Boolean {
        return WebSocketSyncManager.isSimulationActive()
    }

    fun triggerManualSimulationPush() {
        WebSocketSyncManager.triggerManualSimulationPush()
    }

    fun saveStudent(student: Student, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = repository
            if (repo != null) {
                val id = repo.insertStudent(student)
                withContext(Dispatchers.Main) {
                    // Refresh selected student with updated values
                    val updated = student.copy(id = if (student.id == 0L) id else student.id)
                    _selectedStudent.value = updated
                    onComplete(id)
                }
            } else {
                Log.e("MainViewModel", "Gagal menyimpan murid: repository offline/tidak tersedia")
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = repository
            if (repo != null) {
                repo.deleteStudent(student)
                withContext(Dispatchers.Main) {
                    if (_selectedStudent.value?.id == student.id) {
                        _selectedStudent.value = null
                    }
                }
            } else {
                Log.e("MainViewModel", "Gagal menghapus murid: repository offline/tidak tersedia")
            }
        }
    }

    // --- AI GENERATORS ---

    fun generateAcademicAI(student: Student, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingAcademic.value = true
            
            val info = """
                Nilai Matematika: ${student.mathGrade}
                Nilai IPA: ${student.scienceGrade}
                Nilai Bahasa Indonesia: ${student.languageGrade}
                Nilai IPS: ${student.socialGrade}
                Nilai Bahasa Inggris: ${student.englishGrade}
            """.trimIndent()
            
            val result = GeminiClient.generateReportSection(
                aspectType = "academic",
                studentName = student.name,
                notes = notes,
                additionalInfo = info,
                customApiKey = _customApiKeyState.value
            )

            withContext(Dispatchers.Main) {
                val updated = student.copy(
                    academicNotes = notes,
                    generatedAcademicReport = result,
                    updatedAt = System.currentTimeMillis()
                )
                _selectedStudent.value = updated
                saveStudent(updated)
                _isGeneratingAcademic.value = false
            }
        }
    }

    fun generateCharacterAI(student: Student, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingCharacter.value = true
            
            val ratingsDescription = """
                Integritas: ${student.integrityRating}/5
                Kedisiplinan: ${student.disciplineRating}/5
                Kerjasama: ${student.cooperationRating}/5
                Rasa Hormat: ${student.respectRating}/5
            """.trimIndent()

            val result = GeminiClient.generateReportSection(
                aspectType = "character",
                studentName = student.name,
                notes = notes,
                additionalInfo = ratingsDescription,
                customApiKey = _customApiKeyState.value
            )

            withContext(Dispatchers.Main) {
                val updated = student.copy(
                    characterNotes = notes,
                    generatedCharacterReport = result,
                    updatedAt = System.currentTimeMillis()
                )
                _selectedStudent.value = updated
                saveStudent(updated)
                _isGeneratingCharacter.value = false
            }
        }
    }

    fun generateExtracurricularAI(student: Student, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingExtracurricular.value = true
            
            val statsAndPrograms = """
                Tinggi Badan: ${student.heightCm} cm
                Berat Badan: ${student.weightKg} kg
                Ketidakhadiran (Sakit/Izin/Alpa): ${student.sickLeaveDays}/${student.permissionLeaveDays}/${student.unexcusedAbsenceDays} Hari
                Nama Ekstrakurikuler: ${student.extracurricularName}
                Nilai Ekstrakurikuler: ${student.extracurricularGrade}
            """.trimIndent()

            val result = GeminiClient.generateReportSection(
                aspectType = "extracurricular",
                studentName = student.name,
                notes = notes,
                additionalInfo = statsAndPrograms,
                customApiKey = _customApiKeyState.value
            )

            withContext(Dispatchers.Main) {
                val updated = student.copy(
                    extracurricularNotes = notes,
                    generatedExtracurricularReport = result,
                    updatedAt = System.currentTimeMillis()
                )
                _selectedStudent.value = updated
                saveStudent(updated)
                _isGeneratingExtracurricular.value = false
            }
        }
    }

    // --- PDF TRIGGERS ---

    fun generatePdf(context: Context, student: Student, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                ReportPdfGenerator.generate3PageReport(context, student)
            }
            _pdfGenerationState.value = file
            onResult(file)
        }
    }

    fun clearPdfState() {
        _pdfGenerationState.value = null
    }
}
