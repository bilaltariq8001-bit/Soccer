# Phase 2 + 3: Unique value and personalised onboarding

Constraints applied (from ux-audit.md): Android only, Firebase + API-Football via thin backend, AI only through Firebase AI Logic, anonymous identity, no interstitials on navigation, no embedded YouTube, no Shorts, no fake testimonials, v1 features only in recommendations.

## Phase 2 - Unique value

### Differentiators (all work inside the constraints)

| # | Idea | What the user gets | Why competitors don't | Cost / risk |
|---|---|---|---|---|
| 1 | **Match Brief** (hero) | One glanceable card per match that *explains* it: before kickoff what to watch, live what just happened and what it means, after a short recap, in the user's language | They show raw tables; their Stats/Commentary tabs were empty for the live match we opened | Low-medium: AI text is cached per match + language |
| 2 | **Spoiler Shield** | Hide scores and results for chosen teams/leagues until the user taps to reveal; alerts held until "I'm caught up" | No spoiler control seen; their alerts and cards show results immediately | Very low: client logic only, no AI |
| 3 | **Smart Alerts** | Per-team alert levels (every goal / key moments / final only / digest at a time) plus quiet hours | They only offer an on/off bell per item | Low: FCM topics + backend rules |
| 4 | **Ask Football** | Chat that answers with real data (fixtures, form, stats) using function calling | No assistant | Medium: quota + guardrails |
| 5 | **Calm by design** | Light ads, one honest paywall, fast cached screens | Ads on almost every tap; two paywalls | Revenue tuning needed |

### Hero feature: Match Brief

**What it is.** A card at the top of every match and a one-line version on the fixture row.
- *Before:* "City host Brighton tonight. Form: W W D W L vs L D W L L. A win takes City top. Watch Haaland (9 goals)."
- *Live:* "62' - 1-0 Brighton. City need one goal to stay level on points with Arsenal. 71% possession, 9 shots, 0 on target in the last 15'."
- *After:* "Brighton edge a tight game 1-0. Decisive moment: 58' goal from a corner. City stay 2nd."

**Why it works inside the constraints.**
- **Numbers are never invented.** The backend computes every fact deterministically (score, form, standing impact "if result holds", top scorer, possession) and passes a small JSON to the model. The model only phrases it (system instruction: use only provided fields).
- **Cost is shared.** One generation per match + state change + language is cached in Firestore and served to all users; live briefs refresh at most every 5 minutes or on a key event (goal, red card, HT, FT).
- **Always works.** If AI is down or over quota, a template string built from the same JSON is shown (no blank state).
- **Quota.** Brief is free for followed teams; deeper "Why it matters" and chat use the daily AI quota (Free low, Pro higher, rewarded ad for more).
- **Trust.** Small "AI-generated from match data" label and a report link.

**Why a premium UI needs it.** It replaces empty "No data available" tabs (14, 16) with something useful on every match, even low-league games.

**Exclusions check.** No interstitials, no YouTube, no accounts needed, AI via Firebase AI Logic with App Check, no fake content. Passes.

### v1 vs parked

- **v1:** Match Brief, Spoiler Shield, Smart Alerts (levels + quiet hours), Ask Football (basic), light ads, single paywall.
- **v2 (parked):** daily digest notification, voice Q&A, widgets, highlights/news feed, Shorts with licensed source, TV schedule.

## Phase 3 - Personalised onboarding (shown once)

### Principles
- Show value first: after the first answer the user sees a live preview of their home, which fills in as they answer.
- Every step is skippable; no ads in onboarding; ~60 seconds total.
- Branching: skip steps that do not apply.
- Permission asked only when it clearly helps (Step 5), with a rationale.

### Flow

| Step | Screen | Question / content | Branching and notes |
|---|---|---|---|
| 0 | Splash -> Welcome | Logo, one line promise, "Get started" and "I already use the app" (restore) | Restore via sign-in link if the user linked an account earlier |
| 1 | Language | Auto-detected from device, confirm or change | Sets RTL, numerals, AI output language |
| 2 | What do you follow? (multi) | Clubs / National teams / Players (fantasy) / Just the big matches | "Just the big matches" skips Step 3 |
| 3 | Pick teams | Popular teams pre-suggested from locale, search, chips, max free follows shown honestly | Players picker appears only if "Players" chosen |
| 4 | How do you watch? | Live on TV or stream / Highlights later / I check scores during the day | Drives spoiler shield and alert defaults |
| 5 | Alerts | Every goal / Key moments / Final scores only / Not now, plus quiet hours | Permission rationale, then system prompt only if the user chose alerts |
| 6 | Depth | Simple / Detailed stats | Sets default match tab and AI verbosity |
| 7 | Persona result | "You're a Matchday Fanatic" with 3 bullets of what we set up | Editable in Settings |
| 8 | Building your home | Real work: create anonymous user, save prefs to Firestore, subscribe alert topics, prefetch today's fixtures, warm Match Brief cache | Progress shows actual steps, then opens Home |

### Personas (cover every audience)

| Persona | Typical answers | Flags | Defaults and copy | v1 recommendations |
|---|---|---|---|---|
| **Matchday Fanatic** | Follows clubs, watches live, wants all goals, detailed | `liveFirst`, `alerts=all`, `statsDetailed`, `spoilerShield=off` | Home pins live and My Teams; opens Match tab on Summary; copy "Never miss a goal" | Live tab, Match Brief live updates, goal alerts, Ask Football |
| **Catch-up Fan** | Watches highlights later | `spoilerShield=on`, `alerts=final@time`, `briefMode=recap` | Scores hidden for followed teams until reveal; alerts held; copy "Watch first, spoil never" | Spoiler Shield, recap Match Brief, caught-up toggle |
| **Casual Follower** | Just big matches, simple stats | `homeMode=bigMatches`, `alerts=keyMoments`, `statsDetailed=false` | Home shows top leagues and today's big games; short Brief; copy "Only what matters" | Match Brief, key-moment alerts, Fixtures |
| **Stats Nerd** | Players + detailed | `statsDetailed`, `playerFollow`, `briefMode=deep` | Opens Stats first; xG and per-90 up front; copy "All the numbers" | Player pages, Stats tab, deep Brief, Ask Football |
| **League Explorer** | Follows leagues, not clubs | `homeMode=leagueFirst`, `alerts=keyMoments` | Home grouped by league; standings one tap away; copy "Your leagues, one place" | League pages, standings, Fixtures calendar |

### Assignment logic (simple, deterministic)

```
if Q2 == "Just the big matches"            -> Casual Follower
else if Q4 == "Highlights later"           -> Catch-up Fan
else if Q2 includes "Players" and Q6 == Detailed -> Stats Nerd
else if no teams picked and leagues chosen -> League Explorer
else                                       -> Matchday Fanatic
```
Ties go to the first match above; the user can change persona any time in Settings (changes only defaults).

### Data saved (Firestore `users/{uid}`)

`language, persona, follows{teams,leagues,players}, flags{...}, alertLevel, quietHours, spoilerShield{teams,leagues}, aiQuota{day,used}, onboardedAt`

### Rules for copy and recommendations
- Mention only v1 features listed above.
- No reviews, ratings or testimonials; no "millions of fans" style claims.
- Honest limit text: "Free: follow up to N teams" (value set later via Remote Config).

## Next
Phase 4: design system board + full journey canvas (Launch, Onboarding, Setup, Permissions, Daily use, Core features, Alerts, Dialogs, States, Android rules, Services, Design system, v2 parked). Please confirm the hero feature (Match Brief + Spoiler Shield) and the 5 personas, then I will start the design system and canvas.
