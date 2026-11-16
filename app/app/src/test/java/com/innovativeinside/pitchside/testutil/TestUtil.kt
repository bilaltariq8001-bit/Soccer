package com.innovativeinside.pitchside.testutil

import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.DispatcherProvider
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** 2026-11-12 18:30 UTC: today has live matches in the mock data. */
class FixedClock(var now: Long = 1_794_508_200_000L, private val z: ZoneId = ZoneOffset.UTC) : AppClock {
    override fun nowMs() = now
    override fun zone() = z
}

class TestDispatchers(val d: CoroutineDispatcher) : DispatcherProvider {
    override val io = d
    override val default = d
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
