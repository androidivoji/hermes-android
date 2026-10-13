package com.example.myhermes.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhermes.ui.theme.GeminiBlue
import java.util.Locale

@Composable
fun VoiceModeDialog(
    isGenerating: Boolean,
    lastAiMessage: String?,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Menyiapkan Voice Mode...") }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var lastSpokenMessageId by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Speech-to-Text Launcher
    val speechToTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenResults?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                statusText = "Mengirim: \"$spokenText\""
                onSendMessage(spokenText)
            } else {
                statusText = "Ketuk mikrofon untuk berbicara"
            }
        } else {
            statusText = "Ketuk mikrofon untuk berbicara"
        }
    }

    fun startListening() {
        if (isGenerating || isSpeaking) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicaralah dengan Hermes AI...")
        }
        try {
            isListening = true
            statusText = "Mendengarkan..."
            speechToTextLauncher.launch(intent)
        } catch (_: Exception) {
            isListening = false
            statusText = "Gagal membuka pengenalan suara"
        }
    }

    // Initialize TTS
    DisposableEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("id"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
                isTtsReady = true
                statusText = "Ketuk mikrofon untuk mulai berbicara"
                startListening()
            } else {
                statusText = "Gagal memuat Text-to-Speech"
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    statusText = "Hermes sedang berbicara..."
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    statusText = "Ketuk mikrofon untuk berbicara"
                    startListening()
                }
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    statusText = "Ketuk mikrofon untuk berbicara"
                }
            })
        }
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // Speak AI Response when available
    LaunchedEffect(lastAiMessage, isGenerating) {
        if (!isGenerating && !lastAiMessage.isNullOrBlank() && isTtsReady) {
            val messageId = lastAiMessage.hashCode().toString()
            if (messageId != lastSpokenMessageId) {
                lastSpokenMessageId = messageId
                isSpeaking = true
                statusText = "Hermes sedang berbicara..."
                tts?.speak(
                    lastAiMessage,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "AI_RESPONSE_UTTERANCE"
                )
            }
        }
    }

    // Pulse animation for listening/speaking
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Close button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = {
                        tts?.stop()
                        onDismiss()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Voice Mode",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Center Visualizer & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hermes Voice Mode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Interactive Mic Button with Pulse effect
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening || isSpeaking) {
                        Surface(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(scale),
                            shape = CircleShape,
                            color = GeminiBlue.copy(alpha = 0.25f)
                        ) {}
                    }

                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable {
                                if (!isListening && !isSpeaking && !isGenerating) {
                                    startListening()
                                }
                            },
                        shape = CircleShape,
                        color = GeminiBlue,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mulai Bicara",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            // Bottom hint
            Text(
                text = "Percakapan suara dua arah otomatis aktif",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
