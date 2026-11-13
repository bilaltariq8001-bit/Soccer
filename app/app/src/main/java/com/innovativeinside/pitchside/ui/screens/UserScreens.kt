package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.AlertLevel
import com.innovativeinside.pitchside.LocalApp
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FlowChips
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.PcSheet
import com.innovativeinside.pitchside.ui.components.PcSwitch
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SearchField
import com.innovativeinside.pitchside.ui.components.SecondaryButton
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Following ----------

@Composable
fun FollowingScreen(openSearch: () -> Unit, openPaywall: () -> Unit, openSettings: () -> Unit, openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var unfollow by remember { mutableStateOf<String?>(null) }
    val limit = 5
    Screen {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Favorites", style = PcType.Title.copy(fontSize = 28.sp, lineHeight = 34.sp), modifier = Modifier.weight(1f))
            IconAction(Icons.Outlined.Settings, "Settings", openSettings, filled = true)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.Lime).pcClickable(PcShape.Control, "Add a team", onClick = openSearch), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Add a team", tint = Pc.OnLime) }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedTabs(listOf("Teams", "Leagues", "Players"), tab, { tab = it })
            when (tab) {
                0 -> {
                    if (!app.isPro) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${app.followedTeams.size} of $limit free follows", style = PcType.Meta.copy(color = Pc.Text2))
                        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Pc.Border)) { Box(Modifier.fillMaxWidth((app.followedTeams.size / limit.toFloat()).coerceAtMost(1f)).height(6.dp).clip(CircleShape).background(Pc.Lime)) }
                    }
                    if (app.followedTeams.isEmpty()) EmptyState("Nothing here yet", "Follow teams to see their matches first.", actionLabel = "Find teams", onAction = openSearch)
                    app.repo.allTeams().filter { it.id in app.followedTeams }.forEach { t ->
                        val level = app.teamAlert[t.id] ?: AlertLevel.Key
                        PcCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Crest(t, 40.dp)
                                    Column(Modifier.weight(1f)) { Text(t.name, style = PcType.Label.copy(fontSize = 15.sp)); Text("Premier Division", style = PcType.Meta.copy(color = Pc.Text2)) }
                                    val on = t.id in app.shielded
                                    IconAction(if (on) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (on) "Spoiler Shield on for ${t.name}" else "Spoiler Shield off for ${t.name}", { app.toggleShield(t.id) }, tint = if (on) Pc.Lime else Pc.Text2, filled = true)
                                }
                                FlowChips(listOf("Every goal", "Key moments", "Final only", "Mute"), when (level) { AlertLevel.Every -> "Every goal"; AlertLevel.Key -> "Key moments"; AlertLevel.Final -> "Final only"; AlertLevel.Off -> "Mute" }, { label ->
                                    app.teamAlert[t.id] = when (label) { "Every goal" -> AlertLevel.Every; "Key moments" -> AlertLevel.Key; "Final only" -> AlertLevel.Final; else -> AlertLevel.Off }
                                })
                                TextAction("Unfollow", { unfollow = t.id }, color = Pc.Red)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(PcShape.Card).border(1.5.dp, Pc.BorderStrong, PcShape.Card).pcClickable(PcShape.Card, onClick = if (!app.isPro && app.followedTeams.size >= limit) openPaywall else openSearch), contentAlignment = Alignment.Center) {
                        Text(if (!app.isPro && app.followedTeams.size >= limit) "Go Pro for unlimited follows" else "Add a team", color = Pc.Lime, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> app.repo.leagues().forEach { l ->
                    val on = l.id in app.followedLeagues
                    PcCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Text(l.short, color = Pc.Lime, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                            Column(Modifier.weight(1f)) { Text(l.name, style = PcType.Label.copy(fontSize = 15.sp)); Text("${l.teams} teams", style = PcType.Meta.copy(color = Pc.Text2)) }
                            Chip(if (on) "Following" else "Follow", on, { if (on) app.followedLeagues.remove(l.id) else app.followedLeagues.add(l.id) })
                        }
                    }
                }
                else -> {
                    val p = app.repo.player("p9")
                    if (p.id !in app.followedPlayers) EmptyState("No players followed", "Follow players to track their form and goals.")
                    else PcCard(Modifier.fillMaxWidth(), onClick = { openPlayer(p.id) }) {
                        Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(p.team.color)), contentAlignment = Alignment.Center) { Text(p.initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                            Column(Modifier.weight(1f)) { Text(p.name, style = PcType.Label.copy(fontSize = 15.sp)); Text("${p.position}, ${p.team.name}", style = PcType.Meta.copy(color = Pc.Text2)) }
                            Text("${p.goals} goals", style = PcType.Label.copy(color = Pc.Lime))
                        }
                    }
                }
            }
        }
    }
    unfollow?.let { id ->
        val t = app.repo.allTeams().first { it.id == id }
        PcDialog(
            "Unfollow ${t.name}?", "You will stop getting alerts and its matches leave your home.", "Unfollow", { app.toggleFollow(id); unfollow = null },
            "Keep following", { unfollow = null }, danger = true,
        )
    }
}

