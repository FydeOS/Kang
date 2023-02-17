package io.fydeos.kangtester

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    ): View? {

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}