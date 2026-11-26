package com.neel.aaila

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class AailaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val callsChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Family Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts"
                enableVibration(true)
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Family Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New family message notifications"
            }

            notificationManager.createNotificationChannel(callsChannel)
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_CALLS = "aaila_calls_channel"
        const val CHANNEL_ID_MESSAGES = "aaila_messages_channel"
    }
}
