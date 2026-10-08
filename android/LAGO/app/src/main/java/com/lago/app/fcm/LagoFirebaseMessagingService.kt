package com.lago.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lago.app.MainActivity
import com.lago.app.R

class LagoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "lago_daily_quiz_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "=== FCM Message Received ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Data payload: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")
        
        // 무조건 알림 표시 (테스트용)
        showDailyQuizNotification(remoteMessage)
        
        // 데이터 페이로드 처리
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Processing data payload: ${remoteMessage.data}")
            
            val type = remoteMessage.data["type"]
            val action = remoteMessage.data["action"]
            
            Log.d(TAG, "Type: $type, Action: $action")
            
            when (type) {
                "daily_quiz" -> {
                    Log.d(TAG, "Daily quiz notification received")
                }
                else -> {
                    Log.d(TAG, "Unknown notification type: $type")
                }
            }
        }
        
        // 알림 페이로드 처리
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification Title: ${it.title}")
            Log.d(TAG, "Notification Body: ${it.body}")
        }
        
        Log.d(TAG, "=== FCM Processing Complete ===")
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // TODO: 서버에 토큰 전송 (필요시)
        // sendTokenToServer(token)
    }
    
    private fun showDailyQuizNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "새로운 데일리 퀴즈!"
        val body = remoteMessage.notification?.body ?: "오늘의 퀴즈가 준비되었습니다!"
        
        createNotificationChannel()
        
        // 데일리 퀴즈로 이동하는 Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "daily_quiz")
            putExtra("type", remoteMessage.data["type"])
            putExtra("action", remoteMessage.data["action"])
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.main_icon) // TODO: 추후 적절한 알림 아이콘으로 변경 필요
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(getColor(R.color.main_blue))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LAGO 데일리 퀴즈",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "LAGO 데일리 퀴즈 알림 채널"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}