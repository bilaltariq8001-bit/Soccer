# Pitchside visual system

## Direction

Pitchside is an editorial matchday companion. Live scores take the lead on Home and match pages; reporting gets a calmer reading surface; settings and tables stay low-noise. The featured match uses a pitch-green stage, with warm ivory reading surfaces and a restrained coral live signal.

The selected direction is premium editorial. A minimal utility direction informed the compact, flat score rows. A bold supporter direction informed the use of club colors only for crests and team identity so they do not compete with status and score.

## Information hierarchy

1. Current live match state, score, and opening match detail.
2. Upcoming results and the latest reporting.
3. Followed clubs, leagues, players, search, and settings.

Top-level destinations remain Home, Matches, Leagues, News, and Favorites. Compact windows use a bottom navigation bar; wider windows retain the same destinations in a rail. Expanded Home reveals a supporting news and standings column while medium windows retain a single readable match column.

## Theme roles

| Role | Light | Dark | Use |
| --- | --- | --- | --- |
| Background | `#F4F3EC` | `#101A16` | Page canvas |
| Surface | `#FFFEF8` | `#18251F` | Inputs and grouped content |
| Raised | `#E9EBDD` | `#22332A` | Selected controls and supporting groups |
| Primary text | `#17211B` | `#F4F1E7` | Headlines, scores, and body |
| Secondary text | `#4D5B50` | `#B7C2B6` | Metadata and supporting copy |
| Brand lime | `#416D27` | `#C8F06A` | Primary actions and selected states |
| Live coral | `#C74735` | `#FF806B` | Live match state |
| Pitch stage | `#214731` | `#193B2D` | Featured match score |

The brand palette is stable across devices; explicit System, Light, and Dark preferences remain available. Material 3 semantic roles map to the brand's primary, secondary/live, tertiary/warning, error, and surface families.

## Type, shape, and spacing

- Manrope is used for navigation, labels, and reading; Barlow Condensed is reserved for scores and standout statistics.
- Semantic `PcType` roles define titles, headlines, body, labels, metadata, and score hierarchy. Large score type is limited to match moments.
- Shared spacing follows a 4dp rhythm: 4, 8, 12, 16, 24, and 32dp.
- Controls use a 14dp shape, cards 18dp, hero surfaces 24dp, and status badges 8dp. Cards group related content; editorial rows use spacing and separators.
- Touch targets remain at least 48dp; icon-only actions retain descriptive accessibility labels.

## Components and states

`PrimaryButton` is the primary action, with `SecondaryButton` for supporting or destructive actions. Chips filter or choose options within a screen; segmented and underline tabs change peer views. `MatchCard` handles fixtures and results; `HeroMatch` and the match-detail score stage carry the featured score. `InfoBanner`, `EmptyState`, and `MatchCardSkeleton` preserve offline, empty, loading, and recovery states. `SearchField`, `PcDialog`, and the Material 3 bottom sheet provide consistent input and overlays.

Motion is reserved for state changes and the short launch animation. Color is paired with labels and selected semantics; larger screens keep content bounded rather than stretching phone rows.
