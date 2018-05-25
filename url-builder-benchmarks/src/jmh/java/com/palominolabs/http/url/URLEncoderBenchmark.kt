package com.palominolabs.http.url

import org.openjdk.jmh.annotations.Benchmark

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.CharacterCodingException

import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.LARGE_STRING_MIX
import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.SMALL_STRING_MIX

class URLEncoderBenchmark {

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlEncodeSmall(): String {
        return URLEncoder.encode(SMALL_STRING_MIX, "UTF-8")
    }

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlEncodeLarge(): String {
        return URLEncoder.encode(LARGE_STRING_MIX, "UTF-8")
    }
}
