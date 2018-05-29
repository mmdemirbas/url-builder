package com.palominolabs.http.url

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
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


    private val safeCharSet = BitSet().apply { safeChars.forEach { set(it.toInt()) } }

    /**
     * Encodes unsafe characters as a sequence of %XX hex-encoded bytes and returns the resulting text as a String.
     *
     * @param input input string
     * @param charset charset to encode characters with
     *
     * @return the input string with every character that's not in safeChars turned into its byte representation via the
     * specified charset encoder and then percent-encoded
     *
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     * detected
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun encode(input: CharSequence, charset: Charset = Charsets.UTF_8): String {
        val builder = StringBuilder(input.length)
        val charsetEncoder =
                charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)

        // need to handle surrogate pairs, so need to be able to handle 2 chars worth of stuff at once
        val encodedBytes = ByteBuffer.allocate((charsetEncoder.maxBytesPerChar().toInt() + 1) * 2)!!
        val unsafeCharsToEncode = CharBuffer.allocate(2)!!

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (safeCharSet.get(c.toInt())) {
                builder.append(c)
                i++
                continue
            }

            // not a safe char
            unsafeCharsToEncode.clear()
            unsafeCharsToEncode.append(c)

            when {
                !Character.isHighSurrogate(c) -> {
                }
                input.length <= i + 1         -> {
                    val high = Integer.toHexString(c.toInt())
                    throw IllegalArgumentException("Invalid UTF-16: The last character in the input string was a high surrogate (\\u$high)")
                }
                else                          -> {
                    // get the low surrogate as well
                    val lowSurrogate = input[i + 1]
                    when {
                        Character.isLowSurrogate(lowSurrogate) -> {
                            unsafeCharsToEncode.append(lowSurrogate)
                            i++
                        }
                        else                                   -> {
                            val high = c.toInt().toString(16)
                            val low = lowSurrogate.toInt().toString(16)
                            throw IllegalArgumentException("Invalid UTF-16: Char $i is a high surrogate (\\u$high), but char ${i + 1} is not a low surrogate (\\u$low)")
                        }
                    }
                }
            }

            // Encode unsafeCharsToEncode to bytes as per charsetEncoder, then percent-encode those bytes into output.
            // -- need to read from the char buffer, which was most recently written to
            unsafeCharsToEncode.flip()
            encodedBytes.clear()
            charsetEncoder.reset()
            charsetEncoder.encode(unsafeCharsToEncode, encodedBytes, true).throwIfError()
            charsetEncoder.flush(encodedBytes).throwIfError()
            // -- read contents of bytebuffer
            encodedBytes.flip()
            while (encodedBytes.hasRemaining()) {
                val b = encodedBytes.get().toInt()
                builder.append('%')
                builder.append("0123456789ABCDEF"[b shr 4 and 0xF])
                builder.append("0123456789ABCDEF"[b and 0xF])
            }

            i++
        }
        return builder.toString()
    }
}