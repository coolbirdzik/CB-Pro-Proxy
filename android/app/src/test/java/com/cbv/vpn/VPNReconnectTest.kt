package com.cbv.vpn

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkInfo
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VPNReconnectTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
    private val manager get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifi = ShadowNetwork.newInstance(10)

    @Before
    fun prepare() {
        prefs.edit().clear().putBoolean("auto_connect_enabled", true)
                .putString("last_connected_profile_id", "test")
                .putString("profiles", """[{"id":"test","name":"Test","host":"127.0.0.1","port":1080}]""")
                .apply()
        VPNIntentReceiver.cancelPendingReconnect()
        org.robolectric.shadows.ShadowVpnService.setPrepareResult(null)
        shadowOf(manager).clearAllNetworks()
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun setNetwork(transport: Int) {
        val caps = NetworkCapabilities()
        shadowOf(caps).addTransportType(transport)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowOf(manager).addNetwork(wifi, ShadowNetworkInfo.newInstance(
                android.net.NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI, 0, true, true))
        shadowOf(manager).setNetworkCapabilities(wifi, caps)
    }

    private fun networkChanged() {
        VPNIntentReceiver().onReceive(context, Intent(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    @Test
    fun manualStopInvalidatesRecoveryAndPermissionContinuationUntilExplicitConnect() {
        val original = VPNConnectionPolicy.connect(context)
        assertTrue(VPNConnectionPolicy.canRecover(context, original))
        VPNConnectionPolicy.disconnect(context)
        assertFalse(VPNConnectionPolicy.isCurrent(context, original))
        assertFalse(VPNConnectionPolicy.canRecover(context))
        val next = VPNConnectionPolicy.connect(context)
        assertTrue(VPNConnectionPolicy.canRecover(context, next))
        assertFalse(VPNConnectionPolicy.isCurrent(context, original))
    }

    @Test
    fun vpnTransportCannotMasqueradeAsAnUnderlyingNetwork() {
        setNetwork(NetworkCapabilities.TRANSPORT_VPN)
        assertFalse(VPNConnectionPolicy.hasUnderlyingNetwork(context))
        setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
        assertTrue(VPNConnectionPolicy.hasUnderlyingNetwork(context))
    }

    @Test
    fun healthyNetworkCanRecoverWithoutCreatingANewUserIntent() {
        val generation = VPNConnectionPolicy.connect(context)
        networkChanged()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        val start = shadowOf(context).nextStartedService
        assertNotNull(start)
        assertEquals(generation, start.getLongExtra(VPNConnectionPolicy.GENERATION, -1L))
        assertEquals(generation, VPNConnectionPolicy.generation(context))
    }

    @Test
    fun stopCancelsAlreadyScheduledNetworkRecovery() {
        VPNConnectionPolicy.connect(context)
        networkChanged()
        VPNIntentReceiver().onReceive(context, Intent(VPNIntentReceiver.ACTION_STOP_VPN))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        assertTrue(prefs.getBoolean("manually_disconnected", false))
        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun losingNetworkDuringDebounceDoesNotStartVpn() {
        VPNConnectionPolicy.connect(context)
        networkChanged()
        shadowOf(manager).clearAllNetworks()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun bootDoesNotOverrideManualStop() {
        VPNConnectionPolicy.disconnect(context)
        VPNIntentReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(prefs.getBoolean("manually_disconnected", false))
        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun disabledAutoConnectPreventsRecovery() {
        VPNConnectionPolicy.connect(context)
        prefs.edit().putBoolean("auto_connect_enabled", false).apply()
        assertFalse(VPNConnectionPolicy.canRecover(context))
    }

    @Test
    fun notificationStopOverridesAutomationAndStickyRestart() {
        val controller = Robolectric.buildService(VPNConnectionService::class.java)
        val service = controller.get()
        VPNConnectionPolicy.connect(context)
        prefs.edit().putBoolean("automation_session_active", true).apply()
        service.onStartCommand(Intent().putExtra("action", VPNConnectionService.COMMAND_STOP)
                .putExtra("force", true), 0, 1)
        assertTrue(prefs.getBoolean("manually_disconnected", false))
        assertFalse(prefs.getBoolean("automation_session_active", true))
        assertEquals(android.app.Service.START_NOT_STICKY, service.onStartCommand(null, 0, 2))
    }

    @Test
    fun permissionResultAfterStopCannotStartService() {
        val generation = VPNConnectionPolicy.connect(context)
        val profile = org.json.JSONObject(prefs.getString("profiles", "[]")!!
                .removePrefix("[").removeSuffix("]"))
        profile.put(VPNConnectionPolicy.GENERATION, generation)
        VPNConnectionPolicy.disconnect(context)
        VPNIntentReceiver.startVpnService(context, profile)
        assertNull(shadowOf(context).nextStartedService)
        assertTrue(prefs.getBoolean("manually_disconnected", false))
    }

    @Test
    fun statusQueryWhileStoppedDoesNotKeepServiceAlive() {
        val service = Robolectric.buildService(VPNConnectionService::class.java).get()
        service.onStartCommand(Intent().putExtra("action", VPNConnectionService.COMMAND_STATUS), 0, 1)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun notificationStopActionIsExplicitEvenDuringAutomation() {
        val service = Robolectric.buildService(VPNConnectionService::class.java).get()
        val create = VPNConnectionService::class.java.getDeclaredMethod("createNotification", String::class.java)
        create.isAccessible = true
        val notification = create.invoke(service, "Connected to proxy") as android.app.Notification
        val stop = shadowOf(notification.actions.single().actionIntent).savedIntent
        assertEquals(VPNConnectionService.COMMAND_STOP, stop.getStringExtra("action"))
        assertTrue(stop.getBooleanExtra("force", false))
    }

    @Test
    fun lateStopCommandCannotCancelANewerExplicitConnect() {
        val controller = Robolectric.buildService(VPNConnectionService::class.java)
        VPNConnectionPolicy.disconnect(context)
        val stopGeneration = VPNConnectionPolicy.generation(context)
        val current = VPNConnectionPolicy.connect(context)
        controller.get().onStartCommand(Intent().putExtra("action", VPNConnectionService.COMMAND_STOP)
                .putExtra("force", true).putExtra(VPNConnectionPolicy.GENERATION, stopGeneration), 0, 1)
        assertTrue(VPNConnectionPolicy.isCurrent(context, current))
    }
}
