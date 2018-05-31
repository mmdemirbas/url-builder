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
    fun Case.toUrlString() {
        val actual = url.toUrlString(forceTrailingSlash = forceTrailingSlash)
        assertEquals(expectedUrlString, actual)
        assertEquals(expectedUrlString, URI(actual).toString())
    }

    fun cases() = listOf(Case("no url part", "http://foo.com", Url(scheme = "http", host = "foo.com")),
                         Case("only port", "http://foo.com:33", Url(scheme = "http", host = "foo.com", port = 33)),
                         Case("only simple path",
                              "http://foo.com/seg1/seg2",
                              Url("http", "foo.com", PathSegment("seg1"), PathSegment("seg2"))),
                         Case("path with reserved chars",
                                 // RFC 1738 S3.3
                              "http://foo.com/seg%2F%3B%3Fment/seg=&2",
                              Url("http", "foo.com", PathSegment("seg/;?ment"), PathSegment("seg=&2"))),
                         Case("path segments",
                              "http://foo.com/seg1/seg2/seg3",
                              Url("http", "foo.com", PathSegment("seg1"), PathSegment("seg2"), PathSegment("seg3"))),
                         Case("matrix without path has leading slash",
                              "http://foo.com/;foo=bar",
                              Url("http", "foo.com", PathSegment("", "foo" to "bar"))),
                         Case("matrix with reserved",
                              "http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz",
                              Url("http",
                                  "foo.com",
                                  PathSegment("foo", "foo" to "bar", "res;=?#/erved" to "value"),
                                  PathSegment("baz"))),
                         Case("non-safe path segment",
                                 // 1 UTF-16 char
                              "http://foo.com/snowman/%E2%98%83",
                              Url("http", "foo.com", PathSegment("snowman"), PathSegment("\u2603"))),
                         Case("non-safe path segment - surrogate pair",
                                 // musical G clef: 1d11e, has to be represented in surrogate pair form
                              "http://foo.com/clef/%F0%9D%84%9E",
                              Url("http", "foo.com", PathSegment("clef"), PathSegment("\ud834\udd1e"))),
                         Case("only query params",
                              "http://foo.com?foo=bar",
                              Url(scheme = "http", host = "foo.com", query = Query.Html4("foo" to "bar"))),
                         Case("duplicate query params",
                              "http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2",
                              Url(scheme = "http",
                                  host = "foo.com",
                                  query = Query.Html4("foo" to "bar",
                                                      "foo" to "bar2",
                                                      "baz" to "quux",
                                                      "baz" to "quux2"))),
                         Case("query params",
                              "http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2",
                              Url(scheme = "http",
                                  host = "foo.com",
                                  query = Query.Html4("foo" to "bar&=#baz", "foo" to "bar?/2"))),
                         Case("query param with space and plus",
                              "http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B",
                              Url(scheme = "http",
                                  host = "foo.com",
                                  query = Query.Html4("foo" to "spa ce", "fo+o" to "plus+"))),
                         Case("plus in various parts",
                              "http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag",
                              Url("http",
                                  "foo.com",
                                  PathSegment("has+plus", "plusMtx" to "pl+us"),
                                  query = Query.Html4("plusQp" to "pl+us"),
                                  fragment = "plus+frag")),
                         Case("fragment",
                              "http://foo.com?foo=bar#%23frag/?",
                              Url(scheme = "http",
                                  host = "foo.com",
                                  query = Query.Html4("foo" to "bar"),
                                  fragment = "#frag/?")),
                         Case("all parts",
                              "https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
                              Url("https",
                                  "foo.bar.com",
                                  3333,
                                  PathSegment("foo"),
                                  PathSegment("bar", "mtx1" to "val1", "mtx2" to "val2"),
                                  query = Query.Html4("q1" to "v1", "q2" to "v2"),
                                  fragment = "zomg it's a fragment")),
                         Case("IPv4 literal", "http://127.0.0.1", Url(scheme = "http", host = "127.0.0.1")),
                         Case("bad IPv4 literal doesn't choke",
                              "http://300.100.50.1",
                              Url(scheme = "http", host = "300.100.50.1")),
                         Case("IPv6 literal localhost", "http://[::1]", Url(scheme = "http", host = "[::1]")),
                         Case("IPv6 literal",
                              "http://[2001:db8:85a3::8a2e:370:7334]",
                              Url(scheme = "http", host = "[2001:db8:85a3::8a2e:370:7334]")),
                         Case("non-safe reg-name - single-byte",
                              "http://host%3Fname;",
                              Url(scheme = "http", host = "host?name;")),
                         Case("non-safe reg-name - multi-byte",
                              "http://snow%E2%98%83man",
                              Url(scheme = "http", host = "snow\u2603man")),
                         Case("force trailing slash",
                              "https://foo.com/a/b/c/",
                              Url("https", "foo.com", PathSegment("a"), PathSegment("b"), PathSegment("c")),
                              true),
                         Case("force trailing slash with query params",
                              "https://foo.com/a/b/c/?foo=bar",
                              Url("https",
                                  "foo.com",
                                  PathSegment("a"),
                                  PathSegment("b"),
                                  PathSegment("c"),
                                  query = Query.Html4("foo" to "bar")),
                              true),
                         Case("force trailing slash without path and with matrix params",
                              "https://foo.com/;m1=v1/",
                              Url(scheme = "https", host = "foo.com", path = listOf(PathSegment("", "m1" to "v1"))),
                              true),
                         Case("intermingled matrix params and path segments",
                              "http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2",
                              Url("http",
                                  "foo.com",
                                  PathSegment("seg1"),
                                  PathSegment("seg2", "m1" to "v1"),
                                  PathSegment("seg3", "m2" to "v2"))),
                         Case("unstructured query with no special char",
                              "http://foo.com?q",
                              Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q"))),
                         Case("unstructured query with OK special chars",
                              "http://foo.com?q?/&=",
                              Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q?/&="))),
                         Case("unstructured query with escaped special chars",
                              "http://foo.com?q%23%2B",
                              Url(scheme = "http", host = "foo.com", query = Query.Unstructured("q#+"))))

    data class Case(val name: String,
                    val expectedUrlString: String,
                    val url: Url,
                    val forceTrailingSlash: Boolean = false)
}
