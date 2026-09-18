package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.SpeakAiViewModel
import com.example.ui.theme.*

// -------------------------------------------------------------
// Settings Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SpeakAiViewModel,
    onNavigateLanguageSelect: () -> Unit,
    onNavigatePrivacyPolicy: () -> Unit,
    onNavigateTerms: () -> Unit,
    onNavigatePlayStorePreview: () -> Unit,
    onBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    var showLevelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language & Learning Section
            Text("LEARNING PREFERENCES", style = MaterialTheme.typography.labelMedium, color = IndigoPrimary, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItemRow(
                        title = "Native Language",
                        value = "${nativeLang.flag} ${nativeLang.name}",
                        onClick = onNavigateLanguageSelect
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingsItemRow(
                        title = "English Level",
                        value = profile.englishLevel,
                        onClick = { showLevelDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingsItemRow(
                        title = "Accent",
                        value = if (profile.accent == "US") "🇺🇸 American" else "🇬🇧 British",
                        onClick = {
                            val next = if (profile.accent == "US") EnglishAccent.UK else EnglishAccent.US
                            viewModel.updateAccent(next)
                        }
                    )
                }
            }

            // Voice & Speech Section
            Text("AI TEACHER VOICE", style = MaterialTheme.typography.labelMedium, color = IndigoPrimary, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Gender", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = profile.voiceGender == "FEMALE",
                                onClick = { viewModel.updateVoiceGender(VoiceGender.FEMALE) },
                                label = { Text("Female") }
                            )
                            FilterChip(
                                selected = profile.voiceGender == "MALE",
                                onClick = { viewModel.updateVoiceGender(VoiceGender.MALE) },
                                label = { Text("Male") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Speech Speed: ${String.format("%.2fx", profile.voiceSpeed)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Slider(
                        value = profile.voiceSpeed,
                        onValueChange = { viewModel.updateVoiceSpeed(it) },
                        valueRange = 0.6f..1.3f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoPrimary)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.6x (Slower)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.3x (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Appearance Section
            Text("APPEARANCE", style = MaterialTheme.typography.labelMedium, color = IndigoPrimary, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = profile.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            }

            // Legal & Store Section
            Text("ABOUT & LEGAL", style = MaterialTheme.typography.labelMedium, color = IndigoPrimary, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItemRow(
                        title = "Play Store Listing & Description",
                        value = "Preview",
                        onClick = onNavigatePlayStorePreview
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingsItemRow(
                        title = "Privacy Policy",
                        value = "",
                        onClick = onNavigatePrivacyPolicy
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingsItemRow(
                        title = "Terms & Conditions",
                        value = "",
                        onClick = onNavigateTerms
                    )
                }
            }

            Text(
                text = "SpeakAI v1.0.0 • AI-Powered English Learning",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showLevelDialog) {
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            title = { Text("Select English Level") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnglishLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLevel(level)
                                    showLevelDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = profile.englishLevel == level.name,
                                onClick = {
                                    viewModel.updateLevel(level)
                                    showLevelDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(level.displayName, fontWeight = FontWeight.Bold)
                                Text(level.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsItemRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank()) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = IndigoPrimary)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Subscription / Premium Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var isYearly by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpeakAI Premium", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "👑", fontSize = 48.sp)
            Text(
                text = "Accelerate to Fluent English",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Unlock unlimited real-time AI teacher conversations, deep grammar explanations, and phoneme accuracy feedback.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Features Checklist Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumFeatureRow("Unlimited Real-Time AI Voice Conversations")
                    PremiumFeatureRow("Polite Grammar Corrections Explained in Your Native Language")
                    PremiumFeatureRow("Real-Time Pronunciation Clarity & Phonetic Scoring")
                    PremiumFeatureRow("All 10 Real-World Conversation Topics & Scenarios")
                    PremiumFeatureRow("Personalized Daily Lessons & Speaking Quizzes")
                    PremiumFeatureRow("Ad-Free Premium Speaking Experience")
                }
            }

            // Billing Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Monthly", style = MaterialTheme.typography.bodyMedium, fontWeight = if (!isYearly) FontWeight.Bold else FontWeight.Normal)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isYearly,
                    onCheckedChange = { isYearly = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Yearly", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Normal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess
                    ) {
                        Text("SAVE 50%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Plan Pricing Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(IndigoPrimary, CyanAccent))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isYearly) "$4.99 / month" else "$9.99 / month",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IndigoPrimary
                    )
                    Text(
                        text = if (isYearly) "Billed annually ($59.99/yr) • 7 days free" else "Billed monthly • Cancel anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.upgradeToPremium()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("subscribe_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text(
                    text = if (profile.isPremium) "Plan Active (PRO Member)" else "Start 7-Day Free Trial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Subscription automatically renews unless auto-renew is cancelled at least 24 hours before the end of the trial period in Google Play.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PremiumFeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// -------------------------------------------------------------
// Privacy Policy Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Privacy Policy for SpeakAI – AI English Teacher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Last updated: September 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("1. Microphone Access & Voice Data", fontWeight = FontWeight.Bold)
            Text("SpeakAI requires microphone permission strictly to transcribe spoken English for the purpose of conversational practice and pronunciation coaching. Spoken audio is processed in real time and is never stored permanently, recorded for surveillance, or shared with unauthorized third parties.")

            Text("2. AI Teacher Interactions", fontWeight = FontWeight.Bold)
            Text("Text transcriptions are securely transmitted to provide human-like conversational voice feedback and grammar correction. We enforce strict data privacy standards and do not sell user data to advertising brokers.")

            Text("3. Local Storage on Device", fontWeight = FontWeight.Bold)
            Text("Your vocabulary list, grammar mistake history, and daily lesson progress are stored locally on your device using an encrypted local SQLite/Room database. You can clear or reset this data at any time via Settings.")

            Text("4. Children's Privacy & Safety", fontWeight = FontWeight.Bold)
            Text("SpeakAI is an educational language-learning app designed for safe, supportive, and respectful learning environments. We strictly prohibit offensive content and do not collect personal identifiers from children.")
        }
    }
}

// -------------------------------------------------------------
// Terms & Conditions Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Terms of Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text("1. Educational Purpose", fontWeight = FontWeight.Bold)
            Text("SpeakAI is an interactive AI English teacher application designed to facilitate English language practice, pronunciation enhancement, and grammatical understanding.")

            Text("2. User Conduct", fontWeight = FontWeight.Bold)
            Text("Users agree to engage with the AI tutor respectfully and in accordance with standard conversational guidelines. Misuse of the voice interface for malicious purposes is strictly forbidden.")

            Text("3. Subscriptions & Billing", fontWeight = FontWeight.Bold)
            Text("Premium subscriptions are processed securely through Google Play Billing. Subscriptions renew automatically unless canceled at least 24 hours prior to renewal.")
        }
    }
}

// -------------------------------------------------------------
// Play Store Screenshots & Listing Showcase Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStorePreviewScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Play Store Showcase & Metadata", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Google Play Store Listing Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Title (<= 30 chars):", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    Text("SpeakAI: AI English Teacher", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    Text("Short Description (<= 80 chars):", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    Text("Real-time AI voice English teacher. Speak, get instant native corrections.", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider()

                    Text("Full Store Description:", style = MaterialTheme.typography.labelSmall, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Master fluent English speaking with SpeakAI – your personal 24/7 AI English Teacher!\n\n" +
                                "🌟 CORE FEATURES:\n" +
                                "• Real-Time Voice Conversation: Talk naturally with human-like AI voice with very low delay.\n" +
                                "• Instant English Correction: Understands your intent, highlights your exact mistakes, and explains the grammar in your native language (Urdu, Hindi, Arabic, Spanish, French, Punjabi, and more).\n" +
                                "• Repeat & Perfect: AI pronounces the corrected sentence and coaches your pronunciation.\n" +
                                "• 10 Practical Conversation Topics: Daily Life, Job Interviews, Travel English, Restaurants, Doctor Visits, Business English, and more.\n" +
                                "• Personalized Daily Lessons: 5-minute bite-sized lessons with new vocabulary, daily sentences, dialogue practice, and interactive quizzes.\n" +
                                "• How Do I Say This in English?: Instantly translate your thoughts into native, natural English expressions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Key Screenshot Value Propositions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ScreenshotPitchCard("1. Live AI Voice Teacher", "Speak naturally into your phone. AI replies with real-time natural voice.")
            ScreenshotPitchCard("2. Instant Native Grammar Explanations", "Urdu, Hindi, Arabic, Spanish, etc. explanations right below your mistake.")
            ScreenshotPitchCard("3. 10 Immersive Real-Life Scenarios", "Master job interviews, airport travel, restaurant dining, and business negotiations.")
            ScreenshotPitchCard("4. Personalized 5-Minute Daily Lessons", "Maintain your daily streak with words, useful sentences, and quick quizzes.")
        }
    }
}

@Composable
fun ScreenshotPitchCard(title: String, desc: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
