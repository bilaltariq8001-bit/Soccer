# QA checklist

Legend: ✅ automated test, 👁 verified by code review / preview, ⏳ needs a device or real backend.

## Flows
| Flow | Status | Where |
|---|---|---|
| Launch, splash holds until prefs read | 👁 | MainActivity |
| New user onboarding → follow a team → alerts "Not now" → Build my home → Home | ✅ | OnboardingFlowTest |
| Home loads, shows live count, news, table | ✅ | AppNavigationTest, ViewModelTest |
| Tabs: Matches (Live filter), Leagues → League → Top scorers → back, News, Favorites | ✅ | AppNavigationTest |
| Back on Home asks to exit; Cancel keeps app | ✅ | AppNavigationTest |
| Matches: date change, far date empty state, yesterday all finished | ✅ | ViewModelTest |
| Match detail: live data, upcoming without stats/lineups, knockout without table, unknown id → not found | ✅ | RepositoryTest, ViewModelTest |
| Search: debounce, stale query cancelled, empty, error + retry, clear | ✅ | ViewModelTest |
| News: paginate to end, category filter, error | ✅ | ViewModelTest |
| Ask: quota counts only on success, retry failed answer, quota sheet | ✅ | ViewModelTest |
| Sign up with field errors → success; wrong password; reset password | ✅ | AuthScreenTest, ViewModelTest |
| Protected Account route redirects to sign-in when signed out | 👁 | AccountRoute |
| Follow limit (5 free) opens paywall | ✅ (reducer) 👁 (navigation) | PrefsReducerTest, AppViewModel |
| Spoiler Shield hide/reveal/caught up | ✅ | PrefsReducerTest |
| Offline / 503 simulation from Settings → every screen shows error + retry | 👁 | Settings › Developer |
| Pull-to-refresh failure keeps data and shows banner | ✅ | ViewModelTest |
| Deep link `pitchside://match/m1` | ⏳ | `adb shell am start -d pitchside://match/m1` |
| Notification permission on Android 13+ | ⏳ | Onboarding › Alerts |
| Real gateway responses | ⏳ | Needs deployed backend |

## Components
| Component | Default | Pressed/focus | Disabled | Loading | Error | Empty |
|---|---|---|---|---|---|---|
| PrimaryButton | ✅ | 👁 focus ring | 👁 | 👁 spinner | – | – |
| PcTextField | ✅ | 👁 | 👁 | – | ✅ | – |
| StateContent | ✅ | – | – | ✅ | ✅ (retry/no retry) | ✅ |

## Accessibility (👁)
- Match rows, hero, table rows and events have merged content descriptions (including "score hidden by Spoiler Shield").
- Crest monograms are hidden from TalkBack; names are next to them.
- Errors and loading regions are polite live regions; field errors use semantics `error`.
- Touch targets ≥ 48dp; text ≥ 12sp; light theme colours tuned for 4.5:1.
