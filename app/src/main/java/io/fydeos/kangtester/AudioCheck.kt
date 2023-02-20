package io.fydeos.kangtester

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import io.fydeos.kangtester.databinding.FragmentAudioCheckBinding
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AudioCheck.newInstance] factory method to
 * create an instance of this fragment.
 */
class AudioCheck : Fragment() {
    // TODO: Rename and change types of parameters

    private var _binding: FragmentAudioCheckBinding? = null
    private val binding get() = _binding!!
    private lateinit var _player: MediaPlayer
    private var _playerTimer: Timer? = null
    private var _recorderTimer: Timer? = null
    private lateinit var _recorder: MediaRecorder
    private var _recordStart: LocalDateTime? = null
    private var _recordFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _recordFile = null
        _recordStart = null
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _player = MediaPlayer.create(context, R.raw.sample_audio)
        _recorder = MediaRecorder()
        _binding = FragmentAudioCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun updatePlayStatus() {
        if (_recordStart != null) {
            binding.btnStopAudio.isEnabled = true
            binding.tvCurrentPlaying.text = getText(R.string.audio_recording).toString().format(
                Duration.between(_recordStart!!, LocalDateTime.now()).toMillis() / 1000.0
            )

            binding.btnPlayRecordedAudio.isEnabled = false
            binding.btnRecord.isEnabled = false
            binding.btnPlaySampleAudio.isEnabled = false
            binding.tvAmplitude.text = _recorder.maxAmplitude.toString()
        } else if (_player.isPlaying) {
            binding.btnStopAudio.isEnabled = true
            binding.tvCurrentPlaying.text = getText(R.string.audio_playing).toString()
                .format(_player.currentPosition / 1000.0, _player.duration / 1000.0)

            binding.btnPlayRecordedAudio.isEnabled = false
            binding.btnRecord.isEnabled = false
            binding.btnPlaySampleAudio.isEnabled = false
        } else {
            binding.btnStopAudio.isEnabled = false
            binding.tvCurrentPlaying.text = getText(R.string.audio_not_playing)

            binding.btnPlayRecordedAudio.isEnabled = _recordFile != null
            binding.btnRecord.isEnabled = permissionToRecordAccepted
            binding.btnPlaySampleAudio.isEnabled = true
        }
    }

    private fun stopPlaying() {
        if (_player.isPlaying) {
            _player.stop()
        }
        _playerTimer?.cancel()
        _playerTimer = null
    }

    private fun stopRecording() {
        if (_recordStart != null) {
            _recordStart = null
            _recorder.stop()
            _recorder.release()
        }
        _recorderTimer?.cancel()
        _recorderTimer = null
    }

    private var permissionToRecordAccepted = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        run {
            permissionToRecordAccepted = isGranted
            updatePlayStatus()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePlayStatus()
        _audioManager = getSystemService(context!!, AudioManager::class.java)!!
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        val handler = getView()!!.handler
        binding.btnPlaySampleAudio.setOnClickListener {
            stopPlaying()
            _player.reset();
            val afd = context!!.resources.openRawResourceFd(R.raw.sample_audio)
            _player.setDataSource(afd)
            _player.prepare();
            _player.start()
            _playerTimer = fixedRateTimer("player", false, 0, 100) {
                handler.post {
                    updatePlayStatus()
                }
            }
            updatePlayStatus()
        }
        binding.btnPlayRecordedAudio.setOnClickListener {
            stopPlaying()
            _player.reset();
            _player.setDataSource(context!!, Uri.fromFile(_recordFile!!))
            _player.prepare();
            _player.start()
            _playerTimer = fixedRateTimer("player", false, 0, 100) {
                handler.post {
                    updatePlayStatus()
                }
            }
            updatePlayStatus()
        }
        binding.btnStopAudio.setOnClickListener {
            stopPlaying()
            stopRecording()
            updatePlayStatus()
        }
        binding.btnRecord.setOnClickListener {
            stopRecording()
            _recorder = MediaRecorder()
            _recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            _recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
            _recordFile?.delete()
            _recordFile = File.createTempFile("prefix", "suffix", context!!.cacheDir)
            _recorder.setOutputFile(_recordFile)
            _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
            _recorder.prepare()
            _recorder.start()
            _recordStart = LocalDateTime.now()
            _recorderTimer = fixedRateTimer("recorder", false, 0, 100) {
                handler.post {
                    updatePlayStatus()
                }
            }
        }
        binding.rgVolumeType.setOnCheckedChangeListener { _, _ ->
            run {
                volumeRadioChanged()
            }
        }
        binding.btnLowerVolume.setOnClickListener {
            volumeAction(AudioManager.ADJUST_LOWER)
        }
        binding.btnRaiseVolume.setOnClickListener {
            volumeAction(AudioManager.ADJUST_RAISE)
        }
        binding.btnMute.setOnClickListener {
            volumeAction(AudioManager.ADJUST_MUTE)
        }
        binding.btnUnmute.setOnClickListener {
            volumeAction(AudioManager.ADJUST_UNMUTE)
        }
        binding.btnToggleMute.setOnClickListener {
            volumeAction(AudioManager.ADJUST_TOGGLE_MUTE)
        }

        _volumeBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, i: Intent?) {
                volumeRadioChanged()
            }
        }
        context!!.registerReceiver(
            _volumeBroadcast,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlaying()
        stopRecording()
        _recordFile?.delete()
        context!!.unregisterReceiver(_volumeBroadcast)
    }

    private lateinit var _volumeBroadcast: BroadcastReceiver
    private lateinit var _audioManager: AudioManager
    private var _volType = -1
    private fun volumeRadioChanged() {
        val checkedButton = view!!.findViewById<RadioButton>(binding.rgVolumeType.checkedRadioButtonId)
        if (checkedButton != null) {
            if (binding.rbVolumeAccessibility.isChecked) {
                _volType = AudioManager.STREAM_ACCESSIBILITY
            } else if (binding.rbVolumeAlarm.isChecked) {
                _volType = AudioManager.STREAM_ALARM
            } else if (binding.rbVolumeMusic.isChecked) {
                _volType = AudioManager.STREAM_MUSIC
            } else if (binding.rbVolumeRing.isChecked) {
                _volType = AudioManager.STREAM_RING
            } else if (binding.rbVolumeVoiceCall.isChecked) {
                _volType = AudioManager.STREAM_VOICE_CALL
            } else if (binding.rbVolumeSystem.isChecked) {
                _volType = AudioManager.STREAM_SYSTEM
            } else {
                return
            }
            val vol = _audioManager.getStreamVolume(_volType)
            val min = _audioManager.getStreamMinVolume(_volType)
            val max = _audioManager.getStreamMaxVolume(_volType)
            val mute = _audioManager.isStreamMute(_volType)
            binding.tvCurrentVolume.text =
                getText(R.string.current_volume).toString().format(checkedButton.text, vol, min, max, mute)
        }
    }

    private fun volumeAction(t: Int) {
        if (_volType != -1) {
            _audioManager.adjustStreamVolume(_volType, t, AudioManager.FLAG_SHOW_UI)
            volumeRadioChanged()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AudioCheck.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AudioCheck().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}