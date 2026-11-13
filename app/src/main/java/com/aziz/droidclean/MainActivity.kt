package com.aziz.droidclean

import android.app.ActivityManager
import android.os.Bundle
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DroidCleanDashboard(
                        deviceName = android.os.Build.MODEL,
                        androidVersion = android.os.Build.VERSION.RELEASE,
                        memory = readMemory(),
                        storage = readStorage()
                    )
                }
            }
        }
    }

    private fun readMemory(): MemoryStatus {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return MemoryStatus(
            totalMb = info.totalMem / (1024L * 1024L),
            availableMb = info.availMem / (1024L * 1024L),
            lowMemory = info.lowMemory
        )
    }

    private fun readStorage(): StorageStatus {
        val stats = StatFs(filesDir.absolutePath)
        val total = stats.totalBytes
        val available = stats.availableBytes
        return StorageStatus(
            totalGb = total / 1_000_000_000.0,
            availableGb = available / 1_000_000_000.0
        )
    }
}

data class MemoryStatus(
    val totalMb: Long,
    val availableMb: Long,
    val lowMemory: Boolean
)

data class StorageStatus(
    val totalGb: Double,
    val availableGb: Double
)

@Composable
private fun DroidCleanDashboard(
    deviceName: String,
    androidVersion: String,
    memory: MemoryStatus,
    storage: StorageStatus
) {
    val modules = listOf(
        "Speicheranalyse" to "Freien Gerätespeicher anzeigen",
        "RAM-Monitor" to "Arbeitsspeicher prüfen",
        "Datei-Check" to "Große Dateien später finden",
        "Akku-Analyse" to "Energieverbrauch später prüfen"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("DroidClean", style = MaterialTheme.typography.headlineLarge)
        Text(
            "$deviceName · Android $androidVersion",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Gerätestatus", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Freier RAM: ${memory.availableMb} MB von ${memory.totalMb} MB"
                )
                Text(
                    if (memory.lowMemory) "Android meldet wenig Arbeitsspeicher"
                    else "Android meldet keinen akuten RAM-Engpass"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Freier Speicher: ${String.format(Locale.GERMANY, "%.1f", storage.availableGb)} GB"
                )
                Text(
                    "Gesamtspeicherbereich: ${String.format(Locale.GERMANY, "%.1f", storage.totalGb)} GB"
                )
            }
        }

        Text("Werkzeuge", style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 145.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(modules) { module ->
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Text(module.first, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(module.second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text(
            "Erste Version · Analyse ohne automatische Löschung",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
