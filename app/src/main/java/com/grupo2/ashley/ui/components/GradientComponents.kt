package com.grupo2.ashley.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grupo2.ashley.ui.theme.AnimationConstants
import com.grupo2.ashley.ui.theme.AppGradients
import com.grupo2.ashley.ui.theme.RipplePurple

@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = AppGradients.PrimaryGradient,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()


    // Animación de escala al presionar
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationConstants.QuickSpring,
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(
                brush = if (enabled) gradient else Brush.linearGradient(
                    colors = listOf(Color.Gray, Color.DarkGray)
                )
            )
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(
                    color = RipplePurple
                )
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun GradientIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = AppGradients.PrimaryGradient,
    iconTint: Color = Color.White,
    size: Int = 48
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = AnimationConstants.QuickSpring,
        label = "icon_button_scale"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(
                brush = if (enabled) gradient else Brush.linearGradient(
                    colors = listOf(Color.Gray, Color.DarkGray)
                )
            )
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(
                    color = RipplePurple
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush = AppGradients.CardGradientLight,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Int = 2,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = AnimationConstants.FluidSpring,
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        indication = ripple(
                            color = RipplePurple
                        )
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        tonalElevation = elevation.dp,
        shadowElevation = elevation.dp
    ) {
        Box(
            modifier = Modifier.background(gradient)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun GradientTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = AppGradients.PrimaryGradient,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationConstants.QuickSpring,
        label = "gradient_text_button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(gradient)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.3f))
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}



// FAB (Floating Action Button)
@Composable
fun GradientFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    gradient: Brush = AppGradients.PrimaryGradient,
    iconTint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = AnimationConstants.FluidSpring,
        label = "fab_scale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(
                    color = RipplePurple
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
