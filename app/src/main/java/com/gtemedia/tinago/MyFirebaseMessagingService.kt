package com.gtemedia.tinago

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "MyFirebaseMsgService"
    private val CHANNEL_ID = "TinaGo_Notifications"
    private val CHANNEL_NAME = "TinaGo Alerts"
    private val CHANNEL_DESCRIPTION = "Notifications for vehicle theft and recovery alerts"

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(tag, "Message data payload: " + remoteMessage.data)

            // Handle data payload messages here.
            // For example, you might want to update UI or trigger background tasks.
            // In a real app, you would parse the data and act accordingly.
            val title = remoteMessage.data["title"] ?: "TinaGo Alert"
            val messageBody = remoteMessage.data["body"] ?: "New notification received."
            sendNotification(title, messageBody, remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body, remoteMessage.data)
        }
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated on this device, and when a new token
     * is generated.
     */
    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        // For TinaGo, this token should be saved to the 'users' collection in Firestore
        // associated with the current user's UID. This is typically done in LoginActivity/MainActivity
        // after a user logs in or registers.
        Log.d(tag, "Sending token to server: $token")
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageTitle FCM message title.
     * @param messageBody FCM message body.
     * @param data Optional data payload from the FCM message.
     */
    private fun sendNotification(messageTitle: String?, messageBody: String?, data: Map<String, String>) {
        // Create an intent for the activity to open when the notification is tapped.
        // You might want to open a specific activity based on the notification data.
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add data payload to the intent if needed for specific navigation or actions
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE) // Use FLAG_IMMUTABLE for API 31+

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Use a proper notification icon
            .setContentTitle(messageTitle ?: "TinaGo Notification")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority for important alerts

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since Android 8.0 (API level 26) and higher, notification channels are required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = getColor(R.color.design_default_color_primary) // Use a color from your resources
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Generate a unique ID for each notification. You might use System.currentTimeMillis().toInt()
        // or a specific ID from the 'data' payload for related notifications.
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}
