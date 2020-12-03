package eu.binarysystem.logishift.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.activities.WebViewActivity
import eu.binarysystem.logishift.utilities.Constants.Companion.NOTIFICATION_CHANNEL_DESCRIPTION
import eu.binarysystem.logishift.utilities.Constants.Companion.NOTIFICATION_CHANNEL_ID_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.NOTIFICATION_CHANNEL_NAME
import eu.binarysystem.logishift.utilities.Constants.Companion.NOTIFICATION_OPEN_APPLICATION_PENDING_INTENT_CONST

class LogishiftNotificationManager {

    fun showCustomNotification(context: Context, title: String, body: String, mappedParameter: Map<String, String>, notificationId: Int) {

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_CONST)
        notificationBuilder.setSmallIcon(R.mipmap.notification)
        notificationBuilder.setLargeIcon(returnLogishiftLargeIcon(context))
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        notificationBuilder.setContentIntent(returnNotificationPendingIntent(context, mappedParameter))
        notificationBuilder.setAutoCancel(false)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
            }

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationId != 0){
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
        else{
            notificationManager.notify(NOTIFICATION_OPEN_APPLICATION_PENDING_INTENT_CONST, notificationBuilder.build())
        }


    }

    fun cancelCustomNotification(context: Context){

    }

    private fun returnNotificationPendingIntent(context: Context, mappedParameter: Map<String, String>): PendingIntent{
        val startActivityIntent = Intent(context, WebViewActivity::class.java)
        if (mappedParameter.isNullOrEmpty()){
            for (key:String in mappedParameter.keys){
                startActivityIntent.putExtra(key, mappedParameter[key])
            }
        }
        return PendingIntent.getActivity(context, NOTIFICATION_OPEN_APPLICATION_PENDING_INTENT_CONST, startActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context){
        val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID_CONST, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.description = NOTIFICATION_CHANNEL_DESCRIPTION
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun returnLogishiftLargeIcon(context: Context): Bitmap{
        val contextResources: Resources = context.resources
        return BitmapFactory.decodeResource(contextResources, R.mipmap.ic_launcher)
    }
}