package com.keon.musicstream

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private fun fmt(sec: Float): String {
    val s = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val track by vm.current.collectAsState()
    val favs by vm.favorites.collectAsState()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    var dur by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val view = remember { YouTubePlayerView(ctx).apply { enableAutomaticInitialization = false } }

    DisposableEffect(view) {
        lifecycleOwner.lifecycle.addObserver(view)
        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                playing = state == PlayerConstants.PlayerState.PLAYING
                if (state == PlayerConstants.PlayerState.ENDED) vm.next()
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                if (!dragging) pos = second
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                dur = duration
            }

            override fun onError(youTubePlayer: YouTubePlayer, error2: PlayerConstants.PlayerError) {
                error = "Cette vidéo ne peut pas être lue dans une application tierce (${error2.name})."
            }
        }
        // Contrôles officiels du lecteur YouTube conservés (controls = 1)
        val options = IFramePlayerOptions.Builder().controls(1).rel(0).build()
        view.initialize(listener, options)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        }
    }

    LaunchedEffect(player, track?.id) {
        val p = player
        val t = track
        if (p != null && t != null) {
            error = null
            pos = 0f
            dur = 0f
            p.loadVideo(t.id, 0f)
        }
    }

    val t = track ?: return
    val isFav = favs.any { it.id == t.id }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
        Spacer(Modifier.height(8.dp))
        AndroidView(
            factory = { view },
            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.height(20.dp))
        Text(t.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
        Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = pos.coerceIn(0f, maxOf(dur, 1f)),
            onValueChange = { dragging = true; pos = it },
            onValueChangeFinished = { player?.seekTo(pos); dragging = false },
            valueRange = 0f..maxOf(dur, 1f),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(pos), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt(dur), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.toggleFavorite(t) }) {
                Icon(
                    if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Favori",
                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { vm.previous() }) { Icon(Icons.Filled.SkipPrevious, "Précédent", Modifier.size(36.dp)) }
            FilledIconButton(onClick = { if (playing) player?.pause() else player?.play() }, modifier = Modifier.size(64.dp)) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Lecture", Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.next() }) { Icon(Icons.Filled.SkipNext, "Suivant", Modifier.size(36.dp)) }
            Spacer(Modifier.size(48.dp))
        }
    }
}
