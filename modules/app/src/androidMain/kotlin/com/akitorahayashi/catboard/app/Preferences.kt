package com.akitorahayashi.catboard.app

import android.content.Context
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class Preferences(context: Context) : SettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences("screening", Context.MODE_PRIVATE)
    private val current = MutableStateFlow(
        ScreeningSettings(preferences.getBoolean("isScreeningEnabled", true), preferences.getBoolean("scaryMode", false)),
    )
    override val settings = current.asStateFlow()

    override suspend fun save(value: ScreeningSettings) {
        withContext(Dispatchers.IO) {
            check(preferences.edit()
                .putBoolean("isScreeningEnabled", value.isScreeningEnabled)
                .putBoolean("scaryMode", value.scaryMode)
                .commit()) { "Screening settings could not be saved" }
        }
        current.value = value
    }
}
