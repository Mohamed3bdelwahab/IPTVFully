package com.example.newiptv.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.newiptv.R
import com.example.newiptv.databinding.FragmentSettingsBinding
import com.example.newiptv.sync.SyncManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var syncManager: SyncManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        syncManager = SyncManager(requireContext())
        
        setupSyncSettings()
        updateSyncStatus()
        
        return root
    }
    
    private fun setupSyncSettings() {
        // Get current sync status
        val syncStatus = syncManager.getSyncStatus()
        
        // Setup sync enabled switch
        val syncEnabledSwitch = binding.root.findViewById<Switch>(R.id.syncEnabledSwitch)
        syncEnabledSwitch.isChecked = syncStatus.isEnabled
        syncEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("SettingsFragment", "🔄 Sync enabled changed: $isChecked")
            val intervalHours = syncManager.getSyncStatus().intervalHours
            syncManager.updateSyncSettings(isChecked, intervalHours)
            updateSyncStatus()
            
            if (isChecked) {
                Toast.makeText(context, "Daily sync enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Daily sync disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup sync interval seekbar
        val syncIntervalSeekBar = binding.root.findViewById<SeekBar>(R.id.syncIntervalSeekBar)
        val syncIntervalText = binding.root.findViewById<android.widget.TextView>(R.id.syncIntervalText)
        
        // Convert hours to seekbar position (1-24 hours, 0-23 positions)
        val currentInterval = syncStatus.intervalHours.toInt()
        syncIntervalSeekBar.progress = (currentInterval - 1).coerceIn(0, 23)
        
        syncIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val hours = progress + 1 // 1-24 hours
                syncIntervalText.text = "$hours hour${if (hours > 1) "s" else ""}"
                
                if (fromUser) {
                    val isEnabled = syncManager.getSyncStatus().isEnabled
                    syncManager.updateSyncSettings(isEnabled, hours.toLong())
                    updateSyncStatus()
                    Log.d("SettingsFragment", "🔄 Sync interval changed: $hours hours")
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup manual sync button
        val manualSyncButton = binding.root.findViewById<android.widget.Button>(R.id.manualSyncButton)
        manualSyncButton.setOnClickListener {
            Log.d("SettingsFragment", "🔄 Manual sync triggered")
            syncManager.triggerManualSync()
            Toast.makeText(context, "Manual sync started...", Toast.LENGTH_SHORT).show()
            updateSyncStatus()
        }
    }
    
    private fun updateSyncStatus() {
        lifecycleScope.launch {
            try {
                val syncStatus = syncManager.getSyncStatus()
                
                // Update status texts
                val lastSyncText = binding.root.findViewById<android.widget.TextView>(R.id.lastSyncText)
                val nextSyncText = binding.root.findViewById<android.widget.TextView>(R.id.nextSyncText)
                val syncStatusText = binding.root.findViewById<android.widget.TextView>(R.id.syncStatusText)
                
                lastSyncText.text = syncStatus.getLastSyncText()
                nextSyncText.text = syncStatus.getNextSyncText()
                
                syncStatusText.text = when {
                    syncStatus.isRunning -> "Syncing..."
                    syncStatus.isEnabled -> "Enabled"
                    else -> "Disabled"
                }
                
                syncStatusText.setTextColor(
                    when {
                        syncStatus.isRunning -> requireContext().getColor(R.color.accent_color)
                        syncStatus.isEnabled -> requireContext().getColor(R.color.series_color)
                        else -> requireContext().getColor(R.color.text_secondary)
                    }
                )
                
                Log.d("SettingsFragment", "📊 Sync status updated: enabled=${syncStatus.isEnabled}, running=${syncStatus.isRunning}")
                
            } catch (e: Exception) {
                Log.e("SettingsFragment", "❌ Failed to update sync status", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}