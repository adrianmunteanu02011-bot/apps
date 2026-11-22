@file:OptIn(ExperimentalMaterial3Api::class)

package com.mp3os.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Filled.LibraryMusic),
    Player("Now playing", Icons.Filled.MusicNote),
    Apps("Apps", Icons.Filled.Apps),
}

private fun audioPermission() =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

// ================================================================ App root

@Composable
fun App(state: PlayerState) {
    val context = LocalContext.current

    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasAudioPermission(context) }
    val askForPermissions = {
        val wanted = buildList {
            add(audioPermission())
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(wanted.toTypedArray())
    }
    LaunchedEffect(Unit) { if (!granted) askForPermissions() }

    var refresh by remember { mutableIntStateOf(0) }
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var scanning by remember { mutableStateOf(granted) }
    LaunchedEffect(granted, refresh) {
        if (!granted) {
            songs = emptyList()
            scanning = false
            return@LaunchedEffect
        }
        scanning = true
        songs = withContext(Dispatchers.IO) { loadSongs(context) }
        scanning = false
    }

    // moves the seek bar
    LaunchedEffect(Unit) {
        while (true) {
            state.tick()
            delay(if (state.isPlaying) 100 else 400)
        }
    }

    val song = remember(songs, state.currentMediaId) {
        state.currentMediaId?.let { id -> songs.firstOrNull { it.id.toString() == id } }
    }
    // the cover (and its colours) only change once the new one is loaded, so there is no flash
    var cover by remember { mutableStateOf(Cover(null, null)) }
    LaunchedEffect(song?.id) {
        cover = Cover(song?.id, song?.let { Covers.big(context, it) })
    }

    val dance = rememberDance(state.isPlaying)
    val scheme = animatedScheme(remember(cover.art) { schemeFrom(cover.art?.swatches) })

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalDance provides dance) {
            Root(
                state = state,
                songs = songs,
                scanning = scanning,
                granted = granted,
                cover = cover,
                onRescan = { refresh++ },
                onGrant = { askForPermissions() },
            )
        }
    }
}

@Composable
private fun Root(
    state: PlayerState,
    songs: List<Song>,
    scanning: Boolean,
    granted: Boolean,
    cover: Cover,
    onRescan: () -> Unit,
    onGrant: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.Library) }
    BackHandler(enabled = tab != Tab.Library) { tab = Tab.Library }

    // kept here so the list keeps its place when you switch tabs
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }

    // critically damped: slides in without the overshoot/wobble
    val slideSpec = spring<IntOffset>(dampingRatio = 1f, stiffness = 380f)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Backdrop()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    AnimatedVisibility(
                        visible = tab != Tab.Player && state.currentMediaId != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        MiniPlayer(state, songs) { tab = Tab.Player }
                    }
                    NavPill(tab) { tab = it }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.padding(padding).fillMaxSize(),
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(slideSpec) { it * dir } togetherWith
                        slideOutHorizontally(slideSpec) { -it * dir }
                },
                label = "tabs",
            ) { t ->
                // blur sideways while the page is moving in or out, sharp once it has settled
                val blur by transition.animateFloat(
                    transitionSpec = { tween(420) },
                    label = "tabBlur",
                ) { if (it == EnterExitState.Visible) 0f else 1f }

                Box(Modifier.fillMaxSize().motionBlur({ blur * 48f }, { 0f })) {
                    when (t) {
                        Tab.Library -> LibraryScreen(
                            songs = songs,
                            scanning = scanning,
                            granted = granted,
                            state = state,
                            listState = listState,
                            query = query,
                            onQuery = { query = it },
                            onRescan = onRescan,
                            onGrant = onGrant,
                        )
                        Tab.Player -> PlayerScreen(state, cover)
                        Tab.Apps -> AppsScreen()
                    }
                }
            }
        }
    }
}

// ================================================================ Navigation + mini player

