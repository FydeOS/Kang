package io.fydeos.kangtester

import android.graphics.Point
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.fydeos.kangtester.databinding.FragmentAudioCheckBinding
import io.fydeos.kangtester.databinding.FragmentMultiTouchBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [MultiTouchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MultiTouchFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var _binding: FragmentMultiTouchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _scrollX = 0.0
    private var _scrollY = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMultiTouchBinding.inflate(inflater, container, false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding.multiTouchView.statusListener =
            object : MultiTouchCanvas.MultiTouchStatusListener {
                override fun onStatus(pointerLocations: List<Point>, numPoints: Int) {
                    val str =
                        StringBuilder(String.format(getString(R.string.num_touches), numPoints))
                    for (i in 0 until numPoints) {
                        str.append("\n")
                        str.append(pointerLocations[i].x)
                        str.append(", ")
                        str.append(pointerLocations[i].y)
                    }
                    _binding.textView.text = str
                }

                override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float) {
                    Toast.makeText(
                        context,
                        getString(R.string.fling_det).format(p2, p3),
                        Toast.LENGTH_SHORT
                    ).show();
                }

                override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float) {
                    _scrollX += p2
                    _scrollY += p3
                    _binding.textView2.text =
                        getString(R.string.scroll_pos).format(_scrollX, _scrollY)
                }
            }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MultiTouchFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MultiTouchFragment().apply {
            }
    }
}