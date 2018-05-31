package com.mmdemirbas.urlbuilder


import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest


/**
 * Sample Spring app for test.
 */
@SpringBootApplication
open class Application


// endpoints
const val AUTO = "/auto"
const val MANUAL = "/manual"
const val PATTERN = "/pattern"


/**
 * A controller using a path variable without any customization.
 */
@RestController
class DefaultPathVariableController {
    @RequestMapping("$AUTO/{value}")
    fun echo(@PathVariable value: String) = value
}


/**
 * A controller which is manually resolving paths to handle slash (`/`) character in path variables.
 *
 * Spring can not handle slash character in path segments, even if it is encoded.
 *
 * See: [Stack Overflow - How to match a Spring @RequestMapping having a @pathVariable containing “/”](https://stackoverflow.com/a/2335449/471214)
 */
@RestController
class ManualPathResolvingController {
    /**
     * Shared thread-safe [AntPathMatcher] instance.
     */
    private val antPathMatcher by lazy { AntPathMatcher() }

    @RequestMapping("$MANUAL/**")
    fun echo(request: HttpServletRequest): String {
        // Don't repeat a pattern
        val pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        val value = antPathMatcher.extractPathWithinPattern(pattern, request.servletPath)!!
        return value
    }
}


/**
 * A controller which defines a pattern to overcome dot (`.`) character truncation problem.
 *
 * Spring may behave strangely in the case of dot character in path variable.
 *
 * See: [Mkyong.com - Spring MVC – @PathVariable dot (.) get truncated](https://www.mkyong.com/spring-mvc/spring-mvc-pathvariable-dot-get-truncated/)
 *
 * Note that also a global solution suggested to this problem:
 *
 * invoke `configurer.setUseSuffixPatternMatch(false)` inside `WebMvcConfigurerAdapter.configurePathMatch()`
 */
@RestController
class PathVariableWithPattern {
    @RequestMapping("$PATTERN/{value:.+}")
    fun echo(@PathVariable value: String) = value
}


