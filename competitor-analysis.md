# Competitor teardown: LiveFootball (`com.football.livescores.liveupdates`)

Version tested: **1.0.51**, Android, TECNO BG6 (720x1612), Oct 29 2026. Screenshots are in `screenshots/`.

## App structure

**Onboarding (3 steps, all skippable):** Select Language -> Your Teams -> Your Leagues -> Your Players -> Get Started -> notification permission prompt.
(01-05)

**Bottom navigation (5 items):** Fixtures | Highlights | **Live** (large red centre button, badge shows live-match count) | Shorts | Favorites

| Screen | What it contains | Shots |
|---|---|---|
| Fixtures (home) | Yesterday / Today / Tomorrow tabs + calendar picker, "Top Leagues" circular icon row (CL, PL, La Liga, Nations League, Europa, World Cup 2026), "Following" section, then league cards grouped by competition with collapsible match lists and FT / After Pen. badges. Crown icon = Pro upsell, search icon, overflow menu. | 06, 07 |
| Highlights | 3 sub-tabs: Highlights (league filter chips + large video cards with duration), General (news articles), Transfers (transfer-rumour articles). Article cards have a "Trending" badge and a "Read more" external link. | 08-10 |
| Live | Opens a "Live Matches" list with a "Watch" button. In testing the list stayed on a spinner. | - |
| Match detail | Header: league, bell (match alerts), score, live minute, Live badge. Tabs: **Preview** (Win Probability, Results Breakdown, form), **Table** (All/Home/Away standings, Short/full toggle), **Summary** (round, venue, referee), **Commentary**, **Lineup** (coach, players), **Stats**. | 11-16 |
| League page | Header with league logo, calendar, match-alert bell and Following toggle. Tabs: **Matches** (Date / Round toggle, grouped by day, red-card markers), **Standings** (All/Home/Away, Short/full), **Teams** (follow per team), **Top Scorers**, **Referees** (matches officiated). | 22-26 |
| Team page | Crest header with Follow + bell + calendar. Tabs: **Matches**, **Overview** (coach, stadium with capacity / founded / surface), **Squad** (grouped by position, jersey number badge, rating chip), **Standings** (league selector dropdown). | 27-30 |
| Player page | Header with photo, club, position chip, nationality, shirt number, refresh and Follow. Tabs: **Profile** (basic info, current team, market value with yearly change), **Matches** (recent matches with minutes and rating chip), **Stats** (season picker, Overall / Per 90, xG, xGOT, shots), **Career** (summary, timeline per club, trophies). Skeleton loading state shown first. | 31-36 |
| Search | Teams / Leagues tabs, live filtering while typing, illustrated empty state. | 38-40 |
| Calendar | Bottom sheet "Select Date" with month grid and today highlighted. | 41 |
| Live Matches | List of currently live games grouped by league, red LIVE minute badge (LIVE 47', LIVE HT'). "Watch" button opens a **Pro paywall** (Start Free Trial, "No Payment Now"), then a YouTube-based Watch player. | 49-51 |
| Shorts | TikTok-style vertical video feed with share button and likes. Content looked scraped (Pinterest, American football), not soccer. | 18 |
| Favorites | Tabs: Leagues / Teams / Players, each with a Following/Follow toggle and search. Some leagues were pre-followed. | 19 |
| More (side menu) | Go PRO for Life banner, TV Schedule, Players, App Language, Notifications, Theme (System default), Rate Us, Feedback, Share App, Privacy Policy, Terms & Conditions, Version. | 20, 21 |
| TV Schedule | Today / Tomorrow tabs, country picker (Pakistan), illustrated "No matches on TV" empty state. | 42 |
| Players (top list) | Cards with photo, nationality flag, position chip, rating and market value (e.g. Mbappe, Yamal, Haaland). | 43 |
| Notifications settings | Leagues / Teams tabs, per-item bell toggle for everything you follow. | 44 |
| Theme | Bottom sheet: System default / Light / Dark. Dark theme is a proper near-black UI. | 45-47 |
| App Language | Full-screen list of ~30 languages with flags and a confirm tick. | 48 |
| Exit dialog | Pressing Back on home shows "Are you sure you want to exit App?" (Cancel / Exit). | 37 |

