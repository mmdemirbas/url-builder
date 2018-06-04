package com.mmdemirbas.urlbuilder

import java.nio.charset.Charset
import java.util.*


private const val commonSafeChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$'()*,"

/**
 * See **RFC 3986**, **RFC 1738** and [https://www.talisman.org/~erlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding].
 *
 * @param safeChars the set of chars to NOT encode
 */
open class SafeChars(private val safeChars: String) {
    object Scheme : SafeChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-.")
    object Authority : SafeChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$&'()*+,;=:@")
    object UserInfo : SafeChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$&'()*+,;=:")
    object Port : SafeChars("0123456789")

    /**
     * RFC 3986 'reg-name'. This is not very aggressive... it's quite possible to have DNS-illegal names out of this.
     * Regardless, it will at least be URI-compliant even if it's not HTTP URL-compliant.
     */
    object RegName : SafeChars("$commonSafeChars&+=;")

    /**
     * Represents RFC 3986 'pchar'. Remove delimiter that starts matrix section.
     */
    object Path : SafeChars("$commonSafeChars@:&+=")

    /**
     * Remove delims for HTTP matrix params as per RFC 1738 S3.3. The other reserved chars ('/' and '?') are already excluded.
     */
    object Matrix : SafeChars("$commonSafeChars@:&+")

    /**
     * At this point it represents RFC 3986 'query'. http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1 also
     * specifies that "+" can mean space in a query, so we will make sure to say that '+' is not safe to leave as-is
     */
    object UnstructuredQuery : SafeChars("$commonSafeChars@:&=;/?")

    /**
     * Create more stringent requirements for HTML4 queries: remove delimiters for HTML query params so that key=value
     * pairs can be used.
     */
    object QueryParam : SafeChars("$commonSafeChars@:;/?")

    object Fragment : SafeChars("$commonSafeChars@:&+=;/?")

    private val safeCharSet = BitSet().apply { safeChars.forEach { set(it.toInt()) } }

    fun isSafe(c: Char) = safeCharSet[c.toInt()]
}

/**
 * Encodes unsafe chars as a sequence of hex-encoded bytes such as `/` -> `%2F`.
 *
 * @throws IllegalArgumentException if malformed input detected
 */
@JvmOverloads
fun String.encodePercent(safeChars: SafeChars, charset: Charset = Charsets.UTF_8): String {
    val builder = StringBuilder(length)
    var i = 0
    while (i < length) {
        when {
            safeChars.isSafe(this[i]) -> {
                builder.append(this[i])
                i++
            }
            else                      -> {
                val high = this[i]
                val low = getOrNull(i + 1)

                val count = when {
                    !Character.isHighSurrogate(high) -> 1
                    low == null                      -> throw IllegalArgumentException("Invalid UTF-16: The last character in the input string was a high surrogate (\\u${high.toHex()})")
                    !Character.isLowSurrogate(low)   -> throw IllegalArgumentException("Invalid UTF-16: Char $i is a high surrogate (\\u${high.toHex()}), but char at ${i + 1} is not a low surrogate (\\u${low.toHex()})")
                    else                             -> 2
                }
                builder.append(substring(i, i + count).forceEncodePercent(charset))
                i += count
            }
        }
    }
    return builder.toString()
}

/**
 * Encodes all chars as a sequence of hex-encoded bytes such as `/` -> `%2F`.
 */
@JvmOverloads
fun String.forceEncodePercent(charset: Charset = Charsets.UTF_8): String {
    val builder = StringBuilder(length * 3)
    val bytes = charset.encode(this)!!
    while (bytes.hasRemaining()) {
        val b = bytes.get().toInt()
        builder.append("%" + "0123456789ABCDEF"[b shr 4 and 0xF] + "0123456789ABCDEF"[b and 0xF])
    }
    return builder.toString()
}

private fun Char.toHex() = toInt().toString(16)