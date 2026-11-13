package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType

/**
 * Notification centre. Alerts are not delivered to the app yet (push and an alert history need the
 * backend), so this shows the Today / Earlier structure with a designed empty state and a way to the
 * alert settings. See docs/REDESIGN_REPORT.md, "Needs backend support".
 */
@Composable
fun NotificationsScreen(back: () -> Unit, openSettings: () -> Unit) {
    Screen(maxWidth = 640) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Text("Notifications", style = PcType.Subtitle, modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.padding(horizontal = PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Overline("Today")
            EmptyState(
                "No alerts yet",
                "Goals, kickoffs and full-time scores for the teams you follow will collect here, grouped by Today and Earlier. You can choose which moments alert you.",
                actionLabel = "Alert settings", onAction = openSettings, icon = Icons.Outlined.NotificationsNone,
            )
            Overline("Earlier")
            Text("Nothing earlier.", style = PcType.BodyMuted)
        }
    }
}

@Preview(name = "Notifications · empty", showBackground = true, widthDp = 380)
@Composable
private fun NotificationsPreview() = PreviewHost(dark = false) { NotificationsScreen({}, {}) }
