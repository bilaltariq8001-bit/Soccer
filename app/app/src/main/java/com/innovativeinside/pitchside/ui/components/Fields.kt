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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType

/** The app's text field: label above, 52dp control, inline error below announced to TalkBack. */
@Composable
fun PcTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onIme: () -> Unit = {},
    password: Boolean = false,
    enabled: Boolean = true,
    tag: String = label,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = PcType.Label)
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true, enabled = enabled,
            textStyle = PcType.Body, cursorBrush = SolidColor(Pc.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onIme() }),
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("field_$tag").semantics {
                contentDescription = label
                if (error != null) error(error)
            },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxSize().clip(PcShape.Control).background(Pc.Surface)
                        .border(if (error != null) 1.5.dp else 1.dp, if (error != null) Pc.Error else Pc.Outline, PcShape.Control)
                        .padding(start = 16.dp, end = if (password) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = PcType.Body, color = Pc.Text3)
                        inner()
                    }
                    if (password) IconAction(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (visible) "Hide password" else "Show password", { visible = !visible }, tint = Pc.Text2,
                    )
                }
            },
        )
        if (error != null) Text(error, style = PcType.Caption, color = Pc.Error, modifier = Modifier.testTag("error_$tag"))
    }
}

/** Standard top bar for pushed screens: back, title/subtitle, optional trailing actions. */
@Composable
fun PcTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) BackButton(onBack) else Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, style = PcType.Subtitle, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = PcType.Caption.copy(color = Pc.Text2), maxLines = 1)
        }
        actions()
    }
}
