package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-25 14:17
 */
class OpsgenieTest {
    @Test
    fun name() {
        val expected = "https://localhost/schedules/21%2F5/delete"
        assertEquals(expected,
                     Url(scheme = "https",
                         host = "localhost",
                         path = listOf(PathSegment("schedules"),
                                       PathSegment("21/5"),
                                       PathSegment("delete"))).toUrlString())
        assertEquals("", decode(expected))
    }
}