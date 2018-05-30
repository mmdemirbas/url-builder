package com.mmdemirbas.urlbuilder.custom

import com.mmdemirbas.urlbuilder.PathSegment
import com.mmdemirbas.urlbuilder.Url
import com.mmdemirbas.urlbuilder.parseUrl
import com.mmdemirbas.urlbuilder.toUrlString
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
        assertEquals(url, parseUrl(url.toUrlString()))
    }

    @ParameterizedTest
    @MethodSource("cases")
    fun Case.roundTrip() {
        val url = URL(input)
        val uri = URI(input)
        val urlString = url.toString()
        val uriString = uri.toString()

        val parsedInput = parseUrl(input)
        val parsedUrl = parseUrl(url)
        val parsedUrlString = parseUrl(urlString)
        val parsedUriString = parseUrl(uriString)

        assertEquals(expected, parsedInput.toUrlString())
        assertEquals(expected, parsedUrl.toUrlString())
        assertEquals(expected, parsedUrlString.toUrlString())
        assertEquals(expected, parsedUriString.toUrlString())
    }

    fun cases() = listOf(Case("everything",
                              "https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment"),
                         Case("empty path", "http://foo.com"),
                         Case("empty path And slash", "http://foo.com/", "http://foo.com"),
                         Case("port", "http://foo.com:1234"),
                         Case("empty path segment", "http://foo.com/foo//", "http://foo.com/foo"),
                         Case("non-safe host", "http://f%20oo.com/bar"),
                         Case("non-safe path segment", "http://foo.com/foo/b%20ar"),
                         Case("non-safe matrix param", "http://foo.com/foo;m1=v1;m%202=v%202"),
                         Case("non-safe query param", "http://foo.com/foo?q%201=v%202&q2=v2"),
                         Case("non-safe query param delimiter", "http://foo.com/foo?q1=%3Dv1&%26q2=v2"),
                         Case("non-safe fragment", "http://foo.com/foo#b%20ar"),
                         Case("empty path segment with matrix params", "http://foo.com/foo/;m1=v1"),
                         Case("empty path with matrix params", "http://foo.com/;m1=v1"),
                         Case("empty path with multiple matrix params", "http://foo.com/;m1=v1;m2=v2"),
                         Case("path segment ending with semicolon", "http://foo.com/foo;", "http://foo.com/foo"),
                         Case("malformed query param multi values", "http://foo.com/foo?q1=v1=v2"),
                         Case("malformed query param no value", "http://foo.com/foo?q1=v1&q2"),
                         Case("unstructured query with escaped chars", "http://foo.com/foo?query==&%23"))

    data class Case(val name: String, val input: String, val expected: String = input)
}