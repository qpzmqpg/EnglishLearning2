package com.example.englishleaning2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.englishleaning2.Data.WordEntry
import android.speech.tts.TextToSpeech
import java.util.Locale

class WordDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_detail)

        // 获取传递过来的WordEntry数据
        val wordEntry = intent.getParcelableExtra<WordEntry>("word_entry")
        wordEntry?.let { showWordDetail(it) }
    }

    private fun showWordDetail(entry: WordEntry) {
        val spannable = SpannableStringBuilder().apply {
            // 标题样式
            appendStyled("${entry.word}\n", Color.BLUE, 64.sp, Typeface.BOLD)
            // 音标和发音按钮
            entry.phonetic?.let {
                appendStyled("[${it}]  ", Color.DKGRAY, 48.sp)
                append(createSpeakerButton(entry.audio))
            }
            append("\n\n")
            // 词性解释（带彩色标签）
            entry.pos?.let { pos ->
                appendStyled(" $pos ", Color.WHITE, 36.sp, Typeface.BOLD, Color.parseColor("#4CAF50"))
                append("  ")
            }
            // 英文释义
            appendStyled("English Definition:\n", Color.BLACK, 40.sp, Typeface.BOLD)
            appendStyled(entry.definition ?: "", Color.BLACK, 44.sp)
            append("\n\n")
            // 中文释义
            entry.translation?.let { trans ->
                appendStyled("中文释义:\n", Color.BLACK, 40.sp, Typeface.BOLD)
                appendStyled(trans, Color.BLACK, 44.sp)
                append("\n\n")
            }
            // 柯林斯星级
            if (entry.collins > 0) {
                appendStyled("\u2B50".repeat(entry.collins) + " Collins ", Color.parseColor("#FF9800"), 36.sp)
                append("\n")
            }
            // 牛津核心词标识
            if (entry.oxford == 1) {
                appendStyled("\uD83C\uDFEB Oxford Core ", Color.parseColor("#2196F3"), 36.sp)
                append("\n")
            }
            // 标签信息
            entry.tag?.let { tag ->
                appendStyled("Tags: ", Color.BLACK, 36.sp, Typeface.BOLD)
                appendStyled(tag, Color.parseColor("#9C27B0"), 36.sp)
                append("\n")
            }
            // 词频信息
            entry.bnc?.let { bnc ->
                appendStyled("BNC Frequency Rank: ", Color.BLACK, 36.sp, Typeface.BOLD)
                appendStyled(bnc.toString(), Color.parseColor("#795548"), 36.sp)
                append("\n")
            }
            entry.frq?.let { frq ->
                appendStyled("Contemporary Frequency Rank: ", Color.BLACK, 36.sp, Typeface.BOLD)
                appendStyled(frq.toString(), Color.parseColor("#795548"), 36.sp)
                append("\n")
            }
            // 词形变化
            entry.exchange?.let { exchange ->
                appendStyled("Word Forms:\n", Color.BLACK, 36.sp, Typeface.BOLD)
                exchange.split("/").forEach { form ->
                    val parts = form.split(":")
                    if (parts.size == 2) {
                        val (type, word) = parts
                        val formType = when(type) {
                            "p" -> "Past tense"
                            "d" -> "Past participle"
                            "i" -> "Present participle"
                            "3" -> "Third person singular"
                            "r" -> "Comparative"
                            "t" -> "Superlative"
                            "s" -> "Plural"
                            "0" -> "Original"
                            "1" -> "Variant"
                            else -> type
                        }
                        appendStyled("$formType: ", Color.BLACK, 32.sp)
                        appendStyled(word, Color.parseColor("#607D8B"), 32.sp)
                        append("\n")
                    }
                }
            }
            // 详细解释（带缩进）
            entry.detail?.let { detail ->
                appendStyled("Detailed Usage:\n", Color.BLACK, 40.sp, Typeface.BOLD)
                append(parseDetailHtml(detail))
            }
            // 添加间距
            append("\n\n")
        }
        findViewById<TextView>(R.id.result_textview).apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun SpannableStringBuilder.appendStyled(
        text: String,
        color: Int,
        textSize: TextUnit = 16.sp,
        style: Int = Typeface.NORMAL,
        bgColor: Int? = null
    ) {
        val start = length
        append(text)
        setSpan(ForegroundColorSpan(color), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(AbsoluteSizeSpan(textSize.value.toInt()), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(style), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        bgColor?.let {
            setSpan(BackgroundColorSpan(it), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun createSpeakerButton(audioPath: String?): SpannableString {
        return SpannableString("\uD83D\uDD0A").apply {
            audioPath?.let {
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        playAudio(it)
                    }
                }, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun parseDetailHtml(detail: org.json.JSONObject): Spannable {
        val html = buildString {
            append("<div style='color:#666; margin-left:16dp;'>")
            detail.keys().forEach { key ->
                append("<b>$key</b>: ${detail.getString(key)}<br>")
            }
            append("</div>")
        }
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT) as Spannable
    }

    private fun playAudio(audioPath: String?) {
        val word = intent.getParcelableExtra<WordEntry>("word_entry")?.word
        if (word != null) {
            useBackupTTS(word)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnected == true
    }

    private fun useBackupTTS(text: String) {
        if (!isNetworkAvailable()) {
            android.widget.Toast.makeText(this, "需要网络连接才能使用TTS", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://textreadtts.com/tts/convert?accessKey=FREE&language=english&speaker=speaker5&text=$encodedText"
        
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    
                    if (jsonObject.getInt("code") == 0) {
                        val audioUrl = jsonObject.getString("audio")
                        if (audioUrl.isNotEmpty()) {
                            runOnUiThread { playTTSAudio(audioUrl) }
                        } else {
                            throw Exception("获取到的音频URL为空")
                        }
                    } else {
                        throw Exception("API返回错误：${jsonObject.optString("message", "未知错误")}")
                    }
                } else {
                    throw Exception("HTTP请求失败：${connection.responseCode}")
                }
            } catch (e: Exception) {
                handlePlaybackError(e)
            }
        }.start()
    }

    private var mediaPlayer: android.media.MediaPlayer? = null

    private fun handlePlaybackError(e: Exception) {
        android.util.Log.e("TTS", "播放失败：${e.message}")
        runOnUiThread {
            android.widget.Toast.makeText(this@WordDetailActivity, 
                "无法播放音频：${e.message}", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun playTTSAudio(audioUrl: String) {
        try {
            // 释放现有的MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null

            // 创建并配置新的MediaPlayer
            mediaPlayer = android.media.MediaPlayer().apply {
                setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                setDataSource(audioUrl)
                
                setOnPreparedListener { 
                    start()
                }
                
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }

                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("TTS", "播放错误：$what, $extra")
                    mp.release()
                    mediaPlayer = null
                    runOnUiThread {
                        android.widget.Toast.makeText(this@WordDetailActivity, 
                            "播放出错，请重试", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            handlePlaybackError(e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}