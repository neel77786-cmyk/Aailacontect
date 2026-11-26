package com.neel.aaila.ui.screens.calls

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neel.aaila.R
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.ui.components.MemberAvatar
import com.neel.aaila.ui.theme.AailaAqua
import com.neel.aaila.ui.theme.AailaDeepCobalt
import com.neel.aaila.ui.theme.AailaRoyalBlue
import com.neel.aaila.ui.theme.AailaTeal
import com.neel.aaila.ui.theme.CallAcceptGreen
import com.neel.aaila.ui.theme.CallDeclineRed

@Composable
fun IncomingCallScreen(
    caller: FamilyMember,
    isVideo: Boolean,
    onAcceptPreview: () -> Unit,
    onDecline: () -> Unit,
    onQuickReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "incoming_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AailaDeepCobalt,
                        AailaRoyalBlue,
                        AailaTeal
                    )
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("incoming_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top call type & Phase 1 preview indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isVideo) "Incoming AAILA Video Call" else "Incoming AAILA Voice Call",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "UI Preview · Signaling & Push Pending Phase 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }

            // Center caller avatar with concentric pulse ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(154.dp)
                            .scale(pulseScale)
                            .background(
                                color = AailaAqua.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                    )
                    MemberAvatar(
                        initials = caller.initials,
                        colorIndex = caller.avatarColorIndex,
                        size = 112.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = caller.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${caller.relationship} · ${caller.role.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }

            // Bottom Quick Reply + Decline/Answer controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                AssistChip(
                    onClick = onQuickReply,
                    label = {
                        Text(
                            text = stringResource(R.string.call_quick_reply),
                            color = Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White.copy(alpha = 0.14f)
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("incoming_call_quick_reply")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onDecline,
                            shape = CircleShape,
                            color = CallDeclineRed,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incoming_call_decline_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = stringResource(R.string.call_decline),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.call_decline),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onAcceptPreview,
                            shape = CircleShape,
                            color = CallAcceptGreen,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incoming_call_accept_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                                    contentDescription = stringResource(R.string.call_answer),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.call_answer),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
