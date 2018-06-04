package com.mmdemirbas.urlbuilder


import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
const val QUERY = "/query"
const val PARAM_NAME = "value"


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
 * A controller using a request parameter without any customization.
 */
@RestController
class DefaultRequestParamController {
    @RequestMapping(QUERY)
    fun echo(@RequestParam(name = PARAM_NAME) value: String) = value
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
    @MethodSource("url structure")
    fun TestCase.`url structure`() = test()

    fun `url structure`() = listOf(asIsPath("$AUTO", "", notFound),
                                   asIsPath("$AUTO/", "", notFound),
                                   asIsPath("$AUTO/", "a/b", notFound),
                                   asIsPath("$AUTO/", "abc"),
                                   asIsPath("$AUTO/", "aBc"),
                                   asIsPath("$AUTO/", "abc?query=param", ok("abc")),
                                   asIsPath("$AUTO/", "abc?query&param==x", ok("abc")),
                                   asIsPath("$AUTO;a=1/", "abc", ok("abc")),
                                   asIsPath("$AUTO/", "abc;a=1", ok("abc")),
                                   asIsPath("$AUTO/", "abc;a=1", ok("abc")),

                                   asIsPath("$MANUAL", ""), // we really want this?
                                   asIsPath("$MANUAL/", ""), // we really want this?
                                   asIsPath("$MANUAL/", "a/b"), // we really want this?
                                   asIsPath("$MANUAL/", "abc"),
                                   asIsPath("$MANUAL/", "aBc"),
                                   asIsPath("$MANUAL/", "abc?query=param", ok("abc")),
                                   asIsPath("$MANUAL/", "abc?query&param==x", ok("abc")),
                                   asIsPath("$MANUAL;a=1/", "abc", ok("abc")),
                                   asIsPath("$MANUAL/", "abc;a=1", ok("abc")),
                                   asIsPath("$MANUAL/", "abc;a=1", ok("abc")),

                                   asIsPath("$PATTERN", "", notFound),
                                   asIsPath("$PATTERN/", "", notFound),
                                   asIsPath("$PATTERN/", "a/b", notFound),
                                   asIsPath("$PATTERN/", "abc"),
                                   asIsPath("$PATTERN/", "aBc"),
                                   asIsPath("$PATTERN/", "abc?query=param", ok("abc")),
                                   asIsPath("$PATTERN/", "abc?query&param==x", ok("abc")),
                                   asIsPath("$PATTERN;a=1/", "abc", ok("abc")),
                                   asIsPath("$PATTERN/", "abc;a=1", ok("abc")),
                                   asIsPath("$PATTERN/", "abc;a=1", ok("abc")),

                                   asIsQuery(""),
                                   asIsQuery("a/b"),
                                   asIsQuery("abc"),
                                   asIsQuery("aBc"),
                                   asIsQuery("abc?query=param"),
                                   asIsQuery("abc?query&param==x", ok("abc?query")),
                                   asIsQuery("abc"),
                                   asIsQuery("abc;a=1"),
                                   asIsQuery("abc;a=1"))


