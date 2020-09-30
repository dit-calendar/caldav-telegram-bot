package com.ditcalendar.bot.service

import com.ditcalendar.bot.domain.dao.findOrCreate
import com.ditcalendar.bot.domain.dao.updateName
import com.ditcalendar.bot.domain.data.CalendarDTO
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.domain.data.PostCalendarMetaInfo
import com.ditcalendar.bot.domain.data.TelegramLink
import com.ditcalendar.bot.helpMessage
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import net.fortuna.ical4j.model.Calendar

const val assignDeepLinkCommand = "assign_"
const val unassignCallbackCommand = "unassign_"
const val reloadCallbackCommand = "reloadCalendar_"
const val assingWithNameCallbackCommand = "assignme_"
const val assingAnnonCallbackCommand = "assignmeAnnon_"

class CommandExecution(private val calendarService: CalendarService) {

    fun executeCallback(chatId: Int, msgUserId: Int, msgUserFirstName: String, callbaBackData: String, msg: Message): Result<CalendarDTO, Exception> =
            if (callbaBackData.startsWith(unassignCallbackCommand)) {
//                val taskId: String = callbaBackData.substringAfter(unassignCallbackCommand).substringBefore("_")
//                if (taskId.isNotBlank()) {
//                    // if user not existing, the DB of Bot was maybe dropped
//                    val telegramLink = findOrCreate(chatId, msgUserId)
//                    calendarService.unassignUserFromTask(taskId, telegramLink)
//                } else
                    Result.error(InvalidRequest())
            } else if (callbaBackData.startsWith(reloadCallbackCommand)) {
                reloadCalendar(callbaBackData.substringAfter(reloadCallbackCommand), msg.chat.id, msg.message_id)
            } else if (callbaBackData.startsWith(assingWithNameCallbackCommand)) {
//                var telegramLink = findOrCreate(chatId, msgUserId)
//                telegramLink = updateName(telegramLink, msgUserFirstName)
//                executeTaskAssignmentCommand(telegramLink, callbaBackData)
                Result.error(InvalidRequest())
            } else if (callbaBackData.startsWith(assingAnnonCallbackCommand)) {
//                var telegramLink = findOrCreate(chatId, msgUserId)
//                telegramLink = updateName(telegramLink, null)
//                executeTaskAssignmentCommand(telegramLink, callbaBackData)
                Result.error(InvalidRequest())
            } else
                Result.error(InvalidRequest())
//
//    private fun executeTaskAssignmentCommand(telegramLink: TelegramLink, opts: String): Result<TelegramTaskForUnassignment, Exception> {
//        val variables = opts.substringAfter("_").split("_")
//        val taskId = variables.getOrNull(0)
//        val metaInfoId = variables.getOrNull(1)?.toInt()
//        return if (taskId != null && taskId.isNotBlank() && metaInfoId != null)
//            calendarService.assignUserToTask(taskId, telegramLink, metaInfoId)
//        else
//            Result.error(InvalidRequest())
//    }

    fun executePublishCalendarCommand(opts: String, msg: Message): Result<CalendarDTO, Exception> {
        val variables = opts.split(" ")
        val subCalendarName = variables.getOrNull(0)
        val startDate = variables.getOrNull(1)
        var endDate = variables.getOrNull(2)

        return if (subCalendarName != null && subCalendarName.isNotBlank() && startDate != null) {

            if (isDateInputValid(startDate, endDate)) {
                if (endDate == null)
                    endDate = nextDayAfterMidnight(startDate)

                calendarService.getCalendarAndTask(subCalendarName, startDate, endDate, msg.chat.id, msg.message_id)
            } else
                Result.error(InvalidRequest("Dateformat sholud be yyyy-MM-dd e.g. 2015-12-31"))

        } else Result.error(InvalidRequest(helpMessage))
    }

    private fun reloadCalendar(opts: String, chatId: Long, messageId: Int): Result<CalendarDTO, Exception> {
        val variables = opts.split("_")
        val subCalendarName = variables.getOrNull(0)
        val startDate = variables.getOrNull(1)
        val endDate = variables.getOrNull(2)

        return if (subCalendarName != null && startDate != null && endDate != null) {
            val postCalendarMetaInfo = findOrCreate(chatId, messageId, subCalendarName, startDate, endDate)
            calendarService.getCalendarAndTask(subCalendarName, startDate, endDate, postCalendarMetaInfo)
        } else Result.error(InvalidRequest())
    }

//    fun reloadCalendar(postCalendarMetaInfo: PostCalendarMetaInfo?): Result<CalendarDTO, Exception> {
//        return if (postCalendarMetaInfo != null)
//            calendarService.getCalendarAndTask(postCalendarMetaInfo.subCalendarName, postCalendarMetaInfo.startDate, postCalendarMetaInfo.endDate, postCalendarMetaInfo)
//        else Result.error(InvalidRequest())
//    }
}
