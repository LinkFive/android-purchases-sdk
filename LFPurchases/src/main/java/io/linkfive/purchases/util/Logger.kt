package io.linkfive.purchases.util

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object Logger {

    val logLiveData = MutableLiveData<String>()
    val debug = true

    fun d(vararg msg: Any?) {
        msg.forEach {
            Log.d("LinkFive", it.toString())
            postValue(it.toString())
        }
    }

    fun v(vararg msg: Any?) {
        msg.forEach {
            Log.v("LinkFive", it.toString())
            postValue(it.toString())
        }
    }

    fun e(vararg msg: Any?) {
        msg.forEach {
            Log.e("LinkFive", it.toString())
            postValue(it.toString())
        }
    }

    private fun postValue(msg: String) {
        if (!debug) {
            return
        }
        GlobalScope.launch(context = Dispatchers.Main) {
            logLiveData.value = msg
        }
    }
}
