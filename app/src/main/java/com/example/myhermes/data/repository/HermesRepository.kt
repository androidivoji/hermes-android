package com.example.myhermes.data.repository

import android.util.Log
import com.example.myhermes.data.model.ChatMessage
import com.example.myhermes.data.model.HermesModel
import com.example.myhermes.data.model.HermesModelDefaults
import com.example.myhermes.data.model.MessageSender
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HermesRepository {

    companion object {
        private const val TAG = "HermesAPI"
        const val BASE_URL = "http://139.190.99.102:20128/v1"
        const val API_KEY = "sk-766ff1277fb12201-milkba-03b80b7b"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetches list of valid model IDs supported by the server at GET /v1/models
     */
    suspend fun fetchAvailableModels(): List<HermesModel> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/models"
        Log.d(TAG, "--> GET $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $API_KEY")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            Log.d(TAG, "<-- GET /v1/models Code: ${response.code}, Body: $bodyString")

            if (response.isSuccessful && bodyString.isNotEmpty()) {
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val dataArray = jsonObject.getAsJsonArray("data")
                if (dataArray != null && dataArray.size() > 0) {
                    val fetchedModels = mutableListOf<HermesModel>()
                    for (element in dataArray) {
                        val modelObj = element.asJsonObject
                        val modelId = modelObj.get("id").asString
                        fetchedModels.add(
                            HermesModel(
                                id = modelId,
                                name = modelId,
                                description = "Model terdaftar pada server ($modelId)",
                                tag = "Online"
                            )
                        )
                    }
                    Log.d(TAG, "Berhasil mendapatkan ${fetchedModels.size} model dari server!")
                    return@withContext fetchedModels
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil daftar model dari /v1/models", e)
        }
        emptyList()
    }

    /**
     * Sends prompt (and optional image Base64 data URL) to Hermes Agent API at /v1/chat/completions
     */
    fun sendPrompt(
        prompt: String,
        selectedModel: HermesModel = HermesModelDefaults.DEFAULT_MODEL,
        imageBase64: String? = null
    ): Flow<ChatMessage> = flow {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val initialMessage = ChatMessage(
            sender = MessageSender.HERMES,
            text = "Menghubungkan ke Hermes Agent (${selectedModel.id})...",
            timestamp = currentTime,
            thoughtProcess = "1. Menghubungkan ke $BASE_URL/chat/completions\n2. Model: \"${selectedModel.id}\"\n3. Vision Multimodal: ${imageBase64 != null}",
            isStreaming = true
        )
        emit(initialMessage)

        // Construct Request Payload for OpenAI Compatible Chat Completion API (Support Vision Multimodal)
        val requestJson = JsonObject().apply {
            addProperty("model", selectedModel.id)

            val contentElement = if (imageBase64 != null) {
                // OpenAI Vision format: Array of text and image_url
                JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", prompt)
                    })
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        addJsonObject("image_url") {
                            addProperty("url", imageBase64)
                        }
                    })
                }
            } else {
                // Standard text
                gson.toJsonTree(prompt)
            }

            val messagesArray = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", contentElement)
                })
            }

            add("messages", messagesArray)
            addProperty("stream", true)
        }

        val url = "$BASE_URL/chat/completions"
        val jsonPayload = requestJson.toString()

        Log.d(TAG, "==================================================")
        Log.d(TAG, "--> POST $url (Multimodal Vision: ${imageBase64 != null})")
        Log.d(TAG, "Header Authorization: Bearer sk-766f...03b80b7b")
        Log.d(TAG, "Model Selected: \"${selectedModel.id}\"")
        Log.d(TAG, "User Prompt: $prompt")
        Log.d(TAG, "Request Body JSON (Truncated): ${jsonPayload.take(300)}...")
        Log.d(TAG, "==================================================")

        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        var accumulatedText = ""
        var accumulatedThought = ""

        try {
            val response = client.newCall(request).execute()

            Log.d(TAG, "<-- HTTP Response Status: ${response.code} ${response.message}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown server error"
                val errorText = "HTTP Error ${response.code}: $errorBody"
                Log.e(TAG, "<-- HTTP ERROR [${response.code}]: $errorBody")

                emit(
                    initialMessage.copy(
                        text = errorText,
                        isStreaming = false
                    )
                )
                return@flow
            }

            val body = response.body
            if (body == null) {
                Log.e(TAG, "<-- Response body is null!")
                emit(initialMessage.copy(text = "Response body is empty", isStreaming = false))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            Log.d(TAG, "--- Mulai Membaca Stream Response (SSE) ---")

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.isEmpty() || currentLine.startsWith(":")) continue

                if (currentLine.startsWith("data:")) {
                    val dataContent = currentLine.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") {
                        Log.d(TAG, "SSE Stream Completed ([DONE])")
                        break
                    }

                    try {
                        val jsonObject = gson.fromJson(dataContent, JsonObject::class.java)
                        val choices = jsonObject.getAsJsonArray("choices")
                        if (choices != null && choices.size() > 0) {
                            val choiceObj = choices.get(0).asJsonObject
                            val delta = choiceObj.getAsJsonObject("delta")
                            val messageObj = choiceObj.getAsJsonObject("message")

                            var hasChunkUpdate = false

                            val reasoningChunk = when {
                                delta != null && delta.has("reasoning") && !delta.get("reasoning").isJsonNull -> delta.get("reasoning").asString
                                delta != null && delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull -> delta.get("reasoning_content").asString
                                delta != null && delta.has("thinking") && !delta.get("thinking").isJsonNull -> delta.get("thinking").asString
                                else -> ""
                            }

                            val contentChunk = when {
                                delta != null && delta.has("content") && !delta.get("content").isJsonNull -> delta.get("content").asString
                                messageObj != null && messageObj.has("content") && !messageObj.get("content").isJsonNull -> messageObj.get("content").asString
                                else -> ""
                            }

                            if (reasoningChunk.isNotEmpty()) {
                                accumulatedThought += reasoningChunk
                                hasChunkUpdate = true
                            }

                            if (contentChunk.isNotEmpty()) {
                                accumulatedText += contentChunk
                                hasChunkUpdate = true
                            }

                            if (hasChunkUpdate) {
                                emit(
                                    initialMessage.copy(
                                        text = if (accumulatedText.isNotEmpty()) accumulatedText else "Hermes Agent sedang menganalisis gambar...",
                                        thoughtProcess = if (accumulatedThought.isNotEmpty()) accumulatedThought else initialMessage.thoughtProcess,
                                        isStreaming = true
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal memproses JSON SSE chunk: ${e.message}")
                    }
                }
            }

            if (accumulatedText.isEmpty() && accumulatedThought.isNotEmpty()) {
                accumulatedText = accumulatedThought
            } else if (accumulatedText.isEmpty()) {
                accumulatedText = "Respon dari Hermes Agent berhasil diterima."
            }

            Log.d(TAG, "==================================================")
            Log.d(TAG, "<-- FULL RESPONSE TEXT SUCCESS:")
            Log.d(TAG, accumulatedText)
            Log.d(TAG, "==================================================")

            emit(
                initialMessage.copy(
                    text = accumulatedText,
                    thoughtProcess = if (accumulatedThought.isNotEmpty()) accumulatedThought else initialMessage.thoughtProcess,
                    isStreaming = false
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "=================== EXCEPTION ===================", e)

            val failureText = """
                ⚠️ Gagal terhubung ke Hermes Agent (139.190.99.102:20128).
                Detail Error: ${e.localizedMessage ?: e.message}
            """.trimIndent()

            emit(
                initialMessage.copy(
                    text = failureText,
                    isStreaming = false
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}

// Extension helper for clean Gson JSON building
private fun JsonObject.addJsonObject(property: String, init: JsonObject.() -> Unit) {
    val obj = JsonObject().apply(init)
    add(property, obj)
}
