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
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Make sure we handle all touch events
        android.util.Log.d("FreeScrollContainer", "dispatchTouchEvent: ${ev.action}, pointers: ${ev.pointerCount}")
        
        // For multi-touch, ensure parent doesn't interfere
        if (ev.pointerCount >= 2) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        android.util.Log.d("FreeScrollContainer", "onTouchEvent: ${event.action}, pointerCount: ${event.pointerCount}")
        
        var handled = scaleGestureDetector.onTouchEvent(event)
        
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        
        return handled || super.onTouchEvent(event)
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Always intercept multi-touch events for zoom
        if (ev.pointerCount >= 2) {
            android.util.Log.d("FreeScrollContainer", "Intercepting multi-touch")
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        
        // For single touch, let gesture detector decide
        return gestureDetector.onTouchEvent(ev)
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
        override fun onDown(e: MotionEvent): Boolean {
            // Request that the parent doesn't intercept our touch events
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

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
        private var initialScrollX = 0f
        private var initialScrollY = 0f
        private var initialScale = 1f
        private var pivotX = 0f
        private var pivotY = 0f
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            android.util.Log.d("FreeScrollContainer", "Scale gesture began")
            // Request that the parent doesn't intercept our touch events during scaling
            parent?.requestDisallowInterceptTouchEvent(true)
            
            // Store initial state
            initialScrollX = scrollX
            initialScrollY = scrollY
            initialScale = scaleFactor
            pivotX = detector.focusX
            pivotY = detector.focusY
            
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            android.util.Log.d("FreeScrollContainer", "Scale factor: ${detector.scaleFactor}")
            
            val newScale = initialScale * detector.scaleFactor
            
            // Constrain scale factor
            val constrainedScale = max(minScale, min(newScale, maxScale))
            
            // Only update if the scale actually changed
            if (constrainedScale != scaleFactor) {
                val scaleChange = constrainedScale / scaleFactor
                
                // Calculate new scroll position to keep content under pivot point stable
                val dx = pivotX * (scaleChange - 1)
                val dy = pivotY * (scaleChange - 1)
                
                scaleFactor = constrainedScale
                scrollX += dx
                scrollY += dy
                
                applyTransformation()
            }
            
            return true
        }
    }
    
    // Public methods for programmatic control
    fun setScale(scale: Float) {
        val newScale = max(minScale, min(scale, maxScale))
        
        if (newScale != scaleFactor) {
            // Calculate zoom from center of view
            val centerX = width / 2f
            val centerY = height / 2f
            val scaleChange = newScale / scaleFactor
            
            // Adjust scroll to keep center point stable
            val dx = centerX * (scaleChange - 1)
            val dy = centerY * (scaleChange - 1)
            
            scaleFactor = newScale
            scrollX += dx
            scrollY += dy
            
            applyTransformation()
        }
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
