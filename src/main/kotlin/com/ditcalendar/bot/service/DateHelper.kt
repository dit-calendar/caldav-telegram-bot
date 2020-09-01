package com.ditcalendar.bot.service

import com.github.kittinunf.result.Result
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")

fun isDateInputValid(startDate: String, endDate: String?): Boolean {
    val checkDateInput = Result.of<Unit, Exception> {
        df.parse(startDate)
        if (endDate != null) df.parse(endDate)
    }
    return when (checkDateInput) {
        is Result.Failure -> false
        is Result.Success -> true
    }
}

fun nextDayAfterMidnight(startDate: String): String {
    val c = Calendar.getInstance()
    c.time = df.parse(startDate)
    c.add(Calendar.DATE, 1)
    return df.format(c.time)
}