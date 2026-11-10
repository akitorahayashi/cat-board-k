package com.akitorahayashi.catboard.app

import kotlinx.coroutines.Job

class Observation internal constructor(private val job: Job) {
    fun cancel() { job.cancel() }
}