@Composable
private fun NavPill(selected: Tab, onSelect: (Tab) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = cs.surfaceContainerHigh, tonalElevation = 3.dp) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Tab.values().forEach { t ->
                    val on = t == selected
                    val bg by animateColorAsState(
                        if (on) cs.primaryContainer else Color.Transparent,
                        springEffects(),
                        label = "navBg",
                    )
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { onSelect(t) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            t.icon, t.label,
                            tint = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        )
                        AnimatedVisibility(
                            visible = on,
                            enter = fadeIn() + expandHorizontally(springSpatial()),
                            exit = fadeOut() + shrinkHorizontally(springSpatial()),
                        ) {
                            Text(
                                t.label,
                                Modifier.padding(start = 8.dp),
                                color = cs.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(state: PlayerState, songs: List<Song>, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dance = LocalDance.current
    val song = remember(songs, state.currentMediaId) {
        songs.firstOrNull { it.id.toString() == state.currentMediaId }
    }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(32.dp),
        color = cs.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongCover(
                song, RoundedCornerShape(22.dp),
                Modifier.size(52.dp).graphicsLayer {
                    val s = 1f + 0.09f * dance.bass
                    scaleX = s
                    scaleY = s
                    rotationZ = sin(dance.t * 1.6f) * 3f * dance.level
                },
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    state.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = cs.onSurface, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = cs.onSurfaceVariant, fontSize = 12.sp,
                )
            }
            FilledTonalIconButton(onClick = { state.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
            PressButton(
                onClick = { state.togglePlay() },
                modifier = Modifier.size(48.dp),
                container = cs.primary,
                restCorner = if (state.isPlaying) 24.dp else 16.dp,
                pressedCorner = 10.dp,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play/pause", tint = cs.onPrimary,
                )
            }
            FilledTonalIconButton(onClick = { state.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
        }
    }
}

// ================================================================ Covers

/** Shown for audio files that have no cover art. */
@Composable
private fun CoverPlaceholder(modifier: Modifier = Modifier, rotation: (() -> Float)? = null) {
    val cs = MaterialTheme.colorScheme
    val cookie = CookieShape
    Box(modifier.background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize(0.82f)
                .graphicsLayer { rotationZ = rotation?.invoke() ?: 0f }
                .clip(cookie)
                .background(cs.primaryContainer),
        )
        Icon(
            Icons.Filled.MusicNote, null,
            Modifier.fillMaxSize(0.36f),
            tint = cs.onPrimaryContainer,
        )
    }
}

/** Small cover (list rows, mini player). */
@Composable
private fun SongCover(song: Song?, shape: Shape, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val img by produceState<ImageBitmap?>(song?.let { Covers.cachedThumb(it.id) }, song?.id) {
        value = song?.let { Covers.thumb(context, it) }
    }
    Box(modifier.clip(shape)) {
        val bmp = img
        if (bmp != null) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            CoverPlaceholder(Modifier.fillMaxSize())
        }
    }
}

/** Three little bars that bounce to the music. */
@Composable
private fun EqBars(color: Color, modifier: Modifier = Modifier) {
    val dance = LocalDance.current
    Canvas(modifier.size(22.dp)) {
        val bw = size.width / 5f
        for (i in 0 until 3) {
            val wobble = 0.5f + 0.5f * sin(dance.t * (3f + i * 1.3f) + i * 2f)
            val energy = dance.bass * 0.7f + dance.level * 0.3f
            val h = size.height * (0.22f + 0.78f * energy * (0.4f + 0.6f * wobble)).coerceIn(0.22f, 1f)
            drawRoundRect(
                color,
                topLeft = Offset(i * bw * 2f, size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f),
            )
        }
    }
}

// ================================================================ Library

private fun segment(index: Int, count: Int): Shape {
    val top = if (index == 0) 28.dp else 6.dp
    val bottom = if (index == count - 1) 28.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    scanning: Boolean,
    granted: Boolean,
    state: PlayerState,
    listState: LazyListState,
    query: String,
    onQuery: (String) -> Unit,
    onRescan: () -> Unit,
    onGrant: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shown = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }

    // vertical motion blur that grows with scroll speed
    var scrollBlur by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        var lastPos = 0f
        var lastT = 0L
        while (true) {
            if (scrollBlur == 0f && !listState.isScrollInProgress) {
                // idle: stop running every frame until scrolling starts again
                snapshotFlow { listState.isScrollInProgress }.first { it }
                lastT = 0L
            }
            withFrameNanos { now ->
                val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                val pos = (listState.firstVisibleItemIndex * itemSize + listState.firstVisibleItemScrollOffset).toFloat()
                if (lastT != 0L) {
                    val dt = ((now - lastT) / 1_000_000_000f).coerceAtLeast(0.001f)
                    val speed = abs(pos - lastPos) / dt // px per second
                    val target = if (speed < 300f) 0f else (speed / 260f).coerceAtMost(14f)
                    val next = scrollBlur + (target - scrollBlur) * 0.2f
                    val v = if (target == 0f && next < 0.05f) 0f else next
                    if (v != scrollBlur) scrollBlur = v
                }
                lastPos = pos
                lastT = now
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text("${songs.size} songs", color = cs.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = onRescan) { Icon(Icons.Filled.Refresh, "Rescan library") }
        }

        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    FilledTonalIconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, "Clear") }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = cs.surfaceContainerHigh,
                unfocusedContainerColor = cs.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )

        when {
            !granted -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onGrant) { Text("Allow access to your music") }
            }
            scanning && songs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(56.dp))
            }
            songs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No songs found.\nCopy music onto the phone, then tap the refresh icon.",
                    textAlign = TextAlign.Center,
                    color = cs.onSurfaceVariant,
                )
            }
            else -> {
                Button(
                    onClick = { if (shown.isNotEmpty()) state.play(shown.shuffled(), 0) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.Shuffle, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Shuffle all", fontWeight = FontWeight.SemiBold)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().motionBlur({ 0f }, { scrollBlur }),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(shown, key = { _, s -> s.id }) { index, song ->
                        SongRow(
                            song = song,
                            current = song.id.toString() == state.currentMediaId,
                            shape = segment(index, shown.size),
                            modifier = Modifier.animateItem(),
                        ) { state.play(shown, index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, current: Boolean, shape: Shape, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dance = LocalDance.current
    val bg by animateColorAsState(
        if (current) cs.primaryContainer else cs.surfaceContainer,
        springEffects(),
        label = "rowBg",
    )
    Surface(onClick = onClick, shape = shape, color = bg, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongCover(
                song, RoundedCornerShape(if (current) 24.dp else 16.dp),
                Modifier.size(54.dp).graphicsLayer {
                    if (current) {
                        val s = 1f + 0.1f * dance.bass
                        scaleX = s
                        scaleY = s
                        rotationZ = sin(dance.t * 1.6f) * 4f * dance.level
                    }
                },
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) cs.onPrimaryContainer else cs.onSurface,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                )
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) cs.onPrimaryContainer.copy(alpha = 0.75f) else cs.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            if (current) EqBars(cs.onPrimaryContainer)
            else Text(fmt(song.durationMs), color = cs.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

// ================================================================ Now playing

@Composable
private fun PlayerScreen(state: PlayerState, cover: Cover) {
    val cs = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val duration = state.duration

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        BigCover(cover)
        Spacer(Modifier.height(16.dp))

        Text(
            state.title.ifEmpty { "Nothing playing" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            state.artist,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))
        WavySeekBar(
            progress = {
                if (dragging) dragFrac
                else if (duration > 0) (state.position.toFloat() / duration).coerceIn(0f, 1f)
                else 0f
            },
            playing = state.isPlaying,
            onScrub = { dragging = true; dragFrac = it },
            onFinish = {
                if (dragging) {
                    state.seekTo((dragFrac * duration).toLong())
                    dragging = false
                }
            },
        )
        TimeRow(
            position = { if (dragging) (dragFrac * duration).toLong() else state.position },
            duration = duration,
        )

        Spacer(Modifier.height(16.dp))
        Transport(state)
        Spacer(Modifier.height(20.dp))
        TunePanel(state)
        Spacer(Modifier.height(24.dp))
    }
}

/** Own scope, so the 10x/s position updates only recompose these two labels, not the whole player. */
@Composable
private fun TimeRow(position: () -> Long, duration: Long) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmt(position()), color = cs.onSurfaceVariant, fontSize = 13.sp)
        Text(fmt(duration), color = cs.onSurfaceVariant, fontSize = 13.sp)
    }
}

/** The big cover: stays still (only the background dances), blurs while it swaps to the next one. */
@Composable
private fun BigCover(cover: Cover) {
    val dance = LocalDance.current
    val cs = MaterialTheme.colorScheme
    val glow = cs.primary
    val spatial = springSpatial<Float>()
    val effects = springEffects<Float>()

    Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(10.dp)) {
        // glow that swells with the beat
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * (0.62f + 0.22f * dance.bass)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.18f + 0.35f * dance.bass), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = cover,
                contentKey = { it.id },
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(effects) + scaleIn(spatial, initialScale = 0.82f)) togetherWith
                        (fadeOut(effects) + scaleOut(spatial, targetScale = 1.12f))
                },
                label = "cover",
            ) { c ->
                val blur by transition.animateFloat(
                    transitionSpec = { tween(450) },
                    label = "coverBlur",
                ) { if (it == EnterExitState.Visible) 0f else 1f }

                Box(
                    Modifier
                        .fillMaxSize()
                        .motionBlur({ blur * 60f }, { blur * 60f })
                        .clip(RoundedCornerShape(40.dp)),
                ) {
                    val art = c.art
                    if (art != null) {
                        Image(art.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        CoverPlaceholder(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/** Seek bar drawn as a wave that ripples while the music plays. */
@Composable
private fun WavySeekBar(
    progress: () -> Float,
    playing: Boolean,
    onScrub: (Float) -> Unit,
    onFinish: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val dance = LocalDance.current
    val amp by animateFloatAsState(
        if (playing) 1f else 0f,
        springSlow(),
        label = "waveAmp",
    )
    val scrub by rememberUpdatedState(onScrub)
    val finish by rememberUpdatedState(onFinish)
    val primary = cs.primary
    val track = cs.surfaceContainerHighest

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    scrub((off.x / size.width).coerceIn(0f, 1f))
                    finish()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> scrub((off.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { finish() },
                    onDragCancel = { finish() },
                ) { change, _ ->
                    change.consume()
                    scrub((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val stroke = 6.dp.toPx()
        val cy = size.height / 2f
        val inset = 6.dp.toPx()
        val usable = size.width - inset * 2
        val endX = inset + usable * progress()
        val a = 6.dp.toPx() * amp
        val waveLen = 30.dp.toPx()
        val phase = dance.t * 2.6f
        val thumbGap = 8.dp.toPx()

        // played part: the wave
        val path = Path()
        var x = inset
        path.moveTo(x, cy + a * sin(phase))
        val stopX = (endX - thumbGap).coerceAtLeast(inset)
        while (x < stopX) {
            x = (x + 3f).coerceAtMost(stopX)
            path.lineTo(x, cy + a * sin((x - inset) / waveLen * 2f * PI.toFloat() - phase))
        }
        drawPath(path, primary, style = Stroke(stroke, cap = StrokeCap.Round))

        // remaining part: flat track
        val restStart = endX + thumbGap
        if (restStart < size.width - inset) {
            drawLine(track, Offset(restStart, cy), Offset(size.width - inset, cy), stroke, StrokeCap.Round)
        }
        // handle
        drawRoundRect(
            primary,
            topLeft = Offset(endX - 2.5.dp.toPx(), cy - 14.dp.toPx()),
            size = Size(5.dp.toPx(), 28.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
}

/** A rounded button whose corners squish while pressed (Material 3 Expressive feel). */
@Composable
private fun PressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color,
    restCorner: Dp,
    pressedCorner: Dp,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) pressedCorner else restCorner,
        springFast(),
        label = "corner",
    )
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        springFast(),
        label = "pressScale",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        interactionSource = source,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun Transport(state: PlayerState) {
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressButton(
            onClick = { state.previous() },
            modifier = Modifier.weight(1f).height(72.dp),
            container = cs.secondaryContainer, restCorner = 28.dp, pressedCorner = 12.dp,
        ) { Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(34.dp), tint = cs.onSecondaryContainer) }

        // squircle while paused, round pill while playing
        PressButton(
            onClick = { state.togglePlay() },
            modifier = Modifier.weight(1.5f).height(92.dp),
            container = cs.primary,
            restCorner = if (state.isPlaying) 46.dp else 30.dp,
            pressedCorner = 16.dp,
        ) {
            Icon(
                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Play/pause", Modifier.size(48.dp), tint = cs.onPrimary,
            )
        }

        PressButton(
            onClick = { state.next() },
            modifier = Modifier.weight(1f).height(72.dp),
            container = cs.secondaryContainer, restCorner = 28.dp, pressedCorner = 12.dp,
        ) { Icon(Icons.Filled.SkipNext, "Next", Modifier.size(34.dp), tint = cs.onSecondaryContainer) }
    }

    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallToggle(Icons.Filled.Shuffle, "Shuffle", state.shuffle, Modifier.weight(1f)) { state.toggleShuffle() }
        SmallToggle(Icons.Filled.Replay10, "Back 10 seconds", false, Modifier.weight(1f)) { state.skipBy(-10_000) }
        SmallToggle(Icons.Filled.Forward10, "Forward 10 seconds", false, Modifier.weight(1f)) { state.skipBy(10_000) }
        SmallToggle(
            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            "Repeat", state.repeatMode != Player.REPEAT_MODE_OFF, Modifier.weight(1f),
        ) { state.cycleRepeat() }
    }
}

@Composable
private fun SmallToggle(icon: ImageVector, desc: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    PressButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        container = if (on) cs.primaryContainer else cs.surfaceContainerHigh,
        restCorner = if (on) 26.dp else 16.dp,
        pressedCorner = 8.dp,
    ) {
        Icon(icon, desc, tint = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- speed & pitch

@Composable
private fun TunePanel(state: PlayerState) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(32.dp), color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Speed & pitch",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                FilledTonalIconButton(onClick = { state.resetSpeedAndPitch() }) {
                    Icon(Icons.Filled.RestartAlt, "Reset speed and pitch")
                }
            }

            TuneRow("Speed", state.speed, { state.updateSpeed(it) }, { state.nudgeSpeed(it) })

            // sync: when on, moving one moves the other
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = cs.outlineVariant)
                PressButton(
                    onClick = { state.toggleLinked() },
                    modifier = Modifier.padding(horizontal = 12.dp).height(44.dp),
                    container = if (state.linked) cs.primary else cs.surfaceContainerHighest,
                    restCorner = 22.dp,
                    pressedCorner = 10.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (state.linked) Icons.Filled.Link else Icons.Filled.LinkOff,
                            "Sync speed and pitch",
                            Modifier.size(20.dp),
                            tint = if (state.linked) cs.onPrimary else cs.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.linked) "Synced" else "Sync",
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.linked) cs.onPrimary else cs.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(Modifier.weight(1f), color = cs.outlineVariant)
            }

            TuneRow("Pitch", state.pitch, { state.updatePitch(it) }, { state.nudgePitch(it) })
        }
    }
}

