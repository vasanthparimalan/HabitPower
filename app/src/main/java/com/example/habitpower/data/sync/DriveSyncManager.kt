package com.example.habitpower.data.sync

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Handles Drive REST v3 API calls using a Google account access token.
 *
 * Drive integration requires a Google Cloud project with the Drive API enabled
 * and the app's package name + SHA-1 fingerprint registered as an OAuth 2.0
 * Android client. Without this setup, getAccessToken() will fail with an auth error.
 *
 * Setup steps for the developer:
 * 1. Create project at console.cloud.google.com
 * 2. Enable "Google Drive API"
 * 3. Credentials → Create → OAuth 2.0 Client ID → Android
 * 4. Enter package name "com.example.habitpower" + debug SHA-1 from keytool
 * 5. No client ID needs to be embedded — the Play Services handle it via the installed app
 */
object DriveSyncManager {

    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
    private const val FOLDER_NAME = "HabitPower"
    private const val BASE_URL = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"

    suspend fun getAccessToken(context: Context, accountName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val account = Account(accountName, "com.google")
                GoogleAuthUtil.getToken(context, account, SCOPE)
            } catch (e: UserRecoverableAuthException) {
                null
            } catch (e: Exception) {
                null
            }
        }

    suspend fun syncToCloud(context: Context, accountName: String, csvFiles: Map<String, String>): SyncResult =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(context, accountName)
                    ?: return@withContext SyncResult.AuthRequired

                val folderId = findOrCreateFolder(token)
                    ?: return@withContext SyncResult.Error("Could not access Drive folder")

                var uploaded = 0
                csvFiles.forEach { (name, content) ->
                    val existingId = findFile(token, name, folderId)
                    if (existingId != null) {
                        updateFile(token, existingId, content)
                    } else {
                        createFile(token, name, content, folderId)
                    }
                    uploaded++
                }
                SyncResult.Success(uploaded)
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Unknown error")
            }
        }

    private fun findOrCreateFolder(token: String): String? {
        val query = URLEncoder.encode(
            "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false",
            "UTF-8"
        )
        val response = get(token, "$BASE_URL/files?q=$query&fields=files(id,name)")
        val json = JSONObject(response ?: return null)
        val files = json.optJSONArray("files")
        if (files != null && files.length() > 0) {
            return files.getJSONObject(0).getString("id")
        }
        // Create folder
        val body = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }
        val createResponse = post(token, "$BASE_URL/files", body.toString(), "application/json")
        return JSONObject(createResponse ?: return null).optString("id").takeIf { it.isNotBlank() }
    }

    private fun findFile(token: String, name: String, folderId: String): String? {
        val query = URLEncoder.encode(
            "name='$name' and '$folderId' in parents and trashed=false",
            "UTF-8"
        )
        val response = get(token, "$BASE_URL/files?q=$query&fields=files(id,name)")
        val json = JSONObject(response ?: return null)
        val files = json.optJSONArray("files")
        return if (files != null && files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createFile(token: String, name: String, content: String, folderId: String) {
        val boundary = "habitpower_boundary_${System.currentTimeMillis()}"
        val meta = JSONObject().apply {
            put("name", name)
            put("parents", org.json.JSONArray().apply { put(folderId) })
        }
        val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n" +
            "--$boundary\r\nContent-Type: text/csv\r\n\r\n${content}\r\n--$boundary--"
        postMultipart(token, "$UPLOAD_URL/files?uploadType=multipart", body, "multipart/related; boundary=$boundary")
    }

    private fun updateFile(token: String, fileId: String, content: String) {
        val url = URL("$UPLOAD_URL/files/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "text/csv")
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream).use { it.write(content) }
        conn.responseCode
        conn.disconnect()
    }

    private fun get(token: String, urlStr: String): String? {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            if (conn.responseCode == 200) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun post(token: String, urlStr: String, body: String, contentType: String): String? {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", contentType)
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        return try {
            if (conn.responseCode in 200..201) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun postMultipart(token: String, urlStr: String, body: String, contentType: String) {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", contentType)
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        conn.responseCode
        conn.disconnect()
    }
}

sealed class SyncResult {
    data class Success(val filesUploaded: Int) : SyncResult()
    data object AuthRequired : SyncResult()
    data class Error(val message: String) : SyncResult()
}
