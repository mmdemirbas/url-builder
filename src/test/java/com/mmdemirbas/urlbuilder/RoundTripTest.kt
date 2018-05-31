package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object RoundTripTest {
    @Test
    fun `slash in the path segment`() {
        val url =
                Url(scheme = "https",
                    host = "localhost",
                    path = listOf(PathSegment("schedules"), PathSegment("21/5"), PathSegment("delete")))
        val urlString = "https://localhost/schedules/21%2F5/delete"

        assertEquals(urlString, url.toUrlString())
        assertEquals(url, url.toUrlString().toUrl())
    }

    @ParameterizedTest
    @MethodSource("cases")
    fun TestCase.`round trip`() {
        val url = URL(input)
        val uri = URI(input)
        val urlString = url.toString()
        val uriString = uri.toString()

        val parsedInput = input.toUrl()
        val parsedUrl = url.toUrl()
        val parsedUrlString = urlString.toUrl()
        val parsedUriString = uriString.toUrl()

        assertEquals(expected, parsedInput.toUrlString())
        assertEquals(expected, parsedUrl.toUrlString())
        assertEquals(expected, parsedUrlString.toUrlString())
        assertEquals(expected, parsedUriString.toUrlString())
    }

    fun cases() =
            listOf(TestCase("https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment"),
                   TestCase("http://foo.com"),
                   TestCase("http://foo.com/", "http://foo.com"),
                   TestCase("http://foo.com:1234"),
                   TestCase("http://foo.com/foo//", "http://foo.com/foo"),
                   TestCase("http://f%20oo.com/bar"),
                   TestCase("http://foo.com/foo/b%20ar"),
                   TestCase("http://foo.com/foo;m1=v1;m%202=v%202"),
                   TestCase("http://foo.com/foo?q%201=v%202&q2=v2"),
                   TestCase("http://foo.com/foo?q1=%3Dv1&%26q2=v2"),
                   TestCase("http://foo.com/foo#b%20ar"),
                   TestCase("http://foo.com/foo/;m1=v1"),
                   TestCase("http://foo.com/;m1=v1"),
                   TestCase("http://foo.com/;m1=v1;m2=v2"),
                   TestCase("http://foo.com/foo;", "http://foo.com/foo"),
                   TestCase("http://foo.com/foo?q1=v1=v2"),
                   TestCase("http://foo.com/foo?q1=v1&q2"),
                   TestCase("http://foo.com/foo?query==&%23"))

    data class TestCase(val input: String, val expected: String = input)
}