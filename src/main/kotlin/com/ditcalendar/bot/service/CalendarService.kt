package com.ditcalendar.bot.service

import com.ditcalendar.bot.caldav.CalDavManager
import com.ditcalendar.bot.domain.dao.find
import com.ditcalendar.bot.domain.dao.findOrCreate
import com.ditcalendar.bot.domain.data.CalendarDTO
import com.ditcalendar.bot.domain.data.TelegramLinks
import com.ditcalendar.bot.domain.data.TelegramTaskAssignment
import com.ditcalendar.bot.domain.data.TelegramTaskForAssignment
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.ComponentList
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent

class CalendarService(private val calDavManager: CalDavManager) {

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, chatId: Long, messageId: Int): Result<CalendarDTO, Exception> =
            calDavManager.findSubcalendar(subCalendarName)
                    .map {
                        val postCalendarMetaInfo = findOrCreate(chatId, messageId, 0, startDate, endDate)
                        val tasks: List<VEvent> = it.components.getComponents("VEVENT")
                        val constructor = { task: VEvent, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                        CalendarDTO(subCalendarName, startDate, endDate, tasks.map { it.fillWithTelegramLinks(constructor) })
                    }

//    fun getCalendarAndTask(id: Int, startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo): Result<SubCalendar, Exception> =
//            calDavManager.findSubcalendar(id)
//                    .map { it.fillWithTasks(startDate, endDate, postCalendarMetaInfo) }

//    fun assignUserToTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskForUnassignment, Exception> =
//            calDavManager
//                    .getEvent(taskId)
//                    .flatMap { oldTask ->
//                        oldTask.apply { who = addUserToWho(who, telegramLink.telegramUserId.toString()) }
//                        eventEndpoint.updateEvent(oldTask)
//                                .map { it.fillWithTelegramLinks { task: Event, t: TelegramLinks -> TelegramTaskForUnassignment(task, t, metaInfoId) } }
//                    }
//
//    fun unassignUserFromTask(taskId: String, telegramLink: TelegramLink): Result<TelegramTaskAfterUnassignment, Exception> =
//            calDavManager
//                    .getEvent(taskId)
//                    .flatMap { task ->
//                        task.apply { who = removeUserFromWho(who, telegramLink.telegramUserId.toString()) }
//                        eventEndpoint.updateEvent(task)
//                                .map { it.fillWithTelegramLinks(::TelegramTaskAfterUnassignment) }
//                    }

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