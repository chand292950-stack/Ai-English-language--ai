package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class TeacherFeedback(
    val replyText: String,
    val spokenAudioText: String,
    val hasCorrection: Boolean = false,
    val originalSentence: String? = null,
    val correctedSentence: String? = null,
    val nativeExplanation: String? = null,
    val nativeMeaning: String? = null,
    val repeatInstruction: String? = null
)

data class TranslationResult(
    val naturalEnglish: String,
    val simpleAlternative: String,
    val nativeMeaning: String,
    val grammarNote: String,
    val exampleDialogue: String
)

data class DailyLessonData(
    val topicTitle: String,
    val level: EnglishLevel,
    val dateString: String,
    val vocabulary: List<LessonVocab>,
    val usefulSentences: List<LessonSentence>,
    val dialogueScenario: LessonDialogue,
    val speakingChallenge: SpeakingChallenge,
    val testQuestions: List<LessonQuizQuestion>
)

data class LessonVocab(
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val meaningEnglish: String,
    val meaningNative: String,
    val example: String
)

data class LessonSentence(
    val english: String,
    val nativeMeaning: String,
    val situation: String
)

data class LessonDialogue(
    val scenario: String,
    val lines: List<DialogueLine>
)

data class DialogueLine(
    val speaker: String,
    val englishText: String,
    val nativeText: String
)

data class SpeakingChallenge(
    val targetSentence: String,
    val nativeMeaning: String,
    val phonemeTips: String,
    val difficulty: String
)

