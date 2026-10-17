package com.keon.musicstream

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

/** Client minimal de l'API YouTube Data v3 (métadonnées uniquement, aucun flux audio). */
object YouTubeApi {
    private const val BASE = "https://www.googleapis.com/youtube/v3/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 1 unité de quota. */
    suspend fun popular(key: String, region: String): List<Track> = get(
        key, "videos",
        mapOf(
            "part" to "snippet", "chart" to "mostPopular", "videoCategoryId" to "10",
            "regionCode" to region, "maxResults" to "20",
        ),
    )

    /** 100 unités de quota. Filtre : catégorie Musique, vidéos intégrables uniquement. */
    suspend fun search(key: String, query: String, order: String = "relevance"): List<Track> = get(
        key, "search",
        mapOf(
            "part" to "snippet", "type" to "video", "videoCategoryId" to "10",
            "videoEmbeddable" to "true", "safeSearch" to "moderate",
            "maxResults" to "25", "order" to order, "q" to query,
        ),
    )

    private suspend fun get(key: String, path: String, params: Map<String, String>): List<Track> =
        withContext(Dispatchers.IO) {
            if (key.isBlank()) {
                throw ApiException("Clé API YouTube manquante. Ajoute-la dans Paramètres.")
            }
            val url = (BASE + path).toHttpUrl().newBuilder().apply {
                params.forEach { (k, v) -> addQueryParameter(k, v) }
                addQueryParameter("key", key)
            }.build()
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    val body = r.body?.string().orEmpty()
                    if (!r.isSuccessful) throw ApiException(errorMessage(r.code, body))
                    parse(body)
                }
            } catch (e: IOException) {
                throw ApiException("Connexion réseau indisponible. Vérifie ta connexion.")
            }
        }

    private fun errorMessage(code: Int, body: String): String {
        val err = runCatching { JSONObject(body).getJSONObject("error") }.getOrNull()
        val reason = err?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
        return when {
            reason == "quotaExceeded" -> "Quota YouTube dépassé pour aujourd'hui. Réessaie demain."
            code == 400 || code == 403 && reason.contains("key", true) ->
                "Clé API invalide ou non autorisée."
            code == 403 -> "Accès refusé par l'API YouTube (${reason.ifBlank { "403" }})."
            else -> "Erreur YouTube ($code)."
        }
    }

    private fun parse(body: String): List<Track> {
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val idEl = item.opt("id")
            val videoId = when (idEl) {
                is JSONObject -> idEl.optString("videoId")
                is String -> idEl
                else -> ""
            }
            val sn = item.optJSONObject("snippet") ?: continue
            if (videoId.isBlank()) continue
            val th = sn.optJSONObject("thumbnails")
            val thumb = listOf("high", "medium", "default")
                .firstNotNullOfOrNull { th?.optJSONObject(it)?.optString("url")?.takeIf { u -> u.isNotBlank() } }
                ?: ""
            out += Track(
                id = videoId,
                title = clean(sn.optString("title")),
                artist = clean(sn.optString("channelTitle")).removeSuffix(" - Topic"),
                thumb = thumb,
            )
        }
        return out
    }

    private fun clean(s: String) = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
}
