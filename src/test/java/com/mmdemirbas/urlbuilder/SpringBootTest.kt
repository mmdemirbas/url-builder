package com.mmdemirbas.urlbuilder


import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}


@RestController
@EnableAutoConfiguration
class EchoController {
    @RequestMapping("/{value}/echo")
    fun echo(@PathVariable value: String) = value
}


@SpringApplicationConfiguration(classes = [(Application::class)])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringBootTest {
    private val mockMvc = MockMvcBuilders.standaloneSetup(EchoController()).build()!!

    @Test
    fun `context loads`() = Unit

    @ParameterizedTest
    @ValueSource(strings = arrayOf("/test"))
    fun `not found`(path: String) {
        path.returns(NOT_FOUND)
    }

    @ParameterizedTest
    @MethodSource("okCases")
    fun Case.ok() {
        path.returns(OK).andExpect(content().string(expected))
    }

    fun okCases() = listOf(Case("/test/echo", "test"),
                           Case("/test/echo?query=param", "test"))

    data class Case(val path: String, val expected: String)

    private fun String.returns(expected: HttpStatus) =
            mockMvc.perform(get(this)).andExpect(status().`is`(expected.value())).andDo(print())
}