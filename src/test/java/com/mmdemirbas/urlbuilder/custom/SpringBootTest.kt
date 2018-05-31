package com.mmdemirbas.urlbuilder.custom


import com.mmdemirbas.urlbuilder.UrlPart
import com.mmdemirbas.urlbuilder.encode
import com.mmdemirbas.urlbuilder.util.HttpRequest
import com.mmdemirbas.urlbuilder.util.HttpResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest


// spring application for test

@SpringBootApplication
open class Application

@RestController
@EnableAutoConfiguration
class PathVariableController {
    @RequestMapping("/path/{value}")
    fun echo(@PathVariable value: String) = value
}

@RestController
@EnableAutoConfiguration
class PathVariableWithSpecialCharPatchController {
    private val antMatcher by lazy { AntPathMatcher() }

    /**
     * [Stack Overflow - How to match a Spring @RequestMapping having a @pathVariable containing “/”](https://stackoverflow.com/a/2335449/471214)
     */
    @RequestMapping("/patch/**")
    fun echo(request: HttpServletRequest): String {
        // Don't repeat a pattern
        val pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        val value = antMatcher.extractPathWithinPattern(pattern, request.servletPath)!!
        return value
    }
}

@RestController
@EnableAutoConfiguration
class PathVariableWithSpecialCharPatternController {
    /**
     * [Mkyong.com - Spring MVC – @PathVariable dot (.) get truncated](https://www.mkyong.com/spring-mvc/spring-mvc-pathvariable-dot-get-truncated/)
     */
    @RequestMapping("/pattern/{value:.+}")
    fun echo(@PathVariable value: String) = value
}

@RestController
@EnableAutoConfiguration
class RequestParamController {
    @RequestMapping("/param")
    fun echo(@RequestParam(name = "value") value: String) = value
}


