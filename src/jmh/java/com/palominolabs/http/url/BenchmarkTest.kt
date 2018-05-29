package com.palominolabs.http.url

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.CharacterCodingException
import org.openjdk.jmh.annotations.*

abstract class BenchmarkTest {
    @Benchmark
    open fun testPercentEncodeTinyMix() = UrlPart.UnstructuredQuery.encode(TINY_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeSmallMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeLargeMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeSmallSafe() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_ALL_SAFE)

    @Benchmark
    open fun testPercentEncodeLargeSafe() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_ALL_SAFE)

    @Benchmark
    open fun testPercentEncodeSmallUnsafe() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_ALL_UNSAFE)

    @Benchmark
    open fun testPercentEncodeLargeUnsafe() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_ALL_UNSAFE)

    @Benchmark
    open fun testPercentEncodeSmallNoOpMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeLargeNoOpMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeSmallAccumXorMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    open fun testPercentEncodeLargeAccumXorMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    open fun testPercentDecodeSmall() = decode(UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX))

    @Benchmark
    open fun testPercentDecodeLarge() = decode(UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX))

    @Benchmark
    open fun testUrlEncodeSmall() = URLEncoder.encode(SMALL_STRING_MIX, "UTF-8")!!

    @Benchmark
    open fun testUrlEncodeLarge() = URLEncoder.encode(LARGE_STRING_MIX, "UTF-8")!!

    @Benchmark
    open fun testUrlDecodeSmall() = URLDecoder.decode(UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX), "UTF-8")!!

    @Benchmark
    open fun testUrlDecodeLarge() = URLDecoder.decode(UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX), "UTF-8")!!

    companion object {
        // safe and unsafe
        internal const val TINY_STRING_MIX = "foo bar baz"
        internal const val SMALL_STRING_MIX = "small value !@#$%^&*()???????????????!@#$%^&*()"

        // no characters escaped
        internal const val SMALL_STRING_ALL_SAFE = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

        // all characters escaped
        internal const val SMALL_STRING_ALL_UNSAFE = "???????????????????????????????????????????????"

        internal val LARGE_STRING_MIX = SMALL_STRING_MIX.repeat(1000)
        internal val LARGE_STRING_ALL_SAFE = SMALL_STRING_ALL_SAFE.repeat(1000)
        internal val LARGE_STRING_ALL_UNSAFE = SMALL_STRING_ALL_UNSAFE.repeat(1000)
    }
}
