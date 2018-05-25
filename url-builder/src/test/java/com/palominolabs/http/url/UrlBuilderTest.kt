/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import com.google.common.base.Throwables
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.CharacterCodingException
import org.junit.Test

import com.palominolabs.http.url.UrlBuilder.Companion.forHost
import com.palominolabs.http.url.UrlBuilder.Companion.fromUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

class UrlBuilderTest {

    @Test
    @Throws(CharacterCodingException::class)
    fun testNoUrlParts() {
        assertUrlEquals("http://foo.com", forHost("http", "foo.com").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testWithPort() {
        assertUrlEquals("http://foo.com:33", forHost("http", "foo.com", 33).toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testSimplePath() {
        val ub = forHost("http", "foo.com")
        ub.pathSegment("seg1").pathSegment("seg2")
        assertUrlEquals("http://foo.com/seg1/seg2", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathWithReserved() {
        // RFC 1738 S3.3
        val ub = forHost("http", "foo.com")
        ub.pathSegment("seg/;?ment").pathSegment("seg=&2")
        assertUrlEquals("http://foo.com/seg%2F%3B%3Fment/seg=&2", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPathSegments() {
        val ub = forHost("http", "foo.com")
        ub.pathSegments("seg1", "seg2", "seg3")
        assertUrlEquals("http://foo.com/seg1/seg2/seg3", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithoutPathHasLeadingSlash() {
        val ub = forHost("http", "foo.com")
        ub.matrixParam("foo", "bar")
        assertUrlEquals("http://foo.com/;foo=bar", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testMatrixWithReserved() {
        val ub =
                forHost("http", "foo.com").pathSegment("foo").matrixParam("foo", "bar")
                    .matrixParam("res;=?#/erved", "value").pathSegment("baz")
        assertUrlEquals("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8() {
        // 1 UTF-16 char
        val ub = forHost("http", "foo.com")
        ub.pathSegment("snowman").pathSegment("\u2603")
        assertUrlEquals("http://foo.com/snowman/%E2%98%83", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedPathSegmentUtf8SurrogatePair() {
        val ub = forHost("http", "foo.com")
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        ub.pathSegment("clef").pathSegment("\ud834\udd1e")
        assertUrlEquals("http://foo.com/clef/%F0%9D%84%9E", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamNoPath() {
        val ub = forHost("http", "foo.com")
        ub.queryParam("foo", "bar")
        val s = ub.toUrlString()
        assertUrlEquals("http://foo.com?foo=bar", s)
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testQueryParamsDuplicated() {
        val ub = forHost("http", "foo.com")
        ub.queryParam("foo", "bar")
        ub.queryParam("foo", "bar2")
        ub.queryParam("baz", "quux")
        ub.queryParam("baz", "quux2")
        assertUrlEquals("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParams() {
        val ub = forHost("http", "foo.com")
        ub.queryParam("foo", "bar&=#baz")
        ub.queryParam("foo", "bar?/2")
        assertUrlEquals("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeQueryParamWithSpaceAndPlus() {
        val ub = forHost("http", "foo.com")
        ub.queryParam("foo", "spa ce")
        ub.queryParam("fo+o", "plus+")
        assertUrlEquals("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testPlusInVariousParts() {
        val ub = forHost("http", "foo.com")

        ub.pathSegment("has+plus").matrixParam("plusMtx", "pl+us").queryParam("plusQp", "pl+us").fragment("plus+frag")

        assertUrlEquals("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testFragment() {
        val ub = forHost("http", "foo.com")
        ub.queryParam("foo", "bar")
        ub.fragment("#frag/?")
        assertUrlEquals("http://foo.com?foo=bar#%23frag/?", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testAllParts() {
        val ub = forHost("https", "foo.bar.com", 3333)
        ub.pathSegment("foo")
        ub.pathSegment("bar")
        ub.matrixParam("mtx1", "val1")
        ub.matrixParam("mtx2", "val2")
        ub.queryParam("q1", "v1")
        ub.queryParam("q2", "v2")
        ub.fragment("zomg it's a fragment")

        assertEquals("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
                     ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv4Literal() {
        val ub = forHost("http", "127.0.0.1")
        assertUrlEquals("http://127.0.0.1", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testBadIPv4LiteralDoesntChoke() {
        val ub = forHost("http", "300.100.50.1")
        assertUrlEquals("http://300.100.50.1", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6LiteralLocalhost() {
        val ub = forHost("http", "[::1]")
        assertUrlEquals("http://[::1]", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIPv6Literal() {
        val ub = forHost("http", "[2001:db8:85a3::8a2e:370:7334]")
        assertUrlEquals("http://[2001:db8:85a3::8a2e:370:7334]", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameSingleByte() {
        val ub = forHost("http", "host?name;")
        assertUrlEquals("http://host%3Fname;", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodedRegNameMultiByte() {
        val ub = forHost("http", "snow\u2603man")
        assertUrlEquals("http://snow%E2%98%83man", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlash() {
        val ub = forHost("https", "foo.com").forceTrailingSlash().pathSegments("a", "b", "c")

        assertUrlEquals("https://foo.com/a/b/c/", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashWithQueryParams() {
        val ub = forHost("https", "foo.com").forceTrailingSlash().pathSegments("a", "b", "c").queryParam("foo", "bar")

        assertUrlEquals("https://foo.com/a/b/c/?foo=bar", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testForceTrailingSlashNoPathSegmentsWithMatrixParams() {
        val ub = forHost("https", "foo.com").forceTrailingSlash().matrixParam("m1", "v1")

        assertUrlEquals("https://foo.com/;m1=v1/", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testIntermingledMatrixParamsAndPathSegments() {

        val ub =
                forHost("http", "foo.com").pathSegments("seg1", "seg2").matrixParam("m1", "v1").pathSegment("seg3")
                    .matrixParam("m2", "v2")

        assertUrlEquals("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2", ub.toUrlString())
    }

    @Test
    fun testFromUrlWithEverything() {
        val orig =
                "https://foo.bar.com:3333/foo/ba%20r;mtx1=val1;mtx2=val%202/seg%203;m2=v2?q1=v1&q2=v%202#zomg%20it's%20a%20fragment"
        assertUrlBuilderRoundtrip(orig)
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
            fromUrl(URL("http://foo.com/foo;m1=v1=v2"))
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
            fromUrl(URL("http://foo.com/fo%2o"))
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
        val ub = forHost("http", "foo.com").unstructuredQuery("q")

        try {
            ub.queryParam("foo", "bar")
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("Cannot call queryParam() when this already has an unstructured query specified", e.message)
        }

    }

    @Test
    fun testCantUseQueryAfterQueryParam() {
        val ub = forHost("http", "foo.com").queryParam("foo", "bar")

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
        assertUrlEquals("http://foo.com?q", forHost("http", "foo.com").unstructuredQuery("q").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithOkSpecialChars() {
        assertUrlEquals("http://foo.com?q?/&=", forHost("http", "foo.com").unstructuredQuery("q?/&=").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUnstructuredQueryWithEscapedSpecialChars() {
        assertUrlEquals("http://foo.com?q%23%2B", forHost("http", "foo.com").unstructuredQuery("q#+").toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryRemovesQueryParam() {
        val ub = forHost("http", "host").queryParam("foo", "bar").clearQuery()
        assertUrlEquals("http://host", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryRemovesUnstructuredQuery() {
        val ub = forHost("http", "host").unstructuredQuery("foobar").clearQuery()
        assertUrlEquals("http://host", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryAfterQueryParamAllowsQuery() {
        val ub = forHost("http", "host").queryParam("foo", "bar").clearQuery().unstructuredQuery("foobar")
        assertUrlEquals("http://host?foobar", ub.toUrlString())
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testClearQueryAfterQueryAllowsQueryParam() {
        val ub = forHost("http", "host").unstructuredQuery("foobar").clearQuery().queryParam("foo", "bar")
        assertUrlEquals("http://host?foo=bar", ub.toUrlString())
    }

    private fun assertUrlBuilderRoundtrip(url: String) {
        assertUrlBuilderRoundtrip(url, url)
    }

    /**
     * @param origUrl  the url that will be used to create a URL
     * @param finalUrl the URL string it should end up as
     */
    private fun assertUrlBuilderRoundtrip(origUrl: String, finalUrl: String) {
        try {
            assertUrlEquals(finalUrl, fromUrl(URL(origUrl)).toUrlString())
        } catch (e: CharacterCodingException) {
            throw Throwables.propagate(e)
        } catch (e: MalformedURLException) {
            throw Throwables.propagate(e)
        }

    }

    private fun assertUrlEquals(expected: String, actual: String) {
        assertEquals(expected, actual)
        try {
            assertEquals(expected, URI(actual).toString())
        } catch (e: URISyntaxException) {
            throw Throwables.propagate(e)
        }

        try {
            assertEquals(expected, URL(actual).toString())
        } catch (e: MalformedURLException) {
            throw Throwables.propagate(e)
        }

    }
}
