/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.mmdemirbas.urlbuilder

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
