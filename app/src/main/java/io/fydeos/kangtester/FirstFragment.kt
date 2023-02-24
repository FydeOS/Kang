package io.fydeos.kangtester

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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