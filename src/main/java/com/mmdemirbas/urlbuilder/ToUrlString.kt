package com.mmdemirbas.urlbuilder


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
        else                                                                                                         -> {
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
