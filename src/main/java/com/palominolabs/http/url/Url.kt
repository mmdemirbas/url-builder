/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.net.URL
import java.nio.charset.Charset
import java.util.*

/**
 * Url in `scheme://host:port/path/path/path?query#fragment` format.
 *
 * @property scheme String
 * @property host host in any of the valid syntaxes: reg-name (a dns name, e.g. foo.com), ipv4 literal (1.2.3.4),
 * ipv6 literal ([::1]), excluding IPvFuture since no one uses that in practice
 * @property port `null` or positive integer
 * @property path List<PathSegment>
 * @property query Query?
 * @property fragment String?
 * @constructor
 */
data class Url(val scheme: String,
               val host: String,
               val port: Int? = null,
               val path: List<PathSegment> = emptyList(),
               val query: Query? = null,
               val fragment: String? = null)

data class PathSegment(val segment: String, val matrixParams: List<Pair<String, String>> = emptyList()) {
    constructor(segment: String, vararg matrixParams: Pair<String, String>) : this(segment, matrixParams.asList())
}

sealed class Query {
    /**
     * Query in `?a=b&c=d` format.
     *
     * Using query strings to encode key=value pairs is not part of the URI/URL specification; it is specified by
     * http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
     */
    data class Structured(val params: List<Pair<String, String>>) : Query() {
        constructor(vararg params: Pair<String, String>) : this(params.asList())
    }

    /**
     * Set the complete query string of arbitrary structure. This is useful when you want to specify a query string that
     * is not of key=value format. If the query has previously been set via this method, subsequent calls will overwrite
     * that query.
     */
    data class Unstructured(val query: String) : Query()
}

/**
 * Create a UrlBuilder initialized with the contents of a [URL].
 *
 * The query string will be parsed into HTML4 query params if it can be separated into a
 * `&`-separated sequence of `key=value` pairs. The concept of query params is
 * only part of the HTML spec (and common HTTP usage), though, so it's perfectly legal to have a query string that
 * is in some other form. To represent this case, if the aforementioned param-parsing attempt fails, the query
 * string will be treated as just a monolithic, unstructured, string.
 *
 * @param url     url to initialize builder with
 * @param charset the charset to decode encoded bytes with (except for reg names, which are always UTF-8)
 *
 * @return a UrlBuilder containing the host, path, etc. from the url
 *
 * @throws IllegalArgumentException if decoding percent-encoded bytes fails and charsetDecoder is configured to
 * report errors
 */
@Throws(IllegalArgumentException::class)
fun parseUrl(url: URL, charset: Charset = Charsets.UTF_8): Url {
    val pathSegments = url.path.split('/').filterNot { it.isEmpty() }.map { pathChunk ->
        val chunks = pathChunk.split(';')
        PathSegment(decode(chunks.first(), charset), chunks.drop(1).filterNot { it.isEmpty() }.map {
            val parts = it.split('=').filterNot { it.isEmpty() }
            when {
                parts.size == 2 -> Pair(decode(parts[0], charset), decode(parts[1], charset))
                else            -> throw IllegalArgumentException("Malformed matrix param: <$it>")
            }
        })
    }

    val query = url.query?.let {
        // try to parse into &-separated key=value pairs
        val pairs = ArrayList<Pair<String, String>>()
        var parseOk = true

        for (queryChunk in url.query.split('&').filterNot { it.isEmpty() }) {
            val queryParamChunks = queryChunk.split('=').filterNot { it.isEmpty() }
            if (queryParamChunks.size != 2) {
                parseOk = false
                break
            }
            pairs.add(Pair(decode(queryParamChunks[0], charset), decode(queryParamChunks[1], charset)))
        }

        when {
            parseOk -> Query.Structured(pairs)
            else    -> Query.Unstructured(decode(url.query, charset))
        }
    }

    val fragment = url.ref?.let { decode(url.ref, charset) }
    return Url(url.protocol, decode(url.host), url.port, pathSegments, query, fragment)
}

/**
 * Decodes percent-encoded (%XX) Unicode text.
 *
 * @param input Input with %-encoded representation of characters in this instance's configured character set, e.g.
 * "%20" for a space character
 * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
 * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
 * @param charsetDecoder            Charset to decode bytes into chars with
 *
 * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
 *
 * @throws IllegalArgumentException if decoder is configured to report errors and malformed input is detected or an unmappable character is detected
 */
