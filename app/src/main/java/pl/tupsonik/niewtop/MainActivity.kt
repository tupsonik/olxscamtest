package pl.tupsonik.niewtop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NieWtopTheme {
                NieWtopHome()
            }
        }
    }
}

@Composable
private fun NieWtopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF111827),
            secondary = Color(0xFF16A34A),
            background = Color(0xFFF7F8FA),
            surface = Color.White
        ),
        content = content
    )
}

@Composable
private fun NieWtopHome() {
    var url by remember { mutableStateOf("") }
    var showDemo by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Nie Wtop",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Sprawdź, zanim zapłacisz.",
                        color = Color(0xFF667085)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF111827), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 22.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Masz ofertę?",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Wklej link. Poszukamy sygnałów, które warto sprawdzić przed płatnością.",
                        color = Color(0xFFD0D5DD),
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://...", color = Color(0xFF98A2B3)) },
                        label = { Text("Link do oferty", color = Color(0xFF98A2B3)) }
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showDemo = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = url.isNotBlank(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Sprawdź ofertę", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "Możesz też",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111827)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showDemo = true },
                    label = { Text("📷 Screenshot") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = { showDemo = true },
                    label = { Text("↗ Udostępnij") },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }

            if (showDemo) {
                ResultPreview()
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = { showDemo = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Jak działa Nie Wtop?")
            }
        }
    }
}

@Composable
private fun ResultPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wstępna analiza", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Nie jest to dowód oszustwa.", color = Color(0xFF667085), fontSize = 13.sp)
                }
                Text("⚠️", fontSize = 25.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Do sprawdzenia", fontWeight = FontWeight.Bold, color = Color(0xFFB54708))
            Spacer(Modifier.height(6.dp))
            Text("• cena znacząco odbiega od podobnych ofert")
            Text("• płatność prowadzi poza systemem platformy")
            Text("• część informacji o sprzedawcy wymaga weryfikacji")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Pokaż dowody i źródła")
            }
        }
    }
}
