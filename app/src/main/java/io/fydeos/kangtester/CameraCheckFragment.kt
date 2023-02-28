package io.fydeos.kangtester

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import io.fydeos.kangtester.databinding.FragmentCameraCheckBinding
import io.fydeos.kangtester.databinding.FragmentMultiTouchBinding
import java.io.IOException
import java.text.DateFormat
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
        _binding.btnCameraChangeSide.setOnClickListener {
            if (cameraChoice == 1) {
                cameraChoice = 2
            } else {
                cameraChoice = 1
            }

            startCamera()
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

    private var cameraChoice = 0

    private fun rndName(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyymmddhhmmss", Locale.US)
        return dateFormat.format(Date())
    }

    var savedToast: Toast? = null

    private fun startCamera() {
        _binding.tvMessage.text = getString(R.string.camera_loading)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(_binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            _binding.btnCameraChangeSide.visibility = if (hasFrontCamera && hasBackCamera) View.VISIBLE else View.INVISIBLE
            if (cameraChoice == 0 && hasFrontCamera) {
                cameraChoice = 1
            }
            if (cameraChoice == 0 && hasBackCamera) {
                cameraChoice = 2
            }
            val camera =
                when (cameraChoice) {
                    1 -> CameraSelector.DEFAULT_FRONT_CAMERA
                    2 -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> {
                        _binding.tvMessage.text = getString(R.string.camera_missing)
                        return@addListener
                    }
                }
            val imageCapture = ImageCapture.Builder().build()
            _binding.btnCaptureImage.setOnClickListener {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, rndName() + ".jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= 29)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val resolver = requireContext().contentResolver
                    val opt = ImageCapture.OutputFileOptions.Builder(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
                    imageCapture.takePicture(
                        opt,
                        ContextCompat.getMainExecutor(requireContext()), // Defines where the callbacks are run
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                _binding.tvMessage.text = getString(R.string.image_saved_message).format(getRealPathFromURI(requireContext(), outputFileResults.savedUri!!))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                _binding.tvMessage.text = getString(R.string.image_capture_error).format(exception.toString())
                            }
                        }
                    )
                } catch (exception: java.lang.Exception) {
                    _binding.tvMessage.text = getString(R.string.image_capture_error).format(exception.toString())
                }
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, camera, preview, imageCapture
                )

                _binding.tvMessage.text = getString(R.string.camera_loaded).format(
                    cameraProvider.availableCameraInfos.size)

            } catch (exc: Exception) {
                _binding.tvMessage.text = exc.toString()
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CameraCheckFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CameraCheckFragment().apply {
            }
    }
}