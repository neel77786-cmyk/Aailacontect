package com.neel.aaila.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neel.aaila.R
import com.neel.aaila.ui.components.AailaLogoBadge
import com.neel.aaila.ui.theme.AailaAqua
import com.neel.aaila.ui.theme.AailaDeepCobalt
import com.neel.aaila.ui.theme.AailaRoyalBlue
import com.neel.aaila.ui.theme.AailaTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.76f) }
    val contentAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450)
        )
        delay(1300L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AailaDeepCobalt,
                        AailaRoyalBlue,
                        AailaTeal
                    )
                )
            )
            .clickable(onClick = onFinished)
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("splash_screen_root")
    ) {
        // Soft decorative background glow
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp)
                .scale(haloScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AailaAqua.copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AailaLogoBadge(
                size = 112.dp,
                cornerRadius = 32.dp,
                modifier = Modifier.scale(logoScale.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    letterSpacing = 4.sp
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(contentAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.alpha(contentAlpha.value)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Text(
            text = "Phase 1 Foundation · Tap to continue",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .alpha(contentAlpha.value)
        )
    }
}
