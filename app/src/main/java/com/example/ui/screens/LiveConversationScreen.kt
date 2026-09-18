package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ConversationModeType
import com.example.data.model.LanguageCatalog
import com.example.data.model.VoiceGender
import com.example.ui.SpeakAiViewModel
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CorrectionCardView
import com.example.ui.components.LargeMicrophoneButton
import com.example.ui.components.TeacherAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveConversationScreen(
    modeId: String,
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mode = remember(modeId) { ConversationModeType.fromId(modeId) }
    val profile by viewModel.userProfile.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.speechManager.isUserListening.collectAsState()
    val isAiSpeaking by viewModel.speechManager.isAiSpeaking.collectAsState()
    val amplitude by viewModel.speechManager.audioAmplitude.collectAsState()
    val recognizedText by viewModel.speechManager.recognizedText.collectAsState()
    val isProcessingAi by viewModel.isProcessingAi.collectAsState()
    val latestFeedback by viewModel.latestFeedback.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    var showTextInput by remember { mutableStateOf(false) }
    var typedMessage by remember { mutableStateOf("") }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Permission launcher for microphone
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleMic()
        }
    }

    // Auto-scroll transcript to bottom
    LaunchedEffect(messages.size, recognizedText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Start conversation session on launch
    LaunchedEffect(mode) {
        viewModel.startConversation(mode)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopConversation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAiSpeaking) "AI is speaking..."
                            else if (isListening) "Listening to you..."
                            else "Speak naturally",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListening) CyanAccent else if (isAiSpeaking) IndigoLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Voice Speed Pill Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { showSpeedDialog = true }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.2fx", profile.voiceSpeed),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Voice Gender Toggle
                    IconButton(
                        onClick = {
                            val nextGender = if (profile.voiceGender == "FEMALE") VoiceGender.MALE else VoiceGender.FEMALE
                            viewModel.updateVoiceGender(nextGender)
                        }
                    ) {
                        Icon(
                            imageVector = if (profile.voiceGender == "MALE") Icons.Default.Face else Icons.Default.Person,
                            contentDescription = "Toggle Teacher Voice",
                            tint = IndigoPrimary
                        )
                    }

                    // Text Input Toggle
                    IconButton(onClick = { showTextInput = !showTextInput }) {
                        Icon(
                            imageVector = if (showTextInput) Icons.Default.Mic else Icons.Default.Keyboard,
                            contentDescription = "Toggle text input"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Optional keyboard text input fallback
                    AnimatedVisibility(visible = showTextInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typedMessage,
                                onValueChange = { typedMessage = it },
                                placeholder = { Text("Type English sentence...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("typed_message_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (typedMessage.isNotBlank()) {
                                        viewModel.sendTextMessage(typedMessage)
                                        typedMessage = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Main Controls: Replay, Large Mic, Slow Speech
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Replay AI Answer Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.replayAiSpeech(slow = false) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("replay_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Replay AI Answer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Replay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Large Mic Button
                        LargeMicrophoneButton(
                            isListening = isListening,
                            isAiSpeaking = isAiSpeaking,
                            onClick = {
                                if (hasMicPermission) {
                                    viewModel.toggleMic()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )

                        // Slow Speech Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.replayAiSpeech(slow = true) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("slow_speech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = "Slow Speech",
                                    tint = CyanAccent
                                )
                            }
                            Text(
                                text = "Slow",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isListening) "Tap to finish speaking" else "Tap microphone to speak",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isListening) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live AI Teacher Visualizer Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeacherAvatar(
                    isAiSpeaking = isAiSpeaking,
                    isListening = isListening,
                    voiceGender = profile.voiceGender,
                    size = 72.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                AudioWaveformVisualizer(
                    isSpeaking = isAiSpeaking,
                    isListening = isListening,
                    amplitude = amplitude
                )
            }

            // Real-time live speech recognition feedback text
            AnimatedVisibility(
                visible = isListening && recognizedText.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = CyanAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hearing: \"$recognizedText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanAccent
                        )
                    }
                }
            }

            // AI Processing indicator
            AnimatedVisibility(visible = isProcessingAi) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                        color = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Teacher is preparing feedback...",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoPrimary
                    )
                }
            }

            // Error notice banner
            errorMessage?.let { err ->
                Surface(
                    color = RoseError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseError,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = RoseError)
                        }
                    }
                }
            }

            // Chat Messages & Correction Cards
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    if (msg.role == "user") {
                        UserChatBubble(msg.content)
                    } else {
                        AssistantChatBubble(
                            message = msg,
                            nativeLanguageName = nativeLang.name,
                            isRtl = nativeLang.isRtl,
                            onPlayAudio = { viewModel.speakText(msg.content) },
                            onRepeatClick = {
                                msg.correctedSentence?.let {
                                    viewModel.speechManager.speak("Repeat after me: $it")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Speed selection dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Teacher Speech Speed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val speeds = listOf(
                        0.70f to "Slow (0.70x) - Best for Beginners",
                        0.85f to "Gentle (0.85x) - Clear & Patient",
                        1.00f to "Normal (1.00x) - Everyday Speed",
                        1.20f to "Fast (1.20x) - Advanced Native Pace"
                    )
                    speeds.forEach { (speed, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateVoiceSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = kotlin.math.abs(profile.voiceSpeed - speed) < 0.05f,
                                onClick = {
                                    viewModel.updateVoiceSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun UserChatBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp).copy(bottomEnd = CornerSize(4.dp)),
            color = IndigoPrimary,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun AssistantChatBubble(
    message: ChatMessageEntity,
    nativeLanguageName: String,
    isRtl: Boolean,
    onPlayAudio: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        if (message.hasCorrection && !message.correctedSentence.isNullOrBlank()) {
            CorrectionCardView(
                originalSentence = message.originalSentence ?: "",
                correctedSentence = message.correctedSentence,
                nativeExplanation = message.nativeExplanation ?: "",
                nativeMeaning = message.nativeMeaning ?: "",
                nativeLanguageName = nativeLanguageName,
                isRtl = isRtl,
                onRepeatClick = onRepeatClick,
                onPlayCorrection = onPlayAudio
            )
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp).copy(bottomStart = CornerSize(4.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
