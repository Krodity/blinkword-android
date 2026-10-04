package uk.krodity.blinkword.data.discover

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_REDIRECTS = 5

/**
 * A plain GET. Redirects are followed by hand because [HttpURLConnection] won't
 * follow them across protocols, and Gutenberg's download links hop between
 * hosts on the way to a mirror.
 */
internal fun <T> httpGet(url: String, block: (InputStream) -> T): T {
    var target = URL(url)

    repeat(MAX_REDIRECTS) {
        val connection = (target.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "BlinkWord")
        }

        val code = connection.responseCode
        if (code in 200..299) return connection.inputStream.use(block)

        val location = connection.getHeaderField("Location")
        connection.disconnect()

        if (code !in 300..399 || location == null) throw IOException("HTTP $code for $target")
        target = URL(target, location)
    }

    throw IOException("Too many redirects for $url")
}
