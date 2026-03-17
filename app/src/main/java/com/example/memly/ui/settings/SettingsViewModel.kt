package com.example.memly.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memly.data.local.MemlyDatabase
import com.example.memly.data.local.dao.MemoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val memoryCount: Int = 0,
    val mediaCount: Int = 0,
    val diskUsageBytes: Long = 0,
    val isLoading: Boolean = true,
    val showClearDialog: Boolean = false,
    val showFinalConfirmDialog: Boolean = false,
    val isClearing: Boolean = false,
    val clearComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MemlyDatabase,
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    val memoryCount = memoryDao.getMemoryCount()
                    val mediaCount = memoryDao.getMediaFileCount()
                    val mediaDir = File(context.filesDir, "media")
                    val thumbnailDir = File(context.cacheDir, "thumbnails")
                    val diskUsage = calculateDirSize(mediaDir) + calculateDirSize(thumbnailDir)
                    Triple(memoryCount, mediaCount, diskUsage)
                }
                _uiState.update {
                    it.copy(
                        memoryCount = stats.first,
                        mediaCount = stats.second,
                        diskUsageBytes = stats.third,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load stats: ${e.message}") }
            }
        }
    }

    fun showClearDialog() {
        _uiState.update { it.copy(showClearDialog = true) }
    }

    fun hideClearDialog() {
        _uiState.update { it.copy(showClearDialog = false, showFinalConfirmDialog = false) }
    }

    fun showFinalConfirmDialog() {
        _uiState.update { it.copy(showClearDialog = false, showFinalConfirmDialog = true) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true, showFinalConfirmDialog = false) }
            try {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    File(context.filesDir, "media").deleteRecursively()
                    File(context.cacheDir, "thumbnails").deleteRecursively()
                }
                _uiState.update {
                    it.copy(
                        memoryCount = 0,
                        mediaCount = 0,
                        diskUsageBytes = 0,
                        isClearing = false,
                        clearComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isClearing = false, error = "Failed to clear data: ${e.message}") }
            }
        }
    }

    fun clearCompleteAcknowledged() {
        _uiState.update { it.copy(clearComplete = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
