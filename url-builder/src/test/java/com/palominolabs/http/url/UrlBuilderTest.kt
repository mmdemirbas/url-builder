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
        assertUrlEquals("http://foo.com", UrlBuilder("http", "foo.com").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testWithPort() {
        assertUrlEquals("http://foo.com:33", UrlBuilder("http", "foo.com", 33).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testSimplePath() {
        assertUrlEquals("http://foo.com/seg1/seg2",
                        UrlBuilder("http", "foo.com").pathSegment("seg1").pathSegment("seg2").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathWithReserved() {
        // RFC 1738 S3.3
        assertUrlEquals("http://foo.com/seg%2F%3B%3Fment/seg=&2",
                        UrlBuilder("http", "foo.com").pathSegment("seg/;?ment").pathSegment("seg=&2").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathSegments() {
        assertUrlEquals("http://foo.com/seg1/seg2/seg3",
                        UrlBuilder("http", "foo.com").pathSegments("seg1", "seg2", "seg3").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithoutPathHasLeadingSlash() {
        assertUrlEquals("http://foo.com/;foo=bar",
                        UrlBuilder("http", "foo.com").matrixParam("foo", "bar").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithReserved() {
        assertUrlEquals("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz",
                        UrlBuilder("http", "foo.com").pathSegment("foo").matrixParam("foo",
                                                                                     "bar").matrixParam("res;=?#/erved",
                                                                                                        "value").pathSegment(
                                "baz").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8() {
        // 1 UTF-16 char
        assertUrlEquals("http://foo.com/snowman/%E2%98%83",
                        UrlBuilder("http", "foo.com").pathSegment("snowman").pathSegment("\u2603").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertUrlEquals("http://foo.com/clef/%F0%9D%84%9E",
                        UrlBuilder("http", "foo.com").pathSegment("clef").pathSegment("\ud834\udd1e").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamNoPath() {
        assertUrlEquals("http://foo.com?foo=bar", UrlBuilder("http", "foo.com").queryParam("foo", "bar").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamsDuplicated() {
        assertUrlEquals("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2",
                        UrlBuilder("http", "foo.com").queryParam("foo", "bar").queryParam("foo",
                                                                                          "bar2").queryParam("baz",
                                                                                                             "quux").queryParam(
                                "baz",
                                "quux2").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParams() {
        assertUrlEquals("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2",
                        UrlBuilder("http", "foo.com").queryParam("foo", "bar&=#baz").queryParam("foo",
                                                                                                "bar?/2").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParamWithSpaceAndPlus() {
        assertUrlEquals("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B",
                        UrlBuilder("http", "foo.com").queryParam("foo", "spa ce").queryParam("fo+o",
                                                                                             "plus+").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPlusInVariousParts() {
        assertUrlEquals("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag",
                        UrlBuilder("http", "foo.com").pathSegment("has+plus").matrixParam("plusMtx",
                                                                                          "pl+us").queryParam("plusQp",
                                                                                                              "pl+us").fragment(
                                "plus+frag").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testFragment() {
        assertUrlEquals("http://foo.com?foo=bar#%23frag/?",
                        UrlBuilder("http", "foo.com").queryParam("foo", "bar").fragment("#frag/?").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testAllParts() {
        assertEquals("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
                     UrlBuilder("https", "foo.bar.com", 3333).pathSegment("foo").pathSegment("bar").matrixParam("mtx1",
                                                                                                                "val1").matrixParam(
                             "mtx2",
                             "val2").queryParam("q1", "v1").queryParam("q2",
                                                                       "v2").fragment("zomg it's a fragment").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv4Literal() {
        assertUrlEquals("http://127.0.0.1", UrlBuilder("http", "127.0.0.1").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testBadIPv4LiteralDoesntChoke() {
        assertUrlEquals("http://300.100.50.1", UrlBuilder("http", "300.100.50.1").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6LiteralLocalhost() {
        assertUrlEquals("http://[::1]", UrlBuilder("http", "[::1]").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6Literal() {
        assertUrlEquals("http://[2001:db8:85a3::8a2e:370:7334]",
                        UrlBuilder("http", "[2001:db8:85a3::8a2e:370:7334]").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameSingleByte() {
        assertUrlEquals("http://host%3Fname;", UrlBuilder("http", "host?name;").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameMultiByte() {
        assertUrlEquals("http://snow%E2%98%83man", UrlBuilder("http", "snow\u2603man").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlash() {
        assertUrlEquals("https://foo.com/a/b/c/",
                        UrlBuilder("https", "foo.com").forceTrailingSlash().pathSegments("a", "b", "c").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashWithQueryParams() {
        assertUrlEquals("https://foo.com/a/b/c/?foo=bar",
                        UrlBuilder("https", "foo.com").forceTrailingSlash().pathSegments("a",
                                                                                         "b",
                                                                                         "c").queryParam("foo",
                                                                                                         "bar").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashNoPathSegmentsWithMatrixParams() {
        assertUrlEquals("https://foo.com/;m1=v1/",
                        UrlBuilder("https", "foo.com").forceTrailingSlash().matrixParam("m1", "v1").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIntermingledMatrixParamsAndPathSegments() {
        assertUrlEquals("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2",
                        UrlBuilder("http", "foo.com").pathSegments("seg1", "seg2").matrixParam("m1",
                                                                                               "v1").pathSegment("seg3").matrixParam(
                                "m2",
                                "v2").toUrlString())
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
            UrlBuilder(URL("http://foo.com/foo;m1=v1=v2"))
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
            UrlBuilder(URL("http://foo.com/fo%2o"))
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
    fun testCantUseQueryParamAfterQuery() {
        val ub = UrlBuilder("http", "foo.com").unstructuredQuery("q")
        try {
            ub.queryParam("foo", "bar")
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("Cannot call queryParam() when this already has an unstructured query specified", e.message)
        }

    }

    @Test
    fun testCantUseQueryAfterQueryParam() {
        val ub = UrlBuilder("http", "foo.com").queryParam("foo", "bar")
        try {
            ub.unstructuredQuery("q")
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("Cannot call unstructuredQuery() when this already has queryParam pairs specified", e.message)
        }

    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithNoSpecialChars() {
        assertUrlEquals("http://foo.com?q", UrlBuilder("http", "foo.com").unstructuredQuery("q").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithOkSpecialChars() {
        assertUrlEquals("http://foo.com?q?/&=", UrlBuilder("http", "foo.com").unstructuredQuery("q?/&=").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithEscapedSpecialChars() {
        assertUrlEquals("http://foo.com?q%23%2B", UrlBuilder("http", "foo.com").unstructuredQuery("q#+").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryRemovesQueryParam() {
        assertUrlEquals("http://host", UrlBuilder("http", "host").queryParam("foo", "bar").clearQuery().toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryRemovesUnstructuredQuery() {
        assertUrlEquals("http://host",
                        UrlBuilder("http", "host").unstructuredQuery("foobar").clearQuery().toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryAfterQueryParamAllowsQuery() {
        assertUrlEquals("http://host?foobar",
                        UrlBuilder("http", "host").queryParam("foo",
                                                              "bar").clearQuery().unstructuredQuery("foobar").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryAfterQueryAllowsQueryParam() {
        assertUrlEquals("http://host?foo=bar",
                        UrlBuilder("http", "host").unstructuredQuery("foobar").clearQuery().queryParam("foo",
                                                                                                       "bar").toUrlString())
    }

    private fun assertUrlBuilderRoundtrip(url: String) {
        assertUrlBuilderRoundtrip(url, url)
    }

    /**
     * @param origUrl  the url that will be used to create a URL
     * @param finalUrl the URL string it should end up as
     */
    private fun assertUrlBuilderRoundtrip(origUrl: String, finalUrl: String) {
        assertUrlEquals(finalUrl, UrlBuilder(URL(origUrl)).toUrlString())
    }

    private fun assertUrlEquals(expected: String, actual: String) {
        assertEquals(expected, actual)
        assertEquals(expected, URI(actual).toString())
        assertEquals(expected, URL(actual).toString())
    }
}
