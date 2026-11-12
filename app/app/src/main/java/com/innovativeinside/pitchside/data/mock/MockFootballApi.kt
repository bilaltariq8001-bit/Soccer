package com.innovativeinside.pitchside.data.mock

import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.remote.FootballApi
import com.innovativeinside.pitchside.data.remote.Page
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import com.innovativeinside.pitchside.data.remote.dto.AskResponse
import com.innovativeinside.pitchside.data.remote.dto.CareerStopDto
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LeagueDto
import com.innovativeinside.pitchside.data.remote.dto.LineupDto
import com.innovativeinside.pitchside.data.remote.dto.LineupPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerMatchDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SearchDto
import com.innovativeinside.pitchside.data.remote.dto.SquadPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Switches for QA: make the mock fail or go "offline" so every screen's error and retry path can be
 * exercised from Settings > Developer without a backend.
 */
class MockBehavior {
    @Volatile var latencyMs: Long = 350
    @Volatile var offline: Boolean = false
    @Volatile var serverErrors: Boolean = false
}

/**
 * Mock implementation of the football API. It speaks DTOs, exactly like the real gateway, so the mappers
 * and repository run the same code path. All clubs, players and leagues are fictional (no logo or likeness
 * rights issues). Fixtures are generated relative to [clock], so "today" always has live matches.
 */
class MockFootballApi(private val clock: AppClock, val behavior: MockBehavior = MockBehavior()) : FootballApi {

    private suspend fun net() {
        if (behavior.latencyMs > 0) delay(behavior.latencyMs)
        if (behavior.offline) throw AppError.Network()
        if (behavior.serverErrors) throw AppError.Server(503)
    }

    // ---------------- Reference data ----------------

    private fun t(id: String, name: String, short: String, color: String, league: String) = TeamRefDto(id, name, short, color, null, league)

    private val teams = listOf(
        t("ng", "Northgate", "NG", "#1F6F6B", "pd"), t("ru", "Riverside", "RU", "#7A3FBF", "pd"),
        t("hc", "Harbor City", "HC", "#1F5FBF", "pd"), t("rr", "Redmont", "RR", "#B0262F", "pd"),
        t("at", "Ashford", "AT", "#A35508", "pd"), t("wv", "Westvale", "WV", "#2E7D32", "pd"),
        t("kb", "Kingsbridge", "KB", "#34495E", "pd"), t("os", "Olympia SC", "OS", "#8E2C6D", "pd"),
        t("ep", "Eastport", "EP", "#00796B", "al"), t("nt", "Norfolk Town", "NT", "#8A6D00", "al"),
        t("ls", "Lakeside", "LS", "#455A64", "al"), t("hcr", "Hillcrest", "HR", "#6A1B9A", "al"),
        t("nb", "Norbury", "NB", "#3E5161", "st"), t("sb", "Stonebridge", "SB", "#1565C0", "st"),
        t("mb", "Millbrook", "MB", "#AD1457", "st"), t("fh", "Fairhaven", "FH", "#00838F", "st"),
    )
    private val byId = teams.associateBy { it.id }
    private fun ref(id: String) = byId[id] ?: throw AppError.NotFound("This team")

    private val leagues = listOf(
        LeagueDto("pd", "Premier Division", "PD", "Northland", 8, "2026/27", "Round 9"),
        LeagueDto("cc", "Continental Cup", "CC", "International", 16, "2026/27", "Round of 16"),
        LeagueDto("al", "Atlantic League", "AL", "Coastland", 4, "2026/27", "Round 4"),
        LeagueDto("st", "Second Tier", "ST", "Northland", 4, "2026/27", "Round 11"),
    )
    private fun leagueName(id: String) = leagues.firstOrNull { it.id == id }?.name ?: "League"

