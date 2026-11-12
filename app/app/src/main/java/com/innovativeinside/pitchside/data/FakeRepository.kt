package com.innovativeinside.pitchside.data

/** In-memory sample data. Club and player names are fictional so there are no logo or likeness issues. */
object FakeRepository : Repository {

    val ng = Team("ng", "Northgate", "NG", 0xFF1F6F6B)
    val ru = Team("ru", "Riverside", "RU", 0xFF7A3FBF)
    val hc = Team("hc", "Harbor City", "HC", 0xFF1F5FBF)
    val rr = Team("rr", "Redmont", "RR", 0xFFB0262F)
    val at = Team("at", "Ashford", "AT", 0xFFA35508)
    val wv = Team("wv", "Westvale", "WV", 0xFF2E7D32)
    val kb = Team("kb", "Kingsbridge", "KB", 0xFF34495E)
    val os = Team("os", "Olympia SC", "OS", 0xFF8E2C6D)
    val ep = Team("ep", "Eastport", "EP", 0xFF00796B)
    val nt = Team("nt", "Norfolk Town", "NT", 0xFF8A6D00)
    val ls = Team("ls", "Lakeside", "LS", 0xFF455A64)
    val hcr = Team("hcr", "Hillcrest", "HR", 0xFF6A1B9A)
    val nb = Team("nb", "Norbury", "NB", 0xFF34495E)
    val sb = Team("sb", "Stonebridge", "SB", 0xFF1F5FBF)

    private val matches = listOf(
        Match("m1", "Premier Division", "Round 9", "Northgate Park", ng, ru, 2, 1, Status.Live, "74'",
            "Northgate lead 2–1, but Riverside have had 7 shots since half time. One more goal keeps them level on points with the leaders."),
        Match("m2", "Premier Division", "Round 9", "Ashford Ground", at, wv, 0, 0, Status.Live, "52'",
            "A cagey game so far. Westvale have had most of the ball but neither side has found a clear chance."),
        Match("m3", "Continental Cup", "Round of 16", "Kingsbridge Arena", kb, os, 1, 1, Status.Live, "HT",
            "All square at the break. Olympia equalised just before half time."),
        Match("m4", "Premier Division", "Round 9", "Harbor Stadium", hc, rr, null, null, Status.Upcoming, "20:45",
            "A win lifts Harbor City to second. Redmont are unbeaten in 5."),
        Match("m5", "Atlantic League", "Round 4", "Eastport Park", ep, nt, null, null, Status.Upcoming, "21:00",
            "Two sides separated by one point meet tonight."),
        Match("m6", "Atlantic League", "Round 4", "Lakeside Field", ls, hcr, null, null, Status.Postponed, "Postponed",
            "The match was postponed. A new date will be announced."),
        Match("m7", "Second Tier", "Round 11", "Norbury Road", nb, sb, 1, 3, Status.Finished, "FT",
            "Stonebridge cruise past Norbury on the road, with three first-half goals deciding it."),
    )

    override fun matches() = matches
    override fun match(id: String) = matches.firstOrNull { it.id == id }

    override fun events(matchId: String) = listOf(
        MatchEvent("70'", true, "Substitution", "Frost on, Quill off", EventKind.Sub),
        MatchEvent("61'", true, "Goal · Ellis", "Assist Marsh", EventKind.Goal),
        MatchEvent("55'", false, "Yellow card · Okoro", "Foul", EventKind.Card),
        MatchEvent("48'", false, "Goal · Dunn", "Riverside 1 – 1", EventKind.Goal),
        MatchEvent("23'", true, "Goal · Marsh", "Northgate 1 – 0", EventKind.Goal),
    )

    override fun stats(matchId: String) = listOf(
        StatRow("Possession", 54, 46, "54%", "46%"),
        StatRow("Shots", 11, 14),
        StatRow("On target", 5, 6),
        StatRow("Passes", 312, 289),
        StatRow("Fouls", 9, 12),
        StatRow("Corners", 4, 7),
    )

    override fun lineup(matchId: String, home: Boolean): Lineup {
        val h = listOf(
            LineupPlayer(1, "Hale", 0.5f, 0.90f), LineupPlayer(3, "Voss", 0.14f, 0.73f), LineupPlayer(4, "Stone", 0.38f, 0.76f),
            LineupPlayer(5, "Reed", 0.62f, 0.76f), LineupPlayer(2, "Cole", 0.86f, 0.73f), LineupPlayer(6, "Pike", 0.24f, 0.53f),
            LineupPlayer(8, "Marsh", 0.5f, 0.49f, true), LineupPlayer(7, "Quill", 0.76f, 0.53f), LineupPlayer(11, "Brandt", 0.2f, 0.26f),
            LineupPlayer(9, "Ellis", 0.5f, 0.19f, true), LineupPlayer(10, "Dunlop", 0.8f, 0.26f),
        )
        val a = listOf(
            LineupPlayer(1, "Keane", 0.5f, 0.90f), LineupPlayer(3, "Lind", 0.14f, 0.74f), LineupPlayer(5, "Moss", 0.38f, 0.76f),
            LineupPlayer(6, "Okoro", 0.62f, 0.76f), LineupPlayer(2, "Ayele", 0.86f, 0.74f), LineupPlayer(7, "Faro", 0.15f, 0.52f),
            LineupPlayer(8, "Tate", 0.38f, 0.5f), LineupPlayer(4, "Wren", 0.62f, 0.5f), LineupPlayer(11, "Pryor", 0.85f, 0.52f),
            LineupPlayer(9, "Dunn", 0.35f, 0.22f, true), LineupPlayer(10, "Sato", 0.65f, 0.22f),
        )
        return if (home) Lineup("4-3-3", "A. Lindqvist", h, listOf("12 Frost", "14 Abara", "17 Novak", "20 Lund"))
        else Lineup("4-4-2", "R. Calloway", a, listOf("13 Boyd", "15 Reyes", "18 Soto", "21 Wade"))
    }

