package eu.binarysystem.logishift.hilt

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class LocationManager @Inject constructor(@ActivityContext val context: Context) {
    fun getLatitudeAndLongitude(): FusedLocationProviderClient {
      return LocationServices.getFusedLocationProviderClient(context)
    }
}