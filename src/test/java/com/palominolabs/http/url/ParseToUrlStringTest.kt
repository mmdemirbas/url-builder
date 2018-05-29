package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI
import java.net.URL

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-29 12:24
 */
class ParseToUrlStringTest {
    @Test
    fun fromUrlWithEverything() {
        test("https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment")
    }

    @Test
    fun fromUrlWithEmptyPath() {
        test("http://foo.com")
    }

    @Test
    fun fromUrlWithEmptyPathAndSlash() {
        test("http://foo.com/", "http://foo.com")
    }

    @Test
    fun fromUrlWithPort() {
        test("http://foo.com:1234")
    }

    @Test
    fun fromUrlWithEmptyPathSegent() {
        test("http://foo.com/foo//", "http://foo.com/foo")
    }

    @Test
    fun fromUrlWithEncodedHost() {
        test("http://f%20oo.com/bar")
    }

    @Test
    fun fromUrlWithEncodedPathSegment() {
        test("http://foo.com/foo/b%20ar")
    }

    @Test
    fun fromUrlWithEncodedMatrixParam() {
        test("http://foo.com/foo;m1=v1;m%202=v%202")
    }

    @Test
    fun fromUrlWithEncodedQueryParam() {
        test("http://foo.com/foo?q%201=v%202&q2=v2")
    }

    @Test
    fun fromUrlWithEncodedQueryParamDelimiter() {
        test("http://foo.com/foo?q1=%3Dv1&%26q2=v2")
    }

    @Test
    fun fromUrlWithEncodedFragment() {
        test("http://foo.com/foo#b%20ar")
    }

    @Test
    fun fromUrlWithEmptyPathSegmentWithMatrixParams() {
        test("http://foo.com/foo/;m1=v1")
    }

    @Test
    fun fromUrlWithEmptyPathWithMatrixParams() {
        test("http://foo.com/;m1=v1")
    }

    @Test
    fun fromUrlWithEmptyPathWithMultipleMatrixParams() {
        test("http://foo.com/;m1=v1;m2=v2")
    }

    @Test
    fun fromUrlWithPathSegmentEndingWithSemicolon() {
        test("http://foo.com/foo;", "http://foo.com/foo")
    }

    @Test
    fun fromUrlMalformedQueryParamMultiValues() {
        test("http://foo.com/foo?q1=v1=v2")
    }

    @Test
    fun fromUrlMalformedQueryParamNoValue() {
        test("http://foo.com/foo?q1=v1&q2")
    }

    @Test
    fun fromUrlUnstructuredQueryWithEscapedChars() {
        test("http://foo.com/foo?query==&%23")
    }

    private fun test(input: String, expected: String = input) {
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
}