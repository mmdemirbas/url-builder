/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.lang.Character.isHighSurrogate
import java.lang.Character.isLowSurrogate
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import java.util.*
import javax.annotation.concurrent.NotThreadSafe

/**
 * Encodes unsafe characters as a sequence of %XX hex-encoded bytes.
 *
 * This is typically done when encoding components of URLs. See [UrlPercentEncoders] for pre-configured
 * PercentEncoder instances.
 */
@NotThreadSafe
class PercentEncoder
/**
 * @param safeChars      the set of chars to NOT encode, stored as a bitset with the int positions corresponding to
 * those chars set to true. Treated as read only.
 * @param charsetEncoder charset encoder to encode characters with. Make sure to not re-use CharsetEncoder instances
 * across threads.
 */
(private val safeChars: BitSet, private val encoder: CharsetEncoder) {
    /**
     * Pre-allocate a string handler to make the common case of encoding to a string faster
     */
    private val stringHandler = StringBuilderPercentEncoderOutputHandler()
    private val encodedBytes: ByteBuffer
    private val unsafeCharsToEncode: CharBuffer

    init {

        // why is this a float? sigh.
        val maxBytesPerChar = 1 + encoder.maxBytesPerChar().toInt()
        // need to handle surrogate pairs, so need to be able to handle 2 chars worth of stuff at once
        encodedBytes = ByteBuffer.allocate(maxBytesPerChar * 2)
        unsafeCharsToEncode = CharBuffer.allocate(2)
    }

    /**
     * Encode the input and pass output chars to a handler.
     *
     * @param input   input string
     * @param handler handler to call on each output character
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     * detected
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun encode(input: CharSequence, handler: PercentEncoderOutputHandler) {

        var i = 0
        while (i < input.length) {

            val c = input[i]

            if (safeChars.get(c.toInt())) {
                handler.onOutputChar(c)
                i++
                continue
            }

            // not a safe char
            unsafeCharsToEncode.clear()
            unsafeCharsToEncode.append(c)
            if (isHighSurrogate(c)) {
                if (input.length > i + 1) {
                    // get the low surrogate as well
                    val lowSurrogate = input[i + 1]
                    if (isLowSurrogate(lowSurrogate)) {
                        unsafeCharsToEncode.append(lowSurrogate)
                        i++
                    } else {
                        throw IllegalArgumentException("Invalid UTF-16: Char $i is a high surrogate (\\u" + Integer.toHexString(
                                c.toInt()) + "), but char " + (i + 1) + " is not a low surrogate (\\u" + Integer.toHexString(
                                lowSurrogate.toInt()) + ")")
                    }
                } else {
                    throw IllegalArgumentException("Invalid UTF-16: The last character in the input string was a high surrogate (\\u" + Integer.toHexString(
                            c.toInt()) + ")")
                }
            }

            flushUnsafeCharBuffer(handler)
            i++
        }
    }

    /**
     * Encode the input and return the resulting text as a String.
     *
     * @param input input string
     * @return the input string with every character that's not in safeChars turned into its byte representation via the
     * instance's encoder and then percent-encoded
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     * detected
     */
//    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun encode(input: CharSequence): String {
        stringHandler.reset()
        stringHandler.ensureCapacity(input.length)
        encode(input, stringHandler)
        return stringHandler.contents
    }

    /**
     * Encode unsafeCharsToEncode to bytes as per charsetEncoder, then percent-encode those bytes into output.
     *
     * Side effects: unsafeCharsToEncode will be read from and cleared. encodedBytes will be cleared and written to.
     *
     * @param handler where the encoded versions of the contents of unsafeCharsToEncode will be written
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    private fun flushUnsafeCharBuffer(handler: PercentEncoderOutputHandler) {
        // need to read from the char buffer, which was most recently written to
        unsafeCharsToEncode.flip()

        encodedBytes.clear()

        encoder.reset()
        var result = encoder.encode(unsafeCharsToEncode, encodedBytes, true)
        checkResult(result)
        result = encoder.flush(encodedBytes)
        checkResult(result)

        // read contents of bytebuffer
        encodedBytes.flip()

        while (encodedBytes.hasRemaining()) {
            val b = encodedBytes.get().toInt()

            handler.onOutputChar('%')
            handler.onOutputChar(HEX_CODE[b.shr(4) and 0xF])
            handler.onOutputChar(HEX_CODE[(b and 0xF)])
        }
    }

    companion object {

        private val HEX_CODE = "0123456789ABCDEF".toCharArray()

        /**
         * @param result result to check
         * @throws IllegalStateException        if result is overflow
         * @throws MalformedInputException      if result represents malformed input
         * @throws UnmappableCharacterException if result represents an unmappable character
         */
        @Throws(MalformedInputException::class, UnmappableCharacterException::class)
        private fun checkResult(result: CoderResult) {
            if (result.isOverflow) {
                throw IllegalStateException("Byte buffer overflow; this should not happen.")
            }
            if (result.isMalformed) {
                throw MalformedInputException(result.length())
            }
            if (result.isUnmappable) {
                throw UnmappableCharacterException(result.length())
            }
        }
    }
}
