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
import io.hackle.sdk.core.internal.log.Logger

internal object NotificationFactory {

    private val log = Logger<NotificationFactory>()

    fun createNotification(context: Context, extras: Bundle, data: NotificationData): Notification {
        val channelId = getDefaultNotificationChannelId(context)
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setAutoCancel(true)
        setPriority(builder)
        setVisibility(builder)
        setSmallIcon(context, builder, data)
        setThumbnailIcon(context, builder, data)
        setContentTitle(builder, data)
        setContentText(builder, data)
        setStyle(builder, data)
        setContentIntent(context, builder, extras, data)
        setBigPictureStyle(context, builder, data)
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
                log.debug { "Created default notification channel name: $DEFAULT_NOTIFICATION_CHANNEL_NAME" }
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

    private fun setSmallIcon(context: Context, builder: NotificationCompat.Builder, data: NotificationData) {
        val metadata = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        builder.setSmallIcon(metadata.icon)

        if (!data.iconColorFilter.isNullOrEmpty()) {
            try {
                builder.color = Color.parseColor(data.iconColorFilter)
            } catch (_: Exception) {
                log.debug { "Hex color parsing error: ${data.iconColorFilter}" }
            }
        }
    }

    private fun setThumbnailIcon(context: Context, builder: NotificationCompat.Builder, data: NotificationData) {
        val imageUrl = data.thumbnailImageUrl ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        builder.setLargeIcon(image)
    }

    private fun setContentTitle(builder: NotificationCompat.Builder, data: NotificationData) {
        val title = data.title ?: return
        builder.setContentTitle(title)
    }

    private fun setContentText(builder: NotificationCompat.Builder, data: NotificationData) {
        val body = data.body ?: return
        builder.setContentText(body)
    }

    private fun setStyle(builder: NotificationCompat.Builder, data: NotificationData) {
        val body = data.body ?: return
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
    }

    private fun setContentIntent(context: Context, builder: NotificationCompat.Builder, extras: Bundle, data: NotificationData) {
        val notificationIntent = Intent(context, NotificationTrampolineActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        data.link?.apply {
            notificationIntent.data = Uri.parse(this)
        }
        notificationIntent.putExtras(extras)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            notificationIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)
    }

    private fun setBigPictureStyle(context: Context, builder: NotificationCompat.Builder, data: NotificationData) {
        if (data.largeImageUrl.isNullOrEmpty()) {
            return
        }

        val bigPictureStyle = NotificationCompat.BigPictureStyle()
        setBigThumbnailIcon(context, bigPictureStyle, data)
        setBigPicture(context, bigPictureStyle, data)
        builder.setStyle(bigPictureStyle)
    }

    private fun setBigThumbnailIcon(context: Context, bigPictureStyle: NotificationCompat.BigPictureStyle, data: NotificationData) {
        val imageUrl = data.thumbnailImageUrl ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        bigPictureStyle.bigLargeIcon(image)
    }

    private fun setBigPicture(context: Context, bigPictureStyle: NotificationCompat.BigPictureStyle, data: NotificationData) {
        val imageUrl = data.largeImageUrl ?: return
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
            log.debug { "Failed to load image: $url" }
        }
        return null
    }
}