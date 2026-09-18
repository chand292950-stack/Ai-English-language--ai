package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nativeLanguageCode: String = "ur",
    val englishLevel: String = "BEGINNER",
    val accent: String = "US",
    val voiceSpeed: Float = 0.85f,
    val voiceGender: String = "FEMALE",
    val isPremium: Boolean = false,
    val dailyStreak: Int = 3,
    val lastPracticeTimestamp: Long = System.currentTimeMillis(),
    val totalPracticeSeconds: Long = 1420L,
    val totalConversations: Int = 12,
    val totalMistakesCorrected: Int = 28,
    val totalVocabularyLearned: Int = 45,
    val pronunciationScore: Int = 78,
    val fluencyScore: Int = 74,
    val isDarkMode: Boolean = false,
    val isOnboarded: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val modeId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasCorrection: Boolean = false,
    val originalSentence: String? = null,
    val correctedSentence: String? = null,
    val nativeExplanation: String? = null,
    val nativeMeaning: String? = null,
    val repeatInstruction: String? = null
)

@Entity(tableName = "grammar_corrections")
data class GrammarCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalText: String,
    val correctedText: String,
    val nativeExplanation: String,
    val nativeMeaning: String,
    val grammarRule: String,
    val isMastered: Boolean = false,
    val practiceCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocabulary_items")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val nativeTranslation: String,
    val exampleSentence: String,
    val category: String,
    val isMastered: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_lessons")
data class DailyLessonRecordEntity(
    @PrimaryKey val date: String,
    val topicTitle: String,
    val level: String,
    val isCompleted: Boolean = false,
    val quizScore: Int = 0,
    val totalQuestions: Int = 5,
    val timestamp: Long = System.currentTimeMillis()
)
