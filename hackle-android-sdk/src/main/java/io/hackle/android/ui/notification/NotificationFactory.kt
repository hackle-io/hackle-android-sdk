package io.hackle.android.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_ID
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_NAME
import io.hackle.sdk.core.internal.log.Logger
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

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
        setColor(context, builder, data)
        setBigTextStyle(builder, data)
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
        // MARK:
        //  1. manifest
        //  2. app icon
        val smallIcon = getResourceIdFromManifest(context, Constants.DEFAULT_NOTIFICATION_SMALL_ICON) ?: metadata.icon

        builder.setSmallIcon(smallIcon)
    }

    // thumbnail icon == large icon
    private fun setThumbnailIcon(context: Context, builder: NotificationCompat.Builder, data: NotificationData) {
        val imageUrl = data.thumbnailImageUrl
        val largeIconId = getResourceIdFromManifest(context, Constants.DEFAULT_NOTIFICATION_LARGE_ICON)
        // MARK:
        //  1. remote
        //  2. manifest
        //  3. not set
        val image = loadImageFromUrl(context, imageUrl) ?: loadImageFromResource(context, largeIconId) ?: return

        builder.setLargeIcon(image)
    }

    private fun setColor(context: Context, builder: NotificationCompat.Builder, data: NotificationData) {
        val colorFilter = data.iconColorFilter
        val colorId = getResourceIdFromManifest(context, Constants.DEFAULT_NOTIFICATION_COLOR)
        // MARK:
        //  1. remote
        //  2. manifest
        //  3. not set
        val color = colorFilter?.toColorInt() ?: loadColorFromResource(context, colorId) ?: return

        builder.color = color
    }

    private fun setContentTitle(builder: NotificationCompat.Builder, data: NotificationData) {
        val title = data.title ?: return
        builder.setContentTitle(title)
    }

    private fun setContentText(builder: NotificationCompat.Builder, data: NotificationData) {
        val body = data.body ?: return
        builder.setContentText(body)
    }

    private fun setBigTextStyle(builder: NotificationCompat.Builder, data: NotificationData) {
        val body = data.body ?: return
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
    }

    private fun setContentIntent(
        context: Context,
        builder: NotificationCompat.Builder,
        extras: Bundle,
        data: NotificationData
    ) {
        val notificationIntent = Intent(context, NotificationTrampolineActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        data.link?.apply {
            notificationIntent.data = this.toUri()
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

    private fun setBigThumbnailIcon(
        context: Context,
        bigPictureStyle: NotificationCompat.BigPictureStyle,
        data: NotificationData
    ) {
        val imageUrl = data.thumbnailImageUrl ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        bigPictureStyle.bigLargeIcon(image)
    }

    private fun setBigPicture(
        context: Context,
        bigPictureStyle: NotificationCompat.BigPictureStyle,
        data: NotificationData
    ) {
        val imageUrl = data.largeImageUrl ?: return
        val image = loadImageFromUrl(context, imageUrl) ?: return
        bigPictureStyle.bigPicture(image)
    }

    private fun getResourceIdFromManifest(context: Context, key: String): Int? {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )

            val metaData = appInfo.metaData
            if (metaData != null && metaData.containsKey(key)) {
                val resourceId = metaData.getInt(key)
                if (resourceId != 0) {
                    return resourceId
                }
            }
        } catch (_: Exception) {
            log.debug { "Failed to get resource ID from manifest: $key" }
        }

        return null

    }

    private fun loadImageFromUrl(context: Context, url: String?): Bitmap? {
        try {
            if (url.isNullOrEmpty()) {
                return null
            }

            return Glide.with(context)
                .asBitmap()
                .load(url)
                .submit()
                .get()
        } catch (_: Exception) {
            log.debug { "Failed to load image: $url" }
        }
        return null
    }

    private fun loadImageFromResource(context: Context, resourceId: Int?): Bitmap? {
        try {
            if (resourceId == null || resourceId == 0) {
                return null
            }

            return Glide.with(context)
                .asBitmap()
                .load(resourceId)
                .submit()
                .get()
        } catch (_: Exception) {
            log.debug { "Failed to load image resource: $resourceId" }
        }
        return null
    }

    private fun loadColorFromResource(context: Context, resourceId: Int?): Int? {
        try {
            if (resourceId == null || resourceId == 0) {
                return null
            }

            return ContextCompat.getColor(context, resourceId)
        } catch (_: Exception) {
            log.debug { "Failed to load color resource: $resourceId" }
        }
        return null
    }
}
