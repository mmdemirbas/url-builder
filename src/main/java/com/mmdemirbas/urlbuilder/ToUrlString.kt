package com.mmdemirbas.urlbuilder

import com.mmdemirbas.urlbuilder.SafeChars.*
import java.nio.charset.Charset


/**
 * Returns string representation of the [Url] escaping unsafe characters properly.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
 *
 * @param forceTrailingSlash true to force the generated URL to have a trailing slash at the end of the path
 *
 * @return a well-formed URL string
 *
 * @throws IllegalArgumentException if character encoding fails
 */
fun Url.toUrlString(charset: Charset = Charsets.UTF_8, forceTrailingSlash: Boolean = false): String {
    // host encoded as in RFC 3986 section 3.2.2
    val hostEncoded = when {
        IPV4_PATTERN.matches(host) || IPV6_PATTERN.matches(host) -> host
        else                                                     -> {
            // if it's a reg-name, it MUST be encoded as UTF-8 (regardless of the rest of the URL)
            host.encodePercent(RegName)
        }
    }

    val buf = StringBuilder("$scheme://$hostEncoded")
    if (port != null && port >= 0) buf.append(":$port")

    path.forEach { pathSegment ->
        buf.append('/')
        buf.append(pathSegment.segment.encodePercent(Path, charset))
        buf.append(pathSegment.matrixParams.joinToString("") { (name, value) ->
            val nameEncoded = name.encodePercent(Matrix, charset)
            val valueEncoded = value.encodePercent(Matrix, charset)
            ";$nameEncoded=$valueEncoded"
        })
    }

    if (forceTrailingSlash) buf.append('/')

    when (query) {
        is Query.Html4        -> {
            buf.append("?")
            buf.append(query.params.joinToString("&") { (name, value) ->
                val nameEncoded = name.encodePercent(QueryParam, charset)
                val valueEncoded = value.encodePercent(QueryParam, charset)
                "$nameEncoded=$valueEncoded"
            })
        }
        is Query.Unstructured -> {
            buf.append("?")
            buf.append(query.query.encodePercent(UnstructuredQuery, charset))
        }
    }

    if (fragment != null) {
        buf.append('#')
        buf.append(fragment.encodePercent(Fragment, charset))
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
