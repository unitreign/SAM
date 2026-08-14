package fyi.reign.sam

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.reign.sam.apk.APKPatcher
import fyi.reign.sam.data.ShortcutRepository
import fyi.reign.sam.data.db.ShortcutEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ShortcutRepository(app)

    val shortcuts: StateFlow<List<ShortcutEntry>> = repo.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entry: ShortcutEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    fun reinstall(entry: ShortcutEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = entry.iconPath?.let { BitmapFactory.decodeFile(it) }
            APKPatcher.patchAndInstall(getApplication(), entry.generatedPackageName, entry.label, bitmap)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            shortcuts.value.forEach { repo.refreshInstallStatus(it) }
        }
    }
}
