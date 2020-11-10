package com.ditcalendar.bot.domain.data

import com.ditcalendar.bot.telegram.service.wrongRequestResponse

sealed class DitBotError(description: String) : RuntimeException(description)

class InvalidRequest(errorMessage: String?) : DitBotError(errorMessage ?: wrongRequestResponse) {
    constructor() : this(null)
}

class NoSubcalendarFound(name: String) : DitBotError("no subcalendar found with name $name")
class NoEventsFound(name: String) : DitBotError("no events in subcalendar with name $name found")
class PostCalendarMetaInfoIsUnknownForUnassignment : DitBotError("unexpected error, try to assign and unassign again")
class PostCalendarMetaInfoIsUnknownForAssignment : DitBotError("unexpected error, try to reload the calendar in original post")
class MultipleSubcalendarsFound : DitBotError("found more than one subcalendar")
