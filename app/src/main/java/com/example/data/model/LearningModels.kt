package com.example.data.model

enum class EnglishLevel(val displayName: String, val code: String, val description: String) {
    BEGINNER("Beginner", "A1-A2", "Basic words, simple everyday phrases, slower voice"),
    INTERMEDIATE("Intermediate", "B1-B2", "Casual dialogues, work conversations, moderate speed"),
    ADVANCED("Advanced", "C1-C2", "Complex topics, natural native speed, idioms & nuance")
}

enum class EnglishAccent(val displayName: String, val localeTag: String, val flag: String) {
    US("American English", "en-US", "🇺🇸"),
    UK("British English", "en-GB", "🇬🇧")
}

enum class VoiceGender(val displayName: String) {
    FEMALE("Female (Emma)"),
    MALE("Male (Oliver)")
}

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false
)

object LanguageCatalog {
    val languages = listOf(
        SupportedLanguage("ur", "Urdu", "اردو", "🇵🇰", isRtl = true),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
        SupportedLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
        SupportedLanguage("ps", "Pashto", "پښتو", "🇦🇫", isRtl = true),
        SupportedLanguage("bn", "Bengali", "বাংলা", "🇧🇩"),
        SupportedLanguage("fa", "Persian", "فارسی", "🇮🇷", isRtl = true),
        SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦", isRtl = true),
        SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷"),
        SupportedLanguage("es", "Spanish", "Español", "🇪🇸"),
        SupportedLanguage("fr", "French", "Français", "🇫🇷"),
        SupportedLanguage("de", "German", "Deutsch", "🇩🇪"),
        SupportedLanguage("pt", "Portuguese", "Português", "🇧🇷"),
        SupportedLanguage("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        SupportedLanguage("ru", "Russian", "Русский", "🇷🇺"),
        SupportedLanguage("zh", "Chinese", "中文", "🇨🇳"),
        SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵"),
        SupportedLanguage("en", "English", "English", "🇺🇸")
    )

    fun getByCode(code: String): SupportedLanguage {
        return languages.find { it.code.equals(code, ignoreCase = true) } ?: languages.first()
    }
}

enum class ConversationModeType(
    val id: String,
    val title: String,
    val iconName: String,
    val description: String,
    val starterPrompt: String,
    val aiPersona: String
) {
    DAILY_CONVERSATION(
        "daily",
        "Daily Conversation",
        "chat",
        "Casual chit-chat, hobbies, family, weekend plans",
        "Hello! How was your day today? Did you do anything fun?",
        "You are a warm, casual friend chatting in English."
    ),
    ENGLISH_TEACHER(
        "teacher",
        "English Teacher",
        "school",
        "Structured feedback, polite grammar correction & pronunciation",
        "Welcome to today's English lesson! Tell me, what would you like to practice today?",
        "You are a professional, gentle, encouraging English teacher."
    ),
    JOB_INTERVIEW(
        "interview",
        "Job Interview",
        "work",
        "Professional questions, behavioral answers & elevator pitch",
        "Thank you for coming in today. Could you start by introducing yourself and your background?",
        "You are a friendly hiring manager conducting a professional job interview."
    ),
    TRAVEL_ENGLISH(
        "travel",
        "Travel English",
        "flight",
        "Airport customs, asking directions, hotel check-in & taxis",
        "Welcome to London Heathrow Airport! May I see your passport and boarding pass, please?",
        "You are an airport customs officer and travel assistant."
    ),
    RESTAURANT_CONVERSATION(
        "restaurant",
        "Restaurant Conversation",
        "restaurant",
        "Ordering food, asking about ingredients, paying the bill",
        "Good evening! Welcome to The Green Bistro. Would you like to start with something to drink?",
        "You are a polite restaurant waiter taking orders."
    ),
    SHOPPING_CONVERSATION(
        "shopping",
        "Shopping Conversation",
        "shopping_bag",
        "Finding sizes, asking prices, discounts & returns",
        "Hi there! Can I help you find anything specific today, or are you just browsing?",
        "You are a helpful retail store assistant."
    ),
    DOCTOR_CONVERSATION(
        "doctor",
        "Doctor Conversation",
        "local_hospital",
        "Explaining symptoms, medications, health advice",
        "Hello! Please take a seat. How are you feeling today, and what brings you to the clinic?",
        "You are an empathetic, attentive doctor speaking clearly with a patient."
    ),
    BUSINESS_ENGLISH(
        "business",
        "Business English",
        "business_center",
        "Meetings, negotiating, presentations & email etiquette",
        "Let's get started with our quarterly sync. What are your key milestones for this project?",
        "You are a senior corporate executive leading an English business meeting."
    ),
    PRONUNCIATION_PRACTICE(
        "pronunciation",
        "Pronunciation Practice",
        "record_voice_over",
        "Focus on difficult phonemes, stress & intonation",
        "Today we will practice clear vowel sounds. Repeat after me: 'The weather is wonderful today.'",
        "You are an expert pronunciation and accent coach."
    ),
    FREE_CONVERSATION(
        "free",
        "Free Conversation",
        "psychology",
        "Talk about anything freely with no strict script",
        "Hi! I'm here to talk about anything on your mind. What's something interesting you learned recently?",
        "You are an open-minded conversational AI companion helping practice fluent English."
    );

    companion object {
        fun fromId(id: String): ConversationModeType {
            return entries.find { it.id == id } ?: DAILY_CONVERSATION
        }
    }
}
