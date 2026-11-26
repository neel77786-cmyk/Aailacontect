package com.neel.aaila.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneCallback
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neel.aaila.R
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.ui.components.AailaLogoBadge
import com.neel.aaila.ui.components.MemberAvatar
import com.neel.aaila.ui.components.Phase1NoticeBanner
import com.neel.aaila.ui.components.SectionHeader
import com.neel.aaila.ui.theme.AailaDeepCobalt
import com.neel.aaila.ui.theme.AailaRoyalBlue
import com.neel.aaila.ui.theme.AailaTeal

@Composable
fun HomeDashboardScreen(
    viewModel: DashboardViewModel,
    onStartVoiceCall: (memberId: String) -> Unit,
    onStartVideoCall: (memberId: String) -> Unit,
    onPreviewIncomingCall: (memberId: String) -> Unit,
    onOpenFamilyChat: (threadId: String) -> Unit,
    onOpenFamilyDirectory: () -> Unit,
    onOpenAdminSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultContact = uiState.members.firstOrNull { it.id != "member_neel" } ?: uiState.members.first()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Family Space Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AailaDeepCobalt, AailaRoyalBlue, AailaTeal)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AailaLogoBadge(size = 50.dp, cornerRadius = 15.dp)
                                Column {
                                    Text(
                                        text = uiState.familySpace.familyName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.app_tagline),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.86f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = uiState.familySpace.familyId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Hello, ${uiState.familySpace.adminName}. Your private family dashboard is ready to explore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            // Honest Phase 1 Readiness Banner
            item {
                Phase1NoticeBanner(
                    title = stringResource(R.string.phase1_badge_title),
                    message = stringResource(R.string.phase1_backend_notice)
                )
            }

            // Quick Connect Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.dashboard_quick_actions),
                        subtitle = "Large, clear buttons designed for every family member"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionTile(
                            label = stringResource(R.string.action_voice_call),
                            sublabel = "Preview UI",
                            icon = Icons.Rounded.Call,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { onStartVoiceCall(defaultContact.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_voice_call_tile")
                        )
                        QuickActionTile(
                            label = stringResource(R.string.action_video_call),
                            sublabel = "Preview UI",
                            icon = Icons.Rounded.Videocam,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { onStartVideoCall(defaultContact.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_video_call_tile")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionTile(
                            label = stringResource(R.string.action_family_chat),
                            sublabel = "Group Thread",
                            icon = Icons.AutoMirrored.Rounded.Chat,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onOpenFamilyChat("thread_family_circle") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_family_chat_tile")
                        )
                        QuickActionTile(
                            label = "Incoming Call",
                            sublabel = "Preview Ring",
                            icon = Icons.Rounded.PhoneCallback,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.secondary,
                            onClick = { onPreviewIncomingCall(defaultContact.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_incoming_call_tile")
                        )
                    }
                }
            }

            // Family Circle Horizontal Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.dashboard_family_circle),
                        subtitle = "${uiState.members.size} sample profiles in preview",
                        trailing = {
                            TextButton(
                                onClick = onOpenFamilyDirectory,
                                modifier = Modifier.testTag("dashboard_see_all_family")
                            ) {
                                Text("View All")
                            }
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.members, key = { it.id }) { member ->
                            FamilyCircleChipCard(
                                member = member,
                                onClick = { onStartVoiceCall(member.id) }
                            )
                        }
                    }
                }
            }

            // Recent Family Threads Preview
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.dashboard_recent_activity),
                        subtitle = "Tap any conversation to inspect the chat screen"
                    )

                    uiState.recentThreads.forEach { thread ->
                        Card(
                            onClick = { onOpenFamilyChat(thread.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_thread_${thread.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MemberAvatar(
                                    initials = thread.initials,
                                    colorIndex = if (thread.isGroup) 0 else 1,
                                    size = 48.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = thread.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = thread.lastMessagePreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = thread.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Admin Setup Shortcut Card
            item {
                Card(
                    onClick = onOpenAdminSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_admin_setup_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.action_admin_setup),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Configure Family Name, custom ID, and privacy rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    sublabel: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FamilyCircleChipCard(
    member: FamilyMember,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_circle_member_${member.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MemberAvatar(
                initials = member.initials,
                colorIndex = member.avatarColorIndex,
                status = member.status,
                size = 52.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.relationship,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
