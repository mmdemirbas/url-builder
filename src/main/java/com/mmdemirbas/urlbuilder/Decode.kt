package com.mmdemirbas.urlbuilder

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CoderResult


/**
 * Decodes percent-encoded (%XX) Unicode text.
 *
 * @param input Input with %-encoded representation of characters in this instance's configured character set, e.g.
 * "%20" for a space character
 * @param charset            Charset to decode bytes into chars with
 *
 * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
 *
 * @throws IllegalArgumentException      if malformed input or an unmappable character is detected
 */
fun decode(input: CharSequence, charset: Charset = Charsets.UTF_8): String {
    return DecodeState(input, charset).decode()
}

/**
 * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
 * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
 */
class DecodeState(private val input: CharSequence,
                  charset: Charset = Charsets.UTF_8,
                  initialEncodedByteBufSize: Int = 16,
                  decodedCharBufSize: Int = 16) {
    // The decoded string for the current input.
    // this is almost always an underestimate of the size needed:
    // only a 4-byte encoding (which is 12 characters input) would case this to be an overestimate
    private val outputBuf = StringBuilder(input.length / 8)

    // bytes represented by the current sequence of %-triples. Resized as needed.
    private var encodedBuf = ByteBuffer.allocate(initialEncodedByteBufSize)!!

    // Written to with decoded chars by decoder
    private val decodedCharBuf = CharBuffer.allocate(decodedCharBufSize)!!

    private val charsetDecoder = charset.newDecoder()

    @Throws(IllegalArgumentException::class)
    fun decode(): String {
        var i = 0
        loop@ while (i < input.length) {
            val c = input[i]
            when {
                c != '%'                    -> {
                    handleEncodedBytes()
                    outputBuf.append(c)
                    i++
                    continue@loop
                }
                i + 2 >= input.length       -> {
                    throw IllegalArgumentException("Could not percent decode <$input>: incomplete %-pair at position $i")
                }
                encodedBuf.remaining() == 0 -> {
                    // grow the byte buf if needed
                    val largerBuf = ByteBuffer.allocate(encodedBuf.capacity() * 2)
                    encodedBuf.flip()
                    largerBuf.put(encodedBuf)
                    encodedBuf = largerBuf
                }
            }

            // note that we advance i here as we consume chars
            var msBits = Character.digit(input[++i], 16)
            val lsBits = Character.digit(input[++i], 16)

            if (msBits == -1 || lsBits == -1) {
                val tuple = input.subSequence(i - 2, i + 1)
                throw IllegalArgumentException("Invalid %-tuple <$tuple>")
            }

            msBits = msBits shl 4
            msBits = msBits or lsBits

            // msBits can only have 8 bits set, so cast is safe
            encodedBuf.put(msBits.toByte())
            i++
        }

        handleEncodedBytes()
        return outputBuf.toString()
    }

    /**
     * Decode any buffered encoded bytes and write them to the output buf.
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    private fun handleEncodedBytes() {
        // nothing to do
        if (encodedBuf.position() == 0) return

        charsetDecoder.reset()

        // switch to reading mode
        encodedBuf.flip()

        // loop while we're filling up the decoded char buf, or there's any encoded bytes
        // decode() in practice seems to only consume bytes when it can decode an entire char...
        var coderResult: CoderResult
        do {
            decodedCharBuf.clear()
            coderResult = charsetDecoder.decode(encodedBuf, decodedCharBuf, false)
            coderResult.throwIfError(true)
            decodedCharBuf.flip()
            outputBuf.append(decodedCharBuf)
        } while (coderResult.isOverflow && encodedBuf.hasRemaining())

        // final decode with end-of-input flag
        decodedCharBuf.clear()
        coderResult = charsetDecoder.decode(encodedBuf, decodedCharBuf, true)
        coderResult.throwIfError(true)

        when {
            encodedBuf.hasRemaining() -> throw IllegalStateException("Final decode didn't error, but didn't consume remaining input bytes")
            !coderResult.isUnderflow  -> throw IllegalStateException("Expected underflow, but instead final decode returned $coderResult")
            else                      -> {
                decodedCharBuf.flip()
                outputBuf.append(decodedCharBuf)

                // we've finished the input, wrap it up
                encodedBuf.clear()
                decodedCharBuf.clear()
                val coderResult1 = charsetDecoder.flush(decodedCharBuf)
                decodedCharBuf.flip()
                outputBuf.append(decodedCharBuf)
                coderResult1.throwIfError(true)
                if (coderResult1 !== CoderResult.UNDERFLOW) throw IllegalStateException("Decoder flush resulted in $coderResult1")
            }
        }
    }
}


/**
 * Throws if the given [CoderResult] is considered an error.
 *
 * @throws IllegalStateException        if result is overflow
 * @throws IllegalArgumentException     if result represents malformed input or an unmappable character
 */
@Throws(IllegalArgumentException::class, IllegalStateException::class)
fun CoderResult.throwIfError(ignoreOverflow: Boolean = false) {
    when {
        !ignoreOverflow && isOverflow -> throw IllegalStateException("Byte buffer overflow; this should not happen.")
        isMalformed                   -> throw IllegalArgumentException("Malformed input")
        isUnmappable                  -> throw IllegalArgumentException("Unmappable character")
    }
}
