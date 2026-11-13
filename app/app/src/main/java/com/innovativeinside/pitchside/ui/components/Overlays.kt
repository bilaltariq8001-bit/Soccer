package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType

/** Centered confirm dialog from the dialog kit. */
@Composable
fun PcDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    icon: ImageVector? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(PcShape.Hero).background(Pc.Surface).border(1.dp, Pc.OutlineStrong, PcShape.Hero).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Pc.Accent) }
            Text(title, style = PcType.Subtitle)
            Text(body, style = PcType.BodyMuted)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().heightIn48().clip(PcShape.Control).background(if (danger) Pc.Error else Pc.Accent).pcClickable(PcShape.Control, onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) { Text(confirmLabel, color = Pc.OnAccent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
            TextAction(dismissLabel, onDismiss, Modifier.fillMaxWidth(), color = Pc.Text2)
        }
    }
}

private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(52.dp))

/** Bottom sheet built on a dialog so it works without experimental Material APIs. */
@Composable
fun PcSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val none = remember { MutableInteractionSource() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0x99000000)).clickable(interactionSource = none, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Pc.Surface)
                    .border(1.dp, Pc.OutlineStrong, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Pc.OutlineStrong))
                content()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, autoFocus: Boolean = false) {
    val focus = remember { FocusRequester() }
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = PcType.Body, cursorBrush = SolidColor(Pc.Accent),
        modifier = modifier.fillMaxWidth().height(52.dp).focusRequester(focus),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(PcShape.Control).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Control).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, null, tint = Pc.Text2, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = PcType.Body, color = Pc.Text2)
                    inner()
                }
                if (value.isNotEmpty()) Icon(
                    Icons.Outlined.Close, "Clear search", tint = Pc.Text2,
                    modifier = Modifier.size(24.dp).pcClickable(PcShape.Chip, "Clear search") { onValueChange("") },
                )
            }
        },
    )
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
}
