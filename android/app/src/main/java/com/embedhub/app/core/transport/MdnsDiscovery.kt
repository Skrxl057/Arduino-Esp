package com.embedhub.app.core.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredEsp(
    val name: String,
    val host: String,
    val port: Int,
    val serviceType: String
)

class MdnsDiscovery(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredBoards = MutableStateFlow<List<DiscoveredEsp>>(emptyList())
    val discoveredBoards: StateFlow<List<DiscoveredEsp>> = _discoveredBoards.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    var isDiscovering: Boolean = false
        private set

    companion object {
        const val SERVICE_TYPE_HTTP = "_http._tcp."
        const val SERVICE_TYPE_ARDUINO = "_arduino._tcp."
    }

    fun startDiscovery(serviceType: String = SERVICE_TYPE_HTTP) {
        if (isDiscovering || nsdManager == null) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                isDiscovering = true
            }

            override fun onServiceFound(service: NsdServiceInfo?) {
                if (service == null) return
                // Resolve discovered service
                try {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

                        override fun onServiceResolved(resolved: NsdServiceInfo?) {
                            if (resolved == null) return
                            val host = resolved.host?.hostAddress ?: return
                            val port = resolved.port
                            val name = resolved.serviceName ?: "ESP Board"

                            val item = DiscoveredEsp(name, host, port, resolved.serviceType ?: serviceType)
                            val current = _discoveredBoards.value.toMutableList()
                            if (current.none { it.host == host && it.port == port }) {
                                current.add(item)
                                _discoveredBoards.value = current
                            }
                        }
                    })
                } catch (_: Exception) {}
            }

            override fun onServiceLost(service: NsdServiceInfo?) {
                if (service == null) return
                val current = _discoveredBoards.value.toMutableList()
                current.removeAll { it.name == service.serviceName }
                _discoveredBoards.value = current
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
            }
        }

        discoveryListener = listener
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
            isDiscovering = false
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (_: Exception) {}
        }
        discoveryListener = null
        isDiscovering = false
    }

    fun clear() {
        _discoveredBoards.value = emptyList()
    }
}