    /**
     * Tests dot (`.`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("dot")
    fun TestCase.dot() = test()

    fun dot() = listOf(asIsPath("$AUTO/", ".", notFound), // should be ok(".")
                       asIsPath("$AUTO/", "..", notFound), // should be ok("..")
                       asIsPath("$AUTO/", "...", ok("..")), // should be ok("...")
                       asIsPath("$AUTO/", ".abc", ok("")), // should be ok(".abc")
                       asIsPath("$AUTO/", "abc.", ok("abc")), // should be ok("abc.")
                       asIsPath("$AUTO/", ".abc.", ok(".abc")), // should be ok(".abc.")
                       asIsPath("$AUTO/", "ab.c", ok("ab")), // should be ok("ab.c")
                       asIsPath("$AUTO/", "a.b.c", ok("a.b")), // should be ok("a.b.c")
                       asIsPath("$AUTO/", "a.b.c.", ok("a.b.c")), // should be ok("a.b.c.")
                       asIsPath("$AUTO/", ".a.b.c", ok(".a.b")), // should be ok(".a.b.c")
                       asIsPath("$AUTO/", ".a.b.c.", ok(".a.b.c")), // should be ok(".a.b.c.")

                       asIsPath("$MANUAL/", ".", notFound), // should be ok(".")
                       asIsPath("$MANUAL/", "..", notFound), // should be ok("..")
                       asIsPath("$MANUAL/", "..."),
                       asIsPath("$MANUAL/", ".abc"),
                       asIsPath("$MANUAL/", "abc."),
                       asIsPath("$MANUAL/", ".abc."),
                       asIsPath("$MANUAL/", "ab.c"),
                       asIsPath("$MANUAL/", "a.b.c"),
                       asIsPath("$MANUAL/", "a.b.c."),
                       asIsPath("$MANUAL/", ".a.b.c"),
                       asIsPath("$MANUAL/", ".a.b.c."),

                       asIsPath("$PATTERN/", ".", notFound), // should be ok(".")
                       asIsPath("$PATTERN/", "..", notFound), // should be ok("..")
                       asIsPath("$PATTERN/", "..."),
                       asIsPath("$PATTERN/", ".abc"),
                       asIsPath("$PATTERN/", "abc."),
                       asIsPath("$PATTERN/", ".abc."),
                       asIsPath("$PATTERN/", "ab.c"),
                       asIsPath("$PATTERN/", "a.b.c"),
                       asIsPath("$PATTERN/", "a.b.c."),
                       asIsPath("$PATTERN/", ".a.b.c"),
                       asIsPath("$PATTERN/", ".a.b.c."),

                       asIsQuery("."),
                       asIsQuery(".."),
                       asIsQuery("..."),
                       asIsQuery(".abc"),
                       asIsQuery("abc."),
                       asIsQuery(".abc."),
                       asIsQuery("ab.c"),
                       asIsQuery("a.b.c"),
                       asIsQuery("a.b.c."),
                       asIsQuery(".a.b.c"),
                       asIsQuery(".a.b.c."))


    /**
     * Tests slash (`/`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("slash")
    fun TestCase.slash() = test()

    fun slash() = listOf(asIsPath("$AUTO/", "/", notFound),
                         asIsPath("$AUTO/", "//", notFound),
                         asIsPath("$AUTO/", "///", notFound),
                         asIsPath("$AUTO/", "/abc", ok("abc")),
                         asIsPath("$AUTO/", "abc/", ok("abc")),
                         asIsPath("$AUTO/", "/abc/", ok("abc")),
                         asIsPath("$AUTO/", "ab/c", notFound),
                         asIsPath("$AUTO/", "a/b/c", notFound),
                         asIsPath("$AUTO/", "a/b/c/", notFound),
                         asIsPath("$AUTO/", "/a/b/c", notFound),
                         asIsPath("$AUTO/", "/a/b/c/", notFound),

                         asIsPath("$MANUAL/", "/", ok("")),
                         asIsPath("$MANUAL/", "//", ok("")),
                         asIsPath("$MANUAL/", "///", ok("")),
                         asIsPath("$MANUAL/", "/abc", ok("abc")),
                         asIsPath("$MANUAL/", "abc/", ok("abc")),
                         asIsPath("$MANUAL/", "/abc/", ok("abc")),
                         asIsPath("$MANUAL/", "ab/c", ok("ab/c")),
                         asIsPath("$MANUAL/", "a/b/c", ok("a/b/c")),
                         asIsPath("$MANUAL/", "a/b/c/", ok("a/b/c")),
                         asIsPath("$MANUAL/", "/a/b/c", ok("a/b/c")),
                         asIsPath("$MANUAL/", "/a/b/c/", ok("a/b/c")),

                         asIsPath("$PATTERN/", "/", notFound),
                         asIsPath("$PATTERN/", "//", notFound),
                         asIsPath("$PATTERN/", "///", notFound),
                         asIsPath("$PATTERN/", "/abc", ok("abc")),
                         asIsPath("$PATTERN/", "abc/", ok("abc")),
                         asIsPath("$PATTERN/", "/abc/", ok("abc")),
                         asIsPath("$PATTERN/", "ab/c", notFound),
                         asIsPath("$PATTERN/", "a/b/c", notFound),
                         asIsPath("$PATTERN/", "a/b/c/", notFound),
                         asIsPath("$PATTERN/", "/a/b/c", notFound),
                         asIsPath("$PATTERN/", "/a/b/c/", notFound),

                         asIsQuery("/"),
                         asIsQuery("//"),
                         asIsQuery("///"),
                         asIsQuery("/abc"),
                         asIsQuery("abc/"),
                         asIsQuery("/abc/"),
                         asIsQuery("ab/c"),
                         asIsQuery("a/b/c"),
                         asIsQuery("a/b/c/"),
                         asIsQuery("/a/b/c"),
                         asIsQuery("/a/b/c/"),

                         encodedPath("$AUTO/", "/", badRequest), // should be ok("/")
                         encodedPath("$AUTO/", "//", badRequest), // should be ok("//")
                         encodedPath("$AUTO/", "///", badRequest), // should be ok("///")
                         encodedPath("$AUTO/", "/abc", badRequest), // should be ok("/abc")
                         encodedPath("$AUTO/", "abc/", badRequest), // should be ok("abc/")
                         encodedPath("$AUTO/", "/abc/", badRequest), // should be ok("/abc/")
                         encodedPath("$AUTO/", "ab/c", badRequest), // should be ok("ab/c")
                         encodedPath("$AUTO/", "a/b/c", badRequest), // should be ok("a/b/c")
                         encodedPath("$AUTO/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                         encodedPath("$AUTO/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                         encodedPath("$AUTO/", "/a/b/c/", badRequest), // should be ok("/a/b/c/")

                         encodedPath("$MANUAL/", "/", badRequest), // should be ok("/")
                         encodedPath("$MANUAL/", "//", badRequest), // should be ok("//")
                         encodedPath("$MANUAL/", "///", badRequest), // should be ok("///")
                         encodedPath("$MANUAL/", "/abc", badRequest), // should be ok("/abc")
                         encodedPath("$MANUAL/", "abc/", badRequest), // should be ok("abc/")
                         encodedPath("$MANUAL/", "/abc/", badRequest), // should be ok("/abc/")
                         encodedPath("$MANUAL/", "ab/c", badRequest), // should be ok("ab/c")
                         encodedPath("$MANUAL/", "a/b/c", badRequest), // should be ok("a/b/c")
                         encodedPath("$MANUAL/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                         encodedPath("$MANUAL/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                         encodedPath("$MANUAL/", "/a/b/c/", badRequest), // should be ok("/a/b/c/")

                         encodedPath("$PATTERN/", "/", badRequest), // should be ok("/")
                         encodedPath("$PATTERN/", "//", badRequest), // should be ok("//")
                         encodedPath("$PATTERN/", "///", badRequest), // should be ok("///")
                         encodedPath("$PATTERN/", "/abc", badRequest), // should be ok("/abc")
                         encodedPath("$PATTERN/", "abc/", badRequest), // should be ok("abc/")
                         encodedPath("$PATTERN/", "/abc/", badRequest), // should be ok("/abc/")
                         encodedPath("$PATTERN/", "ab/c", badRequest), // should be ok("ab/c")
                         encodedPath("$PATTERN/", "a/b/c", badRequest), // should be ok("a/b/c")
                         encodedPath("$PATTERN/", "a/b/c/", badRequest), // should be ok("a/b/c/")
                         encodedPath("$PATTERN/", "/a/b/c", badRequest), // should be ok("/a/b/c")
                         encodedPath("$PATTERN/", "/a/b/c/", badRequest), // should be ok("/a/b/c/")

                         encodedQuery("/"),
                         encodedQuery("//"),
                         encodedQuery("///"),
                         encodedQuery("/abc"),
                         encodedQuery("abc/"),
                         encodedQuery("/abc/"),
                         encodedQuery("ab/c"),
                         encodedQuery("a/b/c"),
                         encodedQuery("a/b/c/"),
                         encodedQuery("/a/b/c"),
                         encodedQuery("/a/b/c/"))


    /**
     * Tests back-slash (`\`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("back slash")
    fun TestCase.`back slash`() = test()

    fun `back slash`() = listOf(asIsPath("$AUTO/", "\\", badRequest),
                                asIsPath("$AUTO/", "\\\\", badRequest),
                                asIsPath("$AUTO/", "\\\\\\", badRequest),
                                asIsPath("$AUTO/", "\\abc", badRequest),
                                asIsPath("$AUTO/", "abc\\", badRequest),
                                asIsPath("$AUTO/", "\\abc\\", badRequest),
                                asIsPath("$AUTO/", "ab\\c", badRequest),
                                asIsPath("$AUTO/", "a\\b\\c", badRequest),
                                asIsPath("$AUTO/", "a\\b\\c\\", badRequest),
                                asIsPath("$AUTO/", "\\a\\b\\c", badRequest),
                                asIsPath("$AUTO/", "\\a\\b\\c\\", badRequest),

                                asIsPath("$MANUAL/", "\\", badRequest),
                                asIsPath("$MANUAL/", "\\\\", badRequest),
                                asIsPath("$MANUAL/", "\\\\\\", badRequest),
                                asIsPath("$MANUAL/", "\\abc", badRequest),
                                asIsPath("$MANUAL/", "abc\\", badRequest),
                                asIsPath("$MANUAL/", "\\abc\\", badRequest),
                                asIsPath("$MANUAL/", "ab\\c", badRequest),
                                asIsPath("$MANUAL/", "a\\b\\c", badRequest),
                                asIsPath("$MANUAL/", "a\\b\\c\\", badRequest),
                                asIsPath("$MANUAL/", "\\a\\b\\c", badRequest),
                                asIsPath("$MANUAL/", "\\a\\b\\c\\", badRequest),

                                asIsPath("$PATTERN/", "\\", badRequest),
                                asIsPath("$PATTERN/", "\\\\", badRequest),
                                asIsPath("$PATTERN/", "\\\\\\", badRequest),
                                asIsPath("$PATTERN/", "\\abc", badRequest),
                                asIsPath("$PATTERN/", "abc\\", badRequest),
                                asIsPath("$PATTERN/", "\\abc\\", badRequest),
                                asIsPath("$PATTERN/", "ab\\c", badRequest),
                                asIsPath("$PATTERN/", "a\\b\\c", badRequest),
                                asIsPath("$PATTERN/", "a\\b\\c\\", badRequest),
                                asIsPath("$PATTERN/", "\\a\\b\\c", badRequest),
                                asIsPath("$PATTERN/", "\\a\\b\\c\\", badRequest),

                                asIsQuery("\\"),
                                asIsQuery("\\\\"),
                                asIsQuery("\\\\\\"),
                                asIsQuery("\\abc"),
                                asIsQuery("abc\\"),
                                asIsQuery("\\abc\\"),
                                asIsQuery("ab\\c"),
                                asIsQuery("a\\b\\c"),
                                asIsQuery("a\\b\\c\\"),
                                asIsQuery("\\a\\b\\c"),
                                asIsQuery("\\a\\b\\c\\"),

                                encodedPath("$AUTO/", "\\", badRequest), // should be ok("\\")
                                encodedPath("$AUTO/", "\\\\", badRequest), // should be ok("\\\\")
                                encodedPath("$AUTO/", "\\\\\\", badRequest), // should be ok("\\\\\\")
                                encodedPath("$AUTO/", "\\abc", badRequest), // should be ok("\\abc")
                                encodedPath("$AUTO/", "abc\\", badRequest), // should be ok("abc\\")
                                encodedPath("$AUTO/", "\\abc\\", badRequest), // should be ok("\\abc\\")
                                encodedPath("$AUTO/", "ab\\c", badRequest), // should be ok("ab\\c")
                                encodedPath("$AUTO/", "a\\b\\c", badRequest), // should be ok("a\\b\\c")
                                encodedPath("$AUTO/", "a\\b\\c\\", badRequest), // should be ok("a\\b\\c\\")
                                encodedPath("$AUTO/", "\\a\\b\\c", badRequest), // should be ok("\\a\\b\\c")
                                encodedPath("$AUTO/", "\\a\\b\\c\\", badRequest), // should be ok("\\a\\b\\c\\")

                                encodedPath("$MANUAL/", "\\", badRequest), // should be ok("\\")
                                encodedPath("$MANUAL/", "\\\\", badRequest), // should be ok("\\\\")
                                encodedPath("$MANUAL/", "\\\\\\", badRequest), // should be ok("\\\\\\")
                                encodedPath("$MANUAL/", "\\abc", badRequest), // should be ok("\\abc")
                                encodedPath("$MANUAL/", "abc\\", badRequest), // should be ok("abc\\")
                                encodedPath("$MANUAL/", "\\abc\\", badRequest), // should be ok("\\abc\\")
                                encodedPath("$MANUAL/", "ab\\c", badRequest), // should be ok("ab\\c")
                                encodedPath("$MANUAL/", "a\\b\\c", badRequest), // should be ok("a\\b\\c")
                                encodedPath("$MANUAL/", "a\\b\\c\\", badRequest), // should be ok("a\\b\\c\\")
                                encodedPath("$MANUAL/", "\\a\\b\\c", badRequest), // should be ok("\\a\\b\\c")
                                encodedPath("$MANUAL/", "\\a\\b\\c\\", badRequest), // should be ok("\\a\\b\\c\\")

                                encodedPath("$PATTERN/", "\\", badRequest), // should be ok("\\")
                                encodedPath("$PATTERN/", "\\\\", badRequest), // should be ok("\\\\")
                                encodedPath("$PATTERN/", "\\\\\\", badRequest), // should be ok("\\\\\\")
                                encodedPath("$PATTERN/", "\\abc", badRequest), // should be ok("\\abc")
                                encodedPath("$PATTERN/", "abc\\", badRequest), // should be ok("abc\\")
                                encodedPath("$PATTERN/", "\\abc\\", badRequest), // should be ok("\\abc\\")
                                encodedPath("$PATTERN/", "ab\\c", badRequest), // should be ok("ab\\c")
                                encodedPath("$PATTERN/", "a\\b\\c", badRequest), // should be ok("a\\b\\c")
                                encodedPath("$PATTERN/", "a\\b\\c\\", badRequest), // should be ok("a\\b\\c\\")
                                encodedPath("$PATTERN/", "\\a\\b\\c", badRequest), // should be ok("\\a\\b\\c")
                                encodedPath("$PATTERN/", "\\a\\b\\c\\", badRequest), // should be ok("\\a\\b\\c\\")

                                encodedQuery("\\"),
                                encodedQuery("\\\\"),
                                encodedQuery("\\\\\\"),
                                encodedQuery("\\abc"),
                                encodedQuery("abc\\"),
                                encodedQuery("\\abc\\"),
                                encodedQuery("ab\\c"),
                                encodedQuery("a\\b\\c"),
                                encodedQuery("a\\b\\c\\"),
                                encodedQuery("\\a\\b\\c"),
                                encodedQuery("\\a\\b\\c\\"))


    /**
     * Tests dot (`%`) character used in a path variable.
     */
    @ParameterizedTest
    @MethodSource("percent")
    fun TestCase.percent() = test()

