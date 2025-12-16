package space.holin.hanzi

import android.content.Context
import android.webkit.JavascriptInterface
import android.speech.tts.TextToSpeech
import java.util.Locale

// 1. 实现 TextToSpeech.OnInitListener 接口
class WebAppInterface(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.CHINESE // 设置默认语言
        }
    }

    // 2. 暴露给 JS 的方法
    @JavascriptInterface
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}