package space.holin.hanzi

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class LocalWebServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        var uri = session.uri

        // 默认访问首页
        if (uri == "/") {
            uri = "/index.html"
        }

        return try {
            // 从 Android 的 assets 文件夹读取文件
            // 假设你的 React build 文件都放在 assets/www 目录下
            val mimeType = getLocalMimeType(uri)
            val inputStream = context.assets.open("www$uri")

            newChunkedResponse(Response.Status.OK, mimeType, inputStream)
        } catch (e: IOException) {
            // 文件找不到，返回 404
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }
    }

    // 简单的 MIME 类型判断，根据需要补充
    private fun getLocalMimeType(uri: String): String {
        return when {
            uri.endsWith(".html") -> "text/html"
            uri.endsWith(".css") -> "text/css"
            uri.endsWith(".js") -> "application/javascript"
            uri.endsWith(".json") -> "application/json"
            uri.endsWith(".png") -> "image/png"
            else -> "application/octet-stream"
        }
    }
}