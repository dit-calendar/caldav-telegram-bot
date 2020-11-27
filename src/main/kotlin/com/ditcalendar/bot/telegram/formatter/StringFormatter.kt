package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.domain.data.DitBotError
import org.apache.jackrabbit.webdav.DavException
import java.lang.IllegalArgumentException
import java.net.UnknownHostException

const val reloadButtonText = "reload"
const val calendarReloadCallbackNotification = "calendar was reloaded"

fun parseErrorToString(error: Exception): String =
        when (error) {
            is DitBotError -> error.message!!
            else -> when (error.cause) {
                is DavException -> parsingError
                is UnknownHostException -> "wrong calDav url"
                is IllegalArgumentException ->
                    if((error.cause as IllegalArgumentException).message == "DAV:multistatus element expected.")
                        parsingError
                    else
                        unknownError
                else -> unknownError
            }
        }.withMDEscape()

private const val parsingError = "parsing error for calendar. Please check the CalDav url or credentials"
private const val unknownError = "unknown error"