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
                                              path = listOf(PathSegment("schedules"),
                                                            PathSegment("21/5"),
                                                            PathSegment("delete")))
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

    fun cases() = listOf(Case("fromUrlWithEverything",
                                                                      "https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment"),
                         Case("fromUrlWithEmptyPath", "http://foo.com"),
                         Case("fromUrlWithEmptyPathAndSlash",
                                                                      "http://foo.com/",
                                                                      "http://foo.com"),
                         Case("fromUrlWithPort", "http://foo.com:1234"),
                         Case("fromUrlWithEmptyPathSegent",
                                                                      "http://foo.com/foo//",
                                                                      "http://foo.com/foo"),
                         Case("fromUrlWithEncodedHost",
                                                                      "http://f%20oo.com/bar"),
                         Case("fromUrlWithEncodedPathSegment",
                                                                      "http://foo.com/foo/b%20ar"),
                         Case("fromUrlWithEncodedMatrixParam",
                                                                      "http://foo.com/foo;m1=v1;m%202=v%202"),
                         Case("fromUrlWithEncodedQueryParam",
                                                                      "http://foo.com/foo?q%201=v%202&q2=v2"),
                         Case("fromUrlWithEncodedQueryParamDelimiter",
                                                                      "http://foo.com/foo?q1=%3Dv1&%26q2=v2"),
                         Case("fromUrlWithEncodedFragment",
                                                                      "http://foo.com/foo#b%20ar"),
                         Case("fromUrlWithEmptyPathSegmentWithMatrixParams",
                                                                      "http://foo.com/foo/;m1=v1"),
                         Case("fromUrlWithEmptyPathWithMatrixParams",
                                                                      "http://foo.com/;m1=v1"),
                         Case("fromUrlWithEmptyPathWithMultipleMatrixParams",
                                                                      "http://foo.com/;m1=v1;m2=v2"),
                         Case("fromUrlWithPathSegmentEndingWithSemicolon",
                                                                      "http://foo.com/foo;",
                                                                      "http://foo.com/foo"),
                         Case("fromUrlMalformedQueryParamMultiValues",
                                                                      "http://foo.com/foo?q1=v1=v2"),
                         Case("fromUrlMalformedQueryParamNoValue",
                                                                      "http://foo.com/foo?q1=v1&q2"),
                         Case("fromUrlUnstructuredQueryWithEscapedChars",
                                                                      "http://foo.com/foo?query==&%23"))

    data class Case(val name: String, val input: String, val expected: String = input)
}