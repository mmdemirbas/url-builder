package com.mmdemirbas.urlbuilder

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset


/**
 * Decodes percent-encoded (%XX) sequences.
 *
 * @throws IllegalArgumentException if malformed input or an unmappable character is detected
 */
@Throws(IllegalArgumentException::class)
fun decode(input: String, charset: Charset = Charsets.UTF_8): String {
    val bos = ByteArrayOutputStream(input.length)
    var i = 0
    while (i < input.length) {
        val c = input[i]
        when {
            c != '%'              -> bos.write(c.toInt())
            i + 2 >= input.length -> throw IllegalArgumentException("Could not percent decode <${input.substring(i)}>: incomplete %-pair at position $i")
            else                  -> {
                val high = "0123456789ABCDEF".indexOf(input[i + 1])
                val low = "0123456789ABCDEF".indexOf(input[i + 2])
                if (high < 0 || low < 0) throw IllegalArgumentException("Invalid %-tuple <${input.substring(i)}>")
                bos.write(((high shl 4) + low).toChar().toInt())
                i += 2
            }
        }
        i++
    }
    return String(bos.toByteArray(), charset)
}
