package com.aziz.droidclean

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UpdateActivity : Activity() {
    private val releaseApi =
        "https://api.github.com/repos/Postmen1971/DroidClean/releases/latest"

    private lateinit var statusText: TextView
    private lateinit var notesText: TextView
    private lateinit var downloadButton: Button
    private lateinit var installButton: Button
    private var apkUrl: String? = null
    private var downloadId: Long = -1L

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != downloadId) return

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val cursor = manager.query(DownloadManager.Query().setFilterById(id))
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val column = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (column >= 0 && it.getInt(column) == DownloadManager.STATUS_SUCCESSFUL) {
                        statusText.text = "Download abgeschlossen. Du kannst das Update installieren."
                        installButton.visibility = android.view.View.VISIBLE
                    } else {
                        statusText.text = "Download fehlgeschlagen. Bitte versuche es erneut."
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 36, 40, 36)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "DroidClean – App-Updates"
            textSize = 25f
        })
        root.addView(TextView(this).apply {
            text = "Installierte Version: " + BuildConfig.VERSION_NAME
            textSize = 16f
            setPadding(0, 18, 0, 18)
        })

        statusText = TextView(this).apply {
            text = "Prüfe, ob auf GitHub eine neue Version bereitsteht."
            textSize = 16f
        }
        root.addView(statusText)

        notesText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 16)
        }
        root.addView(notesText)

        val checkButton = Button(this).apply {
            text = "Nach Updates suchen"
            setOnClickListener { checkForUpdates() }
        }
        root.addView(checkButton, matchWidth())

        downloadButton = Button(this).apply {
            text = "Update herunterladen"
            visibility = android.view.View.GONE
            setOnClickListener { downloadUpdate() }
        }
        root.addView(downloadButton, matchWidth())

        installButton = Button(this).apply {
            text = "Update installieren"
            visibility = android.view.View.GONE
            setOnClickListener { installUpdate() }
        }
        root.addView(installButton, matchWidth())

        setContentView(scroll)

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
        super.onDestroy()
    }

    private fun checkForUpdates() {
        statusText.text = "Verbindung zu GitHub wird hergestellt …"
        downloadButton.visibility = android.view.View.GONE
        installButton.visibility = android.view.View.GONE
        apkUrl = null

        thread {
            try {
                val connection = URL(releaseApi).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "DroidClean-Android-App")
                connection.connectTimeout = 12000
                connection.readTimeout = 12000

                val response = connection.responseCode
                if (response == 404) {
                    connection.disconnect()
                    runOnUiThread {
                        statusText.text = "Noch wurde kein offizielles GitHub-Release veröffentlicht."
                    }
                    return@thread
                }
                if (response !in 200..299) {
                    throw IllegalStateException("GitHub HTTP " + response)
                }

                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                connection.disconnect()
                val tag = json.optString("tag_name")
                val notes = json.optString("body", "Keine Versionshinweise vorhanden.")
                val assets = json.optJSONArray("assets")
                var foundApk: String? = null

                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.getJSONObject(index)
                        if (asset.optString("name").endsWith(".apk", true)) {
                            foundApk = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (tag.isBlank()) throw IllegalStateException("Release-Version fehlt")
                val newer = isNewer(tag, BuildConfig.VERSION_NAME)

                runOnUiThread {
                    notesText.text = if (newer) "Versionshinweise:\n" + notes else ""
                    if (!newer) {
                        statusText.text = "Du verwendest bereits die aktuelle Version (" +
                            BuildConfig.VERSION_NAME + ")."
                    } else if (foundApk.isNullOrBlank()) {
                        statusText.text = "Version " + tag + " gefunden, aber es wurde keine APK angehängt."
                    } else {
                        apkUrl = foundApk
                        statusText.text = "Neue Version " + tag + " ist verfügbar."
                        downloadButton.visibility = android.view.View.VISIBLE
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text = "Update-Prüfung fehlgeschlagen: " +
                        (error.message ?: "Unbekannter Fehler")
                }
            }
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun numbers(value: String) = value.removePrefix("v").split(".")
            .map { it.takeWhile { character -> character.isDigit() }.toIntOrNull() ?: 0 }
        val remoteParts = numbers(remote)
        val localParts = numbers(local)
        for (index in 0 until maxOf(remoteParts.size, localParts.size)) {
            val a = remoteParts.getOrElse(index) { 0 }
            val b = localParts.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun downloadUpdate() {
        val url = apkUrl ?: return
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("DroidClean Update")
                .setDescription("Neue Version wird heruntergeladen")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(
                    this,
                    Environment.DIRECTORY_DOWNLOADS,
                    "DroidClean-update.apk"
                )

            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            statusText.text = "Download gestartet. Den Fortschritt findest du in den Benachrichtigungen."
        } catch (error: Exception) {
            statusText.text = "Download konnte nicht gestartet werden: " + error.message
        }
    }

    private fun installUpdate() {
        val apk = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "DroidClean-update.apk"
        )
        if (!apk.exists()) {
            statusText.text = "APK nicht gefunden. Bitte lade das Update erneut herunter."
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= 26 &&
            !packageManager.canRequestPackageInstalls()
        ) {
            statusText.text =
                "Erlaube DroidClean in den Android-Einstellungen die Installation unbekannter Apps. Kehre danach hierher zurück und tippe erneut auf Installieren."
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + packageName)
                )
            )
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                packageName + ".fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (error: Exception) {
            statusText.text = "Installer konnte nicht geöffnet werden: " + error.message
        }
    }
}
