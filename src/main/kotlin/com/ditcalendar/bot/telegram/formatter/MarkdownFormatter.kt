package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.config.bot_name
import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.domain.data.*
import com.ditcalendar.bot.service.assignDeepLinkCommand
import com.ditcalendar.bot.teamup.data.Event
import com.ditcalendar.bot.teamup.data.SubCalendar
import java.text.SimpleDateFormat


private val config by config()

private val botName = config[bot_name]

fun TelegramTaskAssignment.toMarkdown(): String {
    val formattedDescription =
            if (task.notes != null && task.notes!!.isNotBlank())
                System.lineSeparator() + task.notes!!
                        .replace("<p>", "")
                        .replace("</p>", "")
                        .withMDEscape()
            else ""
    return when (this) {
        is TelegramTaskForAssignment ->
            "\uD83D\uDD51 *${task.formatTime()}* \\- ${task.title.withMDEscape()}" + formattedDescription + System.lineSeparator() +
                    "Who?: ${assignedUsers.toMarkdown()} [assign me](https://t.me/$botName?start=$assignDeepLinkCommand${task.id}_$postCalendarMetaInfoId)"

        is TelegramTaskForUnassignment ->
            "\uD83C\uDF89 *successfully assigned:*" + System.lineSeparator() +
                    task.formatDate() + System.lineSeparator() +
                    "*${task.formatTime()}* \\- ${task.title.withMDEscape()}$formattedDescription" + System.lineSeparator() +
                    "Who?: ${assignedUsers.toMarkdown()}"

        is TelegramTaskAfterUnassignment ->
            """
                *successfully removed from*:
                ${task.formatDate()}
                *${task.formatTime()}* \- ${task.title.withMDEscape()}
            """.trimIndent()
    }
}

private fun Event.formatTime(): String {
    val formatter = SimpleDateFormat("HH:mm")
    var timeString = formatter.format(this.startDate)
    timeString += " \\- " + formatter.format(this.endDate)
    return timeString
}

private fun Event.formatDate(): String = SimpleDateFormat("dd.MM.yyyy").format(this.startDate).withMDEscape()

@JvmName("toMarkdownForTelegramLinks")
private fun TelegramLinks.toMarkdown(): String {
    var firstNames = this.filter { it.firstName != null }.joinToString(", ") { it.firstName!!.withMDEscape() }
    val anonymousCount = this.count { it.firstName == null }
    firstNames += if (anonymousCount != 0) " \\+$anonymousCount" else ""
    return firstNames
}


fun TelegramTaskAssignments.toMarkdown(): String = System.lineSeparator() +
        if (this.isEmpty()) "no tasks found" else joinToString(separator = System.lineSeparator()) { it.toMarkdown() }

fun SubCalendar.toMarkdown(): String {
    return """
            *$name* \- ${startDate!!.withMDEscape()}${System.lineSeparator()}
        """.trimIndent() + tasks.toMarkdown()
}

fun String.withMDEscape() =
        this.replace("\"", "")
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("~", "\\~")
                .replace(">", "\\>")
                .replace("!", "\\!")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("_", "\\_")
                .replace(".", "\\.")
                .replace("*", "\\*")
                .replace("#", "\\#")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")