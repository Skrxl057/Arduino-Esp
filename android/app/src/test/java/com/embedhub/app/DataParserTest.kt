package com.embedhub.app

import com.embedhub.app.core.parser.DataParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataParserTest {

    @Before
    fun setUp() {
        DataParser.resetCsvHeaders()
    }

    @Test
    fun testParseKeyValuePairs() {
        val input = "temp=24.5,rpm=1200,status=OK"
        val results = DataParser.parse(input)

        assertEquals(3, results.size)
        assertEquals("temp", results[0].key)
        assertEquals(24.5, results[0].numericValue!!, 0.001)

        assertEquals("rpm", results[1].key)
        assertEquals(1200.0, results[1].numericValue!!, 0.001)

        assertEquals("status", results[2].key)
        assertNull(results[2].numericValue)
        assertEquals("OK", results[2].rawText)
    }

    @Test
    fun testParseColonKeyValuePairs() {
        val input = "voltage:3.28 humidity:58"
        val results = DataParser.parse(input)

        assertEquals(2, results.size)
        assertEquals("voltage", results[0].key)
        assertEquals(3.28, results[0].numericValue!!, 0.001)
        assertEquals("humidity", results[1].key)
        assertEquals(58.0, results[1].numericValue!!, 0.001)
    }

    @Test
    fun testParseJson() {
        val json = """{"temp": 28.1, "light": 450, "alert": false}"""
        val results = DataParser.parse(json)

        val temp = results.find { it.key == "temp" }
        assertNotNull(temp)
        assertEquals(28.1, temp!!.numericValue!!, 0.001)

        val alert = results.find { it.key == "alert" }
        assertNotNull(alert)
        assertEquals(0.0, alert!!.numericValue!!, 0.001)
    }

    @Test
    fun testParseCsvWithHeader() {
        val header = "roll,pitch,yaw"
        DataParser.parse(header) // Stores header
        val values = "1.25,-0.45,179.8"
        val results = DataParser.parse(values)

        assertEquals(3, results.size)
        assertEquals("roll", results[0].key)
        assertEquals(1.25, results[0].numericValue!!, 0.001)
        assertEquals("yaw", results[2].key)
        assertEquals(179.8, results[2].numericValue!!, 0.001)
    }

    @Test
    fun testMalformedDataNeverCrashes() {
        val malformed = "%%%$$#@! random bad binary \u0000\u00FF"
        val results = DataParser.parse(malformed)
        assertNotNull(results)
    }
}
