package com.vitalsign.patientmanager.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DiagonalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val scroller = OverScroller(context)
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    private var scrollX = 0
    private var scrollY = 0
    private var maxScrollX = 0
    private var maxScrollY = 0
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    init {
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        if (childCount == 0) return
        
        val child = getChildAt(0)
        val lp = child.layoutParams
        
        val childWidthMeasureSpec = getChildMeasureSpec(
            widthMeasureSpec,
            paddingLeft + paddingRight,
            lp.width
        )
        val childHeightMeasureSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom,
            lp.height
        )
        
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        
        maxScrollX = max(0, child.measuredWidth - (measuredWidth - paddingLeft - paddingRight))
        maxScrollY = max(0, child.measuredHeight - (measuredHeight - paddingTop - paddingBottom))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 0) return
        
        val child = getChildAt(0)
        child.layout(
            paddingLeft - scrollX,
            paddingTop - scrollY,
            paddingLeft - scrollX + child.measuredWidth,
            paddingTop - scrollY + child.measuredHeight
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                scroller.forceFinished(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = lastTouchX - event.x
                val deltaY = lastTouchY - event.y
                
                scrollBy(deltaX.toInt(), deltaY.toInt())
                
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun scrollBy(x: Int, y: Int) {
        val newScrollX = min(maxScrollX, max(0, scrollX + x))
        val newScrollY = min(maxScrollY, max(0, scrollY + y))
        
        if (newScrollX != scrollX || newScrollY != scrollY) {
            scrollX = newScrollX
            scrollY = newScrollY
            requestLayout()
            invalidate()
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        val newScrollX = min(maxScrollX, max(0, x))
        val newScrollY = min(maxScrollY, max(0, y))
        
        if (newScrollX != scrollX || newScrollY != scrollY) {
            scrollX = newScrollX
            scrollY = newScrollY
            requestLayout()
            invalidate()
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    fun smoothScrollBy(dx: Int, dy: Int) {
        scroller.startScroll(scrollX, scrollY, dx, dy, 300)
        invalidate()
    }

    fun smoothScrollTo(x: Int, y: Int) {
        val dx = x - scrollX
        val dy = y - scrollY
        smoothScrollBy(dx, dy)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount > 0) {
            throw IllegalStateException("DiagonalScrollView can host only one direct child")
        }
        super.addView(child, index, params)
    }

    override fun addView(child: View?) {
        if (childCount > 0) {
            throw IllegalStateException("DiagonalScrollView can host only one direct child")
        }
        super.addView(child)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (childCount > 0) {
            throw IllegalStateException("DiagonalScrollView can host only one direct child")
        }
        super.addView(child, params)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            scroller.fling(
                scrollX, scrollY,
                -velocityX.toInt(), -velocityY.toInt(),
                0, maxScrollX,
                0, maxScrollY
            )
            invalidate()
            return true
        }
    }

    // Public methods to get current scroll position
    fun getCurrentScrollX(): Int = scrollX
    fun getCurrentScrollY(): Int = scrollY
    fun getMaxScrollX(): Int = maxScrollX
    fun getMaxScrollY(): Int = maxScrollY
}
