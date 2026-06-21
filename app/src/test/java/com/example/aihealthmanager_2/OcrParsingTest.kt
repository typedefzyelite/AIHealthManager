package com.example.aihealthmanager_2

import org.junit.Test
import org.junit.Assert.*

/**
 * 单元测试 001：AI OCR 文本解析
 * 测试 AI 返回的 "药名#频率#剂量" 格式经 split("#") 后各字段的正确性
 */
class OcrParsingTest {

    data class ParsedMedicine(val name: String, val frequency: Int, val dosage: String)

    private fun parseAiResult(aiResult: String): ParsedMedicine {
        val parts = aiResult.trim().split("#")
        val medName = parts[0]
        val frequency = parts.getOrElse(1) { "1" }.toIntOrNull() ?: 1
        val dosage = parts.getOrElse(2) { "适量" }
        return ParsedMedicine(medName, frequency, dosage)
    }

    @Test
    fun test_normalMedicine_parsedCorrectly() {
        val aiResponse = "感冒灵颗粒#3#一次1袋"
        val result = parseAiResult(aiResponse)

        assertEquals("感冒灵颗粒", result.name)
        assertEquals(3, result.frequency)
        assertEquals("一次1袋", result.dosage)
    }

    @Test
    fun test_missingDosage_usesDefault() {
        val aiResponse = "阿莫西林胶囊#2"
        val result = parseAiResult(aiResponse)

        assertEquals("阿莫西林胶囊", result.name)
        assertEquals(2, result.frequency)
        assertEquals("适量", result.dosage)
    }

    @Test
    fun test_invalidFormat_fallsBackSafely() {
        val aiResponse = "无法识别药品信息"
        val result = parseAiResult(aiResponse)

        assertEquals("无法识别药品信息", result.name)
        assertEquals(1, result.frequency)
        assertEquals("适量", result.dosage)
    }

    @Test
    fun test_frequencyNotNumber_defaultsToOne() {
        val aiResponse = "布洛芬缓释胶囊#两次#一次1粒"
        val result = parseAiResult(aiResponse)

        assertEquals("布洛芬缓释胶囊", result.name)
        assertEquals(1, result.frequency)
        assertEquals("一次1粒", result.dosage)
    }
}
