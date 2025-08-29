package com.example.newiptv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.newiptv.player.VideoPlayerActivity


/**
 * Simple test activity to launch the video player
 */
class TestVideoPlayerActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_video_player)
        
        findViewById<Button>(R.id.btnTestNativeVideo).setOnClickListener {
            launchNativeVideoPlayer()
        }
        
        findViewById<Button>(R.id.btnTestWebVideo).setOnClickListener {
            launchWebVideoPlayer()
        }
    }
    
    private fun launchNativeVideoPlayer() {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, "http://aws85485.amazonedge.net/series/moh7amed819/150730/172237.mkv")
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "Test MKV Video (Native)")
        }
        startActivity(intent)
    }
    
    private fun launchWebVideoPlayer() {
        // WebVideoPlayerActivity has been removed - using native player instead
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, "http://aws85485.amazonedge.net/series/moh7amed819/150730/172237.mkv")
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "Test MKV Video (Native)")
        }
        startActivity(intent)
    }
}
