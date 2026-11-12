package com.innovativeinside.pitchside.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.BuildConfig
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.Languages
import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.ThemeChoice
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.FlowChips
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.PcSheet
import com.innovativeinside.pitchside.ui.components.PcSwitch
import com.innovativeinside.pitchside.ui.components.PcTextField
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SecondaryButton
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.AskState
import com.innovativeinside.pitchside.ui.vm.AskViewModel
import com.innovativeinside.pitchside.ui.vm.AuthForm
import com.innovativeinside.pitchside.ui.vm.AuthMode
import com.innovativeinside.pitchside.ui.vm.AuthViewModel

// ---------------- Ask ----------------

@Composable
fun AskRoute(back: () -> Unit, openPaywall: () -> Unit, vm: AskViewModel = viewModel(factory = AskViewModel.Factory)) {
    val s by vm.state.collectAsStateWithLifecycle()
    AskContent(s, back, vm::onInput, vm::send, vm::retryLast)
    if (s.quotaHit) PcSheet(vm::dismissQuota) {
        Text("You've used today's free questions", style = PcType.Headline)
        Text("They refresh at midnight. Pro gives you ${com.innovativeinside.pitchside.data.prefs.UserPrefs.PRO_ASK_LIMIT} a day.", style = PcType.BodySmall)
        PrimaryButton("See Pro", { vm.dismissQuota(); openPaywall() }, Modifier.fillMaxWidth())
        TextAction("Maybe later", vm::dismissQuota, Modifier.fillMaxWidth(), color = Pc.Text2)
    }
}

@Composable
fun AskContent(s: AskState, back: () -> Unit, onInput: (String) -> Unit, onSend: (String) -> Unit, onRetry: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(s.messages.size, s.sending) { if (s.messages.isNotEmpty()) listState.animateScrollToItem(s.messages.size) }
    Column(Modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackButton(back)
            Column(Modifier.weight(1f)) { Text(stringResource(R.string.action_ask), style = PcType.Headline); Text("Answers from match data", style = PcType.Meta.copy(color = Pc.Text2)) }
            Box(Modifier.heightIn(min = 32.dp).clip(PcShape.Chip).background(Pc.Raised).border(1.dp, Pc.Border, PcShape.Chip).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("${s.left} of ${s.limit} left today", style = PcType.Meta.copy(color = Pc.Text))
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.messages.isEmpty()) item {
                PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ask about any match, team or table", style = PcType.Subhead)
                        Text("Numbers come from match data, never guesses. Try one of these:", style = PcType.BodySmall)
                    }
                }
            }
            items(s.messages) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
                    val bubble = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (m.fromUser) 20.dp else 6.dp, bottomEnd = if (m.fromUser) 6.dp else 20.dp)
                    Column(
                        Modifier.widthIn(max = 340.dp).clip(bubble).background(if (m.fromUser) Pc.Raised else Pc.Surface)
                            .border(1.dp, if (m.failed) Pc.Red else Pc.Border, bubble).padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(m.text, style = PcType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp, color = if (m.failed) Pc.Red else Pc.Text))
                        if (m.failed) TextAction(stringResource(R.string.action_retry), onRetry)
                    }
                }
            }
            item {
                if (s.sending) Text("Thinking…", style = PcType.BodySmall, modifier = Modifier.padding(start = 4.dp).testTag("ask_thinking"))
                else FlowChips(listOf("Who leads the table?", "Redmont form", "Who scores most for Northgate?"), "", { onSend(it) }, Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    s.input, onInput, singleLine = true, textStyle = PcType.Body, cursorBrush = SolidColor(Pc.Lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend(s.input) }),
                    modifier = Modifier.weight(1f).height(52.dp).testTag("ask_input"),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize().clip(PcShape.Control).background(Pc.Surface).border(1.dp, if (s.inputError != null) Pc.Red else Pc.Border, PcShape.Control).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                            if (s.input.isEmpty()) Text("Ask about a match or team", style = PcType.Body.copy(color = Pc.Text2, fontSize = 15.sp), maxLines = 1)
                            inner()
                        }
                    },
                )
                Box(Modifier.size(52.dp).clip(PcShape.Control).background(if (s.sending) Pc.Raised else Pc.Lime).pcClickable(PcShape.Control, "Send") { onSend(s.input) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, tint = if (s.sending) Pc.Text3 else Pc.OnLime)
                }
            }
            if (s.inputError != null) Text(s.inputError, style = PcType.Meta, color = Pc.Red)
            Text("AI-generated from match data. It can make mistakes.", style = PcType.Meta.copy(color = Pc.Text2))
        }
    }
}

// ---------------- Paywall ----------------

