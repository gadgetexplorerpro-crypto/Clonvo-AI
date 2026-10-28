package com.example.data.remote

import com.example.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ElevenLabsCloneResponse(
    val voiceId: String,
    val requiresVerification: Boolean
)

data class RemoteVoiceItem(
    val voiceId: String,
    val name: String,
    val category: String
)

class ElevenLabsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()
) {
    fun getConfiguredApiKey(): String {
        val raw = try {
            BuildConfig.ELEVENLABS_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (raw.isBlank() || raw == "YOUR_ELEVENLABS_API_KEY_HERE" || raw == "DEFAULT_VALUE") {
            ""
        } else {
            raw.trim()
        }
    }

    fun isApiKeyConfigured(): Boolean = getConfiguredApiKey().isNotEmpty()

    private fun requireApiKey(): String {
        val key = getConfiguredApiKey()
        if (key.isEmpty()) {
            throw IllegalStateException(
                "ElevenLabs API key is not configured. Open the Secrets panel in AI Studio and set ELEVENLABS_API_KEY to enable real ElevenLabs voice cloning and speech synthesis."
            )
        }
        return key
    }

    suspend fun cloneVoice(
        name: String,
        description: String,
        consentStatement: String,
        sampleFile: File
    ): Result<ElevenLabsCloneResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            require(sampleFile.exists() && sampleFile.length() > 1024L) {
                "Valid audio recording file is required for voice cloning."
            }
            val labelsJson = JSONObject().apply {
                put("consent_verified", "true")
                put("consent_phrase", consentStatement.take(120))
                put("source", "clonvo_ai_studio")
            }.toString()

            val mediaType = if (sampleFile.name.endsWith(".mp3", ignoreCase = true)) {
                "audio/mpeg".toMediaType()
            } else {
                "audio/wav".toMediaType()
            }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name.trim())
                .addFormDataPart("description", description.trim().ifBlank { "Authorized voice clone created in Clonvo AI" })
                .addFormDataPart("labels", labelsJson)
                .addFormDataPart(
                    "files",
                    sampleFile.name,
                    sampleFile.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/v1/voices/add")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val providerDetail = parseProviderError(response.code, responseText)
                    throw IllegalStateException(providerDetail)
                }
                val json = JSONObject(responseText)
                val voiceId = json.optString("voice_id")
                if (voiceId.isBlank()) {
                    throw IllegalStateException("ElevenLabs response did not contain a valid voice_id.")
                }
                val requiresVerification = json.optBoolean("requires_verification", false)
                ElevenLabsCloneResponse(
                    voiceId = voiceId,
                    requiresVerification = requiresVerification
                )
            }
        }
    }

    suspend fun generateSpeechToFile(
        elevenLabsVoiceId: String,
        text: String,
        modelId: String,
        languageCode: String,
        stability: Double,
        similarityBoost: Double,
        styleExaggeration: Double,
        speed: Double,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            require(text.isNotBlank()) { "Input text cannot be empty." }
            require(elevenLabsVoiceId.isNotBlank()) { "ElevenLabs voice ID is missing." }

            val voiceSettings = JSONObject().apply {
                put("stability", stability.coerceIn(0.0, 1.0))
                put("similarity_boost", similarityBoost.coerceIn(0.0, 1.0))
                put("style", styleExaggeration.coerceIn(0.0, 1.0))
                put("use_speaker_boost", true)
                put("speed", speed.coerceIn(0.7, 1.2))
            }

            val payload = JSONObject().apply {
                put("text", text.trim())
                put("model_id", modelId)
                if (modelId.contains("multilingual") || modelId.contains("turbo")) {
                    put("language_code", languageCode)
                }
                put("voice_settings", voiceSettings)
            }

            val request = Request.Builder()
                .url("$BASE_URL/v1/text-to-speech/$elevenLabsVoiceId?output_format=mp3_44100_128")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    throw IllegalStateException(parseProviderError(response.code, errBody))
                }
                val bytes = response.body?.bytes()
                    ?: throw IllegalStateException("Empty audio stream returned by ElevenLabs TTS.")
                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(bytes)
                outputFile
            }
        }
    }

    suspend fun convertSpeechToSpeech(
        elevenLabsVoiceId: String,
        sourceAudioFile: File,
        stability: Double,
        similarityBoost: Double,
        styleExaggeration: Double,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            require(sourceAudioFile.exists() && sourceAudioFile.length() > 512L) {
                "Source speech recording file is missing or empty."
            }

            val voiceSettingsJson = JSONObject().apply {
                put("stability", stability.coerceIn(0.0, 1.0))
                put("similarity_boost", similarityBoost.coerceIn(0.0, 1.0))
                put("style", styleExaggeration.coerceIn(0.0, 1.0))
                put("use_speaker_boost", true)
            }.toString()

            val mediaType = if (sourceAudioFile.name.endsWith(".mp3", ignoreCase = true)) {
                "audio/mpeg".toMediaType()
            } else {
                "audio/wav".toMediaType()
            }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model_id", "eleven_multilingual_sts_v2")
                .addFormDataPart("voice_settings", voiceSettingsJson)
                .addFormDataPart(
                    "audio",
                    sourceAudioFile.name,
                    sourceAudioFile.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/v1/speech-to-speech/$elevenLabsVoiceId?output_format=mp3_44100_128")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    throw IllegalStateException(parseProviderError(response.code, errBody))
                }
                val bytes = response.body?.bytes()
                    ?: throw IllegalStateException("Empty audio response from ElevenLabs Speech-to-Speech API.")
                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(bytes)
                outputFile
            }
        }
    }

    suspend fun fetchRemoteVoices(): Result<List<RemoteVoiceItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            val request = Request.Builder()
                .url("$BASE_URL/v1/voices")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(parseProviderError(response.code, bodyStr))
                }
                val json = JSONObject(bodyStr)
                val arr = json.optJSONArray("voices") ?: JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            RemoteVoiceItem(
                                voiceId = obj.optString("voice_id"),
                                name = obj.optString("name", "Voice"),
                                category = obj.optString("category", "cloned")
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun deleteRemoteVoice(elevenLabsVoiceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = requireApiKey()
            val request = Request.Builder()
                .url("$BASE_URL/v1/voices/$elevenLabsVoiceId")
                .addHeader("xi-api-key", apiKey)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    val err = response.body?.string().orEmpty()
                    throw IllegalStateException(parseProviderError(response.code, err))
                }
            }
        }
    }

    private fun parseProviderError(httpCode: Int, responseBody: String): String {
        val parsedMessage = try {
            val root = JSONObject(responseBody)
            when {
                root.has("detail") -> {
                    val detail = root.get("detail")
                    if (detail is JSONObject) {
                        val status = detail.optString("status")
                        val message = detail.optString("message")
                        if (status.isNotEmpty()) "$status: $message" else message
                    } else {
                        detail.toString()
                    }
                }
                root.has("error") -> root.optString("error")
                else -> responseBody.take(200)
            }
        } catch (e: Exception) {
            responseBody.take(200)
        }
        return "ElevenLabs API Error (HTTP $httpCode): ${parsedMessage.ifBlank { "Request rejected by provider" }}"
    }

    companion object {
        private const val BASE_URL = "https://api.elevenlabs.io"
    }
}