@Throws(IllegalArgumentException::class)
fun parseUrl(input: String, charset: Charset = Charsets.UTF_8): Url {
    val (scheme, schemeToken, schemeRest) = input.splitIntoTwoParts("://")
    val (host, hostToken, hostRest) = schemeRest.splitIntoTwoParts(":#/;?".toCharArray())
    val hostOrPortRest: String
    val port: Int?
    if (hostToken == ':') {
        val (portStr, portToken, portRest) = hostRest.splitIntoTwoParts("#/;?".toCharArray())
        port = when {
            portStr.isEmpty() -> null
            else              -> portStr.toInt()
        }
        hostOrPortRest = portRest
    } else {
        port = null
        hostOrPortRest = hostRest
    }
    val (pathStr, pathToken, pathRest) = hostOrPortRest.splitIntoTwoParts("#?".toCharArray())
    val path = pathStr.split('/').filter { it.isNotEmpty() }.map { pathSegment ->
        val chunks = pathSegment.split(';')
        PathSegment(decode(chunks.first(), charset), chunks.drop(1).filter { it.isNotEmpty() }.map { matrixChunk ->
            val parts = matrixChunk.split('=')
            if (parts.size != 2) throw IllegalArgumentException("Malformed matrix param: <$matrixChunk>")
            decode(parts[0], charset) to decode(parts[1], charset)
        })
    }
    val query: Query?
    val token: Char?
    val rest: String
    if (pathToken == '?') {
        val (queryParams, queryToken, queryRest) = pathRest.splitIntoTwoParts("#".toCharArray())
        query = when {
            queryParams.isNotEmpty() -> {
                val chunks = queryParams.split('&')
                val splittedChunks = chunks.map { it.split('=') }
                val structured = splittedChunks.all { it.size == 2 }
                when {
                    structured -> Query.Structured(splittedChunks.map {
                        decode(it[0], charset) to decode(it[1], charset)
                    })
                    else       -> Query.Unstructured(decode(queryParams, charset))
                }
            }
            else                     -> null
        }
        token = queryToken
        rest = queryRest
    } else {
        query = null
        token = pathToken
        rest = pathRest
    }
    val fragment = if (token == '#') decode(rest, charset) else null

    return Url(scheme = scheme, host = decode(host), port = port, path = path, query = query, fragment = fragment)
}

private fun String.splitIntoTwoParts(delimiter: String): Triple<String, String?, String> {
    val index = indexOf(delimiter)
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(this.substring(0, index), delimiter, this.substring(index + delimiter.length))
    }
}

private fun String.splitIntoTwoParts(delimiters: CharArray): Triple<String, Char?, String> {
    val index = indexOfAny(delimiters)
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(this.substring(0, index), this[index], this.substring(index + 1))
    }
}

/**
 * Encode the current builder state into a URL string.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
 *
 * @param forceTrailingSlash true to force the generated URL to have a trailing slash at the end of the path
 *
 * @return a well-formed URL string
 * @throws IllegalArgumentException if character encoding fails and the encoder is configured to report errors
 */
@Throws(IllegalArgumentException::class)
fun Url.toUrlString(forceTrailingSlash: Boolean = false): String {
    // host encoded as in RFC 3986 section 3.2.2
    val hostEncoded = when {
        IPV4_PATTERN.matches(host) || IPV6_PATTERN.matches(host) -> host
        else                                                     -> {
            // if it's a reg-name, it MUST be encoded as UTF-8 (regardless of the rest of the URL)
            UrlPart.RegName.encode(host)
        }
    }

    val buf = StringBuilder("$scheme://$hostEncoded")
    if (port != null && port >= 0) buf.append(":$port")

    path.forEach { pathSegment ->
        buf.append('/')
        buf.append(UrlPart.Path.encode(pathSegment.segment))
        buf.append(pathSegment.matrixParams.joinToString("") { (name, value) ->
            val nameEncoded = UrlPart.Matrix.encode(name)
            val valueEncoded = UrlPart.Matrix.encode(value)
            ";$nameEncoded=$valueEncoded"
        })
    }

    if (forceTrailingSlash) buf.append('/')

    when (query) {
        is Query.Structured   -> {
            buf.append("?")
            buf.append(query.params.joinToString("&") { (name, value) ->
                val nameEncoded = UrlPart.QueryParam.encode(name)
                val valueEncoded = UrlPart.QueryParam.encode(value)
                "$nameEncoded=$valueEncoded"
            })
        }
        is Query.Unstructured -> {
            buf.append("?")
            buf.append(UrlPart.UnstructuredQuery.encode(query.query))
        }
    }

    if (fragment != null) {
        buf.append('#')
        buf.append(UrlPart.Fragment.encode(fragment))
    }

    return buf.toString()
}

/**
 * IPv6 address, cribbed from http://stackoverflow.com/questions/46146/what-are-the-java-regular-expressions-for-matching-ipv4-and-ipv6-strings
 */
private val IPV6_PATTERN =
        "\\A\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)]\\z".toRegex()

/**
 * IPv4 dotted quad
 */
private val IPV4_PATTERN = "\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z".toRegex()
