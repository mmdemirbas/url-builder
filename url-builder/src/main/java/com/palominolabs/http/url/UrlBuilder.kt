/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.net.URL
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.text.Charsets.UTF_8

/**
 * Builder for urls with url-encoding applied to path, query param, etc.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
 *
 * Create a URL with UTF-8 encoding.
 *
 * @param scheme scheme (e.g. http)
 * @param host   host in any of the valid syntaxes: reg-name (a dns name, e.g. foo.com), ipv4 literal (1.2.3.4),
 * ipv6 literal ([::1]), excluding IPvFuture since no one uses that in practice
 * @param port   null or a positive integer
 */
class UrlBuilder(private val scheme: String, private val host: String, private val port: Int? = null) {

    /////////  scheme://host:port/path?queryParam=value&queryParam2=value2#fragment
    /////////  scheme://host:port/path?unstructuredQuery


    /**
     * Create a UrlBuilder initialized with the contents of a [URL].
     *
     * The query string will be parsed into HTML4 query params if it can be separated into a
     * `&`-separated sequence of `key=value` pairs. The sequence of query params can then be
     * appended to by continuing to call [UrlBuilder.queryParam]. The concept of query params is
     * only part of the HTML spec (and common HTTP usage), though, so it's perfectly legal to have a query string that
     * is in some other form. To represent this case, if the aforementioned param-parsing attempt fails, the query
     * string will be treated as just a monolithic, unstructured, string. In this case,
     * calls to [UrlBuilder.queryParam] on the resulting instance will throw [IllegalStateException], and only calls
     * to [UrlBuilder.unstructuredQuery]}, which replaces the entire query string, are allowed.
     *
     * @param url            url to initialize builder with
     * @param charsetDecoder the decoder to decode encoded bytes with (except for reg names, which are always UTF-8)
     * @return a UrlBuilder containing the host, path, etc. from the url
     * @throws CharacterCodingException if decoding percent-encoded bytes fails and charsetDecoder is configured to
     * report errors
     * @see UrlBuilder.fromUrl
     */
    @Throws(CharacterCodingException::class) @JvmOverloads constructor(url: URL,
                                                                       charset: Charset = UTF_8) : this(url.protocol,
                                                                                                        decode(url.host),
                                                                                                        url.port) {
        url.path.split('/').filter { it.isNotEmpty() }.forEach { pathChunk ->
            when {
                pathChunk[0] == ';' -> {
                    pathSegment("")
                    // empty path segment, but matrix params
                    pathChunk.substring(1).split(';').dropLastWhile { it.isEmpty() }.forEach { matrixChunk ->
                        populateMatrixParam(charset, matrixChunk)
                    }
                }
                else                -> {
                    // otherwise, path chunk is non empty and does not start with a ';'
                    val matrixChunks = pathChunk.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    // first chunk is always the path segment. If there is a trailing ; and no matrix params, the ; will
                    // not be included in the final url.
                    pathSegment(decode(matrixChunks[0], charset))

                    // if there any other chunks, they're matrix param pairs
                    matrixChunks.drop(1).forEach { populateMatrixParam(charset, it) }
                }
            }
        }

        if (url.query != null) {
            val q = url.query

            // try to parse into &-separated key=value pairs
            val pairs = ArrayList<Pair<String, String>>()
            var parseOk = true

            for (queryChunk in q.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val queryParamChunks = queryChunk.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (queryParamChunks.size != 2) {
                    parseOk = false
                    break
                }
                pairs.add(Pair(decode(queryParamChunks[0], charset), decode(queryParamChunks[1], charset)))
            }

            when {
                parseOk -> pairs.forEach { (key, value) -> queryParam(key, value) }
                else    -> unstructuredQuery(decode(q, charset))
            }
        }

        if (url.ref != null) fragment(decode(url.ref, charset))
    }

    @Throws(CharacterCodingException::class)
    private fun populateMatrixParam(charset: Charset, pathMatrixChunk: String) {
        val pair = pathMatrixChunk.split('=').dropLastWhile { it.isEmpty() }.toTypedArray()
        when {
            pair.size == 2 -> matrixParam(decode(pair[0], charset), decode(pair[1], charset))
            else           -> throw IllegalArgumentException("Malformed matrix param: <$pathMatrixChunk>")
        }
    }

    private var forceTrailingSlash = false

    private val pathSegments = mutableListOf<PathSegment>()
    /** If this is non-null, queryParams must be empty, and vice versa. */
    private var unstructuredQuery: String? = null
    private val queryParams = mutableListOf<Pair<String, String>>()
    private var fragment: String? = null

    /**
     * Add a path segment.
     *
     * @param segment a path segment
     * @return this
     */
    fun pathSegment(segment: String): UrlBuilder {
        pathSegments.add(PathSegment(segment))
        return this
    }

    /**
     * Add multiple path segments. Equivalent to successive calls to [UrlBuilder.pathSegment].
     *
     * @param segments path segments
     * @return this
     */
    fun pathSegments(vararg segments: String): UrlBuilder {
        segments.forEach { pathSegment(it) }
        return this
    }

