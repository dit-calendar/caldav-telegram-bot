package com.ditcalendar.bot.domain

import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.config.database_url
import com.ditcalendar.bot.config.heroku_app_name
import com.ditcalendar.bot.domain.dao.PostCalendarMetaInfoTable
import com.ditcalendar.bot.domain.dao.TelegramLinksTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

fun createDB() {
    val config by config()
    val databaseUrl = config[database_url]
    val herokuApp = config[heroku_app_name]

    val dbUri = URI(databaseUrl)
    val username = dbUri.userInfo.split(":")[0]
    val password = dbUri.userInfo.split(":")[1]
    var dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path

    if (herokuApp.isNotBlank()) //custom config logic needed because of config lib
        dbUrl += "?sslmode=require"

    Database.connect(dbUrl, driver = "org.postgresql.Driver",
            user = username, password = password)
    transaction { SchemaUtils.create(TelegramLinksTable, PostCalendarMetaInfoTable) }
}