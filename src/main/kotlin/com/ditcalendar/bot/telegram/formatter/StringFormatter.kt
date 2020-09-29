package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.domain.data.DitBotError
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.domain.data.MultipleSubcalendarsFound
import com.ditcalendar.bot.domain.data.NoSubcalendarFound

const val reloadButtonText = "reload"
const val calendarReloadCallbackNotification = "calendar was reloaded"

fun parseErrorToString(error: Exception): String =
        when (error) {
//            is FuelError -> {
//                when (error.response.statusCode) {
//                    401 -> "Bot is missing necessary access rights"
//                    403 -> "Bot is missing necessary access rights"
//                    404 -> "calendar or task not found"
//                    503 -> "server not reachable, try again in a moment"
//                    else -> if (error.cause is JsonDecodingException) {
//                        "unexpected server response"
//                    } else if (error.message != null)
//                        "Error: " + error.message.toString()
//                    else "unkown Error"
//                }
//            }
            is DitBotError -> {
                when (error) {
                    is InvalidRequest -> error.message!!
                    is NoSubcalendarFound -> error.message!!
                    is MultipleSubcalendarsFound -> error.message!!
                }
            }
            else -> "unknown error"
        }.withMDEscape()