    private val firstNames = listOf("Leo", "Sam", "Theo", "Ivo", "Kai", "Noah", "Eli", "Jonas", "Milo", "Ari", "Owen", "Rafa", "Luca", "Felix", "Tomas", "Hugo", "Dario", "Emil", "Nico", "Pavel")
    private val lastNames = listOf("Marsh", "Dunn", "Ellis", "Brandt", "Pryor", "Hale", "Voss", "Stone", "Reed", "Cole", "Pike", "Quill", "Dunlop", "Keane", "Lind", "Moss", "Okoro", "Ayele", "Faro", "Tate", "Wren", "Sato", "Frost", "Abara", "Novak", "Lund", "Boyd", "Reyes", "Soto", "Wade")
    private val nations = listOf("Northland", "Coastland", "Eastmark", "Westria", "Southfield")

    /** 16-man squad per team: 2 GK, 5 DEF, 5 MID, 4 FWD. Northgate's #9 is the featured striker Leo Marsh. */
    private fun squad(teamId: String): List<SquadPlayerDto> {
        val r = Random(teamId.hashCode())
        val numbers = listOf(1, 13, 2, 3, 4, 5, 15, 6, 8, 10, 14, 16, 7, 9, 11, 17)
        val pos = listOf("G", "G", "D", "D", "D", "D", "D", "M", "M", "M", "M", "M", "F", "F", "F", "F")
        val used = mutableSetOf<String>()
        return numbers.mapIndexed { i, n ->
            var name: String
            do { name = "${firstNames[r.nextInt(firstNames.size)]} ${lastNames[r.nextInt(lastNames.size)]}" } while (!used.add(name))
            if (teamId == "ng" && n == 9) name = "Leo Marsh"
            if (teamId == "ng" && n == 10) name = "Theo Ellis"
            SquadPlayerDto("$teamId-$n", name, n, pos[i], (60 + r.nextInt(25)) / 10.0)
        }
    }

    private fun summary(teamId: String, p: SquadPlayerDto): PlayerSummaryDto {
        val r = Random(p.playerId.hashCode())
        val position = when (p.position) { "G" -> "Goalkeeper"; "D" -> "Defender"; "M" -> "Midfielder"; else -> "Striker" }
        return PlayerSummaryDto(p.playerId, p.name, ref(teamId), position, p.number, nations[r.nextInt(nations.size)], p.rating ?: 6.5)
    }

    private fun allPlayers(): List<PlayerSummaryDto> = teams.flatMap { tm -> squad(tm.id).map { summary(tm.id, it) } }

    // ---------------- Fixtures ----------------

    private fun at(date: LocalDate, h: Int, m: Int): Long = date.atTime(LocalTime.of(h, m)).atZone(clock.zone()).toInstant().toEpochMilli()

    private fun fx(
        id: String, league: String, round: String, date: LocalDate, h: Int, m: Int, home: String, away: String,
        status: String, hs: Int?, `as`: Int?, label: String = "", minute: Int? = null, brief: String = "",
    ) = FixtureDto(
        id = id, leagueId = league, leagueName = leagueName(league), round = round, kickoffMs = at(date, h, m), status = status,
        statusShort = if (label == "HT") "HT" else "", minute = minute, label = label, venue = "${ref(home).name} Park",
        referee = listOf("A. Calder", "M. Brook", "J. Ferro", "P. Lyle")[(id.hashCode() and 3)], home = ref(home), away = ref(away),
        homeScore = hs, awayScore = `as`, brief = brief,
    )

