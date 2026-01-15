package com.example.checkinreceipts.data.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

object ExportUtils {
    private val phonePattern: Pattern = Pattern.compile(
        """\d{3,4}[\s-]?\d{3,4}"""
    )
    private val idPattern: Pattern = Pattern.compile(
        """"remoteId"\s*:\s*"?([^"]+)"?"""
    )
    fun redactJson(raw: String): String {
        var s = raw
        run {
            val m: Matcher = phonePattern.matcher(s)
            val out = StringBuilder()
            var last = 0
            while (m.find()) {
                val t = m.group()
                val masked = if (t.length <= 4) {
                    "***"
                } else {
                    val start = t.length / 3
                    val end = t.length * 2 / 3
                    val stars = "*".repeat(end - start)
                    t.substring(0, start) + stars + t.substring(end)
                }
                out.append(s, last, m.start()).append(masked)
                last = m.end()
            }
            out.append(s, last, s.length)
            s = out.toString()
        }
        run {
            val m: Matcher = idPattern.matcher(s)
            val out = StringBuilder()
            var last = 0
            while (m.find()) {
                val v = m.group(1) ?: ""
                val r = if (v.length <= 2) {
                    "**"
                } else {
                    v.first() + "*".repeat(v.length - 2) + v.last()
                }
                val replacement = "\"remoteId\":\"$r\""
                out.append(s, last, m.start()).append(replacement)
                last = m.end()
            }
            out.append(s, last, s.length)
            s = out.toString()
        }
        return s
    }

    fun firstLines(text: String, max: Int = 10): String =
        text.lineSequence().take(max).joinToString("\n")

    fun copyToClipboard(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun getContentUri(ctx: Context, file: File): Uri {
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
    }

    fun shareJson(ctx: Context, file: File) {
        val uri = getContentUri(ctx, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share JSON"))
    }

    suspend fun readFileText(file: File): String =
        withContext(Dispatchers.IO) { file.readText() }

    fun isLikelyJsonObjectOrArray(s: String): Boolean {
        val t = s.trim()
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
    }

    fun formatJsonIfPossible(s: String): String = try {
        val t = s.trim()
        when {
            t.startsWith("{") -> JSONObject(t).toString(2)
            t.startsWith("[") -> JSONArray(t).toString(2)
            else -> s
        }
    } catch (_: Exception) {
        s
    }
}