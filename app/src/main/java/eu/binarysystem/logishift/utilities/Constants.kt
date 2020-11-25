package eu.binarysystem.logishift.utilities

import android.Manifest

class Constants {
    companion object {
        const val SHARED_KEY_PREF_USEREMAIL = "user_email"
        const val SHARED_KEY_AUTH_TOKEN = "auth_token"
        const val SHARED_KEY_LOGI_SHIFT_URL = "logishift_url"
        const val SHARED_KEY_URL_SERVER_ENVIRONMENT = "urlKey"
        const val SHARED_KEY_PREF_PUSH_TOKEN = "push_token"
        const val SHARED_KEY_LOGI_SHIFT_BASE64_ICON = "base64StringIcon"

        const val DOCUMENT_TO_OPEN_DETAIL_CONST = "document_to_open_detail"
        const val MIN_DISTANCE_CHANGE_FOR_UPDATES_CONST: Float = 10F
        const val MIN_TIME_BW_UPDATES_CONST: Long = 1000 * 60
        const val SHARED_PREFERENCES_INSTANCE_NAME_CONST: String =
            "eu.binarysystem.logishift_preferences"
        const val READ_UPLOADED_URI_CODE_CONST = 42
        const val LOGI_SHIFT_MOBILE_USER_AGENT_CONST = "bs_logishift__mobile_bs"
        const val FUNCTION_CONST = "function"
        const val TITLE_CONST = "title"
        const val BODY_CONST = "body"
        const val PERMISSIONS_REQUEST_CODE_CONST = 11232

        const val NOTIFICATION_BACKGROUND_MESSAGE = "notifcationBacgroundMessage"


        val PERMISSIONS_ARRAY_ENUM = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}