    private fun todayFixtures(d: LocalDate) = listOf(
        fx("m1", "pd", "Round 9", d, 18, 0, "ng", "ru", "live", 2, 1, minute = 74,
            brief = "Northgate lead 2-1, but Riverside have had 7 shots since half time. One more goal keeps them level on points with the leaders."),
        fx("m2", "pd", "Round 9", d, 18, 30, "at", "wv", "live", 0, 0, minute = 52,
            brief = "A cagey game so far. Westvale have had most of the ball but neither side has found a clear chance."),
        fx("m3", "cc", "Round of 16", d, 19, 0, "kb", "os", "live", 1, 1, label = "HT",
            brief = "All square at the break. Olympia equalised just before half time."),
        fx("m4", "pd", "Round 9", d, 20, 45, "hc", "rr", "upcoming", null, null,
            brief = "A win lifts Harbor City to second. Redmont are unbeaten in 5."),
        fx("m5", "al", "Round 4", d, 21, 0, "ep", "nt", "upcoming", null, null, brief = "Two sides separated by one point meet tonight."),
        fx("m6", "al", "Round 4", d, 16, 0, "ls", "hcr", "postponed", null, null, label = "Postponed",
            brief = "The match was postponed because of a waterlogged pitch. A new date will be announced."),
        fx("m7", "st", "Round 11", d, 14, 0, "nb", "sb", "finished", 1, 3,
            brief = "Stonebridge cruise past Norbury on the road, with three first-half goals deciding it."),
    )

    private val pairings = listOf("pd" to listOf("ng", "ru", "hc", "rr", "at", "wv", "kb", "os"), "al" to listOf("ep", "nt", "ls", "hcr"), "st" to listOf("nb", "sb", "mb", "fh"))

    /** Days within a week of today get a deterministic set of matches; further out is empty (to show the empty state). */
    private fun generated(d: LocalDate, today: LocalDate): List<FixtureDto> {
        val offset = d.toEpochDay() - today.toEpochDay()
        if (offset == 0L || offset !in -7..7) return emptyList()
        val r = Random(d.toEpochDay())
        val past = offset < 0
        return pairings.flatMap { (lg, ids) ->
            val shuffled = ids.shuffled(r)
            shuffled.chunked(2).take(if (lg == "pd") 2 else 1).mapIndexed { i, (h, a) ->
                val hs = if (past) r.nextInt(4) else null
                val aw = if (past) r.nextInt(3) else null
                val hour = listOf(13, 15, 17, 20)[(i + lg.length) % 4]
                fx("g${d.toEpochDay()}-$lg-$i", lg, "Round ${9 + offset.toInt()}", d, hour, if (i == 0) 0 else 30, h, a,
                    if (past) "finished" else "upcoming", hs, aw,
                    brief = if (past) "${ref(h).name} ${hs}-${aw} ${ref(a).name}. Full-time report and stats are in." else "${ref(h).name} host ${ref(a).name}.")
            }
        }
    }

    private fun fixturesFor(d: LocalDate): List<FixtureDto> {
        val today = clock.today()
        return if (d == today) todayFixtures(d) else generated(d, today)
    }

    private fun allRecentFixtures(): List<FixtureDto> {
        val today = clock.today()
        return (-7L..7L).flatMap { fixturesFor(today.plusDays(it)) }
    }

    private fun findFixture(id: String): FixtureDto = allRecentFixtures().firstOrNull { it.id == id } ?: throw AppError.NotFound("This match")

    override suspend fun fixtures(date: String): List<FixtureDto> {
        net()
        val d = runCatching { LocalDate.parse(date) }.getOrElse { throw AppError.Validation("Invalid date") }
        return fixturesFor(d).sortedBy { it.kickoffMs }
    }

    override suspend fun liveFixtures(): List<FixtureDto> { net(); return todayFixtures(clock.today()).filter { it.status == "live" } }
    override suspend fun fixture(id: String): FixtureDto { net(); return findFixture(id) }

