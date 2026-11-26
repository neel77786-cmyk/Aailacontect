package com.neel.aaila.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neel.aaila.R
import com.neel.aaila.data.model.OnlineStatus
import com.neel.aaila.ui.theme.AailaAqua
import com.neel.aaila.ui.theme.AailaDeepCobalt
import com.neel.aaila.ui.theme.AailaRoyalBlue
import com.neel.aaila.ui.theme.AailaTeal
import com.neel.aaila.ui.theme.StatusAvailable
import com.neel.aaila.ui.theme.StatusAway
import com.neel.aaila.ui.theme.StatusBusy
import com.neel.aaila.ui.theme.StatusOffline

@Composable
fun AailaLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    cornerRadius: Dp = 24.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Image(
            painter = painterResource(id = R.drawable.aaila_logo),
            contentDescription = stringResource(R.string.logo_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(size * 0.14f)
        )
    }
}

@Composable
fun Phase1NoticeBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    isWarningTone: Boolean = false
) {
    val containerColor = if (isWarningTone) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    val contentColor = if (isWarningTone) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isWarningTone) Icons.Rounded.CloudOff else Icons.Rounded.Info,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun MemberAvatar(
    initials: String,
    colorIndex: Int,
    status: OnlineStatus? = null,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val avatarGradient = when (colorIndex % 6) {
        0 -> listOf(AailaRoyalBlue, AailaAqua)
        1 -> listOf(AailaTeal, Color(0xFF26C6DA))
        2 -> listOf(AailaDeepCobalt, AailaRoyalBlue)
        3 -> listOf(Color(0xFF0284C7), AailaAqua)
        4 -> listOf(Color(0xFF0E7490), Color(0xFF38BDF8))
        else -> listOf(Color(0xFF1D4ED8), AailaTeal)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarGradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (status != null) {
            val dotColor = statusColor(status)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.28f).coerceIn(12.dp, 18.dp))
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

fun statusColor(status: OnlineStatus): Color = when (status) {
    OnlineStatus.AVAILABLE -> StatusAvailable
    OnlineStatus.BUSY -> StatusBusy
    OnlineStatus.AWAY -> StatusAway
    OnlineStatus.OFFLINE -> StatusOffline
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
