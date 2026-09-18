package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GrammarCorrectionEntity
import com.example.data.local.VocabularyEntity
import com.example.data.model.LanguageCatalog
import com.example.ui.SpeakAiViewModel
import com.example.ui.components.LargeMicrophoneButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 1. English Correction Review Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishCorrectionScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val corrections by viewModel.allCorrections.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    var filterMastered by remember { mutableStateOf<Boolean?>(null) } // null = all, false = pending, true = mastered
    val coroutineScope = rememberCoroutineScope()

    val filtered = remember(corrections, filterMastered) {
        when (filterMastered) {
            true -> corrections.filter { it.isMastered }
            false -> corrections.filter { !it.isMastered }
            null -> corrections
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "English Corrections",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${corrections.size} mistakes analyzed & explained",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMastered == null,
                    onClick = { filterMastered = null },
                    label = { Text("All (${corrections.size})") }
                )
                FilterChip(
                    selected = filterMastered == false,
                    onClick = { filterMastered = false },
                    label = { Text("Needs Practice (${corrections.count { !it.isMastered }})") }
                )
                FilterChip(
                    selected = filterMastered == true,
                    onClick = { filterMastered = true },
                    label = { Text("Mastered (${corrections.count { it.isMastered }})") }
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No corrections found in this filter!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Speak with your AI teacher in Live Chat to get instant personalized feedback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filtered) { item ->
                        CorrectionItemCard(
                            item = item,
                            nativeLanguageName = nativeLang.name,
                            isRtl = nativeLang.isRtl,
                            onPlayAudio = { viewModel.speakText(item.correctedText) },
                            onMarkMastered = {
                                coroutineScope.launch {
                                    viewModel.repository.markCorrectionMastered(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectionItemCard(
    item: GrammarCorrectionEntity,
    nativeLanguageName: String,
    isRtl: Boolean,
    onPlayAudio: () -> Unit,
    onMarkMastered: () -> Unit
) {
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isMastered) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (item.isMastered) "✓ Mastered" else "Practice Needed (${item.practiceCount}x)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isMastered) EmeraldSuccess else AmberWarning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce",
                        tint = IndigoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // You said
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "You said: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoseError,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"${item.originalText}\"",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Correct
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Correct: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"${item.correctedText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Native meaning
            Text(
                text = "$nativeLanguageName: ${item.nativeMeaning}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rule explanation
            Text(
                text = "Rule: ${item.nativeExplanation}",
                style = MaterialTheme.typography.bodySmall,
                color = IndigoLight,
                textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            if (!item.isMastered) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onMarkMastered) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark as Mastered")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Vocabulary Flashcards Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val vocabList by viewModel.allVocabulary.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val categories = remember(vocabList) {
        listOf("All") + vocabList.map { it.category }.distinct()
    }

    val filtered = remember(vocabList, selectedCategory, searchQuery) {
        vocabList.filter {
            (selectedCategory == "All" || it.category == selectedCategory) &&
            (searchQuery.isBlank() || it.word.contains(searchQuery, ignoreCase = true) || it.definition.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vocabulary Bank", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${vocabList.size} essential words for fluency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search words, definitions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Category Chips
            LazyColumn(
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered) { item ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.word,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.phonetic,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyanAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = IndigoPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.partOfSpeech,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IndigoPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.speakText(item.word) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = IndigoPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${nativeLang.name}: ${item.nativeTranslation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Medium,
                                textAlign = if (nativeLang.isRtl) TextAlign.End else TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Example: \"${item.exampleSentence}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            viewModel.toggleVocabMastered(item.id, !item.isMastered)
                                        }
                                    }
                                ) {
                                    Checkbox(
                                        checked = item.isMastered,
                                        onCheckedChange = { checked ->
                                            coroutineScope.launch {
                                                viewModel.toggleVocabMastered(item.id, checked)
                                            }
                                        }
                                    )
                                    Text(
                                        text = "Mastered",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Pronunciation Coach Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val targetSentence by viewModel.pronunciationTarget.collectAsState()
    val score by viewModel.pronunciationScore.collectAsState()
    val feedback by viewModel.pronunciationFeedback.collectAsState()
    val isListening by viewModel.speechManager.isUserListening.collectAsState()
    val recognizedText by viewModel.speechManager.recognizedText.collectAsState()

    val sampleSentences = listOf(
        "Practice makes speaking natural and effortless.",
        "Could you please tell me how to get to the airport?",
        "I am looking forward to our upcoming meeting.",
        "The weather today is unusually pleasant and breezy.",
        "Effective communication is the key to global opportunity."
    )

    // Evaluate whenever recognition completes
    LaunchedEffect(recognizedText, isListening) {
        if (!isListening && recognizedText.isNotBlank()) {
            viewModel.evaluatePronunciation(recognizedText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pronunciation Coach", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Listen to the native voice, then tap the mic and repeat the sentence clearly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target sentence card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(IndigoPrimary, CyanAccent))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Target Sentence",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$targetSentence\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.speakText(targetSentence, 1.0f) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen Normal")
                            }

                            OutlinedButton(
                                onClick = { viewModel.speakText(targetSentence, 0.65f) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen Slow")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Score feedback
                if (score != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (score!! >= 75) EmeraldSuccess.copy(alpha = 0.12f) else AmberWarning.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pronunciation Clarity Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (score!! >= 75) EmeraldSuccess else AmberWarning,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$score%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (score!! >= 75) EmeraldSuccess else AmberWarning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedback ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            if (recognizedText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AI Heard: \"$recognizedText\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select other sentences
                Text(
                    text = "Or choose another sentence:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sampleSentences) { sent ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPronunciationTarget(sent) }
                        ) {
                            Text(
                                text = sent,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Mic Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                LargeMicrophoneButton(
                    isListening = isListening,
                    isAiSpeaking = false,
                    onClick = { viewModel.toggleMic() }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isListening) "Listening... tap to stop" else "Tap mic and repeat the sentence",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isListening) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 4. "How Do I Say This in English?" Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val query by viewModel.translationQuery.collectAsState()
    val result by viewModel.translationResult.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("How Do I Say This?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("From ${nativeLang.name} to natural English", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                    }
                },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Type or think of a phrase in ${nativeLang.name} (${nativeLang.nativeName}), and the AI teacher will give you the exact natural English sentence with correct pronunciation and polite nuances.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setTranslationQuery(it) },
                placeholder = { Text("e.g. 'میں بازار جا رہا ہوں' or 'Please help me'") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translation_query_input")
            )

            Button(
                onClick = { viewModel.performTranslation() },
                enabled = query.isNotBlank() && !isTranslating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("translate_button")
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Translating...")
                } else {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How to say in English?")
                }
            }

            result?.let { res ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(IndigoPrimary, CyanAccent))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Natural Native Expression",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { viewModel.speakText(res.naturalEnglish) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = IndigoPrimary)
                            }
                        }

                        Text(
                            text = "\"${res.naturalEnglish}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Everyday Simple Alternative:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"${res.simpleAlternative}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Usage Note & Cultural Etiquette:",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoLight
                        )
                        Text(
                            text = res.grammarNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Native Dialogue Context:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                                Text(
                                    text = res.exampleDialogue,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
