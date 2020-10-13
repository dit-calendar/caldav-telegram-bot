package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.config.bot_name
import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.domain.data.*
import com.ditcalendar.bot.service.assignDeepLinkCommand
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import java.text.SimpleDateFormat


private val config by config()

private val botName = config[bot_name]

fun TelegramTaskAssignment.toMarkdown(): String {
    val formattedDescription =
            if (task.description != null && task.description.value!!.isNotBlank())
                System.lineSeparator() + task.description.value!!
                        .withMDEscape()
            else ""
    return when (this) {
        is TelegramTaskForAssignment ->
            "\uD83D\uDD51 *${task.formatTime()}* \\- ${task.summary.value.withMDEscape()}" + formattedDescription + System.lineSeparator() +
                    "Who?: ${assignedUsers.toMarkdown()} [assign me](https://t.me/$botName?start=$assignDeepLinkCommand${task.uid.value}_$postCalendarMetaInfoId)"

        is TelegramTaskForUnassignment ->
            "\uD83C\uDF89 *successfully assigned:*" + System.lineSeparator() +
                    task.formatDate() + System.lineSeparator() +
                    "*${task.formatTime()}* \\- ${task.summary.value.withMDEscape()}$formattedDescription" + System.lineSeparator() +
                    "Who?: ${assignedUsers.toMarkdown()}"

        is TelegramTaskAfterUnassignment ->
            """
                *successfully removed from*:
                ${task.formatDate()}
                *${task.formatTime()}* \- ${task.summary.value.withMDEscape()}
            """.trimIndent()
    }
}

private fun VEvent.formatTime(): String {
    val formatter = SimpleDateFormat("HH:mm")
    var timeString = formatter.format(this.startDate.date)
    timeString += " \\- " + formatter.format(this.endDate.date)
    return timeString
}

private fun VEvent.formatDate(): String = SimpleDateFormat("dd.MM.yyyy").format(this.startDate.date).withMDEscape()

@JvmName("toMarkdownForTelegramLinks")
private fun TelegramLinks.toMarkdown(): String {
    var firstNames = this.filter { it.firstName != null }.joinToString(", ") { it.firstName!!.withMDEscape() }
    val anonymousCount = this.count { it.firstName == null }
    firstNames += if (anonymousCount != 0) " \\+$anonymousCount" else ""
    return firstNames
}


fun TelegramTaskAssignments.toMarkdown(): String = System.lineSeparator() +
        if (this.isEmpty()) "no tasks found" else joinToString(separator = System.lineSeparator()) { it.toMarkdown() }

fun CalendarDTO.toMarkdown(): String {
    return """
            *$name* \- ${startDate.withMDEscape()}${System.lineSeparator()}
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