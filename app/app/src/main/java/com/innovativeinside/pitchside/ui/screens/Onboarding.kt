package com.innovativeinside.pitchside.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.innovativeinside.pitchside.ui.components.glowBackground
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.FollowKind
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.data.prefs.PrefsReducer
import com.innovativeinside.pitchside.data.model.Languages
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.vm.OnboardingTeamsViewModel
import com.innovativeinside.pitchside.ui.vm.BuildHomeViewModel
import com.innovativeinside.pitchside.ui.vm.StepState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.data.prefs.Persona
import com.innovativeinside.pitchside.data.prefs.WatchStyle
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.PcSwitch
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SearchField
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay

private const val TOTAL_STEPS = 6

@Composable
private fun OnbScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    cta: String,
    onCta: () -> Unit,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().glowBackground().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) BackButton(onBack) else Spacer(Modifier.size(48.dp))
            // Segmented step progress: one pill per step, done steps in emerald.
            Row(Modifier.weight(1f).padding(horizontal = 8.dp).semantics { contentDescription = "Step $step of $TOTAL_STEPS" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(TOTAL_STEPS) { i -> Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i < step) Pc.Accent else Pc.Outline)) }
            }
            if (onSkip != null) TextAction("Skip", onSkip, color = Pc.Text2) else Spacer(Modifier.size(48.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 560.dp)) {
            if (title.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("STEP $step OF $TOTAL_STEPS", style = PcType.Kicker.copy(color = Pc.Accent, fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                Text(title, style = PcType.Display.copy(fontSize = 34.sp, lineHeight = 36.sp), modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = PcType.BodyMuted)
                Spacer(Modifier.height(24.dp))
            }
            content()
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(cta, onCta, Modifier.fillMaxWidth())
            if (secondary != null) TextAction(secondary, onSecondary, Modifier.fillMaxWidth(), color = Pc.Text2)
        }
    }
}

@Composable
private fun OptionCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(PcShape.Card).background(if (selected) Pc.AccentContainer else Pc.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Pc.Accent else Pc.Outline, PcShape.Card)
            .pcClickable(PcShape.Card, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Box(Modifier.size(48.dp).clip(CircleShape).background(if (selected) Pc.AccentContainer else Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (selected) Pc.Accent else Pc.Text2) }
        Column(Modifier.weight(1f)) {
            Text(title, style = PcType.TeamName)
            Text(desc, style = PcType.Caption)
        }
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) Pc.Accent else Color.Transparent).border(1.5.dp, if (selected) Pc.Accent else Pc.OutlineStrong, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Outlined.Check, null, tint = Pc.OnAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.fillMaxSize().glowBackground().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(PcShape.Chip).background(Pc.Accent), contentAlignment = Alignment.Center) { Text("P", style = PcType.TeamName, color = Pc.OnAccent) }
            Text("PITCHSIDE", style = PcType.SectionTitle)
        }
        Spacer(Modifier.height(40.dp))
        // What the app does, without inventing a match: the real fixtures load right after onboarding.
        com.innovativeinside.pitchside.ui.components.GlowCard(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                com.innovativeinside.pitchside.ui.components.LiveBadge(null, onHero = true)
                listOf(
                    "Live scores, goals and cards as they happen",
                    "Tables, lineups, stats and head-to-head",
                    "Win probability before every kickoff",
                ).forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.HeroRaised), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, null, tint = Pc.HeroAccent, modifier = Modifier.size(16.dp))
                        }
                        Text(line, style = PcType.Body, color = Pc.OnHero)
                    }
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        Text("EVERY MATCH,\nEXPLAINED.", style = PcType.Display.copy(fontSize = 48.sp, lineHeight = 48.sp))
        Spacer(Modifier.height(12.dp))
        Text("Live scores, match alerts, tables and form for the teams you follow.", style = PcType.BodyMuted)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Get started", onStart, Modifier.fillMaxWidth())
        TextAction("I already have an account", onRestore, Modifier.fillMaxWidth(), color = Pc.Text)
        Text("By continuing you accept the Terms and Privacy Policy.", style = PcType.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
    }
}

