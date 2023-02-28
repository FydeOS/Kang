package io.fydeos.kangtester

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentScreenCaptureCheckBinding


class ScreenCaptureCheckFragment : Fragment() {
    private val _stateResultCode = "result_code"
    private val _stateResultData = "result_data"
    private val _stateStarted = "started"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(_stateResultCode)
            mResultData = savedInstanceState.getParcelable(_stateResultData)
            mStarted = savedInstanceState.getBoolean(_stateStarted)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(activity, R.string.user_cancelled_capture, Toast.LENGTH_SHORT).show()
        } else {
            mResultCode = result.resultCode
            mResultData = result.data
            startScreenCapture()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mResultData != null) {
            outState.putInt(_stateResultCode, mResultCode)
            outState.putParcelable(_stateResultData, mResultData)
            outState.putBoolean(_stateStarted, mStarted)
        }
    }

    private lateinit var binding: FragmentScreenCaptureCheckBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScreenCaptureCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mMediaProjectionManager =
            getSystemService(requireContext(), MediaProjectionManager::class.java)
        binding.btnStartScreenCapture.setOnClickListener {
            if (!mStarted) {
                startScreenCapture()
            } else {
                stopScreenCapture()
                mStarted = false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (mStarted)
            stopScreenCapture()
    }

    fun bindDisplay() {
        Log.w("SS", "Create display!")
        mMediaProjection = mMediaProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)
        val density = resources.displayMetrics.densityDpi
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            binding.svCapture.width, binding.svCapture.height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            binding.svCapture.holder.surface, object : VirtualDisplay.Callback() {}, null
        )
    }

    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null

    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var mStarted = false
    private var mServiceBound = false
    private val mServiceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.w("SS", "SB")
            mServiceBound = true
            bindDisplay()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    private fun startScreenCapture() {
        if (mResultCode != 0 && mResultData != null) {
            mStarted = true
            binding.btnStartScreenCapture.text = getString(R.string.stop_screen_capture)
            requireContext().bindService(
                Intent(context, ScreenCaptureService::class.java),
                mServiceConn,
                Context.BIND_AUTO_CREATE
            )
        } else {
            requestPermissionLauncher.launch(mMediaProjectionManager!!.createScreenCaptureIntent())
        }
    }

    private fun stopScreenCapture() {
        mVirtualDisplay?.surface = null
        mVirtualDisplay?.release()
        mVirtualDisplay = null
        mMediaProjection?.stop()
        mMediaProjection = null
        binding.btnStartScreenCapture.text = getString(R.string.start_screen_capture)
        requireContext().stopService(Intent(context, ScreenCaptureService::class.java))
        if (mServiceBound) {
            requireContext().unbindService(mServiceConn)
            mServiceBound = false
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (binding.svCapture.holder.surface.isValid) {
                val c = binding.svCapture.holder.surface.lockCanvas(null)
                c.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
                binding.svCapture.holder.surface.unlockCanvasAndPost(c)
            }
        }, 60)
    }
}