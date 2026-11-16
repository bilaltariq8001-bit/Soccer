package com.innovativeinside.pitchside.data.insight

import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.Status
import kotlin.math.abs

/**
 * Short match briefs written from real data only: the table, recent form, the published win probability, head-to-head
 * results and goal events. Pure and deterministic (unit tested). Returns "" when there is too little data for at
 * least two useful sentences, so the UI never shows filler.
 */
object MatchBrief {

    fun ordinal(n: Int): String = n.toString() + when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    /** Last-five form as "won 3 of their last 5" style text; null when there is no form. */
    private fun formLine(form: String): String? {
        if (form.isBlank()) return null
        val w = form.count { it == 'W' }; val d = form.count { it == 'D' }; val l = form.count { it == 'L' }
        val n = form.length
        return when {
            w == n -> "have won all of their last $n"
            l == n -> "have lost all of their last $n"
            w == 0 && l == 0 -> "have drawn their last $n"
            w == 0 -> "are without a win in $n"
            l == 0 -> "are unbeaten in $n ($w W, $d D)"
            else -> "have won $w of their last $n"
        }
    }

    /** Form string ("WDLWW", oldest first) of [teamId] from finished matches. */
    fun formFrom(teamId: String, matches: List<Match>, max: Int = 5): String =
        matches.filter { it.status == Status.Finished && it.hasScore && it.involves(listOf(teamId)) }
            .sortedBy { it.kickoffMs }
            .takeLast(max)
            .joinToString("") { m ->
                val mine = if (m.home.id == teamId) m.homeScore!! else m.awayScore!!
                val theirs = if (m.home.id == teamId) m.awayScore!! else m.homeScore!!
                when { mine > theirs -> "W"; mine < theirs -> "L"; else -> "D" }
            }

    fun of(
        m: Match,
        table: List<StandingRow> = emptyList(),
        h2h: List<Match> = emptyList(),
        events: List<MatchEvent> = emptyList(),
        homeForm: String = "",
        awayForm: String = "",
    ): String {
        val h = table.firstOrNull { it.team.id == m.home.id }
        val a = table.firstOrNull { it.team.id == m.away.id }
        val hForm = h?.form?.takeIf { it.isNotBlank() } ?: homeForm
        val aForm = a?.form?.takeIf { it.isNotBlank() } ?: awayForm
        val out = mutableListOf<String>()
        fun pos(r: StandingRow?) = r?.let { " (${ordinal(it.rank)}, ${it.points} pts)" }.orEmpty()

        when (m.status) {
            Status.Live, Status.Finished -> if (m.hasScore) {
                val hs = m.homeScore!!; val aw = m.awayScore!!
                val live = m.status == Status.Live
                out += when {
                    hs == aw && live -> "${m.home.name} and ${m.away.name} are level at $hs–$aw${if (m.label.isNotBlank()) " (${m.label})" else ""}."
                    hs == aw -> "${m.home.name} and ${m.away.name} drew $hs–$aw."
                    live -> "${if (hs > aw) m.home.name else m.away.name} lead ${maxOf(hs, aw)}–${minOf(hs, aw)}${if (m.label.isNotBlank()) " at ${m.label}" else ""}."
                    else -> "${if (hs > aw) m.home.name else m.away.name} beat ${if (hs > aw) m.away.name else m.home.name} ${maxOf(hs, aw)}–${minOf(hs, aw)}."
                }
                val goals = events.filter { it.kind == EventKind.Goal }.sortedBy { it.minute }
                if (goals.isNotEmpty()) {
                    val byPlayer = goals.groupBy { it.player }
                    val brace = byPlayer.entries.firstOrNull { it.value.size >= 2 }
                    out += if (brace != null) "${brace.key} scored ${if (brace.value.size == 2) "twice" else "${brace.value.size} goals"}."
                    else "Goals: " + goals.take(4).joinToString(", ") { "${it.player} ${it.minuteLabel}" } + "."
                    val reds = events.count { it.kind == EventKind.RedCard }
                    if (reds > 0) out += if (reds == 1) "One red card was shown." else "$reds red cards were shown."
                }
                if (h != null && a != null) out += "Before this, ${m.home.name} were ${ordinal(h.rank)} and ${m.away.name} ${ordinal(a.rank)} in the table."
            }
            else -> {
                if (h != null && a != null) out += "${m.home.name}${pos(h)} host ${m.away.name}${pos(a)}."
                else if (h != null || a != null) out += "${m.home.name}${pos(h)} host ${m.away.name}${pos(a)}."
            }
        }

        if (m.status != Status.Finished) {
            val hl = formLine(hForm); val al = formLine(aForm)
            when {
                hl != null && al != null -> out += "${m.home.name} $hl; ${m.away.name} $al."
                hl != null -> out += "${m.home.name} $hl."
                al != null -> out += "${m.away.name} $al."
            }
            m.probs?.takeIf { m.status == Status.Upcoming }?.let { p ->
                val fav = when { p.home >= p.away && p.home > p.draw -> m.home.name to p.home; p.away > p.home && p.away > p.draw -> m.away.name to p.away; else -> null }
                out += when {
                    fav == null || abs(p.home - p.away) <= 8 -> "The prediction model sees it as close: ${p.home}% / ${p.draw}% / ${p.away}%."
                    else -> "The prediction model makes ${fav.first} favourites at ${fav.second}%."
                }
            }
        }

        val meetings = h2h.filter { it.hasScore }
        if (meetings.size >= 2) {
            fun wins(id: String) = meetings.count { g -> (g.home.id == id && g.homeScore!! > g.awayScore!!) || (g.away.id == id && g.awayScore!! > g.homeScore!!) }
            val hw = wins(m.home.id); val aw = wins(m.away.id); val d = meetings.size - hw - aw
            out += when {
                hw > aw -> "${m.home.name} won $hw of the last ${meetings.size} meetings."
                aw > hw -> "${m.away.name} won $aw of the last ${meetings.size} meetings."
                else -> "The last ${meetings.size} meetings are split evenly ($hw–$d–$aw)."
            }
        }
        return if (out.size >= 2) out.take(4).joinToString(" ") else ""
    }
}
