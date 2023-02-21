package io.fydeos.kangtester

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fydeos.kangtester.databinding.FragmentWiFiScanCheckBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [WiFiScanCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WiFiScanCheckFragment : Fragment() {
    private lateinit var binding: FragmentWiFiScanCheckBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        run {
            if (isGranted.values.all { it }) {
                permissionUpdated()
            } else {
                if (context != null)
                    Toast.makeText(
                        context!!,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                    ).show()
                activity!!.supportFragmentManager.popBackStack();
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun permissionUpdated() {

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWiFiScanCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun check(perm: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!, perm
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private fun showResult(manual: Boolean) {
        try {
            val result = wifi.scanResults
            val timeFormat = SimpleDateFormat(
                "HH:mm:ss",
                Locale.US
            )
            binding.tvLastReported.text = getString(R.string.scan_result_time).format(
                timeFormat.format(Date()),
                getString(
                    if (manual) {
                        R.string.scan_result_manual
                    } else {
                        R.string.scan_result_auto
                    }
                )
            )
            binding.tvScanResult.text =
                if (result.isNotEmpty()) {
                    result.sortedBy { -it.level }
                        .joinToString("\n") { "%s (%d)".format(it.SSID, it.level) }
                } else {
                    getString(R.string.scan_result_empty)
                }
        } catch (_: SecurityException) {
        }

    }

    private lateinit var wifiScanReceiver: BroadcastReceiver
    private lateinit var wifi: WifiManager
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!(check(Manifest.permission.ACCESS_FINE_LOCATION) && check(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        wifi = context!!.getSystemService(WifiManager::class.java)
        binding.btnScanWifi.setOnClickListener {
            val succeeded = wifi.startScan()
            Toast.makeText(
                context!!, if (succeeded) {
                    R.string.scan_started
                } else {
                    R.string.scan_not_started
                }, Toast.LENGTH_SHORT
            ).show()
        }
        binding.btnGetScanResult.setOnClickListener {
            showResult(true)
        }

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    showResult(false)
                } else {
                    Toast.makeText(
                        context, R.string.scan_failed, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context!!.registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        context!!.unregisterReceiver(wifiScanReceiver)
    }
}