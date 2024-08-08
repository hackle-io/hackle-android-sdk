package io.hackle.android.ui.inappmessage.layout

import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator.Listener
import java.util.concurrent.atomic.AtomicInteger

internal interface InAppMessageAnimator {
    fun start()
    fun setListener(listener: Listener?)

    interface Listener {
        fun onAnimationEnd()
    }

    companion object {
        fun of(view: View, animation: Animation): InAppMessageAnimator {
            return InAppMessageViewAnimator(view, animation)
        }

        fun of(vararg animators: InAppMessageAnimator): InAppMessageAnimator {
            return CompositeInAppMessageAnimator(animators.toList())
        }
    }
}


internal class InAppMessageViewAnimator(
    private val view: View,
    private val animation: Animation
) : InAppMessageAnimator {

    override fun start() {
        view.clearAnimation()
        view.startAnimation(animation)
    }

    override fun setListener(listener: Listener?) {
        if (listener != null) {
            animation.setAnimationListener(ListenerAdapter(listener))
        } else {
            animation.setAnimationListener(null)
        }
    }

    private class ListenerAdapter(private val listener: Listener) : AnimationListener {
        override fun onAnimationEnd(animation: Animation?) = listener.onAnimationEnd()
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

internal class CompositeInAppMessageAnimator(
    private val animators: List<InAppMessageAnimator>
) : InAppMessageAnimator, Listener {

    private val endCount = AtomicInteger()
    private var listener: Listener? = null

    init {
        for (animator in animators) {
            animator.setListener(this)
        }
    }

    override fun onAnimationEnd() {
        if (endCount.incrementAndGet() == animators.size) {
            listener?.onAnimationEnd()
        }
    }

    override fun start() {
        animators.forEach { it.start() }
    }

    override fun setListener(listener: Listener?) {
        this.listener = listener
    }
}