    override suspend fun events(fixtureId: String): List<EventDto> {
        net()
        val f = findFixture(fixtureId)
        if (f.status == "upcoming" || f.status == "postponed") return emptyList()
        if (f.id == "m1") return listOf(
            EventDto("e1", "goal", 23, null, "ng", "Leo Marsh", "Theo Ellis"),
            EventDto("e2", "goal", 48, null, "ru", "Sam Dunn", null, "Header"),
            EventDto("e3", "yellowCard", 55, null, "ru", "Ivo Okoro", null, "Foul"),
            EventDto("e4", "goal", 61, null, "ng", "Theo Ellis", "Leo Marsh"),
            EventDto("e5", "sub", 70, null, "ng", "Hugo Frost", null, "On for Kai Quill"),
        )
        val r = Random(f.id.hashCode())
        val minuteCap = f.minute ?: if (f.statusShort == "HT") 45 else 90
        fun goals(teamId: String, n: Int) = (0 until n).map {
            val scorer = squad(teamId).filter { p -> p.position == "F" || p.position == "M" }.random(r)
            EventDto("${f.id}-$teamId-g$it", "goal", 1 + r.nextInt(minuteCap), null, teamId, scorer.name)
        }
        val cards = (0 until r.nextInt(3)).map {
            val tm = if (r.nextBoolean()) f.home.id else f.away.id
            EventDto("${f.id}-c$it", "yellowCard", 1 + r.nextInt(minuteCap), null, tm, squad(tm).random(r).name, null, "Foul")
        }
        return goals(f.home.id, f.homeScore ?: 0) + goals(f.away.id, f.awayScore ?: 0) + cards
    }

    override suspend fun stats(fixtureId: String): List<StatLineDto> {
        net()
        val f = findFixture(fixtureId)
        if (f.status == "upcoming" || f.status == "postponed") return emptyList()
        val r = Random(f.id.hashCode() * 31)
        val poss = 38 + r.nextInt(25)
        val hs = 6 + r.nextInt(10); val aws = 5 + r.nextInt(10)
        return listOf(
            StatLineDto("Possession", poss, 100 - poss, "$poss%", "${100 - poss}%"),
            StatLineDto("Shots", hs, aws),
            StatLineDto("On target", hs / 2, aws / 2),
            StatLineDto("Expected goals (xG)", 10 + r.nextInt(18), 8 + r.nextInt(16)).let { it.copy(homeText = "%.1f".format(it.home / 10.0), awayText = "%.1f".format(it.away / 10.0)) },
            StatLineDto("Passes", 280 + r.nextInt(200), 260 + r.nextInt(200)),
            StatLineDto("Corners", r.nextInt(9), r.nextInt(9)),
            StatLineDto("Fouls", 6 + r.nextInt(8), 6 + r.nextInt(8)),
        )
    }

    override suspend fun lineups(fixtureId: String): LineupsDto {
        net()
        val f = findFixture(fixtureId)
        // Lineups are published about an hour before kickoff; far-off and postponed matches have none.
        val soon = f.kickoffMs - clock.nowMs() < 60 * 60 * 1000
        if (f.status == "postponed" || (f.status == "upcoming" && !soon)) return LineupsDto(emptyList())
        fun lineup(teamId: String): LineupDto {
            val s = squad(teamId)
            val gk = s.first { it.position == "G" }
            val d = s.filter { it.position == "D" }.take(4)
            val m = s.filter { it.position == "M" }.take(3)
            val fw = s.filter { it.position == "F" }.take(3)
            fun row(list: List<SquadPlayerDto>, r: Int) = list.mapIndexed { i, p -> LineupPlayerDto(p.playerId, p.name, p.number, p.position, "$r:${i + 1}") }
            val xi = row(listOf(gk), 1) + row(d, 2) + row(m, 3) + row(fw, 4)
            val bench = (s - setOf(gk) - d.toSet() - m.toSet() - fw.toSet()).map { LineupPlayerDto(it.playerId, it.name, it.number, it.position) }
            val coach = "${firstNames[(teamId.hashCode() and 0x7fffffff) % firstNames.size].first()}. ${lastNames[(teamId.length * 7) % lastNames.size]}"
            return LineupDto(teamId, coach, "4-3-3", xi, bench)
        }
        return LineupsDto(listOf(lineup(f.home.id), lineup(f.away.id)))
    }

    override suspend fun leagues(): List<LeagueDto> { net(); return leagues }
    override suspend fun league(id: String): LeagueDto { net(); return leagues.firstOrNull { it.id == id } ?: throw AppError.NotFound("This league") }

