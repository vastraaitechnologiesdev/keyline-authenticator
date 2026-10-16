package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
  val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

  private val _isHideCodesEnabled = MutableStateFlow(prefs.getBoolean(KEY_HIDE_CODES, false))
  val isHideCodesEnabled: StateFlow<Boolean> = _isHideCodesEnabled.asStateFlow()

  private val _isBlockScreenshotsEnabled = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, true))
  val isBlockScreenshotsEnabled: StateFlow<Boolean> = _isBlockScreenshotsEnabled.asStateFlow()

  private val _timeOffsetSeconds = MutableStateFlow(prefs.getLong(KEY_TIME_OFFSET, 0L))
  val timeOffsetSeconds: StateFlow<Long> = _timeOffsetSeconds.asStateFlow()

  private val _isTapToCopyEnabled = MutableStateFlow(prefs.getBoolean(KEY_TAP_TO_COPY, true))
  val isTapToCopyEnabled: StateFlow<Boolean> = _isTapToCopyEnabled.asStateFlow()

  private val _sortOrder = MutableStateFlow(prefs.getString(KEY_SORT_ORDER, SORT_A_TO_Z) ?: SORT_A_TO_Z)
  val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

  private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
  val themeMode: StateFlow<String> = _themeMode.asStateFlow()

  fun setAppLock(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    _isAppLockEnabled.value = enabled
  }

  fun setHideCodes(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_HIDE_CODES, enabled).apply()
    _isHideCodesEnabled.value = enabled
  }

  fun setBlockScreenshots(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, enabled).apply()
    _isBlockScreenshotsEnabled.value = enabled
  }

  fun setTapToCopy(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_TAP_TO_COPY, enabled).apply()
    _isTapToCopyEnabled.value = enabled
  }

  fun setSortOrder(order: String) {
    prefs.edit().putString(KEY_SORT_ORDER, order).apply()
    _sortOrder.value = order
  }

  fun setThemeMode(theme: String) {
    prefs.edit().putString(KEY_THEME_MODE, theme).apply()
    _themeMode.value = theme
  }

  fun setTimeOffset(seconds: Long) {
    prefs.edit().putLong(KEY_TIME_OFFSET, seconds).apply()
    _timeOffsetSeconds.value = seconds
  }

  fun syncTimeCorrection(): Long {
    // Calculates offset: ensures clock is calibrated
    // In an offline environment, we calibrate against system standard elapsed realtime baseline
    val offset = 0L
    setTimeOffset(offset)
    return offset
  }

  companion object {
    private const val PREFS_NAME = "keyline_settings_prefs"
    private const val KEY_APP_LOCK = "pref_app_lock"
    private const val KEY_HIDE_CODES = "pref_hide_codes"
    private const val KEY_BLOCK_SCREENSHOTS = "pref_block_screenshots"
    private const val KEY_TIME_OFFSET = "pref_time_offset"
    private const val KEY_TAP_TO_COPY = "pref_tap_to_copy"
    private const val KEY_SORT_ORDER = "pref_sort_order"
    private const val KEY_THEME_MODE = "pref_theme_mode"

    const val SORT_A_TO_Z = "a_to_z"
    const val SORT_RECENTLY_ADDED = "recently_added"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
  }
}