    private val rows = listOf(
        Triple(ng, listOf(9, 7, 1, 1, 14, 22), "WWDWL"),
        Triple(ru, listOf(9, 6, 2, 1, 9, 20), "LDWLL"),
        Triple(hc, listOf(9, 6, 1, 2, 7, 19), "WWLWW"),
        Triple(rr, listOf(9, 5, 3, 1, 6, 18), "DWDWW"),
        Triple(at, listOf(9, 5, 1, 3, 3, 16), "LWWLD"),
        Triple(wv, listOf(9, 4, 2, 3, 1, 14), "DLWDL"),
        Triple(kb, listOf(9, 3, 3, 3, 0, 12), "DDLWD"),
        Triple(os, listOf(9, 2, 2, 5, -6, 8), "LLDLW"),
    )

    override fun standings(leagueId: String) = rows.mapIndexed { i, (t, n, f) ->
        StandingRow(i + 1, t, n[0], n[1], n[2], n[3], n[4], n[5], f)
    }

    override fun scorers(leagueId: String) = listOf(
        Scorer(1, "Leo Marsh", ng, 7), Scorer(2, "Sam Dunn", ru, 6), Scorer(3, "Theo Ellis", ng, 5),
        Scorer(4, "Ivo Brandt", hc, 5), Scorer(5, "Kai Pryor", rr, 4),
    )

    override fun news() = listOf(
        NewsItem("n1", "Match reports", "Northgate edge Riverside in a six-goal thriller", "Two late goals settle a game that swung three times in the second half.", "12 min ago · 4 min read"),
        NewsItem("n2", "Analysis", "Why Harbor City keep winning the second ball", "A look at pressing triggers and the numbers behind their run.", "1 hour ago · 6 min read"),
        NewsItem("n3", "Transfers", "Redmont open talks over a winter move for a left back", "Early stage discussions, nothing agreed yet.", "2 hours ago · 2 min read"),
        NewsItem("n4", "Match reports", "Stonebridge cruise past Norbury on the road", "Three first-half goals decide it.", "3 hours ago · 3 min read"),
        NewsItem("n5", "Analysis", "Five things to watch in tonight's Continental Cup tie", "Lineups, tactics and the key duels.", "5 hours ago · 5 min read"),
        NewsItem("n6", "Transfers", "Olympia confirm a new goalkeeper on a two-year deal", "The club announced the signing this morning.", "Yesterday · 2 min read"),
    )

    override fun player(id: String) = PlayerProfile(
        id = "p9", name = "Leo Marsh", initials = "LM", team = ng, position = "Striker", number = 9,
        age = 27, heightCm = 183, foot = "Right",
        goals = 7, assists = 3, rating = 7.6, matches = 9,
        lastRatings = listOf(8.9, 8.1, 7.3, 7.0, 6.4), shotsPer90 = 3.1, keyPassesPer90 = 1.2,
        scoutingNote = "Marsh has scored in 4 of his last 5 starts and takes the most shots per 90 in the squad.",
        career = listOf(
            CareerStop("Northgate FC", "2022 – now", 98, 41, current = true),
            CareerStop("Stonebridge", "2019 – 2022", 74, 22),
            CareerStop("Eastport", "2017 – 2019", 41, 9),
        ),
    )

    override fun popularTeams() = listOf(ng, ru, hc, rr, at, wv)
    override fun allTeams() = listOf(ng, ru, hc, rr, at, wv, kb, os, ep, nt, ls, hcr, nb, sb)

    override fun leagues() = listOf(
        LeagueInfo("pd", "Premier Division", "PD", 20),
        LeagueInfo("cc", "Continental Cup", "CC", 32),
        LeagueInfo("al", "Atlantic League", "AL", 18),
        LeagueInfo("st", "Second Tier", "ST", 22),
    )

    override fun languages() = listOf(
        Language("English", "Device language"), Language("اردو", "Urdu"), Language("العربية", "Arabic"),
        Language("हिन्दी", "Hindi"), Language("Español", "Spanish"), Language("Português", "Portuguese"),
        Language("Français", "French"), Language("Türkçe", "Turkish"), Language("Deutsch", "German"),
    )

    /** Canned answers until Firebase AI Logic is wired in. */
    override fun answer(question: String): String {
        val q = question.lowercase()
        return when {
            "second" in q || "harbor" in q -> "Harbor City are 1 point behind Riverside. A win against Redmont at 20:45 lifts them to 22 points. Riverside, on 20, are losing 1–2 right now."
            "score" in q || "northgate" in q -> "Leo Marsh leads Northgate with 7 goals this season, followed by Theo Ellis with 5."
            "redmont" in q || "form" in q -> "Redmont are unbeaten in 5, with 3 wins and 2 draws, and sit 4th on 18 points."
            else -> "I can answer questions about fixtures, standings, form and players using live match data. Try asking what a team needs to move up the table."
        }
    }
}
