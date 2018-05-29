/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class ToUrlStringTest {
    @Test
    fun noUrlParts() {
        test("http://foo.com", Url(scheme = "http", host = "foo.com"))
    }

    @Test
    fun withPort() {
        test("http://foo.com:33", Url(scheme = "http", host = "foo.com", port = 33))
    }

    @Test
    fun simplePath() {
        test("http://foo.com/seg1/seg2",
             Url(scheme = "http", host = "foo.com", path = listOf(PathSegment("seg1"), PathSegment("seg2"))))
    }

    @Test
    fun pathWithReserved() {
        // RFC 1738 S3.3
        test("http://foo.com/seg%2F%3B%3Fment/seg=&2",
             Url(scheme = "http", host = "foo.com", path = listOf(PathSegment("seg/;?ment"), PathSegment("seg=&2"))))
    }

    @Test
    fun pathSegments() {
        test("http://foo.com/seg1/seg2/seg3",
             Url(scheme = "http",
                 host = "foo.com",
                 path = listOf(PathSegment("seg1"), PathSegment("seg2"), PathSegment("seg3"))))
    }

    @Test
    fun matrixWithoutPathHasLeadingSlash() {
        test("http://foo.com/;foo=bar",
             Url(scheme = "http", host = "foo.com", path = listOf(PathSegment("", "foo" to "bar"))))
    }

    @Test
    fun matrixWithReserved() {
        test("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz",
             Url(scheme = "http",
                 host = "foo.com",
                 path = listOf(PathSegment("foo", "foo" to "bar", "res;=?#/erved" to "value"), PathSegment("baz"))))
    }

    @Test
    fun urlEncodedPathSegmentUtf8() {
        // 1 UTF-16 char
        test("http://foo.com/snowman/%E2%98%83",
             Url(scheme = "http", host = "foo.com", path = listOf(PathSegment("snowman"), PathSegment("\u2603"))))
    }

    @Test
    fun urlEncodedPathSegmentUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        test("http://foo.com/clef/%F0%9D%84%9E",
             Url(scheme = "http", host = "foo.com", path = listOf(PathSegment("clef"), PathSegment("\ud834\udd1e"))))
    }

    @Test
    fun queryParamNoPath() {
        test("http://foo.com?foo=bar", Url(scheme = "http", host = "foo.com", query = Query.Structured("foo" to "bar")))
    }

    @Test
    fun queryParamsDuplicated() {
        test("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2",
             Url(scheme = "http",
                 host = "foo.com",
                 query = Query.Structured("foo" to "bar", "foo" to "bar2", "baz" to "quux", "baz" to "quux2")))
    }

    @Test
    fun encodeQueryParams() {
        test("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2",
             Url(scheme = "http", host = "foo.com", query = Query.Structured("foo" to "bar&=#baz", "foo" to "bar?/2")))
    }

    @Test
    fun encodeQueryParamWithSpaceAndPlus() {
        test("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B",
             Url(scheme = "http", host = "foo.com", query = Query.Structured("foo" to "spa ce", "fo+o" to "plus+")))
    }

    @Test
    fun plusInVariousParts() {
        test("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag",
             Url(scheme = "http",
                 host = "foo.com",
                 path = listOf(PathSegment("has+plus", "plusMtx" to "pl+us")),
                 query = Query.Structured("plusQp" to "pl+us"),
                 fragment = "plus+frag"))
    }

    @Test
    fun fragment() {
        test("http://foo.com?foo=bar#%23frag/?",
             Url(scheme = "http", host = "foo.com", query = Query.Structured("foo" to "bar"), fragment = "#frag/?"))
    }

    @Test
    fun allParts() {
        test("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
             Url(scheme = "https",
                 host = "foo.bar.com",
                 port = 3333,
                 path = listOf(PathSegment("foo"), PathSegment("bar", "mtx1" to "val1", "mtx2" to "val2")),
                 query = Query.Structured("q1" to "v1", "q2" to "v2"),
                 fragment = "zomg it's a fragment"))
    }

    @Test
    fun ipv4Literal() {
        test("http://127.0.0.1", Url(scheme = "http", host = "127.0.0.1"))
    }

    @Test
    fun badIPv4LiteralDoesntChoke() {
        test("http://300.100.50.1", Url(scheme = "http", host = "300.100.50.1"))
    }

    @Test
    fun ipv6LiteralLocalhost() {
        test("http://[::1]", Url(scheme = "http", host = "[::1]"))
    }

    @Test
    fun ipv6Literal() {
        test("http://[2001:db8:85a3::8a2e:370:7334]", Url(scheme = "http", host = "[2001:db8:85a3::8a2e:370:7334]"))
    }

    @Test
    fun encodedRegNameSingleByte() {
        test("http://host%3Fname;", Url(scheme = "http", host = "host?name;"))
    }

    @Test
    fun encodedRegNameMultiByte() {
        test("http://snow%E2%98%83man", Url(scheme = "http", host = "snow\u2603man"))
    }

    @Test
    fun forceTrailingSlash() {
        test("https://foo.com/a/b/c/",
             Url(scheme = "https",
                 host = "foo.com",
                 path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c"))),
             true)
    }

    @Test
    fun forceTrailingSlashWithQueryParams() {
        test("https://foo.com/a/b/c/?foo=bar",
             Url(scheme = "https",
                 host = "foo.com",
                 path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c")),
                 query = Query.Structured("foo" to "bar")),
             true)
    }

    @Test
    fun forceTrailingSlashNoPathSegmentsWithMatrixParams() {
        test("https://foo.com/;m1=v1/",
             Url(scheme = "https", host = "foo.com", path = listOf(PathSegment("", "m1" to "v1"))),
             true)
    }

    @Test
    fun intermingledMatrixParamsAndPathSegments() {
        test("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2",
             Url(scheme = "http",
                 host = "foo.com",
                 path = listOf(PathSegment("seg1"),
                               PathSegment("seg2", "m1" to "v1"),
                               PathSegment("seg3", "m2" to "v2"))))
    }

    @Test
    fun unstructuredQueryWithNoSpecialChars() {
        test("http://foo.com?q", Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q")))
    }

    @Test
    fun unstructuredQueryWithOkSpecialChars() {
        test("http://foo.com?q?/&=", Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q?/&=")))
    }

    @Test
    fun unstructuredQueryWithEscapedSpecialChars() {
        test("http://foo.com?q%23%2B", Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q#+")))
    }

    private fun test(expectedUrlString: String, url: Url, forceTrailingSlash: Boolean = false) {
        val actual = url.toUrlString(forceTrailingSlash)
        assertEquals(expectedUrlString, actual)
        assertEquals(expectedUrlString, URI(actual).toString())
    }
}