@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var plan by rememberSaveable { mutableIntStateOf(0) }
    var info by remember { mutableStateOf(false) }
    Screen(maxWidth = 560) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Icons.Outlined.Close, "Close", onClose)
            Spacer(Modifier.weight(1f))
            TextAction("Restore purchase", { info = true }, color = Pc.Text2)
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(56.dp).clip(PcShape.Hero).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bolt, null, tint = Pc.OnLime, modifier = Modifier.size(28.dp)) }
            Text("Pitchside Pro", style = PcType.Title.copy(fontSize = 34.sp, lineHeight = 38.sp))
            Text(if (prefs.isPro) "Pro is active on this device." else "Less noise, more insight.", style = PcType.BodySmall.copy(fontSize = 16.sp))
            Spacer(Modifier.height(8.dp))
            listOf("No ads anywhere", "${com.innovativeinside.pitchside.data.prefs.UserPrefs.PRO_ASK_LIMIT} Ask questions a day", "Deeper Match Briefs", "Follow unlimited teams").forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 32.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(14.dp)) }
                    Text(it, style = PcType.Body.copy(fontSize = 15.sp))
                }
            }
            Spacer(Modifier.height(8.dp))
            listOf("Yearly" to "7-day free trial, then billed yearly", "Monthly" to "Billed monthly, cancel any time", "Lifetime" to "Pay once").forEachIndexed { i, (name, sub) ->
                val on = plan == i
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(PcShape.Card).background(if (on) Pc.Raised else Pc.Surface).border(if (on) 1.5.dp else 1.dp, if (on) Pc.Lime else Pc.Border, PcShape.Card)
                        .pcClickable(PcShape.Card, role = Role.RadioButton) { plan = i }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) { Text(name + if (i == 0) "  ·  Best value" else "", style = PcType.Subhead.copy(fontSize = 16.sp)); Text(sub, style = PcType.BodySmall.copy(fontSize = 13.sp)) }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Pc.Lime else Color.Transparent).border(1.5.dp, if (on) Pc.Lime else Pc.BorderStrong, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Outlined.Check, null, tint = Pc.OnLime, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(if (plan == 0) "Start free trial" else "Continue", { info = true }, Modifier.fillMaxWidth())
            Text("Prices show from Google Play once billing is connected. Cancel any time in Google Play.", style = PcType.Meta.copy(color = Pc.Text2))
        }
    }
    if (info) PcDialog(
        "Billing isn't connected yet",
        "Google Play Billing ships with the store release. Nothing was charged." + if (BuildConfig.DEBUG) " In this test build you can turn on a Pro preview." else "",
        if (BuildConfig.DEBUG) (if (prefs.isPro) "Turn off Pro preview" else "Turn on Pro preview") else "OK",
        { info = false; if (BuildConfig.DEBUG) app.setPro(!prefs.isPro) },
        "Close", { info = false },
    )
}

// ---------------- Settings ----------------

class SettingsNav(val back: () -> Unit, val openPaywall: () -> Unit, val openAuth: () -> Unit, val openAccount: () -> Unit, val afterReset: () -> Unit)

