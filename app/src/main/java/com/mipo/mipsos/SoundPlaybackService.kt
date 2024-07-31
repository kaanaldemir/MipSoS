package com.mipo.mipsos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class SoundPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var intervalRunnable: Runnable? = null
    private var interval: Long = 0

    companion object {
        const val CHANNEL_ID = "SoundPlaybackServiceChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_USE_PROVIDED_SOUND = "EXTRA_USE_PROVIDED_SOUND"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("Playing sound at intervals"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            interval = intent.getLongExtra(EXTRA_INTERVAL, 0)
            val useProvidedSound = intent.getBooleanExtra(EXTRA_USE_PROVIDED_SOUND, false)
            startIntervalPlayback(useProvidedSound)
        } else if (intent?.action == ACTION_STOP) {
            stopIntervalPlayback()
        }
        return START_NOT_STICKY
    }

    private fun startIntervalPlayback(useProvidedSound: Boolean) {
        handler = Handler()
        intervalRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.release()
                mediaPlayer = if (useProvidedSound) {
                    MediaPlayer.create(this@SoundPlaybackService, R.raw.provided_sound)
                } else {
                    MediaPlayer().apply {
                        setDataSource(getRecordingFilePath())
                        prepare()
                    }
                }
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener {
                    handler?.postDelayed(this, interval)
                }
            }
        }
        handler?.post(intervalRunnable!!)
    }

    private fun stopIntervalPlayback() {
        handler?.removeCallbacks(intervalRunnable!!)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Sound Playback Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sound Playback")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_play)
            .build()
    }

    private fun getRecordingFilePath(): String {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (storageDir != null && storageDir.exists()) {
            File(storageDir, "audio_recording.3gp").absolutePath
        } else {
            filesDir.absolutePath + "/audio_recording.3gp"
        }
    }
}
