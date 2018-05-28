/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction.REPLACE
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import kotlin.text.Charsets.UTF_16BE
import kotlin.text.Charsets.UTF_8

class PercentEncoderTest {
    private lateinit var alnum: PercentEncoder
    private lateinit var alnum16: PercentEncoder

    @Before
    fun setUp() {
        this.alnum = newEncoder()
        this.alnum16 = newEncoder(charset = UTF_16BE)
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testDoesntEncodeSafe() {
        assertEquals("abcd%41%42%43%44", newEncoder(safeChars = "abcd").encode("abcdABCD"))
    }

    private fun newEncoder(charset: Charset = UTF_8,
                           safeChars: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") =
            PercentEncoder(charset.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE), safeChars)

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testEncodeInBetweenSafe() {
        assertEquals("abc%20123", alnum.encode("abc 123"))
    }

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testSafeInBetweenEncoded() {
        assertEquals("%20abc%20", alnum.encode(" abc "))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%E2%98%83", alnum.encode("snowman\u2603"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%F0%9D%84%9E", alnum.encode("clef\ud834\udd1e"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf16() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%26%03", alnum16.encode("snowman\u2603"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedUtf16SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%D8%34%DD%1E", alnum16.encode("clef\ud834\udd1e"))
    }
}
