package com.sys4soft.deldia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
//        val i = Intent(context, MainActivity::class.java)
////        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        val pendingIntent = PendingIntent.getActivity(context, 0, i, (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
//
//        val builder = NotificationCompat.Builder(context!!, "foxy")
//            .setSmallIcon(R.drawable.ic_baseline_lock_24)
//            .setContentTitle("Foxy alarm manager")
//            .setContentText("subcribete")
//            .setAutoCancel(true)
//            .setDefaults(NotificationCompat.DEFAULT_ALL)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
//
//        val notificationManager = NotificationManagerCompat.from(context)
//        notificationManager.notify(123, builder.build())
        Log.d("Mike", "Alarm just fired")
    }

}