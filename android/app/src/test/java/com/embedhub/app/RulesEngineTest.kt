package com.embedhub.app

import com.embedhub.app.core.model.AutomationRule
import com.embedhub.app.core.model.RuleAction
import com.embedhub.app.core.model.RuleTrigger
import com.embedhub.app.core.rules.RulesEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RulesEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testRuleTriggersWhenThresholdExceeded() = testScope.runTest {
        var commandSent: String? = null
        val engine = RulesEngine(
            scope = this,
            onSendCommand = { _, cmd -> commandSent = cmd },
            onShowNotification = { _, _ -> },
            onVibrate = {},
            onSpeak = {}
        )

        val rule = AutomationRule(
            id = "test_temp",
            name = "Temp Safety",
            trigger = RuleTrigger.DataReceived("temp", ">", 40.0),
            actions = listOf(RuleAction.SendCommand(0, "STOP"))
        )
        engine.setRules(listOf(rule))

        // Normal temperature: should not trigger
        engine.onDataReceived("temp", 35.0)
        advanceUntilIdle()
        assertNull(commandSent)

        // Overheating: should trigger STOP
        engine.onDataReceived("temp", 42.5)
        advanceUntilIdle()
        assertEquals("STOP", commandSent)
    }

    @Test
    fun testOperatorEvaluations() {
        val engine = RulesEngine(
            scope = testScope,
            onSendCommand = { _, _ -> },
            onShowNotification = { _, _ -> },
            onVibrate = {},
            onSpeak = {}
        )

        assertTrue(engine.evaluateComparison(50.0, ">", 40.0))
        assertFalse(engine.evaluateComparison(30.0, ">", 40.0))
        assertTrue(engine.evaluateComparison(10.0, "<", 20.0))
        assertTrue(engine.evaluateComparison(10.0, "==", 10.0))
        assertTrue(engine.evaluateComparison(10.5, "!=", 10.0))
    }
}
