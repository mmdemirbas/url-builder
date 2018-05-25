/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction.REPLACE
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import java.util.*
import kotlin.text.Charsets.UTF_8

/**
 * See **RFC 3986**, **RFC 1738** and [https://www.talisman.org/~erlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding].
 */
@ThreadSafe
object PercentEncoders {
    /**
     * An encoder for RFC 3986 reg-names.
     *
     * RFC 3986 'reg-name'. This is not very aggressive... it's quite possible to have DNS-illegal names out of this.
     * Regardless, it will at least be URI-compliant even if it's not HTTP URL-compliant.
     */
    private val regNameSafeChars = buildBitSet("!$&'()*+,;=")

    /**
     * Represents RFC 3986 'pchar'. Remove delimiter that starts matrix section.
     */
    private val pathSafeChars = buildBitSet("!$&'()*+,=:@")

    /**
     * Remove delims for HTTP matrix params as per RFC 1738 S3.3. The other reserved chars ('/' and '?') are already excluded.
     */
    private val matrixSafeChars = buildBitSet("!$&'()*+,:@")

    /**
     * At this point it represents RFC 3986 'query'. http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1 also
     * specifies that "+" can mean space in a query, so we will make sure to say that '+' is not safe to leave as-is
     */
    private val unstructuredQuerySafeChars = buildBitSet("!$&'()*,;=:@/?")

    /**
     * Create more stringent requirements for HTML4 queries: remove delimiters for HTML query params so that key=value
     * pairs can be used.
     */
    private val queryParamSafeChars = buildBitSet("!$'()*,;:@/?")

    private val fragmentSafeChars = buildBitSet("!$&'()*+,;=:@/?")

    private fun buildBitSet(extra: String) =
            BitSet().apply { "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~$extra".forEach { set(it.toInt()) } }

    fun newRegNameEncoder() = regNameSafeChars.newEncoder()
    fun newPathEncoder() = pathSafeChars.newEncoder()
    fun newMatrixEncoder() = matrixSafeChars.newEncoder()
    fun newUnstructuredQueryEncoder() = unstructuredQuerySafeChars.newEncoder()
    fun newQueryParamEncoder() = queryParamSafeChars.newEncoder()
    fun newFragmentEncoder() = fragmentSafeChars.newEncoder()

    private fun BitSet.newEncoder() =
            PercentEncoder(this, UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE)!!)

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
}
