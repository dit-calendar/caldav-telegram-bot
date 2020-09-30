package com.ditcalendar.bot

import com.ditcalendar.bot.caldav.CalDavManager
import com.ditcalendar.bot.config.*
import com.ditcalendar.bot.domain.createDB
import com.ditcalendar.bot.domain.dao.find
import com.ditcalendar.bot.domain.dao.findByMessageId
import com.ditcalendar.bot.domain.dao.updateMessageId
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.service.*
import com.ditcalendar.bot.telegram.service.*
import com.elbekD.bot.Bot
import com.elbekD.bot.server
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success

val helpMessage =
        """
            Commands which the bot accepts
            /postcalendar {Subcalendar name} {start date as yyyy-MM-dd} {optional end date as yyyy-MM-dd} = Post subcalendar in channel
            /help = show all bot commands
        """.trimIndent()

const val BOT_COMMAND_POST_CALENDAR = "/postcalendar"

fun main(args: Array<String>) {

    val config by config()
    val token = config[telegram_token]
    val herokuApp = config[heroku_app_name]

    createDB()

    val commandExecution = CommandExecution(CalendarService(CalDavManager()))

    val bot = if (config[webhook_is_enabled]) {
        Bot.createWebhook(config[bot_name], token) {
            url = "https://$herokuApp.herokuapp.com/$token"

            /*
            Jetty server is used to listen to incoming request from Telegram servers.
            */
            server {
                host = "0.0.0.0"
                port = config[server_port]
            }
        }
    } else Bot.createPolling(config[bot_name], token)

    fun postCalendarCommand(msg: Message, opts: String?) {
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {
            if (opts != null) {
                val response = commandExecution.executePublishCalendarCommand(opts.removePrefix(BOT_COMMAND_POST_CALENDAR), msg)
                response.success { bot.deleteMessage(msg.chat.id, msg.message_id) }
                val messageResponse = bot.commandResponse(response, msg.chat.id)
                messageResponse.thenApply { findByMessageId(msg.message_id)?.let { metaInfo -> updateMessageId(metaInfo, it.message_id) } }
            } else bot.sendMessage(msg.chat.id, helpMessage)
        }
    }

//    fun reloadOldMessage(optsAfterTaskId: String) {
//        val variables = optsAfterTaskId.split("_")
//        val metaInfoId = variables.getOrNull(0)?.toIntOrNull()
//        if (metaInfoId != null) {
//            val postCalendarMetaInfo = find(metaInfoId)
//            if (postCalendarMetaInfo != null) {
//                commandExecution.reloadCalendar(postCalendarMetaInfo)
//                        .success { bot.editOriginalCalendarMessage(it, postCalendarMetaInfo.chatId, postCalendarMetaInfo.messageId) }
//            }
//        }
//    }

    fun responseForDeeplinkAssignment(chatId: Long, opts: String) {
        if (opts.startsWith(assignDeepLinkCommand)) {
            val callbackOpts: String = opts.substringAfter(assignDeepLinkCommand)
            if (callbackOpts.isNotBlank()) {
                bot.deepLinkResponse(callbackOpts, chatId)
            } else {
                bot.commandResponse(Result.error(InvalidRequest()), chatId)
            }
        } else {
            bot.sendMessage(chatId, helpMessage)
        }
    }

    bot.onCallbackQuery { callbackQuery ->
        checkGlobalStateBeforeHandling(callbackQuery.id) {
            val request = callbackQuery.data
            val originallyMessage = callbackQuery.message

            if (request == null || originallyMessage == null) {
                bot.answerCallbackQuery(callbackQuery.id, wrongRequestResponse)
            } else {
                val msgUser = callbackQuery.from
                val response = commandExecution.executeCallback(originallyMessage.chat.id.toInt(), msgUser.id, msgUser.first_name, request, originallyMessage)

                bot.callbackResponse(response, callbackQuery, originallyMessage)
//                response.success {
//                    if (request.startsWith(assingWithNameCallbackCommand) || request.startsWith(assingAnnonCallbackCommand)
//                            || request.startsWith(unassignCallbackCommand)) {
//                        val optsAfterTaskId = request
//                                .removePrefix(assingWithNameCallbackCommand)
//                                .removePrefix(assingAnnonCallbackCommand)
//                                .removePrefix(unassignCallbackCommand)
//                                .substringAfter("_")
//
//                        reloadOldMessage(optsAfterTaskId)
//                    }
//                }
            }
        }
    }

    //for deeplinking
    bot.onCommand("/start") { msg, opts ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {

            bot.deleteMessage(msg.chat.id, msg.message_id)
            val msgUser = msg.from
            //if message user is not set, we can't process
            if (msgUser == null) {
                bot.sendMessage(msg.chat.id, wrongRequestResponse)
            } else {
                if (opts != null)
                    responseForDeeplinkAssignment(msg.chat.id, opts)
                else
                    bot.sendMessage(msg.chat.id, helpMessage)
            }
        }
    }

    bot.onCommand("/help") { msg, _ ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {
            bot.sendMessage(msg.chat.id, helpMessage)
        }
    }

    bot.onCommand(BOT_COMMAND_POST_CALENDAR) { msg, opts ->
        postCalendarCommand(msg, opts)
    }

    bot.onChannelPost { msg ->
        val msgText = msg.text
        if (msgText != null && msgText.startsWith(BOT_COMMAND_POST_CALENDAR))
            postCalendarCommand(msg, msgText.substringAfter(" "))
    }

    bot.start()
}
