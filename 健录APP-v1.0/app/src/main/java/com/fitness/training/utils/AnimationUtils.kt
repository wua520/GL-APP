package com.fitness.training.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * 动画工具类 - 提供UI微动画效果
 */
object AnimationUtils {
    
    /**
     * 按钮点击缩放效果
     * 按下时缩小到0.95倍，松开时弹回
     */
    fun View.addClickScaleEffect() {
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }
            false // 返回false让点击事件继续传递
        }
    }
    
    /**
     * 卡片点击抬起效果
     * 点击时elevation增加，松开时恢复
     */
    fun View.addCardLiftEffect() {
        val originalElevation = elevation
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate()
                        .translationZ(8f)
                        .setDuration(150)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .translationZ(0f)
                        .setDuration(150)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            false
        }
    }
    
    /**
     * 数字变化时的放大动画
     * 数字更新时放大到1.15倍再缩回
     */
    fun View.animateNumberChange() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.15f, 1f)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.15f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }
    
    /**
     * 淡入动画
     */
    fun View.fadeIn(duration: Long = 300) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * 淡出动画
     */
    fun View.fadeOut(duration: Long = 300, onEnd: (() -> Unit)? = null) {
        animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }
    
    /**
     * 从上方滑入+淡入
     */
    fun View.slideInFromTop(duration: Long = 300) {
        translationY = -100f
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
