package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.SpeakAiViewModel
import com.example.ui.screens.*

@Composable
fun SpeakAiNavHost(
    navController: NavHostController,
    viewModel: SpeakAiViewModel
) {
    val profile by viewModel.userProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                viewModel = viewModel,
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(NavRoutes.LANGUAGE_SELECT)
                }
            )
        }

        composable(NavRoutes.LANGUAGE_SELECT) {
            LanguageSelectionScreen(
                currentSelectedCode = profile.nativeLanguageCode,
                onLanguageSelected = { code ->
                    viewModel.updateLanguage(code)
                },
                onContinue = {
                    navController.navigate(NavRoutes.LEVEL_SELECT)
                }
            )
        }

        composable(NavRoutes.LEVEL_SELECT) {
            LevelSelectionScreen(
                currentLevel = profile.englishLevel,
                currentAccent = profile.accent,
                onComplete = { level, accent ->
                    viewModel.completeOnboarding(profile.nativeLanguageCode, level, accent)
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onStartSpeaking = { mode ->
                    navController.navigate(NavRoutes.liveConversation(mode.id))
                },
                onNavigateDailyLesson = {
                    navController.navigate(NavRoutes.DAILY_LESSON)
                },
                onNavigateVocabulary = {
                    navController.navigate(NavRoutes.VOCABULARY)
                },
                onNavigatePronunciation = {
                    navController.navigate(NavRoutes.PRONUNCIATION)
                },
                onNavigateCorrections = {
                    navController.navigate(NavRoutes.ENGLISH_CORRECTION)
                },
                onNavigateTranslation = {
                    navController.navigate(NavRoutes.TRANSLATION)
                },
                onNavigateProgress = {
                    navController.navigate(NavRoutes.PROGRESS)
                },
                onNavigateProfile = {
                    navController.navigate(NavRoutes.PROFILE)
                },
                onNavigateSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateSubscription = {
                    navController.navigate(NavRoutes.SUBSCRIPTION)
                }
            )
        }

        composable(
            route = NavRoutes.LIVE_CONVERSATION,
            arguments = listOf(navArgument("modeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeId = backStackEntry.arguments?.getString("modeId") ?: "daily"
            LiveConversationScreen(
                modeId = modeId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ENGLISH_CORRECTION) {
            EnglishCorrectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.VOCABULARY) {
            VocabularyScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PRONUNCIATION) {
            PronunciationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DAILY_LESSON) {
            DailyLessonScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.TRANSLATION) {
            TranslationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROGRESS) {
            ProgressScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateSubscription = { navController.navigate(NavRoutes.SUBSCRIPTION) },
                onNavigateProgress = { navController.navigate(NavRoutes.PROGRESS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateLanguageSelect = { navController.navigate(NavRoutes.LANGUAGE_SELECT) },
                onNavigatePrivacyPolicy = { navController.navigate(NavRoutes.PRIVACY_POLICY) },
                onNavigateTerms = { navController.navigate(NavRoutes.TERMS) },
                onNavigatePlayStorePreview = { navController.navigate(NavRoutes.PLAY_STORE_PREVIEW) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SUBSCRIPTION) {
            SubscriptionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TERMS) {
            TermsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.PLAY_STORE_PREVIEW) {
            PlayStorePreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
