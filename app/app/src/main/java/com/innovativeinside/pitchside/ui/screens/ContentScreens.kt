package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.LocalApp
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FormChip
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PhotoPlaceholder
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType

// ---------- League ----------

@Composable
fun LeagueScreen(openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var followed by rememberSaveable { mutableStateOf(true) }
    val rows = app.repo.standings("pd")
    Screen {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.Surface).border(1.dp, Pc.Border, PcShape.Control), contentAlignment = Alignment.Center) { Text("PD", color = Pc.Lime, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.weight(1f)) {
                Text("Premier Division", style = PcType.Title.copy(fontSize = 24.sp, lineHeight = 28.sp))
                Text("Season 2026/27 / Round 9 of 34", style = PcType.BodySmall)
            }
            IconAction(if (followed) Icons.Filled.Star else Icons.Outlined.StarBorder, if (followed) "Following league" else "Follow league", { followed = !followed }, tint = if (followed) Pc.Lime else Pc.Text2, filled = true)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedTabs(listOf("Table", "Form", "Scorers"), tab, { tab = it })
            when (tab) {
                0 -> {
                    PcCard(Modifier.fillMaxWidth()) {
                        Column {
                            Row(Modifier.fillMaxWidth().height(40.dp).background(Pc.Nav).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("#", style = PcType.Meta, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                Text("Club", style = PcType.Meta, modifier = Modifier.weight(1f))
                                Text("P", style = PcType.Meta, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                Text("W-D-L", style = PcType.Meta, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                                Text("GD", style = PcType.Meta, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                Text("Pts", style = PcType.Meta.copy(color = Pc.Text), modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                            rows.forEachIndexed { i, r ->
                                val fav = r.team.id in app.followedTeams
                                val zone = when { i < 3 -> Pc.Text; i == rows.lastIndex -> Pc.Red; else -> Color.Transparent }
                                Box(Modifier.fillMaxWidth().height(52.dp).background(if (fav) Pc.LimeTintSoft else Color.Transparent)) {
                                    Box(Modifier.width(4.dp).fillMaxWidth(0f).height(52.dp))
                                    Box(Modifier.width(4.dp).height(52.dp).background(zone))
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(r.rank.toString(), style = PcType.Label.copy(fontSize = 14.sp), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                        Row(Modifier.weight(1f).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Crest(r.team, 24.dp)
                                            Text(r.team.name, style = PcType.Body.copy(fontSize = 15.sp, fontWeight = if (fav) FontWeight.ExtraBold else FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                            if (fav) Icon(Icons.Filled.Star, "Your club", tint = Pc.Lime, modifier = Modifier.size(14.dp))
                                        }
                                        Text(r.played.toString(), style = PcType.BodySmall, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        Text("${r.won}-${r.drawn}-${r.lost}", style = PcType.BodySmall, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                                        Text(if (r.goalDiff > 0) "+${r.goalDiff}" else r.goalDiff.toString(), style = PcType.BodySmall.copy(color = if (r.goalDiff < 0) Pc.Red else Pc.Text2), modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                        Text(r.points.toString(), style = PcType.ScoreM, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Legend(Pc.Text, "Continental places"); Legend(Pc.Red, "Relegation zone"); Legend(Pc.Lime, "Your club")
                    }
                }
                1 -> PcCard(Modifier.fillMaxWidth()) {
                    Column {
                        rows.forEach { r ->
                            val fav = r.team.id in app.followedTeams
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(if (fav) Pc.LimeTintSoft else Color.Transparent).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(r.rank.toString(), style = PcType.Label.copy(color = Pc.Text2), modifier = Modifier.width(20.dp))
                                Text(r.team.name, style = PcType.Body.copy(fontSize = 15.sp, fontWeight = if (fav) FontWeight.ExtraBold else FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { r.form.forEach { FormChip(it) } }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                        }
                    }
                }.also { Text("Last five matches, oldest on the left. W win, D draw, L loss.", style = PcType.Meta) }
                else -> app.repo.scorers("pd").forEach { s ->
                    PcCard(Modifier.fillMaxWidth(), onClick = { openPlayer("p9") }) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(s.rank.toString(), style = PcType.Label.copy(color = Pc.Text2), modifier = Modifier.width(20.dp))
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(s.team.color)), contentAlignment = Alignment.Center) { Text(s.name.split(" ").map { it.first() }.joinToString(""), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                            Column(Modifier.weight(1f)) { Text(s.name, style = PcType.Label.copy(fontSize = 15.sp)); Text(s.team.name, style = PcType.BodySmall) }
                            Text(s.goals.toString(), style = PcType.ScoreL.copy(fontSize = 32.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(color))
        Text(label, style = PcType.Meta.copy(color = Pc.Text2))
    }
}

// ---------- News ----------

@Composable
fun NewsScreen() {
    val app = LocalApp.current
    var cat by rememberSaveable { mutableStateOf("All") }
    val items = app.repo.news().filter { cat == "All" || it.category == cat }
    Screen {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("News", style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
            Text("Reports, transfers and analysis", style = PcType.BodySmall)
        }
        Spacer(Modifier.height(16.dp))
        ChipRow(listOf("All", "Match reports", "Transfers", "Analysis"), cat, { cat = it })
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                EmptyState("No stories yet", "New stories in this section appear here as soon as they are published.", actionLabel = "Show all news", onAction = { cat = "All" })
            } else {
                val f = items.first()
                PcCard(Modifier.fillMaxWidth(), shape = PcShape.Hero, onClick = {}) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Overline(f.category)
                            Text(f.title, style = PcType.Title.copy(fontSize = 26.sp, lineHeight = 32.sp))
                            Text(f.dek, style = PcType.BodySmall)
                            Text(f.meta, style = PcType.Meta)
                    }
                }
                Spacer(Modifier.height(4.dp))
                items.drop(1).forEach { NewsRow(it, {}) }
            }
        }
    }
}

// ---------- Player ----------

@Composable
fun PlayerScreen(playerId: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val p = app.repo.player(playerId)
    var tab by rememberSaveable { mutableStateOf(0) }
    val following = p.id in app.followedPlayers
    Screen {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            IconAction(if (following) Icons.Filled.Star else Icons.Outlined.StarBorder, if (following) "Unfollow ${p.name}" else "Follow ${p.name}", {
                if (following) app.followedPlayers.remove(p.id) else app.followedPlayers.add(p.id)
            }, tint = if (following) Pc.Lime else Pc.Text)
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(80.dp).clip(PcShape.Hero).background(Color(p.team.color)), contentAlignment = Alignment.Center) { Text(p.initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(p.name, style = PcType.Title)
                Text("${p.position} / ${p.team.name} / #${p.number}", style = PcType.BodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Age ${p.age}", "${p.heightCm} cm", "${p.foot} foot").forEach { Tag(it) } }
            }
        }
        Spacer(Modifier.height(8.dp))
        UnderlineTabs(listOf("Overview", "Stats", "Career"), tab, { tab = it })
        Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (tab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(p.goals.toString(), "Goals", Modifier.weight(1f)); StatTile(p.assists.toString(), "Assists", Modifier.weight(1f))
                        StatTile(p.rating.toString(), "Rating", Modifier.weight(1f), Pc.Lime); StatTile(p.matches.toString(), "Matches", Modifier.weight(1f))
                    }
                    BriefCard(p.scoutingNote, title = "Scouting note", updated = "", compactMeta = false)
                    PcCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Last 5 ratings", style = PcType.Subhead)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                p.lastRatings.forEach { r ->
                                    val bg = when { r >= 8.0 -> Pc.Lime; r >= 7.0 -> Pc.Amber; else -> Pc.Red }
                                    Box(Modifier.weight(1f).height(44.dp).clip(PcShape.Chip).background(bg), contentAlignment = Alignment.Center) { Text(r.toString(), color = Pc.OnLime, style = PcType.ScoreS) }
                                }
                            }
                        }
                    }
                }
                1 -> PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Per 90 minutes", style = PcType.Subhead)
                        PerNinety("Shots", p.shotsPer90, 4.0, Pc.Lime); PerNinety("Key passes", p.keyPassesPer90, 4.0, Pc.Live)
                        PerNinety("Goals", p.goals * 90.0 / (p.matches * 90), 1.5, Pc.Lime)
                    }
                }
                else -> PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        p.career.forEachIndexed { i, c ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(if (c.current) Pc.Lime else Pc.BorderStrong))
                                Column(Modifier.weight(1f)) { Text(c.club, style = PcType.Label.copy(fontSize = 15.sp)); Text(c.years, style = PcType.BodySmall) }
                                Column(horizontalAlignment = Alignment.End) { Text("${c.apps} apps", style = PcType.BodySmall); Text("${c.goals} goals", style = PcType.Label.copy(color = Pc.Lime)) }
                            }
                            if (i < p.career.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.heightIn(min = 26.dp).clip(PcShape.Chip).background(Pc.Surface).border(1.dp, Pc.Border, PcShape.Chip).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(text, style = PcType.Meta.copy(color = Pc.Text)) }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, color: Color = Pc.Text) {
    PcCard(modifier) { Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = PcType.ScoreL.copy(fontSize = 30.sp, color = color)); Text(label, style = PcType.Meta) } }
}

@Composable
private fun PerNinety(label: String, value: Double, max: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = PcType.BodySmall, modifier = Modifier.width(88.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Pc.Border)) { Box(Modifier.fillMaxWidth((value / max).coerceIn(0.03, 1.0).toFloat()).height(8.dp).clip(CircleShape).background(color)) }
        Text(String.format("%.1f", value), style = PcType.Label, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}
