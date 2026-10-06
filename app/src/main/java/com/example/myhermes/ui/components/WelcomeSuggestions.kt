package com.example.myhermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhermes.ui.theme.GeminiBlue
import com.example.myhermes.ui.theme.GeminiCyan
import com.example.myhermes.ui.theme.GeminiPurple

data class SuggestionPrompt(
    val title: String,
    val description: String,
    val prompt: String,
    val icon: ImageVector,
    val iconTint: Color
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun WelcomeSuggestions(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        SuggestionPrompt(
            title = "Bantu Coding Android",
            description = "Buatkan contoh komponen UI Gemini kustom",
            prompt = "Buatkan contoh kode Jetpack Compose untuk komponen UI Gemini kustom di Android",
            icon = Icons.Default.Code,
            iconTint = GeminiBlue
        ),
        SuggestionPrompt(
            title = "Analisis & Refactor",
            description = "Bantu optimasi arsitektur & performa app",
            prompt = "Berikan panduan refactoring dan optimasi performa untuk Jetpack Compose ViewModel",
            icon = Icons.Default.ElectricBolt,
            iconTint = GeminiPurple
        ),
        SuggestionPrompt(
            title = "Penalaran & Logika",
            description = "Jelaskan konsep Kotlin Coroutines & Flow",
            prompt = "Jelaskan cara kerja Kotlin StateFlow dan SharedFlow beserta contoh penggunaan terbaiknya",
            icon = Icons.Default.Psychology,
            iconTint = GeminiCyan
        ),
        SuggestionPrompt(
            title = "Bantu Tulis & Konten",
            description = "Buatkan draf spesifikasi fitur aplikasi",
            prompt = "Buatkan draf spesifikasi teknis (PRD) untuk fitur AI Agent di aplikasi mobile",
            icon = Icons.Default.EditNote,
            iconTint = GeminiBlue
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Gemini Gradient Greeting Text
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(GeminiBlue, GeminiPurple, GeminiCyan)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = GeminiBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Halo, Dev",
                style = TextStyle(
                    brush = gradientBrush,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ada yang bisa Hermes AI Agent bantu hari ini?",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Horizontal Carousel of Suggestion Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suggestions) { item ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onSuggestionClick(item.prompt) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = item.iconTint
                            )
                        }
                    }
                }
            }
        }
    }
}
