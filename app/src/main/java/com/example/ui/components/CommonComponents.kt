package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AudioWaveformVisualizer(
    isSpeaking: Boolean,
    isListening: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAnim"
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 9
        val activeColor = when {
            isListening -> CyanAccent
            isSpeaking -> IndigoLight
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        }

        for (i in 0 until barCount) {
            val offsetMultiplier = kotlin.math.sin((i + 1) * 0.7f).coerceIn(0.2f, 1f)
            val dynamicHeight = when {
                isListening -> (12.dp + (32.dp * amplitude * offsetMultiplier))
                isSpeaking -> (10.dp + (26.dp * waveAnim * offsetMultiplier))
                else -> 8.dp
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor)
            )
        }
    }
}

@Composable
fun TeacherAvatar(
    isAiSpeaking: Boolean,
    isListening: Boolean,
    voiceGender: String = "FEMALE",
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAiSpeaking || isListening) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloColor = when {
        isAiSpeaking -> IndigoPrimary
        isListening -> CyanAccent
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.35f)
    ) {
        // Outer glowing halo
        Box(
            modifier = Modifier
                .size(size * pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(haloColor.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )

        // Middle circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(IndigoPrimary, CyanAccent)
                    )
                )
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (voiceGender.contains("MALE", ignoreCase = true)) Icons.Default.Face else Icons.Default.Person,
                contentDescription = "AI English Teacher",
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }

        // Status badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAiSpeaking) IndigoPrimary else if (isListening) CyanAccent else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 8.dp)
        ) {
            Text(
                text = when {
                    isAiSpeaking -> "AI Speaking..."
                    isListening -> "Listening to you..."
                    else -> "Ready to chat"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isAiSpeaking || isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun LargeMicrophoneButton(
    isListening: Boolean,
    isAiSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "micScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(100.dp)
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.25f))
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(76.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isListening) Brush.linearGradient(listOf(CyanAccent, Color(0xFF0284C7)))
                    else Brush.linearGradient(listOf(IndigoPrimary, Color(0xFF4338CA)))
                )
                .testTag("mic_toggle_button")
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Speak into microphone",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
fun CorrectionCardView(
    originalSentence: String,
    correctedSentence: String,
    nativeExplanation: String,
    nativeMeaning: String,
    nativeLanguageName: String,
    isRtl: Boolean,
    onRepeatClick: () -> Unit,
    onPlayCorrection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(RoseError.copy(alpha = 0.6f), EmeraldSuccess.copy(alpha = 0.6f)))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("correction_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Correction",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "English Correction & Feedback",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onPlayCorrection,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play correction audio",
                        tint = IndigoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original sentence with red strike / badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RoseError.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "You said:",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoseError,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"$originalSentence\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Corrected sentence with emerald badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldSuccess.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Correct:",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"$correctedSentence\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (nativeMeaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$nativeLanguageName meaning: $nativeMeaning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (nativeExplanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tip: $nativeExplanation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onRepeatClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IndigoPrimary.copy(alpha = 0.15f),
                    contentColor = IndigoPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repeat_correction_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Repeat Corrected Sentence",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakAiTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
