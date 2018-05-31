package com.mmdemirbas.urlbuilder.util

import java.io.PrintWriter
import java.net.Socket

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-31 13:45
 */
data class HttpRequest(val host: String = "localhost",
                       val port: Int = 80,
                       val method: String = "GET",
                       val path: String = "/",
                       val httpVersion: String = "HTTP/1.0") {
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
