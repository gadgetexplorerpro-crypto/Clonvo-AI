package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.StudioAccountStore
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.StatusMessageBanner
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricPurpleBright
import com.example.ui.theme.NeonCyanBright

@Composable
fun AuthScreen(
    accountStore: StudioAccountStore,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }

    val githubPrivacyUrl = "https://github.com/clonvo-ai/privacy-and-terms/blob/main/PRIVACY_POLICY_AND_TERMS.md"
    val githubDeleteDataUrl = "https://github.com/clonvo-ai/privacy-and-terms/blob/main/DELETE_ACCOUNT_AND_DATA.md"

    fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            authError = "Unable to open browser link: $url"
        }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF12102E),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_studio_hero),
                contentDescription = "Clonvo AI Voice Cloning Studio Hero",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(ElectricPurpleBright, NeonCyanBright)),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CLONVO AI",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Professional AI Voice Cloning & Multilingual Synthesis Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gmail & Password Login / Sign Up Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !isSignUpMode,
                            onClick = {
                                isSignUpMode = false
                                authError = null
                            },
                            label = {
                                Text(
                                    text = "Login",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_login_chip")
                        )
                        FilterChip(
                            selected = isSignUpMode,
                            onClick = {
                                isSignUpMode = true
                                authError = null
                            },
                            label = {
                                Text(
                                    text = "Sign Up",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_signup_chip")
                        )
                    }

                    Text(
                        text = if (isSignUpMode) "Create Your Clonvo AI Studio Account" else "Sign In with Gmail & Password",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyanBright
                    )

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                authError = null
                            },
                            label = { Text("Your Name (e.g., Alex Studio)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            authError = null
                        },
                        label = { Text("Gmail or Email Address") },
                        placeholder = { Text("yourname@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            authError = null
                        },
                        label = { Text("Password (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = {
                                confirmPasswordInput = it
                                authError = null
                            },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input")
                        )
                    }

                    StatusMessageBanner(
                        statusMessage = null,
                        errorMessage = authError
                    )

                    Button(
                        onClick = {
                            authError = null
                            if (isSignUpMode) {
                                if (passwordInput != confirmPasswordInput) {
                                    authError = "Passwords do not match. Please re-enter your password."
                                    return@Button
                                }
                                val res = accountStore.registerAccount(
                                    displayName = displayName,
                                    email = emailInput,
                                    passwordPlain = passwordInput
                                )
                                res.onSuccess {
                                    onAuthSuccess()
                                }.onFailure { err ->
                                    authError = err.localizedMessage ?: "Sign up failed."
                                }
                            } else {
                                val res = accountStore.loginWithPassword(
                                    email = emailInput,
                                    passwordPlain = passwordInput
                                )
                                res.onSuccess {
                                    onAuthSuccess()
                                }.onFailure { err ->
                                    authError = err.localizedMessage ?: "Login failed."
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPurpleBright,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("email_password_submit_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) "Sign Up & Enter Studio" else "Login with Gmail & Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR ONE-TAP GOOGLE SIGN-IN  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    isLoading = true
                    authError = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isLoading = false
                            authError = err
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonCyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legal, Privacy & Delete Data Section on Login Page
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "By signing up or logging in, you agree to our terms and voice owner consent policy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.testTag("auth_privacy_policy_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Privacy Policy", style = MaterialTheme.typography.labelLarge, color = NeonCyanBright)
                    }

                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)

                    TextButton(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.testTag("auth_terms_service_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Terms of Service", style = MaterialTheme.typography.labelLarge, color = NeonCyanBright)
                    }
                }

                TextButton(
                    onClick = { showDeleteDataDialog = true },
                    modifier = Modifier.testTag("auth_delete_data_url_btn")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Delete Account & Data URL (GitHub)", style = MaterialTheme.typography.labelLarge, color = CoralError)
                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = NeonCyanBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clonvo AI Privacy Policy")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Voice Data Ownership & Security:\n" +
                            "Your voice recordings and cloned voice profiles belong exclusively to you. They are encrypted and linked strictly to your account.\n\n" +
                            "2. Audio Processing:\n" +
                            "Audio samples recorded or uploaded are processed via HTTPS using ElevenLabs API solely to extract acoustic voice features and generate synthetic speech.\n\n" +
                            "3. User Rights & Data Deletion:\n" +
                            "You can delete your cloned voices, generated audio, or your entire account at any time in the app or via our GitHub Data Deletion Request Page.\n\n" +
                            "4. GitHub Privacy URL:\n$githubPrivacyUrl",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubPrivacyUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Policy in GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = NeonCyanBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clonvo AI Terms of Service")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Explicit Voice Owner Consent:\n" +
                            "You may ONLY clone your own voice or voices for which you possess explicit, verified authorization.\n\n" +
                            "2. Prohibition on Deceptive Impersonation:\n" +
                            "Cloning public figures, unauthorized third parties, or using synthesized speech for fraud or harassment is strictly prohibited and will result in immediate ban.\n\n" +
                            "3. AI Watermarking & Disclosure:\n" +
                            "All synthesized speech carries mandatory AI disclosure metadata.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubPrivacyUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurpleBright)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Terms in GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Account & Delete Data URL Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Data URLs")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Clonvo AI provides complete self-service account and data deletion.\n\n" +
                            "• In-App Deletion: Sign in, go to Profile -> Delete Account & All Cloned Voices.\n" +
                            "• External Request: Submit an email request to gadgetexplorerpro@gmail.com or visit our GitHub Data Deletion Guide below.\n\n" +
                            "GitHub Data Deletion URL:\n$githubDeleteDataUrl",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { openUrlInBrowser(githubDeleteDataUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Data Deletion URL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

