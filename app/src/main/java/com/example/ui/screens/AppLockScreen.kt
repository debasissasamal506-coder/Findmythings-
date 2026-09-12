package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppLockScreen(
    preferencesRepository: PreferencesRepository,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Smooth whole-screen transition when unlocking
    val screenAlpha by animateFloatAsState(
        targetValue = if (isSuccess) 0.0f else 1.0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 220, easing = FastOutSlowInEasing),
        label = "screen_alpha"
    )
    val screenScale by animateFloatAsState(
        targetValue = if (isSuccess) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        label = "screen_scale"
    )

    // Lock badge bounce & unlock glow animation
    val lockBadgeScale by animateFloatAsState(
        targetValue = if (isSuccess) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lock_badge_scale"
    )

    // Keypad soft fade out on unlock
    val keypadAlpha by animateFloatAsState(
        targetValue = if (isSuccess) 0.0f else 1.0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "keypad_alpha"
    )
    val keypadOffsetY by animateFloatAsState(
        targetValue = if (isSuccess) 30f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "keypad_offset_y"
    )

    fun onDigitClick(digit: String) {
        if (isSuccess) return // Ignore touch during unlock transition
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            isError = false
            if (newPin.length == 4) {
                val isValid = preferencesRepository.verifyPin(newPin)
                if (isValid) {
                    isSuccess = true
                    coroutineScope.launch {
                        // Choreographed unlock delay for smooth transition into the app
                        delay(460)
                        onUnlocked()
                    }
                } else {
                    isError = true
                    coroutineScope.launch {
                        // Subtle horizontal shake
                        shakeOffset.animateTo(12f, animationSpec = tween(40))
                        shakeOffset.animateTo(-10f, animationSpec = tween(40))
                        shakeOffset.animateTo(8f, animationSpec = tween(40))
                        shakeOffset.animateTo(-6f, animationSpec = tween(40))
                        shakeOffset.animateTo(0f, animationSpec = tween(40))
                        delay(250)
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (isSuccess) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    // Deep dark navy/black canvas with soft ambient glow blobs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060913),
                        Color(0xFF0C1322),
                        Color(0xFF050811)
                    )
                )
            )
            .drawBehind {
                // Top-left subtle indigo ambient blob
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.22f),
                            Color(0xFF4338CA).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(x = size.width * 0.15f, y = size.height * 0.12f)
                )
                // Mid-right subtle electric blue ambient blob
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0284C7).copy(alpha = 0.18f),
                            Color(0xFF0369A1).copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(x = size.width * 0.9f, y = size.height * 0.45f)
                )
                // Bottom-left subtle violet ambient blob
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(x = size.width * 0.25f, y = size.height * 0.88f)
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("app_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .graphicsLayer {
                    alpha = screenAlpha
                    scaleX = screenScale
                    scaleY = screenScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            // ==========================================
            // CENTER HEADER AREA: Lock Icon, App Name, Subtitle, PIN Dots
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glass / Frosted Lock Icon Badge (Smooth unlock bounce & glow)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            scaleX = lockBadgeScale
                            scaleY = lockBadgeScale
                        }
                        .shadow(
                            elevation = if (isSuccess) 24.dp else 16.dp,
                            shape = CircleShape,
                            spotColor = if (isSuccess) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isSuccess) {
                                    listOf(
                                        Color(0xFF34D399).copy(alpha = 0.38f),
                                        Color(0xFF059669).copy(alpha = 0.65f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color(0xFF1E293B).copy(alpha = 0.55f)
                                    )
                                },
                                radius = 90f
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = if (isSuccess) {
                                    listOf(
                                        Color(0xFF6EE7B7).copy(alpha = 0.9f),
                                        Color(0xFF10B981).copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color(0xFF38BDF8).copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (isSuccess) "Unlocked" else "Lock",
                        tint = if (isSuccess) Color(0xFFA7F3D0) else Color(0xFFBAE6FD),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Title
                Text(
                    text = "Find My Things",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status / Prompt Subtitle
                Text(
                    text = if (isSuccess) "Unlocked! Entering Find My Things..." else if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit PIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isSuccess) Color(0xFF34D399) else if (isError) Color(0xFFF87171) else Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4 PIN Indicator Dots (Circular with smooth scale & glow animations)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                ) {
                    repeat(4) { index ->
                        val filled = index < enteredPin.length || isSuccess

                        val dotScale by animateFloatAsState(
                            targetValue = if (isSuccess) 1.10f else if (filled) 1.0f else 0.82f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "pin_dot_scale_$index"
                        )

                        val dotGlowAlpha by animateFloatAsState(
                            targetValue = if (isSuccess) 1.0f else if (filled) 0.85f else 0f,
                            animationSpec = tween(durationMillis = 180),
                            label = "pin_dot_glow_$index"
                        )

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .drawBehind {
                                    // Soft outer glow when filled or unlocked
                                    if (dotGlowAlpha > 0f) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    if (isSuccess) Color(0xFF34D399).copy(alpha = 0.70f * dotGlowAlpha)
                                                    else if (isError) Color(0xFFF87171).copy(alpha = 0.55f * dotGlowAlpha)
                                                    else Color(0xFF38BDF8).copy(alpha = 0.55f * dotGlowAlpha),
                                                    Color.Transparent
                                                ),
                                                radius = size.maxDimension * 1.6f
                                            )
                                        )
                                    }
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isSuccess) {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFA7F3D0),
                                                Color(0xFF34D399),
                                                Color(0xFF059669)
                                            )
                                        )
                                    } else if (filled) {
                                        if (isError) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFCA5A5),
                                                    Color(0xFFEF4444)
                                                )
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF7DD3FC),
                                                    Color(0xFF38BDF8),
                                                    Color(0xFF2563EB)
                                                )
                                            )
                                        }
                                    } else {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.12f),
                                                Color(0xFF1E293B).copy(alpha = 0.40f)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (isSuccess) {
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF6EE7B7), Color(0xFF047857))
                                        )
                                    } else if (filled) {
                                        Brush.verticalGradient(
                                            colors = if (isError) {
                                                listOf(Color(0xFFFECACA), Color(0xFFDC2626))
                                            } else {
                                                listOf(Color(0xFFBAE6FD), Color(0xFF1D4ED8))
                                            }
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.30f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        )
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // ==========================================
            // NUMBER KEYPAD (3 Columns, Perfect Circular Glass Buttons, Zero Square Highlight)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = keypadAlpha
                        translationY = keypadOffsetY
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> {
                                    Spacer(modifier = Modifier.size(72.dp))
                                }
                                "DEL" -> {
                                    GlassKeypadButton(
                                        onClick = { onBackspace() },
                                        testTag = "pin_key_delete"
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = Color.White.copy(alpha = 0.90f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                else -> {
                                    GlassKeypadButton(
                                        onClick = { onDigitClick(key) },
                                        testTag = "pin_key_$key"
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

/**
 * Premium Frosted Glass Keypad Button.
 *
 * Guaranteed completely free of rectangular ripples, square highlights, or default Material boxes.
 * All tactile responses (scale 1.0 -> 0.94, surface brightening, soft blue circular glow, border glow)
 * are strictly clipped to [CircleShape] with [indication = null].
 */
@Composable
private fun GlassKeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press/release scale animation: 1.0 -> 0.94 -> 1.0
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
        label = "keypad_button_scale"
    )

    // Glass surface brightness transition
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.32f else 0.14f,
        animationSpec = tween(durationMillis = 130),
        label = "keypad_surface_alpha"
    )

    // Border brightness transition
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.70f else 0.28f,
        animationSpec = tween(durationMillis = 130),
        label = "keypad_border_alpha"
    )

    // Soft circular blue glow transition
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.55f else 0f,
        animationSpec = tween(durationMillis = 130),
        label = "keypad_glow_alpha"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // Soft circular blue glow behind button when pressed (strictly circular)
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.45f * glowAlpha),
                                Color(0xFF6366F1).copy(alpha = 0.20f * glowAlpha),
                                Color.Transparent
                            ),
                            radius = size.maxDimension * 0.72f
                        )
                    )
                }
            }
            .clip(CircleShape) // Strictly clips all content, backgrounds, and clicks inside CircleShape
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.38f),
                            Color(0xFF1E293B).copy(alpha = 0.78f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = surfaceAlpha),
                            Color(0xFF1E293B).copy(alpha = 0.48f)
                        )
                    },
                    radius = 110f
                )
            )
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        if (isPressed) Color(0xFF60A5FA).copy(alpha = borderAlpha * 0.85f) else Color(0xFF38BDF8).copy(alpha = borderAlpha * 0.30f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disables standard rectangular ripple completely!
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Specular highlight at top curve of glass bubble
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isPressed) 0.25f else 0.12f),
                                Color.Transparent
                            )
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Fill
                    )
                }
        )

        content()
    }
}
