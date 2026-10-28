package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGeneration
import com.example.data.model.VoiceProfile
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyanBright
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    voices: List<VoiceProfile>,
    generations: List<VoiceGeneration>,
    isPlaying: Boolean,
    playingFilePath: String?,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit,
    onToggleFavorite: (VoiceGeneration) -> Unit,
    onDeleteGeneration: (VoiceGeneration) -> Unit,
    onDeleteVoice: (VoiceProfile) -> Unit,
    onExportAudio: (String, String, String) -> Unit,
    onShareAudio: (String, String, String) -> Unit,
    onSubmitAbuseReport: (String, String, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all_audio") }
    var reportTargetId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    val filteredGenerations = remember(generations, selectedFilter) {
        when (selectedFilter) {
            "favorites" -> generations.filter { it.isFavorite }
            "tts" -> generations.filter { it.mode == "tts" }
            "sts" -> generations.filter { it.mode == "sts" }
            else -> generations
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Text(
                text = "Voice & Generation Library",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Manage your reusable cloned voice profiles, favorite generated recordings, and export MP3/WAV masters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        feedbackBanner?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricPurpleBright.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == "voices",
                        onClick = { selectedFilter = "voices" },
                        label = { Text("Saved Voices (${voices.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "all_audio",
                        onClick = { selectedFilter = "all_audio" },
                        label = { Text("All Generations (${generations.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "favorites",
                        onClick = { selectedFilter = "favorites" },
                        label = { Text("Favorites") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "tts",
                        onClick = { selectedFilter = "tts" },
                        label = { Text("TTS") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "sts",
                        onClick = { selectedFilter = "sts" },
                        label = { Text("Voice-to-Voice") }
                    )
                }
            }
        }

        if (selectedFilter == "voices") {
            if (voices.isEmpty()) {
                item {
                    EmptyLibraryCard("No saved voice profiles yet. Record your voice in the Clone tab.")
                }
            } else {
                items(voices, key = { it.id }) { voice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voice.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "ElevenLabs ID: ${voice.elevenLabsVoiceId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyanBright
                                    )
                                }
                                Row {
                                    IconButton(onClick = { reportTargetId = voice.id }) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = "Report voice",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteVoice(voice) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete voice profile",
                                            tint = CoralError
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Duration: ${voice.sampleDurationSec}s • F0: ${voice.fundamentalFreqHz.roundToInt()} Hz • Clarity: ${voice.clarityScore.roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Verified Consent: \"${voice.consentStatement}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        } else {
            if (filteredGenerations.isEmpty()) {
                item {
                    EmptyLibraryCard("No generated audio recordings match this filter.")
                }
            } else {
                items(filteredGenerations, key = { it.id }) { gen ->
                    val isCurrentlyPlaying = isPlaying && playingFilePath == gen.localAudioPath
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isCurrentlyPlaying) onStopAudio()
                                        else onPlayAudio(gen.localAudioPath)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Play or stop",
                                        tint = NeonCyanBright
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${gen.voiceName} (${gen.mode.uppercase()} • ${gen.languageCode.uppercase()})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "F0: ${gen.measuredF0Hz.roundToInt()} Hz • Similarity: ${gen.measuredSimilarityScore}% • ${gen.creditsCharged} credits",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricPurpleBright
                                    )
                                }
                                IconButton(onClick = { onToggleFavorite(gen) }) {
                                    Icon(
                                        imageVector = if (gen.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (gen.isFavorite) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteGeneration(gen) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete recording",
                                        tint = CoralError
                                    )
                                }
                            }

                            Text(
                                text = gen.inputText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        onExportAudio(gen.localAudioPath, gen.voiceName, "mp3")
                                        feedbackBanner = "Exported MP3 to Downloads with AI Disclosure label."
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MP3")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onExportAudio(gen.localAudioPath, gen.voiceName, "wav")
                                        feedbackBanner = "Exported WAV to Downloads with AI Disclosure label."
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WAV")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onShareAudio(gen.localAudioPath, gen.voiceName, gen.inputText)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
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

    if (reportTargetId != null) {
        AlertDialog(
            onDismissRequest = { reportTargetId = null },
            title = { Text("Report Unauthorized Voice or Misuse") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Report suspected impersonation, non-consensual voice cloning, or policy abuse.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason for report (min 5 chars)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = reportTargetId ?: "general"
                        onSubmitAbuseReport(target, reportReason) { msg ->
                            feedbackBanner = msg
                        }
                        reportReason = ""
                        reportTargetId = null
                    },
                    enabled = reportReason.trim().length >= 5
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyLibraryCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    userEmail: String,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSubmitAbuseReport: (String, String, (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var billingDialogPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val githubPrivacyUrl = "https://github.com/clonvo-ai/privacy-and-terms/blob/main/PRIVACY_POLICY_AND_TERMS.md"
    val githubDeleteDataUrl = "https://github.com/clonvo-ai/privacy-and-terms/blob/main/DELETE_ACCOUNT_AND_DATA.md"

    fun openUrlInBrowser(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            statusNotice = "Unable to launch link: $url"
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = userProfile?.displayName?.ifBlank { "Clonvo Studio Creator" } ?: "Clonvo Studio Creator",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = userProfile?.email?.ifBlank { userEmail } ?: userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Credits Remaining", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${userProfile?.creditsRemaining ?: 2500}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonCyanBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Credits Used", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${userProfile?.creditsUsed ?: 0}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ElectricPurpleBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        statusNotice?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricPurpleBright.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Theme Preference
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Navy Studio Theme", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Switch between dark navy/black and light studio theme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_toggle_switch")
                    )
                }
            }
        }

        // Subscriptions & Google Play Billing Section
        item {
            Text(
                text = "Subscriptions & Credits (Google Play Billing)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Credits are tracked in Firestore and deducted strictly according to real API usage. Paid upgrades require a live Google Play Billing merchant SKU.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(SubscriptionPlan.entries) { plan ->
            val isCurrent = (userProfile?.planTier ?: "free_trial") == plan.tierId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) NeonCyanBright else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = plan.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = plan.priceLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyanBright
                            )
                        }
                        if (isCurrent) {
                            Text(
                                text = "ACTIVE PLAN",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                        } else {
                            Button(
                                onClick = { billingDialogPlan = plan },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subscribe")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    plan.features.forEach { feat ->
                        Text(
                            text = "• $feat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Safety, Legal & Account Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Safety, Governance & Account Controls",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = { showTermsDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Gavel, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terms of Service")
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeleteDataDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, tint = CoralError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & Data URL (GitHub)", color = CoralError)
                    }

                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Voice Abuse or Unauthorized Impersonation")
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & All Cloned Voices")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    billingDialogPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { billingDialogPlan = null },
            title = { Text("Google Play Billing Setup Required (${plan.title})") },
            text = {
                Text(
                    "To process real digital subscriptions for ${plan.title} (${plan.priceLabel}, product ID: \"${plan.googlePlayProductId}\"), connect this app package (com.aistudio.clonvoai.vxcqmr) to a Google Play Console merchant account, create the in-app subscription SKU \"${plan.googlePlayProductId}\", and verify purchase tokens on your backend.\n\nClonvo AI never simulates fake payment success or unauthorized premium activation."
                )
            },
            confirmButton = {
                Button(onClick = { billingDialogPlan = null }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Clonvo AI Privacy Policy") },
            text = {
                Text(
                    "1. Voice Data Ownership: Your voice recordings and cloned voice profiles are scoped strictly to your authenticated UID.\n\n" +
                        "2. Local & Cloud Storage: Recorded audio samples are stored in app-private storage and transmitted over HTTPS exclusively to ElevenLabs API when you initiate voice cloning or synthesis.\n\n" +
                        "3. Deletion Rights: You can permanently delete any individual voice profile, generated recording, or your entire account and associated data at any time.\n\n" +
                        "GitHub Privacy URL:\n$githubPrivacyUrl"
                )
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubPrivacyUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright)
                ) {
                    Text("Open in GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Clonvo AI Terms of Service") },
            text = {
                Text(
                    "1. Authorized Voice Consent Only: You may only clone your own voice or voices for which you hold explicit, verifiable legal consent.\n\n" +
                        "2. Prohibition on Deceptive Impersonation: Using Clonvo AI to impersonate public figures, commit fraud, or deceive listeners is strictly prohibited.\n\n" +
                        "3. Mandatory AI Labeling: All exported and shared audio includes AI-generated disclosure metadata.\n\n" +
                        "GitHub Terms URL:\n$githubPrivacyUrl"
                )
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubPrivacyUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright)
                ) {
                    Text("Open in GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete Account & Data URL") },
            text = {
                Text(
                    "Clonvo AI provides complete self-service account and data deletion.\n\n" +
                        "• In-App Deletion: Tap 'Delete Account & All Cloned Voices' below.\n" +
                        "• External Email Request: Send request to gadgetexplorerpro@gmail.com\n\n" +
                        "GitHub Data Deletion URL:\n$githubDeleteDataUrl"
                )
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubDeleteDataUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = Color.White)
                ) {
                    Text("Open GitHub Data Deletion Guide")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) { Text("Close") }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Submit Safety & Abuse Report") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Describe the suspected voice misuse (min 5 chars)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitAbuseReport("general_moderation", reportReason) { msg ->
                            statusNotice = msg
                        }
                        reportReason = ""
                        showReportDialog = false
                    },
                    enabled = reportReason.trim().length >= 5
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account & All Voice Clones?") },
            text = {
                Text("This permanently deletes all of your saved voice profiles, generated recordings, and account profile, and signs you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountAndVoices()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = Color.White)
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

