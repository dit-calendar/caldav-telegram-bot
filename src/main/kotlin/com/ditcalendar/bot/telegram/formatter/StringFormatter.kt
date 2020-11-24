package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.domain.data.DitBotError
import org.apache.jackrabbit.webdav.DavException
import java.net.UnknownHostException

const val reloadButtonText = "reload"
const val calendarReloadCallbackNotification = "calendar was reloaded"

fun parseErrorToString(error: Exception): String =
        when (error) {
            is DitBotError -> error.message!!
            else -> when (error.cause) {
                is DavException -> "parsing error for calDav response. Please check your calDav url or server"
                is UnknownHostException -> "wrong calDav url"
                else -> "unknown error"
            }
        }.withMDEscape()