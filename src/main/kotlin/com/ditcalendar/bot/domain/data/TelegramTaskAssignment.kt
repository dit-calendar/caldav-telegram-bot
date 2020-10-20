package com.ditcalendar.bot.domain.data

import net.fortuna.ical4j.model.component.VEvent

typealias TelegramTaskAssignments = List<TelegramTaskAssignment>


sealed class TelegramTaskAssignment(val task: VEvent, val assignedUsers: TelegramLinks) : BaseDTO()

class TelegramTaskForAssignment(t: VEvent, tl: TelegramLinks, val postCalendarMetaInfoId: Int) : TelegramTaskAssignment(t, tl)
class TelegramTaskForUnassignment(t: VEvent, tl: TelegramLinks, val postCalendarMetaInfoId: Int) : TelegramTaskAssignment(t, tl)
class TelegramTaskAfterUnassignment(t: VEvent, tl: TelegramLinks) : TelegramTaskAssignment(t, tl)