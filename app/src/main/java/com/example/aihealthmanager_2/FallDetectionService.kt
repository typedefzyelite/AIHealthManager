package com.example.aihealthmanager_2

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isFalling = false
    private var fallTimestamp = 0L
    private var isAlertActive = false

    companion object {
        const val CHANNEL_ID = "fall_detection_channel"
        const val NOTIFICATION_ID = 1001
        const val FALL_THRESHOLD = 3.0
        const val IMPACT_THRESHOLD = 25.0
        const val IMPACT_WINDOW_MS = 1000L
        const val TAG = "FallDetection"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("摔倒检测运行中")
        startForeground(NOTIFICATION_ID, notification)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.i(TAG, "传感器监听已启动")
        } ?: Log.w(TAG, "设备不支持加速度传感器")

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || isAlertActive) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())

        val now = System.currentTimeMillis()

        if (!isFalling && magnitude < FALL_THRESHOLD) {
            isFalling = true
            fallTimestamp = now
            Log.d(TAG, "检测到自由落体: magnitude=$magnitude")
        }

        if (isFalling && magnitude > IMPACT_THRESHOLD && (now - fallTimestamp) < IMPACT_WINDOW_MS) {
            isFalling = false
            Log.w(TAG, "检测到摔倒! impact=$magnitude")
            isAlertActive = true
            triggerFallAlert()
        }

        if (isFalling && (now - fallTimestamp) > IMPACT_WINDOW_MS) {
            isFalling = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerFallAlert() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("FALL_DETECTED", true)
        }
        startActivity(intent)
        handler.postDelayed({ isAlertActive = false }, 35000)
    }

    fun resetAlert() {
        isAlertActive = false
        Log.i(TAG, "摔倒警报已重置")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "摔倒检测服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "持续监测加速度传感器以检测摔倒"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("药爱健康")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        Log.i(TAG, "摔倒检测服务已停止")
        super.onDestroy()
    }
}
