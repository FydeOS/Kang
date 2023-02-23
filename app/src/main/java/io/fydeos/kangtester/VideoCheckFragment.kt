package io.fydeos.kangtester

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.*
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import io.fydeos.kangtester.databinding.FragmentVideoCheckBinding

/**
 * A simple [Fragment] subclass.
 * Use the [VideoCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoCheckFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private lateinit var binding: FragmentVideoCheckBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVideoCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var d: AppCompatDialog
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        d = object : AppCompatDialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            override fun onStart() {
                super.onStart()
                val windowInsetsController =
                    WindowCompat.getInsetsController(this.window!!, this.window!!.decorView)
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                binding.lContainer.removeView(binding.videoView)
                addContentView(binding.videoView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }

            override fun onStop() {
                super.onStop()
                val windowInsetsController =
                    WindowCompat.getInsetsController(this.window!!, this.window!!.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                (binding.videoView.parent as ViewGroup).removeView(binding.videoView)
                binding.lContainer.addView(binding.videoView)
            }
        }
        d.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                d.dismiss()
            }
        })
        binding.rgDecoder.setOnCheckedChangeListener { group, checkedId -> initializePlayer() }
        binding.rgEncoding.setOnCheckedChangeListener { group, checkedId -> initializePlayer() }
        binding.rgVideoSource.setOnCheckedChangeListener { group, checkedId -> initializePlayer() }
        binding.btnFullscreen.setOnClickListener {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            d.show()
        }
    }

    private fun printLog(l: String) {
        binding.tvVideoStatus.text = binding.tvVideoStatus.text.toString() + "\n" + l
    }

    private var player: ExoPlayer? = null
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun  initializePlayer() {
        if (player != null) {
            releasePlayer()
        }
        binding.tvVideoStatus.text = ""

        if (binding.rgDecoder.checkedRadioButtonId == -1 ||
            binding.rgEncoding.checkedRadioButtonId == -1 ||
            binding.rgVideoSource.checkedRadioButtonId == -1
        )
            return
        val softwareOnly = binding.rgDecoder.checkedRadioButtonId == R.id.rb_sw_decode

        val rf = DefaultRenderersFactory(requireContext().applicationContext)
        rf.setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val x = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            if (mimeType.startsWith("video"))
                x.filter { it.softwareOnly == softwareOnly }
            else
                x
        }

        val mediaItem = when (Pair(binding.rgVideoSource.checkedRadioButtonId, binding.rgEncoding.checkedRadioButtonId)) {
            Pair(R.id.rb_local_video, R.id.rb_h264) ->
                MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.h264_video))
            Pair(R.id.rb_local_video, R.id.rb_h265) ->
                MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.h265_video))
            Pair(R.id.rb_streaming_video, R.id.rb_h264) ->
                MediaItem.fromUri("https://fydeos-wordpress-uploads.oss-cn-beijing.aliyuncs.com/wp-content/uploads/2023/02/tears_h264_main_720p_8000.mp4")
            Pair(R.id.rb_streaming_video, R.id.rb_h265) ->
                MediaItem.fromUri("https://fydeos-wordpress-uploads.oss-cn-beijing.aliyuncs.com/wp-content/uploads/2023/02/tears_hevc_720p_4000.mp4")
            else -> return
        }
        val builder = ExoPlayer.Builder(requireContext(), rf)
        builder.setAnalyticsCollector(object : AnalyticsCollector {
            override fun onRenderedFirstFrame(output: Any, renderTimeMs: Long) {
            }

            override fun onBandwidthSample(
                elapsedMs: Int,
                bytesTransferred: Long,
                bitrateEstimate: Long
            ) {
            }

            override fun addListener(listener: AnalyticsListener) {
            }

            override fun removeListener(listener: AnalyticsListener) {
            }

            override fun setPlayer(player: Player, looper: Looper) {
            }

            override fun release() {
            }

            override fun updateMediaPeriodQueueInfo(
                queue: MutableList<MediaSource.MediaPeriodId>,
                readingPeriod: MediaSource.MediaPeriodId?
            ) {
            }

            override fun notifySeekStarted() {
            }

            override fun onAudioEnabled(counters: DecoderCounters) {
            }

            override fun onAudioDecoderInitialized(
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                printLog(getString(R.string.codec_name).format(getString(R.string.audio), decoderName))
            }

            override fun onAudioInputFormatChanged(
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
            }

            override fun onAudioPositionAdvancing(playoutStartSystemTimeMs: Long) {
            }

            override fun onAudioUnderrun(
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long
            ) {
            }

            override fun onAudioDecoderReleased(decoderName: String) {
            }

            override fun onAudioDisabled(counters: DecoderCounters) {
            }

            override fun onAudioSinkError(audioSinkError: Exception) {
            }

            override fun onAudioCodecError(audioCodecError: Exception) {
                printLog(getString(R.string.codec_error).format(getString(R.string.audio), audioCodecError.toString()))
            }

            override fun onVideoEnabled(counters: DecoderCounters) {
            }

            override fun onVideoDecoderInitialized(
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                printLog(getString(R.string.codec_name).format(getString(R.string.video), decoderName))
            }

            override fun onVideoInputFormatChanged(
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
            }

            override fun onDroppedFrames(count: Int, elapsedMs: Long) {
            }

            override fun onVideoDecoderReleased(decoderName: String) {
            }

            override fun onVideoDisabled(counters: DecoderCounters) {
            }

            override fun onVideoFrameProcessingOffset(
                totalProcessingOffsetUs: Long,
                frameCount: Int
            ) {
            }

            override fun onVideoCodecError(videoCodecError: Exception) {
                printLog(getString(R.string.codec_error).format(getString(R.string.video), videoCodecError.toString()))
            }

        })
        player = builder
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                exoPlayer.playWhenReady = true
                exoPlayer.seekTo(0)
                exoPlayer.prepare()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        binding.tvPlaying.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE
                    }
                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        printLog(getString(R.string.video_error).format(error.errorCodeName, error.message, error.cause?.toString()) + "\n")
                    }
                })
            }


        binding.videoView.hideController()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}