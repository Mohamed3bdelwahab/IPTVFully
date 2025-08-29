package com.example.newiptv.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class IPTVVideoPlayerTest {

    @MockK
    private lateinit var mockExoPlayer: ExoPlayer
    
    @MockK
    private lateinit var mockPlayerListener: IPTVVideoPlayer.PlayerListener
    
    private lateinit var context: Context
    private lateinit var videoPlayer: IPTVVideoPlayer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = RuntimeEnvironment.getApplication()
        
        // Mock ExoPlayer creation
        mockkConstructor(ExoPlayer.Builder::class)
        every { anyConstructed<ExoPlayer.Builder>().build() } returns mockExoPlayer
        every { mockExoPlayer.addListener(any()) } just Runs
        every { mockExoPlayer.setMediaItem(any()) } just Runs
        every { mockExoPlayer.prepare() } just Runs
        every { mockExoPlayer.play() } just Runs
        every { mockExoPlayer.pause() } just Runs
        every { mockExoPlayer.stop() } just Runs
        every { mockExoPlayer.seekTo(any()) } just Runs
        every { mockExoPlayer.release() } just Runs
        every { mockExoPlayer.currentPosition } returns 0L
        every { mockExoPlayer.duration } returns 0L
        every { mockExoPlayer.isPlaying } returns false
        every { mockExoPlayer.volume } returns 1f
        every { mockExoPlayer.volume = any() } just Runs
        
        videoPlayer = IPTVVideoPlayer(context, mockPlayerListener)
    }

    @Test
    fun `test loadVideo with valid URL`() {
        // Given
        val testUrl = "http://example.com/video.mp4"
        
        // When
        videoPlayer.loadVideo(testUrl)
        
        // Then
        verify {
            mockExoPlayer.setMediaItem(any())
            mockExoPlayer.prepare()
        }
    }

    @Test
    fun `test loadVideo with null URL should handle gracefully`() {
        // Given
        val testUrl: String? = null
        
        // When
        videoPlayer.loadVideo(testUrl ?: "")
        
        // Then
        // Should not crash and should handle gracefully
        verify(exactly = 0) {
            mockPlayerListener.onPlayerError(any())
        }
    }

    @Test
    fun `test play method`() {
        // When
        videoPlayer.play()
        
        // Then
        verify { mockExoPlayer.play() }
    }

    @Test
    fun `test pause method`() {
        // When
        videoPlayer.pause()
        
        // Then
        verify { mockExoPlayer.pause() }
    }

    @Test
    fun `test stop method`() {
        // When
        videoPlayer.stop()
        
        // Then
        verify { mockExoPlayer.stop() }
    }

    @Test
    fun `test seekTo method`() {
        // Given
        val position = 5000L
        
        // When
        videoPlayer.seekTo(position)
        
        // Then
        verify { mockExoPlayer.seekTo(position) }
    }

    @Test
    fun `test getCurrentPosition`() {
        // Given
        val expectedPosition = 10000L
        every { mockExoPlayer.currentPosition } returns expectedPosition
        
        // When
        val result = videoPlayer.getCurrentPosition()
        
        // Then
        assert(result == expectedPosition)
    }

    @Test
    fun `test getDuration`() {
        // Given
        val expectedDuration = 60000L
        every { mockExoPlayer.duration } returns expectedDuration
        
        // When
        val result = videoPlayer.getDuration()
        
        // Then
        assert(result == expectedDuration)
    }

    @Test
    fun `test isPlaying when player is playing`() {
        // Given
        every { mockExoPlayer.isPlaying } returns true
        
        // When
        val result = videoPlayer.isPlaying()
        
        // Then
        assert(result)
    }

    @Test
    fun `test isPlaying when player is not playing`() {
        // Given
        every { mockExoPlayer.isPlaying } returns false
        
        // When
        val result = videoPlayer.isPlaying()
        
        // Then
        assert(!result)
    }

    @Test
    fun `test setVolume with valid range`() {
        // Given
        val volume = 0.5f
        
        // When
        videoPlayer.setVolume(volume)
        
        // Then
        verify { mockExoPlayer.volume = volume }
    }

    @Test
    fun `test setVolume with value above 1.0 should be clamped`() {
        // Given
        val volume = 1.5f
        
        // When
        videoPlayer.setVolume(volume)
        
        // Then
        verify { mockExoPlayer.volume = 1.0f }
    }

    @Test
    fun `test setVolume with value below 0.0 should be clamped`() {
        // Given
        val volume = -0.5f
        
        // When
        videoPlayer.setVolume(volume)
        
        // Then
        verify { mockExoPlayer.volume = 0.0f }
    }

    @Test
    fun `test getVolume`() {
        // Given
        val expectedVolume = 0.7f
        every { mockExoPlayer.volume } returns expectedVolume
        
        // When
        val result = videoPlayer.getVolume()
        
        // Then
        assert(result == expectedVolume)
    }

    @Test
    fun `test release method`() {
        // When
        videoPlayer.release()
        
        // Then
        verify { mockExoPlayer.release() }
    }

    @Test
    fun `test getPlayer returns ExoPlayer instance`() {
        // When
        val result = videoPlayer.getPlayer()
        
        // Then
        assert(result == mockExoPlayer)
    }

    @Test
    fun `test isReady returns false initially`() {
        // When
        val result = videoPlayer.isReady()
        
        // Then
        assert(!result)
    }

    @Test
    fun `test player listener callbacks`() {
        // Given
        val testUrl = "http://example.com/video.mp4"
        
        // When
        videoPlayer.loadVideo(testUrl)
        
        // Then
        // Verify that listener methods can be called without crashing
        verify(exactly = 0) {
            mockPlayerListener.onPlayerReady()
            mockPlayerListener.onPlayerError(any())
            mockPlayerListener.onPlaybackStateChanged(any())
            mockPlayerListener.onProgressChanged(any(), any())
            mockPlayerListener.onBufferingChanged(any())
        }
    }

    @Test
    fun `test player initialization with null listener`() {
        // When
        val playerWithoutListener = IPTVVideoPlayer(context, null)
        
        // Then
        // Should not crash
        assert(playerWithoutListener.getPlayer() != null)
    }

    @Test
    fun `test multiple seek operations`() {
        // Given
        val positions = listOf(1000L, 5000L, 10000L, 30000L)
        
        // When
        positions.forEach { position ->
            videoPlayer.seekTo(position)
        }
        
        // Then
        verify(exactly = positions.size) {
            mockExoPlayer.seekTo(any())
        }
    }

    @Test
    fun `test volume control range`() {
        // Given
        val testVolumes = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        
        // When
        testVolumes.forEach { volume ->
            videoPlayer.setVolume(volume)
        }
        
        // Then
        verify(exactly = testVolumes.size) {
            mockExoPlayer.volume = any()
        }
    }
}
