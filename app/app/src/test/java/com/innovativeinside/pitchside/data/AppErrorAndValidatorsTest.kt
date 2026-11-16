package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.core.Validators
import com.innovativeinside.pitchside.core.toUiState
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorAndValidatorsTest {
    @Test fun throwableMapping() {
        assertTrue(AppError.from(SocketTimeoutException()) is AppError.Timeout)
        assertTrue(AppError.from(UnknownHostException()) is AppError.Network)
        assertTrue(AppError.from(IOException()) is AppError.Network)
        assertTrue(AppError.from(SerializationException("x")) is AppError.Parse)
        assertTrue(AppError.from(IllegalStateException()) is AppError.Unknown)
        val e = AppError.Forbidden()
        assertTrue(AppError.from(e) === e)
    }

    @Test fun httpMapping() {
        assertTrue(AppError.fromHttp(401) is AppError.Unauthorized)
        assertTrue(AppError.fromHttp(403) is AppError.Forbidden)
        assertTrue(AppError.fromHttp(404) is AppError.NotFound)
        assertEquals("bad date", AppError.fromHttp(422, "bad date").message)
        assertTrue(AppError.fromHttp(429) is AppError.RateLimited)
        assertEquals(502, (AppError.fromHttp(502) as AppError.Server).code)
        assertTrue(AppError.fromHttp(418) is AppError.Unknown)
    }

    @Test fun retryability() {
        assertTrue(AppError.Network().retryable)
        assertTrue(AppError.Server(500).retryable)
        assertFalse(AppError.Unauthorized().retryable)
        assertFalse(AppError.NotFound().retryable)
        assertFalse(AppError.Validation("x").retryable)
    }

    @Test fun resultToUiState() {
        assertEquals(UiState.Empty, Result.success(emptyList<Int>()).toUiState())
        assertEquals(UiState.Success(listOf(1)), Result.success(listOf(1)).toUiState())
        val err = Result.failure<List<Int>>(IOException()).toUiState()
        assertTrue(err is UiState.Error && err.error is AppError.Network)
    }

    @Test fun emailRules() {
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("bad@"))
        assertNotNull(Validators.email("a@b"))
        assertNull(Validators.email("fan@pitchside.app"))
        assertNull(Validators.email("  fan@pitchside.app "))
    }

    @Test fun passwordRules() {
        assertNotNull(Validators.password(""))
        assertNotNull(Validators.password("short1"))
        assertNotNull(Validators.password("onlyletters"))
        assertNotNull(Validators.password("12345678"))
        assertNull(Validators.password("goal2026!"))
        assertNull(Validators.loginPassword("x"))
        assertNotNull(Validators.confirm("a", "b"))
        assertNull(Validators.confirm("a", "a"))
        assertNotNull(Validators.displayName(" "))
        assertNotNull(Validators.displayName("x".repeat(41)))
    }
}
