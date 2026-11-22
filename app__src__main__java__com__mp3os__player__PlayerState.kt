package com.mp3os.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlin.math.roundToInt

/** Bridges the background PlaybackService to Compose. */
@Stable
class PlayerState {

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    var isPlaying by mutableStateOf(false)
        private set
    var currentMediaId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var pitch by mutableFloatStateOf(1f)
        private set
    var linked by mutableStateOf(false)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    fun attach(c: MediaController) {
        controller = c
        c.addListener(listener)
        sync(c)
    }

    fun detach() {
        controller?.removeListener(listener)
        controller = null
    }

    private fun sync(p: Player) {
        isPlaying = p.isPlaying
        currentMediaId = p.currentMediaItem?.mediaId
        title = p.mediaMetadata.title?.toString().orEmpty()
        artist = p.mediaMetadata.artist?.toString().orEmpty()
        duration = if (p.duration == C.TIME_UNSET) 0L else p.duration
        position = p.currentPosition
        speed = p.playbackParameters.speed
        pitch = p.playbackParameters.pitch
        shuffle = p.shuffleModeEnabled
        repeatMode = p.repeatMode
    }

    /** Called ~4x per second to move the seek bar. */
    fun tick() {
        controller?.let { position = it.currentPosition }
    }

    fun play(songs: List<Song>, index: Int) {
        val c = controller ?: return
        val items = songs.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() {
        val c = controller ?: return
        // like a real player: restart the song if you're past 3 seconds, else go back
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        position = ms
    }

    fun skipBy(deltaMs: Long) {
        val c = controller ?: return
        val target = (c.currentPosition + deltaMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        seekTo(target)
    }

    /** Snap to 0.01 steps and keep inside 0.5x..2.0x. */
    private fun snap(v: Float) = ((v * 100f).roundToInt() / 100f).coerceIn(0.5f, 2f)

    private fun apply(s: Float, p: Float) {
        speed = s
        pitch = p
        controller?.setPlaybackParameters(PlaybackParameters(s, p))
    }

    fun updateSpeed(value: Float) {
        val s = snap(value)
        apply(s, if (linked) s else pitch)
    }

    fun updatePitch(value: Float) {
        val p = snap(value)
        apply(if (linked) p else speed, p)
    }

    fun nudgeSpeed(delta: Float) = updateSpeed(speed + delta)

    fun nudgePitch(delta: Float) = updatePitch(pitch + delta)

    /** When linked, speed and pitch move together (like a tape / vinyl). Speed wins when you turn it on. */
    fun toggleLinked() {
        linked = !linked
        if (linked) apply(speed, speed)
    }

    fun resetSpeedAndPitch() = apply(1f, 1f)

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
