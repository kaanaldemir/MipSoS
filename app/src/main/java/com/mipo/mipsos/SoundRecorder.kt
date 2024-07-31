package com.mipo.mipsos

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class SoundRecorder(
    private val context: Context,
    private val recordingStatusTextView: TextView,
    private val recordButton: Button,
    private val playbackButton: Button
) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFilePath: String? = null
    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var intervalRunnable: Runnable? = null
    private var hasRecordedInSession = false
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var remainingTime: Long = 0
    private var isCountingDown = false

    fun startRecording() {
        if (mediaRecorder == null) {
            recordingFilePath = getRecordingFilePath()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(recordingFilePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                try {
                    prepare()
                    start()
                    recordingStatusTextView.text = context.getString(R.string.recording_started)
                    recordButton.text = context.getString(R.string.stop)
                    playbackButton.isEnabled = false // Disable playback button during recording
                } catch (e: IOException) {
                    Log.e("AudioRecording", "prepare() failed: ${e.message}")
                    recordingStatusTextView.text = context.getString(R.string.recording_failed)
                    recordButton.text = context.getString(R.string.record_audio)
                    playbackButton.isEnabled = true // Enable playback button if recording fails
                }
            }
        } else {
            stopRecording()
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        hasRecordedInSession = true // Mark that a recording has been made in this session

        recordingStatusTextView.text = context.getString(R.string.not_recording)
        recordButton.text = context.getString(R.string.record_audio)
        playbackButton.isEnabled = true // Re-enable playback button after recording stops

        recordingFilePath?.let { filePath ->
            if (File(filePath).exists()) {
                val message = context.getString(R.string.recording_saved, filePath)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, context.getString(R.string.recording_file_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handlePlayback(useProvidedSound: Boolean, intervalPlayback: Boolean, interval: Long) {
        if (isPlaying || isCountingDown) {
            stopPlayback()
        } else {
            if (useProvidedSound) {
                playProvidedSound(intervalPlayback, interval)
            } else {
                playRecordedSound(intervalPlayback, interval)
            }
        }
    }

    private fun playRecordedSound(intervalPlayback: Boolean, interval: Long) {
        recordingFilePath?.let { filePath ->
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    resetMediaPlayer(false)
                    if (intervalPlayback) {
                        startIntervalCountdown(interval)
                    }
                }
                prepare()
                start()
            }
            playbackButton.text = context.getString(R.string.stop_playback)
            playbackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)
            isPlaying = true
        }
    }

    private fun playProvidedSound(intervalPlayback: Boolean, interval: Long) {
        mediaPlayer = MediaPlayer.create(context, R.raw.provided_sound) // Ensure provided_sound.mp3 is in res/raw
        mediaPlayer?.apply {
            setOnCompletionListener {
                resetMediaPlayer(true)
                if (intervalPlayback) {
                    startIntervalCountdown(interval)
                }
            }
            start()
        }
        playbackButton.text = context.getString(R.string.stop_playback)
        playbackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)
        isPlaying = true
    }

    private fun startIntervalCountdown(interval: Long) {
        remainingTime = interval / 1000 // convert to seconds
        updateCountdownText()
        countdownHandler = Handler()
        isCountingDown = true
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingTime > 0) {
                    remainingTime--
                    updateCountdownText()
                    countdownHandler?.postDelayed(this, 1000)
                } else {
                    isCountingDown = false
                    handlePlayback(true, true, interval) // Start playback after countdown
                }
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }

    private fun stopPlayback() {
        if (isPlaying) {
            mediaPlayer?.stop()
            resetMediaPlayer(false)
        }
        if (isCountingDown) {
            countdownHandler?.removeCallbacks(countdownRunnable!!)
            countdownHandler = null
            countdownRunnable = null
            remainingTime = 0
            isCountingDown = false
            resetMediaPlayer(false)
        }
    }

    private fun updateCountdownText() {
        playbackButton.text = context.getString(R.string.countdown, remainingTime)
        playbackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)
    }

    private fun resetMediaPlayer(useProvidedSound: Boolean) {
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        playbackButton.text = if (useProvidedSound) context.getString(R.string.play_whistle) else context.getString(R.string.play_recording)
        playbackButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0)
    }

    fun resetPlayer() {
        resetMediaPlayer(false)
    }

    fun getRecordingFilePath(): String {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (storageDir != null && storageDir.exists()) {
            File(storageDir, "audio_recording.3gp").absolutePath
        } else {
            context.filesDir.absolutePath + "/audio_recording.3gp"
        }
    }

    fun hasRecorded(): Boolean {
        return hasRecordedInSession
    }
}
