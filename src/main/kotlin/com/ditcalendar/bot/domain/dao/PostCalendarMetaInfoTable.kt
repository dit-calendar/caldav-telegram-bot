package com.ditcalendar.bot.domain.dao

import com.ditcalendar.bot.domain.data.PostCalendarMetaInfo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object PostCalendarMetaInfoTable : IntIdTable() {
    val chatId = long("chatId")
    var messageId = integer("messageId").uniqueIndex()
    val subCalendarName = varchar("subCalendarName", 50)
    val startDate = varchar("startDate", 50)
    val endDate = varchar("endDate", 50)
    val uri = varchar("uri", 100)
}

fun findOrCreate(newChatId: Long, msgUserId: Int, subCalendar: String, start: String, end: String, href: String): PostCalendarMetaInfo = transaction {
    val result = PostCalendarMetaInfo.find { PostCalendarMetaInfoTable.messageId eq msgUserId }
    if (result.count() == 0L) {
        create(newChatId, msgUserId, subCalendar, start, end, href)
    } else result.elementAt(0)
}

fun create(newChatId: Long, msgUserId: Int, subCalendar: String, start: String, end: String, href: String): PostCalendarMetaInfo = transaction {
    PostCalendarMetaInfo.new {
        chatId = newChatId
        messageId = msgUserId
        subCalendarName = subCalendar
        startDate = start
        endDate = end
        uri = href
    }
}

fun findByMessageId(id: Int): PostCalendarMetaInfo? = transaction {
    PostCalendarMetaInfo.find { PostCalendarMetaInfoTable.messageId eq id }.firstOrNull()
}

fun find(id: Int): PostCalendarMetaInfo? = transaction {
    PostCalendarMetaInfo.findById(id)
}

fun updateMessageId(metaInfo: PostCalendarMetaInfo, newMessageUserId: Int) = transaction {
    PostCalendarMetaInfoTable.update({ PostCalendarMetaInfoTable.id eq metaInfo.id }) {
        it[messageId] = newMessageUserId
    }
    metaInfo.also { it.messageId = newMessageUserId }
}