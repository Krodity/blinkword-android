package uk.krodity.blinkword.data

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.provider.Settings

/**
 * Why a network request from this app can't get through, when Android knows.
 *
 * A system-level block on the app (LineageOS-style "Network access" toggles,
 * Data Saver, restricted background data) doesn't show up as a refused
 * connection: DNS lookups for the app simply fail, and the user sees "Unable
 * to resolve host" with a perfectly good connection. Asking ConnectivityManager
 * tells the two apart, so the message can point at the real fix.
 */
enum class NetworkProblem { BLOCKED_FOR_APP, OFFLINE }

@Suppress("DEPRECATION") // NetworkInfo.DetailedState.BLOCKED has no non-deprecated equivalent for a one-shot check.
fun networkProblem(context: Context): NetworkProblem? {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
    val info = cm.activeNetworkInfo
    return when {
        info?.detailedState == NetworkInfo.DetailedState.BLOCKED -> NetworkProblem.BLOCKED_FOR_APP
        // A blocked app is also handed no default network at all.
        cm.activeNetwork == null && info == null -> NetworkProblem.OFFLINE
        else -> null
    }
}

fun NetworkProblem.message(): String = when (this) {
    NetworkProblem.BLOCKED_FOR_APP ->
        "BlinkWord isn't allowed to use the network on this device. " +
            "Turn on network access for BlinkWord in its app settings (often under " +
            "\"Mobile data & Wi-Fi\" or \"Network access\"), then try again."
    NetworkProblem.OFFLINE -> "No internet connection. Connect to Wi-Fi or mobile data and try again."
}

/** Opens this app's system settings page, where per-app network access lives. */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
