package com.keon.musicstream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    val favorites = MutableStateFlow(prefs.favorites())
    val theme = MutableStateFlow(prefs.theme)
    val apiKey = MutableStateFlow(prefs.apiKey)

    val popular = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val newReleases = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val recommended = MutableStateFlow<Load<List<Track>>>(Load.Idle)
    val results = MutableStateFlow<Load<List<Track>>>(Load.Idle)

    val current = MutableStateFlow<Track?>(null)
    private var queue: List<Track> = emptyList()

    private fun key() = apiKey.value.ifBlank { BuildConfig.YT_API_KEY }
    private fun region() = Locale.getDefault().country.ifBlank { "FR" }

    private fun launchLoad(
        target: MutableStateFlow<Load<List<Track>>>,
        block: suspend () -> List<Track>,
    ) {
        target.value = Load.Loading
        viewModelScope.launch {
            target.value = try {
                Load.Ok(block())
            } catch (e: ApiException) {
                Load.Err(e.message ?: "Erreur")
            } catch (e: Exception) {
                Load.Err("Erreur inattendue.")
            }
        }
    }

    private fun needs(s: Load<*>) = s is Load.Idle || s is Load.Err

    fun loadHome(force: Boolean = false) {
        if (force || needs(popular.value)) launchLoad(popular) { YouTubeApi.popular(key(), region()) }
        if (force || needs(newReleases.value)) launchLoad(newReleases) {
            YouTubeApi.search(key(), "nouveau clip officiel", order = "date")
        }
        if (force || needs(recommended.value)) launchLoad(recommended) {
            val seed = favorites.value.lastOrNull()?.artist ?: "top hits"
            YouTubeApi.search(key(), "$seed music")
        }
    }

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        launchLoad(results) { YouTubeApi.search(key(), q) }
    }

    fun play(track: Track, list: List<Track>) {
        queue = list
        current.value = track
    }

    fun next() {
        val i = queue.indexOfFirst { it.id == current.value?.id }
        if (i >= 0 && i + 1 < queue.size) current.value = queue[i + 1]
    }

    fun previous() {
        val i = queue.indexOfFirst { it.id == current.value?.id }
        if (i > 0) current.value = queue[i - 1]
    }

    fun toggleFavorite(t: Track) {
        val list = favorites.value
        favorites.value = if (list.any { it.id == t.id }) list.filterNot { it.id == t.id } else list + t
        prefs.saveFavorites(favorites.value)
    }

    fun setTheme(v: String) {
        prefs.theme = v
        theme.value = v
    }

    fun setApiKey(k: String) {
        prefs.apiKey = k.trim()
        apiKey.value = k.trim()
        popular.value = Load.Idle
        newReleases.value = Load.Idle
        recommended.value = Load.Idle
        results.value = Load.Idle
    }
}
