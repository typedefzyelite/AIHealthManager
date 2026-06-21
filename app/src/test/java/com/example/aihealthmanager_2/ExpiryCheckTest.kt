package com.example.aihealthmanager_2

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 单元测试 003：药品有效期检测
 * diffDays < 0  → 已过期
 * diffDays <= 30 → 即将过期
 * diffDays <= 90 → 显示日期
 * diffDays > 90  → 正常
 */
class ExpiryCheckTest {

    private fun getExpiryStatus(expiryDate: String, today: Date): String {
        if (expiryDate.isEmpty()) return ""
        return try {
            val fmt = if (expiryDate.length <= 7)
                SimpleDateFormat("yyyy-MM", Locale.getDefault())
            else
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiry = fmt.parse(expiryDate) ?: return ""
            val diffDays = ((expiry.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
            when {
                diffDays < 0 -> "已过期"
                diffDays <= 30 -> "即将过期"
                diffDays <= 90 -> "日期标记:$expiryDate"
                else -> "正常:$expiryDate"
            }
        } catch (e: Exception) { "" }
    }

    private fun fixedDate(dateStr: String): Date {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!
    }

    @Test
    fun test_expiredMedicine_returnsExpired() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("2026-03-20", today)
        assertEquals("已过期", status)
    }

    @Test
    fun test_expiringWithin30Days_returnsWarning() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("2026-04-10", today)
        assertEquals("即将过期", status)
    }

    @Test
    fun test_expiringWithin90Days_showsDate() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("2026-06-01", today)
        assertEquals("日期标记:2026-06-01", status)
    }

    @Test
    fun test_farFutureExpiry_returnsNormal() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("2026-09-01", today)
        assertEquals("正常:2026-09-01", status)
    }

    @Test
    fun test_emptyExpiryDate_returnsEmpty() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("", today)
        assertEquals("", status)
    }

    @Test
    fun test_yearMonthFormat_parsesCorrectly() {
        val today = fixedDate("2026-03-24")
        val status = getExpiryStatus("2026-04", today)
        assertEquals("即将过期", status)
    }
}
