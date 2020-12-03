package eu.binarysystem.logishift.utilities

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.binarysystem.logishift.BuildConfig
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_AUTH_TOKEN
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_URL
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_PREF_PUSH_TOKEN
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_PREF_USEREMAIL
import eu.binarysystem.logishift.utilities.Constants.Companion.SSO_SEND_PUSH_TOKEN_URL_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.SSO_SEND_USER_LOG_INFO_URL_CONST
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

class BackendCommunicationManager(private var context: Context, private var pref: SharedPreferencesRetriever) {

    private val mediaTypeJson: MediaType? = MediaType.parse("application/json; charset=utf-8")
    private val okHttp3Client: OkHttpClient = OkHttpClient.Builder().addInterceptor(AuthParameterInterceptor(pref)).addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build()
    private var completeLogishiftEmployeeUrl: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
    private val uuid: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    private val systemVersion: String = String.format("%s %s", Build.MODEL, Build.VERSION.RELEASE)
    private val packageVersion: String = BuildConfig.VERSION_NAME
    private val mobileBrandName: String? = getDeviceName()


    companion object {
        @Volatile private var BACKENDCOMMUNICATIONISTANCE: BackendCommunicationManager? = null

        fun getInstance(context: Context, pref: SharedPreferencesRetriever): BackendCommunicationManager {
            if (BACKENDCOMMUNICATIONISTANCE == null) {
                BACKENDCOMMUNICATIONISTANCE = BackendCommunicationManager(context, pref)
            }
            Timber.d("GPSMANAGER -> instance called")
            return BACKENDCOMMUNICATIONISTANCE as BackendCommunicationManager
        }
    }

    fun sendLogUserInfo() {
        val authToken: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_AUTH_TOKEN, null)
        val jsonObject = createBackendUserJsonData()

        sendDataToBackEnd(String.format("%s%s", SSO_SEND_USER_LOG_INFO_URL_CONST, authToken), jsonObject.toString(), object : BackendCallBack {
            override fun onBackendCallBackResponse(responseBody: String?) {
                Timber.d("callBackend - SendLogUserInfo correctly call")
            }

            override fun onBackendCallBackFailure(errorMessage: String?) {
                Timber.e("callBackend - SendLogUserInfo error ->%s", errorMessage)
            }

        })
    }

    fun sendPushToken() {
        val pushToken: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_PREF_PUSH_TOKEN, null)
        val userEmail: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_PREF_USEREMAIL, null)
        val authToken: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_AUTH_TOKEN, null)


        if (userEmail == null || pushToken == null || authToken == null) {
            Timber.d("callBackend push token sending failed --> userEmail: %s, pushToken: %s, authToken: %s", userEmail, pushToken, authToken)
            return
        }

        val jsonObject = createPushJsonData(authToken, userEmail, pushToken)


        sendDataToBackEnd(SSO_SEND_PUSH_TOKEN_URL_CONST, jsonObject.toString(), object : BackendCallBack {
            override fun onBackendCallBackResponse(responseBody: String?) {
                Timber.d("callBackend - sendPushToken correctly call")
            }

            override fun onBackendCallBackFailure(errorMessage: String?) {
                Timber.e("callBackend - sendPushToken error ->%s", errorMessage)
            }

        })
    }

    private fun createBackendUserJsonData(): JSONObject? {
        val pushToken: String? = pref.getSharedManagerInstance().getString(SHARED_KEY_PREF_PUSH_TOKEN, null)


        val jsonObject = JSONObject()
        try {
            jsonObject.put("uuid", uuid)
            jsonObject.put("app_version", packageVersion)
            jsonObject.put("os", systemVersion)
            if (pushToken != null) {                          // Se push token null non lo invio
                jsonObject.put("push_token", pushToken)
            }
            jsonObject.put("device_model", mobileBrandName)
        } catch (e: JSONException) {
            FirebaseCrashlytics.getInstance().log("Error creating JSON")
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
        return jsonObject
    }

    private fun createPushJsonData(authToken: String?, userEmail: String?, pushToken: String?): JSONObject? {

        val jsonObject = JSONObject()
        try {
            jsonObject.put("auth_token", authToken)
            jsonObject.put("uuid", uuid)
            jsonObject.put("user_email", userEmail)
            jsonObject.put("push_token", pushToken)
        } catch (e: JSONException) {
            FirebaseCrashlytics.getInstance().log("Error creating JSON")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        return jsonObject
    }


     fun sendDataToBackEnd(url: String, data: String, backendCallBack: BackendCallBack) {

        completeLogishiftEmployeeUrl = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
        Timber.d("callBackend - backend url pre replace %s", completeLogishiftEmployeeUrl)
        if (completeLogishiftEmployeeUrl != null) {
            if (completeLogishiftEmployeeUrl != null) {
                val logishiftBaseEndPointUrl = completeLogishiftEmployeeUrl!!.substring(0, completeLogishiftEmployeeUrl!!.indexOf("/app_frontend/"))

                Timber.d("callBackend - backend url after replace %s", logishiftBaseEndPointUrl)

                val requestBuilder: Request.Builder = Request.Builder().url(String.format("%s%s", logishiftBaseEndPointUrl, url))

                if (data.isNotEmpty()) {
                    requestBuilder.post(RequestBody.create(mediaTypeJson, data))

                    val request: Request = requestBuilder.build()
                    Timber.i("Sending data <[[%s]]> to backend API %s", data, request.url())

                    okHttp3Client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            FirebaseCrashlytics.getInstance().log("Error calling backend API(HttpClient on Failure)$url")
                            FirebaseCrashlytics.getInstance().recordException(e)
                            backendCallBack.onBackendCallBackFailure(e.localizedMessage)
                        }

                        override fun onResponse(call: Call, response: Response) {

                            try {
                                val jsonResponse = JSONObject(response.body().toString())
                                if (jsonResponse.getBoolean("success")) {
                                    backendCallBack.onBackendCallBackResponse(jsonResponse.toString())
                                } else {
                                    FirebaseCrashlytics.getInstance().log("Error parsing backend response from ($url) Response: $jsonResponse")
                                    backendCallBack.onBackendCallBackFailure("Error parsing backend response from ($url) Response: $jsonResponse")
                                }
                            } catch (exception: Exception) {
                                FirebaseCrashlytics.getInstance().log("Error calling backend API(" + url + ") errorMessage -> " + exception.localizedMessage)
                                FirebaseCrashlytics.getInstance().recordException(exception)
                                backendCallBack.onBackendCallBackFailure(exception.localizedMessage)
                            }

                        }

                    })
                }

            }


        }
    }


    private fun capitalize(str: String): String? {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        val arr = str.toCharArray()
        var capitalizeNext = true
        val phrase = StringBuilder()
        for (c in arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c))
                capitalizeNext = false
                continue
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true
            }
            phrase.append(c)
        }
        return phrase.toString()
    }

    private fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else capitalize(manufacturer) + " " + model
    }


 interface BackendCallBack {
        fun onBackendCallBackResponse(responseBody: String?)
        fun onBackendCallBackFailure(errorMessage: String?)
    }

}