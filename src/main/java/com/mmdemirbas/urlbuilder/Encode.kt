package com.mmdemirbas.urlbuilder

import java.nio.charset.Charset


/**
 * Encodes unsafe characters as a sequence of %XX hex-encoded bytes.
 *
 * @param input input string
 * @param charset charset to encode characters with
 *
 * @return the input string with every character that's not in [UrlPart.safeChars] turned into
 *         its byte representation according to the specified charset and then percent-encoded
 *
 * @throws IllegalArgumentException if malformed input or an unmappable character is detected
 */
@Throws(IllegalArgumentException::class)
fun UrlPart.encode(input: String, charset: Charset = Charsets.UTF_8): String {
    val builder = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val c = input[i]
        when {
            safeCharSet[c.toInt()] -> {
                builder.append(c)
                i++
            }
            else                   -> {
                val high = input[i]
                val low = input.getOrNull(i + 1)

                val count = when {
                    !Character.isHighSurrogate(high) -> 1
                    low == null                      -> throw IllegalArgumentException("Invalid UTF-16: The last character in the input string was a high surrogate (\\u${high.toHex()})")
                    !Character.isLowSurrogate(low)   -> throw IllegalArgumentException("Invalid UTF-16: Char $i is a high surrogate (\\u${high.toHex()}), but char at ${i + 1} is not a low surrogate (\\u${low.toHex()})")
                    else                             -> 2
                }

                val slice = input.substring(i, i + count)
                val bytes = charset.encode(slice)!!
                while (bytes.hasRemaining()) {
                    val b = bytes.get().toInt()
                    builder.append('%')
                    builder.append("0123456789ABCDEF"[b shr 4 and 0xF])
                    builder.append("0123456789ABCDEF"[b and 0xF])
                }
                i += count
            }
        }
    }
    return builder.toString()
}

private fun Char.toHex() = toInt().toString(16)