/**
 * Test to ensure Spring framework behaviour in the case of various HTTP requests such as;
 *  - various URL structures
 *  - various characters encoded/not-encoded
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object SpringTest {
    private val application = SpringApplication.run(Application::class.java)
    private val serverPort = application.environment.getProperty("local.server.port").toInt()

    @AfterAll
    fun tearDown() = application.close()

    fun TestCase.test() = test(serverPort)


    /**
     * Tests various URL structures.
     */
    @ParameterizedTest
    @MethodSource("url structure cases")
    fun TestCase.`url structure cases`() = test()

    fun `url structure cases`() = listOf(asIs("$AUTO", "", notFound),
                                         asIs("$AUTO/", "", notFound),
                                         asIs("$AUTO/", "a/b", notFound),
                                         asIs("$AUTO/", "abc"),
                                         asIs("$AUTO/", "aBc"),
                                         asIs("$AUTO/", "abc?query=param", ok("abc")),
                                         asIs("$AUTO/", "abc?query&param==x", ok("abc")),
                                         asIs("$AUTO;a=1/", "abc", ok("abc")),
                                         asIs("$AUTO/", "abc;a=1", ok("abc")),
                                         asIs("$AUTO/", "abc;a=1", ok("abc")),

                                         asIs("$MANUAL", ""), // we really want this?
                                         asIs("$MANUAL/", ""), // we really want this?
                                         asIs("$MANUAL/", "a/b"), // we really want this?
                                         asIs("$MANUAL/", "abc"),
                                         asIs("$MANUAL/", "aBc"),
                                         asIs("$MANUAL/", "abc?query=param", ok("abc")),
                                         asIs("$MANUAL/", "abc?query&param==x", ok("abc")),
                                         asIs("$MANUAL;a=1/", "abc", ok("abc")),
                                         asIs("$MANUAL/", "abc;a=1", ok("abc")),
                                         asIs("$MANUAL/", "abc;a=1", ok("abc")),

                                         asIs("$PATTERN", "", notFound),
                                         asIs("$PATTERN/", "", notFound),
                                         asIs("$PATTERN/", "a/b", notFound),
                                         asIs("$PATTERN/", "abc"),
                                         asIs("$PATTERN/", "aBc"),
                                         asIs("$PATTERN/", "abc?query=param", ok("abc")),
                                         asIs("$PATTERN/", "abc?query&param==x", ok("abc")),
                                         asIs("$PATTERN;a=1/", "abc", ok("abc")),
                                         asIs("$PATTERN/", "abc;a=1", ok("abc")),
                                         asIs("$PATTERN/", "abc;a=1", ok("abc")))


    /**
     * Tests various characters used as path segments.
     */
    @ParameterizedTest
    @MethodSource("character encoding cases")
    fun TestCase.`character encoding cases`() = test()

    fun `character encoding cases`(): List<TestCase> {
        val safeInPath = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@".flatMap { c ->
            listOf("$AUTO/", "$MANUAL/", "$PATTERN/").flatMap { prefix ->
                listOf(asIs(prefix, c.toString()), encoded(prefix, c.toString()))
            }
        }
        // following characters change url structure when used in a path segment without encoded.
        // ; used for matrix params, ? used for query params.
        val nonSafeInPath = "?;".flatMap { c ->
            listOf("$AUTO/", "$MANUAL/", "$PATTERN/").map {
                encoded(it, c.toString())
            }
        }
        // dot and slash have their own tests
        return safeInPath + nonSafeInPath
    }


    /**
     * Tests dot (`.`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("dot cases")
    fun TestCase.`dot cases`() = test()

    fun `dot cases`() = listOf(asIs("$AUTO/", ".", notFound), // should be ok(".")
                               asIs("$AUTO/", "", notFound), // should be ok("..")
                               asIs("$AUTO/", "...", ok("")), // should be ok("...")
                               asIs("$AUTO/", ".abc", ok("")), // should be ok(".abc")
                               asIs("$AUTO/", "abc.", ok("abc")), // should be ok("abc.")
                               asIs("$AUTO/", ".abc.", ok(".abc")), // should be ok(".abc.")
                               asIs("$AUTO/", "ab.c", ok("ab")), // should be ok("ab.c")
                               asIs("$AUTO/", "a.b.c", ok("a.b")), // should be ok("a.b.c")
                               asIs("$AUTO/", "a.b.c.", ok("a.b.c")), // should be ok("a.b.c.")
                               asIs("$AUTO/", ".a.b.c", ok(".a.b")), // should be ok(".a.b.c")
                               asIs("$AUTO/", ".a.b.c.", ok(".a.b.c")), // should be ok(".a.b.c.")

                               asIs("$MANUAL/", ".", notFound), // should be ok(".")
                               asIs("$MANUAL/", "", notFound), // should be ok("..")
                               asIs("$MANUAL/", "..."),
                               asIs("$MANUAL/", ".abc"),
                               asIs("$MANUAL/", "abc."),
                               asIs("$MANUAL/", ".abc."),
                               asIs("$MANUAL/", "ab.c"),
                               asIs("$MANUAL/", "a.b.c"),
                               asIs("$MANUAL/", "a.b.c."),
                               asIs("$MANUAL/", ".a.b.c"),
                               asIs("$MANUAL/", ".a.b.c."),

                               asIs("$PATTERN/", ".", notFound), // should be ok(".")
                               asIs("$PATTERN/", "", notFound), // should be ok("..")
                               asIs("$PATTERN/", "..."),
                               asIs("$PATTERN/", ".abc"),
                               asIs("$PATTERN/", "abc."),
                               asIs("$PATTERN/", ".abc."),
                               asIs("$PATTERN/", "ab.c"),
                               asIs("$PATTERN/", "a.b.c"),
                               asIs("$PATTERN/", "a.b.c."),
                               asIs("$PATTERN/", ".a.b.c"),
                               asIs("$PATTERN/", ".a.b.c."))


    /**
     * Tests slash (`/`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("slash cases")
    fun TestCase.`slash cases`() = test()

    fun `slash cases`() = listOf(asIs("$AUTO/", "/", notFound),
                                 asIs("$AUTO/", "//", notFound),
                                 asIs("$AUTO/", "///", notFound),
                                 asIs("$AUTO/", "/abc", ok("abc")),
                                 asIs("$AUTO/", "abc/", ok("abc")),
                                 asIs("$AUTO/", "/abc/", ok("abc")),
                                 asIs("$AUTO/", "ab/c", notFound),
                                 asIs("$AUTO/", "a/b/c", notFound),
                                 asIs("$AUTO/", "a/b/c/", notFound),
                                 asIs("$AUTO/", "/a/b/c", notFound),
                                 asIs("$AUTO/", "/a/b/c/", notFound),

                                 asIs("$MANUAL/", "/", ok("")),
                                 asIs("$MANUAL/", "//", ok("")),
                                 asIs("$MANUAL/", "///", ok("")),
                                 asIs("$MANUAL/", "/abc", ok("abc")),
                                 asIs("$MANUAL/", "abc/", ok("abc")),
                                 asIs("$MANUAL/", "/abc/", ok("abc")),
                                 asIs("$MANUAL/", "ab/c", ok("ab/c")),
                                 asIs("$MANUAL/", "a/b/c", ok("a/b/c")),
                                 asIs("$MANUAL/", "a/b/c/", ok("a/b/c")),
                                 asIs("$MANUAL/", "/a/b/c", ok("a/b/c")),
                                 asIs("$MANUAL/", "/a/b/c/", ok("a/b/c")),

                                 asIs("$PATTERN/", "/", notFound),
                                 asIs("$PATTERN/", "//", notFound),
                                 asIs("$PATTERN/", "///", notFound),
                                 asIs("$PATTERN/", "/abc", ok("abc")),
                                 asIs("$PATTERN/", "abc/", ok("abc")),
                                 asIs("$PATTERN/", "/abc/", ok("abc")),
                                 asIs("$PATTERN/", "ab/c", notFound),
                                 asIs("$PATTERN/", "a/b/c", notFound),
                                 asIs("$PATTERN/", "a/b/c/", notFound),
                                 asIs("$PATTERN/", "/a/b/c", notFound),
                                 asIs("$PATTERN/", "/a/b/c/", notFound),

                                 encoded("$AUTO/", "/", badRequest), // should be ok("/")
                                 encoded("$AUTO/", "//", badRequest), // should be ok("//")
                                 encoded("$AUTO/", "///", badRequest), // should be ok("///")
                                 encoded("$AUTO/", "/abc", badRequest), // should be ok("/abc")
                                 encoded("$AUTO/", "abc/", badRequest), // should be ok("abc/")
                                 encoded("$AUTO/", "/abc/", badRequest), // should be ok("/abc/")
                                 encoded("$AUTO/", "ab/c", badRequest), // should be ok("ab/c")
                                 encoded("$AUTO/", "a/b/c", badRequest), // should be ok("a/b/c")
                                 encoded("$AUTO/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                                 encoded("$AUTO/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                                 encoded("$AUTO/", "/a/b/c/", badRequest), // should be ok("/a/b/c/")

                                 encoded("$MANUAL/", "/", badRequest), // should be ok("/")
                                 encoded("$MANUAL/", "//", badRequest), // should be ok("//")
                                 encoded("$MANUAL/", "///", badRequest), // should be ok("///")
                                 encoded("$MANUAL/", "/abc", badRequest), // should be ok("/abc")
                                 encoded("$MANUAL/", "abc/", badRequest), // should be ok("abc/")
                                 encoded("$MANUAL/", "/abc/", badRequest), // should be ok("/abc/")
                                 encoded("$MANUAL/", "ab/c", badRequest), // should be ok("ab/c")
                                 encoded("$MANUAL/", "a/b/c", badRequest), // should be ok("a/b/c")
                                 encoded("$MANUAL/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                                 encoded("$MANUAL/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                                 encoded("$MANUAL/", "/a/b/c/", badRequest), // should be ok("/a/b/c/")

                                 encoded("$PATTERN/", "/", badRequest), // should be ok("/")
                                 encoded("$PATTERN/", "//", badRequest), // should be ok("//")
                                 encoded("$PATTERN/", "///", badRequest), // should be ok("///")
                                 encoded("$PATTERN/", "/abc", badRequest), // should be ok("/abc")
                                 encoded("$PATTERN/", "abc/", badRequest), // should be ok("abc/")
                                 encoded("$PATTERN/", "/abc/", badRequest), // should be ok("/abc/")
                                 encoded("$PATTERN/", "ab/c", badRequest), // should be ok("ab/c")
                                 encoded("$PATTERN/", "a/b/c", badRequest), // should be ok("a/b/c")
                                 encoded("$PATTERN/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                                 encoded("$PATTERN/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                                 encoded("$PATTERN/", "/a/b/c/", badRequest) // should be ok("/a/b/c/")
                                )
}


// mini-DSL for writing test cases

private fun asIs(prefix: String, pathVariable: String, expected: HttpResponse = ok(pathVariable)) =
        TestCase(prefix + pathVariable, expected)

private fun encoded(prefix: String, pathVariableToEncode: String, expected: HttpResponse = ok(pathVariableToEncode)) =
        TestCase(prefix + encodePathSegment(pathVariableToEncode), expected)

data class TestCase(val path: String, val expected: HttpResponse) {
    fun test(port: Int) {
        val response = HttpRequest(host = "localhost", port = port, method = "GET", path = path).exec()
        when (expected) {
            notFound -> assertEquals(expected.status, response.status)
            else     -> assertEquals(expected, response)
        }
    }
}


// mini-DSL for expected responses

private val notFound by lazy { HttpResponse(404) }
private val badRequest by lazy { HttpResponse(400) }
private fun ok(content: Char) = ok(content.toString())
private fun ok(content: String) = HttpResponse(200, content)


/**
 * All path segment encoding operations delegated to this method.
 */
private fun encodePathSegment(s: String) = s.encodePercent(SafeChars.Path)
