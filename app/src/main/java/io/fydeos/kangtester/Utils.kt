package io.fydeos.kangtester

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
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