    fun percent() = listOf(asIsPath("$AUTO/", "%", badRequest),
                           asIsPath("$AUTO/", "%%", badRequest),
                           asIsPath("$AUTO/", "%%%", badRequest),
                           asIsPath("$AUTO/", "%abc", ok("�c")),
                           asIsPath("$AUTO/", "%xyz", badRequest),
                           asIsPath("$AUTO/", "abc%", badRequest),
                           asIsPath("$AUTO/", "%abc%", badRequest),
                           asIsPath("$AUTO/", "ab%c", badRequest),
                           asIsPath("$AUTO/", "a%b%c", badRequest),
                           asIsPath("$AUTO/", "a%b%c%", badRequest),
                           asIsPath("$AUTO/", "%a%b%c", badRequest),
                           asIsPath("$AUTO/", "%a%b%c%", badRequest),

                           asIsPath("$MANUAL/", "%", badRequest),
                           asIsPath("$MANUAL/", "%%", badRequest),
                           asIsPath("$MANUAL/", "%%%", badRequest),
                           asIsPath("$MANUAL/", "%abc", ok("�c")),
                           asIsPath("$MANUAL/", "%xyz", badRequest),
                           asIsPath("$MANUAL/", "abc%", badRequest),
                           asIsPath("$MANUAL/", "%abc%", badRequest),
                           asIsPath("$MANUAL/", "ab%c", badRequest),
                           asIsPath("$MANUAL/", "a%b%c", badRequest),
                           asIsPath("$MANUAL/", "a%b%c%", badRequest),
                           asIsPath("$MANUAL/", "%a%b%c", badRequest),
                           asIsPath("$MANUAL/", "%a%b%c%", badRequest),

                           asIsPath("$PATTERN/", "%", badRequest),
                           asIsPath("$PATTERN/", "%%", badRequest),
                           asIsPath("$PATTERN/", "%%%", badRequest),
                           asIsPath("$PATTERN/", "%abc", ok("�c")),
                           asIsPath("$PATTERN/", "%xyz", badRequest),
                           asIsPath("$PATTERN/", "abc%", badRequest),
                           asIsPath("$PATTERN/", "%abc%", badRequest),
                           asIsPath("$PATTERN/", "ab%c", badRequest),
                           asIsPath("$PATTERN/", "a%b%c", badRequest),
                           asIsPath("$PATTERN/", "a%b%c%", badRequest),
                           asIsPath("$PATTERN/", "%a%b%c", badRequest),
                           asIsPath("$PATTERN/", "%a%b%c%", badRequest),

                           asIsQuery("%", badRequest),
                           asIsQuery("%%", badRequest),
                           asIsQuery("%%%", badRequest),
                           asIsQuery("%abc", ok("�c")),
                           asIsQuery("%xyz", badRequest),
                           asIsQuery("abc%", badRequest),
                           asIsQuery("%abc%", badRequest),
                           asIsQuery("ab%c", badRequest),
                           asIsQuery("a%b%c", badRequest),
                           asIsQuery("a%b%c%", badRequest),
                           asIsQuery("%a%b%c", badRequest),
                           asIsQuery("%a%b%c%", badRequest),

                           encodedPath("$AUTO/", "%"),
                           encodedPath("$AUTO/", "%%"),
                           encodedPath("$AUTO/", "%%%"),
                           encodedPath("$AUTO/", "%abc"),
                           encodedPath("$AUTO/", "abc%"),
                           encodedPath("$AUTO/", "%abc%"),
                           encodedPath("$AUTO/", "ab%c"),
                           encodedPath("$AUTO/", "a%b%c"),
                           encodedPath("$AUTO/", "a%b%c%"),
                           encodedPath("$AUTO/", "%a%b%c"),
                           encodedPath("$AUTO/", "%a%b%c%"),

                           encodedPath("$MANUAL/", "%"),
                           encodedPath("$MANUAL/", "%%"),
                           encodedPath("$MANUAL/", "%%%"),
                           encodedPath("$MANUAL/", "%abc"),
                           encodedPath("$MANUAL/", "abc%"),
                           encodedPath("$MANUAL/", "%abc%"),
                           encodedPath("$MANUAL/", "ab%c"),
                           encodedPath("$MANUAL/", "a%b%c"),
                           encodedPath("$MANUAL/", "a%b%c%"),
                           encodedPath("$MANUAL/", "%a%b%c"),
                           encodedPath("$MANUAL/", "%a%b%c%"),

                           encodedPath("$PATTERN/", "%"),
                           encodedPath("$PATTERN/", "%%"),
                           encodedPath("$PATTERN/", "%%%"),
                           encodedPath("$PATTERN/", "%abc"),
                           encodedPath("$PATTERN/", "abc%"),
                           encodedPath("$PATTERN/", "%abc%"),
                           encodedPath("$PATTERN/", "ab%c"),
                           encodedPath("$PATTERN/", "a%b%c"),
                           encodedPath("$PATTERN/", "a%b%c%"),
                           encodedPath("$PATTERN/", "%a%b%c"),
                           encodedPath("$PATTERN/", "%a%b%c%"),

                           encodedQuery("%"),
                           encodedQuery("%%"),
                           encodedQuery("%%%"),
                           encodedQuery("%abc"),
                           encodedQuery("%xyz"),
                           encodedQuery("abc%"),
                           encodedQuery("%abc%"),
                           encodedQuery("ab%c"),
                           encodedQuery("a%b%c"),
                           encodedQuery("a%b%c%"),
                           encodedQuery("%a%b%c"),
                           encodedQuery("%a%b%c%"))


