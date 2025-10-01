package com.example.newiptv.ui.settings

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newiptv.R
import com.example.newiptv.database.AppSettings
import com.example.newiptv.database.AppSettingsDao
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.repository.SettingsRepository
import kotlinx.coroutines.launch

/**
 * Settings Activity
 * 
 * Provides comprehensive settings management for the app including:
 * - Login settings (username, password, auto-login)
 * - Player settings (speed, quality, auto-play)
 * - Appearance settings (theme, font size)
 * - General settings (language, notifications)
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var settingsRepository: SettingsRepository
    
    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRememberCredentials: CheckBox
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var seekBarSpeed: SeekBar
    private lateinit var tvSpeedValue: TextView
    private lateinit var cbRememberSpeed: CheckBox
    private lateinit var cbAutoPlayNext: CheckBox
    private lateinit var cbRememberPosition: CheckBox
    private lateinit var rgTheme: RadioGroup
    private lateinit var rbThemeDark: RadioButton
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeAuto: RadioButton
    private lateinit var rgFontSize: RadioGroup
    private lateinit var rbFontSmall: RadioButton
    private lateinit var rbFontMedium: RadioButton
    private lateinit var rbFontLarge: RadioButton
    private lateinit var spinnerLanguage: Spinner
    private lateinit var cbNotifications: CheckBox
    private lateinit var cbAutoUpdate: CheckBox
    private lateinit var btnResetSettings: Button
    private lateinit var btnSaveSettings: Button
    
    // Speed values mapping
    private val speedValues = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize settings repository
        val database = DatabaseProvider.getDatabase(this)
        val settingsDao = database.appSettingsDao()
        settingsRepository = SettingsRepository(settingsDao)
        
        initializeViews()
        setupListeners()
        loadSettings()
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        cbRememberCredentials = findViewById(R.id.cbRememberCredentials)
        cbAutoLogin = findViewById(R.id.cbAutoLogin)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)
        cbRememberSpeed = findViewById(R.id.cbRememberSpeed)
        cbAutoPlayNext = findViewById(R.id.cbAutoPlayNext)
        cbRememberPosition = findViewById(R.id.cbRememberPosition)
        rgTheme = findViewById(R.id.rgTheme)
        rbThemeDark = findViewById(R.id.rbThemeDark)
        rbThemeLight = findViewById(R.id.rbThemeLight)
        rbThemeAuto = findViewById(R.id.rbThemeAuto)
        rgFontSize = findViewById(R.id.rgFontSize)
        rbFontSmall = findViewById(R.id.rbFontSmall)
        rbFontMedium = findViewById(R.id.rbFontMedium)
        rbFontLarge = findViewById(R.id.rbFontLarge)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        cbNotifications = findViewById(R.id.cbNotifications)
        cbAutoUpdate = findViewById(R.id.cbAutoUpdate)
        btnResetSettings = findViewById(R.id.btnResetSettings)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        
        // Setup language spinner
        setupLanguageSpinner()
    }
    
    private fun setupLanguageSpinner() {
        val languages = arrayOf("English", "Arabic", "French", "Spanish", "German")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val speed = speedValues[progress]
                    tvSpeedValue.text = "${speed}x"
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        btnResetSettings.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettingsSync()
                populateSettings(settings)
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Failed to load settings", e)
                Toast.makeText(this@SettingsActivity, "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun populateSettings(settings: AppSettings) {
        // Login settings
        etUsername.setText(settings.username ?: "")
        etPassword.setText(settings.password ?: "")
        cbRememberCredentials.isChecked = settings.rememberCredentials
        cbAutoLogin.isChecked = settings.autoLogin
        
        // Player settings
        val speedIndex = speedValues.indexOf(settings.defaultPlaybackSpeed)
        if (speedIndex >= 0) {
            seekBarSpeed.progress = speedIndex
            tvSpeedValue.text = "${settings.defaultPlaybackSpeed}x"
        }
        cbRememberSpeed.isChecked = settings.rememberPlaybackSpeed
        cbAutoPlayNext.isChecked = settings.autoPlayNext
        cbRememberPosition.isChecked = settings.rememberPosition
        
        // Appearance settings
        when (settings.theme) {
            "dark" -> rbThemeDark.isChecked = true
            "light" -> rbThemeLight.isChecked = true
            "auto" -> rbThemeAuto.isChecked = true
        }
        
        when (settings.fontSize) {
            "small" -> rbFontSmall.isChecked = true
            "medium" -> rbFontMedium.isChecked = true
            "large" -> rbFontLarge.isChecked = true
        }
        
        // General settings
        val languageIndex = when (settings.language) {
            "en" -> 0
            "ar" -> 1
            "fr" -> 2
            "es" -> 3
            "de" -> 4
            else -> 0
        }
        spinnerLanguage.setSelection(languageIndex)
        cbNotifications.isChecked = settings.notifications
        cbAutoUpdate.isChecked = settings.autoUpdate
    }
    
    private fun saveSettings() {
        lifecycleScope.launch {
            try {
                // Get current settings
                val currentSettings = settingsRepository.getSettingsSync()
                
                // Update login settings
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()
                settingsRepository.updateLoginSettings(
                    username = if (username.isNotEmpty()) username else null,
                    password = if (password.isNotEmpty()) password else null,
                    rememberCredentials = cbRememberCredentials.isChecked,
                    autoLogin = cbAutoLogin.isChecked
                )
                
                // Update player settings
                val speedIndex = seekBarSpeed.progress
                val speed = speedValues[speedIndex]
                settingsRepository.updatePlayerSettings(
                    speed = speed,
                    rememberSpeed = cbRememberSpeed.isChecked,
                    quality = currentSettings.defaultVideoQuality,
                    autoPlay = cbAutoPlayNext.isChecked,
                    rememberPos = cbRememberPosition.isChecked,
                    buffer = currentSettings.bufferSize
                )
                
                // Update appearance settings
                val theme = when (rgTheme.checkedRadioButtonId) {
                    R.id.rbThemeDark -> "dark"
                    R.id.rbThemeLight -> "light"
                    R.id.rbThemeAuto -> "auto"
                    else -> "dark"
                }
                
                val fontSize = when (rgFontSize.checkedRadioButtonId) {
                    R.id.rbFontSmall -> "small"
                    R.id.rbFontMedium -> "medium"
                    R.id.rbFontLarge -> "large"
                    else -> "medium"
                }
                
                settingsRepository.updateAppearanceSettings(
                    theme = theme,
                    primaryColor = currentSettings.primaryColor,
                    accentColor = currentSettings.accentColor,
                    fontSize = fontSize,
                    showSubtitles = currentSettings.showSubtitles,
                    subtitleSize = currentSettings.subtitleSize
                )
                
                // Update general settings
                val language = when (spinnerLanguage.selectedItemPosition) {
                    0 -> "en"
                    1 -> "ar"
                    2 -> "fr"
                    3 -> "es"
                    4 -> "de"
                    else -> "en"
                }
                
                settingsRepository.updateGeneralSettings(
                    language = language,
                    notifications = cbNotifications.isChecked,
                    analytics = currentSettings.analytics,
                    crashReporting = currentSettings.crashReporting,
                    autoUpdate = cbAutoUpdate.isChecked
                )
                
                Toast.makeText(this@SettingsActivity, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Failed to save settings", e)
                Toast.makeText(this@SettingsActivity, "Failed to save settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun resetToDefaults() {
        lifecycleScope.launch {
            try {
                settingsRepository.resetToDefaults()
                loadSettings()
                Toast.makeText(this@SettingsActivity, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Failed to reset settings", e)
                Toast.makeText(this@SettingsActivity, "Failed to reset settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
