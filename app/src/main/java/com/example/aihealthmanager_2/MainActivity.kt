package com.example.aihealthmanager_2

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.FileProvider as FProvider
import com.heytap.databaseengine.HeytapHealthApi
import com.heytap.databaseengine.apiv2.HResponse
import com.heytap.databaseengine.apiv2.auth.AuthResult
import com.heytap.databaseengine.apiv3.DataReadRequest
import com.heytap.databaseengine.apiv3.data.DataType
import com.heytap.databaseengine.apiv3.data.DataSet
import com.heytap.databaseengine.apiv3.data.Element
import com.heytap.databaseengine.model.proxy.UserDeviceInfoProxy

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, android.hardware.SensorEventListener {

    private lateinit var imageView: ImageView
    private lateinit var tvResult: TextView

    private lateinit var cardMedDetails: View
    private lateinit var tvScanListDisplay: TextView
    private lateinit var btnManageScanList: TextView
    private lateinit var btnClearScanList: TextView

    // 设备信息控件
    private lateinit var tvDeviceStatusLabel: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var deviceStatusDot: View

    // 心率 & 步数
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartStatus: TextView
    private lateinit var tvSteps: TextView
    private lateinit var progressSteps: ProgressBar

    // 睡眠控件
    private lateinit var tvSleepVal: TextView
    private lateinit var tvSleepStatus: TextView

    // 血氧控件
    private lateinit var tvBloodOxygen: TextView
    private lateinit var tvOxygenStatus: TextView

    // 个人中心控件
    private lateinit var tvReportContent: TextView
    private lateinit var tvMedicineListDisplay: TextView
    private lateinit var btnClearList: TextView
    private lateinit var btnCreateReport: View
    private lateinit var rbMonth: android.widget.RadioButton
    private lateinit var btnTakeMedicine: View
    private lateinit var tvMedicationRecords: TextView
    private lateinit var btnExportPdf: View
    private lateinit var tvComplianceRate: TextView
    private lateinit var tvTakenCount: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var btnRefreshCompliance: TextView
    private lateinit var tvTimeMorning: TextView
    private lateinit var tvTimeNoon: TextView
    private lateinit var tvTimeEvening: TextView
    private lateinit var btnManageMedicine: TextView
    private lateinit var btnManageRecords: TextView
    private lateinit var tvAiAnalysis: TextView
    private lateinit var btnRefreshAnalysis: TextView
    private lateinit var switchElderMode: com.google.android.material.switchmaterial.SwitchMaterial
    private var isElderMode = false

    // 页面视图
    private lateinit var pageHome: View
    private lateinit var pageHealth: View
    private lateinit var pageUser: View

    // 首页新控件
    private lateinit var btnSearchMedicine: View
    private lateinit var btnAskAi: View
    private lateinit var tvHealthTip: TextView

    // 安全守护控件
    private lateinit var switchFallDetection: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var tvFallDetectionStatus: TextView
    private lateinit var tvEmergencyContact: TextView

    private var imageUri: Uri? = null
    private var lastOcrContent: String = ""
    private var myMedicineList = ArrayList<MedicineItem>()
    private var historyList = ArrayList<DailyHealthData>()
    private var medicationRecords = ArrayList<MedicationRecord>()
    private var lastReportText: String = ""
    private var morningTime = Pair(8, 0)
    private var noonTime = Pair(12, 0)
    private var eveningTime = Pair(19, 0)
    private var scanResultList = ArrayList<ScanResult>()

    // 实时数据
    private var currentHeartRate: Int = 0
    private var currentSteps: Int = 0
    private var currentBloodOxygen: Double = 0.0
    private var currentSleepDuration: Double = 0.0

    // OPPO 健康 SDK
    private var isOppoSdkAvailable = false
    private var oppoQueryCount = 0
    private var oppoHeartRate = 0
    private var oppoRestingHR = 0
    private var oppoSteps = 0
    private var oppoSleep = 0.0
    private var oppoBloodOxygen = 0.0
    private var isTestMode = false
    private var titleTapCount = 0
    private var lastTitleTapTime = 0L
    private var lastShakeTime = 0L
    private var shakeSensorManager: android.hardware.SensorManager? = null

    // TTS 变量
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private val gson = Gson()
    private val decimalFormat = DecimalFormat("#.1")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 用药小贴士列表
    private val healthTips = listOf(
        "服药时请用温开水送服，避免用茶水或牛奶",
        "抗生素需按疗程服用，切勿擅自停药",
        "感冒药与酒精不可同时服用",
        "饭前服用的药物应在餐前30分钟服用",
        "饭后服用的药物应在餐后15-30分钟服用",
        "药品应存放在阴凉干燥处，避免阳光直射",
        "不同药物之间服用需间隔至少30分钟",
        "老年人用药剂量通常需要适当减少",
        "服用铁剂时避免同时饮用茶水或咖啡",
        "安眠药应在睡前15-30分钟服用",
        "维生素C不宜与海鲜同时服用",
        "降压药漏服后不要双倍补服",
        "糖尿病患者应随身携带糖果以防低血糖",
        "服药期间应避免食用柚子，可能影响药效",
        "眼药水开封后一般只能使用一个月"
    )

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                imageUri?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageView.setImageBitmap(bitmap)
                    runOCR(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        try {
            HeytapHealthApi.init(this)
            isOppoSdkAvailable = true
            Log.i("HealthSDK", "OPPO 健康 SDK 初始化成功")
        } catch (e: Exception) {
            isOppoSdkAvailable = false
            Log.w("HealthSDK", "OPPO 健康 SDK 不可用: ${e.message}")
        }

        initViews()
        loadMedicines()
        loadMedicationRecords()
        loadMedicationTimes()
        updateMedicineListUI()
        updateMedicationRecordsUI()
        updateComplianceStats()
        updateAiAnalysis()
        updateTimeDisplay()
        loadHistoryData()

        if (historyList.isEmpty()) {
            Toast.makeText(this, "暂无历史数据，请同步健康数据", Toast.LENGTH_SHORT).show()
        }

        setupListeners()

        if (intent?.getBooleanExtra("FALL_DETECTED", false) == true) {
            Handler(Looper.getMainLooper()).postDelayed({ showFallAlert() }, 500)
        }

        shakeSensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        shakeSensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)?.let {
            shakeSensorManager?.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("FALL_DETECTED", false) == true) {
            showFallAlert()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "TTS: 不支持中文", Toast.LENGTH_SHORT).show()
            } else {
                isTtsReady = true
                Handler(Looper.getMainLooper()).postDelayed({
                    speakWelcome()
                }, 500)
            }
        }
    }

    private fun speakWelcome() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 6 -> "夜深了"
            hour < 9 -> "早上好"
            hour < 12 -> "上午好"
            hour < 14 -> "中午好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }
        speak("$greeting，欢迎使用药爱健康")
    }

    private fun initViews() {
        pageHome = findViewById(R.id.page_home)
        pageHealth = findViewById(R.id.page_health)
        pageUser = findViewById(R.id.page_user)

        imageView = findViewById(R.id.iv_preview)
        tvResult = findViewById(R.id.tv_result)

        tvDeviceStatusLabel = findViewById(R.id.tv_device_status_label)
        tvDeviceName = findViewById(R.id.tv_device_name)
        tvDeviceInfo = findViewById(R.id.tv_device_info)
        deviceStatusDot = findViewById(R.id.device_status_dot)

        cardMedDetails = findViewById(R.id.card_med_details)
        tvScanListDisplay = findViewById(R.id.tv_scan_list_display)
        btnManageScanList = findViewById(R.id.btn_manage_scan_list)
        btnClearScanList = findViewById(R.id.btn_clear_scan_list)

        btnSearchMedicine = findViewById(R.id.btn_search_medicine)
        btnAskAi = findViewById(R.id.btn_ask_ai)
        tvHealthTip = findViewById(R.id.tv_health_tip)

        tvHeartRate = findViewById(R.id.tv_heart_rate)
        tvHeartStatus = findViewById(R.id.tv_heart_status)
        tvSteps = findViewById(R.id.tv_steps)
        progressSteps = findViewById(R.id.progress_steps)

        tvSleepVal = findViewById(R.id.tv_sleep_val)
        tvSleepStatus = findViewById(R.id.tv_sleep_status)

        tvBloodOxygen = findViewById(R.id.tv_blood_oxygen)
        tvOxygenStatus = findViewById(R.id.tv_oxygen_status)

        tvReportContent = findViewById(R.id.tv_report_content)
        tvMedicineListDisplay = findViewById(R.id.tv_medicine_list_display)
        btnClearList = findViewById(R.id.btn_clear_list)
        btnCreateReport = findViewById(R.id.btn_create_report)
        rbMonth = findViewById(R.id.rb_month_report)
        btnTakeMedicine = findViewById(R.id.btn_take_medicine)
        tvMedicationRecords = findViewById(R.id.tv_medication_records)
        btnExportPdf = findViewById(R.id.btn_export_pdf)
        tvComplianceRate = findViewById(R.id.tv_compliance_rate)
        tvTakenCount = findViewById(R.id.tv_taken_count)
        tvStreakDays = findViewById(R.id.tv_streak_days)
        tvTimeMorning = findViewById(R.id.tv_time_morning)
        tvTimeNoon = findViewById(R.id.tv_time_noon)
        tvTimeEvening = findViewById(R.id.tv_time_evening)
        btnManageMedicine = findViewById(R.id.btn_manage_medicine)
        btnManageRecords = findViewById(R.id.btn_manage_records)
        tvAiAnalysis = findViewById(R.id.tv_ai_analysis)
        btnRefreshAnalysis = findViewById(R.id.btn_refresh_analysis)
        btnRefreshCompliance = findViewById(R.id.btn_refresh_compliance)
        switchElderMode = findViewById(R.id.switch_elder_mode)
        switchFallDetection = findViewById(R.id.switch_fall_detection)
        tvFallDetectionStatus = findViewById(R.id.tv_fall_detection_status)
        tvEmergencyContact = findViewById(R.id.tv_emergency_contact)
        loadElderMode()
        loadEmergencyContact()
        loadFallDetectionState()
        showRandomHealthTip()
    }

    private fun setupListeners() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchPage(pageHome)
                R.id.nav_health -> switchPage(pageHealth)
                R.id.nav_user -> {
                    updateMedicineListUI()
                    switchPage(pageUser)
                }
            }
            true
        }

        findViewById<View>(R.id.tv_home_title).setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTitleTapTime > 2000) titleTapCount = 0
            lastTitleTapTime = now
            titleTapCount++
            when {
                titleTapCount == 3 -> Toast.makeText(this, "再点${5 - titleTapCount}次开启测试模式", Toast.LENGTH_SHORT).show()
                titleTapCount >= 5 -> {
                    titleTapCount = 0
                    isTestMode = !isTestMode
                    if (isTestMode) {
                        Toast.makeText(this, "🧪 测试模式已开启", Toast.LENGTH_SHORT).show()
                    } else {
                        val todayStr = dateFormat.format(Date())
                        historyList.removeAll { it.date != todayStr }
                        saveHistoryToDisk()
                        Toast.makeText(this, "测试模式已关闭，模拟数据已清除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        findViewById<View>(R.id.card_scan).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
            }
        }

        findViewById<View>(R.id.btn_refresh_health).setOnClickListener {
            val pd = ProgressDialog(this)
            pd.setTitle("正在连接智能设备")
            pd.setMessage("🔍 正在连接 OPPO 健康服务...")
            pd.setCancelable(false)
            pd.show()

            if (!isOppoSdkAvailable) {
                runOnUiThread {
                    pd.dismiss()
                    showNoSdkUI()
                }
                return@setOnClickListener
            }

            try {
                HeytapHealthApi.getInstance().authorityApi().valid(object : HResponse<List<String>> {
                    override fun onSuccess(scopeList: List<String>) {
                        Log.i("HealthSDK", "已授权，范围: $scopeList")
                        if (scopeList.isEmpty()) {
                            Log.w("HealthSDK", "权限列表为空，需要重新授权")
                            runOnUiThread {
                                pd.dismiss()
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle("⚠️ 权限不足")
                                    .setMessage(
                                        "已连接 OPPO 健康，但未授予数据读取权限。\n\n" +
                                        "请点击「去授权」，在 OPPO 健康中勾选以下权限：\n" +
                                        "✅ 心率数据\n" +
                                        "✅ 日常活动数据\n" +
                                        "✅ 睡眠数据\n\n" +
                                        "授权完成后返回，再次点击同步。"
                                    )
                                    .setPositiveButton("🔑 去授权") { _, _ ->
                                        requestOppoAuth()
                                    }
                                    .setNegativeButton("取消", null)
                                    .setCancelable(false)
                                    .show()
                            }
                        } else {
                            runOnUiThread { pd.setMessage("🔗 正在查询设备连接...") }
                            checkDeviceAndFetch(pd)
                        }
                    }
                    override fun onFailure(errorCode: Int) {
                        Log.w("HealthSDK", "未授权, errorCode=$errorCode")
                        runOnUiThread {
                            pd.dismiss()
                            showAuthRetryDialog()
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("HealthSDK", "SDK调用异常: ${e.message}")
                runOnUiThread {
                    pd.dismiss()
                    showNoSdkUI()
                }
            }
        }

        findViewById<View>(R.id.btn_view_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnClearList.setOnClickListener {
            myMedicineList.clear()
            saveMedicines()
            updateMedicineListUI()
            lastOcrContent = ""
            speak("药箱已清空")
            Toast.makeText(this, "药箱已清空", Toast.LENGTH_SHORT).show()
        }

        btnClearScanList.setOnClickListener {
            scanResultList.clear()
            updateScanListUI()
            Toast.makeText(this, "识别记录已清空", Toast.LENGTH_SHORT).show()
        }

        btnManageScanList.setOnClickListener { showManageScanListDialog() }

        btnCreateReport.setOnClickListener {
            val days = if (rbMonth.isChecked) 30 else 7
            speak("正在为您生成健康报告")
            generateReport(days)
        }

        btnTakeMedicine.setOnClickListener { showTakeMedicineDialog() }

        btnExportPdf.setOnClickListener { exportReportToPdf() }

        btnSearchMedicine.setOnClickListener { showSearchMedicineDialog() }
        btnAskAi.setOnClickListener { showAskAiDialog() }

        tvTimeMorning.setOnClickListener { showTimePickerDialog(0) }
        tvTimeNoon.setOnClickListener { showTimePickerDialog(1) }
        tvTimeEvening.setOnClickListener { showTimePickerDialog(2) }

        btnManageMedicine.setOnClickListener { showManageMedicineDialog() }
        btnManageRecords.setOnClickListener { showManageRecordsDialog() }

        btnRefreshAnalysis.setOnClickListener {
            updateAiAnalysis()
            Toast.makeText(this, "分析已刷新", Toast.LENGTH_SHORT).show()
        }

        btnRefreshCompliance.setOnClickListener {
            updateComplianceStats()
            Toast.makeText(this, "统计已刷新", Toast.LENGTH_SHORT).show()
        }

        switchElderMode.setOnCheckedChangeListener { _, isChecked ->
            isElderMode = isChecked
            saveElderMode()
            applyElderMode()
            val msg = if (isChecked) "关怀模式已开启" else "关怀模式已关闭"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (isChecked) speak(msg)
        }

        findViewById<View>(R.id.btn_sos).setOnClickListener { triggerSos() }
        findViewById<View>(R.id.btn_set_emergency_contact).setOnClickListener { showSetEmergencyContactDialog() }

        switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
            saveFallDetectionState(isChecked)
            if (isChecked) {
                val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
                if (sp.getString("emergency_phone", null).isNullOrEmpty()) {
                    switchFallDetection.isChecked = false
                    showSetEmergencyContactDialog()
                    Toast.makeText(this, "请先设置紧急联系人", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                requestPermissionsForFall()
            } else {
                stopService(Intent(this, FallDetectionService::class.java))
                tvFallDetectionStatus.text = "已关闭"
            }
        }
    }

    private fun requestPermissionsForFall() {
        val perms = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.SEND_SMS)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        if (perms.isNotEmpty()) {
            requestPermissions(perms.toTypedArray(), 2001)
        } else {
            startFallDetectionService()
        }
    }

    private fun startFallDetectionService() {
        try {
            val intent = Intent(this, FallDetectionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            tvFallDetectionStatus.text = "监测中..."
            tvFallDetectionStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } catch (e: Exception) {
            Log.e("FallDetection", "启动摔倒检测服务失败: ${e.message}")
            switchFallDetection.isChecked = false
            saveFallDetectionState(false)
            tvFallDetectionStatus.text = "启动失败，请重试"
            Toast.makeText(this, "摔倒检测服务启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSetEmergencyContactDialog() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }
        val etName = android.widget.EditText(this).apply {
            hint = "联系人姓名"
            setText(sp.getString("emergency_name", ""))
        }
        val etPhone = android.widget.EditText(this).apply {
            hint = "手机号码"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(sp.getString("emergency_phone", ""))
        }
        layout.addView(etName)
        layout.addView(etPhone)
        AlertDialog.Builder(this)
            .setTitle("👨‍👩‍👧 设置紧急联系人")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    sp.edit().putString("emergency_name", name).putString("emergency_phone", phone).apply()
                    tvEmergencyContact.text = "$name $phone"
                    Toast.makeText(this, "✅ 紧急联系人已保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadEmergencyContact() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        val name = sp.getString("emergency_name", null)
        val phone = sp.getString("emergency_phone", null)
        tvEmergencyContact.text = if (!name.isNullOrEmpty() && !phone.isNullOrEmpty()) "$name $phone" else "未设置"
    }

    private fun saveFallDetectionState(enabled: Boolean) {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit().putBoolean("fall_detection", enabled).apply()
    }

    private fun loadFallDetectionState() {
        try {
            val enabled = getSharedPreferences("HealthApp", Context.MODE_PRIVATE).getBoolean("fall_detection", false)
            switchFallDetection.isChecked = enabled
            if (enabled) {
                val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
                if (!sp.getString("emergency_phone", null).isNullOrEmpty()) {
                    startFallDetectionService()
                }
            }
        } catch (e: Exception) {
            Log.e("FallDetection", "加载摔倒检测状态失败: ${e.message}")
            saveFallDetectionState(false)
        }
    }

    private fun checkDeviceAndFetch(pd: ProgressDialog) {
        try {
            HeytapHealthApi.getInstance().deviceApi().deviceInfoApi()
                .queryBoundDevice(object : HResponse<List<UserDeviceInfoProxy>> {
                    override fun onSuccess(devices: List<UserDeviceInfoProxy>) {
                        val connected = devices.find {
                            it.connectionState == 102 || it.connectionState == 104 || it.connectionState == 105
                        }
                        runOnUiThread {
                            if (connected != null) {
                                tvDeviceStatusLabel.text = "已连接设备"
                                tvDeviceName.text = connected.deviceName ?: connected.model ?: "OPPO 穿戴设备"
                                tvDeviceInfo.text = "已连接 · 正在同步数据"
                                tvDeviceInfo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_green))
                                pd.setMessage("📥 正在从 ${connected.deviceName} 同步数据...")
                            } else if (devices.isNotEmpty()) {
                                val dev = devices[0]
                                tvDeviceStatusLabel.text = "设备未连接"
                                tvDeviceName.text = dev.deviceName ?: dev.model ?: "OPPO 穿戴设备"
                                tvDeviceInfo.text = "设备离线 · 使用手机数据"
                                tvDeviceInfo.setTextColor(Color.parseColor("#FF9800"))
                                pd.setMessage("📥 设备未连接，获取手机数据...")
                            } else {
                                tvDeviceStatusLabel.text = "未绑定设备"
                                tvDeviceName.text = "使用手机数据"
                                tvDeviceInfo.text = "在 OPPO 健康中绑定手表可获取更多数据"
                                tvDeviceInfo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                                pd.setMessage("📥 正在获取手机健康数据...")
                            }
                        }
                        fetchOppoHealthData(pd)
                    }
                    override fun onFailure(errorCode: Int) {
                        Log.w("HealthSDK", "设备查询失败: $errorCode")
                        runOnUiThread {
                            tvDeviceStatusLabel.text = "设备状态未知"
                            tvDeviceName.text = "使用手机数据"
                            tvDeviceInfo.text = "设备查询失败，尝试获取手机数据"
                            tvDeviceInfo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                            pd.setMessage("📥 正在获取健康数据...")
                        }
                        fetchOppoHealthData(pd)
                    }
                })
        } catch (e: Exception) {
            Log.e("HealthSDK", "设备查询异常: ${e.message}")
            runOnUiThread {
                tvDeviceName.text = "使用手机数据"
                tvDeviceInfo.text = "获取手机健康数据中..."
            }
            fetchOppoHealthData(pd)
        }
    }

    private fun requestOppoAuth() {
        try {
            HeytapHealthApi.getInstance().authorityApi().request(this, object : HResponse<AuthResult> {
                override fun onSuccess(authResult: AuthResult) {
                    Log.i("HealthSDK", "授权成功!")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "✅ 授权成功！请点击同步按钮获取数据", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(errorCode: Int) {
                    Log.e("HealthSDK", "授权失败, errorCode=$errorCode")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "授权未完成(错误码:$errorCode)，返回后可重试", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("HealthSDK", "授权异常: ${e.message}")
            Toast.makeText(this, "无法打开 OPPO 健康: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAuthRetryDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔐 健康数据授权")
            .setMessage(
                "需要您在 OPPO 健康 App 中授权才能获取真实数据。\n\n" +
                "操作步骤：\n" +
                "1. 点击「去授权」跳转到 OPPO 健康\n" +
                "2. 在 OPPO 健康中点击「同意」完成授权\n" +
                "3. 返回本应用，再次点击同步按钮"
            )
            .setPositiveButton("🔑 去授权") { _, _ ->
                requestOppoAuth()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    private fun showNoSdkUI() {
        tvDeviceStatusLabel.text = "未连接"
        tvDeviceName.text = "OPPO 健康不可用"
        tvDeviceInfo.text = "请安装「OPPO 健康」App"
        tvDeviceInfo.setTextColor(Color.RED)
        tvHeartRate.text = "--"
        tvHeartStatus.text = "未连接"
        tvSteps.text = "--"
        progressSteps.progress = 0
        tvBloodOxygen.text = "--"
        tvOxygenStatus.text = "未连接"
        tvSleepVal.text = "--"
        tvSleepStatus.text = "未连接"
        Toast.makeText(this, "⚠️ OPPO 健康服务不可用，请安装「OPPO 健康」App", Toast.LENGTH_LONG).show()
    }

    private fun fetchOppoHealthData(pd: ProgressDialog) {
        oppoQueryCount = 4
        oppoHeartRate = 0
        oppoRestingHR = 0
        oppoSteps = 0
        oppoSleep = 0.0
        oppoBloodOxygen = 0.0

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        // 1. 查询今日心率统计
        try {
            val hrRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_COUNT)
                .setTimeRange(startOfDay, now)
                .build()
            HeytapHealthApi.getInstance().dataApi().read(hrRequest, object : HResponse<List<DataSet>> {
                override fun onSuccess(dataSets: List<DataSet>) {
                    try {
                        for (dataSet in dataSets) {
                            for (dp in dataSet.getDataPoints()) {
                                for (el in dp.dataType.elements) {
                                    val v = dp.getValue(el)
                                    if (v != null && v.isSet) {
                                        when (el.name) {
                                            Element.ELEMENT_NAME_AVERAGE -> oppoHeartRate = v.asFloat().toInt()
                                            Element.ELEMENT_NAME_REST_HR -> oppoRestingHR = v.asInt()
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HealthSDK", "心率解析异常: ${e.message}")
                    }
                    onOppoQueryDone(pd)
                }
                override fun onFailure(errorCode: Int) {
                    Log.w("HealthSDK", "心率查询失败, code=$errorCode")
                    onOppoQueryDone(pd)
                }
            })
        } catch (e: Exception) {
            Log.e("HealthSDK", "心率查询异常: ${e.message}")
            onOppoQueryDone(pd)
        }

        // 2. 查询今日步数（每日活动统计）
        try {
            val stepsRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_DAILY_ACTIVITY_COUNT)
                .setTimeRange(startOfDay, now)
                .build()
            HeytapHealthApi.getInstance().dataApi().read(stepsRequest, object : HResponse<List<DataSet>> {
                override fun onSuccess(dataSets: List<DataSet>) {
                    try {
                        for (dataSet in dataSets) {
                            for (dp in dataSet.getDataPoints()) {
                                for (el in dp.dataType.elements) {
                                    val v = dp.getValue(el)
                                    if (v != null && v.isSet && el.name == Element.ELEMENT_NAME_STEP) {
                                        oppoSteps = v.asInt()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HealthSDK", "步数解析异常: ${e.message}")
                    }
                    onOppoQueryDone(pd)
                }
                override fun onFailure(errorCode: Int) {
                    Log.w("HealthSDK", "步数查询失败, code=$errorCode")
                    onOppoQueryDone(pd)
                }
            })
        } catch (e: Exception) {
            Log.e("HealthSDK", "步数查询异常: ${e.message}")
            onOppoQueryDone(pd)
        }

        // 3. 查询今日睡眠统计
        try {
            val sleepRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_SLEEP_COUNT)
                .setTimeRange(startOfDay - 12 * 60 * 60 * 1000L, now)
                .build()
            HeytapHealthApi.getInstance().dataApi().read(sleepRequest, object : HResponse<List<DataSet>> {
                override fun onSuccess(dataSets: List<DataSet>) {
                    try {
                        for (dataSet in dataSets) {
                            for (dp in dataSet.getDataPoints()) {
                                for (el in dp.dataType.elements) {
                                    val v = dp.getValue(el)
                                    if (v != null && v.isSet && el.name == Element.ELEMENT_NAME_TOTAL) {
                                        oppoSleep = v.asFloat().toDouble() / 60.0
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HealthSDK", "睡眠解析异常: ${e.message}")
                    }
                    onOppoQueryDone(pd)
                }
                override fun onFailure(errorCode: Int) {
                    Log.w("HealthSDK", "睡眠查询失败, code=$errorCode")
                    onOppoQueryDone(pd)
                }
            })
        } catch (e: Exception) {
            Log.e("HealthSDK", "睡眠查询异常: ${e.message}")
            onOppoQueryDone(pd)
        }

        // 4. 查询血氧
        try {
            val oxygenRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_BLOOD_OXYGEN_COUNT)
                .setTimeRange(startOfDay, now)
                .build()
            HeytapHealthApi.getInstance().dataApi().read(oxygenRequest, object : HResponse<List<DataSet>> {
                override fun onSuccess(dataSets: List<DataSet>) {
                    try {
                        for (dataSet in dataSets) {
                            for (dp in dataSet.getDataPoints()) {
                                for (el in dp.dataType.elements) {
                                    val v = dp.getValue(el)
                                    if (v != null && v.isSet && el.name == Element.ELEMENT_NAME_AVERAGE) {
                                        oppoBloodOxygen = v.asFloat().toDouble()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HealthSDK", "血氧解析异常: ${e.message}")
                    }
                    onOppoQueryDone(pd)
                }
                override fun onFailure(errorCode: Int) {
                    Log.w("HealthSDK", "血氧查询失败, code=$errorCode")
                    onOppoQueryDone(pd)
                }
            })
        } catch (e: Exception) {
            Log.e("HealthSDK", "血氧查询异常: ${e.message}")
            onOppoQueryDone(pd)
        }
    }

    @Synchronized
    private fun onOppoQueryDone(pd: ProgressDialog) {
        oppoQueryCount--
        if (oppoQueryCount > 0) return

        runOnUiThread {
            pd.dismiss()

            if (isTestMode) {
                if (oppoHeartRate == 0) oppoHeartRate = (62..85).random()
                if (oppoRestingHR == 0) oppoRestingHR = (55..68).random()
                if (oppoSleep <= 0) oppoSleep = (55..85).random() / 10.0
            }
            val testBloodOxygen = if (isTestMode && oppoBloodOxygen == 0.0) (95..99).random().toDouble() else oppoBloodOxygen

            Log.i("HealthSDK", "数据(测试=$isTestMode): 心率=$oppoHeartRate, 静息=$oppoRestingHR, 步数=$oppoSteps, 睡眠=${oppoSleep}h, 血氧=$testBloodOxygen")

            val hasData = oppoHeartRate > 0 || oppoSteps > 0 || oppoSleep > 0

            // 心率
            currentHeartRate = oppoHeartRate
            tvHeartRate.text = if (oppoHeartRate > 0) oppoHeartRate.toString() else "--"
            tvHeartStatus.text = when {
                oppoHeartRate == 0 -> "等待数据"
                oppoHeartRate > 90 -> "偏快 ⚠️"
                else -> "正常 ✅"
            }

            // 步数
            currentSteps = oppoSteps
            tvSteps.text = oppoSteps.toString()
            progressSteps.progress = oppoSteps

            // 血氧
            currentBloodOxygen = testBloodOxygen
            if (testBloodOxygen > 0) {
                tvBloodOxygen.text = "${testBloodOxygen.toInt()}%"
                tvOxygenStatus.text = when {
                    testBloodOxygen < 90 -> "偏低 ⚠️"
                    testBloodOxygen < 95 -> "注意 ⚠️"
                    else -> "正常 ✅"
                }
                tvOxygenStatus.setTextColor(when {
                    testBloodOxygen < 90 -> Color.parseColor("#F44336")
                    testBloodOxygen < 95 -> Color.parseColor("#FF9800")
                    else -> ContextCompat.getColor(this, R.color.accent_green)
                })
            } else {
                tvBloodOxygen.text = "--"
                tvOxygenStatus.text = "等待数据"
                tvOxygenStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }

            // 睡眠
            currentSleepDuration = oppoSleep
            tvSleepVal.text = if (oppoSleep > 0) decimalFormat.format(oppoSleep) else "--"
            tvSleepStatus.text = when {
                oppoSleep <= 0 -> "等待数据"
                oppoSleep < 6.0 -> "不足 ⚠️"
                else -> "充足 ✅"
            }
            tvSleepStatus.setTextColor(when {
                oppoSleep <= 0 -> ContextCompat.getColor(this, R.color.text_secondary)
                oppoSleep < 6.0 -> Color.parseColor("#FF9800")
                else -> ContextCompat.getColor(this, R.color.accent_purple)
            })

            val restingHR = if (oppoRestingHR > 0) oppoRestingHR else currentHeartRate
            if (hasData) {
                saveTodayData(restingHR, currentSteps, currentBloodOxygen, currentSleepDuration)
            }

            if (isTestMode) {
                generateTestHistoryData()
                Toast.makeText(this, "🧪 测试模式：已生成30天模拟历史数据", Toast.LENGTH_SHORT).show()
            } else if (hasData) {
                Toast.makeText(this, "✅ 已从 OPPO 健康同步真实数据", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ 暂无数据", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun runOCR(bitmap: Bitmap) {
        tvResult.text = "⏳ 正在识别药名、频率与剂量..."
        tvResult.visibility = View.VISIBLE
        cardMedDetails.visibility = View.GONE

        val image = InputImage.fromBitmap(bitmap, 0)
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            .process(image).addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isNotEmpty()) {
                    lastOcrContent = text
                    val prompt = """
                        请提取图片中的药品名称和服用频率以及每次服用剂量。
                        图片文字：$text
                        
                        【输出严格格式】
                        药名#每日次数#每次剂量
                        
                        【示例】
                        阿莫西林胶囊#2#一次2片
                        
                        【注意】
                        1. 如果找不到频率和剂量，请根据药名自动推断最合理的频率和剂量。
                        2. 只输出这一行，不要废话。
                    """.trimIndent()

                    AIService.analyzeText(prompt, object : AIService.AICallback {
                        override fun onSuccess(aiResult: String) {
                            try {
                                val parts = aiResult.trim().split("#")
                                val medName = parts[0]
                                val frequency = parts.getOrElse(1) { "1" }.toIntOrNull() ?: 1
                                val dosage = parts.getOrElse(2) { "适量" }

                                val exists = myMedicineList.any { it.name == medName }
                                if (!exists) {
                                    showAddMedicineDialog(medName, frequency, dosage)
                                }

                                tvResult.text = "✅ 识别完成"
                                addToScanList(medName, frequency, dosage)
                            } catch (e: Exception) {
                                tvResult.text = "识别格式错误：$aiResult"
                            }
                        }
                        override fun onError(error: String) { tvResult.text = "AI 分析失败：$error" }
                    })
                } else { tvResult.text = "❌ 未识别到文字" }
            }
    }

    private fun addToScanList(name: String, freq: Int, dose: String) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val scanTime = timeFormat.format(Date())
        val result = ScanResult(name, freq, dose, scanTime)
        scanResultList.add(result)
        updateScanListUI()
    }

    private fun updateScanListUI() {
        if (scanResultList.isEmpty()) {
            cardMedDetails.visibility = View.GONE
        } else {
            cardMedDetails.visibility = View.VISIBLE
            val sb = StringBuilder()
            for ((index, item) in scanResultList.withIndex()) {
                sb.append("${index + 1}. ${item.name}\n")
                sb.append("    每日${item.frequency}次，${item.dosage}  ⏰${item.scanTime}\n")
            }
            tvScanListDisplay.text = sb.toString().trimEnd()
        }
    }

    private fun showManageScanListDialog() {
        if (scanResultList.isEmpty()) {
            Toast.makeText(this, "暂无识别记录", Toast.LENGTH_SHORT).show()
            return
        }

        val items = scanResultList.map { "${it.name} (每日${it.frequency}次)" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("📝 管理识别记录")
            .setItems(items) { _, which ->
                val result = scanResultList[which]
                showScanItemOptionsDialog(result, which)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showScanItemOptionsDialog(result: ScanResult, index: Int) {
        val options = arrayOf("💊 添加到药箱并设闹钟", "🗑️ 删除此记录")

        AlertDialog.Builder(this)
            .setTitle("${result.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val exists = myMedicineList.any { it.name == result.name }
                        if (!exists) {
                            showAddMedicineDialog(result.name, result.frequency, result.dosage)
                        } else {
                            Toast.makeText(this, "该药品已在药箱中", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        scanResultList.removeAt(index)
                        updateScanListUI()
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddMedicineDialog(medName: String, frequency: Int, dosage: String) {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val tvInfo = TextView(this).apply {
            text = "药品：$medName\n用法：每日${frequency}次，$dosage"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
        }
        layout.addView(tvInfo)

        val tvLabel = TextView(this).apply {
            text = "\n请设置有效期（可选）："
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
        }
        layout.addView(tvLabel)

        val etExpiry = android.widget.EditText(this).apply {
            hint = "格式：2026-11"
            inputType = android.text.InputType.TYPE_CLASS_DATETIME
        }
        layout.addView(etExpiry)

        val ttsText = "已识别 $medName，建议每日 $frequency 次，${dosage}"
        speak(ttsText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("💊 添加药品到药箱")
            .setView(layout)
            .setPositiveButton("添加并设闹钟") { _, _ ->
                val expiryDate = etExpiry.text.toString().trim()
                val todayStr = dateFormat.format(Date())
                val newMed = MedicineItem(medName, expiryDate, 1, todayStr, frequency)
                myMedicineList.add(newMed)
                saveMedicines()
                updateMedicineListUI()
                setSmartAlarms(medName, frequency)
                checkSingleMedicineExpiry(newMed)
            }
            .setNegativeButton("仅添加") { _, _ ->
                val expiryDate = etExpiry.text.toString().trim()
                val todayStr = dateFormat.format(Date())
                val newMed = MedicineItem(medName, expiryDate, 1, todayStr, frequency)
                myMedicineList.add(newMed)
                saveMedicines()
                updateMedicineListUI()
                Toast.makeText(this, "已添加到药箱", Toast.LENGTH_SHORT).show()
                checkSingleMedicineExpiry(newMed)
            }
            .setNeutralButton("🔊 重播", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { speak(ttsText) }
    }

    private fun checkSingleMedicineExpiry(med: MedicineItem) {
        if (med.expiryDate.isEmpty()) return

        val today = Date()
        val expiry = parseExpiryDate(med.expiryDate) ?: return
        val diffDays = ((expiry.time - today.time) / (1000 * 60 * 60 * 24)).toInt()

        when {
            diffDays < 0 -> {
                speak("注意，${med.name}已经过期了")
                AlertDialog.Builder(this)
                    .setTitle("⛔ 药品已过期")
                    .setMessage("您添加的「${med.name}」已过期！\n\n有效期：${med.expiryDate}\n\n建议立即处理，请勿服用过期药品。")
                    .setPositiveButton("知道了", null)
                    .setNegativeButton("从药箱移除") { _, _ ->
                        myMedicineList.remove(med)
                        saveMedicines()
                        updateMedicineListUI()
                        Toast.makeText(this, "已移除", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
            diffDays <= 30 -> {
                speak("提醒您，${med.name}将在${diffDays}天后过期")
                AlertDialog.Builder(this)
                    .setTitle("⚠️ 药品即将过期")
                    .setMessage("您添加的「${med.name}」即将过期\n\n有效期：${med.expiryDate}\n剩余：${diffDays}天\n\n请注意在有效期内使用。")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }
    }

    private fun checkExpiringMedicines() {
        if (myMedicineList.isEmpty()) return

        val today = Date()
        val expiringList = mutableListOf<String>()
        val expiredList = mutableListOf<String>()

        for (med in myMedicineList) {
            if (med.expiryDate.isEmpty()) continue
            val expiry = parseExpiryDate(med.expiryDate) ?: continue
            val diffDays = ((expiry.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
            when {
                diffDays < 0 -> expiredList.add(med.name)
                diffDays <= 30 -> expiringList.add("${med.name}(${med.expiryDate})")
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (expiredList.isNotEmpty()) {
                speak("注意，您有${expiredList.size}种药品已过期")
                AlertDialog.Builder(this)
                    .setTitle("⛔ 药品过期提醒")
                    .setMessage("以下药品已过期，请及时处理：\n\n${expiredList.joinToString("\n")}")
                    .setPositiveButton("知道了", null)
                    .show()
            } else if (expiringList.isNotEmpty()) {
                speak("提醒您，有${expiringList.size}种药品即将过期")
                AlertDialog.Builder(this)
                    .setTitle("⚠️ 药品即将过期")
                    .setMessage("以下药品将在1个月内过期：\n\n${expiringList.joinToString("\n")}")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }, 2000)
    }

    private fun showAlarmConfirmationDialog(medName: String, frequency: Int, dosage: String) {
        val ttsText = "已识别 $medName，建议每日 $frequency 次，${dosage}"
        speak(ttsText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("🤖 智能用药助手")
            .setMessage("已为您解析处方详情：\n\n" +
                    "💊 药名：$medName\n" +
                    "🔄 频率：每日 $frequency 次\n" +
                    "📏 剂量：$dosage\n\n" +
                    "是否为您自动设置系统闹钟？")
            .setPositiveButton("✅ 立即设置") { _, _ ->
                setSmartAlarms(medName, frequency)
            }
            .setNegativeButton("❌ 暂不需要") { dialog, _ ->
                Toast.makeText(this, "已存入药箱，暂未设置闹钟", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNeutralButton("🔊 重播语音", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { speak(ttsText) }
    }

    private fun speak(text: String) {
        if (isTtsReady && isElderMode) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun setSmartAlarms(medName: String, frequency: Int) {
        var alarmInfo = ""
        val mTime = String.format("%02d:%02d", morningTime.first, morningTime.second)
        val nTime = String.format("%02d:%02d", noonTime.first, noonTime.second)
        val eTime = String.format("%02d:%02d", eveningTime.first, eveningTime.second)

        when (frequency) {
            1 -> {
                createSystemAlarm(morningTime.first, morningTime.second, "服用: $medName")
                alarmInfo = "每天$mTime"
            }
            2 -> {
                createSystemAlarm(morningTime.first, morningTime.second, "服用: $medName")
                createSystemAlarm(eveningTime.first, eveningTime.second, "服用: $medName")
                alarmInfo = "早$mTime 晚$eTime"
            }
            3 -> {
                createSystemAlarm(morningTime.first, morningTime.second, "服用: $medName")
                createSystemAlarm(noonTime.first, noonTime.second, "服用: $medName")
                createSystemAlarm(eveningTime.first, eveningTime.second, "服用: $medName")
                alarmInfo = "早中晚"
            }
            else -> {
                createSystemAlarm(morningTime.first, morningTime.second, "服用: $medName")
                alarmInfo = "每天$mTime"
            }
        }
        speak("已为您设置 $alarmInfo 的吃药提醒")
        Toast.makeText(this, "已添加 $alarmInfo 的闹钟", Toast.LENGTH_LONG).show()
    }

    private fun generateReport(days: Int) {
        if (historyList.isEmpty()) {
            tvReportContent.text = "⚠️ 暂无健康数据，请先去「健康」页同步数据。"
            return
        }

        btnCreateReport.isEnabled = false
        btnCreateReport.alpha = 0.6f

        val historyBuilder = StringBuilder()
        val recentHistory = historyList.takeLast(days)
        for (data in recentHistory) {
            historyBuilder.append("- ${data.date}: 静息心率${data.heartRate}, 睡眠${data.sleepDuration}h, 血氧${data.bloodOxygen}%, 步数${data.steps}\n")
        }

        val listBuilder = StringBuilder()
        if (myMedicineList.isEmpty()) listBuilder.append("无存量药品") else {
            for ((index, med) in myMedicineList.withIndex()) listBuilder.append("${index + 1}. ${med.name}\n")
        }

        val reportType = if (days == 7) "周" else "月"
        tvReportContent.text = "🤖 AI 正在分析近 $days 天的数据，为您生成【$reportType】健康报告..."

        val prompt = """
            你是一位经验丰富的全科医生。请根据用户的【药箱库存】和【近 $days 天健康体征】进行综合推理分析。
            
            【📦 药箱库存】
            $listBuilder
            
            【📅 近 $days 天体征数据】
            $historyBuilder
            
            【分析要求】
            请生成一份《个人健康$reportType 报》，内容必须包含以下三点：
            
            1. 📊 **趋势分析**：心率/血氧/睡眠是否有波动？结合药箱评估药效。
            2. ⚠️ **风险预警**：(重要) 扫描数据，如果发现睡眠<6h、心率>100 或血氧<95%，请务必使用 "⚠️" 图标开头进行强调。
            3. 🌟 **生活处方**：给出下阶段的饮食运动建议，请使用 "🌟" 图标开头。
            
            (注意：
             1. 请直接使用 ⚠️、🌟、💊 等 Emoji 图标来代替普通的 * 号，让排版更漂亮。
             2. 重点结论请使用 **加粗**。
             3. 不要使用 #### 标题格式，直接换行加粗。)
        """.trimIndent()

        AIService.analyzeText(prompt, object : AIService.AICallback {
            override fun onSuccess(aiResult: String) {
                val formattedText = formatAIResponse(aiResult)
                tvReportContent.text = formattedText
                tvReportContent.textSize = 16f
                btnCreateReport.isEnabled = true
                btnCreateReport.alpha = 1.0f
                lastReportText = aiResult
                btnExportPdf.visibility = View.VISIBLE
                speak("健康报告已生成，您可以导出为PDF文件")
            }
            override fun onError(error: String) {
                tvReportContent.text = "报告生成失败：$error"
                btnCreateReport.isEnabled = true
                btnCreateReport.alpha = 1.0f
                speak("报告生成失败，请稍后再试")
            }
        })
    }

    private fun formatAIResponse(rawText: String): Spanned {
        var htmlText = rawText
        htmlText = htmlText.replace(Regex("(?m)^#{1,6}\\s+(.*)$"), "<br><b>$1</b><br>")
        htmlText = htmlText.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        htmlText = htmlText.replace(Regex("(?m)^\\s*[-*]\\s+"), "✨ ")
        htmlText = htmlText.replace("\n", "<br>")
        return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun saveTodayData(hr: Int, steps: Int, oxygen: Double, sleep: Double) {
        val todayStr = dateFormat.format(Date())
        val existingData = historyList.find { it.date == todayStr }
        if (existingData != null) {
            existingData.heartRate = hr
            existingData.steps = steps
            existingData.bloodOxygen = oxygen
            existingData.sleepDuration = sleep
        } else {
            val newData = DailyHealthData(todayStr, hr, steps, oxygen, sleep)
            historyList.add(newData)
        }
        saveHistoryToDisk()
    }

    private fun generateTestHistoryData() {
        val todayStr = dateFormat.format(Date())
        historyList.removeAll { it.date != todayStr }
        val calendar = Calendar.getInstance()
        for (i in 30 downTo 1) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(calendar.time)
            if (historyList.any { it.date == dateStr }) continue
            historyList.add(DailyHealthData(
                date = dateStr,
                heartRate = (58..78).random(),
                steps = (3000..12000).random(),
                bloodOxygen = (94..99).random().toDouble(),
                sleepDuration = (55..85).random() / 10.0
            ))
        }
        historyList.sortBy { it.date }
        saveHistoryToDisk()
    }

    private fun saveHistoryToDisk() {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit()
            .putString("history_data", gson.toJson(historyList)).apply()
    }

    private fun loadHistoryData() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        val json = sp.getString("history_data", null)
        if (json != null) {
            val type = object : TypeToken<ArrayList<DailyHealthData>>() {}.type
            historyList = gson.fromJson(json, type)
        }
    }

    private fun createSystemAlarm(hour: Int, minute: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }

    private fun openCamera() {
        val imageFile = File(externalCacheDir, "my_photo.jpg")
        imageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        try { takePictureLauncher.launch(intent) } catch (e: Exception) {}
    }

    private fun saveMedicines() {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit()
            .putString("medicine_list_v2", gson.toJson(myMedicineList)).apply()
    }

    private fun loadMedicines() {
        val json = getSharedPreferences("HealthApp", Context.MODE_PRIVATE).getString("medicine_list_v2", null)
        if (json != null) {
            myMedicineList = gson.fromJson(json, object : TypeToken<ArrayList<MedicineItem>>() {}.type)
        }
    }

    private fun updateMedicineListUI() {
        if (myMedicineList.isEmpty()) {
            tvMedicineListDisplay.text = "药箱是空的"
            btnTakeMedicine.visibility = View.GONE
        } else {
            val sb = StringBuilder()
            val today = Date()
            for ((i, med) in myMedicineList.withIndex()) {
                val status = getExpiryStatus(med.expiryDate, today)
                sb.append("${i + 1}. ${med.name}")
                if (med.expiryDate.isNotEmpty()) {
                    sb.append("  $status")
                }
                sb.append("\n")
            }
            tvMedicineListDisplay.text = sb.toString().trimEnd()
            btnTakeMedicine.visibility = View.VISIBLE
        }
    }

    private fun getExpiryStatus(expiryDate: String, today: Date): String {
        if (expiryDate.isEmpty()) return ""
        return try {
            val expiry = parseExpiryDate(expiryDate) ?: return ""
            val diffDays = ((expiry.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
            when {
                diffDays < 0 -> "⛔ 已过期"
                diffDays <= 30 -> "⚠️ 即将过期"
                diffDays <= 90 -> "📅 $expiryDate"
                else -> "✅ $expiryDate"
            }
        } catch (e: Exception) { "" }
    }

    private fun parseExpiryDate(dateStr: String): Date? {
        return try {
            if (dateStr.matches(Regex("\\d{4}-\\d{2}"))) {
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.time = monthFormat.parse(dateStr) ?: return null
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.time
            } else {
                dateFormat.parse(dateStr)
            }
        } catch (e: Exception) { null }
    }

    private fun updateComplianceStats() {
        val totalRecords = medicationRecords.size
        tvTakenCount.text = totalRecords.toString()

        if (myMedicineList.isEmpty()) {
            tvComplianceRate.text = "--"
            tvComplianceRate.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tvStreakDays.text = "0"
            return
        }

        if (medicationRecords.isEmpty()) {
            tvComplianceRate.text = "0%"
            tvComplianceRate.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
            tvStreakDays.text = "0"
            return
        }

        val uniqueDates = medicationRecords.map { it.date }.distinct().sorted()
        val activeDays = uniqueDates.size.coerceIn(1, 7)
        val recentDates = uniqueDates.takeLast(activeDays)
        val recentRecords = medicationRecords.filter { it.date in recentDates }

        var totalExpected = 0
        var totalCompleted = 0

        for (date in recentDates) {
            val dayRecords = recentRecords.filter { it.date == date }
            for (med in myMedicineList) {
                val expectedForMed = med.frequency.coerceAtLeast(1)
                val actualForMed = dayRecords.count { it.medicineName == med.name }
                val completedForMed = minOf(actualForMed, expectedForMed)
                totalExpected += expectedForMed
                totalCompleted += completedForMed
            }
        }

        val rate = if (totalExpected > 0) (totalCompleted * 100 / totalExpected) else 0
        tvComplianceRate.text = "$rate%"

        when {
            rate >= 80 -> tvComplianceRate.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            rate >= 50 -> tvComplianceRate.setTextColor(ContextCompat.getColor(this, R.color.accent_orange))
            else -> tvComplianceRate.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
        }

        var streak = 0
        val checkCal = Calendar.getInstance()
        while (streak < 365) {
            val checkDate = dateFormat.format(checkCal.time)
            if (medicationRecords.any { it.date == checkDate }) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        tvStreakDays.text = streak.toString()
    }

    private fun updateAiAnalysis() {
        if (medicationRecords.isEmpty() && myMedicineList.isEmpty()) {
            tvAiAnalysis.text = "📍 正在学习您的用药习惯...\n💡 开始记录服药后，将为您生成个性化分析"
            return
        }

        tvAiAnalysis.text = "🔄 正在分析您的用药数据..."

        val medicineInfo = if (myMedicineList.isNotEmpty()) {
            myMedicineList.mapIndexed { i, m -> "${i+1}. ${m.name}" }.joinToString("\n")
        } else { "暂无药品" }

        val recordInfo = if (medicationRecords.isNotEmpty()) {
            val recent = medicationRecords.takeLast(10)
            recent.map { "${it.date} ${it.takeTime} 服用${it.medicineName}" }.joinToString("\n")
        } else { "暂无服药记录" }

        val healthInfo = buildString {
            if (currentHeartRate > 0) append("心率：${currentHeartRate}bpm\n")
            if (currentBloodOxygen > 0) append("血氧：${currentBloodOxygen.toInt()}%\n")
            if (currentSleepDuration > 0) append("睡眠：${currentSleepDuration}h\n")
            if (currentSteps > 0) append("步数：${currentSteps}步")
        }.ifEmpty { "暂无健康数据" }

        val prompt = """
            你是一个用药习惯分析引擎。请根据以下数据进行简短分析。
            
            【药箱药品】
            $medicineInfo
            
            【近期服药记录】
            $recordInfo
            
            【健康指标】
            $healthInfo
            
            【输出要求】
            1. 用3-5条简短结论概括用药规律
            2. 每条以emoji开头（如📊⏰💊⚠️✅💤）
            3. 如果有异常（心率>90、睡眠<6h、漏服等），用⚠️提醒
            4. 给出1条个性化建议
            5. 不要废话，直接输出结论
        """.trimIndent()

        AIService.analyzeText(prompt, object : AIService.AICallback {
            override fun onSuccess(result: String) {
                val cleanResult = result.trim()
                    .replace(Regex("^[#*]+\\s*"), "")
                    .replace(Regex("\\*\\*"), "")
                tvAiAnalysis.text = cleanResult
            }
            override fun onError(error: String) {
                tvAiAnalysis.text = "⚠️ 分析暂时不可用\n💡 请检查网络后点击刷新"
            }
        })
    }

    private fun showTimePickerDialog(timeSlot: Int) {
        val currentTime = when (timeSlot) {
            0 -> morningTime
            1 -> noonTime
            else -> eveningTime
        }

        val picker = android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
            when (timeSlot) {
                0 -> morningTime = Pair(hourOfDay, minute)
                1 -> noonTime = Pair(hourOfDay, minute)
                2 -> eveningTime = Pair(hourOfDay, minute)
            }
            saveMedicationTimes()
            updateTimeDisplay()
            Toast.makeText(this, "时间已更新", Toast.LENGTH_SHORT).show()
        }, currentTime.first, currentTime.second, true)

        picker.setTitle(when (timeSlot) {
            0 -> "设置早间服药时间"
            1 -> "设置午间服药时间"
            else -> "设置晚间服药时间"
        })
        picker.show()
    }

    private fun updateTimeDisplay() {
        tvTimeMorning.text = String.format("%02d:%02d", morningTime.first, morningTime.second)
        tvTimeNoon.text = String.format("%02d:%02d", noonTime.first, noonTime.second)
        tvTimeEvening.text = String.format("%02d:%02d", eveningTime.first, eveningTime.second)
    }

    private fun saveMedicationTimes() {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit()
            .putInt("time_morning_h", morningTime.first)
            .putInt("time_morning_m", morningTime.second)
            .putInt("time_noon_h", noonTime.first)
            .putInt("time_noon_m", noonTime.second)
            .putInt("time_evening_h", eveningTime.first)
            .putInt("time_evening_m", eveningTime.second)
            .apply()
    }

    private fun loadMedicationTimes() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        morningTime = Pair(sp.getInt("time_morning_h", 8), sp.getInt("time_morning_m", 0))
        noonTime = Pair(sp.getInt("time_noon_h", 12), sp.getInt("time_noon_m", 0))
        eveningTime = Pair(sp.getInt("time_evening_h", 19), sp.getInt("time_evening_m", 0))
    }

    private fun saveElderMode() {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit()
            .putBoolean("elder_mode", isElderMode)
            .apply()
    }

    private fun loadElderMode() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        isElderMode = sp.getBoolean("elder_mode", false)
        switchElderMode.isChecked = isElderMode
        applyElderMode()
    }

    private fun applyElderMode() {
        val scale = if (isElderMode) 1.35f else 1.0f
        applyFontScale(pageHome, scale)
        applyFontScale(pageHealth, scale)
        applyFontScale(pageUser, scale)
    }

    private fun applyFontScale(view: View, scale: Float) {
        if (view is TextView) {
            val tag = view.getTag(R.id.tag_original_size)
            val originalSize = if (tag != null) {
                tag as Float
            } else {
                val size = view.textSize / resources.displayMetrics.scaledDensity
                view.setTag(R.id.tag_original_size, size)
                size
            }
            view.textSize = originalSize * scale
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontScale(view.getChildAt(i), scale)
            }
        }
    }

    private fun switchPage(pageToShow: View) {
        pageHome.visibility = if (pageToShow == pageHome) View.VISIBLE else View.GONE
        pageHealth.visibility = if (pageToShow == pageHealth) View.VISIBLE else View.GONE
        pageUser.visibility = if (pageToShow == pageUser) View.VISIBLE else View.GONE

        if (pageToShow == pageHome) {
            showRandomHealthTip()
            speak("首页")
        } else if (pageToShow == pageHealth) {
            speak("健康数据")
            showHealthDisclaimer()
        } else if (pageToShow == pageUser) {
            updateMedicationRecordsUI()
            updateComplianceStats()
            updateAiAnalysis()
            speak("个人中心")
        }
    }

    private fun showHealthDisclaimer() {
        val message = if (isOppoSdkAvailable) {
            "已接入 OPPO 健康服务 SDK，数据来自 OPPO 健康 App。\n\n" +
            "连接 OPPO 智能手表可获取心率、睡眠、血氧等更多健康数据。\n\n" +
            "首次使用需要授权访问健康数据。"
        } else {
            "当前设备未检测到 OPPO 健康服务，健康数据由模拟生成。\n\n" +
            "如需真实数据，请确保已安装「OPPO 健康」App 并连接智能穿戴设备。"
        }
        AlertDialog.Builder(this)
            .setTitle("📢 数据来源说明")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showTakeMedicineDialog() {
        if (myMedicineList.isEmpty()) {
            Toast.makeText(this, "药箱是空的", Toast.LENGTH_SHORT).show()
            return
        }

        val names = myMedicineList.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(names.size) { false }

        AlertDialog.Builder(this)
            .setTitle("💊 选择已服用的药品")
            .setMultiChoiceItems(names, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("确认服药") { _, _ ->
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                val todayStr = dateFormat.format(Date())
                var count = 0

                for (i in names.indices) {
                    if (checkedItems[i]) {
                        val record = MedicationRecord(names[i], currentTime, todayStr)
                        medicationRecords.add(record)
                        count++
                    }
                }

                if (count > 0) {
                    saveMedicationRecords()
                    updateMedicationRecordsUI()
                    updateComplianceStats()
                    speak("已记录 $count 种药品的服药情况")
                    Toast.makeText(this, "✅ 已记录 $count 种药品", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showManageMedicineDialog() {
        if (myMedicineList.isEmpty()) {
            Toast.makeText(this, "药箱是空的", Toast.LENGTH_SHORT).show()
            return
        }

        val names = myMedicineList.map {
            val freq = it.frequency.coerceAtLeast(1)
            val expiry = if (it.expiryDate.isNotEmpty()) " | ${it.expiryDate}" else ""
            "${it.name} (每日${freq}次$expiry)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("📦 管理药箱")
            .setItems(names) { _, which ->
                showMedicineOptionsDialog(which)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showMedicineOptionsDialog(index: Int) {
        val med = myMedicineList[index]
        val options = arrayOf("修改每日服用次数", "删除此药品")

        AlertDialog.Builder(this)
            .setTitle(med.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditFrequencyDialog(index)
                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("确认删除")
                            .setMessage("确定要从药箱中删除「${med.name}」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                myMedicineList.removeAt(index)
                                saveMedicines()
                                updateMedicineListUI()
                                updateComplianceStats()
                                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditFrequencyDialog(index: Int) {
        val med = myMedicineList[index]
        val frequencies = arrayOf("每日1次", "每日2次", "每日3次", "每日4次")
        val currentFreq = med.frequency.coerceIn(1, 4) - 1

        AlertDialog.Builder(this)
            .setTitle("设置「${med.name}」的服用频率")
            .setSingleChoiceItems(frequencies, currentFreq) { dialog, which ->
                med.frequency = which + 1
                saveMedicines()
                updateMedicineListUI()
                updateComplianceStats()
                Toast.makeText(this, "已设置为每日${which + 1}次", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showManageRecordsDialog() {
        val todayStr = dateFormat.format(Date())
        val todayRecords = medicationRecords.filter { it.date == todayStr }

        if (todayRecords.isEmpty()) {
            Toast.makeText(this, "今日暂无服药记录", Toast.LENGTH_SHORT).show()
            return
        }

        val items = todayRecords.map { "${it.medicineName} (${it.takeTime})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("📋 管理服药记录")
            .setItems(items) { _, which ->
                val record = todayRecords[which]
                showRecordOptionsDialog(record)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRecordOptionsDialog(record: MedicationRecord) {
        val options = arrayOf("✏️ 修改时间", "🗑️ 删除记录")

        AlertDialog.Builder(this)
            .setTitle("${record.medicineName} (${record.takeTime})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditRecordTimeDialog(record)
                    1 -> showDeleteRecordConfirm(record)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditRecordTimeDialog(record: MedicationRecord) {
        val timeParts = record.takeTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val picker = android.app.TimePickerDialog(this, { _, newHour, newMinute ->
            val newTime = String.format("%02d:%02d", newHour, newMinute)
            val index = medicationRecords.indexOf(record)
            if (index >= 0) {
                val updatedRecord = MedicationRecord(record.medicineName, newTime, record.date)
                medicationRecords[index] = updatedRecord
                saveMedicationRecords()
                updateMedicationRecordsUI()
                Toast.makeText(this, "时间已修改为 $newTime", Toast.LENGTH_SHORT).show()
            }
        }, hour, minute, true)

        picker.setTitle("修改「${record.medicineName}」的服药时间")
        picker.show()
    }

    private fun showDeleteRecordConfirm(record: MedicationRecord) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除「${record.medicineName}」${record.takeTime}的服药记录吗？")
            .setPositiveButton("删除") { _, _ ->
                medicationRecords.remove(record)
                saveMedicationRecords()
                updateMedicationRecordsUI()
                updateComplianceStats()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateMedicationRecordsUI() {
        val todayStr = dateFormat.format(Date())
        val todayRecords = medicationRecords.filter { it.date == todayStr }

        if (todayRecords.isEmpty()) {
            tvMedicationRecords.text = "今日暂无服药记录"
        } else {
            val sb = StringBuilder()
            for ((index, record) in todayRecords.withIndex()) {
                sb.append("${index + 1}. ${record.medicineName}  ⏰ ${record.takeTime}\n")
            }
            tvMedicationRecords.text = sb.toString().trimEnd()
        }
    }

    private fun saveMedicationRecords() {
        getSharedPreferences("HealthApp", Context.MODE_PRIVATE).edit()
            .putString("medication_records", gson.toJson(medicationRecords)).apply()
    }

    private fun loadMedicationRecords() {
        val json = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
            .getString("medication_records", null)
        if (json != null) {
            val type = object : TypeToken<ArrayList<MedicationRecord>>() {}.type
            medicationRecords = gson.fromJson(json, type)
        }
    }

    private fun exportReportToPdf() {
        if (lastReportText.isEmpty()) {
            Toast.makeText(this, "请先生成报告", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#1A1A1A")
            }

            val contentPaint = Paint().apply {
                textSize = 12f
                color = Color.parseColor("#333333")
            }

            val datePaint = Paint().apply {
                textSize = 11f
                color = Color.parseColor("#666666")
            }

            canvas.drawText("药爱健康 - 个人健康报告", 40f, 60f, titlePaint)

            val dateStr = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("生成时间：$dateStr", 40f, 85f, datePaint)

            val cleanText = lastReportText
                .replace(Regex("\\*\\*"), "")
                .replace(Regex("#{1,6}\\s*"), "")
                .replace("⚠️", "[!]")
                .replace("🌟", "[*]")
                .replace("💊", "[药]")
                .replace("📊", "[图]")
                .replace("✨", "-")

            val lines = mutableListOf<String>()
            val maxWidth = 515f

            for (paragraph in cleanText.split("\n")) {
                if (paragraph.isEmpty()) {
                    lines.add("")
                    continue
                }

                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    val count = contentPaint.breakText(remaining, true, maxWidth, null)
                    lines.add(remaining.substring(0, count))
                    remaining = remaining.substring(count)
                }
            }

            var y = 120f
            val lineHeight = 18f

            for (line in lines) {
                if (y > 800f) break
                canvas.drawText(line, 40f, y, contentPaint)
                y += lineHeight
            }

            val footerPaint = Paint().apply {
                textSize = 10f
                color = Color.parseColor("#999999")
            }
            canvas.drawText("本报告由药爱健康App生成，仅供参考，如有不适请及时就医。", 40f, 820f, footerPaint)

            pdfDocument.finishPage(page)

            val fileName = "健康报告_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(externalCacheDir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            speak("报告已导出")

            val uri = FProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "分享健康报告"))

        } catch (e: Exception) {
            Toast.makeText(this, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRandomHealthTip() {
        val randomTip = healthTips.random()
        tvHealthTip.text = randomTip
    }

    private fun showSearchMedicineDialog() {
        val inputField = android.widget.EditText(this).apply {
            hint = "请输入药品名称，如：阿莫西林"
            setPadding(50, 40, 50, 40)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle("🔍 搜索药品百科")
            .setView(inputField)
            .setPositiveButton("查询") { _, _ ->
                val medicineName = inputField.text.toString().trim()
                if (medicineName.isNotEmpty()) {
                    searchMedicineInfo(medicineName)
                } else {
                    Toast.makeText(this, "请输入药品名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun searchMedicineInfo(medicineName: String) {
        val pd = ProgressDialog(this)
        pd.setTitle("正在查询")
        pd.setMessage("🔍 正在搜索「$medicineName」的相关信息...")
        pd.setCancelable(false)
        pd.show()

        val prompt = """
            请详细介绍药品【$medicineName】的相关信息，包括：
            
            1. 💊 **药品简介**：该药的主要成分和类型
            2. 🎯 **适应症**：该药主要治疗什么疾病/症状
            3. 📋 **用法用量**：推荐的服用方法和剂量
            4. ⚠️ **注意事项**：服药禁忌、不良反应、特殊人群注意
            5. 🚫 **药物相互作用**：不能与哪些药物/食物同服
            
            请使用 emoji 图标让排版更清晰美观，重点内容请加粗。
            如果找不到该药品，请说明并给出建议。
        """.trimIndent()

        AIService.analyzeText(prompt, object : AIService.AICallback {
            override fun onSuccess(aiResult: String) {
                pd.dismiss()
                showMedicineInfoDialog(medicineName, aiResult)
            }
            override fun onError(error: String) {
                pd.dismiss()
                Toast.makeText(this@MainActivity, "查询失败：$error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showMedicineInfoDialog(medicineName: String, info: String) {
        val formattedText = formatAIResponse(info)

        val scrollView = android.widget.ScrollView(this)
        val textView = TextView(this).apply {
            text = formattedText
            textSize = 15f
            setPadding(50, 30, 50, 30)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("💊 $medicineName")
            .setView(scrollView)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showAskAiDialog() {
        val inputField = android.widget.EditText(this).apply {
            hint = "请输入您的健康问题..."
            setPadding(50, 40, 50, 40)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = android.view.Gravity.TOP
        }

        AlertDialog.Builder(this)
            .setTitle("💬 问AI健康助手")
            .setMessage("我是您的智能健康顾问，有任何健康相关的问题都可以问我哦～")
            .setView(inputField)
            .setPositiveButton("提问") { _, _ ->
                val question = inputField.text.toString().trim()
                if (question.isNotEmpty()) {
                    askAiHealthQuestion(question)
                } else {
                    Toast.makeText(this, "请输入您的问题", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun askAiHealthQuestion(question: String) {
        val pd = ProgressDialog(this)
        pd.setTitle("AI思考中")
        pd.setMessage("🤔 正在为您分析问题...")
        pd.setCancelable(false)
        pd.show()

        val prompt = """
            你是一位专业、友善的健康顾问。用户问了以下问题：
            
            「$question」
            
            请给出专业、准确、易懂的回答。注意：
            1. 使用通俗易懂的语言
            2. 如果涉及严重疾病，请建议用户及时就医
            3. 使用 emoji 让回答更亲切
            4. 重点内容请加粗
            5. 分点回答，条理清晰
            
            ⚠️ 最后请附上免责声明：AI建议仅供参考，如有不适请及时就医。
        """.trimIndent()

        AIService.analyzeText(prompt, object : AIService.AICallback {
            override fun onSuccess(aiResult: String) {
                pd.dismiss()
                showAiAnswerDialog(question, aiResult)
            }
            override fun onError(error: String) {
                pd.dismiss()
                Toast.makeText(this@MainActivity, "AI回答失败：$error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAiAnswerDialog(question: String, answer: String) {
        val formattedText = formatAIResponse(answer)

        val scrollView = android.widget.ScrollView(this)
        val textView = TextView(this).apply {
            text = formattedText
            textSize = 15f
            setPadding(50, 30, 50, 30)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("🤖 AI健康助手")
            .setView(scrollView)
            .setPositiveButton("明白了", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera()
        if (requestCode == 2001) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startFallDetectionService()
            } else {
                switchFallDetection.isChecked = false
                Toast.makeText(this, "需要短信和定位权限才能使用摔倒检测", Toast.LENGTH_LONG).show()
            }
        }
        if (requestCode == 2002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pendingSosPhone?.let { doSendSms(it) }
            pendingSosPhone = null
        }
    }

    private fun triggerSos() {
        val sp = getSharedPreferences("HealthApp", Context.MODE_PRIVATE)
        val phone = sp.getString("emergency_phone", null)
        if (phone.isNullOrEmpty()) {
            showSetEmergencyContactDialog()
            Toast.makeText(this, "请先设置紧急联系人", Toast.LENGTH_SHORT).show()
            return
        }
        val name = sp.getString("emergency_name", "紧急联系人")
        AlertDialog.Builder(this)
            .setTitle("🆘 紧急求助")
            .setMessage("将向 $name($phone) 发送求助短信并附带您的当前位置。\n\n确定发送吗？")
            .setPositiveButton("立即发送") { _, _ ->
                val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION)
                val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
                if (needed.isNotEmpty()) {
                    ActivityCompat.requestPermissions(this, needed.toTypedArray(), 2001)
                    return@setPositiveButton
                }
                sendSosMessage(phone)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private var pendingSosPhone: String? = null

    private fun sendSosMessage(phone: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSosPhone = phone
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 2002)
            Toast.makeText(this, "需要短信权限才能发送求助信息", Toast.LENGTH_SHORT).show()
            return
        }
        doSendSms(phone)
    }

    private fun doSendSms(phone: String) {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            @Suppress("MissingPermission")
            val loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val locText = if (loc != null) "当前位置：https://uri.amap.com/marker?position=${loc.longitude},${loc.latitude}" else "（无法获取位置）"
            val msg = "【药爱健康】用户发出紧急求助，请尽快联系！$locText"
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(msg)
            val sentIntent = android.app.PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"), android.app.PendingIntent.FLAG_IMMUTABLE)
            val sentIntents = ArrayList<android.app.PendingIntent>().apply { for (i in parts.indices) add(sentIntent) }
            smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
            Toast.makeText(this, "🆘 求助短信已发送", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "短信发送失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSmsSilent(phone: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSosPhone = phone
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 2002)
            return
        }
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            @Suppress("MissingPermission")
            val loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val locText = if (loc != null) "当前位置：https://uri.amap.com/marker?position=${loc.longitude},${loc.latitude}" else "（无法获取位置）"
            val msg = "【药爱健康】检测到用户可能发生摔倒，请尽快联系确认安全！$locText"
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        } catch (e: Exception) {
            Toast.makeText(this, "自动短信发送失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private var isFallAlertShowing = false

    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
        if (!isTestMode || event?.sensor?.type != android.hardware.Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
        val now = System.currentTimeMillis()
        if (magnitude > 18 && now - lastShakeTime > 3000 && !isFallAlertShowing) {
            lastShakeTime = now
            runOnUiThread { showFallAlert() }
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}

    private fun showFallAlert() {
        isFallAlertShowing = true
        var seconds = 30
        val handler = Handler(Looper.getMainLooper())
        val dialog = AlertDialog.Builder(this)
            .setTitle("⚠️ 检测到摔倒！")
            .setMessage("如果您安全，请点击「我没事」。\n\n${seconds} 秒后将自动发送求助短信给紧急联系人。")
            .setPositiveButton("我没事") { d, _ ->
                handler.removeCallbacksAndMessages(null)
                d.dismiss()
                isFallAlertShowing = false
                Toast.makeText(this, "已取消警报", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .create()
        dialog.show()

        val countdown = object : Runnable {
            override fun run() {
                seconds--
                if (seconds <= 0) {
                    dialog.dismiss()
                    isFallAlertShowing = false
                    val phone = getSharedPreferences("HealthApp", Context.MODE_PRIVATE).getString("emergency_phone", null)
                    if (!phone.isNullOrEmpty()) {
                        sendSmsSilent(phone)
                        Toast.makeText(this@MainActivity, "🆘 已自动发送求助短信", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "未设置紧急联系人", Toast.LENGTH_LONG).show()
                    }
                } else {
                    dialog.setMessage("如果您安全，请点击「我没事」。\n\n${seconds} 秒后将自动发送求助短信给紧急联系人。")
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(countdown, 1000)
    }

    override fun onDestroy() {
        shakeSensorManager?.unregisterListener(this)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
