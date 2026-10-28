package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.StudioAccountStore
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.remote.ElevenLabsService
import com.example.data.repository.ClonvoFirestoreRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CloneVoiceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClonvoViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.AudioStudioEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class ClonvoTab(val label: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "nav_tab_home"),
    CLONE("Clone", Icons.Default.Mic, "nav_tab_clone"),
    STUDIO("Studio", Icons.Default.GraphicEq, "nav_tab_studio"),
    LIBRARY("Library", Icons.Default.LibraryMusic, "nav_tab_library"),
    PROFILE("Profile", Icons.Default.Person, "nav_tab_profile")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { com.google.firebase.FirebaseApp.initializeApp(this) }
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val accountStore = remember(context) { StudioAccountStore(context) }

    val firebaseUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val localSession by accountStore.activeSession.collectAsStateWithLifecycle()

    val fbUser = firebaseUser
    val locUser = localSession

    if (fbUser == null && locUser == null) {
        AuthScreen(
            accountStore = accountStore,
            onAuthSuccess = {}
        )
    } else {
        val activeUid = fbUser?.uid ?: locUser!!.uid
        val activeName = fbUser?.displayName ?: locUser?.displayName ?: "Clonvo Creator"
        val activeEmail = fbUser?.email ?: locUser?.email ?: "creator@clonvo.ai"
        val isCloudSession = fbUser != null

        MainStudioScaffold(
            accountStore = accountStore,
            currentUserId = activeUid,
            userDisplayName = activeName,
            userEmail = activeEmail,
            isCloudFirebaseSession = isCloudSession,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme
        )
    }
}

@Composable
fun MainStudioScaffold(
    accountStore: StudioAccountStore,
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    isCloudFirebaseSession: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ClonvoViewModel = viewModel(
        key = "$currentUserId:$isCloudFirebaseSession",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ClonvoViewModel(
                    repository = ClonvoFirestoreRepository(db),
                    accountStore = accountStore,
                    elevenLabsService = ElevenLabsService(),
                    audioEngine = AudioStudioEngine(app),
                    filesDir = app.filesDir,
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    isCloudFirebaseSession = isCloudFirebaseSession
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableStateOf(ClonvoTab.HOME) }

    if (selectedTab != ClonvoTab.HOME) {
        BackHandler {
            selectedTab = ClonvoTab.HOME
        }
    }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val voicesState by viewModel.voicesState.collectAsStateWithLifecycle()
    val generationsState by viewModel.generationsState.collectAsStateWithLifecycle()
    val cloneState by viewModel.cloneState.collectAsStateWithLifecycle()
    val synthesisState by viewModel.synthesisState.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val liveAmplitudes by viewModel.liveAmplitudes.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playingFilePath by viewModel.playingFilePath.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val voices = (voicesState as? UiState.Success)?.data ?: emptyList()
    val generations = (generationsState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        ClonvoTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        ClonvoTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        ClonvoTab.HOME -> HomeScreen(
                            userProfile = userProfile,
                            voices = voices,
                            generations = generations,
                            isApiKeyConfigured = viewModel.isElevenLabsKeyConfigured,
                            playingFilePath = playingFilePath,
                            isPlaying = isPlaying,
                            onNavigateToClone = { selectedTab = ClonvoTab.CLONE },
                            onNavigateToStudio = { mode ->
                                viewModel.setStudioMode(mode)
                                selectedTab = ClonvoTab.STUDIO
                            },
                            onNavigateToLibrary = { selectedTab = ClonvoTab.LIBRARY },
                            onPlayGeneration = { viewModel.playAudio(it) },
                            onStopPlayback = { viewModel.stopAudio() }
                        )

                        ClonvoTab.CLONE -> CloneVoiceScreen(
                            cloneState = cloneState,
                            isRecording = isRecording,
                            recordingDurationSec = recordingDurationSec,
                            liveAmplitudes = liveAmplitudes,
                            isPlaying = isPlaying,
                            playingFilePath = playingFilePath,
                            isApiKeyConfigured = viewModel.isElevenLabsKeyConfigured,
                            onVoiceNameChange = viewModel::updateCloneVoiceName,
                            onVoiceDescriptionChange = viewModel::updateCloneDescription,
                            onConsentStatementChange = viewModel::updateConsentStatement,
                            onConsentCheckedChange = viewModel::updateConsentChecked,
                            onStartRecording = viewModel::startCloneVoiceRecording,
                            onStopRecording = viewModel::stopMicrophoneRecording,
                            onImportAudioUri = viewModel::importCloneSampleUri,
                            onPlaySample = viewModel::playAudio,
                            onStopPlayback = viewModel::stopAudio,
                            onSubmitClone = viewModel::submitInstantVoiceClone
                        )

                        ClonvoTab.STUDIO -> StudioScreen(
                            synthesisState = synthesisState,
                            voices = voices,
                            creditsRemaining = userProfile?.creditsRemaining ?: 2500L,
                            isRecording = isRecording,
                            isPlaying = isPlaying,
                            playingFilePath = playingFilePath,
                            isApiKeyConfigured = viewModel.isElevenLabsKeyConfigured,
                            onModeChange = viewModel::setStudioMode,
                            onSelectVoice = viewModel::selectVoiceForSynthesis,
                            onTextChange = viewModel::updateSynthesisText,
                            onModelChange = viewModel::updateModelId,
                            onLanguageChange = viewModel::updateLanguageCode,
                            onControlsChange = { stab, sim, style, spd ->
                                viewModel.updateVoiceControls(stab, sim, style, spd)
                            },
                            onStartStsRecording = viewModel::startStsSourceRecording,
                            onStopStsRecording = viewModel::stopMicrophoneRecording,
                            onImportStsUri = viewModel::importStsSourceUri,
                            onGenerate = { viewModel.generateSpeechOrConvert(voices) },
                            onPlayAudio = viewModel::playAudio,
                            onStopAudio = viewModel::stopAudio,
                            onExportAudio = viewModel::exportGeneratedAudio,
                            onShareAudio = viewModel::shareGeneratedAudio
                        )

                        ClonvoTab.LIBRARY -> LibraryScreen(
                            voices = voices,
                            generations = generations,
                            isPlaying = isPlaying,
                            playingFilePath = playingFilePath,
                            onPlayAudio = viewModel::playAudio,
                            onStopAudio = viewModel::stopAudio,
                            onToggleFavorite = viewModel::toggleFavoriteGeneration,
                            onDeleteGeneration = viewModel::deleteGeneration,
                            onDeleteVoice = viewModel::deleteVoiceProfile,
                            onExportAudio = viewModel::exportGeneratedAudio,
                            onShareAudio = viewModel::shareGeneratedAudio,
                            onSubmitAbuseReport = viewModel::submitAbuseReport
                        )

                        ClonvoTab.PROFILE -> ProfileScreen(
                            userProfile = userProfile,
                            userEmail = userEmail,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme,
                            onSubmitAbuseReport = viewModel::submitAbuseReport,
                            onSignOut = {
                                accountStore.clearSession()
                                signOutUser(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            },
                            onDeleteAccountAndVoices = {
                                viewModel.deleteAccountData {
                                    accountStore.clearSession()
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {},
                                        scope = scope
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
