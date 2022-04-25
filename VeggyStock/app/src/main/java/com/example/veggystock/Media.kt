package com.example.veggystock

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.example.veggystock.databinding.ActivityMediaBinding

class Media : AppCompatActivity() {
    lateinit var binding: ActivityMediaBinding
    lateinit var mp: MediaPlayer
    lateinit var mediaController: MediaController
    lateinit var audioManager: AudioManager
    var posicion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMediaBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        listeners()
        inicializeMedia()
        volume()
        mediaController = MediaController(this)
    }

    private fun volume() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun inicializeMedia() {
        mp = MediaPlayer.create(this, R.raw.audio_trim)
    }

    private fun listeners() {
        binding.btnVideo.setOnClickListener {
            startVideo()
        }
        binding.btnStart.setOnClickListener {
            startAudio()
        }
        binding.btnStop.setOnClickListener {
            stopAudio()
        }

    }

    private fun stopAudio() {
        Log.d("Stopping Audio->>>", mp.isPlaying.toString())
        binding.btnStop.isEnabled = false
        binding.btnStart.isEnabled = true
        mp.pause()
    }

    private fun startAudio() {
        mp.start()
        Log.d("Starting Audio->>>", mp.isPlaying.toString())
        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
    }

    private fun startVideo() {
        val video: Int = R.raw.video_trim
        Log.d("STARTING VIDEO->>>", "YUUUUUUUUUU")

        val route = "android.resource://$packageName/$video"
        Log.d("RUTA VIDEO->>", route)
        val uri = Uri.parse(route)

        try {
            binding.videoView.setVideoURI(uri)
            binding.videoView.requestFocus()
        } catch (ex: Exception) {
            Log.e("ERROR AL CARGAR->>", ex.message.toString())
        }

        binding.videoView.setMediaController(mediaController)
        mediaController.setAnchorView(binding.videoView)

        binding.videoView.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("POSICION", binding.videoView.currentPosition)
        binding.videoView.pause()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        posicion = savedInstanceState.getInt("POSICION")
        binding.videoView.seekTo(posicion)
    }
}