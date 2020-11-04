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
import eu.binarysystem.logishift.hilt.LocationManager
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity() {
    @Inject lateinit var locationManagerInjector: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.webview_activity)



        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)

            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }


        locationManagerInjector.getLatitudeAndLongitude().lastLocation.addOnSuccessListener {
            println("Banana ${it.accuracy}")
            println(it.latitude)
            println(it.longitude)
        }.addOnFailureListener { println("Banana ${it.localizedMessage}") }




    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) ==
                                PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                        locationManagerInjector.getLatitudeAndLongitude().lastLocation.addOnSuccessListener {
                            println("Banana ${it.accuracy}")
                            println(it.latitude)
                            println(it.longitude)
                        }.addOnFailureListener { println("Banana ${it.localizedMessage}") }
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }

                return
            }
        }
        }



}