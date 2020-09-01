package com.ditcalendar.bot.domain.data

import com.ditcalendar.bot.domain.dao.PostCalendarMetaInfoTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PostCalendarMetaInfo(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PostCalendarMetaInfo>(PostCalendarMetaInfoTable)

    var chatId: Long by PostCalendarMetaInfoTable.chatId
    var messageId: Int by PostCalendarMetaInfoTable.messageId
    var subCalendarId: Int by PostCalendarMetaInfoTable.subCalendarId
    var startDate: String by PostCalendarMetaInfoTable.startDate
    var endDate: String by PostCalendarMetaInfoTable.endDate
}