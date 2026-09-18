package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.EnglishAccent
import com.example.data.model.VoiceGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isUserListening = MutableStateFlow(false)
    val isUserListening: StateFlow<Boolean> = _isUserListening.asStateFlow()

    private val _isAiSpeaking = MutableStateFlow(false)
    val isAiSpeaking: StateFlow<Boolean> = _isAiSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var lastSpokenText: String = ""
    private var currentAccent: EnglishAccent = EnglishAccent.US
    private var currentRate: Float = 0.85f
    private var currentGender: VoiceGender = VoiceGender.FEMALE

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            applyTtsSettings(currentAccent, currentRate, currentGender)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isAiSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isAiSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isAiSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isAiSpeaking.value = false
                }
            })
        }
    }

    fun applyTtsSettings(accent: EnglishAccent, rate: Float, gender: VoiceGender) {
        currentAccent = accent
        currentRate = rate
        currentGender = gender

        if (!isTtsReady || tts == null) return

        val locale = when (accent) {
            EnglishAccent.US -> Locale.US
            EnglishAccent.UK -> Locale.UK
        }
        tts?.language = locale
        tts?.setSpeechRate(rate)

        // Adjust pitch to provide distinct male/female vocal tone
        when (gender) {
            VoiceGender.FEMALE -> tts?.setPitch(1.15f)
            VoiceGender.MALE -> tts?.setPitch(0.85f)
        }
    }

    fun speak(text: String, rateOverride: Float? = null) {
        if (!isTtsReady || tts == null || text.isBlank()) return
        stopListening()
        stopSpeaking()

        lastSpokenText = text

        val rateToUse = rateOverride ?: currentRate
        tts?.setSpeechRate(rateToUse)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "speakai_utterance_${System.currentTimeMillis()}")
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "speakai_utterance_${System.currentTimeMillis()}")
    }

    fun replayLastSpoken(slow: Boolean = false) {
        if (lastSpokenText.isNotBlank()) {
            val rate = if (slow) 0.65f else currentRate
            speak(lastSpokenText, rate)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isAiSpeaking.value = false
    }

    fun startListening() {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError?.invoke("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isUserListening.value = true
                        _recognizedText.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _isUserListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB..10dB to 0f..1f
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
                        _audioAmplitude.value = norm
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isUserListening.value = false
                        _audioAmplitude.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isUserListening.value = false
                        _audioAmplitude.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard. Please try speaking again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out. Please tap to speak."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for speech recognition."
                            else -> "Could not recognize speech. Please try again."
                        }
                        onSpeechError?.invoke(message)
                    }

                    override fun onResults(results: Bundle?) {
                        _isUserListening.value = false
                        _audioAmplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: ""
                        if (spoken.isNotBlank()) {
                            _recognizedText.value = spoken
                            onSpeechRecognized?.invoke(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _recognizedText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isUserListening.value = true

        } catch (e: Exception) {
            _isUserListening.value = false
            onSpeechError?.invoke("Error starting microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isUserListening.value = false
        _audioAmplitude.value = 0f
    }

    fun cleanup() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
