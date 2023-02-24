package io.fydeos.kangtester

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.content.CursorLoader
import io.fydeos.kangtester.databinding.FragmentExternalStorageCheckBinding
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ExternalStorageCheckFragment : Fragment() {

    private lateinit var binding: FragmentExternalStorageCheckBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentExternalStorageCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnWriteAppSpecific.setOnClickListener {
            writeToAppSpecificDir()
        }
        binding.btnReadAppSpecific.setOnClickListener {
            readFromAppSpecificDir()
        }
        binding.btnSavePicture.setOnClickListener {
            savePictureMediaStore()
        }
    }

    private fun rndName(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyymmddhhmmss", Locale.US)
        return dateFormat.format(Date())
    }

    private var appSpecificFilePath: String? = null
    private var appSpecificContent: String? = null
    private fun writeToAppSpecificDir() {

        try {
            val f =
                File(requireContext().getExternalFilesDir(null), rndName() + ".txt")
            appSpecificFilePath = f.path
            appSpecificContent = UUID.randomUUID().toString()
            f.writeText(appSpecificContent!!, Charset.defaultCharset())
            binding.tvMessage.text = getString(R.string.write_file_success).format(
                appSpecificFilePath,
                appSpecificContent
            )
            binding.btnReadAppSpecific.isEnabled = true
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    private fun readFromAppSpecificDir() {
        try {
            val f = File(appSpecificFilePath!!)
            val content = f.readText(Charset.defaultCharset())
            binding.tvMessage.text = getString(R.string.read_file_success_match).format(
                appSpecificFilePath,
                content,
                content == appSpecificContent
            )
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    private fun browsePictureMediaStore() {

    }

    private fun savePictureMediaStore() {
        val bitmap = BitmapFactory.decodeResource(
            requireContext().resources,
            R.drawable.fydetab
        )
        val filename = rndName() + ".png"
        try {
            val uri = saveBitmap(bitmap, Bitmap.CompressFormat.PNG, "image/x-png", filename)
            binding.tvMessage.text = getString(R.string.write_file_success).format(getRealPathFromURI(uri), "(img)")
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    @Throws(IOException::class)
    fun saveBitmap(
        bitmap: Bitmap, format: Bitmap.CompressFormat,
        mimeType: String, displayName: String
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= 29)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = requireContext().contentResolver
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

    private fun getRealPathFromURI(contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val loader = CursorLoader(requireContext(), contentUri, proj, null, null, null)
        val cursor = loader.loadInBackground()!!
        val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val result: String = cursor.getString(column_index)
        cursor.close()
        return result
    }

    override fun onResume() {
        super.onResume()
        binding.tvAvailability.text =
            getString(R.string.external_storage_desc).format(Environment.getExternalStorageState())
    }
}