    override suspend fun leagueFixtures(leagueId: String): List<FixtureDto> {
        net()
        if (leagues.none { it.id == leagueId }) throw AppError.NotFound("This league")
        return allRecentFixtures().filter { it.leagueId == leagueId }.sortedBy { it.kickoffMs }
    }

    override suspend fun standings(leagueId: String): List<StandingRowDto> {
        net()
        if (leagueId == "cc") return emptyList() // Knockout cup: no table. The UI explains this.
        val ids = pairings.firstOrNull { it.first == leagueId }?.second ?: throw AppError.NotFound("This league")
        if (leagueId == "pd") {
            val rows = listOf(
                Triple("ng", listOf(9, 7, 1, 1, 21, 7), "WWDWL"), Triple("ru", listOf(9, 6, 2, 1, 18, 9), "LDWLL"),
                Triple("hc", listOf(9, 6, 1, 2, 15, 8), "WWLWW"), Triple("rr", listOf(9, 5, 3, 1, 13, 7), "DWDWW"),
                Triple("at", listOf(9, 5, 1, 3, 12, 9), "LWWLD"), Triple("wv", listOf(9, 4, 2, 3, 10, 9), "DLWDL"),
                Triple("kb", listOf(9, 3, 3, 3, 9, 9), "DDLWD"), Triple("os", listOf(9, 2, 2, 5, 7, 13), "LLDLW"),
            )
            return rows.mapIndexed { i, (id, n, form) ->
                StandingRowDto(i + 1, ref(id), n[0], n[1], n[2], n[3], n[4], n[5], n[4] - n[5], n[1] * 3 + n[2], form)
            }
        }
        val r = Random(leagueId.hashCode())
        return ids.map { id ->
            val w = r.nextInt(7); val d = r.nextInt(4); val l = 10 - w - d
            val gf = w * 2 + d + r.nextInt(4); val ga = l * 2 + d
            StandingRowDto(0, ref(id), 10, w, d, l, gf, ga, gf - ga, w * 3 + d, (1..5).map { "WDL"[r.nextInt(3)] }.joinToString(""))
        }.sortedWith(compareByDescending<StandingRowDto> { it.points }.thenByDescending { it.goalDiff }).mapIndexed { i, s -> s.copy(rank = i + 1) }
    }

    override suspend fun scorers(leagueId: String): List<ScorerDto> {
        net()
        val ids = when (leagueId) { "cc" -> listOf("kb", "os", "ng", "hc"); else -> pairings.firstOrNull { it.first == leagueId }?.second ?: throw AppError.NotFound("This league") }
        val forwards = ids.flatMap { id -> squad(id).filter { it.position == "F" }.take(2).map { id to it } }
        return forwards.map { (id, p) ->
            val r = Random(p.playerId.hashCode())
            val goals = if (p.playerId == "ng-9") 7 else r.nextInt(7)
            Triple(id, p, goals to r.nextInt(4))
        }.sortedByDescending { it.third.first }.take(10).mapIndexed { i, (id, p, ga) -> ScorerDto(i + 1, p.playerId, p.name, ref(id), ga.first, ga.second) }
    }

    override suspend fun popularTeams(): List<TeamRefDto> { net(); return teams }

    override suspend fun team(id: String): TeamDto {
        net()
        val tm = ref(id)
        val r = Random(id.hashCode())
        return TeamDto(tm, leagueName(tm.leagueId.orEmpty()), "${firstNames[r.nextInt(firstNames.size)].first()}. ${lastNames[r.nextInt(lastNames.size)]}",
            "${tm.name} Park", tm.name.substringBefore(' ') + " City", 18_000 + r.nextInt(42_000), 1880 + r.nextInt(70), squad(id))
    }

    override suspend fun teamFixtures(teamId: String): List<FixtureDto> {
        net(); ref(teamId)
        return allRecentFixtures().filter { it.home.id == teamId || it.away.id == teamId }.sortedBy { it.kickoffMs }
    }

