package com.raywenderlich.android.locaty

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raywenderlich.android.locaty.LocatyService.Companion.KEY_NOTIFICATION_ID
import com.raywenderlich.android.locaty.LocatyService.Companion.KEY_NOTIFICATION_STOP_ACTION

class ActionListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null && intent.action != null) {
            // 1 Checks if the broadcast’s action is same as for Stop Notifications.
            if (intent.action.equals(KEY_NOTIFICATION_STOP_ACTION)) {
                context?.let {
                    // 2 Gets a reference to NotificationManager.
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val locatyIntent = Intent(context, LocatyService::class.java)
                    // 3 Stops the service.
                    context.stopService(locatyIntent)
                    val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, -1)
                    if (notificationId != -1) {
                        // 4 Removes the persistent notification from the Notification Drawer
                        notificationManager.cancel(notificationId)
                    }
                }
            }
        }
    }
}
