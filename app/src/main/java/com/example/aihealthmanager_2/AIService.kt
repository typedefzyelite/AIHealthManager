package com.example.aihealthmanager_2

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object AIService {

    private const val USE_MOCK_DATA = false
    private const val API_URL = "https://api.deepseek.com/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getKey(): String = BuildConfig.DEEPSEEK_API_KEY

    interface AICallback {
        fun onSuccess(result: String)
        fun onError(error: String)
    }

    fun analyzeText(prompt: String, callback: AICallback) {
        sendToAI(prompt, "你是一个家庭医生，请根据数据生成详细报告。", callback)
    }

    fun extractMedicineName(ocrText: String, callback: AICallback) {
        val prompt = """
            请从下面的文字中提取【一个】最准确的药品通用名称（例如：阿莫西林胶囊、布洛芬缓释胶囊）。
            
            【文字内容】
            $ocrText
            
            【绝对要求】
            1. 只输出药名，不要输出任何其他标点符号或废话。
            2. 如果文字里包含"OTC"、"说明书"、"国药准字"，请忽略，只找药名。
            3. 如果找不到药名，输出"未知药品"。
        """.trimIndent()

        sendToAI(prompt, "你是一个数据提取助手，只输出药名，不要说废话。", callback)
    }

    private fun sendToAI(userContent: String, systemContent: String, callback: AICallback) {
        if (USE_MOCK_DATA) {
            mainHandler.postDelayed({ callback.onSuccess("模拟数据") }, 1000)
            return
        }

        val systemMessage = mapOf("role" to "system", "content" to systemContent)
        val userMessage = mapOf("role" to "user", "content" to userContent)

        val payload = mapOf(
            "model" to "deepseek-chat",
            "messages" to listOf(systemMessage, userMessage),
            "stream" to false
        )

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer ${getKey()}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback.onError("网络错误：${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    try {
                        val jsonObject = gson.fromJson(bodyStr, JsonObject::class.java)
                        val choices = jsonObject.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val content = choices.get(0).asJsonObject
                                .getAsJsonObject("message")
                                .get("content").asString
                            mainHandler.post { callback.onSuccess(content.trim().replace("。", "")) }
                        } else {
                            mainHandler.post { callback.onError("AI 返回空") }
                        }
                    } catch (e: Exception) {
                        mainHandler.post { callback.onError("解析错：${e.message}") }
                    }
                } else {
                    mainHandler.post { callback.onError("API错：${response.code}") }
                }
            }
        })
    }
}