    /**
     * Tests various characters used as path segments.
     */
    @ParameterizedTest
    @MethodSource("other characters")
    fun TestCase.`other characters`() = test()

    fun `other characters`(): List<TestCase> {
        val canEncode = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,=:@".explode()
        val shouldEncode = "çöğüşÇÖĞÜŞİı\"é<>£^#½§{[]}|@∑€®₺¥üiöπ¨æß∂ƒğ^∆¬´æ`<>|Ω≈√∫~≤≥÷".explode()
        val mustEncodeInPath = "?;".explode() // changes url structure - starter for query & matrix params
        val mustEncodeInQuery = "&+".explode()
        return forceEncodeOptional(canEncode + shouldEncode) + forceEncodeInPath(mustEncodeInPath) + forceEncodeInQuery(
                mustEncodeInQuery)
    }

    private fun String.explode() = toCharArray().map(Char::toString)
}


// mini-DSL for writing test cases

private fun forceEncodeOptional(s: List<String>) = forceEncodedAll(s) + asIsPath(s) + asIsQuery(s)
private fun forceEncodeInPath(s: List<String>) = forceEncodedAll(s) + asIsPath(s, notEqualTest = true) + asIsQuery(s)
private fun forceEncodeInQuery(s: List<String>) = forceEncodedAll(s) + asIsPath(s) + asIsQuery(s, notEqualTest = true)

