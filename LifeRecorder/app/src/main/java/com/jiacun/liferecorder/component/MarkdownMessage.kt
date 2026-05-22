package com.jiacun.liferecorder.component

/**
 * MarkdownMessage
 *
 * 使用安全 WebView 渲染 AI/Agent 回复中的 Markdown 文本，负责把 Markdown 转成受控 HTML，
 * 并根据内容高度自动撑开聊天气泡。
 */

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownMessage(
    // 需要显示的 Markdown 原文。
    text: String,
    // 外部传入的布局修饰符。
    modifier: Modifier = Modifier
) {
    // 当前屏幕密度，用于把 WebView 像素高度转换为 dp。
    val density = LocalDensity.current

    // 当前文本对应的 HTML，只在 text 变化时重新生成。
    val html = remember(text) {
        markdownToSafeHtml(text)
    }

    // WebView 的动态高度，每条消息按 text 独立记忆。
    var webViewHeight by remember(text) {
        mutableStateOf(40.dp)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(webViewHeight),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                // JavaScript is enabled only to measure local, escaped HTML height.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = false
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = false
                settings.textZoom = 100
                settings.defaultTextEncodingName = "utf-8"
                settings.blockNetworkLoads = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        updateMeasuredHeight(view, density) { measuredHeight ->
                            webViewHeight = measuredHeight
                        }
                    }
                }
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    "https://liferecorder.local/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
                updateMeasuredHeight(webView, density) { measuredHeight ->
                    webViewHeight = measuredHeight
                }
            } else {
                updateMeasuredHeight(webView, density) { measuredHeight ->
                    webViewHeight = measuredHeight
                }
            }
        }
    )
}

private fun updateMeasuredHeight(
    webView: WebView,
    density: androidx.compose.ui.unit.Density,
    onHeightChanged: (androidx.compose.ui.unit.Dp) -> Unit
) {
    val heightScript = """
        (function() {
            var body = document.body || {};
            var html = document.documentElement || {};
            return Math.max(
                body.scrollHeight || 0,
                body.offsetHeight || 0,
                html.clientHeight || 0,
                html.scrollHeight || 0,
                html.offsetHeight || 0
            );
        })();
    """.trimIndent()

    listOf(80L, 180L, 360L, 720L).forEach { delayMillis ->
        webView.postDelayed(
            {
                webView.evaluateJavascript(heightScript) { value ->
                    val cssHeight = value
                        ?.trim()
                        ?.trim('"')
                        ?.toFloatOrNull()
                    val fallbackHeight = with(density) {
                        (webView.contentHeight * webView.scale)
                            .roundToInt()
                            .toDp()
                    }

                    val measuredHeight = when {
                        cssHeight != null && cssHeight > 0f -> maxOf(cssHeight.dp, fallbackHeight)
                        fallbackHeight > 0.dp -> fallbackHeight
                        else -> 40.dp
                    }.coerceAtLeast(40.dp)

                    onHeightChanged(measuredHeight)
                }
            },
            delayMillis
        )
    }
}

private fun markdownToSafeHtml(markdown: String): String {
    val body = markdownToHtmlBody(markdown)

    return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    color: #222222;
                    font-family: sans-serif;
                    font-size: 15px;
                    line-height: 1.65;
                    word-break: break-word;
                    overflow-wrap: anywhere;
                }
                body {
                    padding: 0;
                }
                h1, h2, h3 {
                    margin: 4px 0 8px;
                    color: #111111;
                    font-weight: 700;
                    line-height: 1.25;
                }
                h1 { font-size: 20px; }
                h2 { font-size: 18px; }
                h3 { font-size: 16px; }
                p {
                    margin: 0 0 10px 0;
                }
                strong {
                    font-weight: 700;
                    color: #111111;
                }
                ul, ol {
                    padding-left: 20px;
                    margin: 6px 0 10px 0;
                }
                li {
                    margin: 4px 0;
                }
                code {
                    display: inline-block;
                    background: #F1F2F4;
                    color: #222222;
                    padding: 2px 6px;
                    border-radius: 6px;
                    font-family: monospace;
                    font-size: 13px;
                }
                pre {
                    background: #F1F2F4;
                    padding: 12px;
                    border-radius: 12px;
                    overflow-x: auto;
                    white-space: pre;
                    margin: 8px 0 12px 0;
                }
                pre code {
                    display: block;
                    background: transparent;
                    padding: 0;
                    border-radius: 0;
                    font-family: monospace;
                    font-size: 13px;
                    line-height: 1.55;
                }
            </style>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()
}

