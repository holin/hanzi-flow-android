package space.holin.hanzi

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import java.io.IOException

class MainActivity : AppCompatActivity() {
    // Kotlin 空安全声明，延迟初始化
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 启动本地服务器 (端口 18080)
        val server = LocalWebServer(this, 18080)
        try {
            server.start()
            println("Server started on http://localhost:18080")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 初始化 WebView
        webView = findViewById(R.id.webview)

        // WebView 核心配置
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // 启用 JS
        webSettings.domStorageEnabled = true // 启用 DOM 存储
        webSettings.loadWithOverviewMode = true // 自适应屏幕
        webSettings.useWideViewPort = true // 支持宽视图
        webSettings.mediaPlaybackRequiresUserGesture = false // 允许网页不经过用户点击就能播放音频

        // 关键：模拟手机浏览器 User-Agent（解决连接重置问题）
        val userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
        webSettings.userAgentString = userAgent

        // 禁止跳转到系统浏览器，在 WebView 内打开链接
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }

            // 可选：添加网络错误处理（比如显示自定义错误页）
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                // 示例：加载本地错误页（可自行添加）
                // view?.loadUrl("file:///android_asset/error.html")
            }
        }

        // 把上面的类注入到 WebView，命名为 "AndroidTTS"
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidTTS")

        // 加载目标网址
        webView.loadUrl("http://localhost:18080")
    }

    // 处理返回键：优先返回网页上一页，无历史则退出 App
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // 销毁 WebView，避免内存泄漏（Kotlin 中用 takeIf 做空安全判断）
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}