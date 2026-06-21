package com.example.aihealthmanager_2

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryActivity : AppCompatActivity() {

    private lateinit var chartHeart: LineChart
    private lateinit var chartOxygen: LineChart
    private lateinit var chartSleep: LineChart
    private lateinit var tvCorrelationAnalysis: TextView
    private var allHistoryList: List<DailyHealthData> = emptyList()
    private var medicationRecords: List<MedicationRecord> = emptyList()
    private var medicineList: List<MedicineItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val rgPeriod = findViewById<RadioGroup>(R.id.rg_history_period)

        chartHeart = findViewById(R.id.chart_heart)
        chartOxygen = findViewById(R.id.chart_oxygen)
        chartSleep = findViewById(R.id.chart_sleep)
        tvCorrelationAnalysis = findViewById(R.id.tv_correlation_analysis)

        btnBack.setOnClickListener { finish() }

        allHistoryList = loadHistoryData()
        medicationRecords = loadMedicationRecords()
        medicineList = loadMedicineList()

        updateCharts(7)
        updateCorrelationAnalysis()

        rgPeriod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_week) {
                updateCharts(7)
            } else {
                updateCharts(30)
            }
        }
    }

    private fun updateCharts(days: Int) {
        val dataToShow = allHistoryList.takeLast(days)

        if (dataToShow.isNotEmpty()) {
            val dates = dataToShow.map { it.date.takeLast(5) }

            val heartEntries = dataToShow.mapIndexed { index, data -> Entry(index.toFloat(), data.heartRate.toFloat()) }
            val sleepEntries = dataToShow.mapIndexed { index, data -> Entry(index.toFloat(), data.sleepDuration.toFloat()) }
            val oxygenEntries = dataToShow.mapIndexed { index, data -> Entry(index.toFloat(), data.bloodOxygen.toFloat()) }

            // 恢复使用 accent color, 并配合浅色透明填充
            setupChart(chartHeart, heartEntries, dates, "静息心率",
                ContextCompat.getColor(this, R.color.accent_orange),
                30,
                50f, 100f)

            setupChart(chartSleep, sleepEntries, dates, "睡眠时长 (h)",
                ContextCompat.getColor(this, R.color.accent_purple),
                30,
                4.0f, 12.0f)

            setupChart(chartOxygen, oxygenEntries, dates, "血氧 (%)",
                ContextCompat.getColor(this, R.color.accent_green),
                30,
                85.0f, 100.0f)
        }
    }

    private fun setupChart(chart: LineChart, entries: List<Entry>, dates: List<String>, label: String, lineColor: Int, fillAlpha: Int, min: Float, max: Float) {
        val dataSet = LineDataSet(entries, label)
        dataSet.color = lineColor
        dataSet.setCircleColor(lineColor)
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawValues(false)

        // 核心：使用与线条同色的浅色透明填充
        dataSet.setDrawFilled(true)
        dataSet.fillColor = lineColor
        dataSet.fillAlpha = fillAlpha // 使用传入的透明度

        val lineData = LineData(dataSet)
        chart.data = lineData

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        xAxis.textColor = Color.GRAY
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.axisLineColor = Color.LTGRAY
        xAxis.granularity = 1f
        xAxis.labelCount = if (dates.size > 10) 6 else dates.size

        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.GRAY
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = Color.parseColor("#e8e8e8")
        yAxisLeft.axisMinimum = min
        yAxisLeft.axisMaximum = max
        chart.axisRight.isEnabled = false

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)

        chart.notifyDataSetChanged()
        chart.invalidate()
    }


    private fun loadHistoryData(): List<DailyHealthData> {
        val sp = getSharedPreferences("HealthApp", MODE_PRIVATE)
        val json = sp.getString("history_data", null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<DailyHealthData>>() {}.type)
        } else {
            emptyList()
        }
    }

    private fun loadMedicationRecords(): List<MedicationRecord> {
        val sp = getSharedPreferences("HealthApp", MODE_PRIVATE)
        val json = sp.getString("medication_records", null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<MedicationRecord>>() {}.type)
        } else {
            emptyList()
        }
    }

    private fun loadMedicineList(): List<MedicineItem> {
        val sp = getSharedPreferences("HealthApp", MODE_PRIVATE)
        val json = sp.getString("medicine_list_v2", null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<MedicineItem>>() {}.type)
        } else {
            emptyList()
        }
    }

    private fun updateCorrelationAnalysis() {
        val results = mutableListOf<String>()

        if (allHistoryList.size < 3) {
            tvCorrelationAnalysis.text = "📍 数据量不足，请持续记录以获得更精准的分析"
            return
        }

        val recentData = allHistoryList.takeLast(7)
        val avgHeartRate = recentData.map { it.heartRate }.average()
        val avgSleep = recentData.map { it.sleepDuration }.average()
        val avgOxygen = recentData.map { it.bloodOxygen }.average()

        if (medicineList.isNotEmpty()) {
            results.add("💊 药箱药品数：${medicineList.size} 种")
        }

        if (medicationRecords.isNotEmpty()) {
            val weekRecords = medicationRecords.size
            results.add("📋 累计服药记录：$weekRecords 次")
        }

        if (avgHeartRate > 80) {
            results.add("❤️ 近期心率偏高(均值${String.format("%.0f", avgHeartRate)})，建议关注")
        } else {
            results.add("❤️ 近期心率稳定(均值${String.format("%.0f", avgHeartRate)})，状态良好")
        }

        if (avgSleep < 6.5) {
            results.add("💤 近期睡眠不足(均值${String.format("%.1f", avgSleep)}h)，可能影响药效")
        } else {
            results.add("💤 近期睡眠充足(均值${String.format("%.1f", avgSleep)}h)，有利康复")
        }

        if (avgOxygen > 0 && avgOxygen < 95) {
            results.add("🫁 近期血氧偏低(均值${String.format("%.0f", avgOxygen)}%)，建议关注")
        } else if (avgOxygen >= 95) {
            results.add("🫁 血氧正常(均值${String.format("%.0f", avgOxygen)}%)，状态良好")
        }

        val heartTrend = if (recentData.size >= 3) {
            val first = recentData.take(3).map { it.heartRate }.average()
            val last = recentData.takeLast(3).map { it.heartRate }.average()
            when {
                last - first > 5 -> "📈 心率有上升趋势"
                first - last > 5 -> "📉 心率有下降趋势"
                else -> "➡️ 心率趋势平稳"
            }
        } else ""

        if (heartTrend.isNotEmpty()) {
            results.add(heartTrend)
        }

        tvCorrelationAnalysis.text = results.joinToString("\n")
    }
}