package com.mmdemirbas.urlbuilder

import java.net.URL
import java.nio.charset.Charset

@Throws(IllegalArgumentException::class)
fun parseUrl(url: URL, charset: Charset = Charsets.UTF_8): Url {
    return Url(url.protocol,
               decode(url.host),
               url.port,
               parsePath(url.path, charset),
               parseQuery(url.query, charset),
               url.ref?.let { decode(url.ref, charset) })
}

@Throws(IllegalArgumentException::class)
fun parseUrl(url: String, charset: Charset = Charsets.UTF_8): Url {
    val (scheme, _, schemeRest) = url.splitBy("://")
    val (host, hostToken, hostRest) = schemeRest.splitByAny(":#/;?")
    val (port, hostOrPortRest) = when {
        hostToken == ':' -> {
            val (portStr, _, portRest) = hostRest.splitByAny("#/;?")
            Pair(if (portStr.isEmpty()) null else portStr.toInt(), portRest)
        }
        else             -> Pair(null, hostRest)
    }
    val (pathStr, pathToken, pathRest) = hostOrPortRest.splitByAny("#?")
    val path = parsePath(pathStr, charset)
    val (query, token, rest) = when {
        pathToken == '?' -> {
            val (queryParams, queryToken, queryRest) = pathRest.splitByAny("#")
            Triple(parseQuery(queryParams, charset), queryToken, queryRest)
        }
        else             -> Triple(null, pathToken, pathRest)
    }
    val fragment = if (token == '#') decode(rest, charset) else null
    return Url(scheme = scheme, host = decode(host), port = port, path = path, query = query, fragment = fragment)
}

private fun parsePath(path: String?, charset: Charset) =
        path.orEmpty().split('/').filterNot { it.isEmpty() }.map { pathSegment ->
            val chunks = pathSegment.split(';')
            PathSegment(decode(chunks.first(), charset), chunks.drop(1).filterNot { it.isEmpty() }.map { matrixChunk ->
                val parts = matrixChunk.split('=')
                when {
                    parts.size == 2 -> decode(parts[0], charset) to decode(parts[1], charset)
                    else            -> throw IllegalArgumentException("Malformed matrix param: <$matrixChunk>")
                }
            })
        }

private fun parseQuery(queryParams: String?, charset: Charset) = when {
    queryParams?.isNotEmpty() == true -> {
        val chunks = queryParams.split('&')
        val splitted = chunks.map { it.split('=') }
        val structured = splitted.all { it.size == 2 }
        when {
            structured -> Query.Html4(splitted.map { decode(it[0], charset) to decode(it[1], charset) })
            else       -> Query.Unstructured(decode(queryParams, charset))
        }
    }
    else                              -> null
}

private fun String.splitBy(delimiter: String): Triple<String, String?, String> {
    val index = indexOf(delimiter)
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(this.substring(0, index), delimiter, this.substring(index + delimiter.length))
    }
}

private fun String.splitByAny(delimiters: String): Triple<String, Char?, String> {
    val index = indexOfAny(delimiters.toCharArray())
    return when {
        index < 0 -> Triple(this, null, "")
        else      -> Triple(this.substring(0, index), this[index], this.substring(index + 1))
    }
}