// ---------- Search ----------

@Composable
fun SearchScreen(onBack: () -> Unit, openPaywall: () -> Unit, openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(0) }
    val teams = app.repo.allTeams().filter { query.isNotBlank() && it.name.contains(query, true) }
    val leagues = app.repo.leagues().filter { query.isNotBlank() && it.name.contains(query, true) }
    val player = app.repo.player("p9").takeIf { query.isNotBlank() && it.name.contains(query, true) }
    Screen(scroll = true) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            SearchField(query, { query = it }, "Search teams, leagues, players", autoFocus = true)
        }
        Spacer(Modifier.height(8.dp))
        ChipRow(listOf("All", "Teams", "Leagues", "Players"), listOf("All", "Teams", "Leagues", "Players")[tab], { tab = listOf("All", "Teams", "Leagues", "Players").indexOf(it) })
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (query.isBlank()) {
                Text("Trending", style = PcType.Overline.copy(color = Pc.Text2))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Northgate", "Riverside", "Harbor").forEach { Chip(it, false, { query = it }) } }
                EmptyState("Search everything", "Find a team, a league or a player. Results appear as you type.")
            } else if (teams.isEmpty() && leagues.isEmpty() && player == null) {
                EmptyState("No results for \"$query\"", "Check the spelling or try a team or league name.", actionLabel = "Clear search", onAction = { query = "" })
            } else {
                if ((tab == 0 || tab == 1) && teams.isNotEmpty()) {
                    Text("Teams", style = PcType.Overline.copy(color = Pc.Text2))
                    teams.forEach { t ->
                        val on = t.id in app.followedTeams
                        ResultRow({ Crest(t, 40.dp) }, t.name, "Premier Division", on, onToggle = {
                            if (!on && !app.isPro && app.followedTeams.size >= 5) openPaywall() else app.toggleFollow(t.id)
                        })
                    }
                }
                if ((tab == 0 || tab == 2) && leagues.isNotEmpty()) {
                    Text("Leagues", style = PcType.Overline.copy(color = Pc.Text2))
                    leagues.forEach { l ->
                        val on = l.id in app.followedLeagues
                        ResultRow({ Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Text(l.short, color = Pc.Lime, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) } }, l.name, "${l.teams} teams", on, onToggle = { if (on) app.followedLeagues.remove(l.id) else app.followedLeagues.add(l.id) })
                    }
                }
                if ((tab == 0 || tab == 3) && player != null) {
                    Text("Players", style = PcType.Overline.copy(color = Pc.Text2))
                    val on = player.id in app.followedPlayers
                    ResultRow({ Box(Modifier.size(40.dp).clip(CircleShape).background(Color(player.team.color)), contentAlignment = Alignment.Center) { Text(player.initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) } }, player.name, "${player.position}, ${player.team.name}", on, onToggle = { if (on) app.followedPlayers.remove(player.id) else app.followedPlayers.add(player.id) }, onOpen = { openPlayer(player.id) })
                }
            }
        }
    }
}

@Composable
private fun ResultRow(leading: @Composable () -> Unit, title: String, sub: String, following: Boolean, onToggle: () -> Unit, onOpen: (() -> Unit)? = null) {
    PcCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading()
            Column(Modifier.weight(1f)) { Text(title, style = PcType.Label.copy(fontSize = 15.sp)); Text(sub, style = PcType.Meta.copy(color = Pc.Text2)) }
            Chip(if (following) "Following" else "Follow", following, onToggle)
        }
    }
}

// ---------- Ask ----------

private data class ChatMessage(val fromUser: Boolean, val text: String)

