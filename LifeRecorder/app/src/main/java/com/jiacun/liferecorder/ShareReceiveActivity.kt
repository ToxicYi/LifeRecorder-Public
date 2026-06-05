package com.jiacun.liferecorder

/**
 * ShareReceiveActivity
 *
 * 接收系统分享面板传入的图片、文件或文本，把内容导入 LifeRecorder 文件库，
 * 然后回到 MainActivity。
 */

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.jiacun.liferecorder.feature.file.library.importSharedTextToFileLibrary
import com.jiacun.liferecorder.feature.file.library.importSharedUriToFileLibrary

class ShareReceiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }

        // 本次分享成功导入到文件库的条目数量。
        val importedCount = runCatching {
            when (intent.action) {
                Intent.ACTION_SEND -> importSingleShare(intent)
                Intent.ACTION_SEND_MULTIPLE -> importMultipleShare(intent)
                else -> 0
            }
        }.onFailure { error ->
            Toast.makeText(
                this,
                "保存到 LifeRecorder 失败：${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }.getOrDefault(0)

        if (importedCount > 0) {
            Toast.makeText(
                this,
                "已保存到 LifeRecorder",
                Toast.LENGTH_SHORT
            ).show()
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }

    private fun importSingleShare(intent: Intent): Int {
        // 单文件/单图片分享时的 Uri。
        val streamUri = intent.getStreamUri()
        if (streamUri != null) {
            importSharedUriToFileLibrary(
                context = this,
                uri = streamUri,
                mimeType = intent.type
            )
            return 1
        }

        // 纯文本分享内容，第一版保存为 txt 文件。
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            importSharedTextToFileLibrary(this, text)
            return 1
        }

        return 0
    }

    private fun importMultipleShare(intent: Intent): Int {
        // 多文件分享时系统传入的 Uri 列表。
        val uris = intent.getStreamUris()
        uris.forEach { uri ->
            importSharedUriToFileLibrary(
                context = this,
                uri = uri,
                mimeType = contentResolver.getType(uri) ?: intent.type
            )
        }

        return uris.size
    }

    @Suppress("DEPRECATION")
    private fun Intent.getStreamUri(): Uri? {
        val extraUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

        return extraUri ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    @Suppress("DEPRECATION")
    private fun Intent.getStreamUris(): List<Uri> {
        val extraUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }.orEmpty()

        if (extraUris.isNotEmpty()) {
            return extraUris
        }

        val clip = clipData ?: return emptyList()
        return (0 until clip.itemCount).mapNotNull { index ->
            clip.getItemAt(index).uri
        }
    }
}
