package com.rotd.havenpad.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.rotd.havenpad.storage.AndroidAppContext

actual fun startConnectivityMonitor() {
    val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return

    fun refreshFromActive() {
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val online = caps != null && (
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            )
        if (online) ConnectivityMonitor.reportOnline()
        else ConnectivityMonitor.reportOffline()
    }

    refreshFromActive()

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    cm.registerNetworkCallback(
        request,
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                ConnectivityMonitor.reportOnline()
            }

            override fun onLost(network: Network) {
                // Another network may still be up — re-check.
                refreshFromActive()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val online = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                if (online) ConnectivityMonitor.reportOnline()
                else refreshFromActive()
            }
        }
    )
}
