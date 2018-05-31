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
    fun TestCase.encode() {
        assertEquals(expected,
                     input.encodePercent(SafeChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"),
                                         charset))
    }

    fun cases() = listOf(TestCase("abcd+-*/", "abcd%2B%2D%2A%2F"),
                         TestCase("abc 123", "abc%20123"),
                         TestCase(" abc ", "%20abc%20"),
                         TestCase("snowman\u2603", "snowman%E2%98%83"),
                         TestCase("clef\ud834\udd1e", "clef%F0%9D%84%9E"),
                         TestCase("snowman\u2603", "snowman%26%03", UTF_16BE),
                         TestCase("clef\ud834\udd1e", "clef%D8%34%DD%1E", UTF_16BE))

    data class TestCase(val input: String, val expected: String, val charset: Charset = UTF_8)
}
