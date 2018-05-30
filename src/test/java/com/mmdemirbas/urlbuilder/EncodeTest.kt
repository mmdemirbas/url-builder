/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_16BE
import kotlin.text.Charsets.UTF_8

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object EncodeTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun Case.encode() {
        assertEquals(expected,
                     UrlComponent("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").encode(input, charset))
    }

    fun cases() = listOf(Case("doesn't encode safe chars", "abcd%2B%2D%2A%2F", "abcd+-*/"),
                         Case("non-safe between safe", "abc%20123", "abc 123"),
                         Case("safe between non-safe", "%20abc%20", " abc "),
            // 1 UTF-16 char (unicode snowman)
                         Case("utf8", "snowman%E2%98%83", "snowman\u2603"),
            // musical G clef: 1d11e, has to be represented in surrogate pair form
                         Case("utf8 surrogate pair", "clef%F0%9D%84%9E", "clef\ud834\udd1e"),
            // 1 UTF-16 char (unicode snowman)
                         Case("utf16", "snowman%26%03", "snowman\u2603", UTF_16BE),
            // musical G clef: 1d11e, has to be represented in surrogate pair form
                         Case("utf16 surrogate pair", "clef%D8%34%DD%1E", "clef\ud834\udd1e", UTF_16BE))

    data class Case(val name: String, val expected: String, val input: String, val charset: Charset = UTF_8)
}
