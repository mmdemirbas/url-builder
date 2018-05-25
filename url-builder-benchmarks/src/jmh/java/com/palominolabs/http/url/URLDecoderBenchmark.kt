package com.palominolabs.http.url

import org.openjdk.jmh.annotations.Benchmark

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.CharacterCodingException

import com.palominolabs.http.url.PercentDecoderBenchmark.Companion.LARGE_STRING_ENCODED
import com.palominolabs.http.url.PercentDecoderBenchmark.Companion.SMALL_STRING_ENCODED

class URLDecoderBenchmark {

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlDecodeSmall(): String {
        return URLDecoder.decode(SMALL_STRING_ENCODED, "UTF-8")
    }

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlDecodeLarge(): String {
        return URLDecoder.decode(LARGE_STRING_ENCODED, "UTF-8")
    }
}
