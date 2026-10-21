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

// Donanım item modeli. Tüm alanları val yaptım ve @Immutable ekledim ki
// Compose derleyicisi gereksiz recomposition yapmayıp akıllıca atlama yapabilsin.
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

// Ana stateful ekran bileşeni. Bütün durumları (state) burada yönetiyorum (State Hoisting).
@Composable
fun DevClusterApp(modifier: Modifier = Modifier) {
    // Şartnamede verilen başlangıç test verileri
    val initialList = remember {
        listOf(
            HardwareItem(id = "101", name = "MacBook Pro M3 Max", category = "Geliştirici Bilgisayarı", cost = 3499.0, stockCount = 5),
            HardwareItem(id = "102", name = "NVIDIA RTX 4090 Sunucu", category = "Yapay Zeka / GPU", cost = 1599.0, stockCount = 2),
            HardwareItem(id = "103", name = "Google Pixel 8 Pro", category = "Mobil Test Cihazı", cost = 999.0, stockCount = 8),
            HardwareItem(id = "104", name = "Dell UltraSharp 49\"", category = "Monitör", cost = 1199.0, stockCount = 4),
            HardwareItem(id = "105", name = "Sony WH-1000XM5 Kulaklık", category = "Odaklanma Ekipmanı", cost = 399.0, stockCount = 12),
            HardwareItem(id = "106", name = "Raspberry Pi 5 8GB", category = "IoT Geliştirme Kiti", cost = 85.0, stockCount = 25),
            HardwareItem(id = "107", name = "HHKB Professional Keyboard", category = "Ergonomik Klavye", cost = 299.0, stockCount = 0), // Stok sınırı testi
            HardwareItem(id = "108", name = "AMD Ryzen Threadripper", category = "Yapay Zeka / GPU", cost = 4999.0, stockCount = 1)
        )
    }

    // Tıklayınca stokların düşmesi için liste durumunu burada tutuyorum.
    // HardwareItem Parcelable olmadığı için crash vermesin diye remember kullandım.
    var items by remember { mutableStateOf(initialList) }

    // Ekranı yan çevirince (rotate) aranan metin silinmesin diye rememberSaveable kullandım.
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Harcanan ödenek miktarını Compose'un Slot Table yapısına kilitliyorum.
    var totalBudgetSpent by remember { mutableDoubleStateOf(0.0) }

    // Turnike Mantığı (Memoization): Her recomposition'da filtreyi gereksiz yere çalıştırmayıp
    // sadece searchQuery veya items değiştiğinde CPU harcaması için remember içine aldım.
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
        // Ödenek özet kartı
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

        // Stateless Arama Çubuğu. State yukarıdan gidiyor, yazılan metin lambda ile yukarı geliyor.
        HardwareSearchBar(
            query = searchQuery,
            onQueryChanged = { newQuery -> searchQuery = newQuery }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Donanım Listesi. Tembel yükleme için LazyColumn ve nokta atışı recomposition için key kullandım.
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
                    key = { item -> item.id } // Nokta Atışı Recomposition için id verdim
                ) { item ->
                    // Stateless Kart bileşeni
                    HardwareCard(
                        item = item,
                        onCardClick = { clickedItem ->
                            // Stok kontrolü yapıp bütçeyi arttırıyorum ve ilgili ürünün stok sayısını 1 düşürüyorum
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

// Arama Çubuğu (Stateless). İçinde hiçbir remember veya local state yok.
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

// Donanım Kartı (Stateless).
// Görsel margin hatalarının önüne geçmek için modifier sıralamasında .clickable()'ı .padding()'den önce yazdım.
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
            // Modifier sıralaması kuralı: Önce .clickable(), sonra .padding()
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

                // Stok rozeti
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

@Preview(showBackground = true)
@Composable
fun DevClusterAppPreview() {
    Week2_assignmentTheme {
        DevClusterApp()
    }
}
