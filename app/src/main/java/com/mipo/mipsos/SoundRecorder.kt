package com.mipo.mipsos

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException

class SoundRecorder(private val context: Context, private val recordingStatusTextView: TextView, private val recordButton: Button, private val playbackButton: Button) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFilePath: String? = null
    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null

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
                    recordingStatusTextView.text = "Recording started"
                    recordButton.text = "Stop"
                    playbackButton.isEnabled = false // Disable playback button during recording
                } catch (e: IOException) {
                    Log.e("AudioRecording", "prepare() failed: ${e.message}")
                    recordingStatusTextView.text = "Recording failed"
                    recordButton.text = "Record Audio"
                    playbackButton.isEnabled = true // Enable playback button if recording fails
                }
            }
        } else {
            stopRecording()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        recordingStatusTextView.text = "Not Recording"
        recordButton.text = "Record Audio"
        playbackButton.isEnabled = true // Re-enable playback button after recording stops

        recordingFilePath?.let { filePath ->
            if (File(filePath).exists()) {
                Toast.makeText(context, "Recording saved at: $filePath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error: Recording file not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handlePlayback() {
        if (isPlaying) {
            mediaPlayer?.stop()
            resetMediaPlayer()
        } else {
            recordingFilePath?.let { filePath ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    setOnCompletionListener {
                        resetMediaPlayer()
                    }
                    prepare()
                    start()
                }
                playbackButton.text = "Stop Playback"
                isPlaying = true
            }
        }
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        playbackButton.text = "Play Recording"
    }

    fun resetPlayer() {
        resetMediaPlayer()
    }

    private fun getRecordingFilePath(): String {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (storageDir != null && storageDir.exists()) {
            File(storageDir, "audio_recording.3gp").absolutePath
        } else {
            context.filesDir.absolutePath + "/audio_recording.3gp"
        }
    }
}
