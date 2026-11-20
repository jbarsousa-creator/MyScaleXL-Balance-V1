package com.example.myapplication.util

import java.util.Calendar

object DateUtils {
    fun startOfDay(timestamp: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun addDays(timestamp: Long, days: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.timeInMillis
    }
}
