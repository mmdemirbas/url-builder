/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.*


/**
 * Decodes percent-encoded (%XX) Unicode text.
 *
 * @param input Input with %-encoded representation of characters in this instance's configured character set, e.g.
 * "%20" for a space character
 * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
 * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
 * @param charsetDecoder            Charset to decode bytes into chars with
 *
 * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
 *
 * @throws MalformedInputException      if decoder is configured to report errors and malformed input is detected
 * @throws UnmappableCharacterException if decoder is configured to report errors and an unmappable character is
 * detected
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
fun decode(input: CharSequence,
           charset: Charset = Charsets.UTF_8,
           initialEncodedByteBufSize: Int = 16,
           decodedCharBufSize: Int = 16): String {
    // The decoded string for the current input.
    // this is almost always an underestimate of the size needed:
    // only a 4-byte encoding (which is 12 characters input) would case this to be an overestimate
    val outputBuf = StringBuilder(input.length / 8)

    // bytes represented by the current sequence of %-triples. Resized as needed.
    var encodedBuf = ByteBuffer.allocate(initialEncodedByteBufSize)!!

    // Written to with decoded chars by decoder
    val decodedCharBuf = CharBuffer.allocate(decodedCharBufSize)!!

    val charsetDecoder = charset.newDecoder()

    var i = 0
    loop@ while (i < input.length) {
        val c = input[i]
        when {
            c != '%'                    -> {
                handleEncodedBytes(outputBuf, encodedBuf, decodedCharBuf, charsetDecoder)
                outputBuf.append(c)
                i++
                continue@loop
            }
            i + 2 >= input.length       -> throw IllegalArgumentException("Could not percent decode <$input>: incomplete %-pair at position $i")
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
            throw IllegalArgumentException("Invalid %-tuple <" + input.subSequence(i - 2, i + 1) + ">")
        }

        msBits = msBits shl 4
        msBits = msBits or lsBits

        // msBits can only have 8 bits set, so cast is safe
        encodedBuf.put(msBits.toByte())
        i++
    }

    handleEncodedBytes(outputBuf, encodedBuf, decodedCharBuf, charsetDecoder)
    return outputBuf.toString()
}

/**
 * Decode any buffered encoded bytes and write them to the output buf.
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
private fun handleEncodedBytes(outputBuf: StringBuilder,
                               encodedBuf: ByteBuffer,
                               decodedCharBuf: CharBuffer,
                               charsetDecoder: CharsetDecoder) {
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
        appendDecodedChars(outputBuf, decodedCharBuf)
    } while (coderResult === CoderResult.OVERFLOW && encodedBuf.hasRemaining())

    // final decode with end-of-input flag
    decodedCharBuf.clear()
    coderResult = charsetDecoder.decode(encodedBuf, decodedCharBuf, true)
    coderResult.throwIfError(true)

    when {
        encodedBuf.hasRemaining()             -> throw IllegalStateException("Final decode didn't error, but didn't consume remaining input bytes")
        coderResult !== CoderResult.UNDERFLOW -> throw IllegalStateException("Expected underflow, but instead final decode returned $coderResult")
        else                                  -> {
            appendDecodedChars(outputBuf, decodedCharBuf)

            // we've finished the input, wrap it up
            encodedBuf.clear()
            flush(outputBuf, decodedCharBuf, charsetDecoder)
        }
    }
}

/**
 * Must only be called when the input encoded bytes buffer is empty
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
private fun flush(outputBuf: StringBuilder, decodedCharBuf: CharBuffer, charsetDecoder: CharsetDecoder) {
    decodedCharBuf.clear()

    val coderResult = charsetDecoder.flush(decodedCharBuf)
    appendDecodedChars(outputBuf, decodedCharBuf)

    coderResult.throwIfError(true)
    if (coderResult !== CoderResult.UNDERFLOW) throw IllegalStateException("Decoder flush resulted in $coderResult")
}

/**
 * Flip the decoded char buf and append it to the string bug
 */
private fun appendDecodedChars(outputBuf: StringBuilder, decodedCharBuf: CharBuffer) {
    decodedCharBuf.flip()
    outputBuf.append(decodedCharBuf)
}

/**
 * Throws if the given [CoderResult] is considered an error.
 *
 * @throws IllegalStateException        if result is overflow
 * @throws MalformedInputException      if result represents malformed input
 * @throws UnmappableCharacterException if result represents an unmappable character
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
fun CoderResult.throwIfError(ignoreOverflow: Boolean = false) {
    when {
        !ignoreOverflow && isOverflow -> throw IllegalStateException("Byte buffer overflow; this should not happen.")
        isMalformed                   -> throw MalformedInputException(length())
        isUnmappable                  -> throw UnmappableCharacterException(length())
    }
}
