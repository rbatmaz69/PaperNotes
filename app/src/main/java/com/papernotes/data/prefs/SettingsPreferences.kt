package com.papernotes.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-Einstellungen (persistiert): gewähltes Papier-Theme und Sortier-Modus. */
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKeyPref = stringPreferencesKey("paper_theme")
    private val sortModeKeyPref = stringPreferencesKey("sort_mode")

    /** Schlüssel des gewählten [com.papernotes.ui.theme.PaperTheme] (Default: AUTO). */
    val themeKey: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[themeKeyPref] ?: "AUTO"
    }

    suspend fun setThemeKey(key: String) {
        context.settingsDataStore.edit { it[themeKeyPref] = key }
    }

    /** Schlüssel des Grid-Sortier-Modus (Default: PINNWAND = freie Anordnung). */
    val sortModeKey: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[sortModeKeyPref] ?: "PINNWAND"
    }

    suspend fun setSortModeKey(key: String) {
        context.settingsDataStore.edit { it[sortModeKeyPref] = key }
    }

    private val gridColumnsPref = intPreferencesKey("grid_columns")

    /** Spaltenzahl des Notiz-Grids (1–3, per Pinch gewählt; Default 2). */
    val gridColumns: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[gridColumnsPref] ?: 2).coerceIn(1, 3)
    }

    suspend fun setGridColumns(columns: Int) {
        context.settingsDataStore.edit { it[gridColumnsPref] = columns.coerceIn(1, 3) }
    }

    private val appOpensPref = intPreferencesKey("app_opens")
    private val longPressHintSeenPref = booleanPreferencesKey("longpress_hint_seen")

    /** Zählt die App-Starts hoch und liefert den neuen Stand (für einmalige Hinweise). */
    suspend fun incrementAppOpens(): Int {
        val prefs = context.settingsDataStore.edit {
            it[appOpensPref] = (it[appOpensPref] ?: 0) + 1
        }
        return prefs[appOpensPref] ?: 0
    }

    /** true, sobald der Langdruck-Hinweis gezeigt oder die Auswahl selbst entdeckt wurde. */
    val longPressHintSeen: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[longPressHintSeenPref] ?: false
    }

    suspend fun markLongPressHintSeen() {
        context.settingsDataStore.edit { it[longPressHintSeenPref] = true }
    }
}
