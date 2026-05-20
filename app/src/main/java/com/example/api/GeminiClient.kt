package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Generates a paragraph of report card evaluation based on teacher prompt.
     */
    suspend fun generateReportSection(
        aspectType: String,
        studentName: String,
        notes: String,
        additionalInfo: String,
        customApiKey: String? = null
    ): String {
        // Fallback to BuildConfig if no custom key provided
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: API Key is not set. Please set the GEMINI_API_KEY inside the App settings or .env file."
        }

        val prompt = when (aspectType) {
            "academic" -> """
                Tulis ulasan laporan akademik (rapor) formal, memotivasi, dan profesional untuk murid bernama "$studentName" dalam Bahasa Indonesia sepanjang 1-2 paragraf padat.
                Detail mata pelajaran dan nilai:
                $additionalInfo
                Catatan khusus guru: "$notes"
                
                Aturan penulisan:
                1. Gunakan gaya bahasa formal, sopan, dan instruktif.
                2. Apresiasi pencapaian tinggi dan berikan saran perbaikan konkret yang membangun untuk nilai yang kurang.
                3. Hindari penggunaan placeholder atau tanda kurung. Langsung berupa hasil ulasan jadi.
            """.trimIndent()
            
            "character" -> """
                Tulis ulasan perkembangan karakter, perilaku, dan sikap sosial formal untuk murid bernama "$studentName" dalam Bahasa Indonesia sepanjang 1-2 paragraf padat.
                Detail tingkat sikap (skala 1-5):
                $additionalInfo
                Catatan khusus sikap guru: "$notes"
                
                Aturan penulisan:
                1. Fokus pada integritas, kedisiplinan, kerjasama, dan rasa hormat yang ditunjukkan murid.
                2. Sampaikan dengan santun, apresiatif terhadap sikap positif, dan berikan arahan lembut untuk kedisiplinan/sikap yang perlu ditingkatkan.
                3. Jangan sertakan teks instruksi, langsung berupa hasil ulasan jadi.
            """.trimIndent()
            
            else -> """
                Tulis ulasan perkembangan fisik dan aktivitas ekstrakurikuler serta kehadiran untuk murid bernama "$studentName" dalam Bahasa Indonesia sepanjang 1-2 paragraf padat.
                Detail kegiatan fisik & ekstra:
                $additionalInfo
                Catatan aktivitas: "$notes"
                
                Aturan penulisan:
                1. Berikan apresiasi terhadap partisipasi murid dalam kegiatan ekstrakurikuler dan keterlibatan fisiknya di sekolah.
                2. Hubungkan data kehadiran dengan kedisiplinan belajarnya secara santun dan profesional.
                3. Jangan merujuk instruksi, langsung berupa ulasan jadi.
            """.trimIndent()
        }

        val systemInstructionText = """
            Anda adalah seorang Guru senior di sekolah dasar/menengah yang ahli menulis narasi rapor murid (student performance evaluation).
            Gaya tulisan Anda sangat bijak, objektif, memicu semangat belajar siswa, dan penuh dedikasi. Anda selalu menulis dalam Bahasa Indonesia yang baik dan benar (formal).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
            generationConfig = GenerationConfig(temperature = 0.6f, maxOutputTokens = 1000)
        )

        return try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Gagal menghasilkan ulasan. Respon kosong."
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateReportSection: ", e)
            "Gagal menghasilkan laporan: ${e.localizedMessage ?: "Koneksi bermasalah"}. Periksa kunci API Anda."
        }
    }
}
