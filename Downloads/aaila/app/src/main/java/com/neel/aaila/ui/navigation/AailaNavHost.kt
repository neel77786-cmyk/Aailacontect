package com.neel.aaila.ui.navigation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.neel.aaila.di.AailaAppContainer
import com.neel.aaila.ui.screens.auth.AdminSetupScreen
import com.neel.aaila.ui.screens.auth.AuthViewModel
import com.neel.aaila.ui.screens.auth.LoginScreen
import com.neel.aaila.ui.screens.calls.CallsScreen
import com.neel.aaila.ui.screens.calls.CallsViewModel
import com.neel.aaila.ui.screens.calls.IncomingCallScreen
import com.neel.aaila.ui.screens.calls.OutgoingCallScreen
import com.neel.aaila.ui.screens.chats.ChatDetailScreen
import com.neel.aaila.ui.screens.chats.ChatsScreen
import com.neel.aaila.ui.screens.chats.ChatsViewModel
import com.neel.aaila.ui.screens.dashboard.DashboardViewModel
import com.neel.aaila.ui.screens.dashboard.HomeDashboardScreen
import com.neel.aaila.ui.screens.family.FamilyScreen
import com.neel.aaila.ui.screens.family.FamilyViewModel
import com.neel.aaila.ui.screens.settings.SettingsScreen
import com.neel.aaila.ui.screens.settings.SettingsViewModel
import com.neel.aaila.ui.screens.splash.SplashScreen

