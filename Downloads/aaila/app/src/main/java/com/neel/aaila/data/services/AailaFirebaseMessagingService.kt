package com.neel.aaila.data.services

import android.app.PendingIntent

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neel.aaila.AailaApplication
import com.neel.aaila.MainActivity

class AailaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token synced to Firestore via Repository when user is active
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Family Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new message"
        val type = message.data["type"] ?: "message"

        sendNotification(title, body, type)
    }

    private fun sendNotification(title: String, body: String, type: String) {
        val channelId = if (type == "call") {
            AailaApplication.CHANNEL_ID_CALLS
        } else {
            AailaApplication.CHANNEL_ID_MESSAGES
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (type == "call") NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                }
            } else {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (_: Exception) {}
    }
}
