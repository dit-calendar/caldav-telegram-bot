package com.ditcalendar.bot.config

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import java.io.File


fun config(): Lazy<Configuration> {
    return lazy {
        systemProperties() overriding
                EnvironmentVariables() overriding
                ConfigurationProperties.fromOptionalFile(File ("src/main/resources/dev.properties")) overriding
                ConfigurationProperties.fromResource("config.properties")
    }
}

val database_url = Key("database.url", stringType)

val bot_name = Key("bot.name", stringType)
val webhook_is_enabled = Key("webhook.enabled", booleanType)

val server_port = Key("port", intType)
val telegram_token = Key("telegram.token", stringType)
val heroku_app_name = Key("heroku.app.name", stringType)

val caldav_base_url = Key("caldav.base.url", stringType)
val caldav_user_name = Key("caldav.user.name", stringType)
val caldav_user_password = Key("caldav.user.password", stringType)
