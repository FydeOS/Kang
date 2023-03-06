package io.fydeos.kangtester

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.fydeos.kangtester.databinding.FragmentFirstBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    private val requestOverlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(requireContext())) {
            createOverlayWindow()
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

    private fun createOverlayWindow() {
        Toast.makeText(requireContext(), R.string.overlay_description, Toast.LENGTH_LONG).show()
        val intent = Intent(requireContext(), OverlayService::class.java)
        requireContext().startForegroundService(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoAudioCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_audioCheck)
        }
        binding.btnGoTouchCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_multiTouchFragment)
        }
        binding.btnCameraCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_cameraCheckFragment)
        }
        binding.btnSensorCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_sensorCheckFragment)
        }
        binding.btnNotificationCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_notificationCheckFragment)
        }
        binding.btnGoWifiCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_wiFiScanCheckFragment)
        }
        binding.btnGoVideoCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_videoCheckFragment)
        }
        binding.btnGoScreenCapture.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_screenCaptureCheckFragment)
        }
        binding.btnGoStorageCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_externalStorageCheckFragment)
        }
        binding.btnShowOverlayWindow.setOnClickListener {
            if (!Settings.canDrawOverlays(requireContext())) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                )
                requestOverlayLauncher.launch(intent)
            } else {
                createOverlayWindow()
            }
        }
        binding.btnGoNpuCheck.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_rk3588NpuCheckFragment)
        }
    }

    /*
    protected fun onNewIntent(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey("menuFragment")) {
                val fragmentTransaction: FragmentTransaction =
                    getSupportFragmentManager().beginTransaction()
                fragmentTransaction.replace(
                    R.id.detail_fragment_container,
                    MyFragment.newInstance()
                ).commit()
            }
        }
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}