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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neel.aaila.R
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.ui.components.MemberAvatar
import com.neel.aaila.ui.theme.AailaDeepCobalt
import com.neel.aaila.ui.theme.AailaTeal
import com.neel.aaila.ui.theme.CallDeclineRed
import com.neel.aaila.ui.theme.DarkBackground

@Composable
fun OutgoingCallScreen(
    callee: FamilyMember,
    initialIsVideo: Boolean,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var isSpeakerOn by rememberSaveable { mutableStateOf(initialIsVideo) }
    var isVideoEnabled by rememberSaveable { mutableStateOf(initialIsVideo) }
    var showPreviewDetails by rememberSaveable { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "calling_dots")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        AailaDeepCobalt,
                        AailaTeal.copy(alpha = 0.85f)
                    )
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("outgoing_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isVideoEnabled) "AAILA Video Call · UI Preview" else "AAILA Voice Call · UI Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = "Requires WebRTC & Signaling Server (Phase 2)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }

            // Center Callee Identity + Video Preview Placeholder Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .alpha(ringAlpha)
                            .background(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = CircleShape
                            )
                    )
                    MemberAvatar(
                        initials = callee.initials,
                        colorIndex = callee.avatarColorIndex,
                        size = 112.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = callee.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = callee.relationship,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ringing preview… (No live media stream)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                if (showPreviewDetails) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Phase 2 will negotiate ICE/TURN candidates and encrypted WebRTC streams via Firebase signaling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Bottom Control Dock
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControlButton(
                            icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (isMuted) stringResource(R.string.call_unmute) else stringResource(R.string.call_mute),
                            active = isMuted,
                            onClick = { isMuted = !isMuted },
                            testTag = "outgoing_mute_button"
                        )
                        CallControlButton(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            label = stringResource(R.string.call_speaker),
                            active = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            testTag = "outgoing_speaker_button"
                        )
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                            label = stringResource(R.string.call_camera),
                            active = isVideoEnabled,
                            onClick = { isVideoEnabled = !isVideoEnabled },
                            testTag = "outgoing_camera_button"
                        )
                        CallControlButton(
                            icon = Icons.Rounded.Info,
                            label = "Status",
                            active = showPreviewDetails,
                            onClick = { showPreviewDetails = !showPreviewDetails },
                            testTag = "outgoing_info_button"
                        )
                    }

                    Surface(
                        onClick = onEndCall,
                        shape = CircleShape,
                        color = CallDeclineRed,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("outgoing_end_call_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CallEnd,
                                contentDescription = stringResource(R.string.call_end),
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) Color.White else Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .size(54.dp)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) AailaDeepCobalt else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
