package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechManager
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.GrammarCorrectionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VocabularyEntity
import com.example.data.model.*
import com.example.data.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SpeakAiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = SpeakAiRepository(db)
    val speechManager = SpeechManager(application)

    // User Profile
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfileEntity())

    // Vocabulary & Corrections
    val allVocabulary: StateFlow<List<VocabularyEntity>> = repository.allVocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCorrections: StateFlow<List<GrammarCorrectionEntity>> = repository.allCorrections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Conversation State
    private val _currentMode = MutableStateFlow(ConversationModeType.DAILY_CONVERSATION)
    val currentMode: StateFlow<ConversationModeType> = _currentMode.asStateFlow()

    private val _currentConversationId = MutableStateFlow("conv_${System.currentTimeMillis()}")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _latestFeedback = MutableStateFlow<TeacherFeedback?>(null)
    val latestFeedback: StateFlow<TeacherFeedback?> = _latestFeedback.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Daily Lesson State
    private val _dailyLesson = MutableStateFlow<DailyLessonData?>(null)
    val dailyLesson: StateFlow<DailyLessonData?> = _dailyLesson.asStateFlow()

    private val _quizCurrentIndex = MutableStateFlow(0)
    val quizCurrentIndex: StateFlow<Int> = _quizCurrentIndex.asStateFlow()

    private val _quizSelectedOption = MutableStateFlow<Int?>(null)
    val quizSelectedOption: StateFlow<Int?> = _quizSelectedOption.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizCompleted = MutableStateFlow(false)
    val quizCompleted: StateFlow<Boolean> = _quizCompleted.asStateFlow()

    // Translation Screen State
    private val _translationQuery = MutableStateFlow("")
    val translationQuery: StateFlow<String> = _translationQuery.asStateFlow()

    private val _translationResult = MutableStateFlow<TranslationResult?>(null)
    val translationResult: StateFlow<TranslationResult?> = _translationResult.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // Pronunciation Coach State
    private val _pronunciationTarget = MutableStateFlow("The quick brown fox jumps over the lazy dog.")
    val pronunciationTarget: StateFlow<String> = _pronunciationTarget.asStateFlow()

    private val _pronunciationScore = MutableStateFlow<Int?>(null)
    val pronunciationScore: StateFlow<Int?> = _pronunciationScore.asStateFlow()

    private val _pronunciationFeedback = MutableStateFlow<String?>(null)
    val pronunciationFeedback: StateFlow<String?> = _pronunciationFeedback.asStateFlow()

    // Conversation timer
    private var sessionStartTime = 0L
    private var conversationMessageJob: Job? = null

    init {
        // Wire SpeechManager callbacks
        speechManager.onSpeechRecognized = { recognized ->
            handleSpokenInput(recognized)
        }
        speechManager.onSpeechError = { err ->
            _errorMessage.value = err
        }

        // Apply profile voice settings when profile loads
        viewModelScope.launch {
            userProfile.collect { profile ->
                val accent = try { EnglishAccent.valueOf(profile.accent) } catch (_: Exception) { EnglishAccent.US }
                val gender = try { VoiceGender.valueOf(profile.voiceGender) } catch (_: Exception) { VoiceGender.FEMALE }
                speechManager.applyTtsSettings(accent, profile.voiceSpeed, gender)
            }
        }

        loadDailyLesson()
    }

    fun startConversation(mode: ConversationModeType) {
        _currentMode.value = mode
        val newConvId = "conv_${mode.id}_${System.currentTimeMillis()}"
        _currentConversationId.value = newConvId
        sessionStartTime = System.currentTimeMillis()

        // Observe messages for this conversation
        conversationMessageJob?.cancel()
        conversationMessageJob = viewModelScope.launch {
            repository.getConversationMessages(newConvId).collect {
                _messages.value = it
            }
        }

        // Welcome prompt from AI Teacher
        viewModelScope.launch {
            val starter = mode.starterPrompt
            repository.saveChatMessage(
                ChatMessageEntity(
                    conversationId = newConvId,
                    modeId = mode.id,
                    role = "assistant",
                    content = starter
                )
            )
            speechManager.speak(starter)
        }
    }

    fun stopConversation() {
        speechManager.stopSpeaking()
        speechManager.stopListening()
        if (sessionStartTime > 0) {
            val elapsedSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).coerceAtLeast(10)
            viewModelScope.launch {
                repository.recordPracticeSession(elapsedSeconds, _currentMode.value.id)
            }
            sessionStartTime = 0
        }
    }

    fun toggleMic() {
        if (speechManager.isUserListening.value) {
            speechManager.stopListening()
        } else {
            speechManager.startListening()
        }
    }

    fun handleSpokenInput(text: String) {
        if (text.isBlank()) return
        val profile = userProfile.value
        val mode = _currentMode.value
        val convId = _currentConversationId.value

        _isProcessingAi.value = true
        viewModelScope.launch {
            val feedback = repository.processUserSpeech(text, convId, mode, profile)
            _latestFeedback.value = feedback
            _isProcessingAi.value = false

            // AI Teacher speaks out loud!
            val speechRate = if (profile.englishLevel == "BEGINNER") 0.75f else profile.voiceSpeed
            speechManager.speak(feedback.spokenAudioText, speechRate)
        }
    }

    fun sendTextMessage(text: String) {
        handleSpokenInput(text)
    }

    fun replayAiSpeech(slow: Boolean = false) {
        speechManager.replayLastSpoken(slow)
    }

    fun speakText(text: String, rate: Float? = null) {
        speechManager.speak(text, rate)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun dismissCorrection() {
        _latestFeedback.value = null
    }

    // Profile & Settings updates
    fun toggleVocabMastered(id: Long, mastered: Boolean) {
        viewModelScope.launch {
            repository.toggleVocabMastered(id, mastered)
        }
    }

    fun markCorrectionMastered(id: Long) {
        viewModelScope.launch {
            repository.markCorrectionMastered(id)
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(nativeLanguageCode = langCode))
        }
    }

    fun updateLevel(level: EnglishLevel) {
        viewModelScope.launch {
            val current = userProfile.value
            val speed = when (level) {
                EnglishLevel.BEGINNER -> 0.75f
                EnglishLevel.INTERMEDIATE -> 0.90f
                EnglishLevel.ADVANCED -> 1.05f
            }
            repository.updateProfile(current.copy(englishLevel = level.name, voiceSpeed = speed))
        }
    }

    fun updateAccent(accent: EnglishAccent) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(accent = accent.name))
        }
    }

    fun updateVoiceGender(gender: VoiceGender) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(voiceGender = gender.name))
        }
    }

    fun updateVoiceSpeed(speed: Float) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(voiceSpeed = speed))
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(isDarkMode = !current.isDarkMode))
        }
    }

    fun completeOnboarding(langCode: String, level: EnglishLevel, accent: EnglishAccent) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(
                current.copy(
                    nativeLanguageCode = langCode,
                    englishLevel = level.name,
                    accent = accent.name,
                    isOnboarded = true
                )
            )
        }
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(isPremium = true))
        }
    }

    // Daily Lesson actions
    fun loadDailyLesson() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            val lesson = repository.getDailyLesson(today, userProfile.value)
            _dailyLesson.value = lesson
            _quizCurrentIndex.value = 0
            _quizSelectedOption.value = null
            _quizScore.value = 0
            _quizCompleted.value = false
        }
    }

    fun selectQuizOption(optionIndex: Int) {
        if (_quizSelectedOption.value != null) return
        _quizSelectedOption.value = optionIndex
        val currentQ = _dailyLesson.value?.testQuestions?.getOrNull(_quizCurrentIndex.value)
        if (currentQ != null && optionIndex == currentQ.correctIndex) {
            _quizScore.value = _quizScore.value + 1
        }
    }

    fun nextQuizQuestion() {
        val total = _dailyLesson.value?.testQuestions?.size ?: 0
        if (_quizCurrentIndex.value + 1 < total) {
            _quizCurrentIndex.value = _quizCurrentIndex.value + 1
            _quizSelectedOption.value = null
        } else {
            _quizCompleted.value = true
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            viewModelScope.launch {
                repository.saveLessonResult(today, _quizScore.value, total)
            }
        }
    }

    // Translation screen
    fun setTranslationQuery(query: String) {
        _translationQuery.value = query
    }

    fun performTranslation() {
        val query = _translationQuery.value.trim()
        if (query.isBlank()) return
        _isTranslating.value = true
        val lang = LanguageCatalog.getByCode(userProfile.value.nativeLanguageCode)
        viewModelScope.launch {
            val res = repository.translateToEnglish(query, lang)
            _translationResult.value = res
            _isTranslating.value = false
            speechManager.speak(res.naturalEnglish)
        }
    }

    // Pronunciation coach
    fun setPronunciationTarget(sentence: String) {
        _pronunciationTarget.value = sentence
        _pronunciationScore.value = null
        _pronunciationFeedback.value = null
    }

    fun evaluatePronunciation(spokenText: String) {
        val targetWords = _pronunciationTarget.value.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex())
        val spokenWords = spokenText.lowercase().replace(Regex("[^a-z0-9 ]"), "").split("\\s+".toRegex())

        var matchCount = 0
        for (w in targetWords) {
            if (spokenWords.contains(w)) matchCount++
        }

        val accuracy = ((matchCount.toFloat() / targetWords.size.coerceAtLeast(1)) * 100).toInt().coerceIn(40, 98)
        _pronunciationScore.value = accuracy
        _pronunciationFeedback.value = when {
            accuracy >= 85 -> "Outstanding native-like articulation and cadence! Keep up the brilliant rhythm."
            accuracy >= 70 -> "Great pronunciation! Pay close attention to word endings and vowel clarity."
            else -> "Good effort! Try slowing down and pronouncing each syllable distinctly."
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.cleanup()
    }
}
