package eu.binarysystem.logishift.services

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import eu.binarysystem.logishift.activities.WebViewActivity
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.Constants.Companion.BODY_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.CALL_JS_COMMAND_FROM_BROADCAST_INTENT_ACTION_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.FUNCTION_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.INTENT_EXTRA_FUNCTION_NEWS_SHIFT
import eu.binarysystem.logishift.utilities.Constants.Companion.INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_PREF_PUSH_TOKEN
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER
import eu.binarysystem.logishift.utilities.Constants.Companion.TITLE_CONST
import eu.binarysystem.logishift.utilities.LogishiftNotificationManager
import me.leolin.shortcutbadger.ShortcutBadger
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseCloudMessagingService() : FirebaseMessagingService() {

    val pref: SharedPreferencesRetriever = SharedPreferencesRetriever(applicationContext)

    private lateinit var payloadTitleParameter: String
    private lateinit var payloadBodyParameter: String
    private lateinit var payloadFunctionParameter: String
    private lateinit var mappedPayLoadRemoteMessage: Map<String, String>
    private var notificationNewsShiftBackgroundCounter: Int = 0
    private var notificationNotAvailabilityUpdateBackgroundCounter: Int = 0
    private lateinit var webViewActivity: WebViewActivity

    override fun onNewToken(refreshedToken: String) {
        pref.getDefaultSharedEditor().putString(SHARED_KEY_PREF_PUSH_TOKEN, refreshedToken).apply()
        Timber.d("FCM Token %s", refreshedToken)
        super.onNewToken(refreshedToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        mappedPayLoadRemoteMessage = remoteMessage.data
        webViewActivity = WebViewActivity()

        updateBackGroundFunctionCounterFromShared()

        if (remoteMessage.notification != null) {
            Timber.d("Cloud Message Notification Body: %s", remoteMessage.notification!!.body)
        }

        if (!isPayLoadParameterCorrectlyEvaluated()) {
            return
        }



        if (webViewActivity.isApplicationInForeGround) {
            // Applicazione in foreground
            Timber.d("Cloud Message data payload foreground: %s", remoteMessage.data)

            if (payloadFunctionParameter == INTENT_EXTRA_FUNCTION_NEWS_SHIFT) {
                sendBroadcast(Intent(CALL_JS_COMMAND_FROM_BROADCAST_INTENT_ACTION_CONST).putExtra(FUNCTION_CONST, INTENT_EXTRA_FUNCTION_NEWS_SHIFT))
            } else if (payloadFunctionParameter == INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY) {
                sendBroadcast(Intent(CALL_JS_COMMAND_FROM_BROADCAST_INTENT_ACTION_CONST).putExtra(FUNCTION_CONST, INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY))
            }
        } else {

            // Applicazione in background
            Timber.d("Cloud Message data payload background: %s", remoteMessage.data)
            if (payloadFunctionParameter == INTENT_EXTRA_FUNCTION_NEWS_SHIFT) {
                notificationNewsShiftBackgroundCounter++
            } else if (payloadFunctionParameter == INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY) {
                notificationNotAvailabilityUpdateBackgroundCounter++
            }

            ShortcutBadger.applyCount(applicationContext, notificationNotAvailabilityUpdateBackgroundCounter + notificationNewsShiftBackgroundCounter)

            saveBackGroundFunctionCounterOnShared()

            val logishiftNotificationManager = LogishiftNotificationManager()

            logishiftNotificationManager.showCustomNotification(applicationContext, payloadTitleParameter, payloadBodyParameter, mappedPayLoadRemoteMessage, 0)
        }


    }

    private fun updateBackGroundFunctionCounterFromShared() {
        notificationNewsShiftBackgroundCounter = pref.getSharedManagerInstance().getInt(SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER, 0)
        notificationNotAvailabilityUpdateBackgroundCounter = pref.getSharedManagerInstance().getInt(SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER, 0)
    }


    private fun isPayLoadParameterCorrectlyEvaluated(): Boolean {
        payloadTitleParameter = mappedPayLoadRemoteMessage[TITLE_CONST].toString()
        payloadBodyParameter = mappedPayLoadRemoteMessage[BODY_CONST].toString()
        payloadFunctionParameter = mappedPayLoadRemoteMessage[FUNCTION_CONST].toString()
        Timber.d("PayLoad parameter - title: %s , body: %s , function: %s", payloadTitleParameter, payloadBodyParameter, payloadFunctionParameter)
        return (payloadTitleParameter.isNotEmpty() || payloadBodyParameter.isNotEmpty() || payloadFunctionParameter.isNotEmpty())
    }

    private fun saveBackGroundFunctionCounterOnShared() {
        pref.getDefaultSharedEditor().putInt(SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER, notificationNewsShiftBackgroundCounter)
        pref.getDefaultSharedEditor().putInt(SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER, notificationNotAvailabilityUpdateBackgroundCounter)
    }

}