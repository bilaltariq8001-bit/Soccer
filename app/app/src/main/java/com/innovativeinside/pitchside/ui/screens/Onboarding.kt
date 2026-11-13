package com.innovativeinside.pitchside.ui.screens

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
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.AlertLevel
import com.innovativeinside.pitchside.AppViewModel
import com.innovativeinside.pitchside.FollowKind
import com.innovativeinside.pitchside.LocalApp
import com.innovativeinside.pitchside.Persona
import com.innovativeinside.pitchside.WatchStyle
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
    Column(Modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) BackButton(onBack) else Spacer(Modifier.size(48.dp))
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Pc.Border)) {
                Box(Modifier.fillMaxWidth(step / TOTAL_STEPS.toFloat()).height(4.dp).clip(CircleShape).background(Pc.Lime))
            }
            if (onSkip != null) TextAction("Skip", onSkip, color = Pc.Text2) else Spacer(Modifier.size(48.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 560.dp)) {
            if (title.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(title, style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = PcType.BodySmall.copy(fontSize = 15.sp, lineHeight = 22.sp))
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
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(PcShape.Card).background(if (selected) Pc.Raised else Pc.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Pc.Lime else Pc.Border, PcShape.Card)
            .pcClickable(PcShape.Card, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.Border), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (selected) Pc.Lime else Pc.Text2) }
        Column(Modifier.weight(1f)) {
            Text(title, style = PcType.Subhead)
            Text(desc, style = PcType.BodySmall)
        }
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) Pc.Lime else Color.Transparent).border(1.5.dp, if (selected) Pc.Lime else Pc.BorderStrong, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).clip(PcShape.Chip).background(Pc.Lime), contentAlignment = Alignment.Center) { Text("P", color = Pc.OnLime, fontWeight = FontWeight.ExtraBold) }
            Text("Pitchside", style = PcType.Headline)
        }
        Spacer(Modifier.height(48.dp))
        PcCard(Modifier.widthIn(max = 520.dp).fillMaxWidth(), shape = PcShape.Hero, border = Pc.BorderStrong) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    com.innovativeinside.pitchside.ui.components.StatusBadge(com.innovativeinside.pitchside.data.Status.Live, "74'")
                    Text("Premier Division", style = PcType.Meta)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { Crest(com.innovativeinside.pitchside.data.FakeRepository.ng, 36.dp); Text("Northgate", style = PcType.Label.copy(fontSize = 14.sp), maxLines = 1) }
                    Text("2 – 1", style = PcType.ScoreL)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { Crest(com.innovativeinside.pitchside.data.FakeRepository.ru, 36.dp); Text("Riverside", style = PcType.Label.copy(fontSize = 14.sp), maxLines = 1) }
                }
                Text("Riverside need one goal to stay level with the leaders.", style = PcType.BodySmall.copy(color = Pc.Text))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Every match,\nexplained.", style = PcType.Title.copy(fontSize = 38.sp, lineHeight = 42.sp))
        Spacer(Modifier.height(12.dp))
        Text("Live scores, smart alerts and a short brief on every game, in your language.", style = PcType.BodySmall.copy(fontSize = 16.sp, lineHeight = 24.sp))
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Get started", onStart, Modifier.fillMaxWidth())
        TextAction("I already have an account", onRestore, Modifier.fillMaxWidth(), color = Pc.Text)
        Text("By continuing you accept the Terms and Privacy Policy.", style = PcType.Meta.copy(color = Pc.Text2), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
    }
}

@Composable
fun LanguageScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    var query by remember { mutableStateOf("") }
    val langs = app.repo.languages().filter { query.isBlank() || it.native.contains(query, true) || it.english.contains(query, true) }
    OnbScaffold(1, "Choose your language", "Scores, alerts and Match Briefs will be written in it. Change it any time in Settings.", onBack, null, "Continue", onNext) {
        SearchField(query, { query = it }, "Search languages")
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            langs.forEach { l -> OptionCard(l.native, l.english, app.language == l.native, { app.chooseLanguage(l.native) }) }
            if (langs.isEmpty()) Text("No language matches \"$query\".", style = PcType.BodySmall)
        }
    }
}

