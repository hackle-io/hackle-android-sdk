package io.hackle.android.internal.inappmessage.schedule

internal interface InAppMessageScheduleListener {
    fun onSchedule(request: InAppMessageScheduleRequest)
}
