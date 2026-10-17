package com.keon.musicstream

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7CF6),
    onPrimary = Color.White,
    background = Color(0xFF09090B),
    surface = Color(0xFF09090B),
    surfaceVariant = Color(0xFF18181B),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA1A1AA),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color.White,
    outline = Color(0xFF3F3F46),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF6D5BD0),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFEDEDF0),
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val theme by vm.theme.collectAsState()
    val dark = when (theme) {
        "light" -> false
        "system" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var showPlayer by rememberSaveable { mutableStateOf(false) }
            val current by vm.current.collectAsState()
            val open: (Track, List<Track>) -> Unit = { t, l ->
                vm.play(t, l)
                showPlayer = true
            }
            if (showPlayer && current != null) {
                BackHandler { showPlayer = false }
                PlayerScreen(vm) { showPlayer = false }
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                            val tabs = listOf(
                                Icons.Filled.Home to "Accueil",
                                Icons.Filled.Search to "Recherche",
                                Icons.Filled.Favorite to "Favoris",
                                Icons.Filled.Settings to "Réglages",
                            )
                            tabs.forEachIndexed { i, (icon, label) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(icon, label) },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    },
                ) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (tab) {
                            0 -> HomeScreen(vm, open)
                            1 -> SearchScreen(vm, open)
                            2 -> FavoritesScreen(vm, open)
                            else -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumb(url: String, modifier: Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
    )
}

@Composable
private fun TrackCard(t: Track, onClick: () -> Unit) {
    Column(
        Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Thumb(t.thumb, Modifier.fillMaxWidth().aspectRatio(16 / 9f))
        Spacer(Modifier.height(8.dp))
        Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        Text(
            t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackRow(t: Track, fav: Boolean, onToggleFav: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(t.thumb, Modifier.width(112.dp).aspectRatio(16 / 9f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(
                t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleFav) {
            Icon(
                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (fav) "Retirer des favoris" else "Ajouter aux favoris",
                tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun Section(
    title: String,
    state: Load<List<Track>>,
    onRetry: () -> Unit,
    onOpen: (Track, List<Track>) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        when (state) {
            is Load.Idle, is Load.Loading -> Centered { CircularProgressIndicator(Modifier.size(28.dp)) }
            is Load.Err -> Centered {
                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry) { Text("Réessayer") }
            }
            is Load.Ok -> if (state.data.isEmpty()) {
                Centered { Text("Aucun résultat.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.data, key = { it.id }) { t -> TrackCard(t) { onOpen(t, state.data) } }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (Track, List<Track>) -> Unit) {
    val popular by vm.popular.collectAsState()
    val news by vm.newReleases.collectAsState()
    val reco by vm.recommended.collectAsState()
    LaunchedEffect(Unit) { vm.loadHome() }
    val retry = { vm.loadHome(force = true) }
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Text("MusicStream", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 16.dp)) }
        item { Section("Musiques populaires", popular, retry, onOpen) }
        item { Section("Nouveautés", news, retry, onOpen) }
        item { Section("Recommandations", reco, retry, onOpen) }
    }
}

@Composable
fun SearchScreen(vm: AppViewModel, onOpen: (Track, List<Track>) -> Unit) {
    val results by vm.results.collectAsState()
    val favs by vm.favorites.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Chansons, artistes, clips…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        when (val s = results) {
            is Load.Idle -> Centered { Text("Recherche une chanson pour commencer.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            is Load.Loading -> Centered { CircularProgressIndicator() }
            is Load.Err -> Centered {
                Text(s.message)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.search(query) }) { Text("Réessayer") }
            }
            is Load.Ok -> if (s.data.isEmpty()) {
                Centered { Text("Aucun résultat pour « $query ».", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn {
                    items(s.data, key = { it.id }) { t ->
                        TrackRow(t, favs.any { it.id == t.id }, { vm.toggleFavorite(t) }) { onOpen(t, s.data) }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(vm: AppViewModel, onOpen: (Track, List<Track>) -> Unit) {
    val favs by vm.favorites.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text("Favoris", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        if (favs.isEmpty()) {
            Centered { Text("Aucun favori pour l'instant.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn {
                items(favs, key = { it.id }) { t ->
                    TrackRow(t, true, { vm.toggleFavorite(t) }) { onOpen(t, favs) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val theme by vm.theme.collectAsState()
    val savedKey by vm.apiKey.collectAsState()
    var key by rememberSaveable { mutableStateOf(savedKey) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Réglages", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text("Thème", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dark" to "Sombre", "light" to "Clair", "system" to "Système").forEach { (v, label) ->
                    FilterChip(selected = theme == v, onClick = { vm.setTheme(v) }, label = { Text(label) })
                }
            }
        }
        item {
            Text("Clé API YouTube Data v3", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("Optionnel si intégrée au build") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.setApiKey(key) }) { Text("Enregistrer") }
        }
        item {
            Text("À propos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "MusicStream recherche des clips musicaux via l'API YouTube Data v3 et les lit avec " +
                    "le lecteur officiel YouTube. Aucun fichier n'est téléchargé et aucun flux audio " +
                    "n'est extrait. La lecture s'arrête lorsque l'application passe en arrière-plan, " +
                    "conformément aux conditions d'utilisation de YouTube. Certaines vidéos peuvent " +
                    "refuser la lecture dans une application tierce.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
