package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 45

    /**
     * Generates a 3-page PDF file for the given student.
     * Page 1: Academic Report (Akademik)
     * Page 2: Character Evaluation (Karakter & Sikap)
     * Page 3: Extracurricular & Physical Metrics (Ekstrakurikuler & Kehadiran)
     *
     * Returns the File location if successful, or null if an error occurs.
     */
    fun generate3PageReport(context: Context, student: Student): File? {
        val pdfDocument = PdfDocument()

        try {
            // Document colors (High contrast)
            val primaryColor = Color.parseColor("#1A365D") // Deep Indigo 
            val secondaryColor = Color.parseColor("#4A5568") // Slate Gray
            val textColor = Color.parseColor("#1A202C") // Dark grey charcoal
            val lightBgColor = Color.parseColor("#F7FAFC") // Off-white
            val borderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // Paints
            val textPaintPrimary = TextPaint().apply {
                color = textColor
                textSize = 10f
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = primaryColor
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = secondaryColor
                textSize = 9f
                isAntiAlias = true
            }

            val sectionTitlePaint = Paint().apply {
                color = primaryColor
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // --- PAGE 1: ACADEMIC PROGRESS REPORT ---
            val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val canvas1 = page1.canvas
            
            drawPageHeader(canvas1, student, "Laporan Capaian Akademik (Page 1/3)", primaryColor, secondaryColor)
            drawAcademicContent(canvas1, student, primaryColor, textColor, lightBgColor, borderPaint, textPaintPrimary, sectionTitlePaint)
            drawPageFooter(canvas1, primaryColor, secondaryColor, "Halaman 1 dari 3 - Laporan Akademik")
            
            pdfDocument.finishPage(page1)

            // --- PAGE 2: CHARACTER & BEHAVIOR REPORT ---
            val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val canvas2 = page2.canvas
            
            drawPageHeader(canvas2, student, "Evaluasi Karakter & Sikap Murid (Page 2/3)", primaryColor, secondaryColor)
            drawCharacterContent(canvas2, student, primaryColor, textColor, lightBgColor, borderPaint, textPaintPrimary, sectionTitlePaint)
            drawPageFooter(canvas2, primaryColor, secondaryColor, "Halaman 2 dari 3 - Sikap & Karakter")
            
            pdfDocument.finishPage(page2)

            // --- PAGE 3: PHYSICAL METRICS & EXTRACURRICULAR REPORT ---
            val pageInfo3 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create()
            val page3 = pdfDocument.startPage(pageInfo3)
            val canvas3 = page3.canvas
            
            drawPageHeader(canvas3, student, "Fisik, Ekstrakurikuler & Kehadiran (Page 3/3)", primaryColor, secondaryColor)
            drawPhysicalContent(canvas3, student, primaryColor, textColor, lightBgColor, borderPaint, textPaintPrimary, sectionTitlePaint)
            drawPageFooter(canvas3, primaryColor, secondaryColor, "Halaman 3 dari 3 - Fisik & Ekstrakurikuler")
            
            pdfDocument.finishPage(page3)

            // Write PDF to target file in app cache or storage
            val fileName = "Rapor_Speedy_${student.name.replace(" ", "_")}_${student.studentNumber}.pdf"
            val outputDir = File(context.cacheDir, "reports")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val pdfFile = File(outputDir, fileName)
            val fileOutputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawPageHeader(canvas: Canvas, student: Student, pageTitle: String, primaryColor: Int, secondaryColor: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        
        // 1. School Title Brand
        paint.color = primaryColor
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("SEKOLAH TUNAS BANGSA CEMERLANG", MARGIN.toFloat(), 45f, paint)

        paint.color = secondaryColor
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Jl. Pendidikan Kreatif No. 101, Jakarta, Indonesia | Telp: (021) 555-0101", MARGIN.toFloat(), 58f, paint)

        // Triple rule visual split
        paint.color = primaryColor
        paint.strokeWidth = 1.5f
        canvas.drawLine(MARGIN.toFloat(), 66f, (PAGE_WIDTH - MARGIN).toFloat(), 66f, paint)
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN.toFloat(), 69f, (PAGE_WIDTH - MARGIN).toFloat(), 69f, paint)

        // 2. Report metadata and Student Biodata Banner
        val bgPaint = Paint().apply {
            color = Color.parseColor("#EDF2F7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN.toFloat(), 80f, (PAGE_WIDTH - MARGIN).toFloat(), 138f, bgPaint)

        // Border of banner
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(MARGIN.toFloat(), 80f, (PAGE_WIDTH - MARGIN).toFloat(), 138f, borderPaint)

        // Draw biodata
        val labelPaint = Paint().apply {
            color = secondaryColor
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#1A202C")
            textSize = 9f
            isFakeBoldText = false
            isAntiAlias = true
        }

        // Bio column 1
        canvas.drawText("Nama Murid:", (MARGIN + 12).toFloat(), 98f, labelPaint)
        canvas.drawText(student.name.uppercase(Locale.getDefault()), (MARGIN + 85).toFloat(), 98f, valuePaint.apply { isFakeBoldText = true })
        
        canvas.drawText("No. Induk (NIS):", (MARGIN + 12).toFloat(), 114f, labelPaint)
        canvas.drawText(student.studentNumber, (MARGIN + 85).toFloat(), 114f, valuePaint.apply { isFakeBoldText = false })

        canvas.drawText("Kelas:", (MARGIN + 12).toFloat(), 128f, labelPaint)
        canvas.drawText(student.className, (MARGIN + 85).toFloat(), 128f, valuePaint)

        // Bio column 2 (Page Scope)
        canvas.drawText("Periode Rapor:", (PAGE_WIDTH - MARGIN - 170).toFloat(), 98f, labelPaint)
        canvas.drawText("Semester Ganjil 2026", (PAGE_WIDTH - MARGIN - 90).toFloat(), 98f, valuePaint)

        canvas.drawText("Aspek Evaluasi:", (PAGE_WIDTH - MARGIN - 170).toFloat(), 114f, labelPaint)
        canvas.drawText(pageTitle, (PAGE_WIDTH - MARGIN - 90).toFloat(), 114f, valuePaint.apply { color = primaryColor; isFakeBoldText = true })

        canvas.drawText("Tanggal Cetak:", (PAGE_WIDTH - MARGIN - 170).toFloat(), 128f, labelPaint)
        val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
        canvas.drawText(formattedDate, (PAGE_WIDTH - MARGIN - 90).toFloat(), 128f, valuePaint.apply { color = Color.parseColor("#1A202C") })
    }

    private fun drawAcademicContent(
        canvas: Canvas,
        student: Student,
        primaryColor: Int,
        textColor: Int,
        lightBgColor: Int,
        borderPaint: Paint,
        textPaint: TextPaint,
        sectionTitlePaint: Paint
    ) {
        val yOffset = 160f
        canvas.drawText("I. CAPAIAN KOMPETENSI AKADEMIK (NILAI MATA PELAJARAN)", MARGIN.toFloat(), yOffset, sectionTitlePaint)

        // Academic Table Columns Header
        // Headers: No | Mata Pelajaran | Nilai Angka | Kategori | Predikat Kelulusan
        val colNo = MARGIN
        val colSubject = MARGIN + 30
        val colGrade = MARGIN + 220
        val colDesc = MARGIN + 320
        val colPred = MARGIN + 420
        val tableRight = PAGE_WIDTH - MARGIN

        val headerPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Table Header Layout
        canvas.drawRect(colNo.toFloat(), yOffset + 12, tableRight.toFloat(), yOffset + 32, headerPaint)

        val whiteTextPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawText("NO", (colNo + 10).toFloat(), yOffset + 25, whiteTextPaint)
        canvas.drawText("MATA PELAJARAN", (colSubject + 10).toFloat(), yOffset + 25, whiteTextPaint)
        canvas.drawText("NILAI", (colGrade + 10).toFloat(), yOffset + 25, whiteTextPaint)
        canvas.drawText("SKALA PRESTASI", (colDesc + 10).toFloat(), yOffset + 25, whiteTextPaint)
        canvas.drawText("PREDIKAT", (colPred + 10).toFloat(), yOffset + 25, whiteTextPaint)

        // Draw Row Data
        val subjects = listOf(
            "Matematika" to student.mathGrade,
            "Ilmu Pengetahuan Alam (IPA)" to student.scienceGrade,
            "Bahasa Indonesia" to student.languageGrade,
            "Ilmu Pengetahuan Sosial (IPS)" to student.socialGrade,
            "Bahasa Inggris" to student.englishGrade
        )

        var rowY = yOffset + 32
        val rowHeight = 22f

        val bgAlternativePaint = Paint().apply {
            color = lightBgColor
            style = Paint.Style.FILL
        }

        val rowTextPaint = Paint().apply {
            color = textColor
            textSize = 9f
            isAntiAlias = true
        }

        subjects.forEachIndexed { index, (subjectName, grade) ->
            if (index % 2 == 1) {
                canvas.drawRect(colNo.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight, bgAlternativePaint)
            }
            // Draw borders
            canvas.drawRect(colNo.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight, borderPaint)

            canvas.drawText((index + 1).toString(), (colNo + 12).toFloat(), rowY + 15, rowTextPaint)
            canvas.drawText(subjectName, (colSubject + 10).toFloat(), rowY + 15, rowTextPaint)
            canvas.drawText(grade.toString(), (colGrade + 20).toFloat(), rowY + 15, rowTextPaint.apply { isFakeBoldText = true })
            
            // Prestasi scale evaluation
            val level = getGradeLevel(grade)
            val predikat = getGradePredikat(grade)

            canvas.drawText(level, (colDesc + 10).toFloat(), rowY + 15, rowTextPaint.apply { isFakeBoldText = false })
            canvas.drawText(predikat, (colPred + 10).toFloat(), rowY + 15, rowTextPaint)

            rowY += rowHeight
        }

        // Draw average box
        val average = subjects.map { it.second }.average()
        canvas.drawRect(colNo.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight + 4, bgAlternativePaint)
        canvas.drawRect(colNo.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight + 4, borderPaint)

        val boldRowText = Paint().apply {
            color = primaryColor
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("NILAI RATA-RATA AKADEMIK", (colSubject + 10).toFloat(), rowY + 16, boldRowText)
        canvas.drawText(String.format(Locale.US, "%.1f", average), (colGrade + 20).toFloat(), rowY + 16, boldRowText)
        canvas.drawText(getGradeLevel(average.toInt()), (colDesc + 10).toFloat(), rowY + 16, boldRowText)

        // Visual Progress Meter
        val meterY = rowY + rowHeight + 25f
        canvas.drawText("VISUALISASI RATA-RATA KOMPETENSI:", colNo.toFloat(), meterY, rowTextPaint.apply { color = Color.parseColor("#4A5568"); isFakeBoldText = true })
        
        // Progress background track
        val trackLeft = colNo + 190f
        val trackRight = tableRight - 20f
        val trackWidth = trackRight - trackLeft
        val barPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(trackLeft, meterY - 10f, trackRight, meterY + 2f, 4f, 4f, barPaint)

        // Filled track
        val filledRatio = (average.coerceIn(0.0, 100.0) / 100.0).toFloat()
        val filledRight = trackLeft + (trackWidth * filledRatio)
        barPaint.color = if (average >= 75) Color.parseColor("#3182CE") else Color.parseColor("#E53E3E") // Blue if passed, Red if failing
        canvas.drawRoundRect(trackLeft, meterY - 10f, filledRight, meterY + 2f, 4f, 4f, barPaint)

        // Progress text percentage
        val percentageText = "${average.toInt()}%"
        canvas.drawText(percentageText, trackRight + 4f, meterY - 1f, boldRowText.apply { textSize = 8f })

        // Narrative Description section
        val descY = meterY + 30f
        canvas.drawText("II. DISKRIPSI CATATAN AKTIVITAS & PERKEMBANGAN AKADEMIK", MARGIN.toFloat(), descY, sectionTitlePaint)

        val finalNarrative = if (student.generatedAcademicReport.isNotBlank()) {
            student.generatedAcademicReport
        } else if (student.academicNotes.isNotBlank()) {
            student.academicNotes
        } else {
            "Siswa kelas ${student.className} bernama ${student.name} ini secara keseluruhan menunjukkan keaktifan belajar yang stabil. Evaluasi spesifik belum ditransformasikan oleh AI."
        }

        // Draw narrative with custom StaticLayout
        val narrativeTop = descY + 14f
        val contentWidth = PAGE_WIDTH - (MARGIN * 2)

        val staticLayout = StaticLayout.Builder.obtain(finalNarrative, 0, finalNarrative.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(MARGIN.toFloat(), narrativeTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // Draw signatures
        drawSignatureBlock(canvas, PAGE_HEIGHT - 130f, primaryColor, textColor)
    }

    private fun drawCharacterContent(
        canvas: Canvas,
        student: Student,
        primaryColor: Int,
        textColor: Int,
        lightBgColor: Int,
        borderPaint: Paint,
        textPaint: TextPaint,
        sectionTitlePaint: Paint
    ) {
        val yOffset = 160f
        canvas.drawText("I. HASIL PENILAIAN KARAKTER, SIKAP, DAN AKHLAK MULIA", MARGIN.toFloat(), yOffset, sectionTitlePaint)

        // Visual rating display
        // We list Intergitas, Kedisiplinan, Kerjasama, Rasa Hormat
        val colAspect = MARGIN + 20
        val colMeter = MARGIN + 250
        val colScoreLabel = MARGIN + 430
        val tableRight = PAGE_WIDTH - MARGIN

        var rowY = yOffset + 20f
        val rowHeight = 30f

        val traits = listOf(
            Triple("INTEGRITAS & KEJUJURAN", student.integrityRating, "Menjunjung tinggi nilai kejujuran, sportivitas, dan kesesuaian tindakan."),
            Triple("KEDISIPLINAN & TANGGUP JAWAB", student.disciplineRating, "Kepatuhan terhadap tata tertib, hadir tepat waktu, dan mengumpulkan tugas."),
            Triple("KERJASAMA / GOTO-ROYONG", student.cooperationRating, "Kemampuan berkolaborasi secara positif dalam kerja kelompok dan kegiatan luar kelas."),
            Triple("SOPAN SANTUN & RASA HORMAT", student.respectRating, "Menunjukkan tutur kata yang baik, toleransi, dan menghargai sesama teman serta guru.")
        )

        val traitNamePaint = Paint().apply {
            color = primaryColor
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val traitDescPaint = Paint().apply {
            color = Color.parseColor("#718096")
            textSize = 7.5f
            isAntiAlias = true
        }

        traits.forEachIndexed { index, (traitName, rating, traitDefinition) ->
            // Zebra striping
            if (index % 2 == 1) {
                val bgPaint = Paint().apply { color = lightBgColor; style = Paint.Style.FILL }
                canvas.drawRect(MARGIN.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight, bgPaint)
            }
            canvas.drawRect(MARGIN.toFloat(), rowY, tableRight.toFloat(), rowY + rowHeight, borderPaint)

            // Aspect text labels
            canvas.drawText(traitName, colAspect.toFloat(), rowY + 13f, traitNamePaint)
            canvas.drawText(traitDefinition, colAspect.toFloat(), rowY + 24f, traitDescPaint)

            // Aspect indicators (5 stars/blocks)
            val indicatorPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
            var starX = colMeter.toFloat()
            for (stars in 1..5) {
                indicatorPaint.color = if (stars <= rating) {
                    Color.parseColor("#3182CE") // Vibrant Blue for filled blocks
                } else {
                    Color.parseColor("#E2E8F0") // Light gray for empty blocks
                }
                
                // Draw a sleek high-contrast rounded square block as a substitute for smiley/emojis! Conforms to guidelines.
                canvas.drawRoundRect(starX, rowY + 10f, starX + 16f, rowY + 22f, 2f, 2f, indicatorPaint)
                starX += 22f
            }

            // Numeric Quality mapping label
            val qualityLabel = when(rating) {
                5 -> "Sangat Baik"
                4 -> "Baik"
                3 -> "Cukup"
                2 -> "Kurang"
                else -> "Sangat Kurang"
            }
            
            canvas.drawText("$rating/5 ($qualityLabel)", colScoreLabel.toFloat(), rowY + 18f, traitNamePaint.apply { color = textColor; textSize = 9f })

            rowY += rowHeight
        }

        // Narrative Description section
        val descY = rowY + 40f
        canvas.drawText("II. NARASI DISKRIPSI EVALUASI PERILAKU DAN BUDI PEKERTI", MARGIN.toFloat(), descY, sectionTitlePaint)

        val finalNarrative = if (student.generatedCharacterReport.isNotBlank()) {
            student.generatedCharacterReport
        } else if (student.characterNotes.isNotBlank()) {
            student.characterNotes
        } else {
            "Siswa menunjukkan karakteristik karakter yang terpuji sehari-hari di lingkungan sekolah. Teladan mulia bagi perkembangan kepribadian telah terperinci sejalan petunjuk dewan pengajar."
        }

        val narrativeTop = descY + 14f
        val contentWidth = PAGE_WIDTH - (MARGIN * 2)

        val staticLayout = StaticLayout.Builder.obtain(finalNarrative, 0, finalNarrative.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(MARGIN.toFloat(), narrativeTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // Signature block
        drawSignatureBlock(canvas, PAGE_HEIGHT - 130f, primaryColor, textColor)
    }

    private fun drawPhysicalContent(
        canvas: Canvas,
        student: Student,
        primaryColor: Int,
        textColor: Int,
        lightBgColor: Int,
        borderPaint: Paint,
        textPaint: TextPaint,
        sectionTitlePaint: Paint
    ) {
        val yOffset = 160f
        canvas.drawText("I. UKURAN FISIK & KONDISI PERTUMBUHAN SISWA", MARGIN.toFloat(), yOffset, sectionTitlePaint)

        // Direct layout for physical metrics cards side-by-side
        val halfWidth = (PAGE_WIDTH - (MARGIN * 2)) / 2
        val leftCardRight = MARGIN + halfWidth - 10
        val rightCardLeft = MARGIN + halfWidth + 10
        val tableRight = PAGE_WIDTH - MARGIN

        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#EDF2F7")
            style = Paint.Style.FILL
        }

        // Left Card - Physical Stats
        canvas.drawRoundRect(MARGIN.toFloat(), yOffset + 14f, leftCardRight.toFloat(), yOffset + 70f, 4f, 4f, cardBgPaint)
        canvas.drawRoundRect(MARGIN.toFloat(), yOffset + 14f, leftCardRight.toFloat(), yOffset + 70f, 4f, 4f, borderPaint)

        val labelPaintBold = Paint().apply {
            color = primaryColor
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val valuePaintBig = Paint().apply {
            color = textColor
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawText("TINGGI BADAN (Height):", (MARGIN + 12).toFloat(), yOffset + 32f, labelPaintBold)
        canvas.drawText("${student.heightCm} cm", (MARGIN + 12).toFloat(), yOffset + 54f, valuePaintBig)

        canvas.drawText("BERAT BADAN (Weight):", (MARGIN + 120).toFloat(), yOffset + 32f, labelPaintBold)
        canvas.drawText("${student.weightKg} kg", (MARGIN + 120).toFloat(), yOffset + 54f, valuePaintBig)


        // Right Card - Attendance Summary
        canvas.drawRoundRect(rightCardLeft.toFloat(), yOffset + 14f, tableRight.toFloat(), yOffset + 70f, 4f, 4f, cardBgPaint)
        canvas.drawRoundRect(rightCardLeft.toFloat(), yOffset + 14f, tableRight.toFloat(), yOffset + 70f, 4f, 4f, borderPaint)

        canvas.drawText("REKAPITULASI KEHADIRAN (Attendance Record)", (rightCardLeft + 12).toFloat(), yOffset + 30f, labelPaintBold)
        
        val recordTextPaint = Paint().apply {
            color = textColor
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText("Sakit (Sick Leave): ${student.sickLeaveDays} Hari", (rightCardLeft + 12).toFloat(), yOffset + 46f, recordTextPaint)
        canvas.drawText("Izin (Permitted Absence): ${student.permissionLeaveDays} Hari", (rightCardLeft + 12).toFloat(), yOffset + 58f, recordTextPaint)
        canvas.drawText("Alpa (Unexcused Absence): ${student.unexcusedAbsenceDays} Hari", (rightCardLeft + 12).toFloat(), yOffset + 66f, recordTextPaint.apply { color = Color.RED })


        // SECTION II - EXTRACURRICULAR activities
        val extraY = yOffset + 95f
        canvas.drawText("II. KEGIATAN EKSTRAKURIKULER & PENGEMBANGAN DIRI", MARGIN.toFloat(), extraY, sectionTitlePaint)

        canvas.drawRoundRect(MARGIN.toFloat(), extraY + 12f, tableRight.toFloat(), extraY + 54f, 4f, 4f, cardBgPaint)
        canvas.drawRoundRect(MARGIN.toFloat(), extraY + 12f, tableRight.toFloat(), extraY + 54f, 4f, 4f, borderPaint)

        canvas.drawText("PROGRAM KEGIATAN:", (MARGIN + 12).toFloat(), extraY + 28f, labelPaintBold)
        canvas.drawText(student.extracurricularName.uppercase(Locale.getDefault()), (MARGIN + 12).toFloat(), extraY + 44f, valuePaintBig.apply { textSize = 11f; color = primaryColor })

        canvas.drawText("NILAI CAPAIAN:", (PAGE_WIDTH - MARGIN - 140).toFloat(), extraY + 28f, labelPaintBold)
        canvas.drawText(student.extracurricularGrade, (PAGE_WIDTH - MARGIN - 140).toFloat(), extraY + 44f, valuePaintBig.apply { color = textColor; textSize = 13f })

        canvas.drawText("DESKRIPSI CAPAIAN:", (PAGE_WIDTH - MARGIN - 70).toFloat(), extraY + 28f, labelPaintBold)
        val termDescription = when(student.extracurricularGrade) {
            "A" -> "Sangat Aktif & Memuaskan"
            "B" -> "Aktif & Baik"
            "C" -> "Cukup Aktif"
            else -> "Memerlukan Bimbingan"
        }
        canvas.drawText(termDescription, (PAGE_WIDTH - MARGIN - 70).toFloat(), extraY + 44f, recordTextPaint.apply { textSize = 9f })


        // SECTION III - Narrative summary (AI generated)
        val descY = extraY + 84f
        canvas.drawText("III. NARASI PENGEMBANGAN FISIK, EKSTRAKURIKULER & DISIPLIN", MARGIN.toFloat(), descY, sectionTitlePaint)

        val finalNarrative = if (student.generatedExtracurricularReport.isNotBlank()) {
            student.generatedExtracurricularReport
        } else if (student.extracurricularNotes.isNotBlank()) {
            student.extracurricularNotes
        } else {
            "Siswa menunjukkan minat sehat dalam program ekstrakurikuler serta rekapitulasi kehadiran yang memadai demi menunjang kebugaran fisik dan partisipasi luar kelas."
        }

        val narrativeTop = descY + 14f
        val contentWidth = PAGE_WIDTH - (MARGIN * 2)

        val staticLayout = StaticLayout.Builder.obtain(finalNarrative, 0, finalNarrative.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(MARGIN.toFloat(), narrativeTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // Signature block
        drawSignatureBlock(canvas, PAGE_HEIGHT - 130f, primaryColor, textColor)
    }

    private fun drawSignatureBlock(canvas: Canvas, yOffset: Float, primaryColor: Int, textColor: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = textColor
        paint.textSize = 9f

        // Parent Column
        canvas.drawText("Mengetahui,", MARGIN.toFloat() + 40f, yOffset, paint)
        canvas.drawText("Orang Tua / Wali Siswa,", MARGIN.toFloat() + 20f, yOffset + 12f, paint)
        canvas.drawLine(MARGIN.toFloat() + 10f, yOffset + 68f, MARGIN.toFloat() + 140f, yOffset + 68f, paint)

        // Teacher Column
        val rightColLeft = PAGE_WIDTH - MARGIN - 140f
        canvas.drawText("Jakarta, .......................... 2026", rightColLeft + 10f, yOffset, paint)
        canvas.drawText("Wali Kelas Pendamping,", rightColLeft + 20f, yOffset + 12f, paint)
        canvas.drawLine(rightColLeft, yOffset + 68f, (PAGE_WIDTH - MARGIN).toFloat(), yOffset + 68f, paint)
        canvas.drawText("Komite Dewan Guru", rightColLeft + 30f, yOffset + 80f, paint.apply { isFakeBoldText = true; color = primaryColor })
    }

    private fun drawPageFooter(canvas: Canvas, primaryColor: Int, secondaryColor: Int, pageLabel: String) {
        val paint = Paint().apply { isAntiAlias = true; color = secondaryColor; textSize = 7.5f }
        paint.color = Color.parseColor("#A0AEC0")
        canvas.drawLine(MARGIN.toFloat(), (PAGE_HEIGHT - 35).toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (PAGE_HEIGHT - 35).toFloat(), paint)
        canvas.drawText("Dicetak dari Sistem Bot Speedy Report AI | Data Digital Sah", MARGIN.toFloat(), (PAGE_HEIGHT - 22).toFloat(), paint)
        canvas.drawText(pageLabel, (PAGE_WIDTH - MARGIN - 130).toFloat(), (PAGE_HEIGHT - 22).toFloat(), paint.apply { isFakeBoldText = true })
    }

    private fun getGradeLevel(grade: Int): String = when {
        grade >= 92 -> "Sangat Baik (A)"
        grade >= 82 -> "Baik (B)"
        grade >= 75 -> "Cukup (C)"
        else -> "Perlu Intervensi Khusus (D)"
    }

    private fun getGradePredikat(grade: Int): String = when {
        grade >= 92 -> "Sangat Kompeten"
        grade >= 82 -> "Kompeten"
        grade >= 75 -> "Cukup Kompeten"
        else -> "Kurang Kompeten"
    }
}