## Monetization

- **Ads are everywhere and very aggressive.** This is the dominant UX feature:
  - Persistent banner (Meta Audience Network / AdMob) at the bottom of every screen.
  - Native ads inside the onboarding lists and between content.
  - A story-style ad strip at the top of the home feed.
  - **Full-screen interstitials on almost every navigation** (tab switches, match sub-tabs, back navigation). Networks seen: AppLovin, Google Ads, Meta. Many are video or playable ads that last 15-30+ s with delayed close buttons, and some open the Play Store.
- **Pro upgrade:** one-time lifetime purchase, **Rs 8,300 (about US$30 in Pakistan pricing)**, "ad-free experience and more". Tapping the More-menu banner goes straight to the Google Play purchase sheet.
- **A second paywall exists** ("Live Score PRO", reached via Live > Watch): hero photo, 5 benefit bullets (instant alerts, detailed stats, unlimited follows, smooth screens, no interruptions), gradient **Start Free Trial** button and "No Payment Now" reassurance (50).
- Crown icon on the home header is a third entry point to Pro.
- Notable: Pro benefits include *unlimited follows*, so free users are likely capped on followed teams and leagues.

## Weaknesses we can beat

1. **Ad load.** Interstitial on nearly every tap makes the app slow and frustrating. A light-ad or rewarded-only model would be a clear differentiator.
2. **Loading states.** "Loading..." spinner on first home load, Live Matches never loaded in testing, and several tabs show "No data available" (Commentary, Stats) for a live match.
3. **Missing data on lesser leagues.** Lineups were only a coach with "N/A", no venue or referee.
4. **Shorts feed is off-topic** (not soccer) and unmoderated.
5. **Small details:** broken characters in news text ("men,Äôs"), blank placeholder images before load, team crests missing for smaller clubs.
6. Onboarding forces three list steps with no preselection help (e.g. no "popular near you").
7. Pro is a single expensive price, with no trial or cheaper monthly option.

## Strengths worth matching

- Clear 5-tab navigation with a prominent Live button and live count badge.
- League grouping with collapsible cards and a "Following" section pinned on top.
- Match page depth: win probability, results breakdown, form, standings, commentary, lineup, stats.
- Highlights video + news + transfers in one place.
- Follow leagues / teams / players model with per-match bell alerts.
- Multi-language support (many languages offered at first launch).
- Light/dark/system theme setting.

## More findings from the second pass (ad-free session)

- **Content depth is the real strength:** league, team and player pages are rich (xG, per-90 stats, career timeline, market value, squad ratings). Our app needs to match this to compete.
- **Watch is fragile:** it embeds YouTube, which showed "Sign in to confirm you're not a bot" and an empty Matches list (51). Do not depend on embedded YouTube for a core feature.
- **Slow data on first load:** player and Top Scorers tabs show skeleton or spinner for a few seconds (31, 25).
- **Placeholder avatars** for most players outside the biggest stars (24, 25).
- Good UX patterns to reuse: bottom-sheet calendar and theme picker, illustrated empty states, skeleton loaders, Date/Round toggle, per-item bell toggles, exit confirmation.

## Not covered

Match-detail Lineup pitch view and Stats for a major game (only a low-league match was available), Rate Us / Feedback / Share flows, and the Highlights video player.

## Capture notes

- Screenshots containing personal notifications or the Google account on the purchase sheet were deleted and not saved.
- Nothing was purchased and no account sign-in was performed.
- Notification permission was declined at the prompt.
- Interstitial ad examples: 17.
- Dark theme was switched on to capture it and set back to System default afterwards. The Pro trial button was not tapped.
- Second pass added screenshots 22-51 (51 files total).
