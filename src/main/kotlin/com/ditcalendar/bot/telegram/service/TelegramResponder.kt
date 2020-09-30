package com.ditcalendar.bot.telegram.service

import com.ditcalendar.bot.domain.data.CalendarDTO
import com.ditcalendar.bot.domain.data.TelegramTaskAfterUnassignment
import com.ditcalendar.bot.domain.data.TelegramTaskForUnassignment
import com.ditcalendar.bot.service.assingAnnonCallbackCommand
import com.ditcalendar.bot.service.assingWithNameCallbackCommand
import com.ditcalendar.bot.service.reloadCallbackCommand
import com.ditcalendar.bot.service.unassignCallbackCommand
import com.ditcalendar.bot.telegram.formatter.calendarReloadCallbackNotification
import com.ditcalendar.bot.telegram.formatter.parseErrorToString
import com.ditcalendar.bot.telegram.formatter.reloadButtonText
import com.ditcalendar.bot.telegram.formatter.toMarkdown
import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import java.util.concurrent.CompletableFuture

const val parseMode = "MarkdownV2"
const val wrongRequestResponse = "request invalid"

fun Bot.commandResponse(response: Result<CalendarDTO, Exception>, chatId: Long): CompletableFuture<Message> =
        when (response) {
            is Result.Success ->
                when (val responseObject = response.value) {
                    is CalendarDTO -> {
                        val inlineButton = InlineKeyboardButton(reloadButtonText, callback_data = "$reloadCallbackCommand${responseObject.name}_${responseObject.startDate}_${responseObject.endDate}")
                        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
                        sendMessage(chatId, responseObject.toMarkdown() + System.lineSeparator(), parseMode, true, markup = inlineKeyboardMarkup)
                    }
                    else ->
                        sendMessage(chatId, "internal server error", parseMode, true)
                }
            is Result.Failure ->
                sendMessage(chatId, parseErrorToString(response.error), parseMode, true)
        }

fun Bot.callbackResponse(response: Result<CalendarDTO, Exception>, callbackQuery: CallbackQuery, originallyMessage: Message) {
    when (response) {
        is Result.Success ->
            when (val responseObject = response.value) {
                is CalendarDTO -> {
                    val inlineButton = InlineKeyboardButton(reloadButtonText, callback_data = "$reloadCallbackCommand${responseObject.name}_${responseObject.startDate}_${responseObject.endDate}")
                    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown() + System.lineSeparator(),
                            parseMode = parseMode, disableWebPagePreview = true, markup = inlineKeyboardMarkup)

                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, calendarReloadCallbackNotification)
                }
//                is TelegramTaskForUnassignment -> {
//                    val inlineButton = InlineKeyboardButton("unassign me", callback_data = "$unassignCallbackCommand${responseObject.task.id}_${responseObject.postCalendarMetaInfoId}")
//                    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
//                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown(),
//                            parseMode = parseMode, disableWebPagePreview = true, markup = inlineKeyboardMarkup)
//
//                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, null)
//                }
//                is TelegramTaskAfterUnassignment -> {
//                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown(),
//                            parseMode = parseMode)
//                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, "successfully signed out")
//                }
                else ->
                    answerCallbackQuery(callbackQuery.id, "internal server error", alert = true)
            }
        is Result.Failure ->
            answerCallbackQuery(callbackQuery.id, parseErrorToString(response.error), alert = true)
    }
}

fun Bot.deepLinkResponse(callbackOpts: String, chatId: Long) {
    val assignMeButton = InlineKeyboardButton("With telegram name", callback_data = assingWithNameCallbackCommand + callbackOpts)
    val anonAssignMeButton = InlineKeyboardButton("anonymous", callback_data = assingAnnonCallbackCommand + callbackOpts)
    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(assignMeButton, anonAssignMeButton)))
    sendMessage(chatId, "Can I use your name?", parseMode, true, markup = inlineKeyboardMarkup)
}

//fun Bot.editOriginalCalendarMessage(calendar: CalendarDTO, chatId: Long, messageId: Int) {
//    val inlineButton = InlineKeyboardButton(reloadButtonText, callback_data = "$reloadCallbackCommand${calendar.name}_${calendar.startDate}_${calendar.endDate}")
//    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
//    editMessageText(chatId, messageId, text = calendar.toMarkdown(),
//            parseMode = parseMode, disableWebPagePreview = true, markup = inlineKeyboardMarkup)
//}

private fun CompletableFuture<Message>.handleCallbackQuery(bot: Bot, calbackQueryId: String, callbackNotificationText: String?) {
    this.handle { _, throwable ->
        if (throwable == null || throwable.message!!.contains("Bad Request: message is not modified"))
            if (callbackNotificationText != null)
                bot.answerCallbackQuery(calbackQueryId, callbackNotificationText)
    }
}