@Composable
fun LanguageScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var query by remember { mutableStateOf("") }
    val langs = Languages.all.filter { query.isBlank() || it.native.contains(query, true) || it.english.contains(query, true) }
    OnbScaffold(1, "Choose your language", "Alerts and Ask answers use it where available. Change it any time in Settings.", onBack, null, "Continue", onNext) {
        SearchField(query, { query = it }, "Search languages")
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            langs.forEach { l -> OptionCard(l.native, l.english, prefs.language == l.code, { app.setLanguage(l.code) }) }
            if (langs.isEmpty()) Text("No language matches \"$query\".", style = PcType.BodyMuted)
        }
    }
}

@Composable
fun TeamsScreen(onBack: () -> Unit, onNext: () -> Unit, vm: OnboardingTeamsViewModel = viewModel(factory = OnboardingTeamsViewModel.Factory)) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val state by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val bigOnly = prefs.followKind == FollowKind.BigOnly
    val limit = com.innovativeinside.pitchside.data.prefs.UserPrefs.FREE_TEAM_LIMIT
    OnbScaffold(
        2, "Who do you follow?", "Pick what you like. We'll build your home around it.", onBack, onNext,
        if (bigOnly) "Continue" else "Continue · ${prefs.teams.size} selected", onNext,
        secondary = "Free: follow up to $limit teams. Change any time.", onSecondary = {},
    ) {
        com.innovativeinside.pitchside.ui.components.ChipRow(
            FollowKind.entries.map { it.label }, prefs.followKind.label,
            { label -> app.setFollowKind(FollowKind.entries.first { it.label == label }) }, contentPadding = 0.dp,
        )
        Spacer(Modifier.height(16.dp))
        if (!bigOnly) {
            SearchField(query, { query = it }, "Search clubs")
            Spacer(Modifier.height(20.dp))
            Text(if (query.isBlank()) "POPULAR" else "RESULTS", style = PcType.SectionTitle.copy(fontSize = 18.sp))
            Spacer(Modifier.height(12.dp))
            StateContent(state, vm::retry) { all ->
                val teams = all.filter { query.isBlank() || it.name.contains(query, true) }
                Column {
                    if (teams.isEmpty()) Text("No club matches \"$query\".", style = PcType.BodyMuted)
                    teams.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { t ->
                                val on = t.id in prefs.teams
                                Column(
                                    Modifier.weight(1f).height(116.dp).clip(PcShape.Card).background(if (on) Pc.AccentContainer else Pc.Surface)
                                        .border(if (on) 1.5.dp else 1.dp, if (on) Pc.Accent else Pc.Outline, PcShape.Card)
                                        .pcClickable(PcShape.Card, if (on) "Unfollow ${t.name}" else "Follow ${t.name}") { app.toggleTeam(t.id, t.name) },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    Box {
                                        Crest(t, 48.dp)
                                        if (on) Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Pc.Accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnAccent, modifier = Modifier.size(12.dp)) }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(t.name, style = PcType.Label, maxLines = 2, textAlign = TextAlign.Center, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        } else {
            PcCard(Modifier.fillMaxWidth()) { Text("We'll show the biggest matches each day. You can follow teams later from Favorites.", style = PcType.BodyMuted, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun WatchScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    OnbScaffold(3, "How do you usually watch?", "This sets your spoiler and alert defaults.", onBack, onNext, "Continue", onNext) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OptionCard(WatchStyle.Live.title, WatchStyle.Live.desc, prefs.watchStyle == WatchStyle.Live, { app.setWatchStyle(WatchStyle.Live) }, Icons.Outlined.Bolt)
            OptionCard(WatchStyle.Later.title, WatchStyle.Later.desc, prefs.watchStyle == WatchStyle.Later, { app.setWatchStyle(WatchStyle.Later) }, Icons.Outlined.VisibilityOff)
            OptionCard(WatchStyle.Check.title, WatchStyle.Check.desc, prefs.watchStyle == WatchStyle.Check, { app.setWatchStyle(WatchStyle.Check) }, Icons.Outlined.CalendarMonth)
        }
        Spacer(Modifier.height(16.dp))
        Text("You can change this, and pick which teams are shielded, in Settings.", style = PcType.Caption)
    }
}

@Composable
fun AlertsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var rationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    OnbScaffold(
        4, "What should we alert you about?", "Only for the teams you follow. Mute any team in one tap.", onBack, onNext,
        if (prefs.alertLevel == AlertLevel.Off) "Continue" else "Turn on alerts",
        { if (prefs.alertLevel == AlertLevel.Off) onNext() else rationale = true },
        secondary = if (prefs.alertLevel == AlertLevel.Off) null else "Not now", onSecondary = { app.setAlertLevel(AlertLevel.Off); onNext() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertLevel.entries.forEach { lvl -> OptionCard(lvl.label, lvl.desc, prefs.alertLevel == lvl, { app.setAlertLevel(lvl) }) }
        }
        Spacer(Modifier.height(16.dp))
        PcCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Quiet hours", style = PcType.BodyStrong); Text("23:00 – 07:00, alerts wait until morning", style = PcType.Caption) }
                PcSwitch(prefs.quietHours, app::setQuietHours)
            }
        }
    }
    if (rationale) PcDialog(
        title = "Get goal alerts for your teams",
        body = "Android will ask next. We only send the alerts you chose. No promotions.",
        confirmLabel = "Continue",
        onConfirm = { rationale = false; if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onNext() },
        dismissLabel = "Not now", onDismiss = { rationale = false },
        icon = Icons.Outlined.Notifications,
    )
}

private fun personaIcon(p: Persona): ImageVector = when (p) {
    Persona.Fanatic -> Icons.Outlined.Bolt
    Persona.CatchUp -> Icons.Outlined.VisibilityOff
    Persona.Casual -> Icons.Outlined.Tune
    Persona.Nerd -> Icons.Outlined.QueryStats
    Persona.Explorer -> Icons.Outlined.Public
}

@Composable
fun PersonaScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    val p = PrefsReducer.persona(LocalPrefs.current)
    OnbScaffold(5, "", "", onBack, null, "Build my home", onNext) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(96.dp).clip(PcShape.Hero).background(Pc.SurfaceVariant).border(1.5.dp, Pc.Accent, PcShape.Hero), contentAlignment = Alignment.Center) { Icon(personaIcon(p), null, tint = Pc.Accent, modifier = Modifier.size(44.dp)) }
            Text("YOUR STYLE", style = PcType.Overline.copy(color = Pc.Accent))
            Text(p.title.uppercase(), style = PcType.Display, textAlign = TextAlign.Center)
            Text(p.tagline, style = PcType.BodyMuted)
        }
        Spacer(Modifier.height(24.dp))
        PcCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                p.bullets.forEachIndexed { i, b ->
                    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.Accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnAccent, modifier = Modifier.size(16.dp)) }
                        Text(b, style = PcType.Body)
                    }
                    if (i < p.bullets.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline))
                }
            }
        }
        TextAction("Not you? Change style", { app.setFollowKind(FollowKind.Clubs); app.setWatchStyle(WatchStyle.Live); onBack() }, Modifier.fillMaxWidth())
    }
}

