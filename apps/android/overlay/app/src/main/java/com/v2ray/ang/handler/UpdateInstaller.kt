package com.v2ray.ang.handler

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Downloads official MolidoVPN release APKs and hands installation to Android's
 * package installer. Android still requires the user to approve installation.
 */
object UpdateInstaller {
    private const val RELEASE_PREFIX =
        "https://github.com/hidooch980/MLH-VPN/releases/download/"
    private const val PREFS = "molidovpn_update"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_PENDING_URI = "pending_uri"
    private const val KEY_SETTINGS_OPENED = "settings_opened"
    private const val APK_MIME = "application/vnd.android.package-archive"

    fun register(activity: Activity): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id >= 0) onDownloadComplete(activity, id)
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return receiver
    }

    fun startDownload(context: Context, rawUrl: String) {
        val uri = Uri.parse(rawUrl)
        val segments = uri.pathSegments
        require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 &&
            uri.userInfo == null && uri.query == null && uri.fragment == null &&
            segments.size == 6 && segments[0] == "hidooch980" &&
            segments[1] == "MLH-VPN" && segments[2] == "releases" &&
            segments[3] == "download" && segments[4].matches(Regex("v[0-9A-Za-z.-]+")) &&
            segments[5].matches(Regex("MLHVPN_[0-9A-Za-z.-]+-fdroid_(arm64-v8a|armeabi-v7a|x86|x86_64|universal)\\.apk"))) {
            "Untrusted update URL"
        }

        val manager = context.getSystemService(DownloadManager::class.java)
        val request = DownloadManager.Request(uri)
            .setTitle("MolidoVPN AI")
            .setDescription("Downloading verified release package")
            .setMimeType(APK_MIME)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, "molidovpn-update.apk"
            )
        val id = manager.enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_DOWNLOAD_ID, id)
            .remove(KEY_PENDING_URI)
            .apply()
        Toast.makeText(context, "دانلود به‌روزرسانی آغاز شد", Toast.LENGTH_LONG).show()
    }

    private fun onDownloadComplete(activity: Activity, id: Long) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_DOWNLOAD_ID, -1L) != id) return
        val manager = activity.getSystemService(DownloadManager::class.java)
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return
        cursor.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
                Toast.makeText(context, "دانلود به‌روزرسانی ناموفق بود", Toast.LENGTH_LONG).show()
                return
            }
        }
        val apkUri = manager.getUriForDownloadedFile(id) ?: return
        prefs.edit().putString(KEY_PENDING_URI, apkUri.toString())
            .remove(KEY_DOWNLOAD_ID).apply()
        if (launchInstaller) openPendingInstall(context)
    }

    /**
     * DownloadManager continues after the app process is killed. Reconcile its
     * persisted download ID on the next foreground, rather than losing the
     * completion broadcast when no Activity receiver was registered.
     */
    fun resumePendingDownload(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0L) return
        val manager = context.getSystemService(DownloadManager::class.java) ?: return
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return
        val status = cursor.use {
            if (!it.moveToFirst()) return
            it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }
        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> onDownloadComplete(context, id, launchInstaller = false)
            DownloadManager.STATUS_FAILED -> {
                prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
                Toast.makeText(context, "دانلود به‌روزرسانی ناموفق بود", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Called when the app returns from Android's unknown-app install permission screen. */
    fun openPendingInstall(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rawUri = prefs.getString(KEY_PENDING_URI, null) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            if (prefs.getBoolean(KEY_SETTINGS_OPENED, false)) {
                prefs.edit().remove(KEY_SETTINGS_OPENED).apply()
                Toast.makeText(context, "برای نصب، اجازهٔ نصب از این برنامه را فعال کن", Toast.LENGTH_LONG).show()
                return
            }
            prefs.edit().putBoolean(KEY_SETTINGS_OPENED, true).apply()
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
        }
        prefs.edit().remove(KEY_SETTINGS_OPENED).apply()
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(rawUri), APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(install)
        prefs.edit().remove(KEY_PENDING_URI).apply()
    }
}