data class LessonQuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class SpeakAiRepository(
    private val db: AppDatabase
) {
    private val userProfileDao = db.userProfileDao()
    private val chatDao = db.chatDao()
    private val grammarCorrectionDao = db.grammarCorrectionDao()
    private val vocabularyDao = db.vocabularyDao()
    private val dailyLessonDao = db.dailyLessonDao()

    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()
    val allVocabulary: Flow<List<VocabularyEntity>> = vocabularyDao.getAllVocabulary()
    val allCorrections: Flow<List<GrammarCorrectionEntity>> = grammarCorrectionDao.getAllCorrections()
    val pendingCorrections: Flow<List<GrammarCorrectionEntity>> = grammarCorrectionDao.getPendingCorrections()

    fun getConversationMessages(conversationId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForConversation(conversationId)
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun recordPracticeSession(durationSeconds: Long, conversationMode: String) {
        val current = userProfileDao.getUserProfileSync() ?: UserProfileEntity()
        val newStreak = calculateStreak(current.lastPracticeTimestamp, current.dailyStreak)
        val updated = current.copy(
            totalPracticeSeconds = current.totalPracticeSeconds + durationSeconds,
            totalConversations = current.totalConversations + 1,
            dailyStreak = newStreak,
            lastPracticeTimestamp = System.currentTimeMillis()
        )
        userProfileDao.insertOrUpdateProfile(updated)
    }

    suspend fun markCorrectionMastered(correctionId: Long) {
        grammarCorrectionDao.markAsMastered(correctionId)
        val current = userProfileDao.getUserProfileSync() ?: return
        userProfileDao.insertOrUpdateProfile(
            current.copy(
                fluencyScore = (current.fluencyScore + 1).coerceAtMost(99),
                totalMistakesCorrected = current.totalMistakesCorrected + 1
            )
        )
    }

    suspend fun toggleVocabMastered(id: Long, mastered: Boolean) {
        vocabularyDao.setMastered(id, mastered)
        val current = userProfileDao.getUserProfileSync() ?: return
        if (mastered) {
            userProfileDao.insertOrUpdateProfile(
                current.copy(
                    totalVocabularyLearned = current.totalVocabularyLearned + 1
                )
            )
        }
    }

    suspend fun saveChatMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    // --- Core AI English Teacher Engine ---
    suspend fun processUserSpeech(
        userInput: String,
        conversationId: String,
        mode: ConversationModeType,
        profile: UserProfileEntity
    ): TeacherFeedback = withContext(Dispatchers.IO) {
        // First record user message
        chatDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                modeId = mode.id,
                role = "user",
                content = userInput
            )
        )

        val nativeLang = LanguageCatalog.getByCode(profile.nativeLanguageCode)
        val level = profile.englishLevel

        // Try Gemini API if valid key is available
        if (GeminiApiClient.hasValidApiKey()) {
            try {
                val prompt = buildTeacherPrompt(userInput, mode, profile, nativeLang)
                val geminiReq = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.6f,
                        maxOutputTokens = 600
                    )
                )
                val response = GeminiApiClient.service.generateContent(
                    BuildConfig.GEMINI_API_KEY,
                    geminiReq
                )
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    val parsed = parseTeacherResponse(text, userInput, nativeLang)
                    saveTeacherMessage(conversationId, mode, parsed)
                    return@withContext parsed
                }
            } catch (e: Exception) {
                // Fallback gracefully to intelligent local teacher engine
            }
        }

        // Local Smart AI English Teacher Engine
        val localFeedback = generateLocalTeacherFeedback(userInput, mode, profile, nativeLang)
        saveTeacherMessage(conversationId, mode, localFeedback)
        return@withContext localFeedback
    }

    private suspend fun saveTeacherMessage(
        conversationId: String,
        mode: ConversationModeType,
        feedback: TeacherFeedback
    ) {
        chatDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                modeId = mode.id,
                role = "assistant",
                content = feedback.replyText,
                hasCorrection = feedback.hasCorrection,
                originalSentence = feedback.originalSentence,
                correctedSentence = feedback.correctedSentence,
                nativeExplanation = feedback.nativeExplanation,
                nativeMeaning = feedback.nativeMeaning,
                repeatInstruction = feedback.repeatInstruction
            )
        )

        if (feedback.hasCorrection && !feedback.correctedSentence.isNullOrBlank()) {
            grammarCorrectionDao.insertCorrection(
                GrammarCorrectionEntity(
                    originalText = feedback.originalSentence ?: "",
                    correctedText = feedback.correctedSentence,
                    nativeExplanation = feedback.nativeExplanation ?: "",
                    nativeMeaning = feedback.nativeMeaning ?: "",
                    grammarRule = "Conversational English Rule"
                )
            )
        }
    }

    private fun buildTeacherPrompt(
        userInput: String,
        mode: ConversationModeType,
        profile: UserProfileEntity,
        nativeLang: SupportedLanguage
    ): String {
        return """
            You are SpeakAI, an expert, warm, and highly supportive AI English Teacher.
            The user is speaking English to you.
            User Profile:
            - Native Language: ${nativeLang.name} (${nativeLang.nativeName})
            - English Level: ${profile.englishLevel}
            - Current Mode: ${mode.title} (${mode.aiPersona})

            TEACHER GUIDELINES:
            1. Keep tone extremely polite, patient, and motivating. Never embarrass the user.
            2. If user is BEGINNER, use simple words and ask 1 clear short question.
            3. Check if user's input contains grammar, tense, vocabulary, or preposition mistakes.
            
            OUTPUT FORMAT RULES:
            If user made a mistake, reply strictly in this format:
            [CORRECTION]
            ORIGINAL: <user's original phrase>
            CORRECTED: <natural, corrected English sentence>
            NATIVE_EXPLANATION: <explain the grammar rule in ${nativeLang.name}>
            NATIVE_MEANING: <meaning of the corrected sentence in ${nativeLang.name}>
            REPEAT_PROMPT: Now repeat: <corrected sentence>
            [/CORRECTION]
            [RESPONSE]
            <Your friendly English response continuing the ${mode.title} conversation, asking ONE question.>
            [/RESPONSE]

            If user made NO mistake, reply strictly as:
            [RESPONSE]
            <Your encouraging conversational answer and ONE next question in English.>
            [/RESPONSE]

            User said: "$userInput"
        """.trimIndent()
    }

    private fun parseTeacherResponse(
        rawText: String,
        originalInput: String,
        nativeLang: SupportedLanguage
    ): TeacherFeedback {
        val hasCorrection = rawText.contains("[CORRECTION]")
        if (hasCorrection) {
            val corrBlock = rawText.substringAfter("[CORRECTION]").substringBefore("[/CORRECTION]")
            val responseBlock = rawText.substringAfter("[RESPONSE]").substringBefore("[/RESPONSE]").trim()

            val original = corrBlock.lines().find { it.startsWith("ORIGINAL:") }?.substringAfter("ORIGINAL:")?.trim() ?: originalInput
            val corrected = corrBlock.lines().find { it.startsWith("CORRECTED:") }?.substringAfter("CORRECTED:")?.trim() ?: originalInput
            val explanation = corrBlock.lines().find { it.startsWith("NATIVE_EXPLANATION:") }?.substringAfter("NATIVE_EXPLANATION:")?.trim() ?: ""
            val meaning = corrBlock.lines().find { it.startsWith("NATIVE_MEANING:") }?.substringAfter("NATIVE_MEANING:")?.trim() ?: ""
            val repeat = corrBlock.lines().find { it.startsWith("REPEAT_PROMPT:") }?.substringAfter("REPEAT_PROMPT:")?.trim() ?: "Now repeat: $corrected"

            val displayReply = "You said: \"$original\"\nCorrect: \"$corrected\"\n${nativeLang.name} meaning: $meaning\n\n$repeat\n\n$responseBlock"
            val spokenAudio = "You said: $original. Correct: $corrected. Now repeat: $corrected. ... $responseBlock"

            return TeacherFeedback(
                replyText = displayReply,
                spokenAudioText = spokenAudio,
                hasCorrection = true,
                originalSentence = original,
                correctedSentence = corrected,
                nativeExplanation = explanation,
                nativeMeaning = meaning,
                repeatInstruction = repeat
            )
        } else {
            val cleanReply = if (rawText.contains("[RESPONSE]")) {
                rawText.substringAfter("[RESPONSE]").substringBefore("[/RESPONSE]").trim()
            } else {
                rawText.trim()
            }
            return TeacherFeedback(
                replyText = cleanReply,
                spokenAudioText = cleanReply,
                hasCorrection = false
            )
        }
    }

    // --- Built-in Intelligent Natural Language Teacher Engine ---
    private fun generateLocalTeacherFeedback(
        userInput: String,
        mode: ConversationModeType,
        profile: UserProfileEntity,
        nativeLang: SupportedLanguage
    ): TeacherFeedback {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        // Detect common English errors
        val commonCorrection = checkCommonMistakes(lower, trimmed, nativeLang)
        if (commonCorrection != null) {
            val followup = getContextualFollowup(mode, profile.englishLevel)
            val fullDisplay = "You said: \"${commonCorrection.original}\"\n" +
                    "Correct: \"${commonCorrection.corrected}\"\n" +
                    "${nativeLang.name} meaning: ${commonCorrection.meaning}\n" +
                    "Rule: ${commonCorrection.explanation}\n\n" +
                    "Now repeat: \"${commonCorrection.corrected}\"\n\n" +
                    followup

            val spoken = "You said: ${commonCorrection.original}. Correct: ${commonCorrection.corrected}. Now repeat: ${commonCorrection.corrected}. $followup"

            return TeacherFeedback(
                replyText = fullDisplay,
                spokenAudioText = spoken,
                hasCorrection = true,
                originalSentence = commonCorrection.original,
                correctedSentence = commonCorrection.corrected,
                nativeExplanation = commonCorrection.explanation,
                nativeMeaning = commonCorrection.meaning,
                repeatInstruction = "Now repeat: ${commonCorrection.corrected}"
            )
        }

        // Natural conversational response based on mode
        val conversationalResponse = generateModeResponse(trimmed, mode, profile.englishLevel)
        return TeacherFeedback(
            replyText = conversationalResponse,
            spokenAudioText = conversationalResponse,
            hasCorrection = false
        )
    }

    private data class CorrectionTuple(
        val original: String,
        val corrected: String,
        val explanation: String,
        val meaning: String
    )

    private fun checkCommonMistakes(
        lower: String,
        original: String,
        lang: SupportedLanguage
    ): CorrectionTuple? {
        val isUrdu = lang.code == "ur"
        val isHindi = lang.code == "hi"
        val isArabic = lang.code == "ar"

        // Rule 1: "Yesterday I go market" / "I go market yesterday"
        if (lower.contains("go market") || (lower.contains("yesterday") && lower.contains("go "))) {
            return CorrectionTuple(
                original = original,
                corrected = "Yesterday I went to the market.",
                explanation = if (isUrdu) "ماضی (Past) کے لیے 'go' کے بجائے 'went' اور منزل کے لیے 'to the' استعمال ہوتا ہے۔"
                else if (isHindi) "भूतकाल में 'go' की जगह 'went' और स्थान से पहले 'to the' लगाते हैं।"
                else if (isArabic) "في الماضي نستخدم went بدلاً من go ونضع to the قبل اسم المكان."
                else "In the past simple tense, use the irregular past form 'went' and add 'to the' before the destination.",
                meaning = if (isUrdu) "کل میں بازار گیا تھا۔"
                else if (isHindi) "कल मैं बाज़ार गया था।"
                else if (isArabic) "بالأمس ذهبتُ إلى السوق."
                else "Yesterday I went to the market."
            )
        }

        // Rule 2: "I am agree"
        if (lower.contains("i am agree") || lower.contains("i'm agree")) {
            return CorrectionTuple(
                original = original,
                corrected = "I agree with you.",
                explanation = if (isUrdu) "انگریزی میں 'agree' خود ایک فعل (verb) ہے، اس لیے 'am' لگانے کی ضرورت نہیں ہے۔"
                else if (isHindi) "'Agree' स्वयं एक क्रिया (verb) है, इसलिए 'am' नहीं लगाया जाता।"
                else if (isArabic) "كلمة agree فعل وليست صفة، فلا نضع قبلها am."
                else "'Agree' is already a verb in English, so do not use 'am' before it.",
                meaning = if (isUrdu) "میں آپ سے متفق ہوں۔"
                else if (isHindi) "मैं आपसे सहमत हूँ।"
                else if (isArabic) "أنا أتفق معك."
                else "I agree with you."
            )
        }

        // Rule 3: "She do not like" / "He do not know"
        if (lower.contains("she do not") || lower.contains("he do not") || lower.contains("she don't") || lower.contains("he don't")) {
            val subject = if (lower.contains("she")) "She" else "He"
            return CorrectionTuple(
                original = original,
                corrected = "$subject does not like that.",
                explanation = if (isUrdu) "تیسرے شخص واحد (He, She, It) کے لیے منفی جملوں میں 'does not' استعمال ہوتا ہے۔"
                else if (isHindi) "He/She के साथ नकारात्मक वाक्य में 'does not' का प्रयोग होता है।"
                else if (isArabic) "مع ضمائر المفرد الغائب (He, She) نستخدم does not بدلاً من do not."
                else "Third-person singular subjects (He, She, It) take 'does not' or 'doesn't' in present tense.",
                meaning = if (isUrdu) "وہ اسے پسند نہیں کرتی ہے۔"
                else if (isHindi) "वह इसे पसंद नहीं करती।"
                else if (isArabic) "هي لا تحب ذلك."
                else "$subject does not like that."
            )
        }

        // Rule 4: "I have 25 years old" / "I have 20 years"
        if (lower.contains("i have") && lower.contains("years old")) {
            return CorrectionTuple(
                original = original,
                corrected = "I am 25 years old.",
                explanation = if (isUrdu) "عمر بتانے کے لیے انگریزی میں فعل 'to be' (I am) استعمال ہوتا ہے، 'have' نہیں۔"
                else if (isHindi) "उम्र बताने के लिए 'I am' का प्रयोग किया जाता है, 'I have' का नहीं।"
                else if (isArabic) "في اللغة الإنجليزية نستخدم فعل الكينونة I am للتعبير عن العمر وليس I have."
                else "In English, state your age using 'I am [age]', not 'I have'.",
                meaning = if (isUrdu) "میری عمر پچیس سال ہے۔"
                else if (isHindi) "मेरी उम्र 25 वर्ष है।"
                else if (isArabic) "عمري 25 سنة."
                else "I am 25 years old."
            )
        }

        // Rule 5: "I am looking forward to meet you"
        if (lower.contains("looking forward to meet")) {
            return CorrectionTuple(
                original = original,
                corrected = "I am looking forward to meeting you.",
                explanation = if (isUrdu) "محاورے 'looking forward to' کے بعد ورب کے ساتھ 'ing' لگایا جاتا ہے۔"
                else if (isHindi) "'Looking forward to' के बाद क्रिया में 'ing' जुड़ता है।"
                else if (isArabic) "العبارة looking forward to تتبع دائماً بصيغة الفعل مع ing."
                else "The prepositional phrase 'look forward to' requires a gerund (-ing form).",
                meaning = if (isUrdu) "مجھے آپ سے ملنے کا بے صبری سے انتظار ہے۔"
                else if (isHindi) "मुझे आपसे मिलने का बेसब्री से इंतज़ार है।"
                else if (isArabic) "أتطلع بشوق إلى لقائك."
                else "I look forward to meeting you."
            )
        }

        return null
    }

    private fun getContextualFollowup(mode: ConversationModeType, level: String): String {
        return when (mode) {
            ConversationModeType.DAILY_CONVERSATION -> "Great job trying! Tell me, what did you enjoy doing most today?"
            ConversationModeType.ENGLISH_TEACHER -> "Excellent correction! Now let's try another sentence: What are your hobbies?"
            ConversationModeType.JOB_INTERVIEW -> "Very good! Now, can you describe your greatest professional strength in one sentence?"
            ConversationModeType.TRAVEL_ENGLISH -> "Nice! And how many days are you planning to stay in the city?"
            ConversationModeType.RESTAURANT_CONVERSATION -> "Perfect! Would you like still or sparkling water with your meal?"
            ConversationModeType.SHOPPING_CONVERSATION -> "Got it! Are you looking for a small, medium, or large size?"
            ConversationModeType.DOCTOR_CONVERSATION -> "Thank you for explaining clearly. Have you experienced any fever or headache?"
            ConversationModeType.BUSINESS_ENGLISH -> "Well stated. Can we align on setting a deadline for next Wednesday?"
            ConversationModeType.PRONUNCIATION_PRACTICE -> "Wonderful intonation! Let's say it one more time with a smooth rhythm."
            ConversationModeType.FREE_CONVERSATION -> "I love chatting with you! What is your favorite movie or book?"
        }
    }

    private fun generateModeResponse(
        userInput: String,
        mode: ConversationModeType,
        level: String
    ): String {
        val isBeginner = level == "BEGINNER"
        return when (mode) {
            ConversationModeType.DAILY_CONVERSATION -> {
                if (isBeginner) "That sounds very nice! What do you like to eat for dinner?"
                else "That's really interesting! How do you usually like to spend your free time on weekends?"
            }
            ConversationModeType.ENGLISH_TEACHER -> {
                "Well said! Your sentence structure is clean and easy to understand. Can you tell me what you plan to do tomorrow?"
            }
            ConversationModeType.JOB_INTERVIEW -> {
                "Thank you for sharing that. Why are you interested in joining this company, and what makes you a great fit for our team?"
            }
            ConversationModeType.TRAVEL_ENGLISH -> {
                "Everything is in order with your documents. Here is your gate pass. Do you have any checked bags with you?"
            }
            ConversationModeType.RESTAURANT_CONVERSATION -> {
                "Wonderful choice. Our chef prepares that fresh daily. Would you also like our garlic bread appetizer while you wait?"
            }
            ConversationModeType.SHOPPING_CONVERSATION -> {
                "We have that in blue and navy black on the second rack. Would you like to try it on in the fitting room?"
            }
            ConversationModeType.DOCTOR_CONVERSATION -> {
                "I understand your symptoms. Make sure to stay well hydrated and rest. Have you taken any pain relievers today?"
            }
            ConversationModeType.BUSINESS_ENGLISH -> {
                "That sounds like an effective plan. I will email the action items to the stakeholders. Shall we review next Tuesday?"
            }
            ConversationModeType.PRONUNCIATION_PRACTICE -> {
                "Great clarity and smooth vowel sounds! Now try this sentence: 'Practice makes speaking natural and effortless.'"
            }
            ConversationModeType.FREE_CONVERSATION -> {
                "That's a fantastic perspective! What do you think is the best way to learn new skills quickly?"
            }
        }
    }

    // --- Translation Helper ("How do I say this in English?") ---
    suspend fun translateToEnglish(
        inputQuery: String,
        nativeLang: SupportedLanguage
    ): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = inputQuery.trim()

        if (GeminiApiClient.hasValidApiKey()) {
            try {
                val prompt = """
                    Translate and explain this expression from ${nativeLang.name} to natural English:
                    Phrase: "$trimmed"
                    
                    Return strictly in this format:
                    NATURAL: <natural fluent English sentence>
                    SIMPLE: <simple everyday alternative>
                    NATIVE_MEANING: <meaning in ${nativeLang.name}>
                    NOTE: <short helpful grammar or cultural usage tip in ${nativeLang.name}>
                    DIALOGUE: <short 2-line mini conversation in English showing how natives say it>
                """.trimIndent()

                val res = GeminiApiClient.service.generateContent(
                    BuildConfig.GEMINI_API_KEY,
                    GeminiRequest(
                        contents = listOf(
                            GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                        )
                    )
                )
                val out = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!out.isNullOrBlank()) {
                    val natural = out.lines().find { it.startsWith("NATURAL:") }?.substringAfter("NATURAL:")?.trim() ?: "Could you please help me?"
                    val simple = out.lines().find { it.startsWith("SIMPLE:") }?.substringAfter("SIMPLE:")?.trim() ?: "Please help me."
                    val meaning = out.lines().find { it.startsWith("NATIVE_MEANING:") }?.substringAfter("NATIVE_MEANING:")?.trim() ?: trimmed
                    val note = out.lines().find { it.startsWith("NOTE:") }?.substringAfter("NOTE:")?.trim() ?: "Polite request structure using 'could you'."
                    val dialogue = out.lines().find { it.startsWith("DIALOGUE:") }?.substringAfter("DIALOGUE:")?.trim() ?: "A: Excuse me, could you help me? B: Certainly, how can I assist?"

                    return@withContext TranslationResult(natural, simple, meaning, note, dialogue)
                }
            } catch (_: Exception) {}
        }

        // Smart local translation fallback
        return@withContext generateLocalTranslation(trimmed, nativeLang)
    }

    private fun generateLocalTranslation(
        query: String,
        nativeLang: SupportedLanguage
    ): TranslationResult {
        val lower = query.lowercase()
        return when {
            lower.contains("بازار") || lower.contains("market") || lower.contains("سوق") -> {
                TranslationResult(
                    naturalEnglish = "I am heading out to the market to pick up some groceries.",
                    simpleAlternative = "I am going to the market.",
                    nativeMeaning = "میں بازار جا رہا ہوں۔",
                    grammarNote = "Use 'head out to' for a natural conversational expression of going somewhere.",
                    exampleDialogue = "A: Where are you off to?\nB: I'm heading out to the market."
                )
            }
            lower.contains("مدد") || lower.contains("help") || lower.contains("مساعدة") -> {
                TranslationResult(
                    naturalEnglish = "Could you please lend me a hand with this?",
                    simpleAlternative = "Can you help me, please?",
                    nativeMeaning = "کیا آپ میری مدد کر سکتے ہیں؟",
                    grammarNote = "'Lend a hand' is an idiomatic native English way to ask for assistance.",
                    exampleDialogue = "A: Could you lend me a hand with these bags?\nB: Of course, let me grab those for you!"
                )
            }
            lower.contains("وقت") || lower.contains("time") || lower.contains("ساعة") -> {
                TranslationResult(
                    naturalEnglish = "Do you happen to have the time on you?",
                    simpleAlternative = "What time is it now?",
                    nativeMeaning = "کیا وقت ہوا ہے؟",
                    grammarNote = "Asking 'Do you happen to...' is a polite British and American etiquette phrase.",
                    exampleDialogue = "A: Excuse me, do you have the time?\nB: It is just past three o'clock."
                )
            }
            else -> {
                TranslationResult(
                    naturalEnglish = "How do you do? It is wonderful to speak with you.",
                    simpleAlternative = "Hello, nice to meet you.",
                    nativeMeaning = query,
                    grammarNote = "Polite greeting used in professional and social introductions.",
                    exampleDialogue = "A: Good morning! How do you do?\nB: Very well, thank you for asking!"
                )
            }
        }
    }

    // --- Personalized Daily Lesson Generator ---
    suspend fun getDailyLesson(date: String, profile: UserProfileEntity): DailyLessonData {
        val level = EnglishLevel.valueOf(profile.englishLevel)
        val lang = LanguageCatalog.getByCode(profile.nativeLanguageCode)

        val vocabList = listOf(
            LessonVocab("Enthusiastic", "/ɪnˌθuː.ziˈæs.tɪk/", "adj.", "Having or showing intense enjoyment", "پرجوش", "She is enthusiastic about speaking English."),
            LessonVocab("Persist", "/pɚˈsɪst/", "verb.", "Continue firmly in an opinion or course of action", "ثابت قدم رہنا", "If you persist, you will achieve fluency."),
            LessonVocab("Clarify", "/ˈkler.ə.faɪ/", "verb.", "Make a statement or situation less confused", "وضاحت کرنا", "Could you clarify what you meant?"),
            LessonVocab("Milestone", "/ˈmaɪl.stoʊn/", "noun.", "A significant stage or event in development", "سنگ میل", "Finishing this daily lesson is a big milestone.")
        )

        val sentences = listOf(
            LessonSentence("I really appreciate your guidance.", "میں آپ کی رہنمائی کی واقعی تعریف کرتا ہوں۔", "Expressing gratitude politely"),
            LessonSentence("Could you please repeat that a bit slower?", "کیا آپ اسے تھوڑا آہستہ دہرا سکتے ہیں؟", "Asking for clarification"),
            LessonSentence("That makes total sense to me now.", "اب یہ بات مجھے پوری طرح سمجھ آگئی ہے۔", "Confirming comprehension"),
            LessonSentence("I look forward to our next conversation.", "مجھے اپنی اگلی گفتگو کا انتظار رہے گا۔", "Polite wrap-up")
        )

        val dialogue = LessonDialogue(
            scenario = "Meeting a coworker at a modern coffee shop",
            lines = listOf(
                DialogueLine("Emma (AI)", "Hi there! Have you been waiting long?", "ہیلو! کیا آپ کافی دیر سے انتظار کر رہے ہیں؟"),
                DialogueLine("You", "No, I just arrived a couple of minutes ago.", "نہیں، میں صرف دو منٹ پہلے ہی پہنچا ہوں۔"),
                DialogueLine("Emma (AI)", "Awesome! Let's grab a table by the window and chat.", "بہت خوب! آئیے کھڑکی کے پاس والی میز پر بیٹھ کر بات کرتے ہیں۔")
            )
        )

        val challenge = SpeakingChallenge(
            targetSentence = "Every single day of speaking practice brings me closer to true English fluency.",
            nativeMeaning = "بولنے کی روزانہ کی مشق مجھے حقیقی روانی کے قریب لاتی ہے۔",
            phonemeTips = "Focus on the 'v' in 'Every', the soft 'th' in 'brings', and the crisp 'fl' blend in 'fluency'.",
            difficulty = "Intermediate"
        )

        val questions = listOf(
            LessonQuizQuestion(
                id = 1,
                question = "Which sentence is grammatically correct?",
                options = listOf(
                    "Yesterday I go to market.",
                    "Yesterday I went to the market.",
                    "Yesterday I gone to the market.",
                    "Yesterday I was go market."
                ),
                correctIndex = 1,
                explanation = "In simple past tense, we use the second form 'went' + 'to the market'."
            ),
            LessonQuizQuestion(
                id = 2,
                question = "Complete the polite phrase: 'Could you please ____ me a hand?'",
                options = listOf("take", "lend", "borrow", "keep"),
                correctIndex = 1,
                explanation = "'Lend a hand' is the idiomatic phrase meaning 'to help'."
            ),
            LessonQuizQuestion(
                id = 3,
                question = "Choose the correct verb form: 'She ____ not drink coffee in the evening.'",
                options = listOf("do", "does", "is", "have"),
                correctIndex = 1,
                explanation = "Third-person singular 'She' takes 'does not' in negative present simple."
            ),
            LessonQuizQuestion(
                id = 4,
                question = "What is the meaning of 'I look forward to meeting you'?",
                options = listOf(
                    "I forgot to meet you.",
                    "I am eagerly awaiting our meeting.",
                    "I looked behind you.",
                    "I don't want to meet."
                ),
                correctIndex = 1,
                explanation = "'Look forward to' means anticipating something with pleasure."
            )
        )

        return DailyLessonData(
            topicTitle = "Confidence & Everyday Idioms",
            level = level,
            dateString = date,
            vocabulary = vocabList,
            usefulSentences = sentences,
            dialogueScenario = dialogue,
            speakingChallenge = challenge,
            testQuestions = questions
        )
    }

    suspend fun saveLessonResult(date: String, score: Int, total: Int) {
        dailyLessonDao.insertOrUpdateLesson(
            DailyLessonRecordEntity(
                date = date,
                topicTitle = "Daily English Lesson",
                level = "INTERMEDIATE",
                isCompleted = true,
                quizScore = score,
                totalQuestions = total
            )
        )
        val profile = userProfileDao.getUserProfileSync() ?: return
        userProfileDao.insertOrUpdateProfile(
            profile.copy(
                fluencyScore = (profile.fluencyScore + 2).coerceAtMost(100),
                totalPracticeSeconds = profile.totalPracticeSeconds + 300L
            )
        )
    }

    private fun calculateStreak(lastTimestamp: Long, currentStreak: Int): Int {
        val now = Calendar.getInstance()
        val last = Calendar.getInstance().apply { timeInMillis = lastTimestamp }

        val diffDays = (now.get(Calendar.DAY_OF_YEAR) - last.get(Calendar.DAY_OF_YEAR))
        return when (diffDays) {
            0 -> currentStreak // same day
            1 -> currentStreak + 1 // consecutive day
            else -> 1 // missed days, reset to 1
        }
    }
}
