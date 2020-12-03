package eu.binarysystem.logishift.utilities

import android.content.Context
import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_QUEUE_MANAGER_QUEUE
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_QUEUE_MANAGER_QUEUE_URL
import timber.log.Timber
import java.lang.Exception


class QueueManager( private val pref: SharedPreferencesRetriever) {
    private var queueUrl: ArrayDeque<String> = ArrayDeque(pref.getSharedManagerInstance().getStringSet(SHARED_QUEUE_MANAGER_QUEUE_URL, HashSet<String>(0)) as Collection<String>)
    private var queue: ArrayDeque<String> = ArrayDeque(pref.getSharedManagerInstance().getStringSet(SHARED_QUEUE_MANAGER_QUEUE, HashSet<String>(0)) as Collection<String>)


    companion object {
        @Volatile private var QUEUMANAGERINSTANCE: QueueManager? = null
        fun getInstance(context: Context, pref: SharedPreferencesRetriever): QueueManager {
            if (QUEUMANAGERINSTANCE == null) {
                QUEUMANAGERINSTANCE = QueueManager(pref)
            }
            return QUEUMANAGERINSTANCE as QueueManager
        }
    }


    fun push(url: String, element: String) {
        queueUrl.add(url)
        queue.add(element)

        pref.getDefaultSharedEditor().putStringSet(SHARED_QUEUE_MANAGER_QUEUE_URL,HashSet<String>(queueUrl))
        pref.getDefaultSharedEditor().putStringSet(SHARED_QUEUE_MANAGER_QUEUE,HashSet<String>(queue))
        Timber.d( "Pushed 1 element, %d elements in queue", queue.size)
    }

    fun pop():Pair<String,String>?{
        val poppedUrl: String
        val popped: String
        return try {
            poppedUrl = queueUrl.first()
            popped = queue.removeFirst()
            pref.getDefaultSharedEditor().putStringSet(SHARED_QUEUE_MANAGER_QUEUE_URL,HashSet<String>(queueUrl))
            pref.getDefaultSharedEditor().putStringSet(SHARED_QUEUE_MANAGER_QUEUE,HashSet<String>(queue))
            return Pair(poppedUrl,popped)
        } catch (exceptions: Exception){
            Timber.e( "Pop exceptions %s", exceptions.localizedMessage)
            null
        }



    }


}