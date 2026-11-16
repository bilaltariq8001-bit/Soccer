package com.innovativeinside.pitchside.core

import java.time.LocalDate
import java.time.ZoneId

/** Time source, injectable for tests. */
interface AppClock {
    fun nowMs(): Long
    fun zone(): ZoneId
    fun today(): LocalDate = java.time.Instant.ofEpochMilli(nowMs()).atZone(zone()).toLocalDate()
}

object SystemClock : AppClock {
    override fun nowMs(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