@Composable
fun AskScreen(onBack: () -> Unit, openPaywall: () -> Unit) {
    val app = LocalApp.current
    val messages = remember { mutableStateListOf(ChatMessage(true, "What does Harbor City need to reach second?"), ChatMessage(false, app.repo.answer("second"))) }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var quota by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(messages.size, thinking) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }

    fun send(q: String) {
        if (q.isBlank() || thinking) return
        if (!app.useAsk()) { quota = true; return }
        messages.add(ChatMessage(true, q)); input = ""; thinking = true
        scope.launch { delay(900); messages.add(ChatMessage(false, app.repo.answer(q))); thinking = false }
    }

    Column(Modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackButton(onBack)
            Column(Modifier.weight(1f)) { Text("Ask Pitchside", style = PcType.Headline); Text("Answers from live match data", style = PcType.Meta.copy(color = Pc.Text2)) }
            Box(Modifier.heightIn(min = 32.dp).clip(PcShape.Chip).background(Pc.Raised).border(1.dp, Pc.Border, PcShape.Chip).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text(if (app.isPro) "Pro" else "${app.askLeft} of ${app.askLimit} free left", style = PcType.Meta.copy(color = Pc.Text))
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
                    val bubble = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (m.fromUser) 20.dp else 6.dp, bottomEnd = if (m.fromUser) 6.dp else 20.dp)
                    Box(
                        Modifier.widthIn(max = 340.dp).clip(bubble).background(if (m.fromUser) Pc.Border else Pc.Surface)
                            .border(if (m.fromUser) 0.dp else 1.dp, Pc.Border, bubble).padding(horizontal = 16.dp, vertical = 12.dp),
                    ) { Text(m.text, style = PcType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp)) }
                }
            }
            item {
                if (thinking) Text("Thinking…", style = PcType.BodySmall, modifier = Modifier.padding(start = 4.dp))
                else FlowChips(listOf("Who scores most for Northgate?", "Redmont form", "Explain the table"), "", { send(it) }, Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    input, { input = it }, singleLine = true, textStyle = PcType.Body, cursorBrush = SolidColor(Pc.Lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.weight(1f).height(52.dp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize().clip(PcShape.Control).background(Pc.Surface).border(1.dp, Pc.Border, PcShape.Control).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                            if (input.isEmpty()) Text("Ask about a match or team", style = PcType.Body.copy(color = Pc.Text2, fontSize = 15.sp), maxLines = 1)
                            inner()
                        }
                    },
                )
                Box(Modifier.size(52.dp).clip(PcShape.Control).background(Pc.Lime).pcClickable(PcShape.Control, "Send", onClick = { send(input) }), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = Pc.OnLime) }
            }
            Text("AI can make mistakes. Numbers come from match data.", style = PcType.Meta.copy(color = Pc.Text2), modifier = Modifier.fillMaxWidth())
        }
    }
    if (quota) PcSheet({ quota = false }) {
        Text("You used today's free questions", style = PcType.Headline)
        Text("They refresh at midnight. Or keep going now.", style = PcType.BodySmall)
        SecondaryButton("Watch a short ad for 1 more", { quota = false }, Modifier.fillMaxWidth())
        PrimaryButton("Go Pro", { quota = false; openPaywall() }, Modifier.fillMaxWidth())
    }
}


// ---------- Paywall ----------

@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val app = LocalApp.current
    var plan by rememberSaveable { mutableStateOf(0) }
    var info by remember { mutableStateOf(false) }
    Screen(maxWidth = 560) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Outlined.Close, "Close", onClose)
            Spacer(Modifier.weight(1f))
            TextAction("Restore", { info = true }, color = Pc.Text2)
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(56.dp).clip(PcShape.Hero).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bolt, null, tint = Pc.OnLime, modifier = Modifier.size(28.dp)) }
            Text("Pitchside Pro", style = PcType.Title.copy(fontSize = 34.sp, lineHeight = 38.sp))
            Text("Less noise, more insight.", style = PcType.BodySmall.copy(fontSize = 16.sp))
            Spacer(Modifier.height(8.dp))
            listOf("No ads anywhere", "More Ask questions every day", "Deeper Match Briefs", "Follow unlimited teams and leagues").forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(14.dp)) }
                    Text(it, style = PcType.Body.copy(fontSize = 15.sp))
                }
            }
            Spacer(Modifier.height(8.dp))
            listOf("Yearly" to "[PRICE] per year, 7-day free trial", "Monthly" to "[PRICE] per month", "Lifetime" to "[PRICE] one time").forEachIndexed { i, (name, sub) ->
                val on = plan == i
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(PcShape.Card).background(if (on) Pc.Raised else Pc.Surface).border(if (on) 1.5.dp else 1.dp, if (on) Pc.Lime else Pc.Border, PcShape.Card)
                        .pcClickable(PcShape.Card, role = androidx.compose.ui.semantics.Role.RadioButton) { plan = i }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name + if (i == 0) " (Best value)" else "", style = PcType.Subhead.copy(fontSize = 16.sp)); Text(sub, style = PcType.BodySmall)
                    }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Pc.Lime else Color.Transparent).border(1.5.dp, if (on) Pc.Lime else Pc.BorderStrong, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(if (plan == 0) "Start free trial" else "Continue", { info = true }, Modifier.fillMaxWidth())
            Text("Then [PRICE] per year. Cancel any time in Google Play.", style = PcType.Meta.copy(color = Pc.Text2), modifier = Modifier.fillMaxWidth())
        }
    }
    if (info) PcDialog("Purchases arrive with billing", "Google Play Billing is connected in the backend phase. Prices are placeholders until then.", "Got it", { info = false }, "Close", { info = false })
}

