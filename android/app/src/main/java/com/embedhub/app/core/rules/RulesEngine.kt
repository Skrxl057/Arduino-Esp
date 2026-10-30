package com.embedhub.app.core.rules

import com.embedhub.app.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class RuleExecutionLog(
    val timestamp: Long,
    val ruleId: String,
    val ruleName: String,
    val actionSummary: String,
    val success: Boolean
)

class RulesEngine(
    private val scope: CoroutineScope,
    private val onSendCommand: suspend (targetSlot: Int, command: String) -> Unit,
    private val onShowNotification: (title: String, body: String) -> Unit,
    private val onVibrate: (durationMs: Long) -> Unit,
    private val onSpeak: (text: String) -> Unit
) {
    private val _rules = mutableListOf<AutomationRule>()
    val rules: List<AutomationRule> get() = _rules.toList()

    private val _executionHistory = MutableSharedFlow<RuleExecutionLog>(replay = 50)
    val executionHistory: SharedFlow<RuleExecutionLog> = _executionHistory.asSharedFlow()

    private val latestNumericData = mutableMapOf<String, Double>()

    fun setRules(newRules: List<AutomationRule>) {
        _rules.clear()
        _rules.addAll(newRules)
    }

    fun addRule(rule: AutomationRule) {
        _rules.add(rule)
    }

    fun updateRule(updated: AutomationRule) {
        val idx = _rules.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            _rules[idx] = updated
        } else {
            _rules.add(updated)
        }
    }

    fun deleteRule(ruleId: String) {
        _rules.removeAll { it.id == ruleId }
    }

    /**
     * Called whenever incoming telemetry data is parsed
     */
    fun onDataReceived(key: String, value: Double) {
        val previous = latestNumericData[key]
        latestNumericData[key] = value

        val now = System.currentTimeMillis()
        for (i in _rules.indices) {
            val rule = _rules[i]
            if (!rule.isEnabled) continue
            if (now - rule.lastExecutedAt < rule.cooldownMs) continue

            var triggerMatched = false
            when (val trigger = rule.trigger) {
                is RuleTrigger.DataReceived -> {
                    if (trigger.key == key) {
                        triggerMatched = evaluateComparison(value, trigger.operator, trigger.threshold)
                    }
                }
                is RuleTrigger.DataChanged -> {
                    if (trigger.key == key && previous != null && previous != value) {
                        triggerMatched = true
                    }
                }
                else -> {}
            }

            if (triggerMatched) {
                // Check optional conditions
                val conditionPassed = rule.condition?.let { evalCondition(it) } ?: true
                if (conditionPassed) {
                    _rules[i] = rule.copy(lastExecutedAt = now)
                    executeRuleActions(rule)
                }
            }
        }
    }

    fun onConnectionEvent(slot: Int, isConnected: Boolean) {
        val now = System.currentTimeMillis()
        for (i in _rules.indices) {
            val rule = _rules[i]
            if (!rule.isEnabled) continue
            if (now - rule.lastExecutedAt < rule.cooldownMs) continue

            if (rule.trigger is RuleTrigger.ConnectionEvent) {
                val t = rule.trigger
                if ((t.targetSlot == slot || t.targetSlot == -1) && t.isConnected == isConnected) {
                    if (rule.condition?.let { evalCondition(it) } ?: true) {
                        _rules[i] = rule.copy(lastExecutedAt = now)
                        executeRuleActions(rule)
                    }
                }
            }
        }
    }

    fun onPhoneEvent(eventType: String) {
        val now = System.currentTimeMillis()
        for (i in _rules.indices) {
            val rule = _rules[i]
            if (!rule.isEnabled) continue
            if (now - rule.lastExecutedAt < rule.cooldownMs) continue

            if (rule.trigger is RuleTrigger.PhoneEvent && rule.trigger.eventType == eventType) {
                if (rule.condition?.let { evalCondition(it) } ?: true) {
                    _rules[i] = rule.copy(lastExecutedAt = now)
                    executeRuleActions(rule)
                }
            }
        }
    }

    /**
     * Executes actions of a rule with support for delays
     */
    fun executeRuleActions(rule: AutomationRule) {
        scope.launch {
            val actionDescriptions = mutableListOf<String>()
            for (action in rule.actions) {
                when (action) {
                    is RuleAction.SendCommand -> {
                        onSendCommand(action.targetSlot, action.command)
                        actionDescriptions.add("TX [Slot ${action.targetSlot}]: ${action.command}")
                    }
                    is RuleAction.ShowNotification -> {
                        onShowNotification(action.title, action.body)
                        actionDescriptions.add("Notify: ${action.title}")
                    }
                    is RuleAction.HapticVibrate -> {
                        onVibrate(action.durationMs)
                        actionDescriptions.add("Vibrate ${action.durationMs}ms")
                    }
                    is RuleAction.TextToSpeech -> {
                        onSpeak(action.text)
                        actionDescriptions.add("Speak: ${action.text}")
                    }
                    is RuleAction.Delay -> {
                        actionDescriptions.add("Delay ${action.delayMs}ms")
                        delay(action.delayMs)
                    }
                }
            }
            _executionHistory.tryEmit(
                RuleExecutionLog(
                    timestamp = System.currentTimeMillis(),
                    ruleId = rule.id,
                    ruleName = rule.name,
                    actionSummary = actionDescriptions.joinToString("; "),
                    success = true
                )
            )
        }
    }

    /**
     * Manual Test-Run button
     */
    fun testRun(rule: AutomationRule) {
        executeRuleActions(rule)
    }

    private fun evalCondition(condition: RuleCondition): Boolean {
        return when (condition) {
            is RuleCondition.Compare -> {
                val current = latestNumericData[condition.key] ?: return false
                evaluateComparison(current, condition.operator, condition.value)
            }
            is RuleCondition.And -> condition.conditions.all { evalCondition(it) }
            is RuleCondition.Or -> condition.conditions.any { evalCondition(it) }
        }
    }

    fun evaluateComparison(actual: Double, operator: String, threshold: Double): Boolean {
        return when (operator) {
            ">" -> actual > threshold
            "<" -> actual < threshold
            ">=" -> actual >= threshold
            "<=" -> actual <= threshold
            "==" -> Math.abs(actual - threshold) < 0.0001
            "!=" -> Math.abs(actual - threshold) >= 0.0001
            else -> false
        }
    }
}
