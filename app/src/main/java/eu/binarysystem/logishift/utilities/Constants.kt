package eu.binarysystem.logishift.utilities

import android.Manifest

class Constants {
    companion object {
        //Shared Const
        const val SHARED_KEY_PREF_USEREMAIL = "user_email"
        const val SHARED_KEY_AUTH_TOKEN = "auth_token"
        const val SHARED_KEY_LOGI_SHIFT_URL = "logishift_url"
        const val SHARED_KEY_URL_SERVER_ENVIRONMENT = "urlKey"
        const val SHARED_KEY_PREF_PUSH_TOKEN = "push_token"
        const val SHARED_KEY_LOGI_SHIFT_BASE64_ICON = "base64StringIcon"
        const val SHARED_HTTP_SCHEMA = "httpSchema"
        const val SHARED_NOTIFICATION_PAYLOAD_MESSAGE = "notificationPayloadMessage"
        const val SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER = "newShiftBackgroundCounter"
        const val SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER = "newShiftBackgroundCounter"
        const val SHARED_QUEUE_MANAGER_QUEUE_URL = "pref_queue_url"
        const val SHARED_QUEUE_MANAGER_QUEUE = "pref_queue"

        //Generic Const
        const val DOCUMENT_TO_OPEN_DETAIL_CONST = "document_to_open_detail"
        const val MIN_DISTANCE_CHANGE_FOR_UPDATES_CONST: Float = 10F
        const val MIN_TIME_BW_UPDATES_CONST: Long = 1000 * 60
        const val SHARED_PREFERENCES_INSTANCE_NAME_CONST: String = "eu.binarysystem.logishift_preferences"
        const val READ_UPLOADED_URI_CODE_CONST = 42
        const val LOGI_SHIFT_MOBILE_USER_AGENT_CONST = "bs_logishift__mobile_bs"
        const val FUNCTION_CONST = "function"
        const val TITLE_CONST:String = "title"
        const val BODY_CONST = "body"
        const val PERMISSIONS_REQUEST_CODE_CONST = 11232
        const val HTTP_SEPARATOR_CONST = "://"
        const val SSO_MAIN_AUTH_URL_CONST = "/oauth_applications/app_sign_on"
        const val SSO_SEND_USER_LOG_INFO_URL_CONST = "/app_backend/service_infos?auth_token="
        const val SSO_SEND_PUSH_TOKEN_URL_CONST = "  /app_backend/notifications"

        const val BROADCAST_INTENT_ACTION_CONST = "WebViewActivity_BroadcastIntent"
        const val JOB_DISPATCHER_SERVICE_TAG_CONST = "logishift-network-service"
        const val NOTIFICATION_OPEN_APPLICATION_PENDING_INTENT_CONST = 3417
        const val NOTIFICATION_CHANNEL_ID_CONST = "my_channel_01"
        const val NOTIFICATION_CHANNEL_NAME = "my_notification_channel_name"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Logishift user notification"


        //Intent const
        const val INTENT_EXTRA_FUNCTION_NEWS_SHIFT = "newsShift"
        const val INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY= "notAvailabilityUpdate"

        //Js Command const
        const val JS_DO_LOG_OUT_COMMAND = "doLogoutFromApp();"
        const val JS_OPEN_INFO_COMMAND = "openInfoTab();"
        const val JS_NEW_SHIFT_NOTIFICATION_COMMAND = "newShiftNotification();"
        fun JS_NOT_AVAILABILITY_UPDATE_COMMAND(notificationBackGroundMessage: String?):String{
            return "notAvailabilityUpdateAuthorizationStateNotification('$notificationBackGroundMessage');"
        }

        val STRINGS_TO_URL_SHOULD_BE_OVERRIDE_ARRAY = arrayOf("verform", "shunting", "trainshunt", "tel")
        val PERMISSIONS_ARRAY_ENUM = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)


    }
}