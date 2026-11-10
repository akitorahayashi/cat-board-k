package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

internal class Preferences(defaultScreeningEnabled: Boolean, private val defaults: NSUserDefaults) : SettingsStore {
    private val current = MutableStateFlow(
        ScreeningSettings(
            if (defaults.objectForKey("isScreeningEnabled") == null) defaultScreeningEnabled else defaults.boolForKey("isScreeningEnabled"),
            defaults.boolForKey("scaryMode"),
        ),
    )
    override val settings = current.asStateFlow()

    override suspend fun save(value: ScreeningSettings) {
        defaults.setBool(value.isScreeningEnabled, "isScreeningEnabled")
        defaults.setBool(value.scaryMode, "scaryMode")
        current.value = value
    }
}
