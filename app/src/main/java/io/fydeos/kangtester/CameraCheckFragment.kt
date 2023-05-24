package io.fydeos.kangtester

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentCameraCheckBinding
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [CameraCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraCheckFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var _binding: FragmentCameraCheckBinding
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            if (context != null)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_permission),
                    Toast.LENGTH_SHORT
                ).show()
            requireActivity().supportFragmentManager.popBackStack();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCameraCheckBinding.inflate(inflater, container, false)
        cameraUseCasePreview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(_binding.viewFinder.surfaceProvider)
            }
        cameraUseCaseCapture = ImageCapture.Builder().build()
        _binding.btnCaptureImage.setOnClickListener {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, rndName() + ".jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val resolver = requireContext().contentResolver
                val opt = ImageCapture.OutputFileOptions.Builder(
                    resolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).build()
                cameraUseCaseCapture.takePicture(
                    opt,
                    ContextCompat.getMainExecutor(requireContext()), // Defines where the callbacks are run
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            _binding.tvMessage.text =
                                getString(R.string.image_saved_message).format(
                                    getRealPathFromURI(
                                        requireContext(),
                                        outputFileResults.savedUri!!
                                    )
                                )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            _binding.tvMessage.text =
                                getString(R.string.image_capture_error).format(exception.toString())
                        }
                    }
                )
            } catch (exception: java.lang.Exception) {
                _binding.tvMessage.text =
                    getString(R.string.image_capture_error).format(exception.toString())
            }
        }

        _binding.spinnerCamera.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.e("SEL", "Selected!!!")
                    onCameraSelected()
                }

            }
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun rndName(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyymmddhhmmss", Locale.US)
        return dateFormat.format(Date())
    }

    private fun onCameraSelected() {

        if (cameraProvider != null) {
            val pos = _binding.spinnerCamera.selectedItemPosition
            try {
                val cam = cameras[pos].cameraSelector
                // Unbind use cases before rebinding
                cameraProvider!!.unbindAll()

                // Bind use cases to camera
                cameraProvider!!.bindToLifecycle(
                    this, cam, cameraUseCasePreview, cameraUseCaseCapture
                )

                _binding.tvMessage.text = getString(R.string.camera_loaded)

            } catch (exc: Exception) {
                _binding.tvMessage.text = exc.toString()
                Log.e("Camera", "Use case binding failed", exc)
            }
        }
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraUseCasePreview: Preview
    private lateinit var cameraUseCaseCapture: ImageCapture

    private var cameras: List<CameraInfo> = listOf()

    private fun lensFacingString(l: Int): String {
        return when (l) {
            CameraSelector.LENS_FACING_BACK -> "Back"
            CameraSelector.LENS_FACING_FRONT -> "Front"
            else -> "Unknown(${l})"
        }
    }

    private fun formatResolution(s: Size?): String {
        val pxTotal = s?.run { width * height } ?: 0

        return DecimalFormat("##.#").format(pxTotal / 1_000_000.0) + "MP"
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun startCamera() {
        _binding.tvMessage.text = getString(R.string.camera_loading)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()

                cameras = cameraProvider!!.availableCameraInfos
                val infos = cameras.map {
                    val c = Camera2CameraInfo.from(it)
                    val facing = c.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    val res =
                        c.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

                    "ID: ${c.cameraId}, Facing: ${lensFacingString(facing ?: -1)}, Res: ${
                        formatResolution(
                            res
                        )
                    }"
                }

                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    this.requireContext(),
                    android.R.layout.simple_spinner_item, infos.toTypedArray()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                _binding.spinnerCamera.adapter = adapter
            } catch (exc: Exception) {
                _binding.tvMessage.text = exc.toString()
                Log.e("Camera", "Camera info loading failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}