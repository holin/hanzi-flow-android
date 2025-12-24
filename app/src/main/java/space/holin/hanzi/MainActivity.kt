package space.holin.hanzi

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), WebAppInterface.OnOCRRequestListener {
    // Kotlin 空安全声明，延迟初始化
    private lateinit var webView: WebView
    private lateinit var webAppInterface: WebAppInterface
    private var photoFile: File? = null

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Log.e("OCR", "相机权限被拒绝")
            showToast("需要相机权限才能使用拍照识别功能")
        }
    }

    // 拍照
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            Log.d("OCR", "拍照成功，开始识别")
            processImageForOCR(photoFile!!)
        } else {
            Log.e("OCR", "拍照失败")
            showToast("拍照失败")
        }
    }

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

        // 初始化 WebAppInterface 并设置监听器
        webAppInterface = WebAppInterface(this)
        webAppInterface.onOCRRequestListener = this

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

        // 把 WebAppInterface 注入到 WebView，命名为 "AndroidTTS"
        webView.addJavascriptInterface(webAppInterface, "AndroidTTS")

        // 加载目标网址
        webView.loadUrl("http://localhost:18080")
    }

    // 实现 OnOCRRequestListener 接口
    override fun onOCRRequest() {
        Log.d("OCR", "收到 OCR 请求")
        checkCameraPermissionAndLaunch()
    }

    // 检查相机权限
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // 启动相机
    private fun launchCamera() {
        photoFile = File(externalCacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile!!)
        takePhotoLauncher.launch(photoUri)
    }

    // 处理图片进行 OCR 识别
    private fun processImageForOCR(file: File) {
        try {
            val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val image = InputImage.fromFilePath(this, photoUri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val recognizedText = result.text
                    Log.d("OCR", "识别成功: $recognizedText")

                    // 调用 JS 回调
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "javascript:if(typeof window.onOCRResult === 'function') { window.onOCRResult(${
                                if (recognizedText.isEmpty()) "\"\"" else "\"${recognizedText.replace(
                                    "\n",
                                    "\\n"
                                ).replace("\"", "\\\"")}\""
                            }); }"
                        ) { value ->
                            Log.d("OCR", "JS 回调完成: $value")
                        }
                    }

                    // 删除临时图片
                    file.delete()
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "识别失败: ${e.message}")
                    showToast("文字识别失败: ${e.message}")
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "javascript:if(typeof window.onOCRResult === 'function') { window.onOCRResult(''); }"
                        , null)
                    }
                    file.delete()
                }
        } catch (e: Exception) {
            Log.e("OCR", "处理图片失败: ${e.message}")
            showToast("处理图片失败")
            file.delete()
        }
    }

    // 显示 Toast 提示
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // 处理返回键：优先返回网页上一页，无历史则退出 App
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // 销毁 WebView，避免内存泄漏（Kotlin 中用 takeIf 做空安全判断）
    override fun onDestroy() {
        webAppInterface.onDestroy()
        webView.destroy()
        super.onDestroy()
    }
}
