/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import java.util.*
import java.util.regex.Pattern

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

private val h = escapeRegexSpecialChars("%" + UrlPart.RegName.safeChars)
private val p = escapeRegexSpecialChars("%" + UrlPart.Path.safeChars)
private val m = escapeRegexSpecialChars("%" + UrlPart.Matrix.safeChars)
private val q = escapeRegexSpecialChars("%" + UrlPart.QueryParam.safeChars)
private val u = escapeRegexSpecialChars("%" + UrlPart.UnstructuredQuery.safeChars)
private val f = escapeRegexSpecialChars("%" + UrlPart.Fragment.safeChars)

private fun escapeRegexSpecialChars(chars: String) =
        chars.replace(Regex("([\\^\\$\\+\\{\\}\\[\\]\\*\\(\\)\\?\\.\\-])"), "\\\\$1")

private val URL_PATTERN =
        Pattern.compile("(?<scheme>\\w+)://(?<host>[$h]+)(:(?<port>\\d+))?(?<path>(/[$p]*(;[$m]+=[$m]+)*;?)*)/?(\\?((?<queryParams>[$q]+=[$q]+(&[$q]+=[$q]+)*)|(?<unstructuredQuery>[$u]+)))?(#(?<fragment>[$f]+))?")

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
 * @throws MalformedInputException      if decoder is configured to report errors and malformed input is detected
 * @throws UnmappableCharacterException if decoder is configured to report errors and an unmappable character is
 * detected
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
fun parseUrl(input: CharSequence): Url {
    val schemeEnd = input.indexOf("://")
    if (schemeEnd < 0) throw IllegalArgumentException("Invalid URL: $input")

    val scheme = input.substring(0, schemeEnd)

    val hostStart = schemeEnd + 3
    val hostEnd = input.indexOfAny(":#/;?".toCharArray(), hostStart)
    val effectiveHostEnd = if (hostEnd < 0) input.length else hostEnd
    val host = input.substring(hostStart, effectiveHostEnd)

    val effectivePortStart =
            if (effectiveHostEnd < input.length && input[effectiveHostEnd] == ':') effectiveHostEnd + 1 else effectiveHostEnd
    val portEnd = input.indexOfAny("#/;?".toCharArray(), effectiveHostEnd)
    val effectivePortEnd = if (portEnd < 0) input.length else portEnd
    val portStr = input.substring(effectivePortStart, effectivePortEnd)

    val pathEnd = input.indexOfAny("#?".toCharArray(), effectivePortEnd)
    val effectivePathEnd = if (pathEnd < 0) input.length else pathEnd
    val path = input.substring(effectivePortEnd, effectivePathEnd)

    val queryEnd = input.indexOfAny("#".toCharArray(), effectivePathEnd)
    val effectiveQueryEnd = if (queryEnd < 0) input.length else queryEnd
    val queryStr = input.substring(effectivePathEnd, effectiveQueryEnd)

    val fragment =
            if (effectiveQueryEnd < input.length && input[effectiveQueryEnd] == '#') input.substring(effectiveQueryEnd + 1) else null

    val matcher = URL_PATTERN.matcher(input)
    if (!matcher.matches()) throw IllegalArgumentException("Invalid URL: $input")

    //    val scheme = matcher.group("scheme")
    //    val host = matcher.group("host")
    //    val portStr = matcher.group("port")
    //    val path = matcher.group("path")
    val queryParams = matcher.group("queryParams")
    val unstructuredQuery = matcher.group("unstructuredQuery")
    //    val fragment = matcher.group("fragment")

    val port = when {
        portStr.isNullOrEmpty() -> null
        else                    -> portStr.toInt()
    }

    val pathSegments = path.split('/').filter { it.isNotEmpty() }.map { pathSegment ->
        val chunks = pathSegment.split(';').filter { it.isNotEmpty() }
        PathSegment(decode(chunks.first()), chunks.drop(1).map { matrixChunk ->
            val parts = matrixChunk.split('=')
            if (parts.size != 2) throw MalformedURLException("Malformed matrix param: <$matrixChunk>")
            decode(parts[0]) to decode(parts[1])
        })
    }

    val query = when {
        queryParams != null && queryParams.isNotEmpty()             -> Query.Structured(queryParams.split('&').map { pair ->
            val parts = pair.split('=')
            if (parts.size != 2) throw MalformedURLException()
            decode(parts[0]) to decode(parts[1])
        })
        unstructuredQuery != null && unstructuredQuery.isNotEmpty() -> Query.Unstructured(decode(unstructuredQuery))
        else                                                        -> null
    }

    return Url(scheme = scheme,
               host = decode(host),
               port = port,
               path = pathSegments,
               query = query,
               fragment = fragment?.let { decode(it) })
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
 * @throws CharacterCodingException if decoding percent-encoded bytes fails and charsetDecoder is configured to
 * report errors
 */
@Throws(CharacterCodingException::class)
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
 * Encode the current builder state into a URL string.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
 *
 * @param forceTrailingSlash true to force the generated URL to have a trailing slash at the end of the path
 *
 * @return a well-formed URL string
 * @throws CharacterCodingException if character encoding fails and the encoder is configured to report errors
 */
@Throws(CharacterCodingException::class)
fun Url.toUrlString(forceTrailingSlash: Boolean = false): String {
    // host encoded as in RFC 3986 section 3.2.2
    val hostEncoded = when {
        IPV4_PATTERN.matches(host) || IPV6_PATTERN.matches(host) -> host
    // if it's a reg-name, it MUST be encoded as UTF-8 (regardless of the rest of the URL)
        else                                                     -> UrlPart.RegName.encode(host)
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
