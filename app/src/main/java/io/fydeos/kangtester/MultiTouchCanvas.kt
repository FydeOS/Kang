package io.fydeos.kangtester

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat

class MultiTouchCanvas @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {

    interface MultiTouchStatusListener {
        fun onStatus(pointerLocations: List<Point>, numPoints: Int)
        fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float)
        fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float)
    }

    var statusListener: MultiTouchStatusListener? = null
    private val paint = Paint()
    private var totalTouches = 0
    private var circleRadius = 0
    private val pointerLocations = mutableListOf<Point>()
    private val pointerColors = intArrayOf(-0x1, -0xbfc0, -0xbf00c0, -0xbfbf01, -0xbf01, -0xc0, -0xbf0001)
    private val pointerColorsDark = intArrayOf(-0x5f5f60, -0x600000, -0xff6000, -0xffff60, -0x5fff60, -0x5f6000, -0xff5f60)
    private var det: GestureDetectorCompat

    init {
        circleRadius = (CIRCLE_RADIUS_DP * resources.displayMetrics.density).toInt()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        paint.strokeWidth = 3f
        val maxPointers = 100
        for (i in 0 until maxPointers) {
            pointerLocations.add(Point())
        }

        det = GestureDetectorCompat(context, object : GestureDetector.OnGestureListener {
            override fun onDown(p0: MotionEvent): Boolean {
                return true
            }

            override fun onShowPress(p0: MotionEvent) {
            }

            override fun onSingleTapUp(p0: MotionEvent): Boolean {
                return true
            }

            override fun onScroll(
                p0: MotionEvent,
                p1: MotionEvent,
                p2: Float,
                p3: Float
            ): Boolean {
                statusListener?.onScroll(p0, p1, p2, p3)
                return true
            }

            override fun onLongPress(p0: MotionEvent) {
                Toast.makeText(context, "Long Press", Toast.LENGTH_SHORT).show();
            }

            override fun onFling(
                p0: MotionEvent,
                p1: MotionEvent,
                p2: Float,
                p3: Float
            ): Boolean {
                statusListener?.onFling(p0, p1, p2, p3)
                return true
            }


        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        for (i in 0 until totalTouches) {
            val p = pointerLocations[i]
            paint.color = pointerColorsDark[i % pointerColorsDark.size]
            canvas.drawLine(0f, p.y.toFloat(), width.toFloat(), p.y.toFloat(), paint)
            canvas.drawLine(p.x.toFloat(), 0f, p.x.toFloat(), height.toFloat(), paint)
            canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), circleRadius * 5f / 4f, paint)
            paint.color = pointerColors[i % pointerColors.size]
            canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), circleRadius.toFloat(), paint)
        }
        statusListener?.onStatus(pointerLocations, totalTouches)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        det.onTouchEvent(event)
        val pointerIndex = event.actionIndex
        val action = event.actionMasked
        val numTouches = event.pointerCount
        if (numTouches > totalTouches) {
            totalTouches = numTouches
        }
        for (i in 0 until numTouches) {
            pointerLocations[i].x = event.getX(i).toInt()
            pointerLocations[i].y = event.getY(i).toInt()
        }
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> totalTouches = numTouches
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {

                //move indices down, and put the last one up
                if (pointerIndex < numTouches - 1) {
                    val p = pointerLocations[pointerIndex]
                    pointerLocations[numTouches - 1].x = p.x
                    pointerLocations[numTouches - 1].y = p.y
                }
            }
            MotionEvent.ACTION_MOVE -> {
            }
        }
        postInvalidate()
        return true
    }

    companion object {
        private const val CIRCLE_RADIUS_DP = 20
    }
}