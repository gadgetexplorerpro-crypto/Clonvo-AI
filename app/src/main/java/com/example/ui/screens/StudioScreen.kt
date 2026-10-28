package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceProfile
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.ApiKeySetupNoticeCard
import com.example.ui.components.StatusMessageBanner
import com.example.ui.components.StudioMetricBadge
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanBright
import com.example.ui.viewmodel.SynthesisStudioUiState
import kotlin.math.max
import kotlin.math.roundToInt

private val SUPPORTED_LANGUAGES = listOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "hi" to "Hindi",
    "pt" to "Portuguese",
    "ja" to "Japanese",
    "ar" to "Arabic"
)

private val SUPPORTED_MODELS = listOf(
    "eleven_multilingual_v2" to "Eleven Multilingual v2 (High Fidelity)",
    "eleven_turbo_v2_5" to "Eleven Turbo v2.5 (Low Latency)"
)

@Composable
fun StudioScreen(
    synthesisState: SynthesisStudioUiState,
    voices: List<VoiceProfile>,
    creditsRemaining: Long,
    isRecording: Boolean,
    isPlaying: Boolean,
    playingFilePath: String?,
    isApiKeyConfigured: Boolean,
    onModeChange: (String) -> Unit,
    onSelectVoice: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onControlsChange: (Float, Float, Float, Float) -> Unit,
    onStartStsRecording: () -> Unit,
    onStopStsRecording: () -> Unit,
    onImportStsUri: (android.net.Uri) -> Unit,
    onGenerate: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit,
    onExportAudio: (String, String, String) -> Unit,
    onShareAudio: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedVoice = voices.firstOrNull { it.id == synthesisState.selectedVoiceId }
        ?: voices.firstOrNull()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartStsRecording()
    }

    val stsFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportStsUri(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Text(
                text = "AI Voice Synthesis & Conversion Studio",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Generate natural speech from text or convert recorded speech into your authorized cloned voice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ApiKeySetupNoticeCard(isConfigured = isApiKeyConfigured)
        }

        // Mode Switcher: Text-to-Speech vs Voice-to-Voice
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = synthesisState.mode == "tts",
                    onClick = { onModeChange("tts") },
                    label = { Text("Text to Cloned Voice (TTS)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("studio_mode_tts_chip")
                )
                FilterChip(
                    selected = synthesisState.mode == "sts",
                    onClick = { onModeChange("sts") },
                    label = { Text("Voice-to-Voice (STS)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("studio_mode_sts_chip")
                )
            }
        }

        // Saved Voice Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Select Authorized Cloned Voice Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (voices.isEmpty()) {
                        Text(
                            text = "No cloned voice profiles saved yet. Go to the Clone tab to record and verify your voice first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(voices, key = { it.id }) { voice ->
                                val isSelected = selectedVoice?.id == voice.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) ElectricPurpleBright.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NeonCyanBright else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectVoice(voice.id) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = voice.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) NeonCyanBright else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "F0: ${voice.fundamentalFreqHz.roundToInt()} Hz • ${voice.clarityScore.roundToInt()}% Clarity",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Card: Text Editor (TTS) or Speech Source Recorder (STS)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (synthesisState.mode == "tts") {
                        Text(
                            text = "2. Script Editor & Language Synthesis",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = synthesisState.inputText,
                            onValueChange = onTextChange,
                            label = { Text("Enter text to speak in your cloned voice") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_tts_text_input")
                        )

                        val charCount = synthesisState.inputText.trim().length
                        val estCredits = max(1, charCount)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$charCount / 2500 characters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Estimated Cost: $estCredits credits (Balance: $creditsRemaining)",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyanBright
                            )
                        }

                        Text(
                            text = "Synthesis Model",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SUPPORTED_MODELS) { (modelId, label) ->
                                FilterChip(
                                    selected = synthesisState.selectedModelId == modelId,
                                    onClick = { onModelChange(modelId) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text(
                            text = "Language",
                            style = MaterialTheme.typography.labelLarge
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SUPPORTED_LANGUAGES) { (code, name) ->
                                FilterChip(
                                    selected = synthesisState.selectedLanguage == code,
                                    onClick = { onLanguageChange(code) },
                                    label = { Text("$name ($code)") }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "2. Source Speech Recording for Voice-to-Voice Conversion",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Record or upload speech to convert its vocal identity into your cloned voice while preserving timing, pauses, and expression.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) {
                                        onStopStsRecording()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) onStartStsRecording()
                                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color(0xFFEF4444) else ElectricPurpleBright
                                ),
                                modifier = Modifier.testTag("sts_record_source_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRecording) "Stop Recording" else "Record Source Speech")
                            }

                            OutlinedButton(
                                onClick = { stsFilePicker.launch(arrayOf("audio/*")) },
                                enabled = !isRecording
                            ) {
                                Icon(imageVector = Icons.Default.AudioFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Audio")
                            }
                        }

                        synthesisState.stsSourceAnalysis?.let { rep ->
                            Text(
                                text = "Loaded Source Audio: ${rep.durationSec}s • F0: ${rep.fundamentalFreqHz} Hz • Clarity: ${rep.clarityScore}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }

        // Vocal Expression, Stability & Speed Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3. Vocal Timbre, Expression & Speed Controls",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ControlSliderRow(
                        label = "Voice Stability",
                        valueText = "${(synthesisState.stability * 100).roundToInt()}%",
                        value = synthesisState.stability,
                        valueRange = 0f..1f,
                        onValueChange = {
                            onControlsChange(
                                it,
                                synthesisState.similarityBoost,
                                synthesisState.styleExaggeration,
                                synthesisState.speed
                            )
                        }
                    )

                    ControlSliderRow(
                        label = "Cloned Voice Similarity Boost",
                        valueText = "${(synthesisState.similarityBoost * 100).roundToInt()}%",
                        value = synthesisState.similarityBoost,
                        valueRange = 0f..1f,
                        onValueChange = {
                            onControlsChange(
                                synthesisState.stability,
                                it,
                                synthesisState.styleExaggeration,
                                synthesisState.speed
                            )
                        }
                    )

                    ControlSliderRow(
                        label = "Emotional Style Exaggeration",
                        valueText = "${(synthesisState.styleExaggeration * 100).roundToInt()}%",
                        value = synthesisState.styleExaggeration,
                        valueRange = 0f..1f,
                        onValueChange = {
                            onControlsChange(
                                synthesisState.stability,
                                synthesisState.similarityBoost,
                                it,
                                synthesisState.speed
                            )
                        }
                    )

                    if (synthesisState.mode == "tts") {
                        ControlSliderRow(
                            label = "Speaking Speed",
                            valueText = String.format("%.2fx", synthesisState.speed),
                            value = synthesisState.speed,
                            valueRange = 0.7f..1.2f,
                            onValueChange = {
                                onControlsChange(
                                    synthesisState.stability,
                                    synthesisState.similarityBoost,
                                    synthesisState.styleExaggeration,
                                    it
                                )
                            }
                        )
                    }

                    Text(
                        text = "Fidelity Notice: AI voice reproduction preserves recognizable tone, pitch range, and cadence as closely as the model allows, but never claims 100% identical human reproduction.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            StatusMessageBanner(
                statusMessage = synthesisState.statusBanner,
                errorMessage = synthesisState.errorBanner
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGenerate,
                enabled = !synthesisState.isGenerating && !isRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricPurpleBright,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_speech_button")
            ) {
                if (synthesisState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generating Real AI Speech...")
                } else {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (synthesisState.mode == "tts") "Generate Cloned Speech" else "Convert Speech to Cloned Voice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Playback, Export & Measured Voice Similarity Analysis Card
        val generatedFile = synthesisState.latestGeneratedFile
        if (generatedFile != null && generatedFile.exists()) {
            item {
                val isPlayingCurrent = isPlaying && playingFilePath == generatedFile.absolutePath
                val voiceLabel = selectedVoice?.name ?: "ClonedVoice"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Generated Studio Master • $voiceLabel",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyanBright
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isPlayingCurrent) onStopAudio()
                                    else onPlayAudio(generatedFile.absolutePath)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyanBright, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingCurrent) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlayingCurrent) "Pause / Stop" else "Play Audio")
                            }

                            OutlinedButton(
                                onClick = onGenerate,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Regenerate")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExportAudio(generatedFile.absolutePath, voiceLabel, "mp3") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MP3")
                            }
                            OutlinedButton(
                                onClick = { onExportAudio(generatedFile.absolutePath, voiceLabel, "wav") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WAV")
                            }
                            OutlinedButton(
                                onClick = {
                                    onShareAudio(
                                        generatedFile.absolutePath,
                                        voiceLabel,
                                        synthesisState.inputText
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }

                        synthesisState.latestSimilarityResult?.let { sim ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = null,
                                    tint = ElectricPurpleBright
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice Similarity Analysis (DSP F0 & Spectral Match)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioMetricBadge(
                                    label = "Ref F0 vs Gen F0",
                                    value = "${sim.referenceF0Hz.roundToInt()} / ${sim.generatedF0Hz.roundToInt()} Hz",
                                    accentColor = NeonCyanBright,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricBadge(
                                    label = "Pitch Contour",
                                    value = "${sim.pitchContourCorrelation}%",
                                    accentColor = ElectricPurpleBright,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricBadge(
                                    label = "Acoustic Match",
                                    value = "${sim.overallAcousticSimilarityPercent}%",
                                    accentColor = EmeraldSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = sim.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            AiDisclosureBanner()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ControlSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyanBright
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
