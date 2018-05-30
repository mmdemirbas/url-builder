package com.palominolabs.http.url

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-30 12:24
 */
inline fun <reified T : Throwable> expectThrows(expectedMessage: String, noinline code: () -> Any) {
    val ex = assertThrows<T> { code() }
    Assertions.assertEquals(expectedMessage, ex.message)
}
