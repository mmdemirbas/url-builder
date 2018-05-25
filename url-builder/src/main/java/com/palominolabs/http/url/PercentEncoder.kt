/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.lang.Character.isHighSurrogate
import java.lang.Character.isLowSurrogate
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharsetEncoder
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

/**
 * Encodes unsafe characters as a sequence of %XX hex-encoded bytes.
 *
 * This is typically done when encoding components of URLs. See [PercentEncoders] for pre-configured
 * PercentEncoder instances.
 *
 *  @param safeChars      the set of chars to NOT encode, stored as a bitset with the int positions corresponding to
 * those chars set to true. Treated as read only.
 * @param charsetEncoder charset encoder to encode characters with. Make sure to not re-use CharsetEncoder instances
 * across threads.
 */
class PercentEncoder(private val charsetEncoder: CharsetEncoder, private val isSafeChar: (Char) -> Boolean) {
    /**
     * Pre-allocate a StringBuilder to make the common case of encoding to a string faster
     */
    private val stringBuilder = StringBuilder()

    // need to handle surrogate pairs, so need to be able to handle 2 chars worth of stuff at once
    private val encodedBytes = ByteBuffer.allocate((charsetEncoder.maxBytesPerChar().toInt() + 1) * 2)!!

    private val unsafeCharsToEncode = CharBuffer.allocate(2)!!

    /**
     * Encode the input and pass output chars to a handler.
     *
     * @param input   input string
     * @param onOutputChar handler to call on each output character
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     * detected
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun encode(input: CharSequence, onOutputChar: (Char) -> Any) {
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (isSafeChar(c)) {
                onOutputChar(c)
                i++
                continue
            }

            // not a safe char
            unsafeCharsToEncode.clear()
            unsafeCharsToEncode.append(c)

            when {
                !isHighSurrogate(c)   -> {
                }
                input.length <= i + 1 -> throw IllegalArgumentException("Invalid UTF-16: The last character in the input string was a high surrogate (\\u" + Integer.toHexString(
                        c.toInt()) + ")")
                else                  -> {
                    // get the low surrogate as well
                    val lowSurrogate = input[i + 1]
                    when {
                        isLowSurrogate(lowSurrogate) -> {
                            unsafeCharsToEncode.append(lowSurrogate)
                            i++
                        }
                        else                         -> {
                            throw IllegalArgumentException("Invalid UTF-16: Char $i is a high surrogate (\\u${c.toInt().toString(
                                    16)}), but char ${i + 1} is not a low surrogate (\\u${lowSurrogate.toInt().toString(
                                    16)})")
                        }
                    }
                }
            }

            flushUnsafeCharBuffer(onOutputChar)
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
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun encode(input: CharSequence): String {
        stringBuilder.setLength(0)
        stringBuilder.ensureCapacity(input.length)
        encode(input, stringBuilder::append)
        return stringBuilder.toString()
    }

    /**
     * Encode unsafeCharsToEncode to bytes as per charsetEncoder, then percent-encode those bytes into output.
     *
     * Side effects: unsafeCharsToEncode will be read from and cleared. encodedBytes will be cleared and written to.
     *
     * @param onOutputChar where the encoded versions of the contents of unsafeCharsToEncode will be written
     */
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    private fun flushUnsafeCharBuffer(onOutputChar: (Char) -> Any) {
        // need to read from the char buffer, which was most recently written to
        unsafeCharsToEncode.flip()
        encodedBytes.clear()
        charsetEncoder.reset()
        charsetEncoder.encode(unsafeCharsToEncode, encodedBytes, true).throwIfError()
        charsetEncoder.flush(encodedBytes).throwIfError()

        // read contents of bytebuffer
        encodedBytes.flip()

        while (encodedBytes.hasRemaining()) {
            val b = encodedBytes.get().toInt()
            onOutputChar('%')
            onOutputChar("0123456789ABCDEF"[b shr 4 and 0xF])
            onOutputChar("0123456789ABCDEF"[b and 0xF])
        }
    }
}