package com.example.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LANGUAGE_SELECT = "language_select"
    const val LEVEL_SELECT = "level_select"
    const val HOME = "home"
    const val LIVE_CONVERSATION = "live_conversation/{modeId}"
    const val ENGLISH_CORRECTION = "english_correction"
    const val VOCABULARY = "vocabulary"
    const val PRONUNCIATION = "pronunciation"
    const val DAILY_LESSON = "daily_lesson"
    const val TRANSLATION = "translation"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SUBSCRIPTION = "subscription"
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS = "terms"
    const val PLAY_STORE_PREVIEW = "play_store_preview"

    fun liveConversation(modeId: String) = "live_conversation/$modeId"
}
