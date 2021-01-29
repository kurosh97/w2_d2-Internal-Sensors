package com.raywenderlich.android.locaty

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.`w3_d2 Internal Storage`.android.locaty.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(
                broadcastReceiver,
                IntentFilter(LocatyService.KEY_ON_SENSOR_CHANGED_ACTION)
            )

    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 1. You retrieve and assign data to views.
            val direction = intent.getStringExtra(LocatyService.KEY_DIRECTION)
            val angle = intent.getDoubleExtra(LocatyService.KEY_ANGLE, 0.0)
            val angleWithDirection = "$angle  $direction"
            binding.directionTextView.text = angleWithDirection
            // 2 angle you get is in a counter-clockwise
            // direction and the views in Android rotate
            // in a clockwise manner, you need to mirror
            // the angle so that it becomes clockwise as well.
            // To do this, you multiply it by -1.
            binding.compassImageView.rotation = angle.toFloat() * -1
        }
    }


    override fun onResume() {
        super.onResume()
        startForegroundServiceForSensors(true)
    }

    private fun startForegroundServiceForSensors(background: Boolean) {
// 1.Create intent for service.
        val locatyIntent = Intent(this, LocatyService::class.java)
        locatyIntent.putExtra(LocatyService.KEY_BACKGROUND, background)
// 2.Starting foreground service.
        ContextCompat.startForegroundService(this, locatyIntent)

    }

    override fun onPause() {
        super.onPause()
        startForegroundServiceForSensors(false)
    }

    //This will unregister your BroadcastReceiver when it’s no longer needed.
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(broadcastReceiver)
    }
}
