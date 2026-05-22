package com.raed.app.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

fun String.toTimeAgo(): String = try {
    val instant = Instant.parse(this)
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(instant, now)
    when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        minutes < 1440 -> "منذ ${minutes / 60} ساعة"
        minutes < 43200 -> "منذ ${minutes / 1440} يوم"
        else -> "منذ ${minutes / 43200} شهر"
    }
} catch (e: Exception) {
    "—"
}
