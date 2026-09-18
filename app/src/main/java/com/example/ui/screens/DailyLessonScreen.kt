package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.LanguageCatalog
import com.example.data.repository.DailyLessonData
import com.example.ui.SpeakAiViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLessonScreen(
    viewModel: SpeakAiViewModel,
    onBack: () -> Unit
) {
    val dailyLesson by viewModel.dailyLesson.collectAsState()
    val currentQIndex by viewModel.quizCurrentIndex.collectAsState()
    val selectedOption by viewModel.quizSelectedOption.collectAsState()
    val quizScore by viewModel.quizScore.collectAsState()
    val quizCompleted by viewModel.quizCompleted.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val nativeLang = remember(profile.nativeLanguageCode) {
        LanguageCatalog.getByCode(profile.nativeLanguageCode)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Vocabulary, 1: Sentences, 2: Dialogue, 3: Quiz

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = dailyLesson?.topicTitle ?: "Today's Daily Lesson",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Level: ${profile.englishLevel} • 5-min boost",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
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
        ) {
            // Tab row: 4 steps
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. Words") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. Sentences") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("3. Dialogue") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("4. Short Test") }
                )
            }

            dailyLesson?.let { lesson ->
                when (selectedTab) {
                    0 -> LessonVocabTab(lesson, viewModel, nativeLang.name, onNext = { selectedTab = 1 })
                    1 -> LessonSentencesTab(lesson, viewModel, nativeLang.name, onNext = { selectedTab = 2 })
                    2 -> LessonDialogueTab(lesson, viewModel, onNext = { selectedTab = 3 })
                    3 -> LessonQuizTab(
                        lesson = lesson,
                        currentIndex = currentQIndex,
                        selectedOption = selectedOption,
                        quizScore = quizScore,
                        quizCompleted = quizCompleted,
                        onSelectOption = { viewModel.selectQuizOption(it) },
                        onNext = { viewModel.nextQuizQuestion() },
                        onRestart = { viewModel.loadDailyLesson() },
                        onFinish = onBack
                    )
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IndigoPrimary)
            }
        }
    }
}

@Composable
fun LessonVocabTab(
    lesson: DailyLessonData,
    viewModel: SpeakAiViewModel,
    nativeLanguageName: String,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Step 1: 4 Essential Words for Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Listen to pronunciation and understand native meaning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(lesson.vocabulary) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.word,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = item.phonetic, style = MaterialTheme.typography.bodySmall, color = CyanAccent)
                                }
                                IconButton(
                                    onClick = { viewModel.speakText(item.word) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play", tint = IndigoPrimary)
                                }
                            }
                            Text(text = item.meaningEnglish, style = MaterialTheme.typography.bodySmall)
                            Text(text = "$nativeLanguageName: ${item.meaningNative}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "\"${item.example}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            Text("Next: 4 Useful Sentences", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LessonSentencesTab(
    lesson: DailyLessonData,
    viewModel: SpeakAiViewModel,
    nativeLanguageName: String,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Step 2: 4 Everyday Useful Sentences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Natural native structures you can use immediately in conversation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(lesson.usefulSentences) { s ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.situation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.speakText(s.english) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play", tint = IndigoPrimary)
                                }
                            }
                            Text(
                                text = "\"${s.english}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$nativeLanguageName: ${s.nativeMeaning}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            Text("Next: Practice Dialogue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LessonDialogueTab(
    lesson: DailyLessonData,
    viewModel: SpeakAiViewModel,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "Step 3: Realistic Scenario Dialogue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = lesson.dialogueScenario.scenario,
                style = MaterialTheme.typography.bodySmall,
                color = CyanAccent,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lesson.dialogueScenario.lines) { line ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (line.speaker.contains("You")) IndigoPrimary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.speaker,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (line.speaker.contains("You")) IndigoPrimary else CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = line.englishText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = line.nativeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.speakText(line.englishText) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play", tint = IndigoPrimary)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
            Text("Next: 4-Question Interactive Short Test", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LessonQuizTab(
    lesson: DailyLessonData,
    currentIndex: Int,
    selectedOption: Int?,
    quizScore: Int,
    quizCompleted: Boolean,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    if (quizCompleted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🎉", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lesson Completed!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your Quiz Score: $quizScore / ${lesson.testQuestions.size}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (quizScore >= 3) EmeraldSuccess else AmberWarning
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fluency score boosted by +2%! Your daily streak is preserved.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Back to Home", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRestart,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Practice Again")
            }
        }
    } else {
        val currentQ = lesson.testQuestions.getOrNull(currentIndex)
        if (currentQ == null) return

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${lesson.testQuestions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                    Text(
                        text = "Score: $quizScore",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1f) / lesson.testQuestions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = IndigoPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentQ.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                currentQ.options.forEachIndexed { idx, option ->
                    val isSelected = selectedOption == idx
                    val isCorrect = idx == currentQ.correctIndex
                    val showAnswer = selectedOption != null

                    val bgColor = when {
                        showAnswer && isCorrect -> EmeraldSuccess.copy(alpha = 0.18f)
                        showAnswer && isSelected && !isCorrect -> RoseError.copy(alpha = 0.18f)
                        isSelected -> IndigoPrimary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val borderColor = when {
                        showAnswer && isCorrect -> EmeraldSuccess
                        showAnswer && isSelected && !isCorrect -> RoseError
                        else -> Color.Transparent
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = if (showAnswer) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(borderColor, borderColor))) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable(enabled = selectedOption == null) {
                                onSelectOption(idx)
                            }
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A' + idx)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (showAnswer && isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                            } else if (showAnswer && isSelected) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = RoseError)
                            }
                        }
                    }
                }

                if (selectedOption != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Explanation: ${currentQ.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onNext,
                enabled = selectedOption != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("next_quiz_button")
            ) {
                Text(
                    text = if (currentIndex + 1 < lesson.testQuestions.size) "Next Question" else "Complete Lesson",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
