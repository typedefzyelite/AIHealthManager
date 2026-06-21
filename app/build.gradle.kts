import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.aihealthmanager_2"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.aihealthmanager_2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "DEEPSEEK_API_KEY",
            "\"${localProperties.getProperty("DEEPSEEK_API_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // 核心安卓库
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 测试库 (保留着，防止报错)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // =============== 下面是你要加的 ML Kit ===============
    // 谷歌 ML Kit 文字识别 (通用版)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    // 谷歌 ML Kit 文字识别 (中文增强包)
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    // 网络请求库 OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // JSON 解析库 Gson (用来处理 AI 返回的数据)
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // OPPO 健康服务 SDK
    implementation("com.heytap.health:sdk:2.1.7")
}