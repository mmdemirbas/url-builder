/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.mmdemirbas.urlbuilder

/**
 * Url in `scheme://host:port/path/path/path?query#fragment` format.
 *
 * @property scheme    http, https etc.
 * @property host      host in IPv4, IPv6 or reg-name (a dns name, e.g. foo.com) format
 * @property port      `null` or positive integer
 * @property path      list of [PathSegment]s (part after `/`)
 * @property query    `null` or [Query] (part after `?`)
 * @property fragment `null` or fragment (part after `#`)
 */
data class Url(val scheme: String,
               val host: String,
               val port: Int? = null,
               val path: List<PathSegment> = emptyList(),
               val query: Query? = null,
               val fragment: String? = null)

/**
 * Path segment in `/segment;matrix=param;matrix=param` format.
 */
data class PathSegment(val segment: String, val matrixParams: List<Pair<String, String>> = emptyList())

sealed class Query {
    /**
     * Query in `?a=b&c=d` format as specified in the HTML4 spec and used commonly in HTTP.
     *
     * Using query strings to encode key=value pairs is not part of the URI/URL specification; it is specified by
     * http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
     */
    data class Html4(val params: List<Pair<String, String>>) : Query() {
        constructor(vararg params: Pair<String, String>) : this(params.asList())
    }

    /**
     * Everything after `?` as is except the fragment part which is starting with `#`.
     *
     * This is useful when you want to specify a query string that is not of key=value format.
     * The concept of query params is only part of the HTML spec (and common HTTP usage), though,
     * so it's perfectly legal to have a query string that is in some other form.
     * To represent this case, if the aforementioned param-parsing attempt fails, the query
     * string will be treated as just a monolithic, unstructured, string.
     */
    data class Unstructured(val query: String) : Query()
}
