package com.neel.aaila.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AailaRoute {
    @Serializable
    data object Splash : AailaRoute

    @Serializable
    data object Login : AailaRoute

    @Serializable
    data object AdminSetup : AailaRoute

    @Serializable
    data object MainShell : AailaRoute

    @Serializable
    data class ChatDetail(val threadId: String) : AailaRoute

    @Serializable
    data class IncomingCall(
        val memberId: String,
        val isVideo: Boolean
    ) : AailaRoute

    @Serializable
    data class OutgoingCall(
        val memberId: String,
        val isVideo: Boolean
    ) : AailaRoute

    @Serializable
    data class GroupCall(val roomName: String) : AailaRoute
}

enum class MainTab(val routeKey: String, val label: String) {
    HOME("home", "Home"),
    CALLS("calls", "Calls"),
    CHATS("chats", "Chats"),
    FAMILY("family", "Family"),
    SETTINGS("settings", "Settings")
}
