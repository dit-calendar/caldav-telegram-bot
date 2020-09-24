package com.ditcalendar.bot.caldav

import com.ditcalendar.bot.config.caldav_user_name
import com.ditcalendar.bot.config.caldav_user_password
import com.ditcalendar.bot.config.config
import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.methods.CalDAV4JMethodFactory
import com.github.caldav4j.util.GenerateQuery
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

    fun findSubcalendar(subCalendarName: String) {
        //System.setProperty("ical4j.unfolding.relaxed", "true")
        System.setProperty("ical4j.parsing.relaxed", "true")
        val uri = "http://localhost:8080/remote.php/dav/calendars/admin/"

        val factory = CalDAV4JMethodFactory()
        val propertyNameSet = DavPropertyNameSet()
        val propertyName = DavPropertyName.create(DavPropertyName.PROPERTY_DISPLAYNAME)
        propertyNameSet.add(propertyName)
        val method = factory.createPropFindMethod(uri, propertyNameSet, 1)

        val response: HttpResponse = httpclient.execute(method)
        val bla = method.getResponseBodyAsMultiStatus(response)
    }

    fun findSubcalendar(id: Int) {

    }

    fun findEvents(subcalendarId: Int, startDate: String, endDate: String) {
        //System.setProperty("ical4j.unfolding.relaxed", "true")
        System.setProperty("ical4j.parsing.relaxed", "true")
        val uri = "http://localhost:8080/remote.php/dav/calendars/admin/personal/"

        val gq = GenerateQuery()
        gq.setComponent("VEVENT")
        gq.setTimeRange(Date("20200801T173752Z"), Date("20200910T173752Z"))
        //gq.setFilter("VEVENT [20200801T173752Z;20200910T173752Z] : STATUS!=CANCELLED")
        val query = gq.generate()

        val collection = CalDAVCollection(uri)
        val resp = collection.queryCalendars(httpclient, query)
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