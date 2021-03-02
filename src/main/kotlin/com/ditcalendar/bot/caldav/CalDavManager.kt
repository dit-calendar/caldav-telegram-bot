package com.ditcalendar.bot.caldav

import com.ditcalendar.bot.config.caldav_base_url
import com.ditcalendar.bot.config.caldav_user_name
import com.ditcalendar.bot.config.caldav_user_password
import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.domain.data.NoEventsFound
import com.ditcalendar.bot.domain.data.NoSubcalendarFound
import com.github.caldav4j.methods.CalDAV4JMethodFactory
import com.github.caldav4j.util.GenerateQuery
import com.github.caldav4j.util.ICalendarUtils
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
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

        return findSubCalendarHref(subCalendarName).flatMap { getCalendarAndEvents(it, subCalendarName, startDate, start, end) }
    }

    private fun getCalendarAndEvents(href: String, subCalendarName: String, start: String, startDate: DateTime, endDate: DateTime): Result<Calendar, Exception> {
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
            val eventsToBeRemoved = collectRecurrentEventsToBeRemoved(cal, start.replace("-", ""))
            cal.components.removeAll(eventsToBeRemoved)
            Result.success(cal)
        }
    }

    private fun collectRecurrentEventsToBeRemoved(cal: Calendar, searchedStartDate: String): List<VEvent> {
        return cal.getComponents<VEvent>(Component.VEVENT).flatMap { collectRecurrentEventsToBeRemoved(cal, searchedStartDate, it.uid) }
    }

    private fun collectRecurrentEventsToBeRemoved(cal: Calendar, searchedStartDate: String, uuid: Uid): List<VEvent> {
        val eventsWIthDuplicatedUUID = cal.getComponents<VEvent>(Component.VEVENT).filter { event -> event.uid == uuid }
        if (eventsWIthDuplicatedUUID.size > 1) {
            val eventsToRemove = eventsWIthDuplicatedUUID
                    .filter { it.recurrenceId == null || !it.startDate.value.startsWith(searchedStartDate) }
            if (eventsToRemove.size == eventsWIthDuplicatedUUID.size) {
                return eventsToRemove.filter{!ICalendarUtils.hasProperty(it, Property.RRULE)}
            }
            return eventsToRemove
        }
        return listOf()
    }

    fun findEvent(href: String, eventUUID: String, searchedStartDate: String): Result<VEvent, Exception> {
        val gq = GenerateQuery()
        gq.setComponent(Component.VEVENT)
        val calClient = buildCalDAVCollection(href)
        val calendar = calClient.queryCalendar(httpclient, Component.VEVENT, eventUUID, null)

        return if (calendar == null)
            Result.error(NoSubcalendarFound(href))
        else {
            val events = calendar.getComponents<VEvent>(Component.VEVENT)
                    .filter { ICalendarUtils.hasProperty(it, Property.RRULE) || it.startDate.value.startsWith(searchedStartDate) }
            if (events.size > 1) {
                Result.success(events.first { it.recurrenceId != null && it.startDate.value.startsWith(searchedStartDate) })
            } else
                Result.success(calendar.getComponent(Component.VEVENT) as VEvent)
        }
    }

    fun updateEvent(href: String, event: VEvent, who: String, startDate: String): Result<VEvent, Exception> {
        val calClient = buildCalDAVCollection(href)
        event.properties.removeAll { it.name == telegramUserCalDavProperty }
        event.properties.add(XProperty(telegramUserCalDavProperty, who))
        val eventResult = Result.of<Unit, Exception> {
            // Event without scheduling
            if (!ICalendarUtils.hasProperty(event, Property.RRULE) && !ICalendarUtils.hasProperty(event, Property.RECURRENCE_ID))
                calClient.updateMasterEvent(httpclient, event, null)
            // Update existing recurrent event
            else if (ICalendarUtils.hasProperty(event, Property.RECURRENCE_ID))
                calClient.updateOriginalEvent(httpclient, event, null)
            // Create new recurrent Event
            else {
                updateScheduledEventProperties(event, startDate)
                calClient.addRecurrentEvent(httpclient, event, null)
            }
        }
        return eventResult.map { event }

    }

    /**
     * Parent of an recurrent Event, which we can recognize by RRULE, have to be created with same UUID as parent,
     * but with another DTStart and DTEnd.
     * https://icalevents.com/4437-correct-handling-of-uid-recurrence-id-sequence/
     */
    private fun updateScheduledEventProperties(event: VEvent, startDate: String) {
        event.properties.remove(event.created)
        event.properties.remove(event.dateStamp)
        event.properties.add(Created())
        event.properties.add(DtStamp())

        event.properties.remove(event.getProperty(Property.RRULE))

        event.properties.remove(event.getProperty(Property.SEQUENCE))
        event.properties.add(Sequence())

        val dtStart = event.getProperty<DtStart>(Property.DTSTART)
        val newDtStart = dtStart.value.replaceBefore("T", startDate.replace("-", ""))
        event.properties.remove(event.getProperty(Property.DTSTART))
        event.properties.add(DtStart(newDtStart, dtStart.timeZone))

        val dtEnd = event.getProperty<DtEnd>(Property.DTEND)
        val newDtEnd = dtEnd.value.replaceBefore("T", startDate.replace("-", ""))
        event.properties.remove(event.getProperty(Property.DTEND))
        event.properties.add(DtEnd(newDtEnd, dtEnd.timeZone))

        event.properties.add(RecurrenceId(newDtStart, dtStart.timeZone))
    }

    private fun aggregateCalendar(l: Calendar, r: Calendar): Calendar {
        l.components.addAll(r.getComponents(Component.VEVENT))
        return l
    }

    private fun buildCalDAVCollection(href: String): CustomCalDAVCollection {
        val caldavUrl = URI(calDavBaseUri)
        val baseUri = "${caldavUrl.scheme}://${caldavUrl.authority}"
        return CustomCalDAVCollection("$baseUri$href")
    }
}