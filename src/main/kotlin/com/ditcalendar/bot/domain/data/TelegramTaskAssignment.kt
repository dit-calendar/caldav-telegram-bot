package com.ditcalendar.bot.domain.data

import com.ditcalendar.bot.teamup.data.Event
import com.ditcalendar.bot.teamup.data.core.Base

typealias TelegramTaskAssignments = List<TelegramTaskAssignment>


sealed class TelegramTaskAssignment(val task: Event, val assignedUsers: TelegramLinks) : Base()

class TelegramTaskForAssignment(t: Event, tl: TelegramLinks, val postCalendarMetaInfoId: Int) : TelegramTaskAssignment(t, tl)
class TelegramTaskForUnassignment(t: Event, tl: TelegramLinks, val postCalendarMetaInfoId: Int) : TelegramTaskAssignment(t, tl)
class TelegramTaskAfterUnassignment(t: Event, tl: TelegramLinks) : TelegramTaskAssignment(t, tl)