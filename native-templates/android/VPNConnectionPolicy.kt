package com.cbv.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Owns user intent independently of connection health and delayed recovery work. */
object VPNConnectionPolicy {
    const val GENERATION = "connection_generation"

    private fun prefs(context: Context) =
            context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)

    fun generation(context: Context): Long = prefs(context).getLong(GENERATION, 0L)

    @Synchronized
    fun connect(context: Context): Long {
        val next = generation(context) + 1
        prefs(context).edit().putLong(GENERATION, next)
                .putBoolean("manually_disconnected", false).apply()
        VPNIntentReceiver.cancelPendingReconnect()
        return next
    }

    @Synchronized
    fun disconnect(context: Context) {
        prefs(context).edit().putLong(GENERATION, generation(context) + 1)
                .putBoolean("manually_disconnected", true)
                .putBoolean("automation_session_active", false).apply()
        VPNIntentReceiver.cancelPendingReconnect()
    }

    fun isCurrent(context: Context, expected: Long): Boolean =
            expected == generation(context) &&
                    !prefs(context).getBoolean("manually_disconnected", false)

    fun canRecover(context: Context, expected: Long = generation(context)): Boolean =
            isCurrent(context, expected) &&
                    prefs(context).getBoolean("auto_connect_enabled", false) &&
                    hasUnderlyingNetwork(context)

    fun hasUnderlyingNetwork(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.allNetworks.any { network ->
            val caps = manager.getNetworkCapabilities(network) ?: return@any false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
