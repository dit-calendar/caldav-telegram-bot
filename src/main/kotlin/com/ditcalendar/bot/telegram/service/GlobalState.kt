package com.ditcalendar.bot.telegram.service

/**
 * check if telegram message is received twice
 */
inline fun checkGlobalStateBeforeHandling(msgId: String, requestHandling: () -> Unit) {
    if (globalStateForFirstMessage == null || globalStateForFirstMessage != msgId) {
        globalStateForFirstMessage = msgId
        requestHandling()
    }
}

var globalStateForFirstMessage: String? = null