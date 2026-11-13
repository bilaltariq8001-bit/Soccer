package com.innovativeinside.pitchside.ui.components

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
            Modifier.fillMaxWidth().clip(PcShape.Hero).background(Pc.Surface).border(1.dp, Pc.BorderStrong, PcShape.Hero).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Pc.Lime) }
            Text(title, style = PcType.Headline)
            Text(body, style = PcType.BodySmall)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().heightIn48().clip(PcShape.Control).background(if (danger) Pc.Red else Pc.Lime).pcClickable(PcShape.Control, onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) { Text(confirmLabel, color = Pc.OnLime, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
            TextAction(dismissLabel, onDismiss, Modifier.fillMaxWidth(), color = Pc.Text2)
        }
    }
}

private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(52.dp))

/** Branded Material 3 sheet with platform dismissal and drag behavior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Pc.Surface,
        contentColor = Pc.Text,
        dragHandle = {
            Box(Modifier.padding(top = 8.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Pc.BorderStrong))
        },
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, autoFocus: Boolean = false) {
    val focus = remember { FocusRequester() }
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = PcType.Body, cursorBrush = SolidColor(Pc.Lime),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).focusRequester(focus).semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(PcShape.Control).background(Pc.Surface).border(1.dp, Pc.Border, PcShape.Control).padding(horizontal = 16.dp),
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
