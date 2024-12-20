package io.hackle.android.ui.inappmessage.layout.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.hackle.android.R
import io.hackle.android.internal.task.TaskExecutors.runOnUiThread
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.InAppMessageEvent
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.handle
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.model.InAppMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class InAppMessageScrollImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), InAppMessageLayout.LifecycleListener {

    private val recyclerView: RecyclerView
    private val pageView: TextView
    private var configured: Boolean = false

    private lateinit var inAppMessageView: InAppMessageView
    private lateinit var images: List<InAppMessage.Message.Image>

    private lateinit var impressionManager: ImpressionManager
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ScrollImageAdapter

    private var autoScroll: InAppMessage.Message.ImageAutoScroll? = null
    private var autoScrollJob: ScheduledJob? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.hackle_iam_scroll_image, this, true)
        recyclerView = view.findViewById(R.id.hackle_iam_scroll_image_recycler_view)
        pageView = view.findViewById(R.id.hackle_iam_scroll_image_page_view)
    }

    fun configure(inAppMessageView: InAppMessageView, images: List<InAppMessage.Message.Image>) {
        this.configured = true
        this.inAppMessageView = inAppMessageView
        this.images = images
        this.impressionManager = ImpressionManager()
        this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        this.adapter = ScrollImageAdapter()
        this.autoScroll = inAppMessageView.message.imageAutoScroll

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        val initialPosition = INFINITE_COUNT / 2 - (INFINITE_COUNT / 2 % images.size)
        recyclerView.scrollToPosition(initialPosition)
        recyclerView.addOnScrollListener(ScrollListener())

        updatePage(position = 0)
    }

    private val currentPosition get() = layoutManager.findFirstVisibleItemPosition()

    private fun imageIndex(position: Int): Int {
        return position % images.size
    }

    // Lifecycle

    override fun afterInAppMessageOpen() {
        if (!configured) return
        impressionManager.impressionIfNeeded(position = currentPosition)
        startAutoScroll()
    }

    override fun beforeInAppMessageClose() {
        if (!configured) return
        stopAutoScroll()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (!configured) return
        when (visibility) {
            View.VISIBLE -> startAutoScroll()
            View.INVISIBLE, View.GONE -> stopAutoScroll()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (!configured) return
        stopAutoScroll()
    }

    // PageControl

    private fun updatePage(position: Int) {
        val index = imageIndex(position)
        val order = index + 1
        pageView.text = context.getString(R.string.hackle_iam_scroll_image_page_template, order, images.size)
    }

    private fun updateCurrentPage() {
        updatePage(position = currentPosition)
    }

    // AutoScroll

    private fun startAutoScroll() {
        stopAutoScroll()
        val interval = autoScroll?.intervalMillis ?: return
        autoScrollJob = InAppMessageUi.instance.scheduler.schedulePeriodically(interval, interval, MILLISECONDS) {
            runOnUiThread { scroll() }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    private fun scroll() {
        val nextPosition = currentPosition + 1
        recyclerView.smoothScrollToPosition(nextPosition)
    }

    inner class ScrollListener : RecyclerView.OnScrollListener() {
        private var state: Int = -1
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    updateCurrentPage()
                    impressionManager.impressionIfNeeded(position = currentPosition)
                    if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
                        startAutoScroll()
                    }
                    state = RecyclerView.SCROLL_STATE_IDLE
                }

                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    stopAutoScroll()
                    state = RecyclerView.SCROLL_STATE_DRAGGING
                }
            }
        }
    }

    inner class ScrollImageAdapter : RecyclerView.Adapter<ImageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.hackle_iam_image_item, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val index = imageIndex(position)
            val image = images[index]
            val imageView = holder.imageView
            imageView.configure(inAppMessageView, image)
            imageView.setOnClickListener(
                inAppMessageView.createImageClickListener(image, index + 1)
            )
        }

        override fun getItemCount(): Int {
            return INFINITE_COUNT
        }
    }

    inner class ImageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: InAppMessageImageView get() = view.findViewById(R.id.hackle_iam_image_item_image_view)
    }

    inner class ImpressionManager {
        private val impressions = ConcurrentHashMap<InAppMessage.Message.Image, Boolean>()

        fun impressionIfNeeded(position: Int) {
            val index = imageIndex(position)
            val image = images[index]
            val isFirstImpression = impressions.putIfAbsent(image, true) == null
            if (isFirstImpression) {
                inAppMessageView.handle(InAppMessageEvent.ImageImpression(image, index + 1))
            }
        }
    }

    companion object {
        private const val INFINITE_COUNT = Int.MAX_VALUE
        private val log = Logger<InAppMessageScrollImageView>()
    }
}
