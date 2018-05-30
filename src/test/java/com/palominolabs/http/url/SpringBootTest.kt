package com.palominolabs.http.url


import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
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


@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = [(Application::class)])
class ApplicationTest {
    @Test
    fun contextLoads() = Unit
}


@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = [(Application::class)])
class SmokeTest {
    @Autowired lateinit var controller: EchoController

    @Test
    fun controllerLoads() = assertNotNull(controller)
}


@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = [(Application::class)])
class EchoControllerTest {
    val mockMvc = MockMvcBuilders.standaloneSetup(EchoController()).build()

    @Test
    fun testIndex() {
        "/test" returns NOT_FOUND
        "/test/echo" returns "test"
        "/test/echo?query=param" returns "test"
    }

    private infix fun String.returns(expected: String) = returns(OK).andExpect(content().string(expected))

    private infix fun String.returns(expected: HttpStatus) =
            mockMvc.perform(get(this)).andExpect(status().`is`(expected.value())).andDo(print())
}
