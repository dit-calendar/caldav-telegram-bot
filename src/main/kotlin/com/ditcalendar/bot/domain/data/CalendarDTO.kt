package com.ditcalendar.bot.domain.data

import java.util.*

data class CalendarDTO(val name: String,
                       val startDate: String,
                       val endDate: String,
                       var tasks: List<TelegramTaskForAssignment> = listOf())