package com.neel.aaila.ui.screens.calls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PhoneCallback
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neel.aaila.R
import com.neel.aaila.data.model.CallDirection
import com.neel.aaila.data.model.CallLogEntry
import com.neel.aaila.data.model.CallType
import com.neel.aaila.ui.components.MemberAvatar
import com.neel.aaila.ui.components.Phase1NoticeBanner
import com.neel.aaila.ui.components.SectionHeader
import com.neel.aaila.ui.theme.CallDeclineRed

@Composable
fun CallsScreen(
    viewModel: CallsViewModel,
    onStartOutgoingCall: (memberId: String, isVideo: Boolean) -> Unit,
    onPreviewIncomingCall: (memberId: String, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sampleCaller = uiState.quickContacts.firstOrNull()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("calls_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = stringResource(R.string.calls_title),
                    subtitle = "One-tap voice & video calling UI"
                )
            }

            item {
                Phase1NoticeBanner(
                    title = "Calling Backend Pending (Phase 2)",
                    message = stringResource(R.string.phase2_calling_notice),
                    isWarningTone = true
                )
            }

            // Call Screen Preview Launcher Card
            if (sampleCaller != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Preview Call Interfaces",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Inspect the dedicated incoming and outgoing call designs with ${sampleCaller.name}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { onPreviewIncomingCall(sampleCaller.id, true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("calls_preview_incoming_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhoneCallback,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Incoming")
                                }

                                FilledTonalButton(
                                    onClick = { onStartOutgoingCall(sampleCaller.id, false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("calls_preview_outgoing_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Outgoing")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Dial Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Call Family",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.quickContacts, key = { it.id }) { member ->
                            Surface(
                                onClick = { onStartOutgoingCall(member.id, false) },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.testTag("quick_dial_${member.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MemberAvatar(
                                        initials = member.initials,
                                        colorIndex = member.avatarColorIndex,
                                        status = member.status,
                                        size = 48.dp
                                    )
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.calls_recent_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(uiState.callHistory, key = { it.id }) { entry ->
                CallHistoryRowCard(
                    entry = entry,
                    onVoiceCall = { onStartOutgoingCall(entry.memberId, false) },
                    onVideoCall = { onStartOutgoingCall(entry.memberId, true) }
                )
            }
        }
    }
}

@Composable
private fun CallHistoryRowCard(
    entry: CallLogEntry,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val directionIcon = when (entry.direction) {
        CallDirection.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
        CallDirection.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
        CallDirection.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
    }
    val directionColor = if (entry.direction == CallDirection.MISSED) {
        CallDeclineRed
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MemberAvatar(
                initials = entry.initials,
                colorIndex = entry.memberName.length,
                size = 48.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${entry.memberName} (${entry.relationship})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        tint = directionColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${entry.timestampLabel} · ${entry.durationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.direction == CallDirection.MISSED) {
                            CallDeclineRed
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledTonalIconButton(
                onClick = if (entry.callType == CallType.VIDEO) onVideoCall else onVoiceCall,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("call_log_action_${entry.id}")
            ) {
                Icon(
                    imageVector = if (entry.callType == CallType.VIDEO) {
                        Icons.Rounded.Videocam
                    } else {
                        Icons.Rounded.Call
                    },
                    contentDescription = "Call ${entry.memberName}"
                )
            }
        }
    }
}
