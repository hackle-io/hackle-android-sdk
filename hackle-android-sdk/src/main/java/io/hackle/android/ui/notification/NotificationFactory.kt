package io.hackle.android.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_NAME
import io.hackle.android.ui.notification.Constants.KEY_BODY
import io.hackle.android.ui.notification.Constants.KEY_COLOR_FILTER
import io.hackle.android.ui.notification.Constants.KEY_LARGE_IMAGE_URL
import io.hackle.android.ui.notification.Constants.KEY_LINK
import io.hackle.android.ui.notification.Constants.KEY_THUMBNAIL_IMAGE_URL
import io.hackle.android.ui.notification.Constants.KEY_TITLE
import io.hackle.sdk.core.internal.log.Logger

internal object NotificationFactory {

    private val log = Logger<NotificationFactory>()

    fun createNotification(context: Context, bundle: Bundle): Notification {
        val channelId = getDefaultNotificationChannelId(context)
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setAutoCancel(true)
        setPriority(builder)
        setVisibility(builder)
        setSmallIcon(context, builder, bundle)
        setThumbnailIcon(context, builder, bundle)
        setContentTitle(builder, bundle)
        setContentText(builder, bundle)
        setContentIntent(context, builder, bundle)
        setBigPictureStyle(context, builder, bundle)
        return builder.build()
    }

    private fun getDefaultNotificationChannelId(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(DEFAULT_NOTIFICATION_CHANNEL_ID) == null) {
                log.debug { "Default notification channel does not exist on device." }
                val channel = NotificationChannel(
                    DEFAULT_NOTIFICATION_CHANNEL_ID,
                    DEFAULT_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
                log.debug { "Created default notification channel name : $DEFAULT_NOTIFICATION_CHANNEL_NAME" }
            }
        }
        return DEFAULT_NOTIFICATION_CHANNEL_ID
    }

    private fun setPriority(builder: NotificationCompat.Builder) {
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
    }

    private fun setVisibility(builder: NotificationCompat.Builder) {
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun setSmallIcon(context: Context, builder: NotificationCompat.Builder, bundle: Bundle) {
        val metadata = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        builder.setSmallIcon(metadata.icon)

        if (bundle.containsKey(KEY_COLOR_FILTER)) {
            val colorFilterString = bundle.getString(KEY_COLOR_FILTER)
            builder.color = Color.parseColor(colorFilterString)
        }
    }

    private fun setThumbnailIcon(context: Context, builder: NotificationCompat.Builder, bundle: Bundle) {
        val imageUrl = bundle.getString(KEY_THUMBNAIL_IMAGE_URL) ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        builder.setLargeIcon(image)
    }

    private fun setContentTitle(builder: NotificationCompat.Builder, bundle: Bundle) {
        val title = bundle.getString(KEY_TITLE) ?: return
        builder.setContentTitle(title)
    }

    private fun setContentText(builder: NotificationCompat.Builder, bundle: Bundle) {
        val body = bundle.getString(KEY_BODY) ?: return
        builder.setContentText(body)
    }

    private fun setContentIntent(context: Context, builder: NotificationCompat.Builder, bundle: Bundle) {
        val notificationIntent = Intent(context, NotificationTrampolineActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        bundle.getString(KEY_LINK)?.apply {
            notificationIntent.data = Uri.parse(this)
        }
        notificationIntent.putExtras(bundle)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            notificationIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)
    }

    private fun setBigPictureStyle(context: Context, builder: NotificationCompat.Builder, bundle: Bundle) {
        if (!bundle.containsKey(KEY_LARGE_IMAGE_URL)) {
            return
        }

        val bigPictureStyle = NotificationCompat.BigPictureStyle()
        setBigThumbnailIcon(context, bigPictureStyle, bundle)
        setBigPicture(context, bigPictureStyle, bundle)
        builder.setStyle(bigPictureStyle)
    }

    private fun setBigThumbnailIcon(context: Context, bigPictureStyle: NotificationCompat.BigPictureStyle, bundle: Bundle) {
        val imageUrl = bundle.getString(KEY_THUMBNAIL_IMAGE_URL) ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        bigPictureStyle.bigLargeIcon(image)
    }

    private fun setBigPicture(context: Context, bigPictureStyle: NotificationCompat.BigPictureStyle, bundle: Bundle) {
        val imageUrl = bundle.getString(KEY_LARGE_IMAGE_URL) ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        bigPictureStyle.bigPicture(image)
    }

    private fun loadImageFromUrl(context: Context, url: String): Bitmap? {
        try {
            return Glide.with(context)
                .asBitmap()
                .load(url)
                .submit()
                .get()
        } catch (e: Exception) {
            log.debug { "Failed to load image : $url" }
        }
        return null
    }
}