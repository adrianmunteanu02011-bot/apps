package com.mp3os.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Colours picked from a cover (ARGB ints). Any of them can be missing. */
data class Swatches(val vibrant: Int?, val dominant: Int?, val muted: Int?, val light: Int?)

class Art(val image: ImageBitmap, val swatches: Swatches)

/** What the player currently shows: the song id and its cover (null art = placeholder). */
data class Cover(val id: Long?, val art: Art?)

object Covers {
    private object Miss

    private val big = LruCache<Long, Art>(8)
    private val noBig = HashSet<Long>()
    private val thumbs = LruCache<Long, Any>(400)

    fun cachedThumb(id: Long): ImageBitmap? = synchronized(thumbs) { thumbs.get(id) as? ImageBitmap }

    /** Small cover for list rows. Null when the file has no cover. */
    suspend fun thumb(context: Context, song: Song): ImageBitmap? = withContext(Dispatchers.IO) {
        val hit = synchronized(thumbs) { thumbs.get(song.id) }
        if (hit != null) return@withContext hit as? ImageBitmap
        val img = load(context, song, 192)?.asImageBitmap()
        synchronized(thumbs) { thumbs.put(song.id, img ?: Miss) }
        img
    }

    /** Large cover + palette for the player and the app colours. Null when there is no cover. */
    suspend fun big(context: Context, song: Song): Art? = withContext(Dispatchers.IO) {
        synchronized(big) { big.get(song.id) }?.let { return@withContext it }
        if (synchronized(noBig) { song.id in noBig }) return@withContext null

        val bmp = load(context, song, 1024)
        if (bmp == null) {
            synchronized(noBig) { noBig += song.id }
            return@withContext null
        }
        val swatches = runCatching {
            // thumbnails can be hardware bitmaps, Palette needs pixels it can read
            val soft = bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
            val p = Palette.from(soft).maximumColorCount(24).generate()
            Swatches(
                vibrant = (p.vibrantSwatch ?: p.lightVibrantSwatch)?.rgb,
                dominant = p.dominantSwatch?.rgb,
                muted = (p.mutedSwatch ?: p.darkMutedSwatch)?.rgb,
                light = (p.lightVibrantSwatch ?: p.lightMutedSwatch)?.rgb,
            )
        }.getOrDefault(Swatches(null, null, null, null))

        val art = Art(bmp.asImageBitmap(), swatches)
        synchronized(big) { big.put(song.id, art) }
        art
    }

    private fun load(context: Context, song: Song, px: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return runCatching { context.contentResolver.loadThumbnail(song.uri, Size(px, px), null) }.getOrNull()
    }
}
