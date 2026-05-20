package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val className: String,
    val studentNumber: String,
    
    // --- ASPECT 1: ACADEMIC (Akademik) ---
    val mathGrade: Int = 80,
    val scienceGrade: Int = 80,
    val languageGrade: Int = 80,
    val socialGrade: Int = 80,
    val englishGrade: Int = 80,
    val academicNotes: String = "",
    val generatedAcademicReport: String = "",
    
    // --- ASPECT 2: CHARACTER & SIKAP (Karakter & Sikap) ---
    val integrityRating: Int = 4, // 1 to 5
    val disciplineRating: Int = 4, // 1 to 5
    val cooperationRating: Int = 4, // 1 to 5
    val respectRating: Int = 4, // 1 to 5
    val characterNotes: String = "",
    val generatedCharacterReport: String = "",
    
    // --- ASPECT 3: PHYSICAL & EXTRA (Ekstrakurikuler & Fisik) ---
    val heightCm: Int = 140,
    val weightKg: Int = 40,
    val sickLeaveDays: Int = 0,
    val permissionLeaveDays: Int = 0,
    val unexcusedAbsenceDays: Int = 0,
    val extracurricularName: String = "Pramuka",
    val extracurricularGrade: String = "A", // A, B, C, D
    val extracurricularNotes: String = "",
    val generatedExtracurricularReport: String = "",
    
    // General timestamp
    val updatedAt: Long = System.currentTimeMillis()
)
