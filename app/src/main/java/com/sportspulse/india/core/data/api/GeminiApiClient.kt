package com.sportspulse.india.core.data.api

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.sportspulse.india.core.domain.entity.Broadcast
import com.sportspulse.india.core.domain.entity.BroadcastPlatform
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiClient @Inject constructor() {

    companion object {
        // Toggle this flag to switch between Vertex AI and Google Developer API backends.
        // Vertex AI is recommended for production apps with Firebase Blaze plan.
        const val USE_VERTEX_AI = true
    }

    private val gson = Gson()

    private val flashModel: GenerativeModel by lazy {
        Firebase.ai(
            backend = if (USE_VERTEX_AI) GenerativeBackend.vertexAI() else GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature      = 0.7f
                topK             = 40
                topP             = 0.95f
                maxOutputTokens  = 512
            }
        )
    }

    private val proModel: GenerativeModel by lazy {
        Firebase.ai(
            backend = if (USE_VERTEX_AI) GenerativeBackend.vertexAI() else GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature      = 0.8f
                topK             = 40
                topP             = 0.95f
                maxOutputTokens  = 1024
            }
        )
    }

    suspend fun generateMatchPreview(
        eventTitle: String,
        competition: String,
        homeTeam: String,
        awayTeam: String,
        venue: String,
        status: String,
        scoreOrTime: String
    ): String? = runCatching {
        val prompt = buildMatchPreviewPrompt(
            eventTitle, competition, homeTeam, awayTeam, venue, status, scoreOrTime
        )
        val response = proModel.generateContent(
            content { text(prompt) }
        )
        response.text?.trim()
    }.onFailure { e ->
        Timber.e(e, "GeminiApiClient: match preview failed for $eventTitle")
    }.getOrNull()

    suspend fun streamMatchAnalysis(
        eventTitle: String,
        competition: String,
        venue: String,
        onToken: (String) -> Unit
    ) {
        val prompt = """
            You are a passionate Indian sports commentator. Write a detailed 150-200 word 
            match analysis for: "$eventTitle" ($competition) at $venue.
            
            Include:
            - Historical rivalry between teams
            - Key players to watch
            - Venue conditions and pitch/surface analysis (if applicable)
            - Your prediction for the outcome
            
            Write in an engaging, enthusiastic tone for Indian fans.
        """.trimIndent()

        runCatching {
            proModel.generateContentStream(
                content { text(prompt) }
            ).collect { chunk ->
                chunk.text?.let { token -> onToken(token) }
            }
        }.onFailure { e ->
            Timber.e(e, "GeminiApiClient: stream analysis failed for $eventTitle")
        }
    }

    suspend fun inferBroadcasts(eventTitle: String, sport: String): List<Broadcast> = runCatching {
        val prompt = buildBroadcastInferencePrompt(eventTitle, sport)
        val response = flashModel.generateContent(
            content { text(prompt) }
        )
        val rawText = response.text ?: return@runCatching emptyList()
        parseBroadcastJson(rawText)
    }.onFailure { e ->
        Timber.e(e, "GeminiApiClient: broadcast inference failed for $eventTitle")
    }.getOrDefault(emptyList())

    suspend fun enrichEventDescription(eventTitle: String, competition: String): String? =
        runCatching {
            val prompt = """
                In exactly 1-2 sentences, describe what "$eventTitle" is, in the context of 
                the "$competition" competition. Focus on the significance of this match for 
                Indian sports fans. Be concise and factual.
            """.trimIndent()
            flashModel.generateContent(content { text(prompt) }).text?.trim()
        }.onFailure { e ->
            Timber.e(e, "GeminiApiClient: enrichment failed for $eventTitle")
        }.getOrNull()

    private fun buildMatchPreviewPrompt(
        eventTitle: String,
        competition: String,
        homeTeam: String,
        awayTeam: String,
        venue: String,
        status: String,
        scoreOrTime: String
    ): String = """
        You are a knowledgeable Indian sports analyst. Write a 3-5 sentence match 
        preview/analysis for this event:
        
        Event: $eventTitle
        Competition: $competition
        Home Team: $homeTeam
        Away Team: $awayTeam
        Venue: $venue
        Status: $status
        Score/Time: $scoreOrTime
        
        Rules:
        - Be factual but engaging
        - Mention key players if relevant to Indian audiences
        - Include historical context briefly
        - End with a likely outcome or what to watch for
        - Do NOT use markdown formatting
        - Write in plain paragraph form
    """.trimIndent()

    private fun buildBroadcastInferencePrompt(eventTitle: String, sport: String): String = """
        Based on your knowledge of Indian sports broadcasting rights as of 2024-2025, 
        return a JSON array of broadcast slots for this event:
        
        Event: "$eventTitle"
        Sport: $sport
        
        Return ONLY valid JSON (no markdown, no explanation) in this exact format:
        [
          {
            "channelName": "Star Sports 1 HD",
            "channelNumber": 451,
            "platform": "JIOSTAR",
            "isFree": false,
            "deeplinkUri": "hotstar://sports",
            "streamingUrl": "https://www.hotstar.com/in/sports"
          }
        ]
        
        Platform must be one of: JIOSTAR, ZEE5, FANCODE, DD_SPORTS, SONY_LIV
        channelNumber is optional (null if unknown)
        deeplinkUri and streamingUrl are optional (null if unknown)
        Include DD Sports (platform: DD_SPORTS, isFree: true) if the event is typically free-to-air.
        Return an empty array [] if you cannot determine the broadcaster with confidence.
    """.trimIndent()

    private fun parseBroadcastJson(raw: String): List<Broadcast> {
        val jsonStr = extractJsonArray(raw)
        val type = object : TypeToken<List<BroadcastJson>>() {}.type
        val items = runCatching<List<BroadcastJson>> {
            gson.fromJson(jsonStr, type)
        }.getOrDefault(emptyList())

        return items.mapNotNull { it.toDomain() }
    }

    private fun extractJsonArray(text: String): String {
        val start = text.indexOf('[')
        val end   = text.lastIndexOf(']')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else "[]"
    }

    private data class BroadcastJson(
        val channelName: String?,
        val channelNumber: Int?,
        val platform: String?,
        val isFree: Boolean?,
        val deeplinkUri: String?,
        val streamingUrl: String?
    ) {
        fun toDomain(): Broadcast? {
            val name = channelName ?: return null
            val plat = BroadcastPlatform.fromString(platform ?: "UNKNOWN")
            return Broadcast(
                channelName   = name,
                channelNumber = channelNumber,
                platform      = plat,
                isFree        = isFree ?: plat.isFreeByDefault,
                deeplinkUri   = deeplinkUri ?: plat.defaultDeeplink,
                streamingUrl  = streamingUrl
            )
        }
    }
}
