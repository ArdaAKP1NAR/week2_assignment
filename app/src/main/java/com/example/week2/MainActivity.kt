package com.example.week2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.week2.ui.theme.Week2_assignmentTheme

// ============================================================================
// A. VERİ KATMANI (DATA LAYER) VE DERLEYİCİ SÖZLEŞMESİ
// ============================================================================
// • Değişmezlik (Immutability): Tüm alanlar 'val' ile tanımlanarak Race Condition engellenir.
// • Kararlılık Sözleşmesi: Compose derleyicisinin Akıllı Atlama (Smart Recomposition Skipping)
//   motorunu tetiklemek için @Immutable anotasyonu kullanılmıştır.
@Immutable
data class HardwareItem(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val stockCount: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Week2_assignmentTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DevClusterApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ============================================================================
// B & D. STATEFUL ANA KAPSAYICI (STATE HOISTING MERKEZİ)
// ============================================================================
@Composable
fun DevClusterApp(modifier: Modifier = Modifier) {
    // Sabit Veri Kümesi (Ödev Şartnamesindeki Test Verisi)
    val initialList = remember {
        listOf(
            HardwareItem(id = "101", name = "MacBook Pro M3 Max", category = "Geliştirici Bilgisayarı", cost = 3499.0, stockCount = 5),
            HardwareItem(id = "102", name = "NVIDIA RTX 4090 Sunucu", category = "Yapay Zeka / GPU", cost = 1599.0, stockCount = 2),
            HardwareItem(id = "103", name = "Google Pixel 8 Pro", category = "Mobil Test Cihazı", cost = 999.0, stockCount = 8),
            HardwareItem(id = "104", name = "Dell UltraSharp 49\"", category = "Monitör", cost = 1199.0, stockCount = 4),
            HardwareItem(id = "105", name = "Sony WH-1000XM5 Kulaklık", category = "Odaklanma Ekipmanı", cost = 399.0, stockCount = 12),
            HardwareItem(id = "106", name = "Raspberry Pi 5 8GB", category = "IoT Geliştirme Kiti", cost = 85.0, stockCount = 25),
            HardwareItem(id = "107", name = "HHKB Professional Keyboard", category = "Ergonomik Klavye", cost = 299.0, stockCount = 0), // Stok sınırı test verisi
            HardwareItem(id = "108", name = "AMD Ryzen Threadripper", category = "Yapay Zeka / GPU", cost = 4999.0, stockCount = 1)
        )
    }

    // İŞ 2: Dinamik Stok Yönetimi State'i (rememberSaveable KULLANILMADI)
    var items by remember { mutableStateOf(initialList) }

    // B. Konfigürasyon Değişim Koruması:
    // Cihaz döndürüldüğünde (Portrait -> Landscape) arama metni RAM dışı geçici Bundle alanında muhafaza edilir.
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // B. Çalışma Zamanı Hafıza Kilidi:
    // Harcanan ödenek miktarı Compose'un Slot Table yapısına 'remember' makrosu ile kilitlenmiştir.
    var totalBudgetSpent by remember { mutableDoubleStateOf(0.0) }

    // B. Turnike Mantığı (Memoization):
    // Arama ve filtreleme algoritması 'remember(searchQuery, items)' önbellek filtresi arkasındadır.
    // Recomposition esnasında boş yere CPU tüketmez, sadece searchQuery veya items değiştiğinde yeniden hesaplanır.
    val filteredList = remember(searchQuery, items) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Başlık ve Ödenek Özet Kartı
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DevCluster Kaynak Yönetimi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Harcanan Toplam Ödenek: $%.2f".format(totalBudgetSpent),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // D. Stateless Arama Çubuğu (State Down, Event Up)
        HardwareSearchBar(
            query = searchQuery,
            onQueryChanged = { newQuery -> searchQuery = newQuery }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // C. Liste Mimarisi (LazyColumn ve Key Parametresi)
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aramanızla eşleşen donanım kaynağı bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredList,
                    key = { item -> item.id } // Nokta Atışı Recomposition için benzersiz kimlik (id)
                ) { item ->
                    // D. Stateless Donanım Kartı
                    HardwareCard(
                        item = item,
                        onCardClick = { clickedItem ->
                            // İŞ 2: Stok azaltma ve ödenek güncelleme mantığı
                            if (clickedItem.stockCount > 0) {
                                totalBudgetSpent += clickedItem.cost
                                items = items.map {
                                    if (it.id == clickedItem.id) it.copy(stockCount = it.stockCount - 1) else it
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// D. SAF FONKSİYON TASARIMI - STATELESS ARAMA ÇUBUĞU
// ============================================================================
// Ekranda yerel hiçbir 'remember' hücresi barındırmaz.
// Durumu dışarıdan alır (query), değişiklik olayını yukarı fırlatır (onQueryChanged).
@Composable
fun HardwareSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        label = { Text("Donanım veya Kategori Ara...") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

// ============================================================================
// D. SAF FONKSİYON TASARIMI - STATELESS DONANIM KARTI
// ============================================================================
// Ekranda yerel hiçbir 'remember' hücresi barındırmaz.
// D. Modifier Sıralaması: Önce .clickable(), ardından .padding() uygulanmıştır.
@Composable
fun HardwareCard(
    item: HardwareItem,
    onCardClick: (HardwareItem) -> Unit
) {
    val isAvailable = item.stockCount > 0

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            // D. MODIFIER SIRALAMASI KURALI:
            // Önce .clickable(), ardından .padding() hiyerarşisi işletilmiştir.
            .clickable(enabled = isAvailable) { onCardClick(item) }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Stok Durumu Rozeti (Badge)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isAvailable) "Stok: ${item.stockCount}" else "Stok Yok",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Kategori: ${item.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Maliyet: $%.2f".format(item.cost),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================================================
// PREVIEW BİLEŞENLERİ
// ============================================================================
@Preview(showBackground = true)
@Composable
fun DevClusterAppPreview() {
    Week2_assignmentTheme {
        DevClusterApp()
    }
}
