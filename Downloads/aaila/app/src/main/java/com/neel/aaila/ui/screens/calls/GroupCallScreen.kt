package com.neel.aaila.ui.screens.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neel.aaila.data.repository.GroupCallState
import com.neel.aaila.data.repository.GroupParticipant

@Composable
fun GroupCallScreen(
    state: GroupCallState,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onLeaveCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showParticipantsList by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = state.roomName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "LiveKit SFU · ${state.participants.size} Family Members",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }

                    IconButton(onClick = { showParticipantsList = !showParticipantsList }) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Participants",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video Grid (5+ Participants Grid)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.participants) { participant ->
                        ParticipantTile(
                            participant = participant,
                            isActiveSpeaker = participant.id == state.activeSpeakerId
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = onToggleMic,
                        containerColor = if (state.isMicMuted) Color.Red else Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic Toggle"
                        )
                    }

                    FloatingActionButton(
                        onClick = onToggleCamera,
                        containerColor = if (!state.isCameraEnabled) Color.Red else Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera Toggle"
                        )
                    }

                    FloatingActionButton(
                        onClick = onLeaveCall,
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Leave Call"
                        )
                    }
                }
            }

            // Participant Drawer Sheet Overlay
            AnimatedVisibility(
                visible = showParticipantsList,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Group Call Participants (${state.participants.size})",
                            style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        state.participants.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.initials,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    modifier = Modifier.weight(1f)
                                )
                                if (p.isSpeaking) {
                                    Text(
                                        text = "Speaking",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantTile(
    participant: GroupParticipant,
    isActiveSpeaker: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActiveSpeaker) Color(0xFF10B981) else Color.Transparent

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = participant.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )

            if (participant.isAudioMuted) {
                Text(
                    text = "Muted",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
