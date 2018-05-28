package com.palominolabs.http.url

import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val commonSafeChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$'()*,"

/**
 * See **RFC 3986**, **RFC 1738** and [https://www.talisman.org/~erlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding].
 */
enum class SafeChars(val chars: String) {
    /**
     * RFC 3986 'reg-name'. This is not very aggressive... it's quite possible to have DNS-illegal names out of this.
     * Regardless, it will at least be URI-compliant even if it's not HTTP URL-compliant.
     */
    REG_NAME("$commonSafeChars&+=;"),

    /**
     * Represents RFC 3986 'pchar'. Remove delimiter that starts matrix section.
     */
    PATH("$commonSafeChars@:&+="),

    /**
     * Remove delims for HTTP matrix params as per RFC 1738 S3.3. The other reserved chars ('/' and '?') are already excluded.
     */
    MATRIX("$commonSafeChars@:&+"),

    /**
     * At this point it represents RFC 3986 'query'. http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1 also
     * specifies that "+" can mean space in a query, so we will make sure to say that '+' is not safe to leave as-is
     */
    UNSTRUCTURED_QUERY("$commonSafeChars@:&=;/?"),

    /**
     * Create more stringent requirements for HTML4 queries: remove delimiters for HTML query params so that key=value
     * pairs can be used.
     */
    QUERY_PARAM("$commonSafeChars@:;/?"),

    FRAGMENT("$commonSafeChars@:&+=;/?");

    fun newEncoder() = PercentEncoder(Charsets.UTF_8, chars)
}