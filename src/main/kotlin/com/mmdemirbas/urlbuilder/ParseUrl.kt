package com.mmdemirbas.urlbuilder

import java.net.URL
import java.nio.charset.Charset

@JvmOverloads
fun URL.toUrl(charset: Charset = Charsets.UTF_8): Url {
    return Url(protocol,
               host.decodePercent(Charsets.UTF_8),
               port,
               path.orEmpty().toPathSegments(charset),
               query.orEmpty().toQuery(charset),
               ref?.let { ref.decodePercent(charset) })
}

@JvmOverloads
fun String.toUrl(charset: Charset = Charsets.UTF_8): Url {
    val (scheme, _, schemeRest) = splitBy("://")
    val (host, hostToken, hostRest) = schemeRest.splitByAny(":#/;?")
    val (port, hostOrPortRest) = when (hostToken) {
        ':'  -> {
            val (portStr, _, portRest) = hostRest.splitByAny("#/;?")
            (if (portStr.isEmpty()) null else portStr.toInt()) to portRest
        }
        else -> null to hostRest
    }
    val (pathStr, pathToken, pathRest) = hostOrPortRest.splitByAny("#?")
    val path = pathStr.toPathSegments(charset)
    val (query, token, rest) = when (pathToken) {
        '?'  -> {
            val (queryParams, queryToken, queryRest) = pathRest.splitByAny("#")
            Triple(queryParams.toQuery(charset), queryToken, queryRest)
        }
        else -> Triple(null, pathToken, pathRest)
    }
    val fragment = if (token == '#') rest.decodePercent(charset) else null
    return Url(scheme = scheme,
               host = host.decodePercent(Charsets.UTF_8),
               port = port,
               path = path,
               query = query,
               fragment = fragment)
}

@JvmOverloads
fun String.toPathSegments(charset: Charset = Charsets.UTF_8) =
        orEmpty().split('/').filterNot { it.isEmpty() }.map { it.toPathSegment(charset) }

@JvmOverloads
fun String.toPathSegment(charset: Charset = Charsets.UTF_8): PathSegment {
    val chunks = split(';')
    return PathSegment(chunks.first().decodePercent(charset),
                       chunks.drop(1).filterNot { it.isEmpty() }.map { matrixChunk ->
                           val parts = matrixChunk.split('=')
                           when {
                               parts.size == 2 -> parts[0].decodePercent(charset) to parts[1].decodePercent(charset)
                               else            -> throw IllegalArgumentException("Malformed matrix param: <$matrixChunk>")
                           }
                       })
}

@JvmOverloads
fun String.toQuery(charset: Charset = Charsets.UTF_8) = when {
    isEmpty() -> null
    else      -> {
        val chunks = split('&')
        val splitted = chunks.map { it.split('=') }
        val structured = splitted.all { it.size == 2 }
        when {
            structured -> Query.Html4(splitted.map { it[0].decodePercent(charset) to it[1].decodePercent(charset) })
            else       -> Query.Unstructured(decodePercent(charset))
        }
    }
}

private fun String.splitBy(delimiter: String): Triple<String, String?, String> {
    val index = indexOf(delimiter)
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(substring(0, index), delimiter, substring(index + delimiter.length))
    }
}

private fun String.splitByAny(delimiters: String): Triple<String, Char?, String> {
    val index = indexOfAny(delimiters.toCharArray())
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(substring(0, index), this[index], substring(index + 1))
    }
}
