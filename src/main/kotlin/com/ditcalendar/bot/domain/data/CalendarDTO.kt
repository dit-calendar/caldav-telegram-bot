package com.ditcalendar.bot.domain.data

data class CalendarDTO(val name: String,
                       val startDate: String,
                       val endDate: String,
                       var tasks: List<TelegramTaskForAssignment> = listOf()) : BaseDTO()