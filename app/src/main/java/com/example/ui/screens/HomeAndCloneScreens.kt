package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGeneration
import com.example.data.model.VoiceProfile
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.AnimatedWaveformCanvas
import com.example.ui.components.ApiKeySetupNoticeCard
import com.example.ui.components.StatusMessageBanner
import com.example.ui.components.StudioMetricBadge
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.viewmodel.CloneStudioUiState
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    voices: List<VoiceProfile>,
    generations: List<VoiceGeneration>,
    isApiKeyConfigured: Boolean,
    playingFilePath: String?,
    isPlaying: Boolean,
    onNavigateToClone: () -> Unit,
    onNavigateToStudio: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onPlayGeneration: (String) -> Unit,
    onStopPlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(ElectricPurpleBright, NeonCyanBright)),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "Clonvo AI Studio Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricPurpleBright)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = (userProfile?.planTier ?: "free_trial").uppercase().replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${userProfile?.creditsRemaining ?: 2500} Credits Available",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyanBright
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Clonvo AI Voice Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Clone your voice, synthesize multilingual speech, and analyze F0 vocal similarity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            ApiKeySetupNoticeCard(isConfigured = isApiKeyConfigured)
        }

        item {
            Text(
                text = "Studio Workflows",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Instant Voice Clone",
                    subtitle = "Record 15–60s or upload WAV/MP3",
                    icon = Icons.Default.Mic,
                    gradientColors = listOf(ElectricPurple, ElectricPurpleBright),
                    onClick = onNavigateToClone,
                    testTag = "quick_action_clone",
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Text to Cloned Voice",
                    subtitle = "ElevenLabs Multilingual TTS",
                    icon = Icons.Default.RecordVoiceOver,
                    gradientColors = listOf(NeonCyan, ElectricPurple),
                    onClick = { onNavigateToStudio("tts") },
                    testTag = "quick_action_tts",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Voice-to-Voice",
                    subtitle = "Convert speech into cloned voice",
                    icon = Icons.Default.SwapHoriz,
                    gradientColors = listOf(Color(0xFF0284C7), NeonCyanBright),
                    onClick = { onNavigateToStudio("sts") },
                    testTag = "quick_action_sts",
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Voice Library & F0",
                    subtitle = "${voices.size} Voices • ${generations.size} Audios",
                    icon = Icons.Default.Equalizer,
                    gradientColors = listOf(Color(0xFF4F46E5), ElectricPurpleBright),
                    onClick = onNavigateToLibrary,
                    testTag = "quick_action_library",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Authorized Cloned Voices (${voices.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "New Clone +",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyanBright,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToClone() }
                        .padding(6.dp)
                )
            }
        }

        if (voices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ElectricPurpleBright,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Cloned Voices Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Record 15–60 seconds of your voice and verify consent to create your first reusable voice profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToClone,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright),
                            modifier = Modifier.testTag("empty_state_create_clone_button")
                        ) {
                            Text("Record & Clone My Voice")
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(voices, key = { it.id }) { voice ->
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .clickable { onNavigateToStudio("tts") },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Consent Verified",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = voice.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "F0: ${voice.fundamentalFreqHz.roundToInt()} Hz • Clarity: ${voice.clarityScore.roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyanBright
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sample: ${voice.sampleDurationSec}s • ID: ${voice.elevenLabsVoiceId.take(10)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (generations.isNotEmpty()) {
            item {
                Text(
                    text = "Recent AI Speech Generations",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(generations.take(4), key = { it.id }) { gen ->
                val isCurrentlyPlaying = isPlaying && playingFilePath == gen.localAudioPath
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isCurrentlyPlaying) onStopPlayback()
                                else onPlayGeneration(gen.localAudioPath)
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isCurrentlyPlaying) "Stop audio" else "Play audio",
                                tint = NeonCyanBright
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${gen.voiceName} • ${gen.mode.uppercase()}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = gen.inputText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Text(
                                text = "Measured F0: ${gen.measuredF0Hz.roundToInt()} Hz • Acoustic Match: ${gen.measuredSimilarityScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricPurpleBright
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CloneVoiceScreen(
    cloneState: CloneStudioUiState,
    isRecording: Boolean,
    recordingDurationSec: Double,
    liveAmplitudes: List<Float>,
    isPlaying: Boolean,
    playingFilePath: String?,
    isApiKeyConfigured: Boolean,
    onVoiceNameChange: (String) -> Unit,
    onVoiceDescriptionChange: (String) -> Unit,
    onConsentStatementChange: (String) -> Unit,
    onConsentCheckedChange: (Boolean) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onImportAudioUri: (android.net.Uri) -> Unit,
    onPlaySample: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onSubmitClone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportAudioUri(uri)
        }
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
                text = "Instant Voice Cloning Studio",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Record 15–60 seconds of clear speech (30–60s recommended) or upload a clean WAV/MP3 recording of your own voice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ApiKeySetupNoticeCard(isConfigured = isApiKeyConfigured)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayedWave = if (isRecording) {
                        liveAmplitudes
                    } else {
                        cloneState.analysisReport?.waveformAmplitudes ?: liveAmplitudes
                    }
                    AnimatedWaveformCanvas(
                        amplitudes = displayedWave,
                        isActive = isRecording
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val shownDuration = if (isRecording) {
                        recordingDurationSec
                    } else {
                        cloneState.analysisReport?.durationSec ?: 0.0
                    }
                    Text(
                        text = String.format("%.1f s / 60.0 s (Min 15.0 s)", shownDuration),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (shownDuration >= 15.0) EmeraldSuccess else NeonCyanBright
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Microphone Recording Button
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isRecording) {
                                        listOf(Color(0xFFEF4444), Color(0xFFF97316))
                                    } else {
                                        listOf(ElectricPurpleBright, NeonCyanBright)
                                    }
                                )
                            )
                            .clickable {
                                if (isRecording) {
                                    onStopRecording()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        onStartRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                            .testTag("record_voice_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop recording" else "Start microphone recording",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isRecording) "Tap to Stop Recording" else "Tap Microphone to Record Voice",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            enabled = !isRecording,
                            modifier = Modifier.testTag("upload_audio_file_button")
                        ) {
                            Icon(imageVector = Icons.Default.AudioFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload MP3 / WAV")
                        }

                        val sampleFile = cloneState.recordedFile
                        if (sampleFile != null && sampleFile.exists()) {
                            val isPlayingSample = isPlaying && playingFilePath == sampleFile.absolutePath
                            OutlinedButton(
                                onClick = {
                                    if (isPlayingSample) onStopPlayback()
                                    else onPlaySample(sampleFile.absolutePath)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingSample) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlayingSample) "Stop Preview" else "Preview Sample")
                            }
                        }
                    }
                }
            }
        }

        // Acoustic DSP Analysis Card
        cloneState.analysisReport?.let { report ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Real-Time Acoustic & F0 Pitch Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyanBright
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioMetricBadge(
                                label = "Duration",
                                value = "${report.durationSec}s",
                                accentColor = if (report.isDurationValid) EmeraldSuccess else Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            StudioMetricBadge(
                                label = "Clarity (SNR)",
                                value = "${report.clarityScore}%",
                                accentColor = NeonCyanBright,
                                modifier = Modifier.weight(1f)
                            )
                            StudioMetricBadge(
                                label = "Noise Floor",
                                value = "${report.noiseFloorDb} dB",
                                accentColor = ElectricPurpleBright,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioMetricBadge(
                                label = "Median F0",
                                value = "${report.fundamentalFreqHz} Hz",
                                accentColor = NeonCyanBright,
                                modifier = Modifier.weight(1f)
                            )
                            StudioMetricBadge(
                                label = "Pitch Range",
                                value = "${report.f0MinHz.roundToInt()}–${report.f0MaxHz.roundToInt()} Hz",
                                accentColor = ElectricPurpleBright,
                                modifier = Modifier.weight(1f)
                            )
                            StudioMetricBadge(
                                label = "Pitch Stability",
                                value = "${report.pitchStabilityPercent}%",
                                accentColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = report.qualityAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Voice Metadata & Mandatory Speaker Consent Verification
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Voice Profile & Speaker Consent Verification",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = cloneState.voiceName,
                        onValueChange = onVoiceNameChange,
                        label = { Text("Voice Profile Name (e.g., My Studio Narrator)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clone_voice_name_input")
                    )

                    OutlinedTextField(
                        value = cloneState.voiceDescription,
                        onValueChange = onVoiceDescriptionChange,
                        label = { Text("Accent, Timbre & Style Notes (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clone_voice_desc_input")
                    )

                    OutlinedTextField(
                        value = cloneState.consentStatement,
                        onValueChange = onConsentStatementChange,
                        label = { Text("Recorded Speaker Consent Statement") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clone_consent_statement_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConsentCheckedChange(!cloneState.consentChecked) }
                    ) {
                        Checkbox(
                            checked = cloneState.consentChecked,
                            onCheckedChange = onConsentCheckedChange,
                            modifier = Modifier.testTag("clone_consent_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I verify that this recording is my own voice (or I hold explicit legal authorization from the speaker) and I prohibit deceptive impersonation.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    StatusMessageBanner(
                        statusMessage = cloneState.cloneStatusMessage,
                        errorMessage = cloneState.cloneErrorMessage
                    )

                    Button(
                        onClick = onSubmitClone,
                        enabled = !cloneState.isCloningInProgress && !isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPurpleBright,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_instant_clone_button")
                    ) {
                        if (cloneState.isCloningInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Cloning Voice with ElevenLabs...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Instant Voice Clone (250 Credits)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
