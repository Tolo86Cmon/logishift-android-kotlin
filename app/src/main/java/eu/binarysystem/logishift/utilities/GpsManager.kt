package eu.binarysystem.logishift.utilities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import eu.binarysystem.logishift.R
import eu.binarysystem.logishift.activities.WebViewActivity
import eu.binarysystem.logishift.utilities.Constants.Companion.MIN_DISTANCE_CHANGE_FOR_UPDATES_CONST
import eu.binarysystem.logishift.utilities.Constants.Companion.MIN_TIME_BW_UPDATES_CONST
import timber.log.Timber

class GpsManager private constructor(private val context: Context, private val locationManager: LocationManager) : LocationListener {

    private var latitude: Double? = null
    private var longitude: Double? = null
    private var accuracy: Float? = null

    private var isGpsEnable: Boolean = false
    private var isWifiEnable: Boolean = false


    companion object {
        @Volatile
        private var GPSMANAGERISTANCE: GpsManager? = null

        fun getInstance(context: Context, locationManager: LocationManager): GpsManager {
            if (GPSMANAGERISTANCE == null) {
                GPSMANAGERISTANCE = GpsManager(context, locationManager)
            }
            Timber.d("GPSMANAGER -> instance called")
            return GPSMANAGERISTANCE as GpsManager
        }
    }


    fun retrieveLocation(): Boolean {
        Timber.d("GPSMANAGER -> get location called")
        return if (checkAtLeastOneLocationProvidersAvailable()) {
            evaluateCoordinatesVariables()
            true
        } else{
            false
        }
    }

    private fun evaluateCoordinatesVariables() {
        var wifiLocationAccuracy: Float? = null
        var wifiLocationLatitude: Double? = null
        var wifiLocationLongitude: Double? = null
        var gpsLocationAccuracy: Float? = null
        var gpsLocationLatitude: Double? = null
        var gpsLocationLongitude: Double? = null
        var isGpsCoordinatesValid = false
        var isWifiCoordinatesValid = false

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
               return
        }

        if (isWifiEnable) {
            requestSpecificLocationUpdate(NETWORK_PROVIDER)
            wifiLocationAccuracy = locationManager.getLastKnownLocation(NETWORK_PROVIDER)?.accuracy
            wifiLocationLatitude = locationManager.getLastKnownLocation(NETWORK_PROVIDER)?.latitude
            wifiLocationLongitude = locationManager.getLastKnownLocation(NETWORK_PROVIDER)?.longitude

            if (wifiLocationAccuracy != null && wifiLocationLatitude != null && wifiLocationLongitude != null) isWifiCoordinatesValid = true
        }
        if (isGpsEnable) {
            requestSpecificLocationUpdate(GPS_PROVIDER)
            gpsLocationAccuracy = locationManager.getLastKnownLocation(GPS_PROVIDER)?.accuracy
            gpsLocationLatitude = locationManager.getLastKnownLocation(GPS_PROVIDER)?.latitude
            gpsLocationLongitude = locationManager.getLastKnownLocation(GPS_PROVIDER)?.longitude
            if (gpsLocationAccuracy != null && gpsLocationLatitude != null && gpsLocationLongitude != null) isGpsCoordinatesValid = true
        }
        Timber.d("GPSMANAGER coordinates consistency:  isGpsCoordinatesValid-> %s  isWifiCoordinatesValid-> %s", isGpsCoordinatesValid, isWifiCoordinatesValid)
        when {
            isGpsCoordinatesValid && isWifiCoordinatesValid -> if (gpsLocationAccuracy!! < wifiLocationAccuracy!!) {
                accuracy = gpsLocationAccuracy
                latitude = gpsLocationLatitude
                longitude = gpsLocationLongitude
            } else {
                accuracy = wifiLocationAccuracy
                latitude = wifiLocationLatitude
                longitude = wifiLocationLongitude
            }

            !isGpsCoordinatesValid && isWifiCoordinatesValid -> {
                accuracy = wifiLocationAccuracy
                latitude = wifiLocationLatitude
                longitude = wifiLocationLongitude
            }

            else -> {
                accuracy = gpsLocationAccuracy
                latitude = gpsLocationLatitude
                longitude = gpsLocationLongitude
            }
        }
    }

    private fun requestSpecificLocationUpdate(provider: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {

        }

        locationManager.requestLocationUpdates(provider, MIN_TIME_BW_UPDATES_CONST, MIN_DISTANCE_CHANGE_FOR_UPDATES_CONST, this)
    }

      fun checkAtLeastOneLocationProvidersAvailable(): Boolean {
        return try {
            isWifiEnable = locationManager.isProviderEnabled(NETWORK_PROVIDER)
            isGpsEnable = locationManager.isProviderEnabled(GPS_PROVIDER)
            Timber.d("GPSMANAGER check provider availability  NETWORK-> %s GPS-> %s", isWifiEnable, isGpsEnable)
            return isGpsEnable || isWifiEnable


        } catch (exceptions: IllegalArgumentException) {
            false
        }
    }

    fun showSettingsAlert(activity: WebViewActivity) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(activity);

        // Setting Dialog Title
        alertDialog.setTitle(context.getString(R.string.gpsSettingsAlertDialogTitle));

        // Setting Dialog Message
        alertDialog.setMessage(context.getString(R.string.gpsSettingsAlertDialogMessage));

        // On pressing Settings button
        alertDialog.setPositiveButton(context.getString(R.string.gpsAlertDialogPositiveButtonText)) { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton(context.getString(R.string.gpsAlertDialogNegativeButtonText)) { dialog, _ ->
            dialog.cancel()
        }

        alertDialog.show()
    }
    fun getBetterLatitude(): Double? {
        return latitude
    }

    fun getBetterLongitude(): Double? {
        return longitude
    }

    fun getBetterAccuracy(): Float? {
        return accuracy
    }

    fun stopUsingGps(){
        Timber.d("GPSMANAGER stopUsing")
        locationManager.removeUpdates(this)
    }

    fun startUsingGps(){
        Timber.d("GPSMANAGER startUsingGps")
        retrieveLocation()
    }

    override fun onLocationChanged(location: Location) {

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }


}