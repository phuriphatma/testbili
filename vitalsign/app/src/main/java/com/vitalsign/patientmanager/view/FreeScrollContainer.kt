package com.vitalsign.patientmanager.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

class FreeScrollContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactor = 1.0f
    
    private val minScale = 0.5f
    private val maxScale = 3.0f
    
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    
    private var childView: View? = null

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (childCount > 0) {
            throw IllegalStateException("FreeScrollContainer can host only one direct child")
        }
        super.addView(child, index, params)
        childView = child
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        
        return handled || super.onTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTransformation()
    }

    private fun applyTransformation() {
        childView?.let { child ->
            // Apply scale
            child.scaleX = scaleFactor
            child.scaleY = scaleFactor
            
            // Calculate bounds for scrolling with zoom
            val scaledWidth = child.width * scaleFactor
            val scaledHeight = child.height * scaleFactor
            
            val maxScrollX = max(0f, scaledWidth - width)
            val maxScrollY = max(0f, scaledHeight - height)
            
            // Constrain scroll position
            scrollX = max(0f, min(scrollX, maxScrollX))
            scrollY = max(0f, min(scrollY, maxScrollY))
            
            // Apply translation
            child.translationX = -scrollX
            child.translationY = -scrollY
            
            android.util.Log.d("FreeScrollContainer", "Scale: $scaleFactor, Scroll: ($scrollX, $scrollY)")
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Move in opposite direction of finger movement
            scrollX += distanceX
            scrollY += distanceY
            
            applyTransformation()
            return true
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Add some momentum scrolling
            scrollX -= velocityX * 0.1f
            scrollY -= velocityY * 0.1f
            
            applyTransformation()
            return true
        }
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            
            // Constrain scale factor
            scaleFactor = max(minScale, min(scaleFactor, maxScale))
            
            // Adjust scroll position to zoom into the center of the gesture
            val focusX = detector.focusX
            val focusY = detector.focusY
            
            val scaleChange = scaleFactor / previousScale
            
            // Calculate the content position under the focus point before scaling
            val contentFocusX = (focusX + scrollX) / previousScale
            val contentFocusY = (focusY + scrollY) / previousScale
            
            // Calculate new scroll position to keep the same content under the focus point
            scrollX = contentFocusX * scaleFactor - focusX
            scrollY = contentFocusY * scaleFactor - focusY
            
            applyTransformation()
            return true
        }
    }
    
    // Public methods for programmatic control
    fun setScale(scale: Float) {
        scaleFactor = max(minScale, min(scale, maxScale))
        applyTransformation()
    }
    
    fun getScale(): Float = scaleFactor
    
    fun resetZoom() {
        scaleFactor = 1.0f
        scrollX = 0f
        scrollY = 0f
        applyTransformation()
    }
    
    fun zoomIn() {
        setScale(scaleFactor * 1.2f)
    }
    
    fun zoomOut() {
        setScale(scaleFactor / 1.2f)
    }
}