@Composable
fun AailaNavHost(
    container: AailaAppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val sessionExpired by container.tokenManager.sessionExpiredEvent.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            navController.navigate(AailaRoute.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(container.familyAuthRepository)
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(container.foundationRepository)
    )
    val callsViewModel: CallsViewModel = viewModel(
        factory = CallsViewModel.Factory(container.foundationRepository)
    )
    val chatsViewModel: ChatsViewModel = viewModel(
        factory = ChatsViewModel.Factory(container.foundationRepository)
    )
    val familyViewModel: FamilyViewModel = viewModel(
        factory = FamilyViewModel.Factory(container.foundationRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            container.themePreferencesRepository,
            container.foundationRepository
        )
    )

    NavHost(
        navController = navController,
        startDestination = AailaRoute.Splash,
        modifier = modifier
    ) {
        composable<AailaRoute.Splash> {
            SplashScreen(
                onFinished = {
                    navController.navigate(AailaRoute.Login) {
                        popUpTo(AailaRoute.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<AailaRoute.Login> {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToDashboardPreview = {
                    navController.navigate(AailaRoute.MainShell) {
                        popUpTo(AailaRoute.Login) { inclusive = true }
                    }
                },
                onNavigateToAdminSetup = {
                    navController.navigate(AailaRoute.AdminSetup)
                }
            )
        }

        composable<AailaRoute.AdminSetup> {
            AdminSetupScreen(
                viewModel = authViewModel,
                onBackToLogin = { navController.popBackStack() },
                onContinueToPreviewDashboard = {
                    navController.navigate(AailaRoute.MainShell) {
                        popUpTo(AailaRoute.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<AailaRoute.MainShell> {
            MainShellScreen(
                dashboardViewModel = dashboardViewModel,
                callsViewModel = callsViewModel,
                chatsViewModel = chatsViewModel,
                familyViewModel = familyViewModel,
                settingsViewModel = settingsViewModel,
                onOpenChatDetail = { threadId ->
                    navController.navigate(AailaRoute.ChatDetail(threadId))
                },
                onOpenIncomingCall = { memberId, isVideo ->
                    navController.navigate(AailaRoute.IncomingCall(memberId, isVideo))
                },
                onOpenOutgoingCall = { memberId, isVideo ->
                    navController.navigate(AailaRoute.OutgoingCall(memberId, isVideo))
                },
                onOpenAdminSetup = {
                    navController.navigate(AailaRoute.AdminSetup)
                },
                onReturnToLogin = {
                    container.familyAuthRepository.signOut()
                    navController.navigate(AailaRoute.Login) {
                        popUpTo(AailaRoute.MainShell) { inclusive = true }
                    }
                }
            )
        }

        composable<AailaRoute.ChatDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<AailaRoute.ChatDetail>()
            ChatDetailScreen(
                threadId = args.threadId,
                viewModel = chatsViewModel,
                onBack = { navController.popBackStack() },
                onStartVoiceCall = { memberId ->
                    navController.navigate(AailaRoute.OutgoingCall(memberId, isVideo = false))
                },
                onStartVideoCall = { memberId ->
                    navController.navigate(AailaRoute.OutgoingCall(memberId, isVideo = true))
                }
            )
        }

        composable<AailaRoute.IncomingCall> { backStackEntry ->
            val args = backStackEntry.toRoute<AailaRoute.IncomingCall>()
            val caller = callsViewModel.findMember(args.memberId)
            IncomingCallScreen(
                caller = caller,
                isVideo = args.isVideo,
                onAcceptPreview = {
                    navController.navigate(AailaRoute.OutgoingCall(args.memberId, args.isVideo)) {
                        popUpTo(AailaRoute.IncomingCall(args.memberId, args.isVideo)) {
                            inclusive = true
                        }
                    }
                },
                onDecline = { navController.popBackStack() },
                onQuickReply = {
                    val threadId = familyViewModel.threadIdForMember(args.memberId)
                    navController.navigate(AailaRoute.ChatDetail(threadId)) {
                        popUpTo(AailaRoute.IncomingCall(args.memberId, args.isVideo)) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<AailaRoute.OutgoingCall> { backStackEntry ->
            val args = backStackEntry.toRoute<AailaRoute.OutgoingCall>()
            val callee = callsViewModel.findMember(args.memberId)
            OutgoingCallScreen(
                callee = callee,
                initialIsVideo = args.isVideo,
                onEndCall = { navController.popBackStack() }
            )
        }

        composable<AailaRoute.GroupCall> { backStackEntry ->
            val args = backStackEntry.toRoute<AailaRoute.GroupCall>()
            val liveKitManager = container.liveKitGroupCallManager
            androidx.compose.runtime.LaunchedEffect(args.roomName) {
                liveKitManager.joinGroupRoom(args.roomName)
            }
            val state by liveKitManager.groupCallState.collectAsStateWithLifecycle()

            com.neel.aaila.ui.screens.calls.GroupCallScreen(
                state = state,
                onToggleMic = { liveKitManager.toggleMic() },
                onToggleCamera = { liveKitManager.toggleCamera() },
                onLeaveCall = {
                    liveKitManager.leaveRoom()
                    navController.popBackStack()
                }
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun MainShellScreen(
    dashboardViewModel: DashboardViewModel,
    callsViewModel: CallsViewModel,
    chatsViewModel: ChatsViewModel,
    familyViewModel: FamilyViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenChatDetail: (threadId: String) -> Unit,
    onOpenIncomingCall: (memberId: String, isVideo: Boolean) -> Unit,
    onOpenOutgoingCall: (memberId: String, isVideo: Boolean) -> Unit,
    onOpenAdminSetup: () -> Unit,
    onReturnToLogin: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    BackHandler(enabled = selectedTab != MainTab.HOME) {
        selectedTab = MainTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("bottom_nav_${tab.routeKey}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_nav_${tab.routeKey}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        MainTab.HOME -> HomeDashboardScreen(
                            viewModel = dashboardViewModel,
                            onStartVoiceCall = { memberId -> onOpenOutgoingCall(memberId, false) },
                            onStartVideoCall = { memberId -> onOpenOutgoingCall(memberId, true) },
                            onPreviewIncomingCall = { memberId -> onOpenIncomingCall(memberId, true) },
                            onOpenFamilyChat = onOpenChatDetail,
                            onOpenFamilyDirectory = { selectedTab = MainTab.FAMILY },
                            onOpenAdminSetup = onOpenAdminSetup
                        )

                        MainTab.CALLS -> CallsScreen(
                            viewModel = callsViewModel,
                            onStartOutgoingCall = onOpenOutgoingCall,
                            onPreviewIncomingCall = onOpenIncomingCall
                        )

                        MainTab.CHATS -> ChatsScreen(
                            viewModel = chatsViewModel,
                            onOpenThread = onOpenChatDetail
                        )

                        MainTab.FAMILY -> FamilyScreen(
                            viewModel = familyViewModel,
                            onVoiceCall = { memberId -> onOpenOutgoingCall(memberId, false) },
                            onVideoCall = { memberId -> onOpenOutgoingCall(memberId, true) },
                            onPreviewIncomingCall = { memberId -> onOpenIncomingCall(memberId, true) },
                            onMessageMember = onOpenChatDetail
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel,
                            onReturnToLogin = onReturnToLogin
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainTab.CALLS -> if (selected) Icons.Rounded.Call else Icons.Outlined.Call
    MainTab.CHATS -> if (selected) Icons.AutoMirrored.Rounded.Chat else Icons.AutoMirrored.Outlined.Chat
    MainTab.FAMILY -> if (selected) Icons.Rounded.Group else Icons.Outlined.Group
    MainTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
