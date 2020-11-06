package eu.binarysystem.logishift.activities

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dk.nodes.filepicker.FilePickerActivity
import dk.nodes.filepicker.FilePickerConstants
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.hilt.LocationManagerRetriever
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.ConnectionUtils
import eu.binarysystem.logishift.utilities.Constants.Companion.BS_LOGISHIFT__MOBILE_BS
import eu.binarysystem.logishift.utilities.Constants.Companion.KEY_LOGISHIFT_URL
import eu.binarysystem.logishift.utilities.Constants.Companion.READ_UPLOADED_URI_CODE
import eu.binarysystem.logishift.utilities.Constants.Companion.URL_SERVER_ENVIROMEN_VARIABLE
import eu.binarysystem.logishift.utilities.GpsManager
import kotlinx.android.synthetic.main.webview_activity.*
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity() {

    lateinit var gpsManager: GpsManager
    var logishiftUrlEndPoint: String? = null
    private var directDomain: String? = null
    lateinit var webSettings: WebSettings


    @Inject
    lateinit var locationManager: LocationManagerRetriever

    @Inject
    lateinit var pref: SharedPreferencesRetriever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        webSettingsSetup()

        webViewSetup()

        evaluateDomainUrlVariables()

        checkGpsLocation()

    }


    private fun webSettingsSetup() {
        webSettings = main_web_view.settings
        webSettings.javaScriptEnabled = true
        webSettings.setAppCacheEnabled(true) // TODO: Considerarne la rimozione in vista della gestione dei Service Workers 06/11/2020
        webSettings.setAppCachePath(cacheDir.absolutePath)  // TODO: Considerarne la rimozione in vista della gestione dei Service Workers 06/11/2020
        webSettings.cacheMode = if (ConnectionUtils.getInstance(this)
                .isOnLine()
        ) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.domStorageEnabled = true
        webSettings.userAgentString = BS_LOGISHIFT__MOBILE_BS
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
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                val chooserFileIntent = Intent(applicationContext, FilePickerActivity::class.java)
                chooserFileIntent.putExtra(
                    FilePickerConstants.MULTIPLE_TYPES,
                    arrayOf(
                        FilePickerConstants.CAMERA,
                        "application/*",
                        FilePickerConstants.MIME_IMAGE
                    )
                )
                chooserFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(chooserFileIntent, READ_UPLOADED_URI_CODE)
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
                    Toast.makeText(
                        applicationContext,
                        R.string.no_app_available_to_open_document,
                        Toast.LENGTH_LONG
                    ).show()
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

            override fun onReceivedHttpError(
                view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
            ) {

                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {

                super.onReceivedError(view, request, error)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                return super.shouldOverrideUrlLoading(view, request)
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return super.shouldOverrideUrlLoading(view, url)
            }
        }

    }


    private fun evaluateDomainUrlVariables() {
        directDomain =
            pref.getSharedManagerInstance().getString(URL_SERVER_ENVIROMEN_VARIABLE, null)
        logishiftUrlEndPoint = pref.getSharedManagerInstance().getString(KEY_LOGISHIFT_URL, null)
    }


    private fun checkGpsLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                )

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                )
            }
        }
        gpsManager = GpsManager.getInstance(this, locationManager.getLocationManagerInstance())
        gpsManager.retrieveLocation()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()

                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }

                return
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gpsManager.retrieveLocation()
    }

    override fun onPause() {
        super.onPause()
        gpsManager.stopUsingGps()
    }


}