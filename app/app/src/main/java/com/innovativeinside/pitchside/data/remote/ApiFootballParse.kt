package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LineupDto
import com.innovativeinside.pitchside.data.remote.dto.LineupPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.CommentDto
import com.innovativeinside.pitchside.data.remote.dto.LiveDataDto
import com.innovativeinside.pitchside.data.remote.dto.OddsDto
import com.innovativeinside.pitchside.data.remote.dto.VideoDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerMatchDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.PredictionDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SquadPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import com.innovativeinside.pitchside.data.remote.dto.CareerStopDto
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Pure parsing of apifootball.com v3 JSON (https://apifootball.com/documentation/) into the app's DTOs.
 * The API sends almost every value as a string ("" for unknown), so everything here is lenient.
 * Times are requested with timezone=UTC, so dates and kickoff times are read as UTC.
 */
object ApiFootballParse {

    // ---- JSON helpers ----

    fun JsonObject.str(key: String): String = when (val v = this[key]) {
        null, is JsonNull -> ""
        is JsonPrimitive -> v.contentOrNull.orEmpty().trim()
        else -> ""
    }

    fun JsonObject.int(key: String): Int? = str(key).toIntOrNull()
    fun JsonObject.intOr0(key: String): Int = int(key) ?: 0
    fun JsonObject.dbl(key: String): Double? = str(key).toDoubleOrNull()
    fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    fun JsonObject.arr(key: String): List<JsonObject> = (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    fun JsonElement.objects(): List<JsonObject> = (this as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    // ---- status ----

    /** Normalized status: one of upcoming | live | finished | postponed, plus the short code and a badge label. */
    data class StatusInfo(val status: String, val short: String = "", val minute: Int? = null, val label: String = "")

    private val minuteRe = Regex("^(\\d{1,3})(?:\\s*\\+\\s*(\\d{0,2}))?'?$")

    fun status(rawStatus: String, live: Boolean = false): StatusInfo {
        val s = rawStatus.trim()
        val l = s.lowercase()
        return when {
            l == "finished" || l == "ft" -> StatusInfo("finished", "FT", label = "FT")
            l == "after et" || l == "aet" -> StatusInfo("finished", "AET", label = "AET")
            l == "after pen." || l == "after pen" || l == "pen." -> StatusInfo("finished", "PEN", label = "Pens")
            l == "awarded" -> StatusInfo("finished", "AWD", label = "FT")
            l == "postponed" -> StatusInfo("postponed", "PST", label = "Postponed")
            l == "cancelled" || l == "canceled" -> StatusInfo("postponed", "CANC", label = "Cancelled")
            l == "abandoned" || l == "interrupted" || l == "suspended" -> StatusInfo("postponed", "SUSP", label = s.replaceFirstChar { it.uppercase() })
            l == "half time" || l == "ht" -> StatusInfo("live", "HT", 45, "HT")
            l == "break time" || l == "extra time" -> StatusInfo("live", "ET", 90, "ET")
            l == "penalties" || l == "pen" -> StatusInfo("live", "P", 120, "Pens")
            minuteRe.matches(s) -> {
                val m = minuteRe.find(s)!!
                val minute = m.groupValues[1].toInt()
                val extra = m.groupValues[2]
                StatusInfo("live", "LIVE", minute, if (m.value.contains('+')) "$minute+$extra'" else "$minute'")
            }
            l.isEmpty() || l == "not started" || l == "ns" || l.matches(Regex("\\d{1,2}:\\d{2}")) ->
                if (live) StatusInfo("live", "LIVE") else StatusInfo("upcoming", "NS")
            live -> StatusInfo("live", "LIVE", label = s)
            else -> StatusInfo("upcoming", "NS")
        }
    }

    /** "2026-11-07" + "15:00" in UTC to epoch ms. A missing or odd time ("TBD") counts as midnight. */
    fun kickoffMs(date: String, time: String): Long {
        val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return 0L
        val t = runCatching { LocalTime.parse(time.take(5)) }.getOrNull() ?: LocalTime.MIDNIGHT
        return d.atTime(t).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    // ---- events (fixtures) ----

    fun homeRef(e: JsonObject, leagueId: String = e.str("league_id")) = TeamRefDto(
        id = e.str("match_hometeam_id"), name = e.str("match_hometeam_name"), logo = e.str("team_home_badge").ifBlank { null }, leagueId = leagueId,
    )

    fun awayRef(e: JsonObject, leagueId: String = e.str("league_id")) = TeamRefDto(
        id = e.str("match_awayteam_id"), name = e.str("match_awayteam_name"), logo = e.str("team_away_badge").ifBlank { null }, leagueId = leagueId,
    )

    /**
     * One get_events (or get_H2H / get_predictions) row to a [FixtureDto]. [leagueId] lets the caller swap the API league for a
     * division id; [prediction] attaches a matching get_predictions row.
     */
    fun fixture(e: JsonObject, leagueId: String = e.str("league_id"), prediction: PredictionDto? = null): FixtureDto {
        val st = status(e.str("match_status"), e.str("match_live") == "1")
        val hs = e.int("match_hometeam_score")
        val aw = e.int("match_awayteam_score")
        val round = e.str("match_round").let { r -> if (r.toIntOrNull() != null) "Round $r" else r }
        val pens = if (st.short == "PEN") listOf(e.int("match_hometeam_penalty_score"), e.int("match_awayteam_penalty_score")) else emptyList()
        return FixtureDto(
            id = e.str("match_id"),
            leagueId = leagueId,
            leagueName = e.str("league_name"),
            round = round,
            kickoffMs = kickoffMs(e.str("match_date"), e.str("match_time")),
            status = st.status,
            statusShort = st.short,
            minute = st.minute,
            label = if (st.status == "upcoming") "" else if (pens.size == 2 && pens.all { it != null }) "Pens ${pens[0]}-${pens[1]}" else st.label,
            venue = e.str("match_stadium"),
            referee = e.str("match_referee").ifBlank { null },
            home = homeRef(e, leagueId),
            away = awayRef(e, leagueId),
            homeScore = if (st.status == "upcoming") null else hs,
            awayScore = if (st.status == "upcoming") null else aw,
            prediction = prediction,
            leagueLogo = e.str("league_logo").ifBlank { null },
            country = e.str("country_name"),
        )
    }

    fun prediction(p: JsonObject): PredictionDto? {
        val h = p.dbl("prob_HW") ?: return null
        val d = p.dbl("prob_D") ?: return null
        val a = p.dbl("prob_AW") ?: return null
        if (h + d + a <= 0.0) return null
        return PredictionDto(h, d, a, p.dbl("prob_O"), p.dbl("prob_bts"))
    }

    /** "45+2" to (45, 2); "90+" to (90, null); "67'" to (67, null). */
    fun minute(raw: String): Pair<Int, Int?> {
        val s = raw.trim().removeSuffix("'")
        val main = s.substringBefore('+').trim().toIntOrNull() ?: 0
        val extra = if ('+' in s) s.substringAfter('+').trim().toIntOrNull() else null
        return main to extra
    }

    /** Goals, cards and substitutions of one event, in the app's [EventDto] shape. */
    fun timeline(e: JsonObject): List<EventDto> {
        val homeId = e.str("match_hometeam_id")
        val awayId = e.str("match_awayteam_id")
        val out = mutableListOf<EventDto>()
        e.arr("goalscorer").forEachIndexed { i, g ->
            val home = g.str("home_scorer").isNotBlank()
            val name = if (home) g.str("home_scorer") else g.str("away_scorer")
            if (name.isBlank()) return@forEachIndexed
            val (m, x) = minute(g.str("time"))
            val assist = (if (home) g.str("home_assist") else g.str("away_assist")).ifBlank { null }
            out += EventDto("g$i", "goal", m, x, if (home) homeId else awayId, name, assist, g.str("info"))
        }
        e.arr("cards").forEachIndexed { i, c ->
            val home = c.str("home_fault").isNotBlank()
            val name = if (home) c.str("home_fault") else c.str("away_fault")
            if (name.isBlank()) return@forEachIndexed
            val card = c.str("card").lowercase()
            val kind = when {
                "red" in card -> "redCard" // "red card" and "yellow red card" (second yellow)
                "yellow" in card -> "yellowCard"
                else -> return@forEachIndexed
            }
            val (m, x) = minute(c.str("time"))
            out += EventDto("c$i", kind, m, x, if (home) homeId else awayId, name, null, c.str("info").ifBlank { if ("yellow" in card && "red" in card) "Second yellow" else "" })
        }
        val subs = e.obj("substitutions")
        listOf("home" to homeId, "away" to awayId).forEach { (side, teamId) ->
            subs?.arr(side)?.forEachIndexed { i, s ->
                val parts = s.str("substitution").split('|').map { it.trim() }
                val off = parts.getOrNull(0).orEmpty()
                val on = parts.getOrNull(1).orEmpty()
                if (on.isBlank() && off.isBlank()) return@forEachIndexed
                val (m, x) = minute(s.str("time"))
                out += EventDto("s-$side-$i", "sub", m, x, teamId, on.ifBlank { off }, null, if (off.isNotBlank() && on.isNotBlank()) "On for $off" else "")
            }
        }
        return out
    }

    private val statLabels = linkedMapOf(
        "ball possession" to "Possession",
        "shots total" to "Shots",
        "shots on goal" to "On target",
        "on target" to "On target",
        "shots off goal" to "Off target",
        "off target" to "Off target",
        "corners" to "Corners",
        "attacks" to "Attacks",
        "dangerous attacks" to "Dangerous attacks",
        "fouls" to "Fouls",
        "offsides" to "Offsides",
        "yellow cards" to "Yellow cards",
        "red cards" to "Red cards",
        "saves" to "Saves",
        "passes total" to "Passes",
        "passes accurate" to "Accurate passes",
        "shots blocked" to "Shots blocked",
        "shots inside box" to "Shots inside box",
        "shots outside box" to "Shots outside box",
        "free kick" to "Free kicks",
        "goal kick" to "Goal kicks",
        "throw in" to "Throw-ins",
    )
    private val labelOrder = statLabels.values.distinct()

    /** Full-match statistics, renamed to the app's labels, possession first, duplicates dropped. */
    fun stats(rows: List<JsonObject>): List<StatLineDto> {
        val seen = mutableSetOf<String>()
        val out = rows.mapNotNull { r ->
            val type = r.str("type")
            if (type.isBlank()) return@mapNotNull null
            val label = statLabels[type.lowercase()] ?: type
            if (!seen.add(label)) return@mapNotNull null
            val ht = r.str("home"); val at = r.str("away")
            val h = ht.filter { it.isDigit() }.toIntOrNull() ?: 0
            val a = at.filter { it.isDigit() }.toIntOrNull() ?: 0
            StatLineDto(label, h, a, ht.ifBlank { "0" }, at.ifBlank { "0" })
        }
        // The feed reports untracked numbers as 0-0 (throw-ins, free kicks...): drop those, keep a real 0-0 for cards.
        return out.filter { it.home != 0 || it.away != 0 || it.label == "Yellow cards" || it.label == "Red cards" }
            .sortedBy { labelOrder.indexOf(it.label).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /**
     * One side of a lineup. apifootball sends the formation separately ("4-2-3-1") and numbers starters 1..11 in
     * lineup_position; with a formation that adds up we turn that into the "row:col" grid the pitch view uses.
     */
    fun lineup(side: JsonObject?, teamId: String, system: String, roster: Map<String, RosterEntry> = emptyMap()): LineupDto {
        if (side == null) return LineupDto(teamId)
        val starters = side.arr("starting_lineups").sortedBy { it.int("lineup_position") ?: 99 }
        val rows = system.split('-').mapNotNull { it.trim().toIntOrNull() }
        val positioned = starters.all { (it.int("lineup_position") ?: 0) in 1..11 } && starters.map { it.int("lineup_position") }.distinct().size == starters.size
        var derived = false
        var formation = system.ifBlank { null }
        val grids: List<String?> = if (starters.size == 11 && positioned && rows.isNotEmpty() && rows.sum() == 10) {
            buildList {
                add("1:1")
                rows.forEachIndexed { r, n -> for (c in 1..n) add("${r + 2}:$c") }
            }
        } else {
            // No published formation: place starters by their squad position (goalkeeper, defenders, midfielders,
            // forwards) when the squad list knows nearly all of them. Otherwise there is no honest pitch layout.
            val shape = squadShape(starters.map { roster[it.str("player_key")]?.role })
            if (shape != null) { derived = true; formation = shape.first; shape.second } else List(starters.size) { null }
        }
        fun player(p: JsonObject, grid: String?): LineupPlayerDto {
            val r = roster[p.str("player_key")]
            return LineupPlayerDto(
                id = p.str("player_key"), name = p.str("lineup_player"), number = p.int("lineup_number") ?: r?.number?.takeIf { it > 0 },
                pos = r?.role?.take(1), grid = grid, photo = r?.photo,
            )
        }
        return LineupDto(
            teamId = teamId,
            coach = side.arr("coach").firstOrNull()?.str("lineup_player")?.ifBlank { null },
            formation = formation,
            startXI = starters.mapIndexed { i, p -> player(p, grids.getOrNull(i)) },
            substitutes = side.arr("substitutes").map { player(it, null) },
            derived = derived,
        )
    }

    /**
     * Rows for 11 starters from their squad roles ("Goalkeeper", "Defender", ...; null = unknown). Needs exactly one
     * goalkeeper, at most two unknowns (placed in midfield) and three to five defenders. Returns ("4-4-2", grids).
     */
    fun squadShape(roles: List<String?>): Pair<String, List<String?>>? {
        if (roles.size != 11) return null
        val letters = roles.map { it?.trim()?.uppercase()?.take(1) }
        if (letters.count { it == null } > 2 || letters.count { it == "G" } != 1) return null
        val line = letters.map { when (it) { "G" -> 1; "D" -> 2; "F" -> 4; else -> 3 } }
        val counts = (2..4).map { r -> line.count { it == r } }
        if (counts[0] !in 3..5 || counts.sum() != 10) return null
        val seen = IntArray(5)
        val grids = line.map { r -> seen[r] += 1; "$r:${seen[r]}" }
        // Collapse empty lines so a 4-6-0 reads as "4-6", never "4-6-0".
        val label = counts.filter { it > 0 }.joinToString("-")
        // Renumber rows without gaps (e.g. no forwards) so the pitch spreads the lines evenly.
        val present = (1..4).filter { r -> r == 1 || line.contains(r) }
        val remap = present.withIndex().associate { (i, r) -> r to i + 1 }
        return label to grids.map { g -> val (r, c) = g.split(':'); "${remap[r.toInt()]}:$c" }
    }

    /** One player of a get_teams squad, keyed by player id in [roster]. */
    data class RosterEntry(val role: String, val photo: String?, val number: Int, val country: String, val teamId: String, val age: Int)

    /** Every player of every team in a get_teams answer (league_id or team_id), keyed by player id. */
    fun roster(teams: List<JsonObject>): Map<String, RosterEntry> = buildMap {
        teams.forEach { t ->
            val teamId = t.str("team_key")
            t.arr("players").forEach { p ->
                val id = p.str("player_id").ifBlank { p.str("player_key") }
                if (id.isBlank() || id == "0") return@forEach
                put(id, RosterEntry(position(p.str("player_type")), p.str("player_image").ifBlank { null }, p.intOr0("player_number"), p.str("player_country"), teamId, p.intOr0("player_age")))
            }
        }
    }

    /** A player's season counted from match events: what the API's own player stats leave blank on smaller plans. */
    data class SeasonLine(
        val apps: Int, val starts: Int, val minutes: Int, val goals: Int, val assists: Int, val yellow: Int, val red: Int,
        val recent: List<PlayerMatchDto>,
    )

    fun playerSeason(playerId: String, teamId: String, events: List<JsonObject>, recentCount: Int = 6): SeasonLine {
        var apps = 0; var starts = 0; var minutes = 0; var goals = 0; var assists = 0; var yellow = 0; var red = 0
        val recent = mutableListOf<PlayerMatchDto>()
        val finished = events.filter { status(it.str("match_status")).status == "finished" }
            .sortedBy { kickoffMs(it.str("match_date"), it.str("match_time")) }
        finished.forEach { e ->
            val home = e.str("match_hometeam_id") == teamId
            val side = if (home) "home" else "away"
            val lineup = e.obj("lineup")?.obj(side)
            val started = lineup?.arr("starting_lineups")?.any { it.str("player_key") == playerId } == true
            var on: Int? = null; var off: Int? = null
            e.obj("substitutions")?.arr(side)?.forEach { sub ->
                val ids = sub.str("substitution_player_id").split('|').map { it.trim() }
                val t = minute(sub.str("time")).first.coerceIn(0, 120)
                if (ids.getOrNull(0) == playerId) off = t
                if (ids.getOrNull(1) == playerId) on = t
            }
            val g = e.arr("goalscorer").count { it.str("${side}_scorer_id") == playerId && "own" !in it.str("info").lowercase() }
            val a = e.arr("goalscorer").count { it.str("${side}_assist_id") == playerId }
            val cards = e.arr("cards").filter { it.str("${side}_player_id") == playerId }.map { it.str("card").lowercase() }
            val played = started || on != null || g > 0 || a > 0 || cards.isNotEmpty()
            if (!played) return@forEach
            apps++; if (started) starts++
            val mins = when {
                started -> off ?: 90
                on != null -> (90 - on!!).coerceAtLeast(1)
                else -> 0
            }
            minutes += mins; goals += g; assists += a
            yellow += cards.count { "yellow" in it && "red" !in it }
            red += cards.count { "red" in it }
            val f = fixture(e)
            val mine = if (home) f.homeScore else f.awayScore
            val theirs = if (home) f.awayScore else f.homeScore
            val result = if (mine == null || theirs == null) "" else when { mine > theirs -> "W"; mine < theirs -> "L"; else -> "D" }
            recent += PlayerMatchDto(
                matchId = f.id, opponent = if (home) f.away else f.home, home = home,
                score = if (f.homeScore != null && f.awayScore != null) "${f.homeScore}-${f.awayScore}" else "",
                minutes = mins, rating = 0.0, dateMs = f.kickoffMs, goals = g, started = started, result = result,
            )
        }
        return SeasonLine(apps, starts, minutes, goals, assists, yellow, red, recent.takeLast(recentCount).reversed())
    }

    // ---- standings ----

    fun standingRow(r: JsonObject, leagueId: String, form: String? = null): StandingRowDto {
        val gf = r.intOr0("overall_league_GF")
        val ga = r.intOr0("overall_league_GA")
        return StandingRowDto(
            rank = r.intOr0("overall_league_position"),
            team = TeamRefDto(r.str("team_id"), r.str("team_name"), logo = r.str("team_badge").ifBlank { null }, leagueId = leagueId),
            played = r.intOr0("overall_league_payed"),
            won = r.intOr0("overall_league_W"),
            drawn = r.intOr0("overall_league_D"),
            lost = r.intOr0("overall_league_L"),
            goalsFor = gf,
            goalsAgainst = ga,
            goalDiff = gf - ga,
            points = r.intOr0("overall_league_PTS"),
            form = form,
        )
    }

    /** Last five results (oldest first) of [teamId] from finished fixtures, as "WDLWW". */
    fun form(teamId: String, fixtures: List<FixtureDto>): String =
        fixtures.filter { it.status == "finished" && (it.home.id == teamId || it.away.id == teamId) && it.homeScore != null && it.awayScore != null }
            .sortedBy { it.kickoffMs }
            .takeLast(5)
            .joinToString("") { f ->
                val mine = if (f.home.id == teamId) f.homeScore!! else f.awayScore!!
                val theirs = if (f.home.id == teamId) f.awayScore!! else f.homeScore!!
                when { mine > theirs -> "W"; mine < theirs -> "L"; else -> "D" }
            }

    // ---- scorers ----

    fun topScorer(r: JsonObject, leagueId: String) = ScorerDto(
        rank = r.intOr0("player_place"),
        playerId = r.str("player_key"),
        name = r.str("player_name"),
        team = TeamRefDto(r.str("team_key"), r.str("team_name"), leagueId = leagueId),
        goals = r.intOr0("goals"),
        assists = r.intOr0("assists"),
    )

    /**
     * Top scorers counted from the goal events of finished fixtures (used when get_topscorers is not available on
     * the plan). Own goals are skipped. Ranked by goals, then assists, then name.
     */
    fun scorersFromEvents(events: List<JsonObject>, leagueId: String, limit: Int = 20): List<ScorerDto> {
        data class Acc(val id: String, val name: String, var team: TeamRefDto, var goals: Int = 0, var assists: Int = 0)
        val acc = linkedMapOf<String, Acc>()
        events.forEach { e ->
            val home = homeRef(e, leagueId)
            val away = awayRef(e, leagueId)
            e.arr("goalscorer").forEach { g ->
                if ("own" in g.str("info").lowercase()) return@forEach
                listOf(Triple("home", home, g), Triple("away", away, g)).forEach { (side, team, row) ->
                    val name = row.str("${side}_scorer")
                    if (name.isNotBlank()) {
                        val id = row.str("${side}_scorer_id").ifBlank { "n:$name" }
                        acc.getOrPut(id) { Acc(id, name, team) }.goals++
                    }
                    val assist = row.str("${side}_assist")
                    if (assist.isNotBlank()) {
                        val id = row.str("${side}_assist_id").ifBlank { "n:$assist" }
                        acc.getOrPut(id) { Acc(id, assist, team) }.assists++
                    }
                }
            }
        }
        return acc.values.filter { it.goals > 0 }
            .sortedWith(compareByDescending<Acc> { it.goals }.thenByDescending { it.assists }.thenBy { it.name })
            .take(limit)
            .mapIndexed { i, a -> ScorerDto(i + 1, a.id, a.name, a.team, a.goals, a.assists) }
    }

    // ---- teams and players ----

    /** "Goalkeepers" to "Goalkeeper"; the app's position mapper reads the first letter. */
    fun position(type: String): String = type.trim().removeSuffix("s")

    fun team(t: JsonObject, leagueId: String?, leagueName: String): TeamDto {
        val venue = t.obj("venue")
        val squad = t.arr("players").mapNotNull { p ->
            val id = p.str("player_id").ifBlank { p.str("player_key") }
            val name = p.str("player_name").ifBlank { p.str("player_complete_name") }
            if (id.isBlank() || id == "0" || name.isBlank()) return@mapNotNull null
            SquadPlayerDto(
                id, name, p.intOr0("player_number"), position(p.str("player_type")).ifBlank { "Forward" }, p.dbl("player_rating")?.takeIf { it > 0 },
                photo = p.str("player_image").ifBlank { null }, age = p.intOr0("player_age"),
            )
        }
        return TeamDto(
            team = TeamRefDto(t.str("team_key"), t.str("team_name"), logo = t.str("team_badge").ifBlank { null }, leagueId = leagueId),
            leagueName = leagueName,
            coach = t.arr("coaches").firstOrNull()?.str("coach_name").orEmpty(),
            stadium = venue?.str("venue_name").orEmpty(),
            city = venue?.str("venue_city").orEmpty(),
            capacity = venue?.intOr0("venue_capacity") ?: 0,
            founded = t.intOr0("team_founded"),
            squad = squad,
        )
    }

    fun playerSummary(p: JsonObject, leagueId: String? = null) = PlayerSummaryDto(
        id = p.str("player_id").ifBlank { p.str("player_key") },
        name = p.str("player_name").ifBlank { p.str("player_complete_name") },
        team = TeamRefDto(p.str("team_key"), p.str("team_name"), leagueId = leagueId),
        position = position(p.str("player_type")),
        number = p.intOr0("player_number"),
        nationality = p.str("player_country"),
        rating = p.dbl("player_rating") ?: 0.0,
        photo = p.str("player_image").ifBlank { null },
    )

    /** get_players can return one row per club the player is registered with; the first is the current one. */
    fun player(rows: List<JsonObject>, leagueId: String? = null): PlayerDto? {
        val p = rows.firstOrNull() ?: return null
        val minutes = p.intOr0("player_minutes")
        fun per90(v: Int?): Double = if (v == null || minutes <= 0) 0.0 else Math.round(v * 90.0 / minutes * 10) / 10.0
        val notes = buildList {
            p.str("player_complete_name").takeIf { it.isNotBlank() && it != p.str("player_name") }?.let { add(it) }
            p.str("player_birthdate").takeIf { it.isNotBlank() }?.let { add("Born $it") }
            if (p.str("player_injured").equals("Yes", true)) add("Currently injured")
        }
        return PlayerDto(
            summary = playerSummary(p, leagueId),
            age = p.intOr0("player_age"),
            goals = p.intOr0("player_goals"),
            assists = p.intOr0("player_assists"),
            matches = p.intOr0("player_match_played"),
            minutes = minutes,
            shotsPer90 = per90(p.int("player_shots_total")),
            keyPassesPer90 = per90(p.int("player_key_passes")),
            scoutingNote = notes.joinToString(" · "),
            career = rows.distinctBy { it.str("team_key") }.mapIndexed { i, r ->
                CareerStopDto(r.str("team_name"), "", r.intOr0("player_match_played"), r.intOr0("player_goals"), current = i == 0)
            }.filter { it.club.isNotBlank() },
        )
    }

    // ---- odds, live commentary, videos ----

    /**
     * Pre-match 1X2 from get_odds rows (one per bookmaker). Prefers bet365, then the first bookmaker that has all three
     * prices, so the number matches a single real book rather than an invented average.
     */
    fun odds(rows: List<JsonObject>, preferred: List<String> = listOf("bet365", "WilliamHill", "Pncl")): OddsDto? {
        val usable = rows.filter { listOf("odd_1", "odd_x", "odd_2").all { k -> (it.dbl(k) ?: 0.0) > 1.0 } }
        val pick = preferred.firstNotNullOfOrNull { b -> usable.firstOrNull { it.str("odd_bookmakers").equals(b, true) } } ?: usable.firstOrNull() ?: return null
        return OddsDto(pick.str("odd_bookmakers"), pick.dbl("odd_1")!!, pick.dbl("odd_x")!!, pick.dbl("odd_2")!!, pick.str("odd_date").ifBlank { null })
    }

    private val resultMarkets = listOf("fulltime result", "full time result", "match winner", "1x2", "3way result", "match result")

    /** 1X2 from the live_odds list of get_live_odds_commnets; null when no full-time result market is open. */
    fun liveOdds(rows: List<JsonObject>): OddsDto? {
        val market = rows.filter { r -> resultMarkets.any { it in r.str("odd_name").lowercase() } && !r.str("suspended").equals("Yes", true) }
        fun price(vararg types: String) = market.firstOrNull { r -> types.any { r.str("type").equals(it, true) } }?.dbl("value")
        val h = price("1", "Home") ?: return null
        val d = price("X", "Draw") ?: return null
        val a = price("2", "Away") ?: return null
        return OddsDto("Live", h, d, a, market.maxOfOrNull { it.str("upd") }?.ifBlank { null }, live = true)
    }

    /** get_live_odds_commnets answers an object keyed by match id; pick the requested match (or the only one). */
    fun liveData(root: JsonElement, matchId: String): LiveDataDto? {
        val obj = root as? JsonObject ?: return null
        val m = (obj[matchId] as? JsonObject) ?: obj.values.mapNotNull { it as? JsonObject }.firstOrNull { it.str("match_id") == matchId } ?: return null
        val comments = m.arr("live_comments").mapNotNull { c ->
            val text = c.str("text")
            if (text.isBlank()) null else CommentDto(c.str("time"), text, c.str("state"))
        }
        return LiveDataDto(comments, liveOdds(m.arr("live_odds")))
    }

    fun videos(rows: List<JsonObject>): List<VideoDto> = rows.mapNotNull { v ->
        val url = v.str("video_url")
        if (!url.startsWith("http")) null else VideoDto(v.str("video_title").ifBlank { v.str("video_title_full") }.ifBlank { "Highlights" }, url)
    }
}