    override suspend fun player(id: String): PlayerDto {
        net()
        val teamId = id.substringBefore('-')
        val sq = runCatching { squad(teamId) }.getOrNull() ?: throw AppError.NotFound("This player")
        val p = sq.firstOrNull { it.playerId == id } ?: throw AppError.NotFound("This player")
        val s = summary(teamId, p)
        val r = Random(id.hashCode())
        val apps = 7 + r.nextInt(3)
        val isFwd = p.position == "F"
        val goals = if (id == "ng-9") 7 else if (isFwd) r.nextInt(6) else r.nextInt(2)
        val today = clock.today()
        val recent = (1..5).map { k ->
            val opp = teams.filter { it.leagueId == byId[teamId]?.leagueId && it.id != teamId }.random(r)
            PlayerMatchDto("past-$id-$k", opp, r.nextBoolean(), "${r.nextInt(4)}-${r.nextInt(3)}", 60 + r.nextInt(31), (60 + r.nextInt(30)) / 10.0, at(today.minusDays(k * 7L), 15, 0))
        }
        val clubs = teams.filter { it.id != teamId }.shuffled(r).take(2)
        return PlayerDto(
            summary = s, age = 19 + r.nextInt(14), heightCm = 170 + r.nextInt(25), foot = if (r.nextInt(4) == 0) "Left" else "Right",
            marketValue = "€${5 + r.nextInt(60)}.${r.nextInt(10)}m", goals = goals, assists = r.nextInt(5), matches = apps, minutes = apps * (65 + r.nextInt(25)),
            shotsPer90 = if (isFwd) 2.0 + r.nextInt(20) / 10.0 else r.nextInt(12) / 10.0, keyPassesPer90 = r.nextInt(25) / 10.0,
            xg = if (isFwd) goals * 0.8 + r.nextInt(10) / 10.0 else r.nextInt(8) / 10.0,
            scoutingNote = if (id == "ng-9") "Marsh has scored in 4 of his last 5 starts and takes the most shots per 90 in the squad."
            else "${p.name.substringBefore(' ')} has started $apps of ${ref(teamId).name}'s matches this season.",
            recentMatches = recent,
            career = listOf(
                CareerStopDto(ref(teamId).name, "2023 - now", 60 + r.nextInt(40), goals + r.nextInt(20), current = true),
                CareerStopDto(clubs[0].name, "2020 - 2023", 40 + r.nextInt(50), r.nextInt(20)),
                CareerStopDto(clubs[1].name, "2018 - 2020", 20 + r.nextInt(30), r.nextInt(8)),
            ),
        )
    }

    override suspend fun topPlayers(): List<PlayerSummaryDto> { net(); return allPlayers().sortedByDescending { it.rating }.take(12) }

