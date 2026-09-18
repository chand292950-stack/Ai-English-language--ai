package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        ChatMessageEntity::class,
        GrammarCorrectionEntity::class,
        VocabularyEntity::class,
        DailyLessonRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatDao(): ChatDao
    abstract fun grammarCorrectionDao(): GrammarCorrectionDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun dailyLessonDao(): DailyLessonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speakai_learning.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            prepopulateData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateData(db: AppDatabase) {
            db.userProfileDao().insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    nativeLanguageCode = "ur",
                    englishLevel = "BEGINNER",
                    accent = "US",
                    voiceSpeed = 0.85f,
                    voiceGender = "FEMALE",
                    isPremium = false,
                    dailyStreak = 4,
                    lastPracticeTimestamp = System.currentTimeMillis(),
                    totalPracticeSeconds = 2450L,
                    totalConversations = 14,
                    totalMistakesCorrected = 32,
                    totalVocabularyLearned = 56,
                    pronunciationScore = 82,
                    fluencyScore = 76,
                    isDarkMode = false,
                    isOnboarded = true
                )
            )

            val initialVocab = listOf(
                VocabularyEntity(
                    word = "Accomplish",
                    phonetic = "/əˈkɑːm.plɪʃ/",
                    partOfSpeech = "verb",
                    definition = "To achieve or complete successfully",
                    nativeTranslation = "پورا کرنا / انجام دینا",
                    exampleSentence = "With daily practice, you will accomplish fluent English speaking.",
                    category = "Daily Essentials",
                    isMastered = false
                ),
                VocabularyEntity(
                    word = "Confidence",
                    phonetic = "/ˈkɑːn.fə.dəns/",
                    partOfSpeech = "noun",
                    definition = "A feeling of self-assurance arising from one's appreciation of one's abilities",
                    nativeTranslation = "اعتماد / خود اعتمادی",
                    exampleSentence = "Speaking with the AI tutor builds speaking confidence fast.",
                    category = "Daily Essentials",
                    isMastered = true
                ),
                VocabularyEntity(
                    word = "Reservation",
                    phonetic = "/ˌrez.ɚˈveɪ.ʃən/",
                    partOfSpeech = "noun",
                    definition = "An arrangement whereby something is held for someone",
                    nativeTranslation = "بکنگ / ریزرویشن",
                    exampleSentence = "I would like to make a table reservation for two tonight.",
                    category = "Travel & Food",
                    isMastered = false
                ),
                VocabularyEntity(
                    word = "Opportunity",
                    phonetic = "/ˌɑː.pɚˈtuː.nə.t̬i/",
                    partOfSpeech = "noun",
                    definition = "A set of circumstances that makes it possible to do something",
                    nativeTranslation = "موقع / چانس",
                    exampleSentence = "Learning English opens up incredible career opportunities.",
                    category = "Career & Work",
                    isMastered = false
                ),
                VocabularyEntity(
                    word = "Prescription",
                    phonetic = "/prɪˈskrɪp.ʃən/",
                    partOfSpeech = "noun",
                    definition = "An instruction written by a medical practitioner that authorizes a patient to be provided a medicine",
                    nativeTranslation = "نسخہ / ڈاکٹر کا نسخہ",
                    exampleSentence = "The doctor gave me a prescription for my cough.",
                    category = "Health & Clinic",
                    isMastered = false
                ),
                VocabularyEntity(
                    word = "Negotiate",
                    phonetic = "/nəˈɡoʊ.ʃi.eɪt/",
                    partOfSpeech = "verb",
                    definition = "Try to reach an agreement or compromise by discussion",
                    nativeTranslation = "بات چیت کرنا / طے کرنا",
                    exampleSentence = "We need to negotiate the contract terms with the client.",
                    category = "Career & Work",
                    isMastered = false
                ),
                VocabularyEntity(
                    word = "Fluency",
                    phonetic = "/ˈfluː.ən.si/",
                    partOfSpeech = "noun",
                    definition = "The quality or condition of being able to speak easily and accurately",
                    nativeTranslation = "روانی / سلاست",
                    exampleSentence = "Fluency comes through natural conversational repetition.",
                    category = "Pronunciation",
                    isMastered = true
                )
            )
            db.vocabularyDao().insertAll(initialVocab)

            val initialCorrections = listOf(
                GrammarCorrectionEntity(
                    originalText = "Yesterday I go market.",
                    correctedText = "Yesterday I went to the market.",
                    nativeExplanation = "ماضی (Past tense) کی بات کرتے ہوئے 'go' کے بجائے دوسری فارم 'went' استعمال کی جاتی ہے اور منزل کے لیے 'to the' لگایا جاتا ہے۔",
                    nativeMeaning = "کل میں بازار گیا تھا۔",
                    grammarRule = "Past Simple Tense (go -> went)",
                    isMastered = false,
                    practiceCount = 2
                ),
                GrammarCorrectionEntity(
                    originalText = "She do not like tea.",
                    correctedText = "She does not like tea.",
                    nativeExplanation = "تیسرے شخص واحد (He, She, It) کے ساتھ نفی کے جملے میں 'do not' کے بجائے 'does not' لگتا ہے۔",
                    nativeMeaning = "وہ چائے پسند نہیں کرتی ہے۔",
                    grammarRule = "Subject-Verb Agreement (Third person singular)",
                    isMastered = true,
                    practiceCount = 4
                ),
                GrammarCorrectionEntity(
                    originalText = "I am agree with your opinion.",
                    correctedText = "I agree with your opinion.",
                    nativeExplanation = "انگریزی میں 'agree' خود ایک فعل (verb) ہے، اس لیے اس کے ساتھ 'am' لگانے کی ضرورت نہیں ہوتی۔",
                    nativeMeaning = "میں آپ کی رائے سے متفق ہوں۔",
                    grammarRule = "Verb Usage ('agree' is a verb, not an adjective)",
                    isMastered = false,
                    practiceCount = 1
                )
            )
            for (c in initialCorrections) {
                db.grammarCorrectionDao().insertCorrection(c)
            }
        }
    }
}
