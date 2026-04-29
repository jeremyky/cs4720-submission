package edu.nd.pmcburne.hwapp.one.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nbalytics_user_prefs"
)

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.userPreferencesDataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            preferredSportsbookKey = prefs[KEY_SPORTSBOOK]
                ?: Sportsbooks.DRAFTKINGS.key,
            oddsFormat = readOddsFormat(prefs[KEY_ODDS_FORMAT]),
            showOnlyFavoriteTeams = prefs[KEY_SHOW_ONLY_FAVORITES] ?: false,
            favoriteTeams = prefs[KEY_FAVORITE_TEAMS] ?: emptySet()
        )
    }

    suspend fun setPreferredSportsbook(key: String) {
        dataStore.edit { it[KEY_SPORTSBOOK] = key }
    }

    suspend fun setOddsFormat(format: OddsFormat) {
        dataStore.edit { it[KEY_ODDS_FORMAT] = format.name }
    }

    suspend fun setShowOnlyFavoriteTeams(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_ONLY_FAVORITES] = enabled }
    }

    suspend fun addFavoriteTeam(teamName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_TEAMS] ?: emptySet()
            prefs[KEY_FAVORITE_TEAMS] = current + teamName
        }
    }

    suspend fun removeFavoriteTeam(teamName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_TEAMS] ?: emptySet()
            prefs[KEY_FAVORITE_TEAMS] = current - teamName
        }
    }

    private fun readOddsFormat(stored: String?): OddsFormat {
        if (stored == null) return OddsFormat.AMERICAN
        return runCatching { OddsFormat.valueOf(stored) }.getOrDefault(OddsFormat.AMERICAN)
    }

    private companion object {
        val KEY_SPORTSBOOK = stringPreferencesKey("preferred_sportsbook")
        val KEY_ODDS_FORMAT = stringPreferencesKey("odds_format")
        val KEY_SHOW_ONLY_FAVORITES = booleanPreferencesKey("show_only_favorites")
        val KEY_FAVORITE_TEAMS = stringSetPreferencesKey("favorite_teams")
    }
}