@Composable
fun BuildingScreen(onDone: () -> Unit, vm: BuildHomeViewModel = viewModel(factory = BuildHomeViewModel.Factory)) {
    val steps by vm.steps.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.start() }
    LaunchedEffect(finished) { if (finished) { delay(400); onDone() } }
    val done = steps.count { it.state == StepState.Done || it.state == StepState.Skipped }
    OnbScaffold(
        TOTAL_STEPS, "Setting up your home", "Saving your choices and loading today's football.", null, null,
        if (finished) "Open the app" else "Working…", { if (finished) onDone() },
    ) {
        PcCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                steps.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (s.state) {
                            StepState.Done -> Box(Modifier.size(28.dp).clip(CircleShape).background(Pc.Accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnAccent, modifier = Modifier.size(16.dp)) }
                            StepState.Skipped -> Box(Modifier.size(28.dp).clip(CircleShape).background(Pc.WarningContainer), contentAlignment = Alignment.Center) { Text("!", color = Pc.Warning, fontWeight = FontWeight.ExtraBold) }
                            StepState.Running -> androidx.compose.material3.CircularProgressIndicator(color = Pc.Accent, trackColor = Pc.Outline, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                            StepState.Waiting -> Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, Pc.Outline, CircleShape))
                        }
                        Column {
                            Text(s.label, style = PcType.Body.copy(color = if (s.state != StepState.Waiting) Pc.Text else Pc.Text2, fontWeight = if (s.state == StepState.Running) FontWeight.Bold else FontWeight.Medium))
                            if (s.state == StepState.Skipped) Text("Offline. This loads when you're connected.", style = PcType.Caption.copy(color = Pc.Warning))
                        }
                    }
                    if (i < steps.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Pc.Outline)) {
            Box(Modifier.fillMaxWidth(done / steps.size.toFloat()).height(8.dp).clip(CircleShape).background(Pc.Accent))
        }
    }
}
