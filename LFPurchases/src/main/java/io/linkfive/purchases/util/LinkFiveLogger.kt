package io.linkfive.purchases.util

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object LinkFiveLogger {
    var logLevel = LinkFiveLogLevel.DEBUG

    val logLiveData = MutableLiveData<String>()

    fun d(vararg msg: Any?) {
        if(logLevel.i <= LinkFiveLogLevel.DEBUG.i) {
            msg.forEach {
                Log.d("LinkFive", it.toString())
                postValue(it.toString())
            }
        }
    }

    fun v(vararg msg: Any?) {
        if(logLevel.i <= LinkFiveLogLevel.TRACE.i) {
            msg.forEach {
                Log.v("LinkFive", it.toString())
                postValue(it.toString())
            }
        }
    }

    fun e(vararg msg: Any?) {
        if(logLevel.i <= LinkFiveLogLevel.ERROR.i) {
            msg.forEach {
                Log.e("LinkFive", it.toString())
                postValue(it.toString())
            }
        }
    }

    private fun postValue(msg: String) {
        GlobalScope.launch(context = Dispatchers.Main) {
            logLiveData.value = msg
        }
    }
}

enum class LinkFiveLogLevel(val i: Int) {
    TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4)
}