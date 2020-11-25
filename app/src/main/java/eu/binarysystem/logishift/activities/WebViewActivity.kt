package eu.binarysystem.logishift.activities

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import dk.nodes.filepicker.FilePickerActivity
import dk.nodes.filepicker.FilePickerConstants
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.hilt.LocationManagerRetriever
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.ConnectionUtils
import eu.binarysystem.logishift.utilities.Constants.Companion.BODY_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.FUNCTION_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.LOGI_SHIFT_MOBILE_USER_AGENT_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.PERMISSIONS_ARRAY_ENUM
import eu.binarysystem.logishift.utilities.Constants.Companion.PERMISSIONS_REQUEST_CODE_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.READ_UPLOADED_URI_CODE_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_BASE64_ICON
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_LOGI_SHIFT_URL
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_URL_SERVER_ENVIRONMENT
import eu.binarysystem.logishift.utilities.GpsManager
import kotlinx.android.synthetic.main.webview_activity.*
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity() {
    lateinit var webSettings: WebSettings
    lateinit var gpsManager: GpsManager
    private var extraPayloadIntentFunction: String? = null
    private var extraPayloadIntentBody: String? = null
    var grantResults = intArrayOf(0, 0, 0)


    var logishiftUrlEndPoint: String? = null
    var environmentUrl: String? = null
    var base64StringIcon: String? = null


    @Inject lateinit var locationManager: LocationManagerRetriever

    @Inject lateinit var pref: SharedPreferencesRetriever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        appSetup()

        checkAppPermissions()

        evaluateSharedVariables()

        setFirebasePayloadParameterExtra()


    }

    private fun appSetup() {
        Timber.plant(DebugTree())
        setDisplayActionBarSetup()
        webSettingsSetup()
        webViewSetup()
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


    private fun showCorrectAppLayoutByDomainUrl(baseDomainUrl: String, logishiftAppUrl: String) {

        if (baseDomainUrl.isEmpty()) {
            main_web_view.visibility = View.GONE
            server_url_linear_layout.visibility = View.VISIBLE
        } else {

        }
    }


    private fun setFirebasePayloadParameterExtra() {
        val extraFirebasePayLoadIntent: Bundle? = intent.extras
        if (extraFirebasePayLoadIntent != null) {
            if (extraFirebasePayLoadIntent.containsKey(FUNCTION_CONST)) {
                extraPayloadIntentFunction = extraFirebasePayLoadIntent.getString(FUNCTION_CONST)
            }
            if (extraFirebasePayLoadIntent.containsKey(BODY_CONST)) {
                extraPayloadIntentBody = extraFirebasePayLoadIntent.getString(BODY_CONST)
            }


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

        main_web_view.setDownloadListener { url, _, _, _, _ ->
            try {

                var intent = Intent(ACTION_VIEW)
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

                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {

                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {

                super.onReceivedError(view, request, error)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {

                return super.shouldOverrideUrlLoading(view, request)
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return super.shouldOverrideUrlLoading(view, url)
            }
        }

    }

    private fun evaluateSharedVariables() {
        environmentUrl = pref.getSharedManagerInstance().getString(SHARED_KEY_URL_SERVER_ENVIRONMENT, null)
        logishiftUrlEndPoint = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_URL, null)
        base64StringIcon = pref.getSharedManagerInstance().getString(SHARED_KEY_LOGI_SHIFT_BASE64_ICON, null)
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_CONST -> {

                if (grantResults.isNotEmpty()) {
                    if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        updateGpsLocation()
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

    private fun updateGpsLocation() {
        gpsManager = GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
        if (!gpsManager.retrieveLocation()){
            gpsManager.showSettingsAlert(this)
        }

    }

    override fun onResume() {
        super.onResume()
        if (checkFineLocationPermission()) {
            GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
            gpsManager.startUsingGps()
        }
    }

    override fun onPause() {
        super.onPause()
        if (checkFineLocationPermission()) {
            GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
            gpsManager.stopUsingGps()
        }

    }


}