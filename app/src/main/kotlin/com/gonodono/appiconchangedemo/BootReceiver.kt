package com.gonodono.appiconchangedemo

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager


/**
 * Created by Charles Manalo on 10/01/2025
 */


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            println("onReceive: Boot completed")
            val packageApp  = context.packageName

            val p: PackageInfo? = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)

            for (activityInfo in p!!.activities!!) {
                println("ACT " + activityInfo.name + " " + activityInfo.packageName)
            }
            val intent = Intent().apply {
                component = ComponentName(packageApp, "$packageApp.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_LAUNCHER)
                putExtra("LaunchApp", true)
            }
            context.startActivity(intent)
            println("Intent will bootReceiver $intent")
        }
    }
}