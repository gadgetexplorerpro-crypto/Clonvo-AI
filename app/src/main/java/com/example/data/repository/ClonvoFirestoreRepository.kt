package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AbuseReport
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGeneration
import com.example.data.model.VoiceProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ClonvoFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) return@runCatching existing
            }
            val safeName = displayName.trim().ifBlank { "Clonvo Creator" }.take(80)
            val safeEmail = email.trim().ifBlank { "creator@clonvo.ai" }.take(120)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "email" to safeEmail,
                "planTier" to "free_trial",
                "creditsRemaining" to 2500L,
                "creditsUsed" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val createdSnap = docRef.get().await()
            createdSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                email = safeEmail,
                planTier = "free_trial",
                creditsRemaining = 2500L,
                creditsUsed = 0L
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deductCredits(creditsToDeduct: Long): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(creditsToDeduct >= 0L) { "Credits to deduct must be non-negative" }
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            val currentRemaining = snapshot.getLong("creditsRemaining") ?: 0L
            val currentUsed = snapshot.getLong("creditsUsed") ?: 0L
            if (currentRemaining < creditsToDeduct) {
                throw IllegalStateException(
                    "Insufficient credits: need $creditsToDeduct credits, but only $currentRemaining remaining."
                )
            }
            docRef.update(
                mapOf(
                    "creditsRemaining" to (currentRemaining - creditsToDeduct),
                    "creditsUsed" to (currentUsed + creditsToDeduct),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeUserVoices(): Flow<List<VoiceProfile>> = flow {
        val uid = requireUserId()
        val path = "voices"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VoiceProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserVoices(): Result<List<VoiceProfile>> = runCatching {
        val uid = requireUserId()
        val path = "voices"
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                VoiceProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getVoiceById(voiceId: String): Result<VoiceProfile> = runCatching {
        val docRef = db.collection("voices").document(voiceId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(
                VoiceProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Voice profile not found: $voiceId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createVoiceProfile(
        name: String,
        elevenLabsVoiceId: String,
        description: String,
        consentStatement: String,
        consentVerified: Boolean,
        sampleDurationSec: Double,
        clarityScore: Double,
        noiseDb: Double,
        fundamentalFreqHz: Double,
        status: String = "ready"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val voiceId = "voice_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("voices").document(voiceId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(60),
            "elevenLabsVoiceId" to elevenLabsVoiceId.trim().take(120),
            "description" to description.trim().take(300),
            "consentStatement" to consentStatement.trim().take(500),
            "consentVerified" to consentVerified,
            "sampleDurationSec" to sampleDurationSec.coerceIn(15.0, 300.0),
            "clarityScore" to clarityScore.coerceIn(0.0, 100.0),
            "noiseDb" to noiseDb.coerceIn(-120.0, 0.0),
            "fundamentalFreqHz" to fundamentalFreqHz.coerceIn(50.0, 500.0),
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            voiceId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteVoiceProfile(voiceId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("voices").document(voiceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeGenerations(): Flow<List<VoiceGeneration>> = flow {
        val uid = requireUserId()
        val path = "generations"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VoiceGeneration::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createGenerationRecord(
        voiceId: String,
        voiceName: String,
        mode: String,
        inputText: String,
        modelId: String,
        languageCode: String,
        stability: Double,
        similarityBoost: Double,
        styleExaggeration: Double,
        speed: Double,
        localAudioPath: String,
        measuredF0Hz: Double,
        measuredSimilarityScore: Double,
        creditsCharged: Long
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val genId = "gen_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("generations").document(genId)
        val payload = mapOf(
            "userId" to uid,
            "voiceId" to voiceId.trim().take(120),
            "voiceName" to voiceName.trim().take(60),
            "mode" to mode,
            "inputText" to inputText.trim().take(2500),
            "modelId" to modelId.trim().take(80),
            "languageCode" to languageCode.trim().take(16),
            "stability" to stability.coerceIn(0.0, 1.0),
            "similarityBoost" to similarityBoost.coerceIn(0.0, 1.0),
            "styleExaggeration" to styleExaggeration.coerceIn(0.0, 1.0),
            "speed" to speed.coerceIn(0.5, 2.0),
            "localAudioPath" to localAudioPath.take(500),
            "measuredF0Hz" to measuredF0Hz.coerceIn(0.0, 600.0),
            "measuredSimilarityScore" to measuredSimilarityScore.coerceIn(0.0, 100.0),
            "creditsCharged" to creditsCharged.coerceIn(0L, 10000L),
            "isFavorite" to false,
            "aiDisclosureLabeled" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            genId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavoriteGeneration(
        generationId: String,
        isFavorite: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("generations").document(generationId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGeneration(generationId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("generations").document(generationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun submitAbuseReport(
        targetVoiceId: String,
        reason: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("abuse_reports").document(reportId)
        val payload = mapOf(
            "userId" to uid,
            "targetVoiceId" to targetVoiceId.trim().ifBlank { "general_report" }.take(120),
            "reason" to reason.trim().take(500),
            "status" to "submitted",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            reportId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val voices = db.collection("voices").whereEqualTo("userId", uid).get().await()
        for (doc in voices.documents) {
            doc.reference.delete().await()
        }
        val generations = db.collection("generations").whereEqualTo("userId", uid).get().await()
        for (doc in generations.documents) {
            doc.reference.delete().await()
        }
        val userDoc = db.collection("users").document(uid).get().await()
        if (userDoc.exists()) {
            userDoc.reference.delete().await()
        }
    }
}
