package com.embedhub.app.core.transport

import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Universal abstraction layer for all physical and wireless hardware transports.
 */
interface Link {
    val name: String
    val type: LinkType
    val mtu: Int get() = 244 // Default MTU-3 for modern BLE, higher for TCP/UDP
    val state: StateFlow<ConnectionState>
    val incomingLines: Flow<String>
    val incomingBytes: Flow<ByteArray>

    suspend fun open(): Result<Unit>
    suspend fun close()
    suspend fun send(bytes: ByteArray): Result<Unit>
}