// ---------- Settings ----------

@Composable
fun SettingsScreen(onBack: () -> Unit, openPaywall: () -> Unit, onReset: () -> Unit) {
    val app = LocalApp.current
    var langSheet by remember { mutableStateOf(false) }
    var alertSheet by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    Screen {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(onBack); Text("Settings", style = PcType.Headline) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PcCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Pc.Border), contentAlignment = Alignment.Center) { Text("A", style = PcType.Label.copy(fontSize = 16.sp)) }
                    Column(Modifier.weight(1f)) { Text("Guest", style = PcType.Label.copy(fontSize = 15.sp)); Text("Link an account to back up your follows", style = PcType.Meta.copy(color = Pc.Text2)) }
                    Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
                }
            }
            if (!app.isPro) PcCard(Modifier.fillMaxWidth(), border = Pc.BorderStrong, onClick = openPaywall) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bolt, null, tint = Pc.OnLime) }
                    Column(Modifier.weight(1f)) { Text("Go Pro", style = PcType.Label.copy(fontSize = 15.sp)); Text("No ads, more Ask questions", style = PcType.Meta.copy(color = Pc.Text2)) }
                    Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
                }
            }
            SettingsGroup("Alerts and spoilers") {
                SettingRow("Default alert level", value = app.alertLevel.label, onClick = { alertSheet = true })
                SettingRow("Quiet hours", "23:00 – 07:00") { PcSwitch(app.quietHours) { app.quietHours = it } }
                SettingRow("Spoiler Shield", "${app.shielded.size} teams shielded") {
                    PcSwitch(app.shielded.isNotEmpty()) { on -> if (on) { app.shielded.clear(); app.shielded.addAll(app.followedTeams) } else app.shielded.clear() }
                }
            }
            SettingsGroup("App") {
                SettingRow("Match Brief detail") { Box(Modifier.width(180.dp)) { SegmentedTabs(listOf("Simple", "Detailed"), if (app.detailed) 1 else 0, { app.detailed = it == 1 }) } }
                SettingRow("Language", value = app.language, onClick = { langSheet = true })
                SettingRow("Theme") { Box(Modifier.width(220.dp)) { SegmentedTabs(listOf("System", "Light", "Dark"), listOf("System", "Light", "Dark").indexOf(app.themeChoice).coerceAtLeast(0), { app.themeChoice = listOf("System", "Light", "Dark")[it] }) } }
            }
            SettingsGroup("About") {
                SettingRow("Privacy Policy"); SettingRow("Terms of Service"); SettingRow("Version", value = "0.1.0")
            }
            SecondaryButton("Reset app data", { reset = true }, Modifier.fillMaxWidth(), danger = true)
        }
    }
    if (langSheet) PcSheet({ langSheet = false }) {
        Text("App language", style = PcType.Headline)
        app.repo.languages().forEach { l ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).pcClickable(PcShape.Control) { app.chooseLanguage(l.native); langSheet = false }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${l.native} (${l.english})", style = PcType.Body.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                if (app.language == l.native) Icon(Icons.Outlined.Check, "Selected", tint = Pc.Lime)
            }
        }
    }
    if (alertSheet) PcSheet({ alertSheet = false }) {
        Text("Default alert level", style = PcType.Headline)
        AlertLevel.entries.forEach { lvl ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).pcClickable(PcShape.Control) { app.alertLevel = lvl; alertSheet = false }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(lvl.label, style = PcType.Label.copy(fontSize = 15.sp)); Text(lvl.desc, style = PcType.Meta.copy(color = Pc.Text2)) }
                if (app.alertLevel == lvl) Icon(Icons.Outlined.Check, "Selected", tint = Pc.Lime)
            }
        }
    }
    if (reset) PcDialog("Reset all app data?", "This removes your follows, alert choices and Spoiler Shield on this device. It cannot be undone.", "Reset", { reset = false; app.resetAll(); onReset() }, "Cancel", { reset = false }, danger = true)
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PcType.Overline.copy(color = Pc.Text2), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        PcCard(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun SettingRow(title: String, value: String? = null, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).let { if (onClick != null) it.pcClickable(PcShape.Control, onClick = onClick) else it }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            if (value != null && trailing != null) Text(value, style = PcType.Meta.copy(color = Pc.Text2))
        }
        if (trailing != null) trailing() else if (value != null) Text(value, style = PcType.BodySmall)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
}
