package com.ditcalendar.bot.service

import com.ditcalendar.bot.caldav.CalDavManager
import com.ditcalendar.bot.domain.dao.findOrCreate
import com.ditcalendar.bot.domain.data.*
import com.ditcalendar.bot.teamup.addUserToWho
import com.ditcalendar.bot.teamup.data.Event
import com.ditcalendar.bot.teamup.data.SubCalendar
import com.ditcalendar.bot.teamup.endpoint.CalendarEndpoint
import com.ditcalendar.bot.teamup.endpoint.EventEndpoint
import com.ditcalendar.bot.teamup.parseWhoToIds
import com.ditcalendar.bot.teamup.removeUserFromWho
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map

class CalendarService(private val calDavManager: CalDavManager) {

    fun getCalendarAndTask(subCalendarName: String, startDate: String, endDate: String, chatId: Long, messageId: Int): Result<SubCalendar, Exception> =
            calDavManager.findSubcalendar(subCalendarName)
                    .map {
                        val postCalendarMetaInfo = findOrCreate(chatId, messageId, it.id, startDate, endDate)
                        it.fillWithTasks(startDate, endDate, postCalendarMetaInfo)
                    }

    fun getCalendarAndTask(id: Int, startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo): Result<SubCalendar, Exception> =
            calDavManager.findSubcalendar(id)
                    .map { it.fillWithTasks(startDate, endDate, postCalendarMetaInfo) }

    fun assignUserToTask(taskId: String, telegramLink: TelegramLink, metaInfoId: Int): Result<TelegramTaskForUnassignment, Exception> =
            calDavManager
                    .getEvent(taskId)
                    .flatMap { oldTask ->
                        oldTask.apply { who = addUserToWho(who, telegramLink.telegramUserId.toString()) }
                        eventEndpoint.updateEvent(oldTask)
                                .map { it.fillWithTelegramLinks { task: Event, t: TelegramLinks -> TelegramTaskForUnassignment(task, t, metaInfoId) } }
                    }

    fun unassignUserFromTask(taskId: String, telegramLink: TelegramLink): Result<TelegramTaskAfterUnassignment, Exception> =
            calDavManager
                    .getEvent(taskId)
                    .flatMap { task ->
                        task.apply { who = removeUserFromWho(who, telegramLink.telegramUserId.toString()) }
                        eventEndpoint.updateEvent(task)
                                .map { it.fillWithTelegramLinks(::TelegramTaskAfterUnassignment) }
                    }

    private fun SubCalendar.fillWithTasks(startDate: String, endDate: String, postCalendarMetaInfo: PostCalendarMetaInfo) =
            this.apply {
                val tasksResulst = eventEndpoint.findEvents(this.id, startDate, endDate)
                val constructor = { task: Event, t: TelegramLinks -> TelegramTaskForAssignment(task, t, postCalendarMetaInfo.id.value) }
                tasksResulst.map {
                    this.apply {
                        this.startDate = startDate
                        this.endDate = endDate
                        this.tasks = it.events
                                .map { it.fillWithTelegramLinks(constructor) }
                    }
                }
            }

    private inline fun <TelTask : TelegramTaskAssignment> Event.fillWithTelegramLinks(
            constructor: (task: Event, t: TelegramLinks) -> TelTask): TelTask {
        val telegramLinks = find(parseWhoToIds(this.who))
        return constructor(this, telegramLinks)
    }
}