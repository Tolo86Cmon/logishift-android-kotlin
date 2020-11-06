package eu.binarysystem.logishift.utilities

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import timber.log.Timber

class ConnectionUtils private constructor(private val context: Context) {


    companion object {



        @Volatile
        private var CONNECTIONUTILSINSTANCE: ConnectionUtils? = null
        fun getInstance(context: Context): ConnectionUtils {
            if (CONNECTIONUTILSINSTANCE == null) {
                CONNECTIONUTILSINSTANCE = ConnectionUtils(context)
            }
            Timber.d("GPSMANAGER -> instance called")
            return CONNECTIONUTILSINSTANCE as ConnectionUtils
        }



    }

    fun isOnLine(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }
}