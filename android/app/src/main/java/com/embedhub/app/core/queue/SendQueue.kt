package com.embedhub.app.core.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe asynchronous send queue.
 * Serializes writes, chunks data according to MTU, prevents packet collision,
 * and maintains rate limiting (e.g., 20 Hz for analog joystick updates).
 */
class SendQueue(
    private val scope: CoroutineScope,
    private val sendBlock: suspend (ByteArray) -> Result<Unit>,
    private var mtuSize: Int = 244,
    private var minIntervalMs: Long = 20L // Max 50Hz by default, adjustable per profile
) {
    private val channel = Channel<ByteArray>(capacity = 100)
    private val mutex = Mutex()
    private var drainJob: Job? = null
    var isRunning = false
        private set

    fun setMtu(newMtu: Int) {
        mtuSize = maxOf(20, newMtu)
    }

    fun setRateLimit(rateHz: Int) {
        minIntervalMs = if (rateHz > 0) (1000L / rateHz) else 0L
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        drainJob = scope.launch(Dispatchers.IO) {
            for (packet in channel) {
                mutex.withLock {
                    // Chunk packet according to MTU
                    var offset = 0
                    while (offset < packet.size) {
                        val chunkSize = minOf(mtuSize, packet.size - offset)
                        val chunk = ByteArray(chunkSize)
                        System.arraycopy(packet, offset, chunk, 0, chunkSize)
                        sendBlock(chunk)
                        offset += chunkSize
                        if (offset < packet.size) {
                            delay(10) // Small inter-chunk pacing for BLE / USB buffers
                        }
                    }
                    if (minIntervalMs > 0) {
                        delay(minIntervalMs)
                    }
                }
            }
        }
    }

    suspend fun enqueue(data: ByteArray): Boolean {
        if (!isRunning) start()
        return channel.trySend(data).isSuccess
    }

    suspend fun enqueueImmediate(data: ByteArray): Result<Unit> {
        // High priority (e.g. EMERGENCY STOP)
        return mutex.withLock {
            sendBlock(data)
        }
    }

    fun stop() {
        isRunning = false
        drainJob?.cancel()
        drainJob = null
    }

    fun clear() {
        while (channel.tryReceive().isSuccess) {
            // Drain remaining
        }
    }
}