private fun markdownToHtmlBody(markdown: String): String {
    val html = StringBuilder()
    val codeBlock = StringBuilder()
    var inCodeBlock = false
    var inList = false

    fun closeListIfNeeded() {
        if (inList) {
            html.append("</ul>")
            inList = false
        }
    }

    markdown.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                html.append("<pre><code>")
                    .append(escapeHtml(codeBlock.toString().trimEnd()))
                    .append("</code></pre>")
                codeBlock.clear()
                inCodeBlock = false
            } else {
                closeListIfNeeded()
                inCodeBlock = true
            }
            return@forEach
        }

        if (inCodeBlock) {
            codeBlock.append(line).append('\n')
            return@forEach
        }

        when {
            trimmed.isBlank() -> {
                closeListIfNeeded()
            }

            trimmed.startsWith("# ") -> {
                closeListIfNeeded()
                html.append("<h1>")
                    .append(renderInline(trimmed.removePrefix("# ").trim()))
                    .append("</h1>")
            }

            trimmed.startsWith("## ") -> {
                closeListIfNeeded()
                html.append("<h2>")
                    .append(renderInline(trimmed.removePrefix("## ").trim()))
                    .append("</h2>")
            }

            trimmed.startsWith("### ") -> {
                closeListIfNeeded()
                html.append("<h3>")
                    .append(renderInline(trimmed.removePrefix("### ").trim()))
                    .append("</h3>")
            }

            trimmed.startsWith("- ") -> {
                if (!inList) {
                    html.append("<ul>")
                    inList = true
                }
                html.append("<li>")
                    .append(renderInline(trimmed.removePrefix("- ").trim()))
                    .append("</li>")
            }

            else -> {
                closeListIfNeeded()
                html.append("<p>")
                    .append(renderInline(trimmed))
                    .append("</p>")
            }
        }
    }

    if (inCodeBlock) {
        html.append("<pre><code>")
            .append(escapeHtml(codeBlock.toString().trimEnd()))
            .append("</code></pre>")
    }
    closeListIfNeeded()

    return html.toString()
}

private fun renderInline(text: String): String {
    val html = StringBuilder()
    var index = 0

    while (index < text.length) {
        val boldIndex = text.indexOf("**", startIndex = index).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val codeIndex = text.indexOf("`", startIndex = index).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val nextIndex = minOf(boldIndex, codeIndex)

        if (nextIndex == Int.MAX_VALUE) {
            html.append(escapeHtml(text.substring(index)))
            break
        }

        if (nextIndex > index) {
            html.append(escapeHtml(text.substring(index, nextIndex)))
        }

        if (nextIndex == boldIndex) {
            val endIndex = text.indexOf("**", startIndex = boldIndex + 2)
            if (endIndex > boldIndex) {
                html.append("<strong>")
                    .append(escapeHtml(text.substring(boldIndex + 2, endIndex)))
                    .append("</strong>")
                index = endIndex + 2
            } else {
                html.append("**")
                index = boldIndex + 2
            }
        } else {
            val endIndex = text.indexOf("`", startIndex = codeIndex + 1)
            if (endIndex > codeIndex) {
                html.append("<code>")
                    .append(escapeHtml(text.substring(codeIndex + 1, endIndex)))
                    .append("</code>")
                index = endIndex + 1
            } else {
                html.append("`")
                index = codeIndex + 1
            }
        }
    }

    return html.toString()
}

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
