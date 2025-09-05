package com.vitalsign.patientmanager.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TwoDimensionalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var scrollRangeX = 0
    private var scrollRangeY = 0
    private var lastMotionX = 0f
    private var lastMotionY = 0f
    private var isBeingDragged = false
    private var activePointerId = -1
    private var velocityTracker: VelocityTracker? = null
    private val scroller = OverScroller(context)

    init {
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = child.layoutParams as FrameLayout.LayoutParams
            
            val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec) + scrollRangeX,
                MeasureSpec.EXACTLY
            )
            val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec) + scrollRangeY,
                MeasureSpec.EXACTLY
            )
            
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            
            scrollRangeX = max(0, child.measuredWidth - measuredWidth)
            scrollRangeY = max(0, child.measuredHeight - measuredHeight)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        
        if (childCount > 0) {
            val child = getChildAt(0)
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        
        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }

        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(activePointerId)
                if (activePointerIndex == -1) {
                    return false
                }

                val x = ev.getX(activePointerIndex)
                val y = ev.getY(activePointerIndex)
                val xDiff = abs(x - lastMotionX)
                val yDiff = abs(y - lastMotionY)
                
                if (xDiff > 5 || yDiff > 5) {
                    isBeingDragged = true
                    lastMotionX = x
                    lastMotionY = y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y
                lastMotionX = x
                lastMotionY = y
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isBeingDragged = false
                activePointerId = -1
            }
        }

        return isBeingDragged
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastMotionX = event.x
                lastMotionY = event.y
                activePointerId = event.getPointerId(0)
                
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                
                scroller.forceFinished(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = event.findPointerIndex(activePointerId)
                if (activePointerIndex == -1) {
                    return false
                }

                val x = event.getX(activePointerIndex)
                val y = event.getY(activePointerIndex)
                
                val deltaX = (lastMotionX - x).toInt()
                val deltaY = (lastMotionY - y).toInt()
                
                scrollBy(deltaX, deltaY)
                
                lastMotionX = x
                lastMotionY = y
                
                velocityTracker?.addMovement(event)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.let { vt ->
                    vt.addMovement(event)
                    vt.computeCurrentVelocity(1000)
                    val velocityX = vt.getXVelocity(activePointerId)
                    val velocityY = vt.getYVelocity(activePointerId)
                    
                    if (abs(velocityX) > 100 || abs(velocityY) > 100) {
                        scroller.fling(
                            scrollX, scrollY,
                            (-velocityX).toInt(), (-velocityY).toInt(),
                            0, scrollRangeX,
                            0, scrollRangeY
                        )
                        postInvalidate()
                    }
                    
                    vt.recycle()
                    velocityTracker = null
                }
                
                activePointerId = -1
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastMotionX = event.getX(newPointerIndex)
                    lastMotionY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun scrollBy(x: Int, y: Int) {
        val newScrollX = min(scrollRangeX, max(0, scrollX + x))
        val newScrollY = min(scrollRangeY, max(0, scrollY + y))
        
        if (newScrollX != scrollX || newScrollY != scrollY) {
            scrollTo(newScrollX, newScrollY)
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        val clampedX = min(scrollRangeX, max(0, x))
        val clampedY = min(scrollRangeY, max(0, y))
        
        if (clampedX != scrollX || clampedY != scrollY) {
            super.scrollTo(clampedX, clampedY)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount > 0) {
            throw IllegalStateException("TwoDimensionalScrollView can host only one direct child")
        }
        super.addView(child, index, params)
    }

    override fun addView(child: View?) {
        if (childCount > 0) {
            throw IllegalStateException("TwoDimensionalScrollView can host only one direct child")
        }
        super.addView(child)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (childCount > 0) {
            throw IllegalStateException("TwoDimensionalScrollView can host only one direct child")
        }
        super.addView(child, params)
    }

    fun getMaxScrollX(): Int = scrollRangeX
    fun getMaxScrollY(): Int = scrollRangeY
}
