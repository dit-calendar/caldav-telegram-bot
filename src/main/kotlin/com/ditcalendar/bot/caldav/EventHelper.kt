package com.ditcalendar.bot.caldav

fun removeUserFromWho(oldWho: String?, telegramLinkUserId: String): String =
        when {
            oldWho.isNullOrBlank() -> ""

            telegramLinkUserId in oldWho ->
                oldWho.replace(telegramLinkUserId, "").replace(";;", ";")

            else -> ""
        }

fun addUserToWho(oldWho: String?, telegramLinkUserId: String): String =
        when {
            oldWho.isNullOrBlank() -> telegramLinkUserId
            telegramLinkUserId in oldWho -> oldWho
            else -> "$telegramLinkUserId;$oldWho"
        }

fun parseWhoToIds(who: String?): List<Int> =
        if (who.isNullOrBlank()) listOf()
        else who.split(";")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }