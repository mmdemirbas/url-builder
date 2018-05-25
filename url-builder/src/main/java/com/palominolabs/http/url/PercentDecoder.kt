package com.palominolabs.http.url

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult
import java.nio.charset.CoderResult.OVERFLOW
import java.nio.charset.CoderResult.UNDERFLOW
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

/**
 * Decodes percent-encoded (%XX) Unicode text.
 *
 * @param charsetDecoder            Charset to decode bytes into chars with
 * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
 * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
 */
class PercentDecoder(private val charsetDecoder: CharsetDecoder,
                     initialEncodedByteBufSize: Int = 16,
                     decodedCharBufSize: Int = 16) {
    /**
     * bytes represented by the current sequence of %-triples. Resized as needed.
     */
    private var encodedBuf = ByteBuffer.allocate(initialEncodedByteBufSize)!!

    /**
     * Written to with decoded chars by decoder
     */
    private val decodedCharBuf = CharBuffer.allocate(decodedCharBufSize)!!

    /**
     * The decoded string for the current input
     */
    private val outputBuf = StringBuilder()

    /**
     * @param input Input with %-encoded representation of characters in this instance's configured character set, e.g.
     * "%20" for a space character
     * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
     * @throws MalformedInputException      if decoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if decoder is configured to report errors and an unmappable character is
     * detected
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun decode(input: CharSequence): String {
        outputBuf.setLength(0)
        // this is almost always an underestimate of the size needed:
        // only a 4-byte encoding (which is 12 characters input) would case this to be an overestimate
        outputBuf.ensureCapacity(input.length / 8)
        encodedBuf.clear()

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '%') {
                handleEncodedBytes()
                outputBuf.append(c)
                i++
                continue
            }

            if (i + 2 >= input.length) throw IllegalArgumentException("Could not percent decode <$input>: incomplete %-pair at position $i")

            // grow the byte buf if needed
            if (encodedBuf.remaining() == 0) {
                val largerBuf = ByteBuffer.allocate(encodedBuf.capacity() * 2)
                encodedBuf.flip()
                largerBuf.put(encodedBuf)
                encodedBuf = largerBuf
            }

            // note that we advance i here as we consume chars
            var msBits = Character.digit(input[++i], 16)
            val lsBits = Character.digit(input[++i], 16)

            if (msBits == -1 || lsBits == -1) throw IllegalArgumentException("Invalid %-tuple <" + input.subSequence(i - 2,
                                                                                                                     i + 1) + ">")

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
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    private fun handleEncodedBytes() {
        // nothing to do
        if (encodedBuf.position() == 0) return

        charsetDecoder.reset()
        var coderResult: CoderResult

        // switch to reading mode
        encodedBuf.flip()

        // loop while we're filling up the decoded char buf, or there's any encoded bytes
        // decode() in practice seems to only consume bytes when it can decode an entire char...
        do {
            decodedCharBuf.clear()
            coderResult = charsetDecoder.decode(encodedBuf, decodedCharBuf, false)
            coderResult.throwIfError(true)
            appendDecodedChars()
        } while (coderResult === OVERFLOW && encodedBuf.hasRemaining())

        // final decode with end-of-input flag
        decodedCharBuf.clear()
        coderResult = charsetDecoder.decode(encodedBuf, decodedCharBuf, true)
        coderResult.throwIfError(true)

        when {
            encodedBuf.hasRemaining() -> throw IllegalStateException("Final decode didn't error, but didn't consume remaining input bytes")
            coderResult !== UNDERFLOW -> throw IllegalStateException("Expected underflow, but instead final decode returned $coderResult")
            else                      -> {
                appendDecodedChars()

                // we've finished the input, wrap it up
                encodedBuf.clear()
                flush()
            }
        }
    }

    /**
     * Must only be called when the input encoded bytes buffer is empty
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    private fun flush() {
        decodedCharBuf.clear()

        val coderResult = charsetDecoder.flush(decodedCharBuf)
        appendDecodedChars()

        coderResult.throwIfError(true)
        if (coderResult !== UNDERFLOW) throw IllegalStateException("Decoder flush resulted in $coderResult")
    }

    /**
     * Flip the decoded char buf and append it to the string bug
     */
    private fun appendDecodedChars() {
        decodedCharBuf.flip()
        outputBuf.append(decodedCharBuf)
    }
}
