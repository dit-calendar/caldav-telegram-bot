package com.ditcalendar.bot.domain.data

import com.ditcalendar.bot.domain.dao.TelegramLinksTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID


typealias TelegramLinks = List<TelegramLink>

class TelegramLink(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TelegramLink>(TelegramLinksTable)

    var chatId: Int by TelegramLinksTable.chatId
    var telegramUserId: Int by TelegramLinksTable.telegramUserId
    var firstName: String? by TelegramLinksTable.firstName
}