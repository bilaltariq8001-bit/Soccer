package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.data.Match
import com.innovativeinside.pitchside.data.NewsItem
import com.innovativeinside.pitchside.data.StandingRow
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PhotoPlaceholder
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcType
import java.util.Calendar

enum class WidthClass { Compact, Medium, Expanded }

@Composable
fun widthClass(): WidthClass {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> WidthClass.Expanded
        w >= 600 -> WidthClass.Medium
        else -> WidthClass.Compact
    }
}

/** Page container: status bar inset, background, optional scroll and a readable max width on big screens. */
@Composable
fun Screen(modifier: Modifier = Modifier, scroll: Boolean = true, maxWidth: Int = 720, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        val col = Modifier.widthIn(max = maxWidth.dp).fillMaxSize()
        Column(if (scroll) col.verticalScroll(rememberScrollState()) else col) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Good morning"; h < 18 -> "Good afternoon"; else -> "Good evening" }
}

@Composable
fun NewsRow(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline(item.category)
                Text(item.title, style = PcType.Subhead.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold))
                Text(item.meta, style = PcType.Meta)
        }
    }
}

@Composable
fun StandingsPreview(rows: List<StandingRow>, followedIds: Set<String>, onClick: () -> Unit, count: Int = 3) {
    PcCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            rows.take(count).forEachIndexed { i, r ->
                val fav = r.team.id in followedIds
                Row(
                    Modifier.fillMaxWidth().height(48.dp).background(if (fav) Pc.LimeTintSoft else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(r.rank.toString(), style = PcType.Label.copy(fontSize = 14.sp), color = if (fav) Pc.Text else Pc.Text2, modifier = Modifier.widthIn(min = 20.dp))
                    Crest(r.team, 24.dp)
                    Text(r.team.name, style = PcType.Body.copy(fontSize = 15.sp, fontWeight = if (fav) FontWeight.ExtraBold else FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    if (fav) Text("Your club", style = PcType.Meta.copy(color = Pc.Lime, fontWeight = FontWeight.SemiBold))
                    Text(r.points.toString(), style = PcType.ScoreS)
                }
                if (i < count - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
            }
        }
    }
}

fun Match.isFollowed(followed: Collection<String>) = home.id in followed || away.id in followed