@Composable
fun SettingsScreen(nav: SettingsNav) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val session by app.session.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var langSheet by remember { mutableStateOf(false) }
    var alertSheet by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    Screen {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(nav.back); Text("Settings", style = PcType.Headline) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val s = session
            PcCard(Modifier.fillMaxWidth(), onClick = if (s != null) nav.openAccount else nav.openAuth) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Pc.Raised), contentAlignment = Alignment.Center) { Text((s?.displayName?.firstOrNull() ?: 'G').uppercase(), style = PcType.Label.copy(fontSize = 16.sp)) }
                    Column(Modifier.weight(1f)) {
                        Text(s?.displayName ?: "Guest", style = PcType.Label.copy(fontSize = 15.sp))
                        Text(s?.email ?: "Sign in to back up your follows", style = PcType.Meta.copy(color = Pc.Text2))
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
                }
            }
            if (!prefs.isPro) PcCard(Modifier.fillMaxWidth(), border = Pc.BorderStrong, onClick = nav.openPaywall) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bolt, null, tint = Pc.OnLime) }
                    Column(Modifier.weight(1f)) { Text("Go Pro", style = PcType.Label.copy(fontSize = 15.sp)); Text("No ads, more Ask questions, unlimited follows", style = PcType.Meta.copy(color = Pc.Text2)) }
                    Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
                }
            }
            SettingsGroup("ALERTS AND SPOILERS") {
                SettingRow("Default alert level", value = prefs.alertLevel.label, onClick = { alertSheet = true })
                SettingRow("Quiet hours", "23:00 – 07:00") { PcSwitch(prefs.quietHours, app::setQuietHours) }
                SettingRow("Spoiler Shield", "${prefs.shielded.size} of ${prefs.teams.size} teams shielded") { PcSwitch(prefs.shielded.isNotEmpty(), app::setShieldAll) }
            }
            SettingsGroup("APP") {
                SettingRow("Match Brief detail") { Box(Modifier.width(180.dp)) { SegmentedTabs(listOf("Simple", "Detailed"), if (prefs.detailed) 1 else 0, { app.setDetailed(it == 1) }) } }
                SettingRow("Language", value = Languages.byCode(prefs.language).native, onClick = { langSheet = true })
                SettingRow("Theme") { Box(Modifier.width(220.dp)) { SegmentedTabs(ThemeChoice.entries.map { it.label }, prefs.theme.ordinal, { app.setTheme(ThemeChoice.entries[it]) }) } }
            }
            val mock = app.mockBehavior
            if (mock != null) SettingsGroup("DEVELOPER · MOCK DATA") {
                var offline by remember { mutableStateOf(mock.offline) }
                var errors by remember { mutableStateOf(mock.serverErrors) }
                SettingRow("Simulate offline", "Every request fails with a network error") { PcSwitch(offline) { offline = it; mock.offline = it } }
                SettingRow("Simulate server errors", "Every request fails with HTTP 503") { PcSwitch(errors) { errors = it; mock.serverErrors = it } }
                if (BuildConfig.DEBUG) SettingRow("Pro preview", "Unlock Pro features on this device") { PcSwitch(prefs.isPro, app::setPro) }
            }
            SettingsGroup("ABOUT") {
                SettingRow("Privacy", onClick = { privacy = true })
                SettingRow("Share Pitchside", onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Pitchside: live scores with a short brief on every match. https://play.google.com/store/apps/details?id=${ctx.packageName}")
                    ctx.startActivity(Intent.createChooser(send, "Share Pitchside"))
                })
                SettingRow("Rate on Google Play", onClick = {
                    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
                    catch (_: ActivityNotFoundException) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${ctx.packageName}"))) }
                })
                SettingRow("Version", value = BuildConfig.VERSION_NAME + if (app.usesMockData) " · mock data" else "")
            }
            SecondaryButton("Reset app data", { reset = true }, Modifier.fillMaxWidth(), danger = true)
        }
    }
    if (langSheet) PcSheet({ langSheet = false }) {
        Text("App language", style = PcType.Headline)
        Text("Match Briefs and Ask answers use this language. Screens are in English for now.", style = PcType.Meta.copy(color = Pc.Text2))
        Languages.all.forEach { l ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).pcClickable(PcShape.Control) { app.setLanguage(l.code); langSheet = false }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${l.native} · ${l.english}", style = PcType.Body.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                if (prefs.language == l.code) Icon(Icons.Outlined.Check, "Selected", tint = Pc.Lime)
            }
        }
    }
    if (alertSheet) PcSheet({ alertSheet = false }) {
        Text("Default alert level", style = PcType.Headline)
        AlertLevel.entries.forEach { lvl ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).pcClickable(PcShape.Control) { app.setAlertLevel(lvl); alertSheet = false }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(lvl.label, style = PcType.Label.copy(fontSize = 15.sp)); Text(lvl.desc, style = PcType.Meta.copy(color = Pc.Text2)) }
                if (prefs.alertLevel == lvl) Icon(Icons.Outlined.Check, "Selected", tint = Pc.Lime)
            }
        }
    }
    if (privacy) PcDialog(
        "Your data", "Follows, alerts and Spoiler Shield are stored on this device. Account sign-in keeps an encrypted session token. No ad tracking, no selling data.",
        "Got it", { privacy = false }, "Close", { privacy = false },
    )
    if (reset) PcDialog("Reset all app data?", "This removes your follows, alert choices and Spoiler Shield on this device and signs you out. It can't be undone.", "Reset", { reset = false; app.resetAll(); nav.afterReset() }, stringResource(R.string.action_cancel), { reset = false }, danger = true)
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PcType.Overline.copy(color = Pc.Text3), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
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
        when {
            trailing != null -> trailing()
            value != null -> Text(value, style = PcType.BodySmall)
            onClick != null -> Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
}

// ---------------- Auth ----------------

@Composable
fun AuthRoute(mode: AuthMode, back: () -> Unit, onDone: () -> Unit, vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(mode))) {
    val f by vm.form.collectAsStateWithLifecycle()
    LaunchedEffect(f.done) { if (f.done) onDone() }
    AuthContent(f, back, vm::setMode, vm::onName, vm::onEmail, vm::onPassword, vm::onConfirm, vm::submit)
}

