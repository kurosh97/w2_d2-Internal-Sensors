package com.raywenderlich.android.locaty

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.math.round

class LocatyService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var background = false

    //These variables will hold the latest accelerometer and magnetometer values.
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    //These two arrays will hold the values of the rotation matrix and orientation angles.
    // You’ll learn more about them soon.
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    //These are the request codes you use when creating a PendingIntent.
    // Each PendingIntent should have a unique request code.
    private val notificationActivityRequestCode = 0
    private val notificationId = 1
    private val notificationStopRequestCode = 2


    //These are keys that will be used to send data from LocatyService to MainActivity.
    companion object {
        val KEY_ANGLE = "angle"
        val KEY_DIRECTION = "direction"
        val KEY_BACKGROUND = "background"
        val KEY_NOTIFICATION_ID = "notificationId"
        val KEY_ON_SENSOR_CHANGED_ACTION = "com.raywenderlich.android.locaty.ON_SENSOR_CHANGED"
        val KEY_NOTIFICATION_STOP_ACTION = "com.raywenderlich.android.locaty.NOTIFICATION_STOP"
    }


    override fun onCreate() {
        super.onCreate()
        //1.Initializes SensorManager.
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        //2. Registers a sensor event callback to listen to changes in the accelerometer
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI
            )
        }
        //3. Registers a sensor event callback to listen to changes in the magnetometer.
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this, magneticField,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI
            )
        }

        // 1 Create a notification
        val notification = createNotification(getString(R.string.not_available), 0.0)
        // 2 Start the service with the notification as a Foreground Service
        startForeground(notificationId, notification)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //1.If the event is null, then simply return
        if (event == null) {

        }
        //2.Check the type of sensor
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            //3.System.arrayCopy copies values from the sensors into its respective array.
            System.arraycopy(
                event.values, 0, accelerometerReading, 0, accelerometerReading.size
            )
        } else if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(
                event.values, 0, magnetometerReading, 0, magnetometerReading.size
            )
        }

        updateOrientationAngles()
    }

    fun updateOrientationAngles() {
        //1.First, it gets the rotation matrix.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        //2.It then uses that rotation matrix, which consists of an array of nine values,
        // and maps it to a usable matrix with three values.
        val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
        //3.Next, it converts the azimuth to degrees, adding 360 because the angle is always positive.
        val degrees = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
        //4.Finally, it rounds the angle up to two decimal places.
        val angle = round(degrees * 100) / 100

        val direction = getDirection(degrees)

        //1.Create an intent object and put data in it with respect to its keys.
        val intent = Intent()
        intent.putExtra(KEY_ANGLE, angle)
        intent.putExtra(KEY_DIRECTION, direction)
        intent.action = KEY_ON_SENSOR_CHANGED_ACTION
        //2.You then send out a local broadcast with the intent
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        if (background) {
            // 1Creates and shows a notification when the app goes into the background.
            val notification = createNotification(direction, angle)
            startForeground(notificationId, notification)
        } else {
            // 2 Hides the notification as soon as the app comes into the foreground.
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            //1.Gets the application state from MainActivity,
            // which you pass when you start the service.
            background = it.getBooleanExtra(KEY_BACKGROUND, false)
        }
        return START_STICKY
    }

    //etermine which direction the user is facing
    private fun getDirection(angle: Double): String {
        var direction = ""

        if (angle >= 350 || angle <= 10)
            direction = "N"
        if (angle < 350 && angle > 280)
            direction = "NW"
        if (angle <= 280 && angle > 260)
            direction = "W"
        if (angle <= 260 && angle > 190)
            direction = "SW"
        if (angle <= 190 && angle > 170)
            direction = "S"
        if (angle <= 170 && angle > 100)
            direction = "SE"
        if (angle <= 100 && angle > 80)
            direction = "E"
        if (angle <= 80 && angle > 10)
            direction = "NE"

        return direction
    }

    private fun createNotification(direction: String, angle: Double): Notification {
        // 1 Creates a NotificationManager.
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                application.packageName,
                "Notifications", NotificationManager.IMPORTANCE_DEFAULT
            )

            // Configure the notification channel.
            notificationChannel.enableLights(false)
            notificationChannel.setSound(null, null)
            notificationChannel.enableVibration(false)
            notificationChannel.vibrationPattern = longArrayOf(0L)
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(baseContext, application.packageName)
        // 2 Opens the main screen of the app on a notification tap.
        val contentIntent = PendingIntent.getActivity(
            this, notificationActivityRequestCode,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )
        // 3 Adds an intent to stop the notifications from appearing.
        val stopNotificationIntent = Intent(this, WifiP2pManager.ActionListener::class.java)
        stopNotificationIntent.action = KEY_NOTIFICATION_STOP_ACTION
        stopNotificationIntent.putExtra(KEY_NOTIFICATION_ID, notificationId)
        val pendingStopNotificationIntent =
            PendingIntent.getBroadcast(
                this,
                notificationStopRequestCode,
                stopNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText("You're currently facing $direction at an angle of $angle°")
            .setWhen(System.currentTimeMillis())
            .setDefaults(0)
            .setVibrate(longArrayOf(0L))
            .setSound(null)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(contentIntent)
            .addAction(
                R.mipmap.ic_launcher_round,
                getString(R.string.stop_notifications),
                pendingStopNotificationIntent
            )


        return notificationBuilder.build()
    }


}