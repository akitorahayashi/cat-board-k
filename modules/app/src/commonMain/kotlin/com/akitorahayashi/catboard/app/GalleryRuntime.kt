package com.akitorahayashi.catboard.app

import com.akitorahayashi.catboard.imageloader.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

class GalleryRuntime internal constructor(
    private val scope: CoroutineScope,
    private val loader: Loader,
    private val createModel: (CoroutineScope) -> GalleryViewModel,
) {
    fun createSession(): GallerySession {
        check(scope.isActive) { "The gallery runtime is closed" }
        val sessionScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        return GallerySession(createModel(sessionScope), loader, sessionScope)
    }

    fun close() { scope.cancel() }
}
