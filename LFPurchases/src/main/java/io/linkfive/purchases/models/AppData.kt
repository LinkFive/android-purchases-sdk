package io.linkfive.purchases.models

import android.content.Context
import java.util.*

data class AppData(
    val appVersion: String,
    val country: String
) {
    constructor(context: Context) : this(
        appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
        country = Locale.getDefault().country
    )

    constructor(): this(
        appVersion =  "0.0",
        country = Locale.getDefault().country
    )
}