package com.example.checkinreceipts.core.settings
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

object Keys {
    val CheckInRadiusM = intPreferencesKey("checkin_radius_m")
    val ApiBaseUrl = stringPreferencesKey("api_base_url")
    val SaveOriginalImage = booleanPreferencesKey("save_original_image")
    val EnableShakeToAdd = booleanPreferencesKey("enable_shake_to_add")
}

class SettingsDataStore(private val context: Context) {
    val checkInRadiusM: Flow<Int> =
        context.dataStore.data.map { it[Keys.CheckInRadiusM] ?: 200 }
    val apiBaseUrl: Flow<String> =
        context.dataStore.data.map { it[Keys.ApiBaseUrl] ?: "https://jsonplaceholder.typicode.com/" }

    val saveOriginalImage: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SaveOriginalImage] ?: false }

    val enableShakeToAdd: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.EnableShakeToAdd] ?: true }

    suspend fun setCheckInRadiusM(value: Int) {
        context.dataStore.edit { it[Keys.CheckInRadiusM] = value.coerceIn(50, 2000) }
    }

    suspend fun setApiBaseUrl(value: String) {
        context.dataStore.edit { it[Keys.ApiBaseUrl] = value }
    }

    suspend fun setSaveOriginalImage(value: Boolean) {
        context.dataStore.edit { it[Keys.SaveOriginalImage] = value }
    }

    suspend fun setEnableShakeToAdd(value: Boolean) {
        context.dataStore.edit { it[Keys.EnableShakeToAdd] = value }
    }
}