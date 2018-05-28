/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.CharacterCodingException
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import kotlin.text.Charsets.UTF_16BE

class PercentEncoderTest {
    @Test
    @Throws(CharacterCodingException::class)
    fun testDoesntEncodeSafe() {
        assertEquals("abcd%41%42%43%44", custom("abcd").encode("abcdABCD"))
    }

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testEncodeInBetweenSafe() {
        assertEquals("abc%20123", custom().encode("abc 123"))
    }

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testSafeInBetweenEncoded() {
        assertEquals("%20abc%20", custom().encode(" abc "))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%E2%98%83", custom().encode("snowman\u2603"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%F0%9D%84%9E", custom().encode("clef\ud834\udd1e"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf16() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%26%03", custom().encode("snowman\u2603", UTF_16BE))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedUtf16SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%D8%34%DD%1E", custom().encode("clef\ud834\udd1e", UTF_16BE))
    }

    private fun custom(safeChars: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") =
            UrlPart.Custom(safeChars)
}
