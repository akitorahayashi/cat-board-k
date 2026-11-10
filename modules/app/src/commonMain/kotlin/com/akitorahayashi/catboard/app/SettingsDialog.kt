package com.akitorahayashi.catboard.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.akitorahayashi.catboard.imagescreener.ScreeningSettings

@Composable
internal fun SettingsDialog(settings: ScreeningSettings, onChange: (ScreeningSettings) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Settings", style = MaterialTheme.typography.h6)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("画像スクリーニング", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.isScreeningEnabled,
                        onCheckedChange = { onChange(settings.copy(isScreeningEnabled = it)) },
                        modifier = Modifier.testTag("screeningToggle"),
                    )
                }
                Text("不適切な画像を除外します。オフにするとすべての画像が表示されます。", style = MaterialTheme.typography.body2)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("スケアリーモード", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.scaryMode,
                        onCheckedChange = { onChange(settings.copy(scaryMode = it)) },
                        enabled = settings.isScreeningEnabled,
                        modifier = Modifier.testTag("scaryModeToggle"),
                    )
                }
                Text("危険と判定された画像のみを表示します。", style = MaterialTheme.typography.body2)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).testTag("doneButton")) { Text("完了") }
            }
        }
    }
}
