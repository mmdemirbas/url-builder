package com.mmdemirbas.urlbuilder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import java.io.PrintWriter
import java.net.Socket


data class HttpRequest(val host: String = "localhost",
                       val port: Int = 80,
                       val method: String = "GET",
                       val path: String = "/",
                       val httpVersion: String = "HTTP/1.0") {
    /**
     * Performs the request using low-level socket operations.
     */
    fun exec(): HttpResponse {
        return Socket(host, port).use { socket ->
            // no auto-flushing
            PrintWriter(socket.outputStream, false).run {
                // native line endings are uncertain so add them manually
                print("$method $path $httpVersion\r\n")
                print("\r\n")
                flush()
            }
            HttpResponse(socket.inputStream.bufferedReader().readText())
        }
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
