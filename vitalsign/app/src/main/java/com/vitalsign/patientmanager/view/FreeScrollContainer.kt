package com.vitalsign.patientmanager.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FreeScrollContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var maxScrollX = 0
    private var maxScrollY = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var activePointerId = -1
    
    private var velocityTracker: VelocityTracker? = null
    private val scroller = OverScroller(context)
    
    private val touchSlop = 8f // Small threshold for immediate response

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        
        // Measure children with unlimited space
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            child.measure(childWidthSpec, childHeightSpec)
        }
        
        // Calculate scroll ranges based on child size vs container size
        if (childCount > 0) {
            val child = getChildAt(0)
            maxScrollX = max(0, child.measuredWidth - measuredWidth)
            maxScrollY = max(0, child.measuredHeight - measuredHeight)
            
            // Debug log
            android.util.Log.d("FreeScrollContainer", "Container: ${measuredWidth}x${measuredHeight}, Child: ${child.measuredWidth}x${child.measuredHeight}, MaxScroll: ${maxScrollX}x${maxScrollY}")
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Layout children
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = ev.x
                lastTouchY = ev.y
                activePointerId = ev.getPointerId(0)
                isDragging = false
                scroller.forceFinished(true)
            }
            
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    val x = ev.getX(pointerIndex)
                    val y = ev.getY(pointerIndex)
                    val dx = abs(x - lastTouchX)
                    val dy = abs(y - lastTouchY)
                    
                    if (dx > touchSlop || dy > touchSlop) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }
        }
        return isDragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()
        velocityTracker?.addMovement(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false

                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                
                val deltaX = (lastTouchX - x).toInt()
                val deltaY = (lastTouchY - y).toInt()
                
                // Scroll immediately
                smoothScrollBy(deltaX, deltaY)
                
                lastTouchX = x
                lastTouchY = y
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.let { tracker ->
                    tracker.computeCurrentVelocity(1000)
                    val xVelocity = tracker.getXVelocity(activePointerId)
                    val yVelocity = tracker.getYVelocity(activePointerId)
                    
                    if (abs(xVelocity) > 50 || abs(yVelocity) > 50) {
                        scroller.fling(
                            scrollX, scrollY,
                            (-xVelocity).toInt(), (-yVelocity).toInt(),
                            0, maxScrollX, 0, maxScrollY
                        )
                        postInvalidate()
                    }
                }
                
                recycleVelocityTracker()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun smoothScrollBy(dx: Int, dy: Int) {
        val newScrollX = min(maxScrollX, max(0, scrollX + dx))
        val newScrollY = min(maxScrollY, max(0, scrollY + dy))
        scrollTo(newScrollX, newScrollY)
    }

    override fun scrollTo(x: Int, y: Int) {
        val oldX = scrollX
        val oldY = scrollY
        val newX = min(maxScrollX, max(0, x))
        val newY = min(maxScrollY, max(0, y))
        
        if (newX != oldX || newY != oldY) {
            super.scrollTo(newX, newY)
            onScrollChanged(newX, newY, oldX, oldY)
            
            // Debug: Print scroll position to verify scrolling is working
            android.util.Log.d("FreeScrollContainer", "Scrolled to: ($newX, $newY) Max: ($maxScrollX, $maxScrollY)")
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recycleVelocityTracker()
    }

    // Prevent multiple children
    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (childCount > 0) {
            throw IllegalStateException("FreeScrollContainer can host only one direct child")
        }
        super.addView(child, index, params)
    }
}
