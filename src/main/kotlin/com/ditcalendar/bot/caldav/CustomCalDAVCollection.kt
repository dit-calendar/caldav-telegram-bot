package com.ditcalendar.bot.caldav

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.CalDAVResource
import com.github.caldav4j.exceptions.BadStatusException
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.exceptions.ResourceOutOfDateException
import com.github.caldav4j.model.request.CalendarRequest
import com.github.caldav4j.util.ICalendarUtils
import com.github.caldav4j.util.UrlUtils
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import org.apache.http.client.HttpClient

class CustomCalDAVCollection(uri: String) : CalDAVCollection(uri) {

    /**
     * copy of updateMasterEvent
     *
     * @param httpClient the httpClient which will make the request
     * @param vevent the vevent to update
     * @param timezone The VTimeZone of the VEvent if it references one, otherwise null
     * @throws CalDAV4JException on error
     */
    @Throws(CalDAV4JException::class)
    fun addRecurrentEvent(httpClient: HttpClient, vevent: VEvent, timezone: VTimeZone?) {
        val uid = ICalendarUtils.getUIDValue(vevent)
        val resource = getCalDAVResourceByUID(httpClient, Component.VEVENT, uid)
        val calendar = resource.calendar

        calendar.components.add(vevent)
        if (timezone != null) {
            val originalVTimeZone = ICalendarUtils.getTimezone(calendar)
            if (originalVTimeZone != null) calendar.components.remove(originalVTimeZone)
            calendar.components.add(timezone)
        }
        put(
                httpClient,
                calendar,
                UrlUtils.stripHost(resource.resourceMetadata.href),
                resource.resourceMetadata.eTag)
    }


    /**
     * copy from CalDAVCollection
     *
     * @param httpClient the httpClient which will make the request
     * @param calendar iCal body to place on the server
     * @param path Path to the new/old resource
     * @param etag ETag if updation of calendar has to take place.
     * @throws CalDAV4JException on error
     */
    @Throws(CalDAV4JException::class)
    private fun put(httpClient: HttpClient, calendar: Calendar, path: String, etag: String) {
        val cr = CalendarRequest()
        cr.addEtag(etag)
        cr.isIfMatch = true
        cr.calendar = calendar
        val putMethod = methodFactory.createPutMethod(path, cr)
        try {
            val response = httpClient.execute(getDefaultHttpHost(putMethod.uri), putMethod)
            when (val statusCode = response.statusLine.statusCode) {
                204, 201 -> {
                }
                412 -> throw ResourceOutOfDateException("Etag was not matched: $etag")
                else -> throw BadStatusException(statusCode, putMethod.method, path)
            }
            if (isCacheEnabled) {
                val h = putMethod.getFirstHeader("ETag")
                var newEtag: String? = null
                newEtag = if (h != null) {
                    h.value
                } else {
                    getETagbyMultiget(httpClient, path)
                }
                cache.putResource(
                        CalDAVResource(calendar, newEtag, putMethod.uri.toString()))
            }
        } catch (e: ResourceOutOfDateException) {
            throw e
        } catch (e: BadStatusException) {
            throw e
        } catch (e: Exception) {
            throw CalDAV4JException("Problem executing put method", e)
        } finally {
            putMethod.reset()
        }
    }
}