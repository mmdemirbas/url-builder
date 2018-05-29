/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.text.Charsets.UTF_16BE

class EncodeTest {
    @Test
    fun doesntEncodeSafe() {
        assertEquals("abcd%41%42%43%44", UrlPart("abcd").encode("abcdABCD"))
    }

    @Test
    fun encodeInBetweenSafe() {
        assertEquals("abc%20123", urlPart().encode("abc 123"))
    }

    @Test
    fun safeInBetweenEncoded() {
        assertEquals("%20abc%20", urlPart().encode(" abc "))
    }

    @Test
    fun encodeUtf8() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%E2%98%83", urlPart().encode("snowman\u2603"))
    }

    @Test
    fun encodeUtf8SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%F0%9D%84%9E", urlPart().encode("clef\ud834\udd1e"))
    }

    @Test
    fun encodeUtf16() {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%26%03", urlPart().encode("snowman\u2603", UTF_16BE))
    }

    @Test
    fun urlEncodedUtf16SurrogatePair() {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%D8%34%DD%1E", urlPart().encode("clef\ud834\udd1e", UTF_16BE))
    }

    private fun urlPart() = UrlPart("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
}
