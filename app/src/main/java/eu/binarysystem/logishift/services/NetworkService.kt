package eu.binarysystem.logishift.services

import com.firebase.jobdispatcher.JobParameters
import dagger.hilt.android.AndroidEntryPoint
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.BackendCommunicationManager
import eu.binarysystem.logishift.utilities.QueueManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NetworkService: com.firebase.jobdispatcher.JobService()    {
    @Inject
    lateinit var  pref: SharedPreferencesRetriever

    override fun onStartJob(job: JobParameters): Boolean {
        val poppedData: Pair<String, String>? = QueueManager.getInstance(applicationContext, pref).pop()
        if (poppedData!= null){
            BackendCommunicationManager.getInstance(applicationContext,pref).sendDataToBackEnd(poppedData.first, poppedData.second, object : BackendCommunicationManager.BackendCallBack {
                override fun onBackendCallBackResponse(responseBody: String?) {
                    jobFinished(job, false)
                }

                override fun onBackendCallBackFailure(errorMessage: String?) {
                    QueueManager.getInstance(applicationContext,pref).push(poppedData.first,poppedData.second)
                }

            })
        }
        else {
           return false
        }
        Timber.d("onStartJob %s", job.tag)

        return true
    }

    override fun onStopJob(job: JobParameters): Boolean {

        // Se il Job ha eseguito un thread separato, ritornare true e richiamare jobFinished()
        return true
    }

}