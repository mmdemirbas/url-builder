/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.CharacterCodingException

class UrlBuilderTest {
    @Test
    @Throws(CharacterCodingException::class)
    fun testNoUrlParts() {
        assertUrlEquals("http://foo.com", Url(scheme = "http", host = "foo.com").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testWithPort() {
        assertUrlEquals("http://foo.com:33", Url(scheme = "http", host = "foo.com", port = 33).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testSimplePath() {
        assertUrlEquals("http://foo.com/seg1/seg2",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("seg1"), PathSegment("seg2"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathWithReserved() {
        // RFC 1738 S3.3
        assertUrlEquals("http://foo.com/seg%2F%3B%3Fment/seg=&2",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("seg/;?ment"), PathSegment("seg=&2"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathSegments() {
        assertUrlEquals("http://foo.com/seg1/seg2/seg3",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("seg1"), PathSegment("seg2"), PathSegment("seg3"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithoutPathHasLeadingSlash() {
        assertUrlEquals("http://foo.com/;foo=bar",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("", "foo" to "bar"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithReserved() {
        assertUrlEquals("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("foo", "foo" to "bar", "res;=?#/erved" to "value"),
                                          PathSegment("baz"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8() {
        // 1 UTF-16 char
        assertUrlEquals("http://foo.com/snowman/%E2%98%83",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("snowman"), PathSegment("\u2603"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertUrlEquals("http://foo.com/clef/%F0%9D%84%9E",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("clef"), PathSegment("\ud834\udd1e"))).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamNoPath() {
        assertUrlEquals("http://foo.com?foo=bar",
                        Url(scheme = "http", host = "foo.com", query = Query.Structured("foo" to "bar")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamsDuplicated() {
        assertUrlEquals("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2",
                        Url(scheme = "http",
                            host = "foo.com",
                            query = Query.Structured("foo" to "bar",
                                                     "foo" to "bar2",
                                                     "baz" to "quux",
                                                     "baz" to "quux2")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParams() {
        assertUrlEquals("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2",
                        Url(scheme = "http",
                            host = "foo.com",
                            query = Query.Structured("foo" to "bar&=#baz", "foo" to "bar?/2")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParamWithSpaceAndPlus() {
        assertUrlEquals("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B",
                        Url(scheme = "http",
                            host = "foo.com",
                            query = Query.Structured("foo" to "spa ce", "fo+o" to "plus+")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPlusInVariousParts() {
        assertUrlEquals("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("has+plus", "plusMtx" to "pl+us")),
                            query = Query.Structured("plusQp" to "pl+us"),
                            fragment = "plus+frag").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testFragment() {
        assertUrlEquals("http://foo.com?foo=bar#%23frag/?",
                        Url(scheme = "http",
                            host = "foo.com",
                            query = Query.Structured("foo" to "bar"),
                            fragment = "#frag/?").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testAllParts() {
        assertEquals("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
                     Url(scheme = "https",
                         host = "foo.bar.com",
                         port = 3333,
                         path = listOf(PathSegment("foo"), PathSegment("bar", "mtx1" to "val1", "mtx2" to "val2")),
                         query = Query.Structured("q1" to "v1", "q2" to "v2"),
                         fragment = "zomg it's a fragment").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv4Literal() {
        assertUrlEquals("http://127.0.0.1", Url(scheme = "http", host = "127.0.0.1").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testBadIPv4LiteralDoesntChoke() {
        assertUrlEquals("http://300.100.50.1", Url(scheme = "http", host = "300.100.50.1").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6LiteralLocalhost() {
        assertUrlEquals("http://[::1]", Url(scheme = "http", host = "[::1]").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6Literal() {
        assertUrlEquals("http://[2001:db8:85a3::8a2e:370:7334]",
                        Url(scheme = "http", host = "[2001:db8:85a3::8a2e:370:7334]").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameSingleByte() {
        assertUrlEquals("http://host%3Fname;", Url(scheme = "http", host = "host?name;").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameMultiByte() {
        assertUrlEquals("http://snow%E2%98%83man", Url(scheme = "http", host = "snow\u2603man").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlash() {
        assertUrlEquals("https://foo.com/a/b/c/",
                        Url(scheme = "https",
                            host = "foo.com",
                            path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c"))).toUrlString(true))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashWithQueryParams() {
        assertUrlEquals("https://foo.com/a/b/c/?foo=bar",
                        Url(scheme = "https",
                            host = "foo.com",
                            path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c")),
                            query = Query.Structured("foo" to "bar")).toUrlString(true))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashNoPathSegmentsWithMatrixParams() {
        assertUrlEquals("https://foo.com/;m1=v1/",
                        Url(scheme = "https",
                            host = "foo.com",
                            path = listOf(PathSegment("", "m1" to "v1"))).toUrlString(true))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIntermingledMatrixParamsAndPathSegments() {
        assertUrlEquals("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2",
                        Url(scheme = "http",
                            host = "foo.com",
                            path = listOf(PathSegment("seg1"),
                                          PathSegment("seg2", "m1" to "v1"),
                                          PathSegment("seg3", "m2" to "v2"))).toUrlString())
    }

    @Test
    fun testFromUrlWithEverything() {
        assertUrlBuilderRoundtrip("https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment")
    }

    @Test
    fun testFromUrlWithEmptyPath() {
        assertUrlBuilderRoundtrip("http://foo.com")
    }

    @Test
    fun testFromUrlWithEmptyPathAndSlash() {
        assertUrlBuilderRoundtrip("http://foo.com/", "http://foo.com")
    }

    @Test
    fun testFromUrlWithPort() {
        assertUrlBuilderRoundtrip("http://foo.com:1234")
    }

    @Test
    fun testFromUrlWithEmptyPathSegent() {
        assertUrlBuilderRoundtrip("http://foo.com/foo//", "http://foo.com/foo")
    }

    @Test
    fun testFromUrlWithEncodedHost() {
        assertUrlBuilderRoundtrip("http://f%20oo.com/bar")
    }

    @Test
    fun testFromUrlWithEncodedPathSegment() {
        assertUrlBuilderRoundtrip("http://foo.com/foo/b%20ar")
    }

    @Test
    fun testFromUrlWithEncodedMatrixParam() {
        assertUrlBuilderRoundtrip("http://foo.com/foo;m1=v1;m%202=v%202")
    }

    @Test
    fun testFromUrlWithEncodedQueryParam() {
        assertUrlBuilderRoundtrip("http://foo.com/foo?q%201=v%202&q2=v2")
    }

    @Test
    fun testFromUrlWithEncodedQueryParamDelimiter() {
        assertUrlBuilderRoundtrip("http://foo.com/foo?q1=%3Dv1&%26q2=v2")
    }

    @Test
    fun testFromUrlWithEncodedFragment() {
        assertUrlBuilderRoundtrip("http://foo.com/foo#b%20ar")
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testFromUrlWithMalformedMatrixPair() {
        try {
            parseUrl(URL("http://foo.com/foo;m1=v1=v2"))
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Malformed matrix param: <m1=v1=v2>", e.message)
        }

    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testFromUrlWithMalformedMatrixPair2() {
        try {
            parseUrl("http://foo.com/foo;m1=v1=v2")
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Malformed matrix param: <m1=v1=v2>", e.message)
        }

    }

    @Test
    fun testFromUrlWithEmptyPathSegmentWithMatrixParams() {
        assertUrlBuilderRoundtrip("http://foo.com/foo/;m1=v1")
    }

    @Test
    fun testFromUrlWithEmptyPathWithMatrixParams() {
        assertUrlBuilderRoundtrip("http://foo.com/;m1=v1")
    }

    @Test
    fun testFromUrlWithEmptyPathWithMultipleMatrixParams() {
        assertUrlBuilderRoundtrip("http://foo.com/;m1=v1;m2=v2")
    }

    @Test
    fun testFromUrlWithPathSegmentEndingWithSemicolon() {
        assertUrlBuilderRoundtrip("http://foo.com/foo;", "http://foo.com/foo")
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testPercentDecodeInvalidPair() {
        try {
            parseUrl(URL("http://foo.com/fo%2o"))
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid %-tuple <%2o>", e.message)
        }
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testPercentDecodeInvalidPair2() {
        try {
            parseUrl("http://foo.com/fo%2o")
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid %-tuple <%2o>", e.message)
        }
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testFromUrlMalformedQueryParamMultiValues() {
        assertUrlBuilderRoundtrip("http://foo.com/foo?q1=v1=v2")
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testFromUrlMalformedQueryParamNoValue() {
        assertUrlBuilderRoundtrip("http://foo.com/foo?q1=v1&q2")
    }

    @Test
    @Throws(MalformedURLException::class, CharacterCodingException::class)
    fun testFromUrlUnstructuredQueryWithEscapedChars() {
        assertUrlBuilderRoundtrip("http://foo.com/foo?query==&%23")
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithNoSpecialChars() {
        assertUrlEquals("http://foo.com?q",
                        Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithOkSpecialChars() {
        assertUrlEquals("http://foo.com?q?/&=",
                        Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q?/&=")).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithEscapedSpecialChars() {
        assertUrlEquals("http://foo.com?q%23%2B",
                        Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q#+")).toUrlString())
    }

    private fun assertUrlBuilderRoundtrip(url: String) {
        assertUrlBuilderRoundtrip(url, url)
    }

    /**
     * @param origUrl  the url that will be used to create a URL
     * @param finalUrl the URL string it should end up as
     */
    private fun assertUrlBuilderRoundtrip(origUrl: String, finalUrl: String) {
        assertUrlEquals(finalUrl, parseUrl(URL(origUrl)).toUrlString())
        assertUrlEquals(finalUrl, parseUrl(origUrl).toUrlString())
    }

    private fun assertUrlEquals(expected: String, actual: String) {
        assertEquals(expected, actual)
        assertEquals(expected, URI(actual).toString())
        assertEquals(expected, URL(actual).toString())
    }
}
