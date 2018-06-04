package com.mmdemirbas.urlbuilder

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset


/**
 * Decodes percent-encoded sequences such as '%2F' -> '/'.
 *
 * @throws IllegalArgumentException if malformed input detected
 */
@JvmOverloads
fun String.decodePercent(charset: Charset = Charsets.UTF_8): String {
    val bos = ByteArrayOutputStream(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        when {
            c != '%'        -> bos.write(c.toInt())
            i + 2 >= length -> throw IllegalArgumentException("Could not percent decode <${substring(i)}>: incomplete %-pair at position $i")
            else            -> {
                val high = "0123456789ABCDEF".indexOf(this[i + 1])
                val low = "0123456789ABCDEF".indexOf(this[i + 2])
                if (high < 0 || low < 0) throw IllegalArgumentException("Invalid %-tuple <${substring(i)}>")
                bos.write(((high shl 4) + low).toChar().toInt())
                i += 2
            }
        }
        i++
    }
    return String(bos.toByteArray(), charset)
}
