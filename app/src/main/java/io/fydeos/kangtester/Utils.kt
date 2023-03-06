package io.fydeos.kangtester

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


fun getRealPathFromURI(context: Context, contentUri: Uri): String {
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    val loader = CursorLoader(context, contentUri, proj, null, null, null)
    val cursor = loader.loadInBackground()!!
    val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
    cursor.moveToFirst()
    val result: String = cursor.getString(column_index)
    cursor.close()
    return result
}

fun rndName(): String {
    val dateFormat: DateFormat = SimpleDateFormat("yyyymmddhhmmss", Locale.US)
    return dateFormat.format(Date())
}

@Throws(IOException::class)
fun saveBitmap(
    ctx: Context,
    bitmap: Bitmap, format: Bitmap.CompressFormat,
    mimeType: String, displayName: String
): Uri {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= 29)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val resolver = ctx.contentResolver
    var uri: Uri? = null

    try {
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use {
            if (!bitmap.compress(format, 95, it))
                throw IOException("Failed to save bitmap.")
        } ?: throw IOException("Failed to open output stream.")

        return uri

    } catch (e: IOException) {

        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            resolver.delete(orphanUri, null, null)
        }

        throw e
    }
}
