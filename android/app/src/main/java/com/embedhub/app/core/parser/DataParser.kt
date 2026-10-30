package com.embedhub.app.core.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ParsedStreamValue(
    val key: String,
    val numericValue: Double?,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

object DataParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var cachedCsvHeaders: List<String>? = null

    /**
     * Parses an incoming string into a map of stream values.
     * Guaranteed safe against malformed data.
     */
    fun parse(input: String): List<ParsedStreamValue> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Try JSON
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val jsonResults = parseJson(trimmed)
            if (jsonResults.isNotEmpty()) return jsonResults
        }

        // 2. Try Key=Value or Key:Value (single or space/comma separated pairs)
        val kvResults = parseKeyValuePairs(trimmed)
        if (kvResults.isNotEmpty()) return kvResults

        // 3. Try CSV (comma, semicolon or tab delimited numbers)
        val csvResults = parseCsv(trimmed)
        if (csvResults.isNotEmpty()) return csvResults

        // 4. Fallback: Raw text packet
        return listOf(
            ParsedStreamValue(
                key = "raw",
                numericValue = trimmed.toDoubleOrNull(),
                rawText = trimmed
            )
        )
    }

    private fun parseJson(input: String): List<ParsedStreamValue> {
        return try {
            val element = json.parseToJsonElement(input)
            if (element is JsonObject) {
                element.mapNotNull { (key, jsonVal) ->
                    val primitive = jsonVal.jsonPrimitive
                    val num = primitive.doubleOrNull
                        ?: primitive.booleanOrNull?.let { if (it) 1.0 else 0.0 }
                    ParsedStreamValue(
                        key = key,
                        numericValue = num,
                        rawText = primitive.content
                    )
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseKeyValuePairs(input: String): List<ParsedStreamValue> {
        val results = mutableListOf<ParsedStreamValue>()
        // Splits by comma, space or semicolon between pairs e.g. "temp=24.5,humid=60"
        val tokens = input.split(Regex("[,;\\s]+"))
        for (token in tokens) {
            if (token.contains('=')) {
                val parts = token.split('=', limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    val k = parts[0].trim()
                    val v = parts[1].trim()
                    results.add(ParsedStreamValue(k, v.toDoubleOrNull(), v))
                }
            } else if (token.contains(':')) {
                val parts = token.split(':', limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    val k = parts[0].trim()
                    val v = parts[1].trim()
                    results.add(ParsedStreamValue(k, v.toDoubleOrNull(), v))
                }
            }
        }
        return results
    }

    private fun parseCsv(input: String): List<ParsedStreamValue> {
        val parts = input.split(',').map { it.trim() }
        if (parts.size <= 1) return emptyList()

        // Check if all are doubles
        val doubles = parts.map { it.toDoubleOrNull() }
        val allDoubles = doubles.all { it != null }

        if (!allDoubles) {
            // Might be a header line! e.g. "roll,pitch,yaw"
            val allLetters = parts.all { it.isNotEmpty() && !it[0].isDigit() }
            if (allLetters) {
                cachedCsvHeaders = parts
                return emptyList()
            }
        } else {
            // Assign keys using cached header or default col0, col1...
            val headers = cachedCsvHeaders
            return parts.mapIndexed { index, str ->
                val key = if (headers != null && index < headers.size) headers[index] else "col$index"
                ParsedStreamValue(
                    key = key,
                    numericValue = doubles[index],
                    rawText = str
                )
            }
        }

        return emptyList()
    }

    fun resetCsvHeaders() {
        cachedCsvHeaders = null
    }
}
