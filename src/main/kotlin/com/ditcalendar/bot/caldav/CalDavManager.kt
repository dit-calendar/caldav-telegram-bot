package com.ditcalendar.bot.caldav

import com.ditcalendar.bot.config.caldav_base_url
import com.ditcalendar.bot.config.caldav_user_name
import com.ditcalendar.bot.config.caldav_user_password
import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.domain.data.NoEventsFound
import com.ditcalendar.bot.domain.data.NoSubcalendarFound
import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.methods.CalDAV4JMethodFactory
import com.github.caldav4j.util.GenerateQuery
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Url
import net.fortuna.ical4j.model.property.XProperty
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.jackrabbit.webdav.MultiStatus
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat

const val telegramUserCalDavProperty: String = "X-TELEGRAM-USER"

class CalDavManager {

    private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
    private val config by config()
    private val httpclient: HttpClient
    private val calDavBaseUri = config[caldav_base_url].removeSuffix("/")
    private val calDavUser = config[caldav_user_name]

    init {
        val calDavPassword = config[caldav_user_password]

        val provider: CredentialsProvider = BasicCredentialsProvider()
        val credentials = UsernamePasswordCredentials(calDavUser, calDavPassword)
        provider.setCredentials(AuthScope.ANY, credentials)

        httpclient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build()
    }

    fun findSubCalendarHref(subCalendarName: String): Result<String, Exception> {
        val factory = CalDAV4JMethodFactory()
        val propertyNameSet = DavPropertyNameSet()
        val propertyName = DavPropertyName.create(DavPropertyName.PROPERTY_DISPLAYNAME)
        propertyNameSet.add(propertyName)

        val method = factory.createPropFindMethod("$calDavBaseUri/calendars/$calDavUser", propertyNameSet, 1)

        val multiStatusResult = Result.of<MultiStatus, java.lang.Exception> {
            val response: HttpResponse = httpclient.execute(method)
            method.getResponseBodyAsMultiStatus(response)
        }
        if (multiStatusResult.component2() != null)
            return Result.error(Exception(multiStatusResult.component2()))

        val multiStatusResponses = multiStatusResult.component1()!!
        for (multiStatusResponse in multiStatusResponses.responses) {
            val davProperties = multiStatusResponse.getProperties(200)
            val displayName = davProperties[DavPropertyName.PROPERTY_DISPLAYNAME]

            if (displayName != null && displayName.value.toString() == subCalendarName)
                return Result.success(multiStatusResponse.href)
        }
        return Result.error(NoSubcalendarFound(subCalendarName))
    }

    fun findSubcalendarAndEvents(subCalendarName: String, startDate: String, endDate: String): Result<Calendar, Exception> {
        val start = DateTime(df.parse("${startDate}T00:00"))
        val end = DateTime(df.parse("${endDate}T04:00"))

        return findSubCalendarHref(subCalendarName).flatMap { getCalendarAndEvents(it, subCalendarName, start, end) }
    }

    private fun getCalendarAndEvents(href: String, subCalendarName: String, startDate: DateTime, endDate: DateTime): Result<Calendar, Exception> {
        val gq = GenerateQuery()
        gq.setFilterComponent(Component.VEVENT)
        gq.setTimeRange(startDate, endDate)
        gq.setFilterComponentProperties(listOf("STATUS!=CANCELLED"))
        val calendarQuery = gq.generate()
        val calClient = buildCalDAVCollection(href)
        val calendars = calClient.queryCalendars(httpclient, calendarQuery)

        return if (calendars.isEmpty()) {
            Result.error(NoEventsFound(subCalendarName))
        } else {
            val firstCalendar = calendars.first()
            firstCalendar.properties.add(Url(URI(href)))
            val cal = calendars.drop(1).fold(firstCalendar, ::aggregateCalendar)
            Result.success(cal)
        }
    }

    fun findEvent(href: String, eventUUID: String): Result<VEvent, Exception> {
        val gq = GenerateQuery()
        gq.setComponent(Component.VEVENT)
        val calClient = buildCalDAVCollection(href)
        val calendar = calClient.queryCalendar(httpclient, Component.VEVENT, eventUUID, null)

        return if (calendar == null)
            Result.error(NoSubcalendarFound(href))
        else
            Result.success(calendar.getComponent(Component.VEVENT) as VEvent)
    }

    fun updateEvent(href: String, event: VEvent, who: String): Result<VEvent, Exception> {
        val calClient = buildCalDAVCollection(href)
        event.properties.removeAll { it.name == telegramUserCalDavProperty }
        event.properties.add(XProperty(telegramUserCalDavProperty, who))
        return Result.of<Unit, Exception> { calClient.updateMasterEvent(httpclient, event, null) }.map { event }
    }

    private fun aggregateCalendar(l: Calendar, r: Calendar): Calendar {
        l.components.addAll(r.getComponents(Component.VEVENT))
        return l
    }

    private fun buildCalDAVCollection(href: String): CalDAVCollection {
        val caldavUrl = URI(calDavBaseUri)
        val baseUri = "${caldavUrl.scheme}://${caldavUrl.authority}"
        return CalDAVCollection("$baseUri$href")
    }
}