package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.FollowKind
import com.innovativeinside.pitchside.data.prefs.Persona
import com.innovativeinside.pitchside.data.prefs.PrefsReducer
import com.innovativeinside.pitchside.data.prefs.PrefsReducer.FollowResult
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.data.prefs.WatchStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsReducerTest {
    @Test fun followAndUnfollowTeam() {
        val p = UserPrefs(alertLevel = AlertLevel.Every)
        val r = PrefsReducer.toggleTeam(p, "ng") as FollowResult.Ok
        assertTrue(r.following)
        assertEquals(listOf("ng"), r.prefs.teams)
        assertEquals(AlertLevel.Every, r.prefs.teamAlerts["ng"])
        val shielded = PrefsReducer.toggleShield(r.prefs, "ng")
        val off = PrefsReducer.toggleTeam(shielded, "ng") as FollowResult.Ok
        assertFalse(off.following)
        assertTrue(off.prefs.teams.isEmpty() && off.prefs.shielded.isEmpty() && off.prefs.teamAlerts.isEmpty())
    }

    @Test fun freeLimitBlocksSixthTeamButNotPro() {
        val five = UserPrefs(teams = listOf("a", "b", "c", "d", "e"))
        assertEquals(FollowResult.LimitReached, PrefsReducer.toggleTeam(five, "f"))
        assertTrue(PrefsReducer.toggleTeam(five.copy(isPro = true), "f") is FollowResult.Ok)
        // Unfollowing is always allowed at the limit.
        assertTrue(PrefsReducer.toggleTeam(five, "a") is FollowResult.Ok)
    }

    @Test fun spoilerShieldHidesOnlyScoredUnrevealedShieldedMatches() {
        val p = UserPrefs(teams = listOf("ng"), shielded = listOf("ng"))
        assertTrue(PrefsReducer.isHidden(p, "m1", "ng", "ru", hasScore = true))
        assertFalse(PrefsReducer.isHidden(p, "m1", "ng", "ru", hasScore = false))
        assertFalse(PrefsReducer.isHidden(p, "m2", "hc", "rr", hasScore = true))
        val revealed = PrefsReducer.reveal(p, "m1")
        assertFalse(PrefsReducer.isHidden(revealed, "m1", "ng", "ru", true))
        assertEquals(revealed, PrefsReducer.reveal(revealed, "m1"))
        val caught = PrefsReducer.caughtUp(p, listOf("m1", "m9"))
        assertEquals(listOf("m1", "m9"), caught.revealed)
        assertTrue(PrefsReducer.shieldAll(caught, false).shielded.isEmpty())
    }

    @Test fun personaAssignment() {
        assertEquals(Persona.Casual, PrefsReducer.persona(UserPrefs(followKind = FollowKind.BigOnly)))
        assertEquals(Persona.CatchUp, PrefsReducer.persona(UserPrefs(watchStyle = WatchStyle.Later, teams = listOf("ng"))))
        assertEquals(Persona.Nerd, PrefsReducer.persona(UserPrefs(followKind = FollowKind.Players, detailed = true, teams = listOf("x"))))
        assertEquals(Persona.Explorer, PrefsReducer.persona(UserPrefs()))
        assertEquals(Persona.Fanatic, PrefsReducer.persona(UserPrefs(teams = listOf("ng"))))
    }

    @Test fun completeOnboardingAppliesCatchUpDefaults() {
        val p = PrefsReducer.completeOnboarding(UserPrefs(watchStyle = WatchStyle.Later, teams = listOf("ng", "ru"), alertLevel = AlertLevel.Key))
        assertTrue(p.onboarded)
        assertEquals(Persona.CatchUp, p.persona)
        assertEquals(listOf("ng", "ru"), p.shielded)
        assertEquals(AlertLevel.Final, p.alertLevel)
        assertEquals(AlertLevel.Final, p.teamAlerts["ru"])
    }

    @Test fun askQuotaResetsDaily() {
        var p = UserPrefs()
        repeat(UserPrefs.FREE_ASK_LIMIT) { p = PrefsReducer.useAsk(p, 100)!! }
        assertEquals(0, PrefsReducer.askLeft(p, 100))
        assertNull(PrefsReducer.useAsk(p, 100))
        assertEquals(UserPrefs.FREE_ASK_LIMIT, PrefsReducer.askLeft(p, 101))
        assertEquals(UserPrefs.PRO_ASK_LIMIT - UserPrefs.FREE_ASK_LIMIT, PrefsReducer.askLeft(p.copy(isPro = true), 100))
    }
}
