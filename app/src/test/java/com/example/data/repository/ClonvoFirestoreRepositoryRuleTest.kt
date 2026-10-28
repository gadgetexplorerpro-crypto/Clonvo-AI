package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClonvoFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createVoiceProfile_validConsent_createsAndReturnsVoice() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ClonvoFirestoreRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVoiceProfile(
                name = "Alice Studio Clone",
                elevenLabsVoiceId = "el_voice_alice_01",
                description = "Warm natural voice clone",
                consentStatement = "I am Alice and I explicitly consent to cloning my voice.",
                consentVerified = true,
                sampleDurationSec = 25.0,
                clarityScore = 92.5,
                noiseDb = -49.0,
                fundamentalFreqHz = 205.0,
                status = "ready"
            )
        }
        assertTrue("Expected createVoiceProfile to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val voiceId = createResult.getOrThrow()

        val voicesResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserVoices() }
        assertTrue(voicesResult.isSuccess)
        assertTrue(voicesResult.getOrThrow().any { it.id == voiceId })
    }

    @Test
    fun observeUserVoices_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ClonvoFirestoreRepository(firestore, auth)
        val voiceId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVoiceProfile(
                name = "Realtime Voice",
                elevenLabsVoiceId = "el_voice_rt",
                description = "Realtime test",
                consentStatement = "I am Alice and I consent to voice synthesis.",
                consentVerified = true,
                sampleDurationSec = 30.0,
                clarityScore = 90.0,
                noiseDb = -46.0,
                fundamentalFreqHz = 198.0
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserVoices().first { list -> list.any { it.id == voiceId } }
        }
        assertTrue(emitted.any { it.id == voiceId })
    }

    @Test
    fun getVoiceById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ClonvoFirestoreRepository(firestore, auth)
        val voiceId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createVoiceProfile(
                name = "Alice Private Voice",
                elevenLabsVoiceId = "el_voice_priv",
                description = "Private clone",
                consentStatement = "I am Alice and I consent to cloning my voice.",
                consentVerified = true,
                sampleDurationSec = 22.0,
                clarityScore = 89.0,
                noiseDb = -45.0,
                fundamentalFreqHz = 212.0
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ClonvoFirestoreRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getVoiceById(voiceId) }
        assertTrue("Expected cross-user access to fail", result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeUserVoices_unauthenticated_fails() = runBlocking {
        auth.signOut()
        val repository = ClonvoFirestoreRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserVoices().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull("Expected unauthenticated observation to throw", caught)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_clonvo@test.com"
        const val BOB_EMAIL = "bob_clonvo@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