    private val articles: List<NewsDto> by lazy {
        val now = clock.nowMs()
        val min = 60_000L
        listOf(
            NewsDto("n1", "reports", "Northgate edge Riverside in a six-goal thriller", "Two late goals settle a game that swung three times in the second half.",
                "Northgate came from behind twice before two goals in the final ten minutes settled a game that never let up. Riverside pressed high from the start and led at the break, but substitutions changed the shape of the match.\n\nThe result keeps Northgate top of the Premier Division.", "Pitchside Desk", now - 12 * min, 4),
            NewsDto("n2", "analysis", "Why Harbor City keep winning the second ball", "A look at pressing triggers and the numbers behind their run.",
                "Harbor City recover the ball in the opponent half more often than any side in the division. Their midfield three sit close together, so every clearance lands near a teammate.\n\nThe numbers back it up: they lead the league in counter-pressing regains per 90.", "Pitchside Desk", now - 60 * min, 6),
            NewsDto("n3", "transfers", "Redmont open talks over a winter move for a left back", "Early stage discussions, nothing agreed yet.",
                "Redmont have opened talks about a winter loan for a left back after an injury to their first choice. Discussions are at an early stage and no fee has been agreed.", "Pitchside Desk", now - 120 * min, 2),
            NewsDto("n4", "reports", "Stonebridge cruise past Norbury on the road", "Three first-half goals decide it.",
                "Stonebridge were three up inside 40 minutes and managed the rest of the game comfortably. Norbury pulled one back late on.", "Pitchside Desk", now - 180 * min, 3),
            NewsDto("n5", "analysis", "Five things to watch in tonight's Continental Cup tie", "Lineups, tactics and the key duels.",
                "Kingsbridge and Olympia meet with a quarter-final place at stake. Watch the battle in wide areas, set pieces and how both coaches use their bench.", "Pitchside Desk", now - 300 * min, 5),
            NewsDto("n6", "transfers", "Olympia confirm a new goalkeeper on a two-year deal", "The club announced the signing this morning.",
                "Olympia SC have signed a goalkeeper on a two-year contract. He arrives on a free transfer and goes straight into the squad.", "Pitchside Desk", now - 1_440 * min, 2),
            NewsDto("n7", "reports", "Eastport and Norfolk Town set for a tight Atlantic League meeting", "One point separates the sides.",
                "Eastport host Norfolk Town tonight with a single point between them in the Atlantic League table.", "Pitchside Desk", now - 1_600 * min, 2),
            NewsDto("n8", "analysis", "The numbers behind Leo Marsh's scoring streak", "Shot volume, not luck.",
                "Leo Marsh has scored in four of his last five starts. He takes more shots per 90 than anyone in his squad, and most come from inside the box.", "Pitchside Desk", now - 2_000 * min, 4),
        )
    }

    override suspend fun news(category: String?, cursor: String?): Page<NewsDto> {
        net()
        val all = articles.filter { category == null || it.category == category }
        val start = cursor?.toIntOrNull() ?: 0
        val page = all.drop(start).take(PAGE)
        val next = if (start + PAGE < all.size) (start + PAGE).toString() else null
        return Page(page, next)
    }

    override suspend fun article(id: String): NewsDto { net(); return articles.firstOrNull { it.id == id } ?: throw AppError.NotFound("This story") }

    override suspend fun search(query: String): SearchDto {
        net()
        val q = query.trim()
        if (q.length < 2) return SearchDto()
        return SearchDto(
            teams = teams.filter { it.name.contains(q, true) },
            leagues = leagues.filter { it.name.contains(q, true) || it.country.contains(q, true) },
            players = allPlayers().filter { it.name.contains(q, true) }.take(20),
        )
    }

    /** Canned, data-grounded answers until Firebase AI Logic is wired in (see FINAL_REPORT.md). */
    override suspend fun ask(request: AskRequest): AskResponse {
        net()
        val q = request.question.lowercase()
        val table = standings("pd")
        fun row(id: String) = table.first { it.team.id == id }
        val answer = when {
            "second" in q || "harbor" in q -> "Harbor City are on ${row("hc").points} points, ${row("ru").points - row("hc").points} behind Riverside in second. A win against Redmont tonight would lift them to ${row("hc").points + 3}."
            "score" in q || "northgate" in q || "marsh" in q -> "Leo Marsh leads Northgate with 7 league goals this season, ahead of Theo Ellis."
            "redmont" in q || "form" in q -> "Redmont are unbeaten in 5 (${row("rr").form}) and sit ${row("rr").rank}th on ${row("rr").points} points."
            "table" in q || "top" in q || "lead" in q -> "${table[0].team.name} lead the Premier Division on ${table[0].points} points, ${table[0].points - table[1].points} clear of ${table[1].team.name}."
            else -> "I can answer questions about fixtures, standings, form and players using match data. Try \"Who leads the table?\" or \"Redmont form\"."
        }
        return AskResponse(answer)
    }

    private companion object { const val PAGE = 4 }
}
