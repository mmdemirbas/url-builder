/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object ToUrlStringTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun TestCase.toUrlString() {
        val actual = input.toUrlString(forceTrailingSlash = forceTrailingSlash)
        assertEquals(expected, actual)
        assertEquals(expected, URI(actual).toString())
    }

    fun cases() = listOf(TestCase("http://foo.com", Url(scheme = "http", host = "foo.com")),
                         TestCase("http://foo.com:33", Url(scheme = "http", host = "foo.com", port = 33)),
                         TestCase("http://foo.com/seg1/seg2",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("seg1"), PathSegment("seg2")))),
                         TestCase(
                                 // RFC 1738 S3.3
                                 "http://foo.com/seg%2F%3B%3Fment/seg=&2",
                                 Url(scheme = "http",
                                     host = "foo.com",
                                     path = listOf(PathSegment("seg/;?ment"), PathSegment("seg=&2")))),
                         TestCase("http://foo.com/seg1/seg2/seg3",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("seg1"), PathSegment("seg2"), PathSegment("seg3")))),
                         TestCase("http://foo.com/;foo=bar",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("", listOf("foo" to "bar"))))),
                         TestCase("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("foo",
                                                                listOf("foo" to "bar", "res;=?#/erved" to "value")),
                                                    PathSegment("baz")))),
                         TestCase(
                                 // 1 UTF-16 char
                                 "http://foo.com/snowman/%E2%98%83",
                                 Url(scheme = "http",
                                     host = "foo.com",
                                     path = listOf(PathSegment("snowman"), PathSegment("\u2603")))),
                         TestCase(
                                 // musical G clef: 1d11e, has to be represented in surrogate pair form
                                 "http://foo.com/clef/%F0%9D%84%9E",
                                 Url(scheme = "http",
                                     host = "foo.com",
                                     path = listOf(PathSegment("clef"), PathSegment("\ud834\udd1e")))),
                         TestCase("http://foo.com?foo=bar",
                                  Url(scheme = "http", host = "foo.com", query = Query.Html4("foo" to "bar"))),
                         TestCase("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      query = Query.Html4("foo" to "bar",
                                                          "foo" to "bar2",
                                                          "baz" to "quux",
                                                          "baz" to "quux2"))),
                         TestCase("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      query = Query.Html4("foo" to "bar&=#baz", "foo" to "bar?/2"))),
                         TestCase("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      query = Query.Html4("foo" to "spa ce", "fo+o" to "plus+"))),
                         TestCase("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("has+plus", listOf("plusMtx" to "pl+us"))),
                                      query = Query.Html4("plusQp" to "pl+us"),
                                      fragment = "plus+frag")),
                         TestCase("http://foo.com?foo=bar#%23frag/?",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      query = Query.Html4("foo" to "bar"),
                                      fragment = "#frag/?")),
                         TestCase("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
                                  Url("https",
                                      "foo.bar.com",
                                      3333,
                                      listOf(PathSegment("foo"),
                                             PathSegment("bar", listOf("mtx1" to "val1", "mtx2" to "val2"))),
                                      query = Query.Html4("q1" to "v1", "q2" to "v2"),
                                      fragment = "zomg it's a fragment")),
                         TestCase("http://127.0.0.1", Url(scheme = "http", host = "127.0.0.1")),
                         TestCase("http://300.100.50.1", Url(scheme = "http", host = "300.100.50.1")),
                         TestCase("http://[::1]", Url(scheme = "http", host = "[::1]")),
                         TestCase("http://[2001:db8:85a3::8a2e:370:7334]",
                                  Url(scheme = "http", host = "[2001:db8:85a3::8a2e:370:7334]")),
                         TestCase("http://host%3Fname;", Url(scheme = "http", host = "host?name;")),
                         TestCase("http://snow%E2%98%83man", Url(scheme = "http", host = "snow\u2603man")),
                         TestCase("https://foo.com/a/b/c/",
                                  Url(scheme = "https",
                                      host = "foo.com",
                                      path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c"))),
                                  true),
                         TestCase("https://foo.com/a/b/c/?foo=bar",
                                  Url(scheme = "https",
                                      host = "foo.com",
                                      path = listOf(PathSegment("a"), PathSegment("b"), PathSegment("c")),
                                      query = Query.Html4("foo" to "bar")),
                                  true),
                         TestCase("https://foo.com/;m1=v1/",
                                  Url(scheme = "https",
                                      host = "foo.com",
                                      path = listOf(PathSegment("", listOf("m1" to "v1")))),
                                  true),
                         TestCase("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2",
                                  Url(scheme = "http",
                                      host = "foo.com",
                                      path = listOf(PathSegment("seg1"),
                                                    PathSegment("seg2", listOf("m1" to "v1")),
                                                    PathSegment("seg3", listOf("m2" to "v2"))))),
                         TestCase("http://foo.com?q",
                                  Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q"))),
                         TestCase("http://foo.com?q?/&=",
                                  Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q?/&="))),
                         TestCase("http://foo.com?q%23%2B",
                                  Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q#+"))))

    data class TestCase(val expected: String, val input: Url, val forceTrailingSlash: Boolean = false)
}