@Composable
private fun TuneRow(label: String, value: Float, onChange: (Float) -> Unit, onNudge: (Float) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(
                String.format(Locale.US, "%.2fx", value),
                color = cs.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatButton(Icons.Filled.Remove, "$label down 0.01") { onNudge(-0.01f) }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = 0.5f..2f,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            RepeatButton(Icons.Filled.Add, "$label up 0.01") { onNudge(0.01f) }
        }
    }
}

/** Tap for one 0.01 step; hold to keep stepping, faster the longer you hold. */
@Composable
private fun RepeatButton(icon: ImageVector, desc: String, onStep: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val step by rememberUpdatedState(onStep)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerHighest)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    step()
                    val job = scope.launch {
                        delay(380)
                        var wait = 120L
                        while (true) {
                            step()
                            delay(wait)
                            wait = (wait * 0.92f).toLong().coerceAtLeast(35L)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = cs.onSurface)
    }
}

// ================================================================ Apps

@Composable
private fun AppsScreen() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val apps by produceState(emptyList<AppEntry>()) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Apps",
            Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(88.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            context.packageManager.getLaunchIntentForPackage(app.packageName)
                                ?.let { context.startActivity(it) }
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(app.icon, app.label, Modifier.size(56.dp))
                    Text(
                        app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = cs.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