@Composable
fun TeamsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    var query by remember { mutableStateOf("") }
    val teams = app.repo.allTeams().filter { query.isBlank() || it.name.contains(query, true) }
    val bigOnly = app.followKind == FollowKind.BigOnly
    OnbScaffold(
        2, "Who do you follow?", "Pick what you like. We will build your home around it.", onBack, onNext,
        if (bigOnly) "Continue" else "Continue, ${app.followedTeams.size} selected", onNext,
        secondary = "Free: follow up to 5 teams. Change any time.", onSecondary = {},
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Chips scroll horizontally on narrow screens
        }
        com.innovativeinside.pitchside.ui.components.ChipRow(
            FollowKind.entries.map { it.label }, app.followKind.label,
            { label -> app.followKind = FollowKind.entries.first { it.label == label } }, contentPadding = 0.dp,
        )
        Spacer(Modifier.height(16.dp))
        if (!bigOnly) {
            SearchField(query, { query = it }, "Search clubs")
            Spacer(Modifier.height(20.dp))
            Text(if (query.isBlank()) "Popular near you" else "Results", style = PcType.Overline.copy(color = Pc.Text2))
            Spacer(Modifier.height(12.dp))
            teams.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t ->
                        val on = t.id in app.followedTeams
                        Column(
                            Modifier.weight(1f).height(112.dp).clip(PcShape.Card).background(if (on) Pc.Raised else Pc.Surface)
                                .border(if (on) 1.5.dp else 1.dp, if (on) Pc.Lime else Pc.Border, PcShape.Card)
                                .pcClickable(PcShape.Card, if (on) "Unfollow ${t.name}" else "Follow ${t.name}") { app.toggleFollow(t.id) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                Crest(t, 48.dp)
                                if (on) Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(12.dp)) }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(t.name, style = PcType.Label, maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            PcCard(Modifier.fillMaxWidth()) { Text("We will show the biggest matches each day. You can follow teams later from Favorites.", style = PcType.BodySmall, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun WatchScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    OnbScaffold(3, "How do you usually watch?", "This sets your spoiler and alert defaults.", onBack, onNext, "Continue", onNext) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OptionCard(WatchStyle.Live.title, WatchStyle.Live.desc, app.watchStyle == WatchStyle.Live, { app.watchStyle = WatchStyle.Live }, Icons.Outlined.Bolt)
            OptionCard(WatchStyle.Later.title, WatchStyle.Later.desc, app.watchStyle == WatchStyle.Later, { app.watchStyle = WatchStyle.Later }, Icons.Outlined.VisibilityOff)
            OptionCard(WatchStyle.Check.title, WatchStyle.Check.desc, app.watchStyle == WatchStyle.Check, { app.watchStyle = WatchStyle.Check }, Icons.Outlined.CalendarMonth)
        }
        Spacer(Modifier.height(16.dp))
        Text("You can change this, and pick which teams are shielded, in Settings.", style = PcType.Meta.copy(color = Pc.Text2))
    }
}

@Composable
fun AlertsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    var rationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    OnbScaffold(
        4, "What should we alert you about?", "Only for the teams you follow. Mute any team in one tap.", onBack, onNext,
        if (app.alertLevel == AlertLevel.Off) "Continue" else "Turn on alerts",
        { if (app.alertLevel == AlertLevel.Off) onNext() else rationale = true },
        secondary = if (app.alertLevel == AlertLevel.Off) null else "Not now", onSecondary = { app.alertLevel = AlertLevel.Off; onNext() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertLevel.entries.forEach { lvl -> OptionCard(lvl.label, lvl.desc, app.alertLevel == lvl, { app.alertLevel = lvl }) }
        }
        Spacer(Modifier.height(16.dp))
        PcCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Quiet hours", style = PcType.Label.copy(fontSize = 15.sp)); Text("23:00 – 07:00, alerts wait until morning", style = PcType.Meta.copy(color = Pc.Text2)) }
                PcSwitch(app.quietHours) { app.quietHours = it }
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
    val p = app.persona()
    OnbScaffold(5, "", "", onBack, null, "Build my home", onNext) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(96.dp).clip(PcShape.Hero).background(Pc.Raised).border(1.5.dp, Pc.Lime, PcShape.Hero), contentAlignment = Alignment.Center) { Icon(personaIcon(p), null, tint = Pc.Lime, modifier = Modifier.size(44.dp)) }
            Text("Your style", style = PcType.Overline.copy(color = Pc.Text2))
            Text(p.title, style = PcType.Title.copy(fontSize = 34.sp, lineHeight = 38.sp), textAlign = TextAlign.Center)
            Text(p.tagline, style = PcType.BodySmall.copy(fontSize = 16.sp))
        }
        Spacer(Modifier.height(24.dp))
        PcCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                p.bullets.forEachIndexed { i, b ->
                    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(16.dp)) }
                        Text(b, style = PcType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp))
                    }
                    if (i < p.bullets.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                }
            }
        }
        TextAction("Not you? Change style", { app.followKind = FollowKind.Clubs; app.watchStyle = WatchStyle.Live; onBack() }, Modifier.fillMaxWidth())
    }
}

@Composable
fun BuildingScreen(onDone: () -> Unit) {
    val app = LocalApp.current
    val steps = listOf(
        "Language set to ${app.language}", "Preferences saved", "Subscribing to alerts for ${app.followedTeams.size} teams",
        "Loading today's fixtures", "Preparing your Match Briefs",
    )
    var done by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (done < steps.size) { delay(650); done++ }
        app.completeOnboarding()
        delay(300)
        onDone()
    }
    OnbScaffold(
        TOTAL_STEPS, "Setting up your home", "Real steps, not a fake loader. Usually under 10 seconds.", null, null, "Open the app", { app.completeOnboarding(); onDone() },
    ) {
        PcCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                steps.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        when {
                            i < done -> Box(Modifier.size(28.dp).clip(CircleShape).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(16.dp)) }
                            i == done -> androidx.compose.material3.CircularProgressIndicator(color = Pc.Lime, trackColor = Pc.Border, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                            else -> Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, Pc.Border, CircleShape))
                        }
                        Text(s, style = PcType.Body.copy(fontSize = 15.sp, color = if (i <= done) Pc.Text else Pc.Text2, fontWeight = if (i == done) FontWeight.Bold else FontWeight.Medium))
                    }
                    if (i < steps.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Step ${(done + 1).coerceAtMost(steps.size)} of ${steps.size}", style = PcType.Meta.copy(color = Pc.Text2))
            Text("${(done * 100) / steps.size}%", style = PcType.Meta.copy(color = Pc.Text2))
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Pc.Border)) {
            Box(Modifier.fillMaxWidth(done / steps.size.toFloat()).height(8.dp).clip(CircleShape).background(Pc.Lime))
        }
    }
}
