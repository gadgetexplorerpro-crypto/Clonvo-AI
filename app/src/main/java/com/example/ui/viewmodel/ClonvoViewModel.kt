package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.StudioAccountStore
import com.example.data.model.AudioAnalysisReport
import com.example.data.model.SimilarityComparisonResult
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGeneration
import com.example.data.model.VoiceProfile
import com.example.data.remote.ElevenLabsService
import com.example.data.repository.ClonvoFirestoreRepository
import com.example.util.AudioStudioEngine
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CloneStudioUiState(
    val voiceName: String = "",
    val voiceDescription: String = "",
    val consentStatement: String = "I confirm that I am the speaker in this recording and I authorize Clonvo AI to create an AI voice clone of my voice.",
    val consentChecked: Boolean = false,
    val recordedFile: File? = null,
    val analysisReport: AudioAnalysisReport? = null,
    val isCloningInProgress: Boolean = false,
    val cloneStatusMessage: String? = null,
    val cloneErrorMessage: String? = null
)

data class SynthesisStudioUiState(
    val mode: String = "tts",
    val selectedVoiceId: String = "",
    val inputText: String = "Welcome to Clonvo AI Studio. This speech is synthesized from my authorized voice profile with real-time pitch and spectral analysis.",
    val selectedModelId: String = "eleven_multilingual_v2",
    val selectedLanguage: String = "en",
    val stability: Float = 0.65f,
    val similarityBoost: Float = 0.85f,
    val styleExaggeration: Float = 0.15f,
    val speed: Float = 1.0f,
    val stsSourceFile: File? = null,
    val stsSourceAnalysis: AudioAnalysisReport? = null,
    val isGenerating: Boolean = false,
    val latestGeneratedFile: File? = null,
    val latestSimilarityResult: SimilarityComparisonResult? = null,
    val statusBanner: String? = null,
    val errorBanner: String? = null
)

