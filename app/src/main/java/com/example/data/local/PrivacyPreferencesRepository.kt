package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jagaperangkat_privacy_prefs"
)

data class PrivacyPreferences(
    val hasAcceptedDisclosure: Boolean = false,
    val includeAppMetadataScan: Boolean = true,
    val includeNetworkMetadataScan: Boolean = true,
    val completedGuidedCheckIds: Set<String> = emptySet()
)

class PrivacyPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAS_ACCEPTED_DISCLOSURE = booleanPreferencesKey("has_accepted_disclosure")
        val INCLUDE_APP_SCAN = booleanPreferencesKey("include_app_metadata_scan")
        val INCLUDE_NETWORK_SCAN = booleanPreferencesKey("include_network_metadata_scan")
        val COMPLETED_GUIDED_IDS = stringSetPreferencesKey("completed_guided_check_ids")
    }

    val preferencesFlow: Flow<PrivacyPreferences> = context.privacyDataStore.data.map { prefs ->
        PrivacyPreferences(
            hasAcceptedDisclosure = prefs[Keys.HAS_ACCEPTED_DISCLOSURE] ?: false,
            includeAppMetadataScan = prefs[Keys.INCLUDE_APP_SCAN] ?: true,
            includeNetworkMetadataScan = prefs[Keys.INCLUDE_NETWORK_SCAN] ?: true,
            completedGuidedCheckIds = prefs[Keys.COMPLETED_GUIDED_IDS] ?: emptySet()
        )
    }

    suspend fun acceptPrivacyDisclosure() {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.HAS_ACCEPTED_DISCLOSURE] = true
        }
    }

    suspend fun setIncludeAppMetadataScan(enabled: Boolean) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.INCLUDE_APP_SCAN] = enabled
        }
    }

    suspend fun setIncludeNetworkMetadataScan(enabled: Boolean) {
        context.privacyDataStore.edit { prefs ->
            prefs[Keys.INCLUDE_NETWORK_SCAN] = enabled
        }
    }

    suspend fun toggleGuidedCheck(checkId: String) {
        context.privacyDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_GUIDED_IDS]?.toMutableSet() ?: mutableSetOf()
            if (checkId in current) {
                current.remove(checkId)
            } else {
                current.add(checkId)
            }
            prefs[Keys.COMPLETED_GUIDED_IDS] = current
        }
    }

    suspend fun resetAllPreferences() {
        context.privacyDataStore.edit { it.clear() }
    }
}
