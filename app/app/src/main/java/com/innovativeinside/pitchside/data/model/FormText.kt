package com.innovativeinside.pitchside.data.model

/** Small text helpers for tables and form guides. Pure and unit tested. */
object FormText {

    fun ordinal(n: Int): String = n.toString() + when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
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
}
