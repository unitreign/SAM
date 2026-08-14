package fyi.reign.sam

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.reign.sam.data.db.ShortcutEntry
import fyi.reign.sam.ui.theme.ShortcutAPKMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShortcutAPKMakerTheme {
                SAMDashboard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAMDashboard(vm: MainViewModel = viewModel()) {
    val shortcuts by vm.shortcuts.collectAsState()
    var selectedEntry by remember { mutableStateOf<ShortcutEntry?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.refreshAll() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shortcut APK Maker") },
                actions = {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }) { Text("Set Launcher") }
                }
            )
        }
    ) { padding ->
        if (shortcuts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shortcuts yet.\n\nTap \"Set Launcher\", choose SAM, then create a shortcut in any app.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(shortcuts, key = { it.id }) { entry ->
                    ShortcutCard(entry = entry, onLongPress = { selectedEntry = entry })
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.label) },
            text = { Text(if (entry.isInstalled) "Installed" else "Not installed") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reinstall(entry)
                    selectedEntry = null
                }) { Text("Reinstall") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.delete(entry)
                    selectedEntry = null
                }) { Text("Delete") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutCard(entry: ShortcutEntry, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onLongClick = onLongPress, onClick = {})
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(entry.iconPath) {
                entry.iconPath?.let { BitmapFactory.decodeFile(it) }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleMedium)
                Text(entry.generatedPackageName, style = MaterialTheme.typography.bodySmall)
            }
            if (entry.isInstalled) {
                Badge { Text("✓") }
            }
        }
    }
}
