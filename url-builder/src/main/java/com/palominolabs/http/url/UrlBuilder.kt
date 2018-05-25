/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import com.google.common.base.Charsets.UTF_8
import com.google.common.collect.Lists
import org.apache.commons.lang3.tuple.Pair
import java.net.URL
import java.nio.charset.CharacterCodingException
import java.nio.charset.CharsetDecoder
import java.util.*
import java.util.regex.Pattern
import javax.annotation.concurrent.NotThreadSafe

/**
 * Builder for urls with url-encoding applied to path, query param, etc.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
 */
@NotThreadSafe
class UrlBuilder
/**
 * Create a URL with UTF-8 encoding.
 *
 * @param scheme scheme (e.g. http)
 * @param host   host (e.g. foo.com or 1.2.3.4 or [::1])
 * @param port   null or a positive integer
 */
private constructor(private val scheme: String, private val host: String, private val port: Int?) {

    private val queryParams = Lists.newArrayList<Pair<String, String>>()

    /**
     * If this is non-null, queryParams must be empty, and vice versa.
     */
    private var unstructuredQuery: String? = null

    private val pathSegments = Lists.newArrayList<PathSegment>()

    private val pathEncoder = UrlPercentEncoders.pathEncoder
    private val regNameEncoder = UrlPercentEncoders.regNameEncoder
    private val matrixEncoder = UrlPercentEncoders.matrixEncoder
    private val queryParamEncoder = UrlPercentEncoders.queryParamEncoder
    private val unstructuredQueryEncoder = UrlPercentEncoders.unstructuredQueryEncoder
    private val fragmentEncoder = UrlPercentEncoders.fragmentEncoder

    private var fragment: String? = null

    private var forceTrailingSlash = false

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
        for (segment in segments) {
            pathSegment(segment)
        }

        return this
    }

    /**
     * Add an HTML query parameter. Query parameters will be encoded in the order added.
     *
     * Using query strings to encode key=value pairs is not part of the URI/URL specification; it is specified by
     * http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
     *
     * If you use this method to build a query string, or created this builder from a url with a query string that can
     * successfully be parsed into query param pairs, you cannot subsequently use [ ][UrlBuilder.unstructuredQuery]. See [UrlBuilder.fromUrl].
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    fun queryParam(name: String, value: String): UrlBuilder {
        if (unstructuredQuery != null) {
            throw IllegalStateException("Cannot call queryParam() when this already has an unstructured query specified")
        }

        queryParams.add(Pair.of(name, value))
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
        if (!queryParams.isEmpty()) {
            throw IllegalStateException("Cannot call unstructuredQuery() when this already has queryParam pairs specified")
        }

        unstructuredQuery = query

        return this
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

        val seg = pathSegments[pathSegments.size - 1]
        seg.matrixParams.add(Pair.of(name, value))
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
     * Force the generated URL to have a trailing slash at the end of the path.
     *
     * @return this
     */
    fun forceTrailingSlash(): UrlBuilder {
        forceTrailingSlash = true
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
        val buf = StringBuilder()

        buf.append(scheme)
        buf.append("://")

        buf.append(encodeHost(host))
        if (port != null) {
            buf.append(':')
            buf.append(port)
        }

        for (pathSegment in pathSegments) {
            buf.append('/')
            buf.append(pathEncoder.encode(pathSegment.segment))

            for ((key, value) in pathSegment.matrixParams) {
                buf.append(';')
                buf.append(matrixEncoder.encode(key))
                buf.append('=')
                buf.append(matrixEncoder.encode(value))
            }
        }

        if (forceTrailingSlash) {
            buf.append('/')
        }

        if (!queryParams.isEmpty()) {
            buf.append("?")
            val qpIter = queryParams.iterator()
            while (qpIter.hasNext()) {
                val queryParam = qpIter.next()
                buf.append(queryParamEncoder.encode(queryParam.key))
                buf.append('=')
                buf.append(queryParamEncoder.encode(queryParam.value))
                if (qpIter.hasNext()) {
                    buf.append('&')
                }
            }
        } else if (unstructuredQuery != null) {
            buf.append("?")
            buf.append(unstructuredQueryEncoder.encode(unstructuredQuery!!))
        }

        if (fragment != null) {
            buf.append('#')
            buf.append(fragmentEncoder.encode(fragment!!))
        }

        return buf.toString()
    }

    /**
     * @param host original host string
     * @return host encoded as in RFC 3986 section 3.2.2
     */
    @Throws(CharacterCodingException::class)
    private fun encodeHost(host: String): String {
        // matching order: IP-literal, IPv4, reg-name
        return if (IPV4_PATTERN.matcher(host).matches() || IPV6_PATTERN.matcher(host).matches()) {
            host
        } else regNameEncoder.encode(host)

        // it's a reg-name, which MUST be encoded as UTF-8 (regardless of the rest of the URL)
    }

    /**
     * Bundle of a path segment name and any associated matrix params.
     */
    private class PathSegment internal constructor(val segment: String) {
        val matrixParams = Lists.newArrayList<Pair<String, String>>()
    }

    companion object {

        /**
         * IPv6 address, cribbed from http://stackoverflow.com/questions/46146/what-are-the-java-regular-expressions-for-matching-ipv4-and-ipv6-strings
         */
        private val IPV6_PATTERN =
                Pattern.compile("\\A\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)]\\z")

        /**
         * IPv4 dotted quad
         */
        private val IPV4_PATTERN =
                Pattern.compile("\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z")

        /**
         * Create a URL with an null port and UTF-8 encoding.
         *
         * @param scheme scheme (e.g. http)
         * @param host   host in any of the valid syntaxes: reg-name (a dns name), ipv4 literal (1.2.3.4), ipv6 literal
         * ([::1]), excluding IPvFuture since no one uses that in practice
         * @return a url builder
         * @see UrlBuilder.forHost
         */
        fun forHost(scheme: String, host: String): UrlBuilder {
            return UrlBuilder(scheme, host, null)
        }

        /**
         * @param scheme scheme (e.g. http)
         * @param host   host in any of the valid syntaxes: reg-name ( a dns name), ipv4 literal (1.2.3.4), ipv6 literal
         * ([::1]), excluding IPvFuture since no one uses that in practice
         * @param port   port
         * @return a url builder
         */
        fun forHost(scheme: String, host: String, port: Int): UrlBuilder {
            return UrlBuilder(scheme, host, port)
        }

        /**
         * Create a UrlBuilder initialized with the contents of a [URL].
         *
         * The query string will be parsed into HTML4 query params if it can be separated into a
         * `&`-separated sequence of `key=value` pairs. The sequence of query params can then be
         * appended to by continuing to call [UrlBuilder.queryParam]. The concept of query params is
         * only part of the HTML spec (and common HTTP usage), though, so it's perfectly legal to have a query string that
         * is in some other form. To represent this case, if the aforementioned param-parsing attempt fails, the query
         * string will be treated as just a monolithic, unstructured, string. In this case, calls to [ ][UrlBuilder.queryParam] on the resulting instance will throw IllegalStateException, and only calls
         * to [UrlBuilder.unstructuredQuery]}, which replaces the entire query string, are allowed.
         *
         * @param url            url to initialize builder with
         * @param charsetDecoder the decoder to decode encoded bytes with (except for reg names, which are always UTF-8)
         * @return a UrlBuilder containing the host, path, etc. from the url
         * @throws CharacterCodingException if decoding percent-encoded bytes fails and charsetDecoder is configured to
         * report errors
         * @see UrlBuilder.fromUrl
         */
        @Throws(CharacterCodingException::class)
        @JvmOverloads
        fun fromUrl(url: URL, charsetDecoder: CharsetDecoder = UTF_8.newDecoder()): UrlBuilder {

            val decoder = PercentDecoder(charsetDecoder)
            // reg names must be encoded UTF-8
            val regNameDecoder: PercentDecoder
            if (charsetDecoder.charset() == UTF_8) {
                regNameDecoder = decoder
            } else {
                regNameDecoder = PercentDecoder(UTF_8.newDecoder())
            }

            var port: Int? = url.port
            if (port == -1) {
                port = null
            }

            val builder = UrlBuilder(url.protocol, regNameDecoder.decode(url.host), port)

            buildFromPath(builder, decoder, url)

            buildFromQuery(builder, decoder, url)

            if (url.ref != null) {
                builder.fragment(decoder.decode(url.ref))
            }

            return builder
        }

        /**
         * Populate a url builder based on the query of a url
         *
         * @param builder builder
         * @param decoder decoder
         * @param url     url
         * @throws CharacterCodingException
         */
        @Throws(CharacterCodingException::class)
        private fun buildFromQuery(builder: UrlBuilder, decoder: PercentDecoder, url: URL) {
            if (url.query != null) {
                val q = url.query

                // try to parse into &-separated key=value pairs
                val pairs = ArrayList<Pair<String, String>>()
                var parseOk = true

                for (queryChunk in q.split("&".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                    val queryParamChunks =
                            queryChunk.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                    if (queryParamChunks.size != 2) {
                        parseOk = false
                        break
                    }

                    pairs.add(Pair.of(decoder.decode(queryParamChunks[0]), decoder.decode(queryParamChunks[1])))
                }

                if (parseOk) {
                    for ((key, value) in pairs) {
                        builder.queryParam(key, value)
                    }
                } else {
                    builder.unstructuredQuery(decoder.decode(q))
                }
            }
        }

        /**
         * Populate the path segments of a url builder from a url
         *
         * @param builder builder
         * @param decoder decoder
         * @param url     url
         * @throws CharacterCodingException
         */
        @Throws(CharacterCodingException::class)
        private fun buildFromPath(builder: UrlBuilder, decoder: PercentDecoder, url: URL) {
            for (pathChunk in url.path.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                if (pathChunk == "") {
                    continue
                }

                if (pathChunk.get(0) == ';') {
                    builder.pathSegment("")
                    // empty path segment, but matrix params
                    for (matrixChunk in pathChunk.substring(1).split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                        buildFromMatrixParamChunk(decoder, builder, matrixChunk)
                    }

                    continue
                }

                // otherwise, path chunk is non empty and does not start with a ';'

                val matrixChunks = pathChunk.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                // first chunk is always the path segment. If there is a trailing ; and no matrix params, the ; will
                // not be included in the final url.
                builder.pathSegment(decoder.decode(matrixChunks[0]))

                // if there any other chunks, they're matrix param pairs
                for (i in 1 until matrixChunks.size) {
                    buildFromMatrixParamChunk(decoder, builder, matrixChunks[i])
                }
            }
        }

        @Throws(CharacterCodingException::class)
        private fun buildFromMatrixParamChunk(decoder: PercentDecoder, ub: UrlBuilder, pathMatrixChunk: String) {
            val mtxPair = pathMatrixChunk.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (mtxPair.size != 2) {
                throw IllegalArgumentException("Malformed matrix param: <$pathMatrixChunk>")
            }

            val mtxName = mtxPair[0]
            val mtxVal = mtxPair[1]
            ub.matrixParam(decoder.decode(mtxName), decoder.decode(mtxVal))
        }
    }
}
/**
 * Calls [UrlBuilder.fromUrl] with a UTF-8 CharsetDecoder. The same semantics about the
 * query string apply.
 *
 * @param url url to initialize builder with
 * @return a UrlBuilder containing the host, path, etc. from the url
 * @throws CharacterCodingException if char decoding fails
 * @see UrlBuilder.fromUrl
 */
