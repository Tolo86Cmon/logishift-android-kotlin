package eu.binarysystem.logishift.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.hilt.android.AndroidEntryPoint
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import timber.log.Timber
import javax.inject.Inject


class NetworkWorker(var context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    val pref: SharedPreferencesRetriever = SharedPreferencesRetriever(context)

    override fun doWork(): Result {


        Timber.d("Worker called")
        val poppedData : Pair<String, String>? = QueueManager.getInstance(context, pref).pop();

        if (poppedData != null){
            Timber.d("Worker popped-data not null")
            BackendCommunicationManager.getInstance(context, pref).sendDataToBackEnd(poppedData.first, poppedData.second, object : BackendCommunicationManager.BackendCallBack {
                override fun onBackendCallBackFailure(errorMessage: String?) {
                    QueueManager.getInstance(context, pref).push(poppedData.first, poppedData.second);
                    Timber.e("Worker responseBody null re-push %s",errorMessage)
                }

                override fun onBackendCallBackResponse(responseBody: String?) {
                    Timber.d("Worker popped-data not null")
                }
            })
        }
        else{
            Timber.d("Worker popped data null")
        }

        return Result.success()
    }


}