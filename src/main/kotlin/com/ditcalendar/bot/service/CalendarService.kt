package com.ditcalendar.bot.service

import com.ditcalendar.bot.caldav.*
import com.ditcalendar.bot.domain.dao.find
import com.ditcalendar.bot.domain.dao.findOrCreate
import com.ditcalendar.bot.domain.data.*
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Url
import net.fortuna.ical4j.model.property.XProperty

class CalendarService(private val calDavManager: CalDavManager) {

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, chatId: Long, messageId: Int): Result<CalendarDTO, Exception> =
            calDavManager.findSubcalendarAndEvents(subCalendarName, startDate, endDate)
                    .map {
                        val href = it.getProperty<Url>(Property.URL)
                        val postCalendarMetaInfo = findOrCreate(chatId, messageId, subCalendarName, startDate, endDate, href.value)
                        val tasks: List<VEvent> = it.components.getComponents(Component.VEVENT)
                        val constructor = { task: VEvent, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                        CalendarDTO(subCalendarName, startDate, endDate, tasks.map { it.fillWithTelegramLinks(constructor) })
                    }

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo): Result<CalendarDTO, Exception> =
            calDavManager.findSubcalendarAndEvents(subCalendarName, startDate, endDate)
                    .map {
                        val tasks: List<VEvent> = it.components.getComponents(Component.VEVENT)
                        val constructor = { task: VEvent, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                        CalendarDTO(subCalendarName, startDate, endDate, tasks.map { it.fillWithTelegramLinks(constructor) })
                    }

    fun assignUserToTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskForUnassignment, Exception> {
        val postCalendarMetaInfo = find(metaInfoId)
        return if (postCalendarMetaInfo == null)
            Result.error(PostCalendarMetaInfoIsUnknownForAssignment())
        else calDavManager
                .findEvent(postCalendarMetaInfo.uri, taskId, postCalendarMetaInfo.startDate.replace("-", ""))
                .flatMap { oldTask ->
                    var who = oldTask.getTelegramUserCalDavProperty()
                    who = addUserToWho(who, telegramLink.telegramUserId.toString())
                    calDavManager.updateEvent(postCalendarMetaInfo.uri, oldTask, who, postCalendarMetaInfo.startDate)
                }
                .map { it.fillWithTelegramLinks { task: VEvent, t: TelegramLinks -> TelegramTaskForUnassignment(task, t, metaInfoId) } }
    }

    fun unassignUserFromTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskAfterUnassignment, Exception> {
        val postCalendarMetaInfo = find(metaInfoId)
        return if (postCalendarMetaInfo == null)
            Result.error(PostCalendarMetaInfoIsUnknownForUnassignment())
        else calDavManager
                .findEvent(postCalendarMetaInfo.uri, taskId, postCalendarMetaInfo.startDate.replace("-", ""))
                .flatMap { oldTask ->
                    var who = oldTask.getTelegramUserCalDavProperty()
                    who = removeUserFromWho(who, telegramLink.telegramUserId.toString())
                    calDavManager.updateEvent(postCalendarMetaInfo.uri, oldTask, who, postCalendarMetaInfo.startDate)
                }
                .map { it.fillWithTelegramLinks { task: VEvent, t: TelegramLinks -> TelegramTaskAfterUnassignment(task, t) } }
    }

    fun findSubCalendarHref(subCalendarName: String) = calDavManager.findSubCalendarHref(subCalendarName)

    private inline fun <TelTask : TelegramTaskAssignment> VEvent.fillWithTelegramLinks(
            constructor: (task: VEvent, t: TelegramLinks) -> TelTask): TelTask {
        val assignedId: String? = this.getTelegramUserCalDavProperty()
        val telegramLinks = find(parseWhoToIds(assignedId))
        return constructor(this, telegramLinks)
    }

    private fun VEvent.getTelegramUserCalDavProperty(): String? =
            this.getProperty<XProperty>(telegramUserCalDavProperty)?.run { this.value }
}