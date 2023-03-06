package io.fydeos.kangtester

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import io.fydeos.kangtester.databinding.FragmentExternalStorageCheckBinding
import java.io.*
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
            writeFile(requireContext().getExternalFilesDir(null)!!)
            binding.btnReadAppSpecific.isEnabled = true
        }
        binding.btnReadAppSpecific.setOnClickListener {
            readAndCheck()
        }
        binding.btnSavePicture.setOnClickListener {
            savePictureMediaStore()
        }
        binding.btnBrowsePictures.setOnClickListener {
            val perm = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(perm)
            }
            browsePictureMediaStore()
        }
        binding.btnDocOpen.setOnClickListener {
            requestOpenFileLauncher.launch(arrayOf(binding.inputMime.text.toString()))
        }
        binding.btnDocDirOpen.setOnClickListener {
            requestOpenDirLauncher.launch(null)
        }
        binding.btnReqPerm.setOnClickListener {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            if (Build.VERSION.SDK_INT >= 30) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        uri
                    )
                )
            } else {
                requestStoragePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
        binding.btnWriteExternalStorage.setOnClickListener {
            writeFile(Environment.getExternalStorageDirectory())
            binding.btnReadExternalStorage.isEnabled = true
        }
        binding.btnReadExternalStorage.setOnClickListener {
            readAndCheck()
        }
    }


    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        storageUnlimitedAccessPermission()
    }

    private fun storageUnlimitedAccessPermission() {
        val perm =
            if (Build.VERSION.SDK_INT >= 30) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }

        binding.btnWriteExternalStorage.isEnabled = perm
        binding.btnReqPerm.isEnabled = !perm
        binding.btnReqPerm.text =
            getString(if (perm) R.string.got_storage_manage_permission else R.string.request_storage_manage_permission)
    }

    private var testFilePath: String? = null
    private var testFileContent: String? = null
    private fun writeFile(dir: File) {
        try {
            val f = File(dir, rndName() + ".txt")
            testFilePath = f.path
            testFileContent = UUID.randomUUID().toString()
            f.writeText(testFileContent!!, Charset.defaultCharset())
            binding.tvMessage.text = getString(R.string.write_file_success).format(
                testFilePath,
                testFileContent
            )
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    private fun readAndCheck() {
        try {
            val f = File(testFilePath!!)
            val content = f.readText(Charset.defaultCharset())
            binding.tvMessage.text = getString(R.string.read_file_success_match).format(
                testFilePath,
                content,
                content == testFileContent
            )
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        browsePictureMediaStore()
        if (!isGranted) {
            appendLog(getString(R.string.no_image_permission))
        }
    }

    private val requestOpenFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { url ->
        if (url != null) {
            var fileSize = "Unknown"
            val cursor: Cursor? = requireContext().contentResolver.query(
                url, null, null, null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                    if (!it.isNull(sizeIndex)) {
                        fileSize = it.getString(sizeIndex)
                    }
                }
            }
            var content = ""
            try {
                requireContext().contentResolver.openInputStream(url)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        if (line.length > 20)
                            line = line.substring(0, 20)
                        content = line
                    }
                }
            } catch (ex: java.lang.Exception) {
                content = ex.toString()
            }

            binding.tvMessage.text = getString(R.string.file_info).format(fileSize, content)
        } else {
            binding.tvMessage.text = getString(R.string.file_pick_cancelled)
        }
    }

    private val requestOpenDirLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val sb = java.lang.StringBuilder()
                val documentFile = DocumentFile.fromTreeUri(requireContext(), uri)
                val files = documentFile!!.listFiles()

                for (f in files) {
                    if (f.isDirectory) {
                        sb.appendLine("FOLDER: " + f.name)
                    } else {
                        sb.appendLine("FILE: " + f.name)
                    }
                }
                binding.tvMessage.text = sb.toString()
            } else {
                binding.tvMessage.text = getString(R.string.file_pick_cancelled)
            }
        }

    private fun appendLog(x: String) {
        binding.tvMessage.text = binding.tvMessage.text.toString() + "\n" + x
    }

    private fun browsePictureMediaStore() {
        val galleryImageUrls = mutableListOf<Uri>()
        val columns = arrayOf(MediaStore.Images.Media._ID)
        val orderBy = MediaStore.Images.Media.DATE_MODIFIED

        requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
            null, null, "$orderBy DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                galleryImageUrls.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                )
            }
        }

        binding.tvMessage.text = ""
        binding.rvPictures.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPictures.adapter = MediaStorePictureAdaptor(galleryImageUrls) { n, e ->
            appendLog(
                getString(R.string.image_load_error).format(
                    n,
                    e
                )
            )
        }
        appendLog(
            getString(R.string.image_count).format(
                binding.rvPictures.adapter!!.itemCount
            )
        )
    }

    private fun savePictureMediaStore() {
        val bitmap = BitmapFactory.decodeResource(
            requireContext().resources,
            R.drawable.fydetab
        )
        val filename = rndName() + ".png"
        try {
            val uri = saveBitmap(requireContext(), bitmap, Bitmap.CompressFormat.PNG, "image/x-png", filename)
            binding.tvMessage.text =
                getString(R.string.write_file_success).format(getRealPathFromURI(requireContext(), uri), "(img)")
        } catch (ex: IOException) {
            binding.tvMessage.text = ex.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        storageUnlimitedAccessPermission()
        binding.tvAvailability.text =
            getString(R.string.external_storage_desc).format(Environment.getExternalStorageState())
    }
}