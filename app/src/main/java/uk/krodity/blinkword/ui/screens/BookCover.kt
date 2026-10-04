package uk.krodity.blinkword.ui.screens

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.krodity.blinkword.data.discover.httpGet
import java.io.File

/** Thumbnails are small and re-shown constantly while scrolling; decode each one once. */
private val coverCache = LruCache<String, ImageBitmap>(64)

/** Covers only ever render as thumbnails, so there's no reason to decode them full size. */
private const val TARGET_WIDTH_PX = 400

/**
 * A book's real cover when it has one -- extracted from the ePub at import, or
 * fetched from the catalogue while browsing Discover -- falling back to the
 * generated letter tile.
 */
@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    coverPath: String? = null,
    coverUrl: String? = null,
) {
    val key = coverPath ?: coverUrl

    val bitmap by produceState<ImageBitmap?>(initialValue = key?.let { coverCache.get(it) }, key) {
        if (key == null || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (coverPath != null) decodeFile(coverPath) else decodeUrl(key)
            }.getOrNull()?.also { coverCache.put(key, it) }
        }
    }

    val image = bitmap
    if (image == null) {
        BookThumbnail(title = title, modifier = modifier)
    } else {
        Box(modifier = modifier) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun decodeFile(path: String): ImageBitmap? {
    if (!File(path).exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth) }
    return BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}

private fun decodeUrl(url: String): ImageBitmap? {
    val bytes = httpGet(url) { it.readBytes() }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

private fun sampleSizeFor(sourceWidth: Int): Int {
    var sample = 1
    while (sourceWidth > 0 && sourceWidth / sample > TARGET_WIDTH_PX * 2) sample *= 2
    return sample
}
