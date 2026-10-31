package com.nova.runtime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.runtime.app.ui.theme.NovaAccentBlue
import com.nova.runtime.app.ui.theme.NovaErrorRed
import com.nova.runtime.app.ui.theme.NovaPureBlack
import com.nova.runtime.app.ui.theme.NovaSecondaryGrey
import com.nova.runtime.app.ui.theme.NovaSurfaceContainerHighest
import com.nova.runtime.app.ui.theme.NovaSurfaceLowest

/** Rounded surface with a quiet divider and consistent inset. */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NovaSurfaceContainerHighest,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = NovaSurfaceLowest,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(24.dp))
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content,
    )
}

enum class NovaButtonVariant {
    PRIMARY,
    PRIMARY_RED,
    SECONDARY,
    OUTLINE,
}

/** Pill button with clipped touch feedback, native semantics, and wrapping labels. */
@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NovaButtonVariant = NovaButtonVariant.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val primary = variant == NovaButtonVariant.PRIMARY || variant == NovaButtonVariant.PRIMARY_RED
    val bgColor = when {
        !enabled -> NovaSurfaceContainerHighest
        primary -> NovaAccentBlue
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = when {
        !enabled -> NovaSecondaryGrey
        primary -> NovaSurfaceLowest
        else -> NovaAccentBlue
    }
    val borderColor = if (variant == NovaButtonVariant.OUTLINE) NovaSurfaceContainerHighest else bgColor

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .background(bgColor, RoundedCornerShape(28.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.sp,
                ),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingIcon != null) Spacer(modifier = Modifier.width(8.dp))
            trailingIcon?.invoke()
        }
    }
}

/** Quiet status pill; text carries the meaning as well as color. */
@Composable
fun NovaStatusChip(
    status: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isError: Boolean = false,
    isSecondary: Boolean = false,
) {
    val bgColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isPrimary -> MaterialTheme.colorScheme.primaryContainer
        else -> NovaSurfaceContainerHighest
    }
    val textColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isPrimary -> NovaAccentBlue
        else -> NovaSecondaryGrey
    }
    val borderColor = bgColor

    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = status.lowercase().replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.05.sp,
            ),
            color = textColor,
        )
    }
}

/** Native switch semantics and a minimum 48dp interaction target. */
@Composable
fun NovaToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        colors = SwitchDefaults.colors(
            checkedThumbColor = NovaSurfaceLowest,
            checkedTrackColor = NovaAccentBlue,
            uncheckedThumbColor = NovaSecondaryGrey,
            uncheckedTrackColor = NovaSurfaceLowest,
            uncheckedBorderColor = NovaSecondaryGrey,
        ),
    )
}

/** Native slider using the shared accent. */
@Composable
fun NovaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = NovaAccentBlue,
            activeTrackColor = NovaAccentBlue,
            inactiveTrackColor = NovaSurfaceContainerHighest,
        ),
        modifier = modifier,
    )
}