    /**
     * Add a matrix param to the last added path segment. If no segments have been added, the param will be added to the
     * root. Matrix params will be encoded in the order added.
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    fun matrixParam(name: String, value: String): UrlBuilder {
        if (pathSegments.isEmpty()) {
            // create an empty path segment to represent a matrix param applied to the root
            pathSegment("")
        }
        pathSegments.last().matrixParams.add(Pair(name, value))
        return this
    }

    /**
     * Force the generated URL to have a trailing slash at the end of the path.
     *
     * @return this
     */
    fun forceTrailingSlash(): UrlBuilder {
        forceTrailingSlash = true
        return this
    }

    /**
     * Set the complete query string of arbitrary structure. This is useful when you want to specify a query string that
     * is not of key=value format. If the query has previously been set via this method, subsequent calls will overwrite
     * that query.
     *
     * If you use this method, or create a builder from a URL whose query is not parseable into query param pairs, you
     * cannot subsequently use [UrlBuilder.queryParam]. See [UrlBuilder.fromUrl].
     *
     * @param query Complete URI query, as specified by https://tools.ietf.org/html/rfc3986#section-3.4
     * @return this
     */
    fun unstructuredQuery(query: String): UrlBuilder {
        when {
            queryParams.isEmpty() -> {
                unstructuredQuery = query
                return this
            }
            else                  -> throw IllegalStateException("Cannot call unstructuredQuery() when this already has queryParam pairs specified")
        }
    }

    /**
     * Add an HTML query parameter. Query parameters will be encoded in the order added.
     *
     * Using query strings to encode key=value pairs is not part of the URI/URL specification; it is specified by
     * http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
     *
     * If you use this method to build a query string, or created this builder from a url with a query string that can
     * successfully be parsed into query param pairs, you cannot subsequently use [UrlBuilder.unstructuredQuery].
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    fun queryParam(name: String, value: String): UrlBuilder {
        when {
            unstructuredQuery == null -> {
                queryParams.add(Pair(name, value))
                return this
            }
            else                      -> throw IllegalStateException("Cannot call queryParam() when this already has an unstructured query specified")
        }
    }

    /**
     * Clear the unstructured query and any query params.
     *
     * Since the query / query param situation is a little complicated, this method will let you remove all query
     * information and start again from scratch. This may be useful when taking an existing url, parsing it into a
     * builder, and then re-doing its query params, for instance.
     *
     * @return this
     */
    fun clearQuery(): UrlBuilder {
        queryParams.clear()
        unstructuredQuery = null
        return this
    }

    /**
     * Set the fragment.
     *
     * @param fragment fragment string
     * @return this
     */
    fun fragment(fragment: String): UrlBuilder {
        this.fragment = fragment
        return this
    }

    /**
     * Encode the current builder state into a URL string.
     *
     * @return a well-formed URL string
     * @throws CharacterCodingException if character encoding fails and the encoder is configured to report errors
     */
    @Throws(CharacterCodingException::class)
    fun toUrlString(): String {
        val buf = StringBuilder("$scheme://${encodeHost(host)}")
        if (port != null && port >= 0) buf.append(":$port")

        pathSegments.forEach { pathSegment ->
            buf.append('/')
            buf.append(UrlPart.Path.encode(pathSegment.segment))
            pathSegment.matrixParams.forEach { (key, value) ->
                val keyEncoded = UrlPart.Matrix.encode(key)
                val valueEncoded = UrlPart.Matrix.encode(value)
                buf.append(";$keyEncoded=$valueEncoded")
            }
        }

        if (forceTrailingSlash) buf.append('/')

        if (!queryParams.isEmpty()) {
            buf.append("?")
            val qpIter = queryParams.iterator()
            while (qpIter.hasNext()) {
                val queryParam = qpIter.next()
                buf.append(UrlPart.QueryParam.encode(queryParam.first))
                buf.append('=')
                buf.append(UrlPart.QueryParam.encode(queryParam.second))
                if (qpIter.hasNext()) buf.append('&')
            }
        } else if (unstructuredQuery != null) {
            buf.append("?")
            buf.append(UrlPart.UnstructuredQuery.encode(unstructuredQuery!!))
        }

        if (fragment != null) {
            buf.append('#')
            buf.append(UrlPart.Fragment.encode(fragment!!))
        }

        return buf.toString()
    }

    /**
     * @param host original host string
     * @return host encoded as in RFC 3986 section 3.2.2
     */
    @Throws(CharacterCodingException::class)
    private fun encodeHost(host: String): String {
        return when {
            IPV4_PATTERN.matches(host) || IPV6_PATTERN.matches(host) -> host
            else                                                     -> {
                // it's a reg-name, which MUST be encoded as UTF-8 (regardless of the rest of the URL)
                UrlPart.RegName.encode(host)
            }
        }
    }

    /**
     * Bundle of a path segment name and any associated matrix params.
     */
    private class PathSegment(val segment: String) {
        val matrixParams = mutableListOf<Pair<String, String>>()
    }

    companion object {
        /**
         * IPv6 address, cribbed from http://stackoverflow.com/questions/46146/what-are-the-java-regular-expressions-for-matching-ipv4-and-ipv6-strings
         */
        private val IPV6_PATTERN =
                "\\A\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)]\\z".toRegex()

        /**
         * IPv4 dotted quad
         */
        private val IPV4_PATTERN =
                "\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z".toRegex()

    }
}