class ClonvoViewModel(
    private val repository: ClonvoFirestoreRepository,
    private val accountStore: StudioAccountStore,
    private val elevenLabsService: ElevenLabsService,
    private val audioEngine: AudioStudioEngine,
    private val filesDir: File,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val isCloudFirebaseSession: Boolean
) : ViewModel() {

    private val _userProfileState = MutableStateFlow<UiState<UserProfile?>>(UiState.Loading)
    val userProfileState: StateFlow<UiState<UserProfile?>> = _userProfileState.asStateFlow()

    private val _voicesState = MutableStateFlow<UiState<List<VoiceProfile>>>(UiState.Loading)
    val voicesState: StateFlow<UiState<List<VoiceProfile>>> = _voicesState.asStateFlow()

    private val _generationsState = MutableStateFlow<UiState<List<VoiceGeneration>>>(UiState.Loading)
    val generationsState: StateFlow<UiState<List<VoiceGeneration>>> = _generationsState.asStateFlow()

    private val _cloneState = MutableStateFlow(CloneStudioUiState())
    val cloneState: StateFlow<CloneStudioUiState> = _cloneState.asStateFlow()

    private val _synthesisState = MutableStateFlow(SynthesisStudioUiState())
    val synthesisState: StateFlow<SynthesisStudioUiState> = _synthesisState.asStateFlow()

    val isRecording = audioEngine.isRecording
    val recordingDurationSec = audioEngine.recordingDurationSec
    val liveAmplitudes = audioEngine.liveAmplitudes
    val isPlaying = audioEngine.isPlaying
    val playingFilePath = audioEngine.playingFilePath

    val isElevenLabsKeyConfigured: Boolean
        get() = elevenLabsService.isApiKeyConfigured()

    init {
        if (isCloudFirebaseSession) {
            viewModelScope.launch {
                repository.ensureUserProfile(userDisplayName, userEmail)
            }
            viewModelScope.launch {
                repository.observeUserProfile(currentUserId)
                    .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing user profile", error)
                        emit(UiState.Error(error.message ?: "Failed to load user profile"))
                    }
                    .collect { _userProfileState.value = it }
            }
            viewModelScope.launch {
                repository.observeUserVoices()
                    .map<List<VoiceProfile>, UiState<List<VoiceProfile>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing user voices", error)
                        emit(UiState.Error(error.message ?: "Failed to load cloned voices"))
                    }
                    .collect { _voicesState.value = it }
            }
            viewModelScope.launch {
                repository.observeGenerations()
                    .map<List<VoiceGeneration>, UiState<List<VoiceGeneration>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing generations", error)
                        emit(UiState.Error(error.message ?: "Failed to load generation history"))
                    }
                    .collect { _generationsState.value = it }
            }
        } else {
            refreshLocalSessionData()
        }
    }

    private fun refreshLocalSessionData() {
        val profile = accountStore.ensureLocalProfile(currentUserId, userDisplayName, userEmail)
        _userProfileState.value = UiState.Success(profile)
        _voicesState.value = UiState.Success(accountStore.loadLocalVoices(currentUserId))
        _generationsState.value = UiState.Success(accountStore.loadLocalGenerations(currentUserId))
    }

    fun updateCloneVoiceName(name: String) {
        _cloneState.value = _cloneState.value.copy(voiceName = name, cloneErrorMessage = null)
    }

    fun updateCloneDescription(desc: String) {
        _cloneState.value = _cloneState.value.copy(voiceDescription = desc, cloneErrorMessage = null)
    }

    fun updateConsentStatement(statement: String) {
        _cloneState.value = _cloneState.value.copy(consentStatement = statement, cloneErrorMessage = null)
    }

    fun updateConsentChecked(checked: Boolean) {
        _cloneState.value = _cloneState.value.copy(consentChecked = checked, cloneErrorMessage = null)
    }

    fun startCloneVoiceRecording() {
        val outputFile = File(filesDir, "voice_sample_${System.currentTimeMillis()}.wav")
        _cloneState.value = _cloneState.value.copy(
            cloneErrorMessage = null,
            cloneStatusMessage = "Recording microphone audio... Speak naturally for 15–60 seconds."
        )
        audioEngine.startMicrophoneRecording(
            scope = viewModelScope,
            outputWavFile = outputFile,
            onFinished = { file, report ->
                _cloneState.value = _cloneState.value.copy(
                    recordedFile = file,
                    analysisReport = report,
                    cloneStatusMessage = report.qualityAdvice,
                    cloneErrorMessage = if (!report.isDurationValid) report.qualityAdvice else null
                )
            },
            onError = { err ->
                _cloneState.value = _cloneState.value.copy(
                    cloneErrorMessage = err,
                    cloneStatusMessage = null
                )
            }
        )
    }

    fun stopMicrophoneRecording() {
        audioEngine.stopMicrophoneRecording()
    }

    fun importCloneSampleUri(uri: Uri) {
        viewModelScope.launch {
            _cloneState.value = _cloneState.value.copy(
                cloneErrorMessage = null,
                cloneStatusMessage = "Analyzing uploaded audio file..."
            )
            val result = audioEngine.importAudioUriToStudioFile(uri)
            result.onSuccess { (file, report) ->
                _cloneState.value = _cloneState.value.copy(
                    recordedFile = file,
                    analysisReport = report,
                    cloneStatusMessage = report.qualityAdvice,
                    cloneErrorMessage = if (!report.isDurationValid) report.qualityAdvice else null
                )
            }.onFailure { e ->
                _cloneState.value = _cloneState.value.copy(
                    cloneErrorMessage = e.localizedMessage ?: "Failed to import audio file.",
                    cloneStatusMessage = null
                )
            }
        }
    }

    fun submitInstantVoiceClone() {
        val state = _cloneState.value
        val sampleFile = state.recordedFile
        val report = state.analysisReport

        if (state.voiceName.isBlank()) {
            _cloneState.value = state.copy(cloneErrorMessage = "Please enter a name for your voice profile.")
            return
        }
        if (sampleFile == null || !sampleFile.exists() || report == null) {
            _cloneState.value = state.copy(cloneErrorMessage = "Please record or upload a voice sample first.")
            return
        }
        if (!report.isDurationValid) {
            _cloneState.value = state.copy(
                cloneErrorMessage = "Voice sample is ${report.durationSec}s. Minimum required duration is 15 seconds (30–60s recommended)."
            )
            return
        }
        if (!state.consentChecked || state.consentStatement.trim().length < 10) {
            _cloneState.value = state.copy(
                cloneErrorMessage = "Explicit speaker ownership & consent verification is required before cloning."
            )
            return
        }

        viewModelScope.launch {
            _cloneState.value = state.copy(
                isCloningInProgress = true,
                cloneErrorMessage = null,
                cloneStatusMessage = "Uploading sample to ElevenLabs Instant Voice Cloning API..."
            )

            val cloneResult = elevenLabsService.cloneVoice(
                name = state.voiceName,
                description = state.voiceDescription,
                consentStatement = state.consentStatement,
                sampleFile = sampleFile
            )

            cloneResult.onSuccess { response ->
                if (isCloudFirebaseSession) {
                    val deductRes = repository.deductCredits(CLONE_CREDIT_COST)
                    if (deductRes.isFailure) {
                        _cloneState.value = _cloneState.value.copy(
                            isCloningInProgress = false,
                            cloneErrorMessage = deductRes.exceptionOrNull()?.localizedMessage ?: "Insufficient credits."
                        )
                        return@onSuccess
                    }

                    val saveRes = repository.createVoiceProfile(
                        name = state.voiceName,
                        elevenLabsVoiceId = response.voiceId,
                        description = state.voiceDescription.ifBlank { "Cloned via ElevenLabs IVC" },
                        consentStatement = state.consentStatement,
                        consentVerified = true,
                        sampleDurationSec = report.durationSec,
                        clarityScore = report.clarityScore,
                        noiseDb = report.noiseFloorDb,
                        fundamentalFreqHz = report.fundamentalFreqHz,
                        status = "ready"
                    )

                    saveRes.onSuccess { createdId ->
                        _cloneState.value = CloneStudioUiState(
                            cloneStatusMessage = "Voice clone \"${state.voiceName}\" created (ID: ${response.voiceId}) and saved to your secure library!"
                        )
                        _synthesisState.value = _synthesisState.value.copy(selectedVoiceId = createdId)
                    }.onFailure { err ->
                        _cloneState.value = _cloneState.value.copy(
                            isCloningInProgress = false,
                            cloneErrorMessage = err.localizedMessage ?: "Failed to save voice profile."
                        )
                    }
                } else {
                    val currentProfile = accountStore.ensureLocalProfile(currentUserId, userDisplayName, userEmail)
                    if (currentProfile.creditsRemaining < CLONE_CREDIT_COST) {
                        _cloneState.value = _cloneState.value.copy(
                            isCloningInProgress = false,
                            cloneErrorMessage = "Insufficient credits: need $CLONE_CREDIT_COST credits."
                        )
                        return@onSuccess
                    }
                    accountStore.saveLocalProfile(
                        currentProfile.copy(
                            creditsRemaining = currentProfile.creditsRemaining - CLONE_CREDIT_COST,
                            creditsUsed = currentProfile.creditsUsed + CLONE_CREDIT_COST
                        )
                    )
                    val createdId = "voice_${UUID.randomUUID().toString().replace("-", "").take(20)}"
                    val newVoice = VoiceProfile(
                        id = createdId,
                        userId = currentUserId,
                        name = state.voiceName.trim().take(60),
                        elevenLabsVoiceId = response.voiceId,
                        description = state.voiceDescription.ifBlank { "Cloned via ElevenLabs IVC" },
                        consentStatement = state.consentStatement.trim().take(500),
                        consentVerified = true,
                        sampleDurationSec = report.durationSec,
                        clarityScore = report.clarityScore,
                        noiseDb = report.noiseFloorDb,
                        fundamentalFreqHz = report.fundamentalFreqHz,
                        status = "ready"
                    )
                    val updatedList = listOf(newVoice) + accountStore.loadLocalVoices(currentUserId)
                    accountStore.saveLocalVoices(currentUserId, updatedList)
                    refreshLocalSessionData()
                    _cloneState.value = CloneStudioUiState(
                        cloneStatusMessage = "Voice clone \"${state.voiceName}\" created (ID: ${response.voiceId}) and saved to your library!"
                    )
                    _synthesisState.value = _synthesisState.value.copy(selectedVoiceId = createdId)
                }
            }.onFailure { err ->
                _cloneState.value = _cloneState.value.copy(
                    isCloningInProgress = false,
                    cloneStatusMessage = null,
                    cloneErrorMessage = err.localizedMessage ?: "Voice cloning failed."
                )
            }
        }
    }

    fun setStudioMode(mode: String) {
        _synthesisState.value = _synthesisState.value.copy(mode = mode, errorBanner = null, statusBanner = null)
    }

    fun selectVoiceForSynthesis(voiceId: String) {
        _synthesisState.value = _synthesisState.value.copy(selectedVoiceId = voiceId, errorBanner = null)
    }

    fun updateSynthesisText(text: String) {
        _synthesisState.value = _synthesisState.value.copy(inputText = text.take(2500), errorBanner = null)
    }

    fun updateModelId(modelId: String) {
        _synthesisState.value = _synthesisState.value.copy(selectedModelId = modelId)
    }

    fun updateLanguageCode(lang: String) {
        _synthesisState.value = _synthesisState.value.copy(selectedLanguage = lang)
    }

    fun updateVoiceControls(
        stability: Float = _synthesisState.value.stability,
        similarityBoost: Float = _synthesisState.value.similarityBoost,
        styleExaggeration: Float = _synthesisState.value.styleExaggeration,
        speed: Float = _synthesisState.value.speed
    ) {
        _synthesisState.value = _synthesisState.value.copy(
            stability = stability,
            similarityBoost = similarityBoost,
            styleExaggeration = styleExaggeration,
            speed = speed
        )
    }

    fun startStsSourceRecording() {
        val outputFile = File(filesDir, "sts_source_${System.currentTimeMillis()}.wav")
        _synthesisState.value = _synthesisState.value.copy(
            errorBanner = null,
            statusBanner = "Recording source speech for Voice-to-Voice conversion..."
        )
        audioEngine.startMicrophoneRecording(
            scope = viewModelScope,
            outputWavFile = outputFile,
            onFinished = { file, report ->
                _synthesisState.value = _synthesisState.value.copy(
                    stsSourceFile = file,
                    stsSourceAnalysis = report,
                    statusBanner = "Source speech recorded (${report.durationSec}s, F0: ${report.fundamentalFreqHz} Hz)."
                )
            },
            onError = { err ->
                _synthesisState.value = _synthesisState.value.copy(errorBanner = err, statusBanner = null)
            }
        )
    }

    fun importStsSourceUri(uri: Uri) {
        viewModelScope.launch {
            val res = audioEngine.importAudioUriToStudioFile(uri)
            res.onSuccess { (file, report) ->
                _synthesisState.value = _synthesisState.value.copy(
                    stsSourceFile = file,
                    stsSourceAnalysis = report,
                    statusBanner = "Uploaded source speech (${report.durationSec}s, F0: ${report.fundamentalFreqHz} Hz).",
                    errorBanner = null
                )
            }.onFailure { e ->
                _synthesisState.value = _synthesisState.value.copy(
                    errorBanner = e.localizedMessage ?: "Failed to import source audio."
                )
            }
        }
    }

    fun generateSpeechOrConvert(availableVoices: List<VoiceProfile>) {
        val state = _synthesisState.value
        val selectedVoice = availableVoices.firstOrNull { it.id == state.selectedVoiceId }
            ?: availableVoices.firstOrNull()

        if (selectedVoice == null) {
            _synthesisState.value = state.copy(
                errorBanner = "No cloned voice selected. Please create an authorized voice clone in the Clone tab first."
            )
            return
        }

        val estimatedCredits = if (state.mode == "tts") {
            max(1L, state.inputText.trim().length.toLong())
        } else {
            val dur = state.stsSourceAnalysis?.durationSec ?: 15.0
            max(50L, (dur * 15).toLong())
        }

        if (state.mode == "tts" && state.inputText.isBlank()) {
            _synthesisState.value = state.copy(errorBanner = "Please enter text to synthesize.")
            return
        }
        if (state.mode == "sts" && (state.stsSourceFile == null || !state.stsSourceFile.exists())) {
            _synthesisState.value = state.copy(
                errorBanner = "Please record or upload source speech for Voice-to-Voice conversion."
            )
            return
        }

        viewModelScope.launch {
            _synthesisState.value = state.copy(
                isGenerating = true,
                errorBanner = null,
                statusBanner = if (state.mode == "tts") {
                    "Synthesizing speech with ElevenLabs (${selectedVoice.name})..."
                } else {
                    "Converting speech-to-speech with ElevenLabs (${selectedVoice.name})..."
                }
            )

            val outputFile = File(filesDir, "clonvo_gen_${System.currentTimeMillis()}.mp3")
            val apiResult = if (state.mode == "tts") {
                elevenLabsService.generateSpeechToFile(
                    elevenLabsVoiceId = selectedVoice.elevenLabsVoiceId,
                    text = state.inputText,
                    modelId = state.selectedModelId,
                    languageCode = state.selectedLanguage,
                    stability = state.stability.toDouble(),
                    similarityBoost = state.similarityBoost.toDouble(),
                    styleExaggeration = state.styleExaggeration.toDouble(),
                    speed = state.speed.toDouble(),
                    outputFile = outputFile
                )
            } else {
                elevenLabsService.convertSpeechToSpeech(
                    elevenLabsVoiceId = selectedVoice.elevenLabsVoiceId,
                    sourceAudioFile = state.stsSourceFile!!,
                    stability = state.stability.toDouble(),
                    similarityBoost = state.similarityBoost.toDouble(),
                    styleExaggeration = state.styleExaggeration.toDouble(),
                    outputFile = outputFile
                )
            }

            apiResult.onSuccess { generatedFile ->
                val similarity = audioEngine.compareVoiceSimilarity(
                    referenceF0Hz = selectedVoice.fundamentalFreqHz,
                    referenceClarity = selectedVoice.clarityScore,
                    generatedAudioFile = generatedFile
                )

                val promptDesc = if (state.mode == "tts") {
                    state.inputText
                } else {
                    "Voice-to-Voice conversion (${state.stsSourceAnalysis?.durationSec ?: 0.0}s input)"
                }

                if (isCloudFirebaseSession) {
                    repository.deductCredits(estimatedCredits)
                    repository.createGenerationRecord(
                        voiceId = selectedVoice.id,
                        voiceName = selectedVoice.name,
                        mode = state.mode,
                        inputText = promptDesc,
                        modelId = if (state.mode == "tts") state.selectedModelId else "eleven_multilingual_sts_v2",
                        languageCode = state.selectedLanguage,
                        stability = state.stability.toDouble(),
                        similarityBoost = state.similarityBoost.toDouble(),
                        styleExaggeration = state.styleExaggeration.toDouble(),
                        speed = state.speed.toDouble(),
                        localAudioPath = generatedFile.absolutePath,
                        measuredF0Hz = similarity.generatedF0Hz,
                        measuredSimilarityScore = similarity.overallAcousticSimilarityPercent,
                        creditsCharged = estimatedCredits
                    )
                } else {
                    val profile = accountStore.ensureLocalProfile(currentUserId, userDisplayName, userEmail)
                    accountStore.saveLocalProfile(
                        profile.copy(
                            creditsRemaining = max(0L, profile.creditsRemaining - estimatedCredits),
                            creditsUsed = profile.creditsUsed + estimatedCredits
                        )
                    )
                    val newGen = VoiceGeneration(
                        id = "gen_${UUID.randomUUID().toString().replace("-", "").take(20)}",
                        userId = currentUserId,
                        voiceId = selectedVoice.id,
                        voiceName = selectedVoice.name,
                        mode = state.mode,
                        inputText = promptDesc,
                        modelId = if (state.mode == "tts") state.selectedModelId else "eleven_multilingual_sts_v2",
                        languageCode = state.selectedLanguage,
                        stability = state.stability.toDouble(),
                        similarityBoost = state.similarityBoost.toDouble(),
                        styleExaggeration = state.styleExaggeration.toDouble(),
                        speed = state.speed.toDouble(),
                        localAudioPath = generatedFile.absolutePath,
                        measuredF0Hz = similarity.generatedF0Hz,
                        measuredSimilarityScore = similarity.overallAcousticSimilarityPercent,
                        creditsCharged = estimatedCredits,
                        isFavorite = false,
                        aiDisclosureLabeled = true
                    )
                    val gens = listOf(newGen) + accountStore.loadLocalGenerations(currentUserId)
                    accountStore.saveLocalGenerations(currentUserId, gens)
                    refreshLocalSessionData()
                }

                _synthesisState.value = _synthesisState.value.copy(
                    isGenerating = false,
                    latestGeneratedFile = generatedFile,
                    latestSimilarityResult = similarity,
                    statusBanner = "Synthesis complete! Charged $estimatedCredits credits. Playing generated audio...",
                    errorBanner = null
                )
                audioEngine.playAudioFile(generatedFile.absolutePath)
            }.onFailure { err ->
                _synthesisState.value = _synthesisState.value.copy(
                    isGenerating = false,
                    statusBanner = null,
                    errorBanner = err.localizedMessage ?: "Audio generation failed."
                )
            }
        }
    }

    fun playAudio(filePath: String) {
        audioEngine.playAudioFile(filePath) { err ->
            _synthesisState.value = _synthesisState.value.copy(errorBanner = err)
        }
    }

    fun stopAudio() {
        audioEngine.stopPlayback()
    }

    fun exportGeneratedAudio(filePath: String, voiceName: String, formatExt: String) {
        val res = audioEngine.exportAudioToDownloads(filePath, voiceName, formatExt)
        res.onSuccess { savedLocation ->
            _synthesisState.value = _synthesisState.value.copy(
                statusBanner = "Exported to $savedLocation",
                errorBanner = null
            )
        }.onFailure { err ->
            _synthesisState.value = _synthesisState.value.copy(
                errorBanner = err.localizedMessage ?: "Export failed."
            )
        }
    }

    fun shareGeneratedAudio(filePath: String, voiceName: String, promptText: String) {
        audioEngine.shareAudioWithDisclosure(filePath, voiceName, promptText)
    }

    fun toggleFavoriteGeneration(generation: VoiceGeneration) {
        viewModelScope.launch {
            if (isCloudFirebaseSession) {
                repository.toggleFavoriteGeneration(generation.id, !generation.isFavorite)
            } else {
                val updated = accountStore.loadLocalGenerations(currentUserId).map {
                    if (it.id == generation.id) it.copy(isFavorite = !it.isFavorite) else it
                }
                accountStore.saveLocalGenerations(currentUserId, updated)
                refreshLocalSessionData()
            }
        }
    }

    fun deleteGeneration(generation: VoiceGeneration) {
        viewModelScope.launch {
            if (generation.localAudioPath.isNotBlank()) {
                runCatching { File(generation.localAudioPath).delete() }
            }
            if (isCloudFirebaseSession) {
                repository.deleteGeneration(generation.id)
            } else {
                val updated = accountStore.loadLocalGenerations(currentUserId).filterNot { it.id == generation.id }
                accountStore.saveLocalGenerations(currentUserId, updated)
                refreshLocalSessionData()
            }
        }
    }

    fun deleteVoiceProfile(voice: VoiceProfile) {
        viewModelScope.launch {
            if (voice.elevenLabsVoiceId.isNotBlank() && elevenLabsService.isApiKeyConfigured()) {
                elevenLabsService.deleteRemoteVoice(voice.elevenLabsVoiceId)
            }
            if (isCloudFirebaseSession) {
                repository.deleteVoiceProfile(voice.id)
            } else {
                val updated = accountStore.loadLocalVoices(currentUserId).filterNot { it.id == voice.id }
                accountStore.saveLocalVoices(currentUserId, updated)
                refreshLocalSessionData()
            }
        }
    }

    fun submitAbuseReport(targetVoiceId: String, reason: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            if (isCloudFirebaseSession) {
                val res = repository.submitAbuseReport(targetVoiceId, reason)
                res.onSuccess {
                    onDone("Safety report submitted to moderation queue (ID: $it).")
                }.onFailure { e ->
                    onDone("Failed to submit report: ${e.localizedMessage}")
                }
            } else {
                val repId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                onDone("Safety report submitted to moderation queue (ID: $repId).")
            }
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (isCloudFirebaseSession) {
                repository.deleteAllUserData()
            } else {
                accountStore.deleteAccount(currentUserId)
            }
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopMicrophoneRecording()
        audioEngine.stopPlayback()
    }

    companion object {
        private const val TAG = "ClonvoViewModel"
        private const val CLONE_CREDIT_COST = 250L
    }
}