private fun forceEncodedAll(s: List<String>) =
        pathPrefixes(s) { prefix, it -> forceEncodedPath(prefix, it) } + s.map { forceEncodedQuery(it) }

private fun asIsPath(s: List<String>, notEqualTest: Boolean = false) =
        pathPrefixes(s) { prefix, it -> asIsPath(prefix, it, notEqualTest = notEqualTest) }

private fun asIsQuery(s: List<String>, notEqualTest: Boolean = false) =
        s.map { asIsQuery(it, notEqualTest = notEqualTest) }

private fun pathPrefixes(s: List<String>, fn: (prefix: String, s: String) -> TestCase) =
        listOf(AUTO, MANUAL, PATTERN).map { "$it/" }.flatMap { prefix -> s.map { fn(prefix, it) } }

private fun encodedPath(prefix: String,
                        pathVariableToEncode: String,
                        expected: HttpResponse = ok(pathVariableToEncode)) =
        asIsPath(prefix, encodePathSegment(pathVariableToEncode), expected)

private fun forceEncodedPath(prefix: String,
                             pathVariableToEncode: String,
                             expected: HttpResponse = ok(pathVariableToEncode)) =
        asIsPath(prefix, forceEncodePathSegment(pathVariableToEncode), expected)

private fun asIsPath(prefix: String,
                     pathVariable: String,
                     expected: HttpResponse = ok(pathVariable),
                     notEqualTest: Boolean = false) = TestCase(prefix + pathVariable, expected, notEqualTest)

