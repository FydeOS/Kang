package io.fydeos.kangtester

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.media.Image
import android.os.*
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentRk3588NpuCheckBinding
import io.fydeos.kangtester.nn.InferenceResult
import io.fydeos.kangtester.nn.yolo.InferenceWrapper
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * A simple [Fragment] subclass.
 * Use the [Rk3588NpuCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class Rk3588NpuCheckFragment : Fragment() {

    private lateinit var binding: FragmentRk3588NpuCheckBinding
    private lateinit var modelPath: String
    private var downloadFuture: Future<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelPath = requireContext().cacheDir.absolutePath + "/model.rknn"
        mInferenceResult.init(requireContext())
    }

    private fun isSafe(): Boolean {
        return !(this.isRemoving || this.activity == null || this.isDetached || !this.isAdded || this.view != null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRk3588NpuCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDownloadModel.setOnClickListener {
            binding.btnDownloadModel.isEnabled = false
            downloadModel()
        }
        binding.rgSource.setOnCheckedChangeListener { group, cid ->
            binding.btnLoadImage.isEnabled = cid == R.id.rb_image_static
            if (cid == R.id.rb_image_static) {
                cameraProvider?.unbindAll()
                val img = resources.openRawResource(R.raw.test_img_yolo).use {
                    BitmapFactory.decodeStream(it)
                }
                inferenceImage(img)
            } else if (cid == R.id.rb_image_from_camera) {
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
        }
        binding.btnLoadImage.setOnClickListener {
            requestOpenFileLauncher.launch(arrayOf("image/*"))
        }
    }

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
        }
    }

    private fun imageToBuffer(img: Image): ByteArray {
        val pixels = img.height * img.width
        val bytes = ByteArray(pixels * 4)
        img.planes[0].buffer.get(bytes)
        return bytes
    }

    private var cameraModelInited = false
    private var cameraProvider: ProcessCameraProvider? = null
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val l = binding.fmImg.layoutParams as ConstraintLayout.LayoutParams
        l.dimensionRatio = "720:1280"

        binding.svCamera.visibility = View.VISIBLE
        binding.ivRecoImage.visibility = View.INVISIBLE

        cameraProviderFuture.addListener({
            val handler = Handler(Looper.getMainLooper())
            cameraProvider?.unbindAll()
            cameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.svCamera.surfaceProvider)
                }
            val imageAnalysis = ImageAnalysis.Builder()
                // enable the following line if RGBA output is needed.
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            cameraModelInited = false
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val img = imageProxy.image
                if (!cameraModelInited) {
                    mInferenceWrapper.initModel(imageProxy.height, imageProxy.width, 3, modelPath);
                    cameraModelInited = true
                }
                if (img != null) {
                    val b = imageToBuffer(img)
                    val output = mInferenceWrapper.run(b)
                    handler.post {
                        showTrackSelectResults(
                            mInferenceWrapper.postProcess(output),
                            imageProxy.width,
                            imageProxy.height,
                            rotationDegrees.toFloat()
                        )
                    }
                }
                imageProxy.close()
            }

            val hasBackCamera = cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            if (hasBackCamera) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
            try {
                // Unbind use cases before rebinding

                // Bind use cases to camera
                cameraProvider!!.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private val mInferenceWrapper = InferenceWrapper()
    private val mInferenceResult = InferenceResult()
    private fun checkModelReady() {
        var ready = false
        if (File(modelPath).exists()) {
            ready = true
            binding.btnDownloadModel.text = getString(R.string.model_ready)
        } else {
            binding.btnDownloadModel.text = getString(R.string.download_npu_model)
        }
        binding.btnDownloadModel.isEnabled = !ready
        binding.rbImageStatic.isEnabled = ready
        binding.rbImageFromCamera.isEnabled = ready
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private fun downloadModel() {
        val handler = Handler(Looper.getMainLooper())

        val urls = getString(R.string.rk_nn_url)
        downloadFuture = executor.submit {
            try {
                val url =
                    URL(urls)
                val inputStream: InputStream = url.openStream()
                val tmpFile = "$modelPath.download"
                val fileOutputStream =
                    FileOutputStream(tmpFile)
                var length: Int
                var downloaded = 0
                val buffer = ByteArray(1024)
                while (inputStream.read(buffer).also { length = it } > -1) {
                    fileOutputStream.write(buffer, 0, length)
                    downloaded += length
                    handler.post {
                        binding.btnDownloadModel.text =
                            getString(R.string.download_progress).format(downloaded / 1024)
                    }
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }
                }
                fileOutputStream.close()
                inputStream.close()
                File(tmpFile).renameTo(File(modelPath))
                handler.post {
                    checkModelReady()
                }
            } catch (e: IOException) {
                binding.btnDownloadModel.text = e.message
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkModelReady()
    }

    override fun onStop() {
        super.onStop()
        downloadFuture?.cancel(true)
    }

    private fun bitmapToRgba(bitmap: Bitmap): ByteArray {
        require(bitmap.config == Bitmap.Config.ARGB_8888) { "Bitmap must be in ARGB_8888 format" }
        val pixels = IntArray(bitmap.width * bitmap.height)
        val bytes = ByteArray(pixels.size * 4)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var i = 0
        for (pixel in pixels) {
            // Get components assuming is ARGB
            val A = pixel shr 24 and 0xff
            val R = pixel shr 16 and 0xff
            val G = pixel shr 8 and 0xff
            val B = pixel and 0xff
            bytes[i++] = R.toByte()
            bytes[i++] = G.toByte()
            bytes[i++] = B.toByte()
            bytes[i++] = A.toByte()
        }
        return bytes
    }

    private fun inferenceImage(bmp: Bitmap) {
        mInferenceResult.reset()

        val l = binding.fmImg.layoutParams as ConstraintLayout.LayoutParams
        l.dimensionRatio = "%d:%d".format(bmp.width, bmp.height)

        binding.svCamera.visibility = View.INVISIBLE
        binding.ivRecoImage.visibility = View.VISIBLE
        binding.ivRecoImage.setImageBitmap(bmp)

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.submit {
            val data = bitmapToRgba(bmp)
            try {
                mInferenceWrapper.initModel(bmp.height, bmp.width, 3, modelPath);
                val output = mInferenceWrapper.run(data)
                handler.post {
                    showTrackSelectResults(
                        mInferenceWrapper.postProcess(output),
                        bmp.width,
                        bmp.height
                    )
                }
            } catch (ex: java.lang.Exception) {
                handler.post {
                    Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sp2px(spValue: Float): Float {
        val r = Resources.getSystem()
        val scale = r.displayMetrics.scaledDensity
        return (spValue * scale + 0.5f)
    }


    private val requestOpenFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { url ->
        if (url != null) {
            val img = requireContext().contentResolver.openInputStream(url)?.use {
                BitmapFactory.decodeStream(it)
            }
            if (img != null) {
                inferenceImage(img)
            }
        }
    }

    private fun showTrackSelectResults(recognitions: ArrayList<InferenceResult.Recognition>, width: Int, height: Int, rotate: Float = 0f) {
        val mTrackResultPaint = Paint().apply{
            color = -0xf91401
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f
            style = Paint.Style.STROKE
            textAlign = Paint.Align.LEFT
            textSize = sp2px(10f)
            typeface = Typeface.SANS_SERIF
            isFakeBoldText = false
        }

        val mTrackResultTextPaint = Paint().apply {
            color = -0xf91401
            strokeWidth = 2f
            textAlign = Paint.Align.LEFT
            textSize = sp2px(12f)
            typeface = Typeface.SANS_SERIF
            isFakeBoldText = false
        }


        val mTrackResultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mTrackResultCanvas = Canvas(mTrackResultBitmap)

        mTrackResultPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)

        for (i in recognitions.indices) {
            val rego: InferenceResult.Recognition = recognitions[i]
            val detection: RectF = rego.location

            mTrackResultCanvas.drawRect(detection, mTrackResultPaint)
            mTrackResultCanvas.drawText(
                mInferenceResult.mPostProcess.getLabelTitle(rego.id),
                detection.left + 5, detection.bottom - 5, mTrackResultTextPaint
            )
        }
        var b = mTrackResultBitmap
        if (rotate != 0f) {
            b = b.rotate(rotate)
        }
        binding.ivRecoResult.setImageBitmap(b)
    }
}