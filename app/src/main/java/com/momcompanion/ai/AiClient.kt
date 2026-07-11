package com.friendai

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.friendai.CaregiverSettings
import com.friendai.CompanionReply
import com.friendai.CompanionEngine

interface AiClient {
    fun generateReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings,
        context: android.content.Context? = null,
        callback: (CompanionReply) -> Unit
    )
}

class LocalAiClient(
    private val companionEngine: CompanionEngine = CompanionEngine()
) : AiClient {

    override fun generateReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings,
        context: android.content.Context?,
        callback: (CompanionReply) -> Unit
    ) {
        // Pass context (persistent memory) and recentConversation (so the engine can seed
        // lastAIReply even when created fresh each turn, enabling follow-up detection).
        callback(companionEngine.replyTo(message, settings, context, recentConversation))
    }
}

class ProxyAiClient(
    private val endpointUrl: String,
    private val companionEngine: CompanionEngine = CompanionEngine(),
    private val fallbackClient: LocalAiClient = LocalAiClient(companionEngine),
    private val fallbackEnabled: Boolean = true
) : AiClient {

    override fun generateReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings,
        context: android.content.Context?,
        callback: (CompanionReply) -> Unit
    ) {
        val safetyReply = companionEngine.safetyReplyFor(message, settings)
        if (safetyReply != null) {
            callback(safetyReply)
            return
        }

        Thread {
            val result = runCatching {
                requestProxyReply(message, recentConversation, settings)
            }

            if (result.isSuccess) {
                callback(result.getOrThrow())
            } else if (fallbackEnabled) {
                fallbackClient.generateReply(message, recentConversation, settings, context, callback)
            } else {
                val detail = result.exceptionOrNull()?.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unable to reach the AI backend."
                callback(CompanionReply("AI backend test failed: $detail"))
            }
        }.start()
    }

    private fun requestProxyReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings
    ): CompanionReply {
        val prompt = PromptBuilder.build(settings, message, recentConversation)
        val body = JSONObject()
            .put("instructions", prompt.instructions)
            .put("input", prompt.input)

        val connection = (URL(normalizedEndpointUrl()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000  // 30s — some AI backends are slow on a cold start
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (settings.backendToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${settings.backendToken}")
            }
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val responseBody = stream.bufferedReader().use { it.readText() }
        connection.disconnect()

        if (responseCode !in 200..299) {
            error(responseBody)
        }

        val json = JSONObject(responseBody)
        val text = json.optString("reply").trim()
        if (text.isBlank()) {
            error("Proxy returned an empty reply.")
        }

        return CompanionReply(text)
    }

    private fun normalizedEndpointUrl(): String {
        val trimmed = endpointUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/companion/reply")) {
            trimmed
        } else {
            "$trimmed/companion/reply"
        }
    }
}

class GeminiAiClient(
    private val apiKey: String,
    private val companionEngine: CompanionEngine = CompanionEngine(),
    private val fallbackClient: LocalAiClient = LocalAiClient(companionEngine)
) : AiClient {

    override fun generateReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings,
        context: android.content.Context?,
        callback: (CompanionReply) -> Unit
    ) {
        val safetyReply = companionEngine.safetyReplyFor(message, settings)
        if (safetyReply != null) {
            callback(safetyReply)
            return
        }

        Thread {
            val result = runCatching { requestGeminiReply(message, recentConversation, settings) }
            if (result.isSuccess) {
                callback(result.getOrThrow())
            } else {
                fallbackClient.generateReply(message, recentConversation, settings, context, callback)
            }
        }.start()
    }

    private fun requestGeminiReply(
        message: String,
        recentConversation: String,
        settings: CaregiverSettings
    ): CompanionReply {
        val prompt = PromptBuilder.build(settings, message, recentConversation)
        val body = org.json.JSONObject()
            .put(
                "system_instruction",
                org.json.JSONObject().put(
                    "parts",
                    org.json.JSONArray().put(org.json.JSONObject().put("text", prompt.instructions))
                )
            )
            .put(
                "contents",
                org.json.JSONArray().put(
                    org.json.JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            org.json.JSONArray().put(org.json.JSONObject().put("text", prompt.input))
                        )
                )
            )
            .put(
                "generationConfig",
                org.json.JSONObject().put("maxOutputTokens", 220).put("temperature", 0.8)
            )

        val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
        val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        java.io.OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
        val responseBody = stream.bufferedReader().use { it.readText() }
        connection.disconnect()

        if (responseCode !in 200..299) error(responseBody)

        val json = org.json.JSONObject(responseBody)
        val parts = json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text") ?: "")
                }
            }
        }.trim()

        if (text.isBlank()) error("Gemini returned empty reply.")
        return CompanionReply(text)
    }
}

object AiClientFactory {
    fun create(settings: CaregiverSettings): AiClient {
        return when {
            settings.geminiApiKey.isNotBlank() -> GeminiAiClient(settings.geminiApiKey)
            settings.backendUrl.isNotBlank() -> ProxyAiClient(settings.backendUrl)
            else -> LocalAiClient()
        }
    }
}
