package eu.binarysystem.logishift.hilt

import android.app.Application
import androidx.multidex.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class Application: Application(){
    override fun onCreate() {
        super.onCreate()

    }
}