private fun forceEncodedQuery(queryParamToEncode: String, expected: HttpResponse = ok(queryParamToEncode)) =
        asIsQuery(forceEncodeQueryParam(queryParamToEncode), expected)

private fun encodedQuery(queryParamToEncode: String, expected: HttpResponse = ok(queryParamToEncode)) =
        asIsQuery(encodeQueryParam(queryParamToEncode), expected)

private fun asIsQuery(queryParam: String, expected: HttpResponse = ok(queryParam), notEqualTest: Boolean = false) =
        TestCase("$QUERY?$PARAM_NAME=$queryParam", expected, notEqualTest)

data class TestCase(val path: String, val expected: HttpResponse, val notEqualTest: Boolean = false) {
    fun test(port: Int) {
        val response = HttpRequest(host = "localhost", port = port, path = path).get()
        when (expected) {
            notFound, badRequest -> test(expected.status, response.status)
            else                 -> test(expected, response)
        }
    }

    private fun <T> test(expected: T, actual: T) = when {
        notEqualTest -> assertNotEquals(expected, actual)
        else         -> assertEquals(expected, actual)
    }
}


// mini-DSL for expected responses

private val notFound by lazy { HttpResponse(404) }
private val badRequest by lazy { HttpResponse(400) }
private fun ok(content: Char) = ok(content.toString())
private fun ok(content: String) = HttpResponse(200, content)


// encoding methods

private fun encodePathSegment(s: String) = s.encodePercent(SafeChars.Path)
private fun encodeQueryParam(s: String) = s.encodePercent(SafeChars.QueryParam)
private fun forceEncodePathSegment(s: String) = s.forceEncodePercent()
private fun forceEncodeQueryParam(s: String) = s.forceEncodePercent()
