package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import java.io.PrintWriter
import java.net.Socket


data class HttpRequest(val host: String = "localhost", val port: Int = 80, val path: String = "/") {
    private val requestLine = "GET $path HTTP/1.0"

    /**
     * Performs a GET request using only low-level socket operations. (no 3rd-party library)
     */
    fun get() = Socket(host, port).use { socket ->
        // no auto-flushing
        PrintWriter(socket.outputStream, false).run {
            // native line endings are uncertain so add them manually
            print("$requestLine\r\n")
            print("\r\n")
            flush()
        }
        HttpResponse(socket.inputStream.bufferedReader().readText())
    }
}


data class HttpResponse(val status: Int, val content: String = "") {
    constructor(response: String) : this(response.split(' ', limit = 3)[1].toInt(),
                                         response.split("\r\n\r\n", limit = 2)[1])
}


inline fun <reified T : Throwable> expectThrows(expectedMessage: String, noinline code: () -> Any) {
    val ex = assertThrows<T> { code() }
    Assertions.assertEquals(expectedMessage, ex.message)
}
