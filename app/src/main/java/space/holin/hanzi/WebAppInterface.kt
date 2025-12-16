package space.holin.hanzi
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.JavascriptInterface
import java.util.Locale

class WebAppInterface(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isLoaded = false // 标记是否初始化成功

    init {
        // 初始化 TTS
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 尝试设置中文
            val result = tts?.setLanguage(Locale.CHINA)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "错误：当前引擎不支持中文，或者缺少语音数据包！")
                // 这里可以尝试弹个 Toast 提示用户
            } else {
                isLoaded = true
                Log.i("TTS", "TTS 初始化成功！")
            }
        } else {
            Log.e("TTS", "TTS 初始化失败，错误码：$status")
        }
    }

    @JavascriptInterface
    fun speak(text: String) {
        Log.d("TTS", "JS 请求播放: $text")

        if (!isLoaded) {
            Log.e("TTS", "播放失败：TTS 还没准备好，或者初始化失败。")
            return
        }

        // 使用 QUEUE_FLUSH：打断上一次，立即播放新的
        // Bundle 参数传 null
        val code = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UniqueId")

        if (code == TextToSpeech.ERROR) {
            Log.e("TTS", "播放指令发送失败 (Generic Error)")
        }
    }

    // 记得在 Activity 销毁时释放资源，防止内存泄漏
    fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
    }
}