// tests

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object SpringBootTest {
    private val context = SpringApplication.run(Application::class.java)
    private val port = context.environment.getProperty("local.server.port").toInt()

    @AfterAll
    fun tearDown() = context.close()

    @ParameterizedTest
    @MethodSource("url structure cases")
    fun Case.`url structure cases`() = test(port)

    @ParameterizedTest
    @MethodSource("character encoding cases")
    fun Case.`character encoding cases`() = test(port)

    @ParameterizedTest
    @MethodSource("dot cases")
    fun Case.`dot cases`() = test(port)

    @ParameterizedTest
    @MethodSource("slash cases")
    fun Case.`slash cases`() = test(port)

    fun `url structure cases`() = listOf(asIs("/path", "", notFound),
                                         asIs("/path/", "", notFound),
                                         asIs("/path/", "a/b", notFound),
                                         asIs("/path/", "abc"),
                                         asIs("/path/", "aBc"),
                                         asIs("/path/", "abc?query=param", ok("abc")),
                                         asIs("/path/", "abc?query&param==x", ok("abc")),
                                         asIs("/path;a=1/", "abc", ok("abc")),
                                         asIs("/path/", "abc;a=1", ok("abc")),
                                         asIs("/path/", "abc;a=1", ok("abc")),

                                         asIs("/patch", ""), // we really want this?
                                         asIs("/patch/", ""), // we really want this?
                                         asIs("/patch/", "a/b"), // we really want this?
                                         asIs("/patch/", "abc"),
                                         asIs("/patch/", "aBc"),
                                         asIs("/patch/", "abc?query=param", ok("abc")),
                                         asIs("/patch/", "abc?query&param==x", ok("abc")),
                                         asIs("/patch;a=1/", "abc", ok("abc")),
                                         asIs("/patch/", "abc;a=1", ok("abc")),
                                         asIs("/patch/", "abc;a=1", ok("abc")),

                                         asIs("/pattern", "", notFound),
                                         asIs("/pattern/", "", notFound),
                                         asIs("/pattern/", "a/b", notFound),
                                         asIs("/pattern/", "abc"),
                                         asIs("/pattern/", "aBc"),
                                         asIs("/pattern/", "abc?query=param", ok("abc")),
                                         asIs("/pattern/", "abc?query&param==x", ok("abc")),
                                         asIs("/pattern;a=1/", "abc", ok("abc")),
                                         asIs("/pattern/", "abc;a=1", ok("abc")),
                                         asIs("/pattern/", "abc;a=1", ok("abc")))

    fun `character encoding cases`(): List<Case> {
        val a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@".flatMap {
            asIsAll(it.toString())
        }
        val b = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@?;".flatMap {
            encodedAll(it.toString())
        }
        val c = listOf(asIs("/path/", "/", notFound), asIs("/patch/", "/", ok("")) // we really want this?
                       , asIs("/pattern/", "/", notFound))
        val d = encodedAll("/", badRequest) // should be ok("/")
        val e = encodedAll(encodePathSegment("/"))
        return a + b + c + d + e
    }

    /**
     * Spring may behave strangely in the case the path variable contains dot character.
     *
     * See: [Mkyong.com - Spring MVC – @PathVariable dot (.) get truncated](https://www.mkyong.com/spring-mvc/spring-mvc-pathvariable-dot-get-truncated/)
     *
     *
     * Note that a global solution suggested to this problem:
     *
     * invoke `configurer.setUseSuffixPatternMatch(false)` inside `WebMvcConfigurerAdapter.configurePathMatch()`
     */
    fun `dot cases`() = listOf(asIs("/path/", ".", notFound), // should be ok(".")
                               asIs("/path/", "..", notFound), // should be ok("..")
                               asIs("/path/", "...", ok("..")), // should be ok("...")
                               asIs("/path/", ".abc", ok("")), // should be ok(".abc")
                               asIs("/path/", "abc.", ok("abc")), // should be ok("abc.")
                               asIs("/path/", ".abc.", ok(".abc")), // should be ok(".abc.")
                               asIs("/path/", "ab.c", ok("ab")), // should be ok("ab.c")
                               asIs("/path/", "a.b.c", ok("a.b")), // should be ok("a.b.c")
                               asIs("/path/", "a.b.c.", ok("a.b.c")), // should be ok("a.b.c.")
                               asIs("/path/", ".a.b.c", ok(".a.b")), // should be ok(".a.b.c")
                               asIs("/path/", ".a.b.c.", ok(".a.b.c")), // should be ok(".a.b.c.")

                               asIs("/patch/", ".", notFound), // should be ok(".")
                               asIs("/patch/", "..", notFound), // should be ok("..")
                               asIs("/patch/", "..."),
                               asIs("/patch/", ".abc"),
                               asIs("/patch/", "abc."),
                               asIs("/patch/", ".abc."),
                               asIs("/patch/", "ab.c"),
                               asIs("/patch/", "a.b.c"),
                               asIs("/patch/", "a.b.c."),
                               asIs("/patch/", ".a.b.c"),
                               asIs("/patch/", ".a.b.c."),

                               asIs("/pattern/", ".", notFound), // should be ok(".")
                               asIs("/pattern/", "..", notFound), // should be ok("..")
                               asIs("/pattern/", "..."),
                               asIs("/pattern/", ".abc"),
                               asIs("/pattern/", "abc."),
                               asIs("/pattern/", ".abc."),
                               asIs("/pattern/", "ab.c"),
                               asIs("/pattern/", "a.b.c"),
                               asIs("/pattern/", "a.b.c."),
                               asIs("/pattern/", ".a.b.c"),
                               asIs("/pattern/", ".a.b.c."))

    /**
     * Spring can not handle slash character in path segments, even if it is encoded.
     * See: [Stack Overflow - Spring @RequestMapping handling of special characters](https://stackoverflow.com/a/31058480/471214)
     */
    fun `slash cases`() = listOf(asIs("/path/", "/", notFound),
                                 asIs("/path/", "//", notFound),
                                 asIs("/path/", "///", notFound),
                                 asIs("/path/", "/abc", ok("abc")),
                                 asIs("/path/", "abc/", ok("abc")),
                                 asIs("/path/", "/abc/", ok("abc")),
                                 asIs("/path/", "ab/c", notFound),
                                 asIs("/path/", "a/b/c", notFound),
                                 asIs("/path/", "a/b/c/", notFound),
                                 asIs("/path/", "/a/b/c", notFound),
                                 asIs("/path/", "/a/b/c/", notFound),

                                 encoded("/path/", "/", badRequest), // should be ok("/")
                                 encoded("/path/", "//", badRequest), // should be ok("//")
                                 encoded("/path/", "///", badRequest), // should be ok("///")
                                 encoded("/path/", "/abc", badRequest), // should be ok("/abc")
                                 encoded("/path/", "abc/", badRequest), // should be ok("abc/")
                                 encoded("/path/", "/abc/", badRequest), // should be ok("/abc/")
                                 encoded("/path/", "ab/c", badRequest), // should be ok("ab/c")
                                 encoded("/path/", "a/b/c", badRequest), // should be ok("a/b/c")
                                 encoded("/path/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                                 encoded("/path/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                                 encoded("/path/", "/a/b/c/", badRequest) // should be ok("/a/b/c/")
                                )

    private fun asIsAll(pathSegment: String, expected: HttpResponse = ok(pathSegment)) =
            listOf("/path/", "/patch/", "/pattern/").map { asIs(it, pathSegment, expected) }

    private fun encodedAll(pathSegmentToEncode: String, expected: HttpResponse = ok(pathSegmentToEncode)) =
            listOf("/path/", "/patch/", "/pattern/").map { encoded(it, pathSegmentToEncode, expected) }

    private fun asIs(pathPrefix: String, pathSegment: String, expected: HttpResponse = ok(pathSegment)) =
            Case(pathPrefix + pathSegment, expected)

    private fun encoded(pathPrefix: String,
                        pathSegmentToEncode: String,
                        expected: HttpResponse = ok(pathSegmentToEncode)) =
            Case(pathPrefix + encodePathSegment(pathSegmentToEncode), expected)

    data class Case(val path: String, val expected: HttpResponse) {
        fun test(port: Int) {
            val response = HttpRequest(host = "localhost", port = port, method = "GET", path = path).exec()
            when (expected) {
                notFound -> assertEquals(expected.status, response.status)
                else     -> assertEquals(expected, response)
            }
        }
    }

    private fun encodePathSegment(s: String) = UrlPart.Path.encode(s)
}

// responses

private val notFound by lazy { HttpResponse(404) }
private val badRequest by lazy { HttpResponse(400) }
private fun ok(content: Char) = ok(content.toString())
private fun ok(content: String) = HttpResponse(200, content)