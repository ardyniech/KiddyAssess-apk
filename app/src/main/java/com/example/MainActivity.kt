package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.Student
import com.example.data.User
import com.example.api.*
import com.example.ui.theme.*
import com.example.ui.components.AnalyticsDashboard
import com.example.viewmodel.MainViewModel
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = try {
            ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            )[MainViewModel::class.java]
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "FATAL: Gagal instansiasi MainViewModel: ${t.localizedMessage}", t)
            throw t
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = BackgroundLight
                ) { innerPadding ->
                    val context = LocalContext.current
                    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

                    if (currentUser == null) {
                        AuthScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        AppWorkspaceScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            onSharePdf = { file -> sharePdfFile(context, file) }
                        )
                    }
                }
            }
        }
    }

    private fun sharePdfFile(context: Context, file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Kirim / Bagikan Rapor PDF Belajar"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppWorkspaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onSharePdf: (File) -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val selectedStudent by viewModel.selectedStudent.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKeyState.collectAsStateWithLifecycle()

    var showApiKeyPanel by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showSyncSettings by remember { mutableStateOf(false) }
    var workspaceTabMode by remember { mutableStateOf("DAFTAR") } // "DAFTAR" or "GRAFIK"

    val filteredList = remember(students, searchKeyword) {
        if (searchKeyword.isBlank()) students else {
            students.filter {
                it.name.contains(searchKeyword, ignoreCase = true) ||
                it.studentNumber.contains(searchKeyword) ||
                it.className.contains(searchKeyword, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_workspace_column")
    ) {
        // App top header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPEEDY REPORT WORKSPACE",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "AI-Driven Report Auto Logger",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Connection Sync Status indicator
                        val syncState by viewModel.syncStatus.collectAsStateWithLifecycle()
                        val connectionColor = when (syncState) {
                            SyncStatus.CONNECTED -> SuccessGreen
                            SyncStatus.SYNCING -> PrimaryPurple
                            SyncStatus.CONNECTING -> AmberContrast
                            SyncStatus.DISCONNECTED -> DangerRed
                        }
                        val connectionLabel = when (syncState) {
                            SyncStatus.CONNECTED -> "Online"
                            SyncStatus.SYNCING -> "Sinkron"
                            SyncStatus.CONNECTING -> "Konek..."
                            SyncStatus.DISCONNECTED -> "Offline"
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(connectionColor.copy(alpha = 0.15f))
                                .border(1.dp, connectionColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { showSyncSettings = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(connectionColor, CircleShape)
                                )
                                Text(
                                    text = connectionLabel,
                                    color = connectionColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Settings Toggle Icon
                        IconButton(
                            onClick = { showApiKeyPanel = !showApiKeyPanel },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("settings_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan Kunci API",
                                tint = if (customApiKey.isNotBlank() && customApiKey != "MY_GEMINI_API_KEY") SuccessGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // User profile initials circle button
                        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                        currentUser?.let { user ->
                            val initials = remember(user.fullName) {
                                val parts = user.fullName.trim().split("\\s+".toRegex())
                                if (parts.size >= 2) {
                                    "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                } else if (parts.isNotEmpty()) {
                                    parts[0].take(2).uppercase()
                                } else {
                                    "GU"
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainer)
                                    .border(1.dp, PrimaryPurple, CircleShape)
                                    .clickable { showProfileDialog = true }
                                    .testTag("profile_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = OnPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // API Key input panel
                AnimatedVisibility(visible = showApiKeyPanel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(CardInnerWhite, RoundedCornerShape(12.dp))
                            .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Pengaturan Kunci API Gemini (Opsional)",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aturan Keamanan: Kunci API disimpan secara lokal di SharedPreferences aplikasi. Jangan membagikan APK produksi dengan kunci yang disematkan.",
                            color = AmberContrast,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        var tempKeyState by remember { mutableStateOf(customApiKey) }
                        var showKeySecret by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = tempKeyState,
                            onValueChange = { tempKeyState = it },
                            placeholder = { Text("Masukkan GEMINI_API_KEY Anda...", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = if (showKeySecret) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                TextButton(onClick = { showKeySecret = !showKeySecret }) {
                                    Text(
                                        text = if (showKeySecret) "TUTUP" else "LIHAT",
                                        color = PrimaryPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = OutlineBorder,
                                focusedContainerColor = CardInnerWhite,
                                unfocusedContainerColor = CardInnerWhite
                            ),
                            textStyle = TextStyle(fontSize = 11.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("api_key_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveApiKey(tempKeyState)
                                    showApiKeyPanel = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("save_api_key_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simpan Kunci", color = PureWhite, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Main Dynamic Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val focusStudent = selectedStudent
            if (focusStudent == null) {
                // Directory & Dashboard View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Compact Workspace Segmented Switch (List vs Analytics) with extremely high contrast
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceLight)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (workspaceTabMode == "DAFTAR") CardInnerWhite else Color.Transparent)
                                .border(
                                    width = if (workspaceTabMode == "DAFTAR") 1.dp else 0.dp,
                                    color = if (workspaceTabMode == "DAFTAR") OutlineBorder else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { workspaceTabMode = "DAFTAR" }
                                .testTag("tab_workspace_directory"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (workspaceTabMode == "DAFTAR") PrimaryPurple else TextSecondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "DIREKTORI PROFIL",
                                    color = if (workspaceTabMode == "DAFTAR") TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (workspaceTabMode == "GRAFIK") CardInnerWhite else Color.Transparent)
                                .border(
                                    width = if (workspaceTabMode == "GRAFIK") 1.dp else 0.dp,
                                    color = if (workspaceTabMode == "GRAFIK") OutlineBorder else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { workspaceTabMode = "GRAFIK" }
                                .testTag("tab_workspace_analytics"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = if (workspaceTabMode == "GRAFIK") PrimaryPurple else TextSecondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "ANALISIS GRAFIK",
                                    color = if (workspaceTabMode == "GRAFIK") TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (workspaceTabMode == "GRAFIK") {
                        AnalyticsDashboard(students = students, modifier = Modifier.weight(1f))
                    } else {
                        // Visual quick dashboard layout (compact & clear Material You style cards)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Students Box (Uses Primary Container matching Tailwind Daily Analytics block)
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .background(PrimaryContainer, RoundedCornerShape(16.dp))
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Text("JUMLAH MURID", color = OnPrimaryContainer, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(
                                    text = "${students.size}",
                                    color = OnPrimaryContainer,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Generated Documents Box
                            Column(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .background(SurfaceLight, RoundedCornerShape(16.dp))
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Text("REKAP Rapor AI", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                val populatedAI = students.count { it.generatedAcademicReport.isNotBlank() }
                                Text(
                                    text = "$populatedAI/${students.size}",
                                    color = if (populatedAI == students.size && populatedAI > 0) SuccessGreen else AmberContrast,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // High average Box
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .background(SurfaceLight, RoundedCornerShape(16.dp))
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Text("RATA KELAS", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                val classAvg = if (students.isEmpty()) 0.0 else {
                                    students.map { (it.mathGrade + it.scienceGrade + it.languageGrade + it.socialGrade + it.englishGrade) / 5.0 }.average()
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f", classAvg),
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Search and Action Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchKeyword,
                                onValueChange = { searchKeyword = it },
                                placeholder = { Text("Cari nama/kelas...", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, "Cari", tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = OutlineBorder,
                                    focusedContainerColor = CardInnerWhite,
                                    unfocusedContainerColor = CardInnerWhite
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("search_student_field")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Add Student button
                            Button(
                                onClick = { showAddStudentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("add_student_trigger")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Murid", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Student list directory
                        Text(
                            text = "DIREKTORI PROFIL MURID (${filteredList.size})",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (filteredList.isEmpty()) {
                            // Empty states
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                                    .background(SurfaceLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AccountCircle, "Kosong", tint = OutlineBorder, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Belum Ada Profil Murid Terdaftar", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Klik tombol '+ Murid' untuk memulai rekap siswa", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredList) { student ->
                                    StudentRowItem(
                                        student = student,
                                        onClick = { viewModel.selectStudent(student) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Bot Synchronization Feed Dashboard panel
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UMPAN AKTIVITAS REAL-TIME BOT",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            // Button to manually trigger sync/simulation
                            TextButton(
                                onClick = {
                                    viewModel.triggerManualSimulationPush()
                                    android.widget.Toast.makeText(context, "Sinkronisasi Manual Dipaksa⚡", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("SINKRON SEKARANG ⚡", color = PrimaryPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
                        if (syncLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(94.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardInnerWhite)
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Menunggu Stream Data Bot...",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Aktifkan simulasi umpan untuk demo streaming otomatis.",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardInnerWhite)
                                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(syncLogs) { log ->
                                    SyncLogItem(log)
                                }
                            }
                        }
                    }
                }
            } else {
                // Selected Focus workspace (Evaluation panels)
                StudentReportWorkspace(
                    student = focusStudent,
                    viewModel = viewModel,
                    onBack = { viewModel.selectStudent(null) },
                    onSharePdf = onSharePdf
                )
            }
        }
    }

    // Modal Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onSave = { name, className, number ->
                viewModel.saveStudent(
                    Student(
                        name = name,
                        className = className,
                        studentNumber = number
                    )
                )
                showAddStudentDialog = false
            }
        )
    }

    // --- PROFIL DIALOG (M3 style) ---
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    if (showProfileDialog && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, null, tint = PrimaryPurple)
                    Text("Profil Pengguna Sesi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(color = OutlineBorder)
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Nama:", modifier = Modifier.width(60.dp), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(currentUser!!.fullName, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Email:", modifier = Modifier.width(60.dp), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(currentUser!!.email, fontSize = 11.sp, color = TextPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Sumber:", modifier = Modifier.width(60.dp), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(if (currentUser!!.isGoogleUser) "Google Sign-In API" else "Aplikasi (Email/Password)", fontSize = 11.sp, color = TextPrimary)
                    }

                    Divider(color = OutlineBorder)

                    Text(
                        text = "Data disimpan aman secara enkripsi database local sandboxed Room DB.",
                        fontSize = 9.sp,
                        color = AmberContrast,
                        lineHeight = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Keluar (Logout)", color = PureWhite, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Tutup", color = TextSecondary, fontSize = 11.sp)
                }
            },
            containerColor = CardInnerWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- SYNC CONFIGURATION DIALOG ---
    if (showSyncSettings) {
        var tempWsUrl by remember { mutableStateOf("wss://speedy-report-bot.io/sync") }
        var appSimulateEnabled by remember { mutableStateOf(viewModel.isSimulationActive()) }

        AlertDialog(
            onDismissRequest = { showSyncSettings = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = PrimaryPurple)
                    Text("Konfigurasi Real-time Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Atur alamat endpoint WebSocket server Speedy Report Bot untuk menerima push data langsung:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    OutlinedTextField(
                        value = tempWsUrl,
                        onValueChange = { tempWsUrl = it },
                        placeholder = { Text("wss://domain-bot.com/sync", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                viewModel.setWsUrl(tempWsUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Sambungkan Ulang WS", color = PureWhite, fontSize = 9.sp)
                        }
                    }

                    Divider(color = OutlineBorder)

                    // Simulation Switch section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Umpan Simulasi Latar Belakang Bot", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Menstimulasikan tanggapan real-time bot otomatis", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = appSimulateEnabled,
                            onCheckedChange = {
                                appSimulateEnabled = it
                                viewModel.setSimulationEnabled(it)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Update Murid Sekarang:",
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                viewModel.triggerManualSimulationPush()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Pancing Push Bot ⚡", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSyncSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Selesai", color = PureWhite, fontSize = 11.sp)
                }
            },
            containerColor = CardInnerWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun StudentRowItem(
    student: Student,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardInnerWhite)
            .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("student_item_row_${student.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name.uppercase(Locale.getDefault()),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NIS: ${student.studentNumber}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(OutlineBorder, CircleShape)
                )
                Text(
                    text = "Kelas: ${student.className}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        // Beautiful Material You style circular/rounded average score badge
        val avg = (student.mathGrade + student.scienceGrade + student.languageGrade + student.socialGrade + student.englishGrade) / 5.0
        val isPopulated = student.generatedAcademicReport.isNotBlank() || student.generatedCharacterReport.isNotBlank()

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinkBadgeBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${avg.toInt()}",
                    color = PinkBadgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (isPopulated) {
                Text(
                    text = "AI Aktif",
                    color = SuccessGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StudentReportWorkspace(
    student: Student,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSharePdf: (File) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val isGenAcademic by viewModel.isGeneratingAcademic.collectAsStateWithLifecycle()
    val isGenCharacter by viewModel.isGeneratingCharacter.collectAsStateWithLifecycle()
    val isGenExtra by viewModel.isGeneratingExtracurricular.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("report_workspace_layout")
    ) {
        // Workspace Head Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardInnerWhite)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_to_directory_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name.uppercase(Locale.getDefault()),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rekap Evaluasi Kelas ${student.className} | NIS ${student.studentNumber}",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }

            // Compact student profile delete item
            IconButton(
                onClick = { viewModel.deleteStudent(student) },
                modifier = Modifier.testTag("delete_student_button")
            ) {
                Icon(Icons.Default.Delete, "Hapus", tint = DangerRed, modifier = Modifier.size(18.dp))
            }
        }
        
        HorizontalDivider(thickness = 1.dp, color = OutlineBorder)

        // Custom M3 Aspect Tab bar (Beautiful rounded pill buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("AKADEMIK (P1)", "KARAKTER (P2)", "FISIK & EKSTRA (P3)")
            tabs.forEachIndexed { index, title ->
                val isActive = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) PrimaryContainer else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isActive) OnPrimaryContainer else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tab Content Areas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            when (selectedTab) {
                0 -> AcademicEvaluationTab(
                    student = student,
                    isGenerating = isGenAcademic,
                    onSaveStudent = { viewModel.saveStudent(it) },
                    onGenerateAI = { notes -> viewModel.generateAcademicAI(student, notes) }
                )
                1 -> CharacterEvaluationTab(
                    student = student,
                    isGenerating = isGenCharacter,
                    onSaveStudent = { viewModel.saveStudent(it) },
                    onGenerateAI = { notes -> viewModel.generateCharacterAI(student, notes) }
                )
                2 -> PhysicalExtraTab(
                    student = student,
                    isGenerating = isGenExtra,
                    onSaveStudent = { viewModel.saveStudent(it) },
                    onGenerateAI = { notes -> viewModel.generateExtracurricularAI(student, notes) }
                )
            }
        }

        // Sticky Footer: CETAK 3 HALAMAN PDF (Success color scheme)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceLight,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, OutlineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.generatePdf(context, student) { pdfFile ->
                            if (pdfFile != null) {
                                onSharePdf(pdfFile)
                            } else {
                                Toast.makeText(context, "Gagal mengompilasi PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_3page_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, "Unduh", tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BAGIKAN / CETAK RAPOR (3 HALAMAN PDF)",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ------ TAB 1: ACADEMIC ------
@Composable
fun AcademicEvaluationTab(
    student: Student,
    isGenerating: Boolean,
    onSaveStudent: (Student) -> Unit,
    onGenerateAI: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var draftNotes by remember(student.id) { mutableStateOf(student.academicNotes) }
    var draftReportText by remember(student.id) { mutableStateOf(student.generatedAcademicReport) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "EDIT KOMPETENSI KOMPONEN AKADEMIK",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Multi inputs for subjects
        val subjects = listOf(
            SubjectMetric("Matematika", student.mathGrade) { onSaveStudent(student.copy(mathGrade = it)) },
            SubjectMetric("Sains / IPA", student.scienceGrade) { onSaveStudent(student.copy(scienceGrade = it)) },
            SubjectMetric("Bahasa Indonesia", student.languageGrade) { onSaveStudent(student.copy(languageGrade = it)) },
            SubjectMetric("IPS / Sosial", student.socialGrade) { onSaveStudent(student.copy(socialGrade = it)) },
            SubjectMetric("Bahasa Inggris", student.englishGrade) { onSaveStudent(student.copy(englishGrade = it)) }
        )

        subjects.forEach { item ->
            SubjectGradeRow(metric = item)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Raw notes formulation block
        Text(
            text = "DRAFT CATATAN DAN PERKEMBANGAN GURU",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftNotes,
            onValueChange = { draftNotes = it },
            placeholder = { Text("cth: Menguasai aljabar dasar, namun perlu bimbingan perkalian ganda...", color = TextSecondary, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .testTag("academic_notes_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // AI trigger row
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gemini AI merumuskan penjelasan akademik (1-2 paragraf)...", color = OnPrimaryContainer, fontSize = 10.sp)
                }
            } else {
                Button(
                    onClick = { onGenerateAI(draftNotes) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("generate_academic_ai_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, "AI", tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRANSFORMASI CAPAIAN DENGAN AI (Page 1)", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generated block text wrapping area
        Text(
            text = "PREVIEW ULASAN KOMPETENSI (Edit langsung jika perlu)",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftReportText,
            onValueChange = {
                draftReportText = it
                onSaveStudent(student.copy(generatedAcademicReport = it))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("academic_ai_output")
        )
    }
}

data class SubjectMetric(
    val label: String,
    val currentValue: Int,
    val onValueChange: (Int) -> Unit
)

@Composable
fun SubjectGradeRow(metric: SubjectMetric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardInnerWhite, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = metric.label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(130.dp)
        )

        // Increment Decrement controllers
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { metric.onValueChange((metric.currentValue - 5).coerceIn(0, 100)) },
                modifier = Modifier.size(24.dp)
            ) {
                Text("-", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "${metric.currentValue}",
                color = if (metric.currentValue >= 75) SuccessGreen else DangerRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(40.dp)
                    .testTag("score_value_${metric.label.replace(" ", "_")}")
            )

            IconButton(
                onClick = { metric.onValueChange((metric.currentValue + 5).coerceIn(0, 100)) },
                modifier = Modifier.size(24.dp)
            ) {
                Text("+", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------ TAB 2: CHARACTER ------
@Composable
fun CharacterEvaluationTab(
    student: Student,
    isGenerating: Boolean,
    onSaveStudent: (Student) -> Unit,
    onGenerateAI: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var draftNotes by remember(student.id) { mutableStateOf(student.characterNotes) }
    var draftReportText by remember(student.id) { mutableStateOf(student.generatedCharacterReport) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "EVALUASI RATING SIKAP & AKHLAK MULIA",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // 4 Trait rows
        val traits = listOf(
            CharacterTraitItem("Integritas & Kejujuran", student.integrityRating) { onSaveStudent(student.copy(integrityRating = it)) },
            CharacterTraitItem("Kedisiplinan & Tanggungjawab", student.disciplineRating) { onSaveStudent(student.copy(disciplineRating = it)) },
            CharacterTraitItem("Kerjasama Kelompok", student.cooperationRating) { onSaveStudent(student.copy(cooperationRating = it)) },
            CharacterTraitItem("Sopan Santun / Rasa Hormat", student.respectRating) { onSaveStudent(student.copy(respectRating = it)) }
        )

        traits.forEach { trait ->
            TraitRatingRow(trait = trait)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Behaviors input formulating
        Text(
            text = "DRAFT CATATAN DAN REKAP SIKAP SISWA",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftNotes,
            onValueChange = { draftNotes = it },
            placeholder = { Text("cth: Santun dalam berdiskusi kelompok, namun kepatuhan membuang sampah harus ditingkatkan...", color = TextSecondary, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .testTag("character_notes_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // AI trigger row
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gemini AI merumuskan ulasan karakter (1-2 paragraf)...", color = OnPrimaryContainer, fontSize = 10.sp)
                }
            } else {
                Button(
                    onClick = { onGenerateAI(draftNotes) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("generate_character_ai_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, "AI", tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRANSFORMASI SIKAP DENGAN AI (Page 2)", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview output narrative
        Text(
            text = "PREVIEW ULASAN PERILAKU-KARAKTER",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftReportText,
            onValueChange = {
                draftReportText = it
                onSaveStudent(student.copy(generatedCharacterReport = it))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("character_ai_output")
        )
    }
}

data class CharacterTraitItem(
    val label: String,
    val score: Int,
    val onScoreChange: (Int) -> Unit
)

@Composable
fun TraitRatingRow(trait: CharacterTraitItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardInnerWhite, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = trait.label,
            color = TextPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(130.dp)
        )

        // Custom blocks rating
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (stars in 1..5) {
                val isFilled = stars <= trait.score
                Box(
                    modifier = Modifier
                        .size(24.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFilled) PrimaryPurple else OutlineBorder)
                        .clickable { trait.onScoreChange(stars) }
                        .testTag("trait_${trait.label.replace(" ", "_")}_star_$stars")
                )
            }
        }

        Text(
            text = "${trait.score}/5",
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ------ TAB 3: PHYSICAL & EXTRA ------
@Composable
fun PhysicalExtraTab(
    student: Student,
    isGenerating: Boolean,
    onSaveStudent: (Student) -> Unit,
    onGenerateAI: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var draftNotes by remember(student.id) { mutableStateOf(student.extracurricularNotes) }
    var draftReportText by remember(student.id) { mutableStateOf(student.generatedExtracurricularReport) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Physical Metric Inputs
        Text(
            text = "KONDISI FISIK DAN PERTUMBUHAN SISWA",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Height Text Field
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                border = BorderStroke(1.dp, OutlineBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("TINGGI BADAN", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(onClick = { onSaveStudent(student.copy(heightCm = (student.heightCm - 2).coerceIn(50, 250))) }, modifier = Modifier.size(24.dp)) {
                            Text("-", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${student.heightCm} cm",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).testTag("height_val_text")
                        )
                        IconButton(onClick = { onSaveStudent(student.copy(heightCm = (student.heightCm + 2).coerceIn(50, 250))) }, modifier = Modifier.size(24.dp)) {
                            Text("+", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Weight Text Field
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                border = BorderStroke(1.dp, OutlineBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("BERAT BADAN", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(onClick = { onSaveStudent(student.copy(weightKg = (student.weightKg - 1).coerceIn(10, 150))) }, modifier = Modifier.size(24.dp)) {
                            Text("-", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${student.weightKg} kg",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).testTag("weight_val_text")
                        )
                        IconButton(onClick = { onSaveStudent(student.copy(weightKg = (student.weightKg + 1).coerceIn(10, 150))) }, modifier = Modifier.size(24.dp)) {
                            Text("+", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Attendances Counter
        Text(
            text = "REKAPITULASI KEHADIRAN (Hari)",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sick
            AttendanceCounterCard(
                label = "SAKIT",
                count = student.sickLeaveDays,
                onChanged = { onSaveStudent(student.copy(sickLeaveDays = it)) },
                modifier = Modifier.weight(1f),
                tag = "sick_tag"
            )
            // Permission
            AttendanceCounterCard(
                label = "IZIN",
                count = student.permissionLeaveDays,
                onChanged = { onSaveStudent(student.copy(permissionLeaveDays = it)) },
                modifier = Modifier.weight(1f),
                tag = "permission_tag"
            )
            // Unexcused
            AttendanceCounterCard(
                label = "ALPA",
                count = student.unexcusedAbsenceDays,
                onChanged = { onSaveStudent(student.copy(unexcusedAbsenceDays = it)) },
                modifier = Modifier.weight(1f),
                tag = "alpa_tag"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Extracurricular details
        Text(
            text = "YAYASAN EKSTRAKURIKULER & NILAI CAPAIAN",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = student.extracurricularName,
                onValueChange = { onSaveStudent(student.copy(extracurricularName = it)) },
                placeholder = { Text("cth: Pramuka, Musik, Tari...", color = TextSecondary, fontSize = 11.sp) },
                label = { Text("Nama Program", color = TextSecondary, fontSize = 9.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = OutlineBorder,
                    focusedContainerColor = CardInnerWhite,
                    unfocusedContainerColor = CardInnerWhite
                ),
                textStyle = TextStyle(fontSize = 11.sp),
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
                    .testTag("extra_program_field")
            )

            // Extracurricular Grade Selection Row
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardInnerWhite)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val grades = listOf("A", "B", "C", "D")
                grades.forEach { g ->
                    val isSelected = student.extracurricularGrade == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { onSaveStudent(student.copy(extracurricularGrade = g)) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g,
                            color = if (isSelected) PureWhite else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Draft activity notes
        Text(
            text = "DRAFT CATATAN KEGIATAN & PERTUMBUHAN GURU",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftNotes,
            onValueChange = { draftNotes = it },
            placeholder = { Text("cth: Sangat antusias dalam baris-berbaris pramuka, kehadiran sangat stabil...", color = TextSecondary, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .testTag("activity_notes_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // AI generate triggers
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gemini AI merumuskan ulasan fisik & ekstra...", color = OnPrimaryContainer, fontSize = 10.sp)
                }
            } else {
                Button(
                    onClick = { onGenerateAI(draftNotes) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("generate_extra_ai_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, "AI", tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FORMULASI FISIK & EKSTRA DENGAN AI (Page 3)", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview output ulasan physical & extra
        Text(
            text = "PREVIEW ULASAN FISIK-EKSTRA (Edit langsung)",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = draftReportText,
            onValueChange = {
                draftReportText = it
                onSaveStudent(student.copy(generatedExtracurricularReport = it))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = OutlineBorder,
                focusedContainerColor = CardInnerWhite,
                unfocusedContainerColor = CardInnerWhite
            ),
            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("extra_ai_output")
        )
    }
}

@Composable
fun AttendanceCounterCard(
    label: String,
    count: Int,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                IconButton(onClick = { onChanged((count - 1).coerceAtLeast(0)) }, modifier = Modifier.size(24.dp)) {
                    Text("-", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "$count",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .width(20.dp)
                        .testTag("attendance_${tag}"),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onChanged(count + 1) }, modifier = Modifier.size(24.dp)) {
                    Text("+", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tambah Profil Murid Baru", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Siswa Lengkap", color = TextSecondary, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = OutlineBorder,
                        focusedContainerColor = CardInnerWhite,
                        unfocusedContainerColor = CardInnerWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_name_field")
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Kelas (contoh: 4-A)", color = TextSecondary, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = OutlineBorder,
                        focusedContainerColor = CardInnerWhite,
                        unfocusedContainerColor = CardInnerWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_class_field")
                )

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = { Text("Nomor Induk Siswa (NIS)", color = TextSecondary, fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = OutlineBorder,
                        focusedContainerColor = CardInnerWhite,
                        unfocusedContainerColor = CardInnerWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_number_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && className.isNotBlank() && studentNumber.isNotBlank()) {
                        onSave(name, className, studentNumber)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier.testTag("dialog_save_student_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Daftarkan", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary, fontSize = 11.sp)
            }
        },
        containerColor = SurfaceLight
    )
}

@Composable
fun SyncLogItem(log: SyncLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLight)
            .border(1.dp, OutlineBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val typeColor = when (log.type) {
            "AKADEMIK" -> PrimaryPurple
            "KARAKTER" -> SuccessGreen
            "SYNC" -> SuccessGreen
            "GRADE UPDATE" -> PrimaryPurple
            else -> AmberContrast
        }
        val typeBg = when (log.type) {
            "AKADEMIK" -> PrimaryContainer
            "KARAKTER" -> BlueBadgeBg
            "SYNC" -> BlueBadgeBg
            "GRADE UPDATE" -> PrimaryContainer
            else -> PinkBadgeBg
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(typeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = log.type,
                color = typeColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.studentName.uppercase(Locale.getDefault()),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = log.source,
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = log.content,
                color = TextPrimary,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

    val authError by viewModel.authError.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
        ) {
            // App Logo pairing (resembles Tailwind logo style)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Logo Bot",
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "SPEEDY REPORT BOT",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Sistem Sinkronisasi Laporan AI Sekolah",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isForgotPasswordMode) {
                // Forgot Password Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                    border = BorderStroke(1.dp, OutlineBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "LUPA KATA SANDI / UBAH PASSWORD",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        authError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DangerRed.copy(alpha = 0.15f))
                                    .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = err,
                                    color = DangerRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        resetSuccessMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuccessGreen.copy(alpha = 0.15f))
                                    .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Masukkan Email Terdaftar", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = OutlineBorder,
                                focusedContainerColor = SurfaceLight,
                                unfocusedContainerColor = SurfaceLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("reset_email_field")
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = { Text("Password Baru (Min 6 Karakter)", fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = OutlineBorder,
                                focusedContainerColor = SurfaceLight,
                                unfocusedContainerColor = SurfaceLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("reset_password_field")
                        )

                        Button(
                            onClick = {
                                resetSuccessMessage = null
                                if (email.isBlank() || newPassword.isBlank()) {
                                    android.widget.Toast.makeText(context, "Email dan Password tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.resetPassword(email, newPassword) { success ->
                                    if (success) {
                                        resetSuccessMessage = "Sandi berhasil diubah! Silakan masuk kembali."
                                        password = newPassword
                                        isForgotPasswordMode = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("reset_password_submit")
                        ) {
                            Text(
                                text = "UBAH KATA SANDI",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "Kembali ke Halaman Masuk",
                            color = PrimaryPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isForgotPasswordMode = false }
                                .padding(vertical = 4.dp)
                                .testTag("back_to_login")
                        )
                    }
                }
            } else {
                // Compact Segment Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLight)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isLoginMode) CardInnerWhite else Color.Transparent)
                            .clickable { isLoginMode = true }
                            .testTag("tab_login"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Masuk Akun",
                            color = if (isLoginMode) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!isLoginMode) CardInnerWhite else Color.Transparent)
                            .clickable { isLoginMode = false }
                            .testTag("tab_register"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Daftar Baru",
                            color = if (!isLoginMode) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Form container Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                    border = BorderStroke(1.dp, OutlineBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) "SILAKAN MASUK KE SYSTEM" else "DAFTARKAN AKUN BARU",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        authError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DangerRed.copy(alpha = 0.15f))
                                    .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = err,
                                    color = DangerRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        if (!isLoginMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                placeholder = { Text("Nama Lengkap Anda", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = OutlineBorder,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedContainerColor = SurfaceLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("auth_name_field")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Alamat Email", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = OutlineBorder,
                                focusedContainerColor = SurfaceLight,
                                unfocusedContainerColor = SurfaceLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("auth_email_field")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Kata Sandi (Min 6 karakter)", fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = OutlineBorder,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedContainerColor = SurfaceLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("auth_password_field")
                            )

                            if (isLoginMode) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Lupa Kata Sandi?",
                                        color = PrimaryPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { isForgotPasswordMode = true }
                                            .padding(top = 4.dp, bottom = 4.dp)
                                            .testTag("forgot_password_trigger")
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isLoginMode) {
                                    viewModel.login(email, password)
                                } else {
                                    viewModel.signUp(email, password, fullName)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("auth_submit_btn")
                        ) {
                            Text(
                                text = if (isLoginMode) "MASUK SEKARANG" else "REGISTRASI AKUN",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Divider + Social Sign In Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineBorder)
                Text("ATAU MASUK CEPAT", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineBorder)
            }

            // Social Logins Container Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Google Sign-In Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.loginWithGoogle()
                        }
                        .testTag("google_login_btn"),
                    colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                    border = BorderStroke(1.dp, OutlineBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(DangerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Facebook Sign-In Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.loginWithFacebook()
                        }
                        .testTag("facebook_login_btn"),
                    colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
                    border = BorderStroke(1.dp, OutlineBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1877F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("F", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Facebook",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

