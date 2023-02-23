package io.fydeos.kangtester

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentSensorCheckBinding

/**
 * A simple [Fragment] subclass.
 * Use the [SensorCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SensorCheckFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private lateinit var _binding: FragmentSensorCheckBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorCheckBinding.inflate(inflater, container, false)
        return _binding.root
    }

    data class SensorType(val type: Int, val name: Int, var listener: SensorEventListener? = null, var sensor: Sensor? = null)

    val sensorTypes = arrayOf(
        SensorType(Sensor.TYPE_ACCELEROMETER, R.string.accelerometer),
        SensorType(Sensor.TYPE_GRAVITY, R.string.gravity),
        SensorType(Sensor.TYPE_GYROSCOPE, R.string.gyroscope),
        SensorType(Sensor.TYPE_LINEAR_ACCELERATION, R.string.linear_acceleration),
        SensorType(Sensor.TYPE_ROTATION_VECTOR, R.string.rotation_vector),
        SensorType(Sensor.TYPE_LIGHT, R.string.luminance),
        SensorType(Sensor.TYPE_MAGNETIC_FIELD, R.string.magnetic_field),
        SensorType(Sensor.TYPE_PROXIMITY, R.string.proximity),
    )


    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = getSystemService(requireContext(), SensorManager::class.java)!!
        for (s in sensorTypes) {
            val sensor = sensorManager.getDefaultSensor(s.type)
            val textView1 = TextView(requireContext())
            textView1.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textView1.setPadding(20, 20, 20, 20) // in pixels (left, top, right, bottom)
            _binding.lSensors.addView(textView1)
            if (sensor != null) {
                textView1.text = getString(R.string.sensor_value).format(
                    getString(s.name),
                    if (sensor.vendor != null) {
                        sensor.vendor
                    } else {
                        getText(R.string.vendor_unknown)
                    },
                    getText(R.string.waiting_for_data)
                )
                s.listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event != null) {
                            val v = event.values.joinToString(", ") { String.format("%.2f", it) }
                            textView1.text = getString(R.string.sensor_value).format(
                                getString(s.name),
                                if (sensor.vendor != null) {
                                    sensor.vendor
                                } else {
                                    getText(R.string.vendor_unknown)
                                },
                                v
                            )
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    }
                }
                s.sensor = sensor
            } else {
                textView1.text = getString(R.string.sensor_value).format(
                    getString(s.name),
                    getText(R.string.vendor_unknown),
                    getText(R.string.sensor_not_exist)
                )
            }

        }
        vibrator = getSystemService(requireContext(), Vibrator::class.java)!!
        _binding.btnVibrate.setOnClickListener {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    override fun onPause() {
        super.onPause()
        for (s in sensorTypes) {
            sensorManager.unregisterListener(s.listener)
        }
    }

    override fun onResume() {
        super.onResume()
        for (s in sensorTypes) {
            sensorManager.registerListener(
                s.listener, s.sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SensorCheckFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SensorCheckFragment().apply {
            }
    }
}