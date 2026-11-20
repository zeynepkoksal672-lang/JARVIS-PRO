package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvis.assistant.ui.components.ArcReactorState
import com.jarvis.assistant.ui.components.ArcReactorView
import com.jarvis.assistant.ui.theme.JarvisAmber
import com.jarvis.assistant.ui.theme.JarvisBlack
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisDeepSurface
import com.jarvis.assistant.ui.theme.JarvisElectricGreen
import com.jarvis.assistant.ui.theme.JarvisNeonBlue
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard
import com.jarvis.assistant.ui.theme.JarvisSurfaceHighlight
import com.jarvis.assistant.ui.theme.JarvisTheme
import com.jarvis.assistant.ui.theme.TextPrimary
import com.jarvis.assistant.ui.theme.TextSecondary
import java.util.Locale

data class Message(val role: String, val content: String)

class MainActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // İzinleri iste
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissionLauncher.launch(permissions)

        // Türkçe TTS başlat
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("tr", "TR")
                textToSpeech?.speak(
                    "Sisteme hoş geldiniz efendim. Ben JARVIS, kişisel yapay zekâ asistanınız.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "INIT"
                )
            }
        }

        setContent {
            JarvisTheme {
                MainJarvisScreen(
                    onSpeak = { text ->
                        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTER")
                    },
                    onOpenApp = { pkg ->
                        try {
                            val intent = packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    onOpenSpotify = { query ->
                        try {
                            val uri = Uri.parse("spotify:search:${Uri.encode(query)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                            startActivity(intent)
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}

@Composable
fun MainJarvisScreen(
    onSpeak: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenSpotify: (String) -> Unit
) {
    var reactorState by remember { mutableStateOf(ArcReactorState.IDLE) }
    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            Message("assistant", "Sisteme hoş geldiniz efendim. Ben JARVIS. Tüm protokoller devrede. Size nasıl yardımcı olabilirim?")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBlack)
            .padding(16.dp)
    ) {
        // 1. Üst Bar: Hava Durumu ve Sistem Durumu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceCard)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = JarvisAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Ankara, Çankaya", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Açık ve Güneşli • 18°C", color = JarvisCyan, fontSize = 12.sp)
                }
            }
            Text(
                text = "ÇEVRİMİÇİ",
                color = JarvisElectricGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Ortadaki Fütüristik Ark Reaktörü
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcReactorView(
                    state = reactorState,
                    size = 150.dp,
                    onClick = {
                        if (reactorState == ArcReactorState.IDLE) {
                            reactorState = ArcReactorState.LISTENING
                            onSpeak("Dinliyorum efendim.")
                        } else {
                            reactorState = ArcReactorState.IDLE
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (reactorState) {
                        ArcReactorState.IDLE -> "JARVIS DEVREDE • DOKUNUN"
                        ArcReactorState.LISTENING -> "SİZİ DİNLİYORUM EFENDİM..."
                        ArcReactorState.ANALYZING -> "ANALİZ EDİLİYOR..."
                        ArcReactorState.SPEAKING -> "KONUŞUYOR..."
                    },
                    color = when (reactorState) {
                        ArcReactorState.IDLE -> JarvisCyan
                        ArcReactorState.LISTENING -> JarvisElectricGreen
                        ArcReactorState.ANALYZING -> JarvisAmber
                        ArcReactorState.SPEAKING -> JarvisCyan
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Hızlı Kısayol Butonları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortcutPill("🎬 Edit Stüdyosu") {
                messages.add(Message("user", "Edit Stüdyosunu aç"))
                messages.add(Message("assistant", "Fotoğraf ve Video düzenleme stüdyosu hazır efendim."))
                onSpeak("Edit stüdyosu hazır efendim.")
            }
            ShortcutPill("📚 Ödev Çöz") {
                messages.add(Message("user", "Ödev Asistanı"))
                messages.add(Message("assistant", "Fotoğraf yükleyin veya soruyu yazın, adım adım çözelim efendim."))
                onSpeak("Sorunuzu dinliyorum efendim.")
            }
            ShortcutPill("🎵 Spotify Aç") {
                onOpenSpotify("")
                messages.add(Message("assistant", "Spotify başlatılıyor efendim."))
                onSpeak("Spotify açılıyor efendim.")
            }
            ShortcutPill("💬 WhatsApp") {
                onOpenApp("com.whatsapp")
                messages.add(Message("assistant", "WhatsApp açılıyor efendim."))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Mesajlaşma / Asistan Sohbet Akışı
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = (msg.role == "user")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) JarvisNeonBlue.copy(alpha = 0.25f) else JarvisSurfaceCard)
                            .border(
                                1.dp,
                                if (isUser) JarvisNeonBlue.copy(alpha = 0.6f) else JarvisCyan.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "SİZ" else "JARVIS",
                                color = if (isUser) JarvisNeonBlue else JarvisCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Alt Giriş Kutusu ve Komut Gönderme
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("JARVIS'e bir emir verin...", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JarvisSurfaceCard,
                    unfocusedContainerColor = JarvisSurfaceCard,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userCmd = inputText
                        inputText = ""
                        messages.add(Message("user", userCmd))

                        // Komut yanıtı
                        val reply = when {
                            userCmd.contains("merhaba", ignoreCase = true) -> "Merhaba efendim. Sistemler emrinizde."
                            userCmd.contains("nasılsın", ignoreCase = true) -> "Tüm donanım ve yazılım çekirdeklerim en yüksek performansta çalışıyor efendim."
                            userCmd.contains("araba", ignoreCase = true) || userCmd.contains("edit", ignoreCase = true) -> "Sinematik araba editi modülü devrede efendim. Renk düzenlemesi ve speed ramp hazır."
                            userCmd.contains("müzik", ignoreCase = true) || userCmd.contains("şarkı", ignoreCase = true) -> {
                                onOpenSpotify(userCmd)
                                "İstediğiniz parça Spotify üzerinden çalınıyor efendim."
                            }
                            else -> "Emredersiniz efendim. Komutunuz işleme alındı."
                        }
                        messages.add(Message("assistant", reply))
                        onSpeak(reply)
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(JarvisCyan, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gönder", tint = JarvisBlack, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShortcutPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurfaceCard)
            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
