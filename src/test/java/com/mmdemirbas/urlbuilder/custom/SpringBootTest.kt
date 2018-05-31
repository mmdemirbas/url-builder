package com.mmdemirbas.urlbuilder.custom


import com.mmdemirbas.urlbuilder.UrlPart
import com.mmdemirbas.urlbuilder.encode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.PrintWriter
import java.net.Socket


@SpringBootApplication
open class Application


@RestController
@EnableAutoConfiguration
class EchoController {
    @RequestMapping("/{value}/z")
    fun echo(@PathVariable value: String) = value
}


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object SpringBootTest {
    private val context = SpringApplication.run(Application::class.java)
    private val port = context.environment.getProperty("local.server.port").toInt()

    @AfterAll
    fun tearDown() = context.close()

    @ParameterizedTest
    @MethodSource("url structure cases")
    fun Case.`url structure cases`() = test(port)

    fun `url structure cases`(): List<Case> {
        return listOf(Case("not found", "/", notFound),
                      Case("not found", "/test", notFound),
                      Case("not found", "/test/c", notFound),
                      Case("simple", "/abc/z", ok("abc")),
                      Case("case-sensitive", "/aBc/z", ok("aBc")),
                      Case("structured query params", "/abc/z?query=param", ok("abc")),
                      Case("unstructured query params", "/abc/z?query&param==x", ok("abc")),
                      Case("matrix params after prefix", ";a=1/abc/z", ok("abc")),
                      Case("matrix params after path variable", "/abc;a=1/z", ok("abc")),
                      Case("matrix params after suffix", "/abc/z;a=1", ok("abc")))
    }

    @ParameterizedTest
    @MethodSource("encoding cases")
    fun Case.`encoding cases`() = test(port)

    fun `encoding cases`(): List<Case> {
        return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@?;".map { encoded(it) } + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@".map {
            notEncoded(it)
        } + notEncoded('/', notFound) + encoded('/', Response(400)) + Case("'/' double-encoded",
                                                                           "/${encode(encode('/'))}/z",
                                                                           ok(encode('/')))
    }

    @Disabled
    @Test
    fun `dot test`() {
        TODO("not implemented")
    }

    private fun encoded(c: Char, expected: Response = ok(c)) = Case("'$c' encoded", "/${encode(c)}/z", expected)
    private fun notEncoded(c: Char, expected: Response = ok(c)) = Case("'$c' not-encoded", "/$c/z", expected)

    private fun encode(c: Char) = encode(c.toString())
    private fun encode(s: String) = UrlPart.Path.encode(s)

    data class Case(val name: String, val path: String, val expected: Response) {
        fun test(port: Int) {
            val response = Request(host = "localhost", port = port, method = "GET", path = path).exec()
            when (expected) {
                notFound -> assertEquals(expected.status, response.status)
                else     -> assertEquals(expected, response)
            }
        }
    }
}

data class Request(val host: String = "localhost",
                   val port: Int = 80,
                   val method: String = "GET",
                   val path: String = "/",
                   val httpVersion: String = "HTTP/1.0") {
    fun exec(): Response {
        return Socket(host, port).use { socket ->
            // no auto-flushing
            PrintWriter(socket.outputStream, false).run {
                // native line endings are uncertain so add them manually
                print("$method $path $httpVersion\r\n")
                print("\r\n")
                flush()
            }
            Response(socket.inputStream.bufferedReader().readText())
        }
    }
}

private val notFound by lazy { Response(404) }
private fun ok(content: Char) = ok(content.toString())
private fun ok(content: String) = Response(200, content)

data class Response(val status: Int, val content: String = "") {
    constructor(response: String) : this(response.split(' ', limit = 3)[1].toInt(),
                                         response.split("\r\n\r\n", limit = 2)[1])
}
