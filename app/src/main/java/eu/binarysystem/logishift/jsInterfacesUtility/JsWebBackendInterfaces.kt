package eu.binarysystem.logishift.jsInterfacesUtility

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.binarysystem.logishift.activities.WebViewActivity
import eu.binarysystem.logishift.hilt.LocationManagerRetriever
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.BackendCommunicationManager
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_ASK_FOR_COORDINATES
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_IS_GPS_ENABLE_COMMAND
import eu.binarysystem.logishift.utilities.Constants.Companion.SAVE_VARIABLES
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_AUTH_TOKEN
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_BASE64_ICON
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_URL
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_PREF_USEREMAIL
import eu.binarysystem.logishift.utilities.GpsManager
import eu.binarysystem.logishift.utilities.QueueManager
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class JsWebBackendInterfaceslocationManager(val context: Context, private val webViewActivity: WebViewActivity, private var gpsManager: GpsManager, private val locationManger: LocationManagerRetriever, val pref: SharedPreferencesRetriever) {

    @JavascriptInterface fun setUserInformation(authToken: String?, userEmail: String?, logishiftUrlEndpoint: String?) {


        Timber.d("WebAppInterface - setUserInformation Token -->%s | UserEmail --> %s | LogishiftUrl --> %s", authToken, userEmail, logishiftUrlEndpoint)

        webViewActivity.updateSharedVariablesManager(SAVE_VARIABLES, hashMapOf(SHARED_KEY_AUTH_TOKEN to authToken, SHARED_KEY_PREF_USEREMAIL to userEmail, SHARED_KEY_LOGI_SHIFT_URL to logishiftUrlEndpoint))




        if (userEmail != null) {
            pref.getDefaultSharedEditor().putString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null)
            FirebaseCrashlytics.getInstance().setUserId(userEmail)
        }



        BackendCommunicationManager.getInstance(context, pref).sendPushToken()
        BackendCommunicationManager.getInstance(context, pref).sendLogUserInfo()

        webViewActivity.runOnUiThread { webViewActivity.resetActionBarIcon(webViewActivity) }
        webViewActivity.invalidateOptionsMenu()

    }

    @JavascriptInterface fun sendOfflineData(backendUrl: String, data: String) {
        Timber.d("WebAppInterface - sendOfflineData: backendUrl '%s', data '%s'", backendUrl, data)
        val jsonObject = JSONObject()
        try {
            jsonObject.put("app_params", data)
        } catch (e: JSONException) {
            FirebaseCrashlytics.getInstance().log("Error creating JSON")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        QueueManager.getInstance(context, pref).push(backendUrl, jsonObject.toString())
    }

    @JavascriptInterface fun isGpsEnabled() {
        gpsManager = GpsManager.getInstance(context, locationManger.getLocationManagerInstance())
        Timber.d("WebAppInterface - IS GPS ENABLED")
        webViewActivity.runOnUiThread {
            if (!webViewActivity.isFinishing) {
                webViewActivity.callJavascript(JS_IS_GPS_ENABLE_COMMAND(gpsManager.checkAtLeastOneLocationProvidersAvailable().toString()))
            }
        }
    }

    @JavascriptInterface fun turnOnGeoLoc() {
        gpsManager = GpsManager.getInstance(context, locationManger.getLocationManagerInstance())
        Timber.d("WebAppInterface - TURN ON GEOLOC")
        gpsManager.retrieveLocation()

    }

    @JavascriptInterface fun turnOffGeoLoc() {
        gpsManager = GpsManager.getInstance(context, locationManger.getLocationManagerInstance())
        Timber.d("WebAppInterface - TURN OFF GEOLOC")
        gpsManager.stopUsingGps()
    }

    @JavascriptInterface fun askGeoLoc() {
        gpsManager = GpsManager.getInstance(context, locationManger.getLocationManagerInstance())
        Timber.d("WebAppInterface - askGeoLoc")
        gpsManager.retrieveLocation()
        webViewActivity.runOnUiThread {
            if (!webViewActivity.isFinishing) {
                webViewActivity.callJavascript(JS_ASK_FOR_COORDINATES(webViewActivity.latitude, webViewActivity.longitude, webViewActivity.accuracy))
            }
        }
    }


    @JavascriptInterface fun openLoginPageApp() {
        Timber.d("WebAppInterface - openLoginPageApp")
        webViewActivity.runOnUiThread {
            if (webViewActivity.isCallingEditServer) {
                webViewActivity.resetDomainAndLayout()
                webViewActivity.isCallingEditServer = false
            }
        }
    }

    @JavascriptInterface fun closeApp() {
        webViewActivity.finish()
    }

}
