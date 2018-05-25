/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction.REPLACE
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import java.util.*
import kotlin.text.Charsets.UTF_16BE
import kotlin.text.Charsets.UTF_8

class PercentEncoderTest {

    private var alnum: PercentEncoder? = null
    private var alnum16: PercentEncoder? = null

    @Before
    fun setUp() {
        val bs = BitSet()
        run {
            var i: Int = 'a'.toInt()
            while (i <= 'z'.toInt()) {
                bs.set(i)
                i++
            }
        }
        run {
            var i: Int = 'A'.toInt()
            while (i <= 'Z'.toInt()) {
                bs.set(i)
                i++
            }
        }
        var i: Int = '0'.toInt()
        while (i <= '9'.toInt()) {
            bs.set(i)
            i++
        }

        this.alnum = PercentEncoder(bs, UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))
        this.alnum16 =
                PercentEncoder(bs, UTF_16BE.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testDoesntEncodeSafe() {
        val set = BitSet()
        var i: Int = 'a'.toInt()
        while (i <= 'z'.toInt()) {
            set.set(i)
            i++
        }

        val pe = PercentEncoder(set, UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))
        assertEquals("abcd%41%42%43%44", pe.encode("abcdABCD"))
    }

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testEncodeInBetweenSafe() {
        assertEquals("abc%20123", alnum!!.encode("abc 123"))
    }

    @Test
    @Throws(MalformedInputException::class, UnmappableCharacterException::class)
    fun testSafeInBetweenEncoded() {
        assertEquals("%20abc%20", alnum!!.encode(" abc "))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%E2%98%83", alnum!!.encode("snowman\u2603"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%F0%9D%84%9E", alnum!!.encode("clef\ud834\udd1e"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testEncodeUtf16() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%26%03", alnum16!!.encode("snowman\u2603"))
    }

    @Test
    @Throws(CharacterCodingException::class)
    fun testUrlEncodedUtf16SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%D8%34%DD%1E", alnum16!!.encode("clef\ud834\udd1e"))
    }
}
