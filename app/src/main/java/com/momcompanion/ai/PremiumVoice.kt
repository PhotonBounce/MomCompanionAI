package com.friendai

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * Premium cloud voice (ElevenLabs) for Friendai.
 *
 * Two modes:
 *  - [playRaw]  plays a bundled premium clip (e.g. the welcome greeting) — always available,
 *               zero runtime cost, so the very first thing Mom hears is a warm premium voice.
 *  - [speak]    synthesizes arbitrary reply text on demand via ElevenLabs and plays it, used
 *               only when the caregiver has opted into premium live voice (it consumes cloud
 *               credits). Any failure or a missing key calls [fallback] so the phone's built-in
 *               voice speaks instead — Mom is never left in silence.
 *
 * The API key is baked in at build time from gemini.properties (BuildConfig.DEFAULT_ELEVENLABS_KEY)
 * so it works out of the box but is never visible in the UI or the repo.
 */
object PremiumVoice {
    /** Warm, reassuring female voice — the same one used in the concept video. */
    const val VOICE_DEFAULT = "EXAVITQu4vr4xnSDxMaL" // Sarah

    // Flash v2.5: ~0.5 credit/char and low latency, so live replies stay affordable and snappy.
    // It is multilingual, so it handles both English and Russian from the same voice.
    private const val MODEL = "eleven_flash_v2_5"

    private var player: MediaPlayer? = null

    fun key(): String = BuildConfig.DEFAULT_ELEVENLABS_KEY
    fun available(): Boolean = key().isNotBlank()

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    /** Play a bundled raw audio resource; [onDone] fires when it finishes or on any error. */
    fun playRaw(context: Context, resId: Int, onDone: (() -> Unit)?) {
        stop()
        runCatching {
            val mp = MediaPlayer.create(context, resId)
            if (mp == null) { onDone?.invoke(); return }
            player = mp.apply {
                setOnCompletionListener { onDone?.invoke() }
                setOnErrorListener { _, _, _ -> onDone?.invoke(); true }
                start()
            }
        }.onFailure { onDone?.invoke() }
    }

    /**
     * Synthesize [text] with ElevenLabs off the UI thread, then play it and call [onDone].
     * On any failure (no key, network, API error) [fallback] is invoked on the main thread so
     * the caller can speak with the phone's TTS instead.
     */
    fun speak(
        context: Context,
        text: String,
        voice: String,
        onDone: (() -> Unit)?,
        fallback: () -> Unit
    ) {
        val apiKey = key()
        if (apiKey.isBlank() || text.isBlank()) { fallback(); return }
        Thread {
            val file = runCatching { synth(context, text, voice, apiKey) }.getOrNull()
            val main = android.os.Handler(context.mainLooper)
            if (file == null) {
                main.post { fallback() }
            } else {
                main.post {
                    stop()
                    runCatching {
                        player = MediaPlayer().apply {
                            setDataSource(file.absolutePath)
                            setOnCompletionListener { onDone?.invoke(); runCatching { file.delete() } }
                            setOnErrorListener { _, _, _ ->
                                onDone?.invoke(); runCatching { file.delete() }; true
                            }
                            prepare()
                            start()
                        }
                    }.onFailure { onDone?.invoke(); runCatching { file.delete() } }
                }
            }
        }.start()
    }

    private fun synth(context: Context, text: String, voice: String, apiKey: String): File {
        val url = java.net.URL(
            "https://api.elevenlabs.io/v1/text-to-speech/$voice?output_format=mp3_44100_128"
        )
        val body = org.json.JSONObject()
            .put("text", text)
            .put("model_id", MODEL)
            .put(
                "voice_settings",
                org.json.JSONObject()
                    .put("stability", 0.5)
                    .put("similarity_boost", 0.8)
                    .put("style", 0.15)
                    .put("use_speaker_boost", true)
            )
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("xi-api-key", apiKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg")
        }
        java.io.OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = runCatching {
                (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            conn.disconnect()
            error("ElevenLabs $code: $err")
        }
        val out = File.createTempFile("pv_", ".mp3", context.cacheDir)
        conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
        conn.disconnect()
        return out
    }
}
