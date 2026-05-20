package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AnalyticsDashboard(
    students: List<Student>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Akademik") } // "Akademik", "Karakter", "Kehadiran"
    var selectedDateRange by remember { mutableStateOf("Semua") } // "Semua", "7 Hari", "30 Hari"

    val filteredStudents = remember(students, selectedDateRange) {
        val now = System.currentTimeMillis()
        students.filter { student ->
            when (selectedDateRange) {
                "7 Hari" -> (now - student.updatedAt) <= 7L * 24 * 60 * 60 * 1000L
                "30 Hari" -> (now - student.updatedAt) <= 30L * 24 * 60 * 60 * 1000L
                else -> true
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High level warning if empty
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                    .background(SurfaceLight)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = PrimaryPurple, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data Grafik Kosong",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daftarkan murid dan input nilai untuk memvisualisasikan grafik analitis di sini.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            return@Column
        }

        // Filter 1: Category Selection
        Text(
            text = "KATEGORI ASPEK ANALISIS",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight, RoundedCornerShape(10.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val categories = listOf("Akademik", "Karakter", "Kehadiran")
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) CardInnerWhite else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) OutlineBorder else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp)
                        .testTag("filter_cat_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filter 2: Date Selector
        Text(
            text = "FILTER TENTANG TANGGAL",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight, RoundedCornerShape(10.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val dates = listOf("Semua", "7 Hari", "30 Hari")
            dates.forEach { range ->
                val isSelected = selectedDateRange == range
                val label = when (range) {
                    "7 Hari" -> "7 Hari"
                    "30 Hari" -> "30 Hari"
                    else -> "Semua Waktu"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) CardInnerWhite else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) OutlineBorder else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedDateRange = range }
                        .padding(vertical = 8.dp)
                        .testTag("filter_date_$range"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active State KPI box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            border = BorderStroke(1.dp, OutlineBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SAMPEL TERFILTER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = OnPrimaryContainer,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${filteredStudents.size} dari ${students.size} Murid",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnPrimaryContainer
                    )
                }
                Box(
                    modifier = Modifier
                        .background(CardInnerWhite, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val contextLabel = when (selectedCategory) {
                        "Akademik" -> "Rata Rapor"
                        "Karakter" -> "Rata Sikap"
                        else -> "Rata Hadir"
                    }
                    val metricValue = when (selectedCategory) {
                        "Akademik" -> {
                            if (filteredStudents.isEmpty()) "-" else {
                                val avg = filteredStudents.map {
                                    (it.mathGrade + it.scienceGrade + it.languageGrade + it.socialGrade + it.englishGrade) / 5.0
                                }.average()
                                String.format(Locale.US, "%.1f", avg)
                            }
                        }
                        "Karakter" -> {
                            if (filteredStudents.isEmpty()) "-" else {
                                val avg = filteredStudents.map {
                                    (it.integrityRating + it.disciplineRating + it.cooperationRating + it.respectRating) / 4.0
                                }.average()
                                String.format(Locale.US, "%.2f", avg)
                            }
                        }
                        else -> {
                            if (filteredStudents.isEmpty()) "-" else {
                                val avgOff = filteredStudents.map {
                                    it.sickLeaveDays + it.permissionLeaveDays + it.unexcusedAbsenceDays
                                }.average()
                                String.format(Locale.US, "%.1f H", avgOff)
                            }
                        }
                    }
                    Text(
                        text = "$contextLabel: $metricValue",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                    .background(SurfaceLight)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada murid dalam filter tanggal ini",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            return@Column
        }

        // Render Charts according to selected Category
        when (selectedCategory) {
            "Akademik" -> {
                // Bar Chart: Subject averages
                SubjectBarChartCard(filteredStudents)

                // Line Chart: Trend of Averages
                AverageTrendLineChartCard(filteredStudents)
            }
            "Karakter" -> {
                // Column/Bar Chart of specific ratings
                CharacterAspectsBarChartCard(filteredStudents)

                // Pie Chart of Integrity distribution
                CharacterLevelPieChartCard(filteredStudents)
            }
            "Kehadiran" -> {
                // Bar Chart of unexcused/sick days
                AttendanceComparisonCard(filteredStudents)

                // Extracurricular Pie chart distribution
                ExtraDistributionPieChartCard(filteredStudents)
            }
        }

        // Tightly packed grid comparison table
        ComparisonListingSection(filteredStudents, selectedCategory)
    }
}

@Composable
fun SubjectBarChartCard(students: List<Student>) {
    val mathAvg = students.map { it.mathGrade }.average().toFloat()
    val scienceAvg = students.map { it.scienceGrade }.average().toFloat()
    val languageAvg = students.map { it.languageGrade }.average().toFloat()
    val socialAvg = students.map { it.socialGrade }.average().toFloat()
    val englishAvg = students.map { it.englishGrade }.average().toFloat()

    val subjectData = listOf(
        Pair("Math", mathAvg),
        Pair("Science", scienceAvg),
        Pair("Indo", languageAvg),
        Pair("Social", socialAvg),
        Pair("Eng", englishAvg)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "GRAFIK BATANG: RATA-RATA KOMPETENSI AKADEMIK (SKALAI 0-100)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("academic_bar_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = subjectData.size
                val spacing = 20.dp.toPx()
                val totalFixedSpacing = spacing * (barCount + 1)
                val barWidth = (canvasWidth - totalFixedSpacing) / barCount

                // Draw Y-axis guide lines (25%, 50%, 75%, 100%)
                val guideLines = listOf(0.25f, 0.5f, 0.75f, 1f)
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 8.dp.toPx()
                    isAntiAlias = true
                }

                guideLines.forEach { scale ->
                    val y = canvasHeight * (1f - scale)
                    drawLine(
                        color = OutlineBorder.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                    // Draw guide text
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(scale * 100).toInt()}",
                        5f,
                        y - 4f,
                        textPaint
                    )
                }

                // Draw Bars
                subjectData.forEachIndexed { index, (subject, valAvg) ->
                    val barHeightFraction = valAvg / 100f
                    val barHeight = canvasHeight * barHeightFraction
                    val x = spacing + index * (barWidth + spacing)
                    val y = canvasHeight - barHeight

                    // Beautiful high-contrast primary brush
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryPurple, PrimaryPurple.copy(alpha = 0.7f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw value text inside or on top of bar
                    val printValue = String.format(Locale.US, "%.1f", valAvg)
                    val valuePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 9.dp.toPx()
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        printValue,
                        x + (barWidth / 2) - (valuePaint.measureText(printValue) / 2),
                        y - 6f,
                        valuePaint
                    )

                    // Draw X Label
                    drawContext.canvas.nativeCanvas.drawText(
                        subject,
                        x + (barWidth / 2) - (textPaint.measureText(subject) / 2),
                        canvasHeight - 6f,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
fun AverageTrendLineChartCard(students: List<Student>) {
    // Sort students by update history or ID
    val sortedStudents = remember(students) {
        students.sortedBy { it.id }.takeLast(10) // Display last 10 updates for readability
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "GRAFIK GARIS: TREN PERSENTASE NILAI KELAS (10 SISWA TERAKHIR)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("academic_line_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (sortedStudents.isEmpty()) return@Canvas

                val paintGrid = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    strokeWidth = 1f
                    style = android.graphics.Paint.Style.STROKE
                }
                val paintText = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 8.dp.toPx()
                    isAntiAlias = true
                }

                // Draw horizontal rules
                val scaleLines = listOf(60f, 70f, 80f, 90f, 100f)
                scaleLines.forEach { target ->
                    val fraction = (target - 50f) / 50f // Map 50-100 to 0-1
                    val y = canvasHeight * (1f - fraction.coerceIn(0f, 1f))
                    drawLine(
                        color = OutlineBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${target.toInt()}",
                        5f,
                        y - 4f,
                        paintText
                    )
                }

                // Calculate control points
                val points = sortedStudents.mapIndexed { index, student ->
                    val studentAvg = (student.mathGrade + student.scienceGrade + student.languageGrade + student.socialGrade + student.englishGrade) / 5f
                    val xFraction = if (sortedStudents.size > 1) index.toFloat() / (sortedStudents.size - 1) else 0.5f
                    val yFraction = (studentAvg - 50f) / 50f
                    Offset(
                        x = 30.dp.toPx() + xFraction * (canvasWidth - 50.dp.toPx()),
                        y = canvasHeight * (1f - yFraction.coerceIn(0f, 1f))
                    )
                }

                // Draw Path line
                if (points.size > 1) {
                    val linePath = Path()
                    val backgroundPath = Path()

                    linePath.moveTo(points[0].x, points[0].y)
                    backgroundPath.moveTo(points[0].x, canvasHeight)
                    backgroundPath.lineTo(points[0].x, points[0].y)

                    for (i in 1 until points.size) {
                        linePath.lineTo(points[i].x, points[i].y)
                        backgroundPath.lineTo(points[i].x, points[i].y)
                    }

                    backgroundPath.lineTo(points.last().x, canvasHeight)
                    backgroundPath.close()

                    // Draw gradient underneath trend line
                    drawPath(
                        path = backgroundPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryPurple.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )

                    // Draw trend path
                    drawPath(
                        path = linePath,
                        color = PrimaryPurple,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Draw point nodes & labels
                points.forEachIndexed { i, pt ->
                    drawCircle(
                        color = SuccessGreen,
                        radius = 4.dp.toPx(),
                        center = pt
                    )

                    val studentAvg = (sortedStudents[i].mathGrade + sortedStudents[i].scienceGrade + sortedStudents[i].languageGrade + sortedStudents[i].socialGrade + sortedStudents[i].englishGrade) / 5f
                    val scoreText = String.format(Locale.US, "%.0f", studentAvg)
                    val initials = sortedStudents[i].name.take(3).uppercase()

                    // Value label
                    drawContext.canvas.nativeCanvas.drawText(
                        scoreText,
                        pt.x - 10f,
                        pt.y - 10f,
                        paintText.apply {
                            color = android.graphics.Color.BLACK
                            isFakeBoldText = true
                        }
                    )

                    // Student name initial label at bottom
                    drawContext.canvas.nativeCanvas.drawText(
                        initials,
                        pt.x - 12f,
                        canvasHeight - 6f,
                        paintText.apply {
                            color = android.graphics.Color.DKGRAY
                            isFakeBoldText = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterAspectsBarChartCard(students: List<Student>) {
    val avgIntegrity = students.map { it.integrityRating }.average().toFloat()
    val avgDiscipline = students.map { it.disciplineRating }.average().toFloat()
    val avgCooperation = students.map { it.cooperationRating }.average().toFloat()
    val avgRespect = students.map { it.respectRating }.average().toFloat()

    val ratingData = listOf(
        Pair("Integritas", avgIntegrity),
        Pair("Disiplin", avgDiscipline),
        Pair("Kerjasama", avgCooperation),
        Pair("Hormat", avgRespect)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "RATARATA INDEKS KARAKTER & SIKAP (SKALA 1-5)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("character_bar_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = ratingData.size
                val spacing = 24.dp.toPx()
                val totalSpacing = spacing * (barCount + 1)
                val barWidth = (canvasWidth - totalSpacing) / barCount

                val paintText = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 8.dp.toPx()
                    isAntiAlias = true
                }

                // Draw horizontal guide lines for 1 to 5 stars range rating
                for (rating in 1..5) {
                    val y = canvasHeight * (1f - (rating / 5f))
                    drawLine(
                        color = OutlineBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "$rating",
                        5f,
                        y - 4f,
                        paintText
                    )
                }

                ratingData.forEachIndexed { idx, (label, value) ->
                    val barHeightFraction = value / 5f
                    val barHeight = canvasHeight * barHeightFraction
                    val x = spacing + idx * (barWidth + spacing)
                    val y = canvasHeight - barHeight

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(AmberContrast, AmberContrast.copy(alpha = 0.7f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw text index
                    val printVal = String.format(Locale.US, "%.2f", value)
                    drawContext.canvas.nativeCanvas.drawText(
                        printVal,
                        x + (barWidth / 2) - (paintText.measureText(printVal) / 2),
                        y - 6f,
                        paintText.apply {
                            color = android.graphics.Color.BLACK
                            isFakeBoldText = true
                        }
                    )

                    // Draw Label
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x + (barWidth / 2) - (paintText.measureText(label) / 2),
                        canvasHeight - 6f,
                        paintText.apply {
                            color = android.graphics.Color.DKGRAY
                            isFakeBoldText = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterLevelPieChartCard(students: List<Student>) {
    // Classify students into levels based on internal rating averagings
    val results = remember(students) {
        var excellent = 0
        var good = 0
        var fair = 0

        students.forEach { s ->
            val avg = (s.integrityRating + s.disciplineRating + s.cooperationRating + s.respectRating) / 4.0
            if (avg >= 4.2) excellent++
            else if (avg >= 3.5) good++
            else fair++
        }
        listOf(
            Triple("Sangat Baik", excellent, SuccessGreen),
            Triple("Baik", good, PrimaryPurple),
            Triple("Perlu Bimbingan", fair, DangerRed)
        ).filter { it.second > 0 }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "PERSENTASE DISTRIBUSI KELAS SIKAP (RATA RATING)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (results.isEmpty()) {
                    Text("Data tidak tersedia", fontSize = 11.sp, color = TextSecondary)
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .testTag("character_pie_canvas")
                    ) {
                        val total = results.sumOf { it.second }.toFloat()
                        var startAngle = 0f

                        results.forEach { (_, count, color) ->
                            val sweepAngle = (count.toFloat() / total) * 360f
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val grandTotal = results.sumOf { it.second }
                        results.forEach { (label, count, color) ->
                            val pct = (count.toFloat() / grandTotal) * 100f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                                Text(
                                    text = "$label: $count murid (${String.format(Locale.US, "%.0f", pct)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceComparisonCard(students: List<Student>) {
    val totalSick = students.sumOf { it.sickLeaveDays }
    val totalPerm = students.sumOf { it.permissionLeaveDays }
    val totalUnex = students.sumOf { it.unexcusedAbsenceDays }

    val attData = listOf(
        Triple("Sakit", totalSick, SuccessGreen),
        Triple("Izin", totalPerm, AmberContrast),
        Triple("Alpa (Tanpa Ket)", totalUnex, DangerRed)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "GRAFIK BATANG HENTIAN ABSENSI KELAS (AKUMULASI HARI)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("attendance_bar_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val maxVal = attData.maxOf { it.second }.toFloat().coerceAtLeast(5f)
                val paintText = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 8.dp.toPx()
                    isAntiAlias = true
                }

                // Horizontal scale lines
                for (i in 0..4) {
                    val labelVal = (maxVal * (i / 4f)).toInt()
                    val y = canvasHeight * (1f - (i / 4f))

                    drawLine(
                        color = OutlineBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "$labelVal H",
                        5f,
                        y - 4f,
                        paintText
                    )
                }

                // Draw Group bar
                val barWidth = 36.dp.toPx()
                val gap = (canvasWidth - (barWidth * attData.size)) / (attData.size + 1)

                attData.forEachIndexed { index, (label, count, barColor) ->
                    val heightFraction = count.toFloat() / maxVal
                    val barHeight = canvasHeight * heightFraction
                    val x = gap + index * (barWidth + gap)
                    val y = canvasHeight - barHeight

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw text label val
                    drawContext.canvas.nativeCanvas.drawText(
                        "$count Hari",
                        x + (barWidth / 2) - (paintText.measureText("$count") / 2),
                        y - 6f,
                        paintText.apply {
                            color = android.graphics.Color.BLACK
                            isFakeBoldText = true
                        }
                    )

                    // Title
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x + (barWidth / 2) - (paintText.measureText(label) / 2),
                        canvasHeight - 6f,
                        paintText.apply {
                            color = android.graphics.Color.DKGRAY
                            isFakeBoldText = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtraDistributionPieChartCard(students: List<Student>) {
    val results = remember(students) {
        val distribution = mutableMapOf<String, Int>()
        students.forEach { s ->
            val key = s.extracurricularName.trim()
            val existing = distribution[key] ?: 0
            distribution[key] = existing + 1
        }
        val colors = listOf(PrimaryPurple, AmberContrast, SuccessGreen, DangerRed, BlueBadgeText)
        distribution.entries.toList().mapIndexed { i, entry ->
            Triple(entry.key, entry.value, colors[i % colors.size])
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
        border = BorderStroke(1.dp, OutlineBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "PROPORSINAL SEBARAN EKSTRAKURIKULER",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (results.isEmpty()) {
                    Text("Data tidak tersedia", fontSize = 11.sp, color = TextSecondary)
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .testTag("extra_pie_canvas")
                    ) {
                        val total = results.sumOf { it.second }.toFloat()
                        var startAngle = 0f

                        results.forEach { (_, count, color) ->
                            val sweepAngle = (count.toFloat() / total) * 360f
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val grandTotal = results.sumOf { it.second }
                        results.forEach { (label, count, color) ->
                            val pct = (count.toFloat() / grandTotal) * 100f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                                Text(
                                    text = "$label: $count (${String.format(Locale.US, "%.0f", pct)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonListingSection(
    students: List<Student>,
    category: String
) {
    Column {
        Text(
            text = "DAFTAR PERBANDINGAN NILAI RINCI",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardInnerWhite),
            border = BorderStroke(1.dp, OutlineBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Table Header row with strong background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NAMA DISIWA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.5f))
                    when (category) {
                        "Akademik" -> {
                            Text("MTK", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("IPA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("RATA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        }
                        "Karakter" -> {
                            Text("INTEG", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("DISIP", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("RATA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        }
                        else -> {
                            Text("SAK", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("ALPA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text("EKSTRA", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                students.forEachIndexed { i, student ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = student.name,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1
                        )

                        when (category) {
                            "Akademik" -> {
                                Text("${student.mathGrade}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text("${student.scienceGrade}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                val avg = (student.mathGrade + student.scienceGrade + student.languageGrade + student.socialGrade + student.englishGrade) / 5.0
                                Text(
                                    text = String.format(Locale.US, "%.1f", avg),
                                    color = PrimaryPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.End
                                )
                            }
                            "Karakter" -> {
                                Text("${student.integrityRating}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text("${student.disciplineRating}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                val avg = (student.integrityRating + student.disciplineRating + student.cooperationRating + student.respectRating) / 4.0
                                Text(
                                    text = String.format(Locale.US, "%.2f", avg),
                                    color = AmberContrast,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.End
                                )
                            }
                            else -> {
                                Text("${student.sickLeaveDays}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text("${student.unexcusedAbsenceDays}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                Text(
                                    text = "${student.extracurricularName} (${student.extracurricularGrade})",
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (i < students.size - 1) {
                        HorizontalDivider(color = OutlineBorder.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}
