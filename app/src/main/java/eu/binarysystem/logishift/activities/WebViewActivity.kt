package eu.binarysystem.logishift.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import dk.nodes.filepicker.FilePickerActivity
import dk.nodes.filepicker.FilePickerConstants
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.hilt.LocationManagerRetriever
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.jsInterfacesUtility.JsWebBackendInterfaceslocationManager
import eu.binarysystem.logishift.utilities.ConnectionUtils
import eu.binarysystem.logishift.utilities.Constants.Companion.BODY_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.CALL_JS_COMMAND_FROM_BROADCAST_INTENT_ACTION_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.FUNCTION_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.HTTP_SEPARATOR_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.INTENT_EXTRA_FUNCTION_NEWS_SHIFT
import eu.binarysystem.logishift.utilities.Constants.Companion.INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_DO_LOG_OUT_COMMAND
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_NEW_SHIFT_NOTIFICATION_COMMAND
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_NOT_AVAILABILITY_UPDATE_COMMAND
import eu.binarysystem.logishift.utilities.Constants.Companion.JS_OPEN_INFO_COMMAND
import eu.binarysystem.logishift.utilities.Constants.Companion.LOGI_SHIFT_MOBILE_USER_AGENT_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.PERMISSIONS_ARRAY_ENUM
import eu.binarysystem.logishift.utilities.Constants.Companion.PERMISSIONS_REQUEST_CODE_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.READ_UPLOADED_URI_CODE_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.RESET_VARIABLES
import eu.binarysystem.logishift.utilities.Constants.Companion.SAVE_VARIABLES
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_HTTP_SCHEMA
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_BASE64_ICON
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_URL
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_URL_SERVER_ENVIRONMENT
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_NOTIFICATION_PAYLOAD_MESSAGE
import eu.binarysystem.logishift.utilities.Constants.Companion.SSO_MAIN_AUTH_URL_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.STRINGS_TO_URL_SHOULD_BE_OVERRIDE_ARRAY
import eu.binarysystem.logishift.utilities.Constants.Companion.UPDATE_VARIABLES
import eu.binarysystem.logishift.utilities.GpsManager
import kotlinx.android.synthetic.main.webview_activity.*
import me.leolin.shortcutbadger.ShortcutBadger
import okhttp3.*
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection.HTTP_OK
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity() {
    @Inject lateinit var locationManager: LocationManagerRetriever
    @Inject lateinit var pref: SharedPreferencesRetriever


    lateinit var webSettings: WebSettings
    lateinit var gpsManager: GpsManager

    private var extraPayloadFunction: String? = null
    private var extraPayloadMessage: String? = null

    private var backgroundSharedPayloadMessage: String? = null
    private var backgroundSharedNewShiftCounter: Int = 0
    private var backgroundSharedNotAvailabilityCounter: Int = 0


    private lateinit var multiFunctionBroadcastReceiver: BroadcastReceiver

    var logishiftUrlEndPoint: String? = null
    var environmentSSOUrl: String? = null
    var base64StringIcon: String? = null
    var lastHttpSchemeUsed: String? = null

    var grantResults = intArrayOf(0, 0, 0)

    var isHttpErrorOccurred: Boolean = false
    var isCallingEditServer: Boolean = false
    var isApplicationInForeGround: Boolean = false

    var latitude: Double? = 0.0
    var longitude: Double? = 0.0
    var accuracy: Float? = 0.0F


    //AppCompatActivity Cycle method

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        setContentView(R.layout.webview_activity)

        checkAppPermissions()



        setFirebaseMessagingPayloadParameterExtra()

    }

    override fun onDestroy() {
        unregisterReceiver(multiFunctionBroadcastReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        setActivityVisibility(true)
        if (checkFineLocationPermission()) {
            updateGpsLocation()
        }
        Timber.d("OnResume updateHttpLayoutSchema PRE update variables ")
        updateHttpLayoutSchema()

        setNotificationBackGroundMessageAndCounter()

        callBackEndJavascriptCommandsIfNeeded()

        resetNotificationBackGroundMessageAndCounter()

    }

    override fun onPause() {
        super.onPause()
        if (checkFineLocationPermission()) {
            GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
            gpsManager.stopUsingGps()
        }

        setActivityVisibility(false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_CONST -> {

                if (grantResults.isNotEmpty()) {
                    if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        updateGpsLocation()
                        appSetup()
                    }

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    }

                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    }

                    Toast.makeText(this, getString(R.string.mainActivityPermissionGrantedText), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.mainActivityPermissionDeniedText), Toast.LENGTH_SHORT).show()
                }

                return
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Timber.d("onCreateOptionsMenu")
        return if (!logishiftUrlEndPoint.isNullOrEmpty()) {
            menuInflater.inflate(R.menu.manage_server_logging_menu, menu)
            true
        } else if (environmentSSOUrl.isNullOrEmpty()) {
            false
        } else {
            menuInflater.inflate(R.menu.just_log_out_menu, menu)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_logout -> {
                isCallingEditServer = false
                callJavascript(JS_DO_LOG_OUT_COMMAND)
            }
            R.id.action_info -> {
                callJavascript(JS_OPEN_INFO_COMMAND)
            }
            R.id.action_change_server -> {
                isCallingEditServer = true
                if (lastHttpSchemeUsed != null) {
                    server_scheme_edit_text.setText(lastHttpSchemeUsed)
                }

                if (!logishiftUrlEndPoint.isNullOrEmpty()) {
                    callJavascript(JS_DO_LOG_OUT_COMMAND)
                } else {
                    resetDomainAndLayout()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }


    //BackupMethod

    private fun appSetup() {
        setDisplayActionBarSetup()
        webSettingsSetup()
        webViewSetup()
        addListenerSetup()
        registerReceiver(multiFunctionBroadcastReceiver, IntentFilter(CALL_JS_COMMAND_FROM_BROADCAST_INTENT_ACTION_CONST))
        showCorrectAppLayoutByDomainUrl()
    }


    fun resetDomainAndLayout() {
        Timber.d("resetDomainAndLayout")
        updateSharedVariablesManager(RESET_VARIABLES)
        sso_server_edit_text.setText("")
        error_linear_layout.visibility = View.GONE
        showCorrectAppLayoutByDomainUrl()
        invalidateOptionsMenu()
        Toast.makeText(applicationContext, R.string.server_successfully_logged_out, Toast.LENGTH_SHORT).show()
    }


    private fun addListenerSetup() {
        check_sso_url_button.setOnClickListener {
            environmentSSOUrl = sso_server_edit_text.text.toString()
            ssoServerUrlManager(server_scheme_edit_text.text.toString())
        }

        change_url_schema_button.setOnClickListener {
            if (!server_scheme_edit_text.isEnabled) {
                server_scheme_edit_text.isEnabled = true
                change_url_schema_button.text = resources.getString(R.string.action_save_url_schema)
            } else {
                if (server_scheme_edit_text.text.isNotEmpty() && (server_scheme_edit_text.text.toString() == "https" || server_scheme_edit_text.text.toString() == "https")) {
                    server_scheme_edit_text.isEnabled = false
                    change_url_schema_button.text = resources.getString(R.string.action_change_url_schema)
                } else {
                    Toast.makeText(applicationContext, R.string.server_schema_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        multiFunctionBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.d("INTENT CALLED %s", intent?.action)
            }

        }


    }


    fun setActivityVisibility(isApplicationInForeGround: Boolean) {
        this.isApplicationInForeGround = isApplicationInForeGround
    }

    private fun setDisplayActionBarSetup() {
        supportActionBar?.displayOptions = DISPLAY_USE_LOGO or DISPLAY_SHOW_HOME or DISPLAY_SHOW_TITLE
        resetActionBarIcon(this)
    }

    fun resetActionBarIcon(webViewActivity: WebViewActivity) {
        if (base64StringIcon != null) {
            val biteDecodedImageFromString: ByteArray = Base64.decode(base64StringIcon, Base64.DEFAULT)
            val bitmapImageFromDecodedByte: Bitmap = BitmapFactory.decodeByteArray(biteDecodedImageFromString, 0, biteDecodedImageFromString.size)
            val resultedDrawableFromBitmap: Drawable = BitmapDrawable(webViewActivity.resources, bitmapImageFromDecodedByte)
            supportActionBar?.setIcon(resultedDrawableFromBitmap)
        } else {
            supportActionBar?.setIcon(R.mipmap.ic_launcher)
        }
    }

    fun callJavascript(javascriptStringCommand: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            main_web_view.evaluateJavascript(javascriptStringCommand, null)
        } else {
            main_web_view.loadUrl("javascript:$javascriptStringCommand")
        }
    }


    private fun ssoServerUrlManager(serverScheme: String) {
        login_progress_bar.visibility = View.VISIBLE
        if (!server_scheme_edit_text.isEnabled) run {
            try {
                val okHttp3Client = OkHttpClient()

                val okHttp3Request = Request.Builder().url(String.format("%s%s%s%s", serverScheme, HTTP_SEPARATOR_CONST, environmentSSOUrl, SSO_MAIN_AUTH_URL_CONST)).build()

                okHttp3Client.newCall(okHttp3Request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        this@WebViewActivity.runOnUiThread {
                            login_progress_bar.visibility = View.INVISIBLE
                            Toast.makeText(applicationContext, R.string.server_sso_login_error, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code() == HTTP_OK) {
                            updateSharedVariablesManager(SAVE_VARIABLES, hashMapOf(SHARED_HTTP_SCHEMA to serverScheme, SHARED_KEY_URL_SERVER_ENVIRONMENT to environmentSSOUrl))
                            this@WebViewActivity.runOnUiThread {
                                login_progress_bar.visibility = View.INVISIBLE
                                showCorrectAppLayoutByDomainUrl()
                                Toast.makeText(applicationContext, R.string.server_sso_login_successful, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            this@WebViewActivity.runOnUiThread {
                                login_progress_bar.visibility = View.INVISIBLE
                                Toast.makeText(applicationContext, R.string.server_sso_login_error, Toast.LENGTH_SHORT).show();

                            }
                        }
                    }

                })
            } catch (exception: Exception) {
                login_progress_bar.visibility = View.INVISIBLE
                Toast.makeText(applicationContext, R.string.server_sso_login_error, Toast.LENGTH_SHORT).show()
                FirebaseCrashlytics.getInstance().log("Error calling SSO login (" + String.format("%s%s%s%s", serverScheme, "://", environmentSSOUrl, SHARED_KEY_URL_SERVER_ENVIRONMENT) + ") ErrorMessage ->" + exception.getLocalizedMessage())
                FirebaseCrashlytics.getInstance().recordException(exception)
            }


        } else {
            login_progress_bar.visibility = View.INVISIBLE
            Toast.makeText(applicationContext, R.string.save_schema_message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCorrectAppLayoutByDomainUrl() {
        Timber.d("showCorrectAppLayoutByDomainUrl  environmentSSOUrl ->%s logishiftUrlEndPoint ->%s", environmentSSOUrl, logishiftUrlEndPoint)
        updateSharedVariablesManager(UPDATE_VARIABLES, hashMapOf(SHARED_KEY_URL_SERVER_ENVIRONMENT to environmentSSOUrl, SHARED_KEY_LOGI_SHIFT_URL to logishiftUrlEndPoint))
        if (environmentSSOUrl.isNullOrEmpty()) {
            main_web_view.visibility = View.GONE
            server_url_linear_layout.visibility = View.VISIBLE
        } else {
            main_web_view.visibility = View.VISIBLE
            server_url_linear_layout.visibility = View.GONE
            if (!logishiftUrlEndPoint.isNullOrEmpty()) {
                main_web_view.loadUrl(logishiftUrlEndPoint!!)
            } else {
                main_web_view.loadUrl(String.format("%s%s%s%s", server_scheme_edit_text.text.toString(), HTTP_SEPARATOR_CONST, environmentSSOUrl, SSO_MAIN_AUTH_URL_CONST))
            }
        }

        invalidateOptionsMenu()
    }


    private fun setNotificationBackGroundMessageAndCounter() {
        backgroundSharedPayloadMessage = pref.getSharedManagerInstance().getString(SHARED_NOTIFICATION_PAYLOAD_MESSAGE, null)
        backgroundSharedNewShiftCounter = pref.getSharedManagerInstance().getInt(SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER, 0)
        backgroundSharedNotAvailabilityCounter = pref.getSharedManagerInstance().getInt(SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER, 0)

    }

    private fun resetNotificationBackGroundMessageAndCounter() {
        pref.getDefaultSharedEditor().putString(SHARED_NOTIFICATION_PAYLOAD_MESSAGE, null)
        pref.getDefaultSharedEditor().putInt(SHARED_NOTIFICATION_NEW_SHIFT_BACKGROUND_COUNTER, 0)
        pref.getDefaultSharedEditor().putInt(SHARED_NOTIFICATION_NOT_AVAILABILITY_BACKGROUND_COUNTER, 0)

    }


    private fun setFirebaseMessagingPayloadParameterExtra() {
        val extraFirebasePayLoadIntent: Bundle? = intent.extras
        if (extraFirebasePayLoadIntent != null) {
            if (extraFirebasePayLoadIntent.containsKey(FUNCTION_CONST)) {
                extraPayloadFunction = extraFirebasePayLoadIntent.getString(FUNCTION_CONST)
            }
            if (extraFirebasePayLoadIntent.containsKey(BODY_CONST)) {
                extraPayloadMessage = extraFirebasePayLoadIntent.getString(BODY_CONST)
            }
        }
    }

    private fun callBackEndJavascriptCommandsIfNeeded() {
        if (extraPayloadFunction != null) {
            if (extraPayloadFunction == INTENT_EXTRA_FUNCTION_NEWS_SHIFT) {
                callJavascript(JS_NEW_SHIFT_NOTIFICATION_COMMAND)
            } else if (extraPayloadFunction == INTENT_EXTRA_FUNCTION_NOT_AVAILABILITY) {
                if (extraPayloadMessage != null) {
                    callJavascript(JS_NOT_AVAILABILITY_UPDATE_COMMAND(extraPayloadMessage))
                }
            }
        } else if (backgroundSharedNewShiftCounter > 0) {
            callJavascript(JS_NEW_SHIFT_NOTIFICATION_COMMAND)
            ShortcutBadger.removeCount(applicationContext)
        } else if (backgroundSharedNotAvailabilityCounter > 0 && backgroundSharedPayloadMessage != null) {
            callJavascript(JS_NOT_AVAILABILITY_UPDATE_COMMAND(backgroundSharedPayloadMessage))
            ShortcutBadger.removeCount(applicationContext)
        }
    }

    private fun webSettingsSetup() {
        webSettings = main_web_view.settings
        webSettings.javaScriptEnabled = true
        webSettings.setAppCacheEnabled(true) // TODO: Considerarne la rimozione in vista della gestione dei Service Workers 06/11/2020
        webSettings.setAppCachePath(cacheDir.absolutePath)  // TODO: Considerarne la rimozione in vista della gestione dei Service Workers 06/11/2020
        webSettings.cacheMode = if (ConnectionUtils.getInstance(this).isOnLine()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.domStorageEnabled = true
        webSettings.userAgentString = LOGI_SHIFT_MOBILE_USER_AGENT_CONST
        webSettings.setSupportMultipleWindows(false)
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.loadsImagesAutomatically = true
        webSettings.allowFileAccess = true
        webSettings.databaseEnabled = true
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
    }

    private fun webViewSetup() {
        main_web_view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                val chooserFileIntent = Intent(applicationContext, FilePickerActivity::class.java)
                chooserFileIntent.putExtra(FilePickerConstants.MULTIPLE_TYPES, arrayOf(FilePickerConstants.CAMERA, "application/*", FilePickerConstants.MIME_IMAGE))
                chooserFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(chooserFileIntent, READ_UPLOADED_URI_CODE_CONST)
                return true
            }
        }

        main_web_view.addJavascriptInterface(JsWebBackendInterfaceslocationManager(applicationContext, this, gpsManager, locationManager, pref), "Android")

        main_web_view.setDownloadListener { url, _, _, _, _ ->
            try {

                val intent = Intent(ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(applicationContext, R.string.no_app_available_to_open_document, Toast.LENGTH_LONG).show()
                }
            }

        }

        main_web_view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isHttpErrorOccurred = false;
                Timber.d("MainWebView onPageStarted - isHttpErrorOccurred -> %s", isHttpErrorOccurred)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                //Se l'errore http è avvenuto prima del termine del caricamento della pagina lo ignoro e resetto il flag a false
                if (isHttpErrorOccurred) {
                    error_linear_layout.visibility = View.GONE
                }
                isHttpErrorOccurred = false

                callBackEndJavascriptCommandsIfNeeded()

                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                isHttpErrorOccurred = true
                Timber.d("MainWebView onPageStarted - errorResponse -> %s", errorResponse)
                super.onReceivedHttpError(view, request, errorResponse)
            }


            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                isHttpErrorOccurred = true
                Timber.d("MainWebView onReceivedError general - errorResponse -> %s", description)
                if (description != "net::ERR_FAILED") {
                    showHttpErrorLayout()
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                isHttpErrorOccurred = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error!!.description != "net::ERR_FAILED"
                    showHttpErrorLayout()
                }
                super.onReceivedError(view, request, error)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Timber.d("MainWebView shouldOverrideUrlLoading new old URL %s", url)
                return handleHttpUriBeforeRedirecting(Uri.parse(url))
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP) override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Timber.d("MainWebView shouldOverrideUrlLoading new HOST %s SCHEMA %s", request?.url?.host, request?.url?.scheme)
                return handleHttpUriBeforeRedirecting(request?.url)
            }
        }

    }

    private fun handleHttpUriBeforeRedirecting(uri: Uri?): Boolean {
        val host: String? = uri?.host
        val scheme: String? = uri?.scheme

        if (host != null && scheme != null) {
            for (string: String in STRINGS_TO_URL_SHOULD_BE_OVERRIDE_ARRAY) {
                if (!host.contains(string)) {
                    continue
                } else {
                    return if (string == "tel") {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = uri
                        startActivity(intent)
                        true
                    } else {
                        val intent = Intent(ACTION_VIEW)
                        intent.data = uri
                        startActivity(intent)
                        true
                    }
                }
            }
            return false
        } else {
            return false
        }
    }


     fun updateSharedVariablesManager(function: String, variablesMap: HashMap<String, String?> = hashMapOf()) {
        Timber.d("updateSharedVariables isCallingReset  %s", function)
        when (function) {
            RESET_VARIABLES -> {
                pref.getDefaultSharedEditor().putString(SHARED_KEY_URL_SERVER_ENVIRONMENT, null).apply()
                pref.getDefaultSharedEditor().putString(SHARED_KEY_LOGI_SHIFT_URL, null).apply()
                pref.getDefaultSharedEditor().putString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null).apply()
                pref.getDefaultSharedEditor().putString(SHARED_HTTP_SCHEMA, null).apply()

                environmentSSOUrl = pref.getSharedManagerInstance().getString(SHARED_KEY_URL_SERVER_ENVIRONMENT, null)
                logishiftUrlEndPoint = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
                base64StringIcon = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null)
                lastHttpSchemeUsed = pref.getSharedManagerInstance().getString(SHARED_HTTP_SCHEMA, null)
            }
            UPDATE_VARIABLES -> {
                for ((key) in variablesMap) {
                    when (key) {
                        SHARED_KEY_URL_SERVER_ENVIRONMENT -> environmentSSOUrl = pref.getSharedManagerInstance().getString(SHARED_KEY_URL_SERVER_ENVIRONMENT, null)
                        SHARED_KEY_LOGI_SHIFT_URL -> logishiftUrlEndPoint = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
                        SHARED_KEY_LOGI_SHIFT_BASE64_ICON -> base64StringIcon = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null)
                        SHARED_HTTP_SCHEMA -> lastHttpSchemeUsed = pref.getSharedManagerInstance().getString(SHARED_HTTP_SCHEMA, null)
                    }
                }
            }
            SAVE_VARIABLES -> {
                for ((key, value) in variablesMap) {
                    pref.getDefaultSharedEditor().putString(key, value).apply()
                    when (key) {
                        SHARED_KEY_URL_SERVER_ENVIRONMENT -> environmentSSOUrl = pref.getSharedManagerInstance().getString(SHARED_KEY_URL_SERVER_ENVIRONMENT, null)
                        SHARED_KEY_LOGI_SHIFT_URL -> logishiftUrlEndPoint = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
                        SHARED_KEY_LOGI_SHIFT_BASE64_ICON -> base64StringIcon = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null)
                        SHARED_HTTP_SCHEMA -> lastHttpSchemeUsed = pref.getSharedManagerInstance().getString(SHARED_HTTP_SCHEMA, null)
                    }
                }
            }
        }






        Timber.d("updateSharedVariables environmentSSOUrl %s logishiftUrlEndPoint %s base64StringIcon %s,lastHttpSchemeUsed %s ", environmentSSOUrl, logishiftUrlEndPoint, base64StringIcon, lastHttpSchemeUsed)
    }

    private fun checkFineLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }


    private fun checkAppPermissions() {
        grantResults
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, PERMISSIONS_ARRAY_ENUM, PERMISSIONS_REQUEST_CODE_CONST);

        } else {
            onRequestPermissionsResult(PERMISSIONS_REQUEST_CODE_CONST, PERMISSIONS_ARRAY_ENUM, grantResults)
        }

    }

    private fun showHttpErrorLayout() {
        check_sso_url_button.visibility = View.GONE
        server_url_linear_layout.visibility = View.GONE
        error_linear_layout.visibility = View.VISIBLE
    }


    private fun updateGpsLocation() {
        gpsManager = GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
        if (!gpsManager.retrieveLocation()) {
            gpsManager.showSettingsAlert(this)
            latitude = gpsManager.getBetterLatitude()
            longitude = gpsManager.getBetterLongitude()
            accuracy = gpsManager.getBetterAccuracy()
        }

    }


    private fun updateHttpLayoutSchema() {
        updateSharedVariablesManager(UPDATE_VARIABLES, hashMapOf(SHARED_HTTP_SCHEMA to lastHttpSchemeUsed))
        if (lastHttpSchemeUsed != null) {
            server_scheme_edit_text.setText(lastHttpSchemeUsed)
        }
    }


}