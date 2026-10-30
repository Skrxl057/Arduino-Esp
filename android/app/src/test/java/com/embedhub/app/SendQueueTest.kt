package com.embedhub.app

import com.embedhub.app.core.queue.SendQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendQueueTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testChunkingToMtu() = testScope.runTest {
        val sentChunks = mutableListOf<ByteArray>()
        val queue = SendQueue(
            scope = this,
            sendBlock = { chunk ->
                sentChunks.add(chunk)
                Result.success(Unit)
            },
            mtuSize = 10,
            minIntervalMs = 0
        )
        queue.start()

        // Send 25 bytes with MTU = 10 -> Should split into 10 + 10 + 5 bytes
        val data = ByteArray(25) { it.toByte() }
        queue.enqueue(data)

        advanceUntilIdle()
        queue.stop()

        assertEquals(3, sentChunks.size)
        assertEquals(10, sentChunks[0].size)
        assertEquals(10, sentChunks[1].size)
        assertEquals(5, sentChunks[2].size)
    }
}
