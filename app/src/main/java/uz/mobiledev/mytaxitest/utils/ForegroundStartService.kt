package uz.mobiledev.mytaxitest.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import uz.mobiledev.mytaxitest.tools.LocationForegroundService

fun Context.foregroundStartService(context: Context) {
    if (!isMyServiceRunning(context.applicationContext,LocationForegroundService::class.java)) {
        Intent(context.applicationContext, LocationForegroundService::class.java).apply {
            context.applicationContext.startService(this)
        }
    }
}

private fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}