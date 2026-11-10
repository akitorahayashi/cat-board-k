@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imagescreener.ScreeningSettings
import kotlinx.coroutines.flow.StateFlow

@kotlin.native.HiddenFromObjC
interface SettingsStore {
    val settings: StateFlow<ScreeningSettings>
    suspend fun save(value: ScreeningSettings)
}
