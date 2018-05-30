package com.mmdemirbas.urlbuilder

import java.net.URL
import java.nio.charset.Charset
import java.util.*

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
        PathSegment(decode(chunks.first(), charset),
                                              chunks.drop(1).filterNot { it.isEmpty() }.map {
                                                  val parts = it.split('=').filterNot { it.isEmpty() }
                                                  when {
                                                      parts.size == 2 -> Pair(decode(parts[0],
                                                                                                               charset),
                                                                              decode(parts[1],
                                                                                                               charset))
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
            pairs.add(Pair(decode(queryParamChunks[0], charset),
                           decode(queryParamChunks[1], charset)))
        }

        when {
            parseOk -> Query.Structured(pairs)
            else    -> Query.Unstructured(decode(url.query,
                                                                                                     charset))
        }
    }

    val fragment = url.ref?.let { decode(url.ref, charset) }
    return Url(url.protocol,
                                         decode(url.host),
                                         url.port,
                                         pathSegments,
                                         query,
                                         fragment)
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
        PathSegment(decode(chunks.first(), charset),
                                              chunks.drop(1).filter { it.isNotEmpty() }.map { matrixChunk ->
                                                  val parts = matrixChunk.split('=')
                                                  if (parts.size != 2) throw IllegalArgumentException("Malformed matrix param: <$matrixChunk>")
                                                  decode(parts[0],
                                                                                   charset) to decode(
                                                          parts[1],
                                                          charset)
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
                        decode(it[0], charset) to decode(it[1],
                                                                                                             charset)
                    })
                    else       -> Query.Unstructured(decode(
                            queryParams,
                            charset))
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

    return Url(scheme = scheme,
                                         host = decode(host),
                                         port = port,
                                         path = path,
                                         query = query,
                                         fragment = fragment)
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
