package com.ditcalendar.bot.caldav

import com.ditcalendar.bot.config.caldav_user_name
import com.ditcalendar.bot.config.caldav_user_password
import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.domain.data.NoSubcalendarFound
import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.methods.CalDAV4JMethodFactory
import com.github.caldav4j.util.GenerateQuery
import com.github.kittinunf.result.Result
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Date
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet

class CalDavManager {

    private val config by config()
    private val httpclient: HttpClient

    init {
        val calDavUser = config[caldav_user_name]
        val calDavPassword = config[caldav_user_password]

        val provider: CredentialsProvider = BasicCredentialsProvider()
        val credentials = UsernamePasswordCredentials(calDavUser, calDavPassword)
        provider.setCredentials(AuthScope.ANY, credentials)

        httpclient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build()
    }

    fun findSubcalendar(subCalendarName: String): Result<Calendar, Exception> {
        //System.setProperty("ical4j.unfolding.relaxed", "true")
        System.setProperty("ical4j.parsing.relaxed", "true")
        val uri = "http://localhost:8080/remote.php/dav/calendars/admin/"

        val factory = CalDAV4JMethodFactory()
        val propertyNameSet = DavPropertyNameSet()
        val propertyName = DavPropertyName.create(DavPropertyName.PROPERTY_DISPLAYNAME)
        propertyNameSet.add(propertyName)
        val method = factory.createPropFindMethod(uri, propertyNameSet, 1)

        val response: HttpResponse = httpclient.execute(method)
        val calendar = method.getResponseBodyAsMultiStatus(response)
        for (respons in calendar.responses) {
            val properties = respons.getProperties(200)
            val displayName = properties.get(DavPropertyName.DISPLAYNAME)
            if (displayName != null && displayName.value.toString() == subCalendarName) {
                return getCalendarAndEvents(respons.href, subCalendarName)
            }
        }
        return Result.error(NoSubcalendarFound(subCalendarName))
    }

    private fun getCalendarAndEvents(href: String, subCalendarName: String): Result<Calendar, Exception> {
        //System.setProperty("ical4j.parsing.relaxed", "true")
        val gq = GenerateQuery()
        gq.setComponent("VEVENT")
        gq.setTimeRange(Date("20200801T173752Z"), Date("20200910T173752Z"))
        val calendarQuery = gq.generate()
        val calClient = CalDAVCollection("http://localhost:8080$href")
        val calendars = calClient.queryCalendars(httpclient, calendarQuery)

        return if (calendars.isEmpty()) {
            Result.error(NoSubcalendarFound(subCalendarName))
        } else {
            Result.success(calendars.first())
        }
    }

    fun findSubcalendar(id: Int) {

    }

    fun getEvent(eventId: String) {

    }

    fun updateEvent() {

    }

    fun testConnection() {
        val uri = "http://localhost:8080/remote.php/dav/calendars/admin/personal/"

        val collection = CalDAVCollection(uri)
        val testConnection = collection.testConnection(httpclient)
    }
}