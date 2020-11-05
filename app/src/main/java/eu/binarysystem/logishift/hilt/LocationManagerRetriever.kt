package eu.binarysystem.logishift.hilt

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.LocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocationManagerRetriever @Inject constructor(@ApplicationContext val appContext: Context)  {
    fun getLocationManager(): LocationManager{
      return  appContext.getSystemService(LOCATION_SERVICE) as LocationManager
    }
}