package eu.binarysystem.logishift.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.hilt.LocationManagerRetriever
import eu.binarysystem.logishift.utilities.GpsManager
import timber.log.Timber
import java.util.concurrent.BlockingDeque
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity() {

   lateinit var  gpsManager: GpsManager


    @Inject
    lateinit var locationManager: LocationManagerRetriever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)

        checkGpsLocation()
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
        gpsManager =  GpsManager.getInstance(this, locationManager.getLocationManager())
        Timber.d("Banana checkAtLeast %s",  gpsManager.checkAtLeastOneLocationProvidersAvailable())
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