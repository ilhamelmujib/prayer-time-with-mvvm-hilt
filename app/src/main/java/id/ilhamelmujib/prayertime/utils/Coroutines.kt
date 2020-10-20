package id.ilhamelmujib.prayertime.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object Coroutines {

    fun main(work: suspend (CoroutineScope) -> Unit) =
        CoroutineScope(Dispatchers.Main).launch {
            work(this)
        }

    fun main(job: Job, work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Main + job).launch {
            work()
        }

    fun default(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Default).launch {
            work()
        }

    fun io(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO).launch {
            work()
        }

    fun io(job: Job, work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO + job).launch {
            work()
        }

}