package eu.binarysystem.logishift.hilt

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.binarysystem.logishift.utilities.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesRetriever @Inject constructor(@ApplicationContext val appContext: Context) {
    fun getSharedManagerInstance(): SharedPreferences {
        return appContext.getSharedPreferences(
            Constants.SHARED_PREFERENCES_INSTANCE_NAME_CONST,
            AppCompatActivity.MODE_PRIVATE
        )
    }

    fun getDefaultSharedEditor(): SharedPreferences.Editor {
    return  getSharedManagerInstance().edit()
    }
}