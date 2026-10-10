package com.codetoday.remembrance.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetoday.remembrance.RemembranceApp
import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage
import com.codetoday.remembrance.service.RemembranceForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemembranceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RemembranceApp).repository

    val reminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceRunning: StateFlow<Boolean> = RemembranceForegroundService.isRunningState

    private val _selectedLanguage = MutableStateFlow(SupportedLanguage.BENGALI)
    val selectedLanguage: StateFlow<SupportedLanguage> = _selectedLanguage.asStateFlow()

    init {
        // Seed default item if empty
        viewModelScope.launch {
            if (repository.getCount() == 0) {
                repository.insert(
                    ReminderItem(
                        text = "সুবহানাল্লাহি ওয়া বিহামদিহি",
                        language = SupportedLanguage.BENGALI,
                        fontName = "Noto Serif Bengali",
                        durationSeconds = 5,
                        frequencyMinutes = 1,
                        frequencyUnit = "MINUTES",
                        position = OverlayPosition.TOP
                    )
                )
            }
        }
    }

    fun setLanguage(language: SupportedLanguage) {
        _selectedLanguage.value = language
    }

    fun addReminder(language: SupportedLanguage) {
        viewModelScope.launch {
            val defaultText = when (language) {
                SupportedLanguage.BENGALI -> "আলহামদুলিল্লাহ"
                SupportedLanguage.ENGLISH -> "Stay mindful & grateful"
                SupportedLanguage.ARABIC -> "لا إله إلا الله"
            }
            val defaultFont = language.defaultFont

            repository.insert(
                ReminderItem(
                    text = defaultText,
                    language = language,
                    fontName = defaultFont,
                    durationSeconds = 5,
                    frequencyMinutes = 1,
                    frequencyUnit = "MINUTES",
                    position = OverlayPosition.TOP
                )
            )
        }
    }

    fun updateReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.update(reminder)
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.delete(reminder)
        }
    }

    fun toggleService() {
        val context = getApplication<Application>()
        if (isServiceRunning.value) {
            RemembranceForegroundService.stop(context)
        } else {
            RemembranceForegroundService.start(context)
        }
    }
}
