package com.palominolabs.http.url

import org.junit.Assert
import org.junit.Test
import java.net.URL

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-29 12:23
 */
class ParseUrlTest {
    @Test
    fun percentDecodeInvalidPair() {
        expectedIllegalArgumentException("Invalid %-tuple <%2o>") {
            parseUrl(URL("http://foo.com/fo%2o"))
        }
    }

    @Test
    fun percentDecodeInvalidPair2() {
        expectedIllegalArgumentException("Invalid %-tuple <%2o>") {
            parseUrl("http://foo.com/fo%2o")
        }
    }

    @Test
    fun fromUrlWithMalformedMatrixPair() {
        expectedIllegalArgumentException("Malformed matrix param: <m1=v1=v2>") {
            parseUrl(URL("http://foo.com/foo;m1=v1=v2"))
        }
    }

    @Test
    fun fromUrlWithMalformedMatrixPair2() {
        expectedIllegalArgumentException("Malformed matrix param: <m1=v1=v2>") {
            parseUrl("http://foo.com/foo;m1=v1=v2")
        }
    }

    private fun expectedIllegalArgumentException(expectedMessage: String, code: () -> Any) {
        try {
            code()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(expectedMessage, e.message)
        }
    }
}