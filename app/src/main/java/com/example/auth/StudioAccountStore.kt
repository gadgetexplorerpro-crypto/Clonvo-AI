package com.example.auth

import android.content.Context
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGeneration
import com.example.data.model.VoiceProfile
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class LocalSessionUser(
    val uid: String,
    val displayName: String,
    val email: String
)

class StudioAccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeSession = MutableStateFlow(loadSavedSession())
    val activeSession: StateFlow<LocalSessionUser?> = _activeSession.asStateFlow()

    private fun loadSavedSession(): LocalSessionUser? {
        val uid = prefs.getString(KEY_ACTIVE_UID, null) ?: return null
        val name = prefs.getString(KEY_ACTIVE_NAME, "Clonvo Creator") ?: "Clonvo Creator"
        val email = prefs.getString(KEY_ACTIVE_EMAIL, "") ?: ""
        if (email.isBlank()) return null
        return LocalSessionUser(uid = uid, displayName = name, email = email)
    }

    fun registerAccount(
        displayName: String,
        email: String,
        passwordPlain: String
    ): Result<LocalSessionUser> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail.contains("@") && normalizedEmail.contains(".")) {
            "Please enter a valid Gmail or email address (e.g. yourname@gmail.com)."
        }
        require(passwordPlain.length >= 6) {
            "Password must be at least 6 characters long."
        }
        val safeName = displayName.trim().ifBlank {
            normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

        val accountsJson = JSONObject(prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}")
        if (accountsJson.has(normalizedEmail)) {
            throw IllegalStateException("An account with $normalizedEmail already exists. Please switch to Login.")
        }

        val uid = "usr_${sha256(normalizedEmail).take(20)}"
        val passwordHash = sha256("$normalizedEmail:$passwordPlain")
        val userObj = JSONObject().apply {
            put("uid", uid)
            put("displayName", safeName)
            put("email", normalizedEmail)
            put("passwordHash", passwordHash)
        }
        accountsJson.put(normalizedEmail, userObj)

        prefs.edit()
            .putString(KEY_ACCOUNTS_MAP, accountsJson.toString())
            .putString(KEY_ACTIVE_UID, uid)
            .putString(KEY_ACTIVE_NAME, safeName)
            .putString(KEY_ACTIVE_EMAIL, normalizedEmail)
            .apply()

        ensureLocalProfile(uid, safeName, normalizedEmail)
        val session = LocalSessionUser(uid = uid, displayName = safeName, email = normalizedEmail)
        _activeSession.value = session
        session
    }

    fun loginWithPassword(
        email: String,
        passwordPlain: String
    ): Result<LocalSessionUser> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail.contains("@") && normalizedEmail.contains(".")) {
            "Please enter a valid Gmail or email address."
        }
        require(passwordPlain.isNotEmpty()) {
            "Please enter your password."
        }

        val accountsJson = JSONObject(prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}")
        if (!accountsJson.has(normalizedEmail)) {
            throw IllegalStateException("No account found for $normalizedEmail. Tap 'Sign Up' to create your account.")
        }

        val userObj = accountsJson.getJSONObject(normalizedEmail)
        val expectedHash = userObj.optString("passwordHash")
        val actualHash = sha256("$normalizedEmail:$passwordPlain")
        if (expectedHash != actualHash) {
            throw IllegalStateException("Incorrect password for $normalizedEmail. Please try again.")
        }

        val uid = userObj.optString("uid")
        val displayName = userObj.optString("displayName", "Clonvo Creator")

        prefs.edit()
            .putString(KEY_ACTIVE_UID, uid)
            .putString(KEY_ACTIVE_NAME, displayName)
            .putString(KEY_ACTIVE_EMAIL, normalizedEmail)
            .apply()

        ensureLocalProfile(uid, displayName, normalizedEmail)
        val session = LocalSessionUser(uid = uid, displayName = displayName, email = normalizedEmail)
        _activeSession.value = session
        session
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_UID)
            .remove(KEY_ACTIVE_NAME)
            .remove(KEY_ACTIVE_EMAIL)
            .apply()
        _activeSession.value = null
    }

    fun deleteAccount(uid: String) {
        val email = _activeSession.value?.email?.lowercase()
        if (email != null) {
            val accountsJson = JSONObject(prefs.getString(KEY_ACCOUNTS_MAP, "{}") ?: "{}")
            accountsJson.remove(email)
            prefs.edit().putString(KEY_ACCOUNTS_MAP, accountsJson.toString()).apply()
        }
        prefs.edit()
            .remove("profile_$uid")
            .remove("voices_$uid")
            .remove("generations_$uid")
            .apply()
        clearSession()
    }

    fun ensureLocalProfile(uid: String, displayName: String, email: String): UserProfile {
        val key = "profile_$uid"
        val existingRaw = prefs.getString(key, null)
        if (existingRaw != null) {
            return runCatching {
                val obj = JSONObject(existingRaw)
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = obj.optString("displayName", displayName),
                    email = obj.optString("email", email),
                    planTier = obj.optString("planTier", "free_trial"),
                    creditsRemaining = obj.optLong("creditsRemaining", 2500L),
                    creditsUsed = obj.optLong("creditsUsed", 0L)
                )
            }.getOrDefault(
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    planTier = "free_trial",
                    creditsRemaining = 2500L,
                    creditsUsed = 0L
                )
            )
        }
        val created = UserProfile(
            id = uid,
            userId = uid,
            displayName = displayName,
            email = email,
            planTier = "free_trial",
            creditsRemaining = 2500L,
            creditsUsed = 0L
        )
        saveLocalProfile(created)
        return created
    }

    fun saveLocalProfile(profile: UserProfile) {
        val obj = JSONObject().apply {
            put("userId", profile.userId)
            put("displayName", profile.displayName)
            put("email", profile.email)
            put("planTier", profile.planTier)
            put("creditsRemaining", profile.creditsRemaining)
            put("creditsUsed", profile.creditsUsed)
        }
        prefs.edit().putString("profile_${profile.userId}", obj.toString()).apply()
    }

    fun loadLocalVoices(uid: String): List<VoiceProfile> {
        val raw = prefs.getString("voices_$uid", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VoiceProfile(
                            id = o.optString("id"),
                            userId = uid,
                            name = o.optString("name"),
                            elevenLabsVoiceId = o.optString("elevenLabsVoiceId"),
                            description = o.optString("description"),
                            consentStatement = o.optString("consentStatement"),
                            consentVerified = o.optBoolean("consentVerified", true),
                            sampleDurationSec = o.optDouble("sampleDurationSec", 20.0),
                            clarityScore = o.optDouble("clarityScore", 90.0),
                            noiseDb = o.optDouble("noiseDb", -48.0),
                            fundamentalFreqHz = o.optDouble("fundamentalFreqHz", 160.0),
                            status = o.optString("status", "ready")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveLocalVoices(uid: String, voices: List<VoiceProfile>) {
        val arr = JSONArray()
        voices.forEach { v ->
            arr.put(
                JSONObject().apply {
                    put("id", v.id)
                    put("userId", v.userId)
                    put("name", v.name)
                    put("elevenLabsVoiceId", v.elevenLabsVoiceId)
                    put("description", v.description)
                    put("consentStatement", v.consentStatement)
                    put("consentVerified", v.consentVerified)
                    put("sampleDurationSec", v.sampleDurationSec)
                    put("clarityScore", v.clarityScore)
                    put("noiseDb", v.noiseDb)
                    put("fundamentalFreqHz", v.fundamentalFreqHz)
                    put("status", v.status)
                }
            )
        }
        prefs.edit().putString("voices_$uid", arr.toString()).apply()
    }

    fun loadLocalGenerations(uid: String): List<VoiceGeneration> {
        val raw = prefs.getString("generations_$uid", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VoiceGeneration(
                            id = o.optString("id"),
                            userId = uid,
                            voiceId = o.optString("voiceId"),
                            voiceName = o.optString("voiceName"),
                            mode = o.optString("mode", "tts"),
                            inputText = o.optString("inputText"),
                            modelId = o.optString("modelId", "eleven_multilingual_v2"),
                            languageCode = o.optString("languageCode", "en"),
                            stability = o.optDouble("stability", 0.65),
                            similarityBoost = o.optDouble("similarityBoost", 0.85),
                            styleExaggeration = o.optDouble("styleExaggeration", 0.15),
                            speed = o.optDouble("speed", 1.0),
                            localAudioPath = o.optString("localAudioPath"),
                            measuredF0Hz = o.optDouble("measuredF0Hz", 160.0),
                            measuredSimilarityScore = o.optDouble("measuredSimilarityScore", 90.0),
                            creditsCharged = o.optLong("creditsCharged", 10L),
                            isFavorite = o.optBoolean("isFavorite", false),
                            aiDisclosureLabeled = true
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveLocalGenerations(uid: String, generations: List<VoiceGeneration>) {
        val arr = JSONArray()
        generations.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("userId", g.userId)
                    put("voiceId", g.voiceId)
                    put("voiceName", g.voiceName)
                    put("mode", g.mode)
                    put("inputText", g.inputText)
                    put("modelId", g.modelId)
                    put("languageCode", g.languageCode)
                    put("stability", g.stability)
                    put("similarityBoost", g.similarityBoost)
                    put("styleExaggeration", g.styleExaggeration)
                    put("speed", g.speed)
                    put("localAudioPath", g.localAudioPath)
                    put("measuredF0Hz", g.measuredF0Hz)
                    put("measuredSimilarityScore", g.measuredSimilarityScore)
                    put("creditsCharged", g.creditsCharged)
                    put("isFavorite", g.isFavorite)
                    put("aiDisclosureLabeled", true)
                }
            )
        }
        prefs.edit().putString("generations_$uid", arr.toString()).apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_NAME = "clonvo_studio_accounts"
        const val KEY_ACCOUNTS_MAP = "accounts_map_json"
        const val KEY_ACTIVE_UID = "active_uid"
        const val KEY_ACTIVE_NAME = "active_name"
        const val KEY_ACTIVE_EMAIL = "active_email"
    }
}
