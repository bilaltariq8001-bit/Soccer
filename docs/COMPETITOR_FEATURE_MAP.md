# Competitor feature map (51 screenshots)

Reference only: LiveFootball v1.0.51 (`screenshots/`). We take the user flows, screen completeness and data needs. We do not copy its name, logo, green/red palette, artwork, text or layouts. Every row says where the feature lives in Pitchside and which repository call feeds it.

States legend: L loading, E empty, X error, S success.

| # | Screen | User goal | Main components / data | Actions → destination | Pitchside screen · data source | States in Pitchside |
|---|---|---|---|---|---|---|
| 01 | Select language | Pick UI language | Language list with flags, tick | Confirm → teams step | Onboarding · Language (`Languages.all`) | S (static list, searchable, "no match" text) |
| 02 | Your teams | Follow clubs | Team rows + Follow | Next → leagues | Onboarding · Teams (`popularTeams()`) | L/X/S, follow limit text |
| 03 | Your leagues | Follow leagues | League rows + Follow, native ads | Next → players | Folded into Leagues tab + Search (follow chip) | L/E/X/S |
| 04 | Your players | Follow players | Player rows + Follow | Get started | Player page star, Search players tab | L/X/S |
| 05 | After get started | Notification prompt | Cold system prompt over spinner | Allow / deny | Onboarding · Alerts: rationale dialog, then system prompt only if alerts chosen | S |
| 06 | Home today | See today's football | Yesterday/Today/Tomorrow, Top leagues row, Following, league cards | Match → detail; league icon → league | Home (`matchesOn(today)`, `leagues()`, `news()`, `standings("pd")`) | L skeleton, X retry, E card, S |
| 07 | Home scrolled | Scan all games | Collapsible league cards, FT / After Pen. | Match → detail | Home + Matches: collapsible `LeagueGroup` | as above |
| 08 | Highlights | Watch video | League chips, video cards | Video → YouTube | Not in v1 (no licensed source); News tab instead | - |
| 09 | News general | Read news | Article cards, Trending, Read more | External link | News tab + Article (`news()`, `article(id)`) | L/E/X/S, pagination |
| 10 | Transfers | Transfer news | Same as 09 | External link | News · Transfers chip | same |
| 11 | Match preview | Who will win | Win probability, results breakdown | Tabs | Match · Brief tab (Match Brief + key numbers) | L/X/S |
| 12 | Match table | Where teams stand | All/Home/Away, Short | Tab | Match · Table tab (highlights both clubs; knockout explained) | E "no table" |
| 13 | Match summary | Match facts | Round, venue, referee | - | Match · Brief info card (kickoff, venue, referee, round; never blank) | S |
| 14 | Commentary empty | Follow live | "No data" + Retry | Retry | Match · Timeline (events) with explained empty state | E explained |
| 15 | Lineup | See teams | Coach, players (N/A) | - | Match · Lineups + Pitch view (`lineups`) | E "announced ~1h before" |
| 16 | Stats empty | Numbers | "No data" | Retry | Match · Stats bars (possession, shots, xG) | E "start at kickoff" |
| 17 | Interstitial ad | - | Full-screen ad | - | Deliberately none | - |
| 18 | Shorts | Short videos | Vertical feed (off-topic) | - | Dropped (off-topic, unlicensed) | - |
| 19 | Favorites | Manage follows | Leagues/Teams/Players tabs, Following toggle | Unfollow | Favorites tab (`FavoritesViewModel`) | L/E per tab/X/S, unfollow confirm |
| 20-21 | More menu | Settings | Pro banner, TV, Players, Language, Notifications, Theme, Rate, Feedback, Share, Privacy, Terms, Version | Rows | Settings (reached from Favorites) | S; every row works |
| 22 | League matches | League fixtures | Date/Round toggle, grouped by day | Match → detail | League · Matches (grouped by day) | E |
| 23 | League standings | Table | All/Home/Away, Short/Full | Team | League · Table Short/Full/Form | E knockout |
| 24 | League teams | Follow teams | Team rows + Follow | Team page | League · Teams (follow chip, opens team) | E |
| 25 | Top scorers | Who scores | Ranked players with goals | Player page | League · Top scorers (`scorers()`) | E |
| 26 | Referees | Officials | Matches officiated | - | Not in v1 (no data in our API) | - |
| 27 | Team matches | Club fixtures | Grouped fixtures | Match | Team · Matches (`teamFixtures`) | E |
| 28 | Team overview | Club facts | Coach, stadium, capacity, founded, surface | - | Team · Overview tiles | S, "Not listed" fallbacks |
| 29 | Team squad | Players | By position, number badge, rating | Player | Team · Squad (by position, rating chip) | E |
| 30 | Team standings | Club in table | League dropdown | Team | Team · Table (club highlighted) | E |
| 31 | Player skeleton | - | Skeleton loader | - | Shared `DetailSkeleton` on every detail page | L |
| 32 | Player profile | Who is this | Height, weight, age, nationality, foot, market value | Follow | Player · Profile (tags + stat tiles + scouting note) | L/X/S |
| 33 | Player matches | Recent form | Matches with minutes + rating | - | Player · Matches (rating chips) | E |
| 34 | Player stats | Numbers | Season, Overall/Per 90, xG | - | Player · Stats Season/Per 90 bars | S |
| 35-36 | Player career | History | Timeline per club, trophies | - | Player · Career timeline | S |
| 37 | Exit dialog | Avoid accidental exit | Cancel / Exit | Exit | Back on Home → "Leave Pitchside?" dialog | S |
| 38 | Search empty | Find things | Teams/Leagues tabs | - | Search Idle state with suggestions | Idle |
| 39 | Search results | Find team | Live filtering | Team | Search (debounced `search(q)`) | L/E/X/S |
| 40 | Search leagues tab | Find league | - | League | Search · Leagues chip | E per tab |
| 41 | Calendar | Pick a date | Month grid sheet | Date → fixtures | Matches · calendar `DatePickerDialog` | E for empty days |
| 42 | TV schedule | Where to watch | Country picker, empty TV | - | Not in v1 (no data source) | - |
| 43 | Players list | Top players | Photo, flag, position, rating, value | Player | Leagues tab · Top-rated players (`topPlayers()`) | L/S |
| 44 | Notifications | Per-item alerts | Bell per league/team | Toggle | Favorites team card alert levels + match bell | S |
| 45 | Theme sheet | Light/Dark | System/Light/Dark | - | Settings · Theme segmented control (persisted) | S |
| 46-47 | Dark mode | Night use | Near-black UI | - | Dark theme is our default palette; light theme tuned for contrast | S |
| 48 | App language | Change language | List | - | Settings · Language sheet | S |
| 49 | Live matches | What's live | Grouped live list, LIVE minute | Match | Matches · Live filter + nav badge with live count | L/E/X/S, polls every 30 s |
| 50 | Pro paywall | Upsell | Hero, bullets, Start free trial | Trial | Paywall (one honest screen; billing not connected yet, says so) | S |
| 51 | Watch player | Watch | YouTube embed failing | - | Dropped (fragile embed) | - |
