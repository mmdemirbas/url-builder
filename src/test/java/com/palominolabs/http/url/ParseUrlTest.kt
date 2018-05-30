package com.palominolabs.http.url

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object ParseUrlTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun Case.parseUrl() {
        expectThrows<IllegalArgumentException>(expectedMessage) { parseUrl(input) }
        expectThrows<IllegalArgumentException>(expectedMessage) { parseUrl(URL(input)) }
    }

    fun cases() = listOf(Case("percentDecodeInvalidPair", "Invalid %-tuple <%2o>", "http://foo.com/fo%2o"),
                         Case("percentDecodeInvalidPair2", "Invalid %-tuple <%2o>", "http://foo.com/fo%2o"),
                         Case("fromUrlWithMalformedMatrixPair",
                              "Malformed matrix param: <m1=v1=v2>",
                              "http://foo.com/foo;m1=v1=v2"),
                         Case("fromUrlWithMalformedMatrixPair2",
                              "Malformed matrix param: <m1=v1=v2>",
                              "http://foo.com/foo;m1=v1=v2"))

    data class Case(val name: String, val expectedMessage: String, val input: String)
}