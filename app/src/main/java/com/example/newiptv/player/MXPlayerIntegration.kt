package com.example.newiptv.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity

/**
 * MX Player Integration - Handles launching videos in MX Player
 * This provides better codec support and hardware decoder compatibility
 * compared to the built-in ExoPlayer for problematic videos
 */
class MXPlayerIntegration(
    private val context: Context,
    private val listener: MXPlayerListener? = null
) {
    
    companion object {
        private const val TAG = "MXPlayerIntegration"
        
        // MX Player package names
        private const val MX_PLAYER_PRO = "com.mxtech.videoplayer.pro"
        private const val MX_PLAYER_FREE = "com.mxtech.videoplayer.ad"
        
        // Decoder modes
        const val DECODE_MODE_SOFTWARE = 1
        const val DECODE_MODE_HARDWARE = 2
        const val DECODE_MODE_AUTO = 0
    }
    
    interface MXPlayerListener {
        fun onMXPlayerLaunchSuccess()
        fun onMXPlayerLaunchFailed(error: String)
        fun onMXPlayerNotInstalled()
    }
    
    
    /**
     * Check if MX Player is installed on the device
     */
    fun isMXPlayerInstalled(): Boolean {
        val packageManager = context.packageManager
        
        Log.d(TAG, "🔍 Checking for MX Player installation...")
        Log.d(TAG, "🔍 Looking for Pro version: $MX_PLAYER_PRO")
        Log.d(TAG, "🔍 Looking for Free version: $MX_PLAYER_FREE")
        
        // List all installed packages to debug
        val installedPackages = packageManager.getInstalledPackages(0)
        val mxPackages = installedPackages.filter { it.packageName.contains("mxtech") }
        Log.d(TAG, "🔍 Found MX packages: ${mxPackages.map { it.packageName }}")
        
        // Try to find MX Player by checking if any package contains "mxtech"
        val hasMXPlayer = installedPackages.any { it.packageName.contains("mxtech") }
        Log.d(TAG, "🔍 Has MX Player (any version): $hasMXPlayer")
        
        return try {
            // Check for Pro version first
            val proInfo = packageManager.getPackageInfo(MX_PLAYER_PRO, 0)
            Log.d(TAG, "✅ MX Player Pro detected: ${proInfo.packageName}")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "❌ MX Player Pro not found: ${e.message}")
            try {
                // Check for Free version
                val freeInfo = packageManager.getPackageInfo(MX_PLAYER_FREE, 0)
                Log.d(TAG, "✅ MX Player Free detected: ${freeInfo.packageName}")
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                Log.w(TAG, "❌ MX Player Free not found: ${e2.message}")
                // If we found any MX Player package, return true
                if (hasMXPlayer) {
                    Log.d(TAG, "✅ MX Player found by package search")
                    true
                } else {
                    Log.w(TAG, "⚠️ MX Player not installed")
                    false
                }
            }
        }
    }
    
    /**
     * Get the installed MX Player package name
     */
    private fun getMXPlayerPackageName(): String? {
        val packageManager = context.packageManager
        
        return try {
            packageManager.getPackageInfo(MX_PLAYER_PRO, 0)
            MX_PLAYER_PRO
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                packageManager.getPackageInfo(MX_PLAYER_FREE, 0)
                MX_PLAYER_FREE
            } catch (e2: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
    
    /**
     * Launch a single video in MX Player
     */
    fun launchVideo(
        videoUrl: String,
        title: String? = null,
        startPosition: Long = 0L,
        decodeMode: Int = DECODE_MODE_AUTO,
        subtitleUrl: String? = null,
        subtitleName: String? = null
    ): Boolean {
        
        if (!isMXPlayerInstalled()) {
            Log.w(TAG, "⚠️ MX Player not installed, cannot launch video")
            listener?.onMXPlayerNotInstalled()
            return false
        }
        
        val packageName = getMXPlayerPackageName()
        if (packageName == null) {
            Log.e(TAG, "❌ Could not determine MX Player package name")
            listener?.onMXPlayerLaunchFailed("MX Player package not found")
            return false
        }
        
        try {
            Log.i(TAG, "🎬 Launching video in MX Player:")
            Log.i(TAG, "   URL: $videoUrl")
            Log.i(TAG, "   Title: $title")
            Log.i(TAG, "   Start Position: ${startPosition}ms")
            Log.i(TAG, "   Decode Mode: $decodeMode")
            Log.i(TAG, "   Package: $packageName")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), "video/*")
                setPackage(packageName)
                
                // Add optional parameters
                title?.let { putExtra("title", it) }
                if (startPosition > 0) {
                    putExtra("position", startPosition)
                }
                if (decodeMode != DECODE_MODE_AUTO) {
                    putExtra("decode_mode", decodeMode.toByte()) // Byte instead of Integer
                }
                
                // Add subtitle support
                subtitleUrl?.let { url ->
                    putExtra("subs", Uri.parse(url))
                    putExtra("subs.enable", true)
                    subtitleName?.let { name ->
                        putExtra("subs.name", name)
                    }
                }
                
                // Additional MX Player options
                putExtra("return_result", true) // Return to app when finished
                putExtra("secure_uri", true) // Handle secure URIs
            }
            
            // Launch MX Player
            context.startActivity(intent)
            
            Log.i(TAG, "✅ Successfully launched MX Player")
            listener?.onMXPlayerLaunchSuccess()
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch MX Player", e)
            listener?.onMXPlayerLaunchFailed("Failed to launch MX Player: ${e.message}")
            return false
        }
    }
    
    /**
     * Launch a season playlist in MX Player with episode names
     */
    fun launchSeasonPlaylist(
        videoUrls: List<String>,
        episodeNames: List<String>? = null,
        title: String? = null,
        startIndex: Int = 0,
        startPosition: Long = 0L,
        decodeMode: Int = DECODE_MODE_AUTO
    ): Boolean {
        
        if (!isMXPlayerInstalled()) {
            Log.w(TAG, "⚠️ MX Player not installed, cannot launch season playlist")
            listener?.onMXPlayerNotInstalled()
            return false
        }
        
        val packageName = getMXPlayerPackageName()
        if (packageName == null) {
            Log.e(TAG, "❌ Could not determine MX Player package name")
            listener?.onMXPlayerLaunchFailed("MX Player package not found")
            return false
        }
        
        try {
            Log.i(TAG, "🎬 Launching season playlist in MX Player:")
            Log.i(TAG, "   Episodes: ${videoUrls.size}")
            Log.i(TAG, "   Title: $title")
            Log.i(TAG, "   Start Index: $startIndex")
            Log.i(TAG, "   Start Position: ${startPosition}ms")
            Log.i(TAG, "   Package: $packageName")
            
            // Convert URLs to URIs
            val videoUris = videoUrls.map { Uri.parse(it) }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(packageName)
                type = "video/*"
                
                // Set the primary video to the start index (this is key!)
                if (startIndex >= 0 && startIndex < videoUris.size) {
                    setDataAndType(videoUris[startIndex], "video/*")
                    Log.d(TAG, "🎯 Set primary video to index $startIndex: ${videoUris[startIndex]}")
                } else if (videoUris.isNotEmpty()) {
                    setDataAndType(videoUris[0], "video/*")
                    Log.d(TAG, "🎯 Fallback: Set primary video to index 0")
                }
                
                // Add playlist with episode names - FIXED: Use correct data types
                putExtra("video_list", videoUris.toTypedArray()) // Uri[] instead of ArrayList<Uri>
                putExtra("video_list_is_explicit", true)
                putExtra("video_list.play_index", startIndex)
                
                // Additional MX Player parameters for starting index
                putExtra("play_index", startIndex)
                putExtra("position", startIndex)
                putExtra("start_index", startIndex)
                putExtra("video_list.start_index", startIndex)
                putExtra("video_list.position", startIndex)
                
                Log.d(TAG, "🎯 Setting start index: $startIndex (multiple parameters)")
                
                // Add episode names if provided - FIXED: Use correct data types
                episodeNames?.let { names ->
                    if (names.size == videoUrls.size) {
                        putExtra("video_list.name", names.toTypedArray()) // String[] instead of ArrayList<String>
                        Log.i(TAG, "   Episode names: ${names.joinToString(", ")}")
                    }
                }
                
                // Add optional parameters - FIXED: Use correct data types
                title?.let { putExtra("title", it) }
                if (startPosition > 0) {
                    putExtra("start_position", startPosition)
                }
                putExtra("decode_mode", decodeMode.toByte()) // Byte instead of Integer
                
                // Additional MX Player options
                putExtra("return_result", true)
                putExtra("secure_uri", true)
            }
            
            // Launch MX Player
            context.startActivity(intent)
            
            Log.i(TAG, "✅ Successfully launched season playlist in MX Player")
            listener?.onMXPlayerLaunchSuccess()
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch season playlist in MX Player", e)
            listener?.onMXPlayerLaunchFailed("Failed to launch season playlist in MX Player: ${e.message}")
            return false
        }
    }
    
    /**
     * Launch a playlist in MX Player (legacy method for backward compatibility)
     */
    fun launchPlaylist(
        videoUrls: List<String>,
        title: String? = null,
        startIndex: Int = 0,
        startPosition: Long = 0L,
        decodeMode: Int = DECODE_MODE_AUTO
    ): Boolean {
        
        if (!isMXPlayerInstalled()) {
            Log.w(TAG, "⚠️ MX Player not installed, cannot launch playlist")
            listener?.onMXPlayerNotInstalled()
            return false
        }
        
        val packageName = getMXPlayerPackageName()
        if (packageName == null) {
            Log.e(TAG, "❌ Could not determine MX Player package name")
            listener?.onMXPlayerLaunchFailed("MX Player package not found")
            return false
        }
        
        try {
            Log.i(TAG, "🎬 Launching playlist in MX Player:")
            Log.i(TAG, "   Videos: ${videoUrls.size}")
            Log.i(TAG, "   Title: $title")
            Log.i(TAG, "   Start Index: $startIndex")
            Log.i(TAG, "   Start Position: ${startPosition}ms")
            Log.i(TAG, "   Package: $packageName")
            
            // Convert URLs to URIs
            val playlist = videoUrls.map { Uri.parse(it) }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(packageName)
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(playlist))
                
                // Add optional parameters
                title?.let { putExtra("title", it) }
                putExtra("position", startIndex)
                if (startPosition > 0) {
                    putExtra("start_position", startPosition)
                }
                if (decodeMode != DECODE_MODE_AUTO) {
                    putExtra("decode_mode", decodeMode.toByte()) // Byte instead of Integer
                }
                
                // Additional MX Player options
                putExtra("return_result", true)
                putExtra("secure_uri", true)
            }
            
            // Launch MX Player
            context.startActivity(intent)
            
            Log.i(TAG, "✅ Successfully launched playlist in MX Player")
            listener?.onMXPlayerLaunchSuccess()
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch playlist in MX Player", e)
            listener?.onMXPlayerLaunchFailed("Failed to launch playlist in MX Player: ${e.message}")
            return false
        }
    }
    
    /**
     * Launch video with automatic fallback to ExoPlayer
     */
    fun launchVideoWithFallback(
        videoUrl: String,
        title: String? = null,
        startPosition: Long = 0L,
        fallbackPlayer: () -> Unit
    ) {
        val success = launchVideo(videoUrl, title, startPosition)
        
        if (!success) {
            Log.i(TAG, "🔄 MX Player launch failed, using fallback player")
            fallbackPlayer()
        }
    }
    
    /**
     * Show MX Player installation prompt
     */
    fun showMXPlayerInstallationPrompt() {
        Toast.makeText(
            context,
            "MX Player not installed. Please install MX Player from Play Store for better video compatibility.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * Test method for season playlist - easy to test with sample data
     */
    fun testSeasonPlaylist(): Boolean {
        Log.i(TAG, "🧪 Testing season playlist with sample data...")
        
        val testUrls = listOf(
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497321.mkv",
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497322.mkv",
            "http://aws85485.amazonedge.net/series/moh7amed819/150730/497323.mkv"
        )
        
        val testNames = listOf(
            "Episode 1 - Pilot",
            "Episode 2 - The Beginning", 
            "Episode 3 - The Journey"
        )
        
        return launchSeasonPlaylist(
            videoUrls = testUrls,
            episodeNames = testNames,
            title = "Test Season Playlist",
            startIndex = 0,
            decodeMode = DECODE_MODE_AUTO
        )
    }
    
    /**
     * Get MX Player version info
     */
    fun getMXPlayerVersion(): String? {
        val packageName = getMXPlayerPackageName() ?: return null
        
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get MX Player version", e)
            null
        }
    }
    
    /**
     * Check if MX Player supports specific features
     */
    fun supportsFeature(feature: String): Boolean {
        val packageName = getMXPlayerPackageName() ?: return false
        
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            // Basic feature check - can be expanded if needed
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check MX Player features", e)
            false
        }
    }
}
