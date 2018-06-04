package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object ParseUrlTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun TestCase.parseUrl() {
        expectThrows<IllegalArgumentException>(expected) { input.toUrl() }
        expectThrows<IllegalArgumentException>(expected) { URL(input).toUrl() }
    }

    fun cases() = listOf(TestCase("http://foo.com/fo%2o", "Invalid %-tuple <%2o>"),
                         TestCase("http://foo.com/foo;m1=v1=v2", "Malformed matrix param: <m1=v1=v2>"))

    data class TestCase(val input: String, val expected: String)
}