# Checkpoints

| # | Milestone | Key files | Tests run | Result | Known issues |
|---|---|---|---|---|---|
| 1 | Toolchain works in sandbox | (local only) | `assembleDebug` on original code | Reached compile | aarch64 needs `android.aapt2FromMavenOverride` (sandbox only, not committed) |
| 2 | Data + state layers | core/, data/, ui/vm/ | compile | Green | – |
| 3 | All screens on ViewModels | ui/screens/, ui/AppRoot.kt | `assembleDebug` | APK built (commit 8a1f848) | – |
| 4 | Test suite | src/test/ | `testDebugUnitTest` | 57/57 | Robolectric needs `robolectric.conscryptMode=OFF` on ARM hosts (set in Gradle) |
| 5 | Lint clean-up | Manifest, strings, formatters | `lintDebug` | 0 errors, 2 warnings (from 16) | Remaining warnings listed in TEST_REPORT.md |
| 6 | Docs + PR | docs/, README.md | full `assembleDebug testDebugUnitTest lintDebug` | Green | Firebase/Billing/FCM not wired (no keys) |
