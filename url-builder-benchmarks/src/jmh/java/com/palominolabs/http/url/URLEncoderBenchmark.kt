package com.palominolabs.http.url

import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.LARGE_STRING_MIX
import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.SMALL_STRING_MIX
import org.openjdk.jmh.annotations.Benchmark
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.CharacterCodingException

class URLEncoderBenchmark {

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlEncodeSmall() = URLEncoder.encode(SMALL_STRING_MIX, "UTF-8")!!

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    fun testUrlEncodeLarge() = URLEncoder.encode(LARGE_STRING_MIX, "UTF-8")!!
}
