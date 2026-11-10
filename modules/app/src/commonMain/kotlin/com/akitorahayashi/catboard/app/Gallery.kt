@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.akitorahayashi.catboard.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@kotlin.native.HiddenFromObjC
fun Gallery(model: GalleryViewModel, imageLoader: ImageLoader, imageModel: (String) -> Any) {
    val state by model.state.collectAsState()
    val settings by model.settings.collectAsState()
    val list = rememberLazyListState()
    var showingSettings by remember { mutableStateOf(false) }

    LaunchedEffect(model) { model.loadInitialImages() }
    LaunchedEffect(state.images.firstOrNull()?.id) { list.scrollToItem(0) }
    LaunchedEffect(list, state.images.size, state.isInitializing, state.isAdditionalFetching) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } }
            .distinctUntilChanged()
            .collect { last ->
                val chunks = (state.images.size + 9) / 10
                if (last != null && chunks > 0 && last >= chunks - 1) model.fetchAdditionalImages()
            }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = { Text("Cat Board") },
                    navigationIcon = {
                        TextButton(
                            onClick = { model.refresh() },
                            enabled = !state.isInitializing && !state.isAdditionalFetching,
                            modifier = Modifier.testTag("refreshButton"),
                        ) { Text("更新", color = MaterialTheme.colors.onPrimary) }
                    },
                    actions = {
                        TextButton(onClick = { showingSettings = true }, modifier = Modifier.testTag("settingsButton")) {
                            Text("設定", color = MaterialTheme.colors.onPrimary)
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                state.warning?.let { Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colors.error) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        state.errorMessage != null -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text("エラーが発生しました", modifier = Modifier.testTag("errorTitle"))
                            Text(requireNotNull(state.errorMessage))
                            TextButton(onClick = { model.refresh() }, modifier = Modifier.testTag("retryButton")) { Text("再試行") }
                        }
                        state.isInitializing && state.images.isEmpty() -> CircularProgressIndicator()
                        else -> LazyColumn(
                            state = list,
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize().testTag("gallery"),
                        ) {
                            itemsIndexed(state.images.chunked(10), key = { _, images -> images.first().id }) { chunk, images ->
                                TieredChunk(images, chunk * 10, imageLoader, imageModel)
                            }
                            if (state.isAdditionalFetching) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                        }
                    }
                }
            }
        }
        if (showingSettings) {
            SettingsDialog(settings, onChange = { model.changeSettings(it) }, onDismiss = { showingSettings = false })
        }
    }
}
