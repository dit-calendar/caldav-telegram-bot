package com.ditcalendar.bot.domain.data

import com.ditcalendar.bot.telegram.service.wrongRequestResponse

sealed class DitBotError(description: String) : RuntimeException(description)

class InvalidRequest(errorMessage: String?) : DitBotError(errorMessage ?: wrongRequestResponse) {
    constructor() : this(null)
}

class NoSubcalendarFound(name: String) : DitBotError("no subcalendar found with name $name")
class MultipleSubcalendarsFound : DitBotError("found more than one subcalendar")
