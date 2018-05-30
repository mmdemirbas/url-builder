package com.mmdemirbas.urlbuilder

import java.util.*

private const val commonSafeChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$'()*,"

/**
 * See **RFC 3986**, **RFC 1738** and [https://www.talisman.org/~erlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding].
 *
 * @param safeChars the set of chars to NOT encode
 */
open class UrlPart(val safeChars: String) {
    /**
     * RFC 3986 'reg-name'. This is not very aggressive... it's quite possible to have DNS-illegal names out of this.
     * Regardless, it will at least be URI-compliant even if it's not HTTP URL-compliant.
     */
    object RegName : UrlPart("$commonSafeChars&+=;")

    /**
     * Represents RFC 3986 'pchar'. Remove delimiter that starts matrix section.
     */
    object Path : UrlPart("$commonSafeChars@:&+=")

    /**
     * Remove delims for HTTP matrix params as per RFC 1738 S3.3. The other reserved chars ('/' and '?') are already excluded.
     */
    object Matrix : UrlPart("$commonSafeChars@:&+")

    /**
     * At this point it represents RFC 3986 'query'. http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1 also
     * specifies that "+" can mean space in a query, so we will make sure to say that '+' is not safe to leave as-is
     */
    object UnstructuredQuery : UrlPart("$commonSafeChars@:&=;/?")

    /**
     * Create more stringent requirements for HTML4 queries: remove delimiters for HTML query params so that key=value
     * pairs can be used.
     */
    object QueryParam : UrlPart("$commonSafeChars@:;/?")

    object Fragment : UrlPart("$commonSafeChars@:&+=;/?")


    val safeCharSet = BitSet().apply { safeChars.forEach { set(it.toInt()) } }
}