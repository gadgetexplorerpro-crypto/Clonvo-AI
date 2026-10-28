package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val planTier: String = "free_trial",
    val creditsRemaining: Long = 2500L,
    val creditsUsed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VoiceProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val elevenLabsVoiceId: String = "",
    val description: String = "",
    val consentStatement: String = "",
    val consentVerified: Boolean = false,
    val sampleDurationSec: Double = 0.0,
    val clarityScore: Double = 0.0,
    val noiseDb: Double = -45.0,
    val fundamentalFreqHz: Double = 150.0,
    val status: String = "ready",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VoiceGeneration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val voiceId: String = "",
    val voiceName: String = "",
    val mode: String = "tts",
    val inputText: String = "",
    val modelId: String = "eleven_multilingual_v2",
    val languageCode: String = "en",
    val stability: Double = 0.65,
    val similarityBoost: Double = 0.85,
    val styleExaggeration: Double = 0.15,
    val speed: Double = 1.0,
    val localAudioPath: String = "",
    val measuredF0Hz: Double = 150.0,
    val measuredSimilarityScore: Double = 0.0,
    val creditsCharged: Long = 0L,
    val isFavorite: Boolean = false,
    val aiDisclosureLabeled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AbuseReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetVoiceId: String = "",
    val reason: String = "",
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AudioAnalysisReport(
    val durationSec: Double,
    val clarityScore: Double,
    val noiseFloorDb: Double,
    val rmsDb: Double,
    val fundamentalFreqHz: Double,
    val f0MinHz: Double,
    val f0MaxHz: Double,
    val pitchStabilityPercent: Double,
    val isDurationValid: Boolean,
    val qualityAdvice: String,
    val waveformAmplitudes: List<Float>
)

data class SimilarityComparisonResult(
    val referenceF0Hz: Double,
    val generatedF0Hz: Double,
    val f0DeltaHz: Double,
    val pitchContourCorrelation: Double,
    val spectralEnvelopeMatchPercent: Double,
    val overallAcousticSimilarityPercent: Double,
    val explanation: String
)

enum class SubscriptionPlan(
    val tierId: String,
    val title: String,
    val priceLabel: String,
    val monthlyCredits: Long,
    val googlePlayProductId: String,
    val features: List<String>
) {
    FREE_TRIAL(
        tierId = "free_trial",
        title = "Free Trial",
        priceLabel = "$0.00 / trial",
        monthlyCredits = 2500L,
        googlePlayProductId = "clonvo_free_trial",
        features = listOf(
            "Up to 2,500 character credits",
            "1 Instant Voice Clone profile",
            "Standard MP3 export with AI label",
            "Real-time F0 & acoustic analysis"
        )
    ),
    STARTER(
        tierId = "starter",
        title = "Starter Plan",
        priceLabel = "$4.99 / month",
        monthlyCredits = 30000L,
        googlePlayProductId = "clonvo_starter_monthly",
        features = listOf(
            "30,000 monthly character credits",
            "Up to 5 Instant Voice Clone profiles",
            "Multilingual TTS & Speech-to-Speech",
            "MP3 & lossless WAV Studio exports"
        )
    ),
    PRO(
        tierId = "pro",
        title = "Pro Studio",
        priceLabel = "$9.99 / month",
        monthlyCredits = 100000L,
        googlePlayProductId = "clonvo_pro_monthly",
        features = listOf(
            "100,000 monthly character credits",
            "Up to 20 Instant Voice Clone profiles",
            "Full pitch, speed & expression controls",
            "Deep F0 & spectral similarity analysis"
        )
    ),
    BUSINESS(
        tierId = "business",
        title = "Business Studio",
        priceLabel = "$24.99 / month",
        monthlyCredits = 350000L,
        googlePlayProductId = "clonvo_business_monthly",
        features = listOf(
            "350,000 monthly character credits",
            "Unlimited reusable voice profiles",
            "Priority ElevenLabs v2 synthesis",
            "Commercial voice governance & audit logs"
        )
    )
}
