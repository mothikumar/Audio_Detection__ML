package com.addev.listaspam.service

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.addev.listaspam.R
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Class responsible for checking if a newer version of the app is available on a GitHub repository.
 *
 * @property context The application context, used for accessing package information and displaying dialogs.
 * @property githubUser The GitHub username or organization name.
 * @property githubRepo The name of the GitHub repository.
 */
class UpdateChecker(
    private val context: Context,
    private val githubUser: String,
    private val githubRepo: String,
) {

    private val client = OkHttpClient()

    /**
     * Gets the current version name of the app.
     * @return The version name as a string, or "1.0.0" if not available.
     */
    private fun getCurrentVersion(): String {
        return try {
            val manager = context.packageManager
            val info = manager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    /**
     * Gets the current version code of the app.
     * @return The version code as a long.
     */
    private fun getCurrentVersionCode(): Long {
        return try {
            val manager = context.packageManager
            val info = manager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info)
        } catch (e: PackageManager.NameNotFoundException) {
            1L
        }
    }

    fun checkForUpdateSync() {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$githubUser/$githubRepo/releases/latest")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return
            }

            val responseBody = response.body?.string()
            val json = JSONObject(responseBody ?: "")
            val latestTag = json.optString("tag_name")
            val assetsArray = json.optJSONArray("assets")

            if (assetsArray == null || assetsArray.length() == 0) {
                return
            }

            val firstAsset = assetsArray.optJSONObject(0)
            val downloadUrl = firstAsset?.optString("browser_download_url") ?: ""

            val currentVersion = getCurrentVersion()

            if (isUpdateAvailable(currentVersion, latestTag)) {
                showAlert(
                    context.getString(R.string.update_available_title),
                    context.getString(R.string.update_available_message, latestTag),
                    positiveAction = {
                        if (downloadUrl.isNotBlank()) {
                            redirectToGitHubReleasePage(context, downloadUrl)
                        }
                    }
                )
            }

        } catch (e: IOException) {
            // Network error, ignore
        } catch (e: Exception) {
            // Other errors, ignore
        }
    }

    private fun isUpdateAvailable(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false

        val currentClean = current.trimStart('v', 'V').trim()
        val latestClean = latest.trimStart('v', 'V').trim()

        return try {
            // Simple version comparison
            val currentParts = currentClean.split(".")
            val latestParts = latestClean.split(".")

            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val currentPart = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
                val latestPart = latestParts.getOrNull(i)?.toIntOrNull() ?: 0

                if (latestPart > currentPart) {
                    return true
                } else if (latestPart < currentPart) {
                    return false
                }
            }
            false // Versions are equal
        } catch (e: Exception) {
            // Fallback to string comparison if parsing fails
            currentClean != latestClean
        }
    }

    private fun showAlert(title: String, message: String, positiveAction: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.aceptar) { _, _ ->
                    positiveAction?.invoke()
                }
                .setNegativeButton(R.string.cancelar, null)
                .setCancelable(true)
                .show()
        }
    }

    companion object {
        fun redirectToGitHubReleasePage(context: Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                // Could not open URL
            }
        }
    }
}