@Composable
fun AuthContent(
    f: AuthForm,
    back: () -> Unit,
    onMode: (AuthMode) -> Unit,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Screen(maxWidth = 520) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                when (f.mode) { AuthMode.SignIn -> stringResource(R.string.auth_sign_in); AuthMode.SignUp -> stringResource(R.string.auth_sign_up); AuthMode.Reset -> stringResource(R.string.auth_reset) },
                style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp),
            )
            Text(if (f.mode == AuthMode.Reset) "We'll email you a link to choose a new password." else stringResource(R.string.auth_why), style = PcType.BodySmall)
            if (f.formError != null) InfoBanner(f.formError, BannerKind.Error, Modifier.testTag("auth_form_error"))
            if (f.resetSent) {
                PcCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MarkEmailRead, null, tint = Pc.Lime)
                        Text(stringResource(R.string.auth_reset_sent), style = PcType.BodySmall.copy(color = Pc.Text), modifier = Modifier.testTag("auth_reset_sent"))
                    }
                }
                PrimaryButton("Back to sign in", { onMode(AuthMode.SignIn) }, Modifier.fillMaxWidth())
                return@Column
            }
            if (f.mode == AuthMode.SignUp) PcTextField(stringResource(R.string.auth_name), f.name, onName, error = f.nameError, placeholder = "Your name", tag = "name", enabled = !f.submitting)
            PcTextField(stringResource(R.string.auth_email), f.email, onEmail, error = f.emailError, placeholder = "name@example.com", keyboardType = KeyboardType.Email, tag = "email", enabled = !f.submitting,
                imeAction = if (f.mode == AuthMode.Reset) ImeAction.Done else ImeAction.Next, onIme = { if (f.mode == AuthMode.Reset) onSubmit() })
            if (f.mode != AuthMode.Reset) PcTextField(stringResource(R.string.auth_password), f.password, onPassword, error = f.passwordError, password = true, tag = "password", enabled = !f.submitting,
                placeholder = if (f.mode == AuthMode.SignUp) "8+ characters with a number" else "", imeAction = if (f.mode == AuthMode.SignUp) ImeAction.Next else ImeAction.Done, onIme = { if (f.mode == AuthMode.SignIn) onSubmit() })
            if (f.mode == AuthMode.SignUp) PcTextField(stringResource(R.string.auth_confirm), f.confirm, onConfirm, error = f.confirmError, password = true, tag = "confirm", enabled = !f.submitting, imeAction = ImeAction.Done, onIme = onSubmit)
            PrimaryButton(
                when (f.mode) { AuthMode.SignIn -> stringResource(R.string.auth_sign_in); AuthMode.SignUp -> stringResource(R.string.auth_sign_up); AuthMode.Reset -> "Send reset link" },
                onSubmit, Modifier.fillMaxWidth().testTag("auth_submit"), loading = f.submitting,
            )
            when (f.mode) {
                AuthMode.SignIn -> {
                    TextAction(stringResource(R.string.auth_forgot), { onMode(AuthMode.Reset) }, Modifier.fillMaxWidth(), color = Pc.Text2)
                    TextAction(stringResource(R.string.auth_no_account), { onMode(AuthMode.SignUp) }, Modifier.fillMaxWidth())
                }
                AuthMode.SignUp -> TextAction(stringResource(R.string.auth_have_account), { onMode(AuthMode.SignIn) }, Modifier.fillMaxWidth())
                AuthMode.Reset -> TextAction("Back to sign in", { onMode(AuthMode.SignIn) }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Protected route: only reachable with a session. Without one, it redirects to sign in. */
@Composable
fun AccountRoute(back: () -> Unit, requireSignIn: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val session by app.session.collectAsStateWithLifecycle()
    var signOut by remember { mutableStateOf(false) }
    val s = session
    LaunchedEffect(s) { if (s == null) requireSignIn() }
    if (s == null) return
    Screen(maxWidth = 560) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(back); Text("Account", style = PcType.Headline) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.displayName, style = PcType.Headline)
                    Text(s.email, style = PcType.BodySmall)
                }
            }
            PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backed up with this account", style = PcType.Subhead)
                    Text("${prefs.teams.size} teams, ${prefs.leagues.size} leagues, ${prefs.players.size} players, alert levels and Spoiler Shield.", style = PcType.BodySmall)
                }
            }
            SecondaryButton(stringResource(R.string.auth_sign_out), { signOut = true }, Modifier.fillMaxWidth(), danger = true)
        }
    }
    if (signOut) PcDialog("Sign out?", "Your follows stay on this device. Sign in again any time to sync them.", stringResource(R.string.auth_sign_out), { signOut = false; app.signOut() }, stringResource(R.string.action_cancel), { signOut = false }, danger = true)
}

@Preview(name = "Auth · sign in errors", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun AuthPreview() = PreviewHost {
    AuthContent(AuthForm(email = "bad", emailError = "Enter a valid email, like name@example.com.", passwordError = "Enter your password."), {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Ask · empty", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun AskPreview() = PreviewHost { AskContent(AskState(left = 5, limit = 5), {}, {}, {}, {}) }
