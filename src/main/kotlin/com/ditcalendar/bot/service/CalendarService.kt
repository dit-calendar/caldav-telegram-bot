package com.ditcalendar.bot.service

import com.ditcalendar.bot.caldav.CalDavManager
import com.ditcalendar.bot.caldav.addUserToWho
import com.ditcalendar.bot.caldav.removeUserFromWho
import com.ditcalendar.bot.caldav.telegramUserCalDavProperty
import com.ditcalendar.bot.domain.dao.find
import com.ditcalendar.bot.domain.dao.findOrCreate
import com.ditcalendar.bot.domain.data.*
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Url
import net.fortuna.ical4j.model.property.XProperty

class CalendarService(private val calDavManager: CalDavManager) {

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, chatId: Long, messageId: Int): Result<CalendarDTO, Exception> =
            calDavManager.findSubcalendarAndEvents(subCalendarName, startDate, endDate)
                    .map {
                        var href = it.getProperty<Url>("URL")
                        val postCalendarMetaInfo = findOrCreate(chatId, messageId, subCalendarName, startDate, endDate, href.value)
                        val tasks: List<VEvent> = it.components.getComponents("VEVENT")
                        val constructor = { task: VEvent, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                        CalendarDTO(subCalendarName, startDate, endDate, tasks.map { it.fillWithTelegramLinks(constructor) })
                    }

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo): Result<CalendarDTO, Exception> =
            calDavManager.findSubcalendarAndEvents(subCalendarName, startDate, endDate)
                    .map {
                        val tasks: List<VEvent> = it.components.getComponents("VEVENT")
                        val constructor = { task: VEvent, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                        CalendarDTO(subCalendarName, startDate, endDate, tasks.map { it.fillWithTelegramLinks(constructor) })
                    }

    fun assignUserToTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskForUnassignment, Exception> {
        val postCalendarMetaInfo = find(metaInfoId)
        return calDavManager
                .findEvent(postCalendarMetaInfo!!.uri, taskId)
                .flatMap { oldTask ->
                    var who = oldTask.getProperty<XProperty>(telegramUserCalDavProperty).value
                    who = addUserToWho(who, telegramLink.telegramUserId.toString())
                    calDavManager.updateEvent(postCalendarMetaInfo!!.uri, oldTask, who)
                }
                .map { it.fillWithTelegramLinks { task: VEvent, t: TelegramLinks -> TelegramTaskForUnassignment(task, t, metaInfoId) } }
    }

    fun unassignUserFromTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskAfterUnassignment, Exception> {
        val postCalendarMetaInfo = find(metaInfoId)
        return calDavManager
                .findEvent(postCalendarMetaInfo!!.uri, taskId)
                .flatMap { oldTask ->
                    var who = oldTask.getProperty<XProperty>(telegramUserCalDavProperty).value
                    who = removeUserFromWho(who, telegramLink.telegramUserId.toString())
                    calDavManager.updateEvent(postCalendarMetaInfo!!.uri, oldTask, who)
                }
                .map { it.fillWithTelegramLinks { task: VEvent, t: TelegramLinks -> TelegramTaskAfterUnassignment(task, t) } }
    }

    //    private fun SubCalendar.fillWithTasks(startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo) =
//            this.apply {
//                val tasksResulst = eventEndpoint.findEvents(this.id, startDate, endDate)
//                val constructor = { task: Event, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
//                tasksResulst.map {
//                    this.apply {
//                        this.startDate = startDate
//                        this.endDate = endDate
//                        this.tasks = it.events
//                                .map { it.fillWithTelegramLinks(constructor) }
//                    }
//                }
//            }
//
    private inline fun <TelTask : TelegramTaskAssignment> VEvent.fillWithTelegramLinks(
            constructor: (task: VEvent, t: TelegramLinks) -> TelTask): TelTask {
        val telegramLinks = find(listOf()) //TODO TelegramUser ID's missing
        return constructor(this, telegramLinks)
    }
}