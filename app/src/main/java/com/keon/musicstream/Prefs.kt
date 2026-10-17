package com.keon.musicstream

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stockage local : favoris, thème, clé API saisie par l'utilisateur. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("musicstream", Context.MODE_PRIVATE)

    fun favorites(): List<Track> = runCatching {
        val arr = JSONArray(sp.getString("favs", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Track(o.getString("id"), o.getString("title"), o.getString("artist"), o.getString("thumb"))
        }
    }.getOrDefault(emptyList())

    fun saveFavorites(list: List<Track>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("thumb", it.thumb))
        }
        sp.edit().putString("favs", arr.toString()).apply()
    }

    var theme: String
        get() = sp.getString("theme", "dark") ?: "dark"
        set(v) = sp.edit().putString("theme", v).apply()

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) = sp.edit().putString("api_key", v).apply()
}
