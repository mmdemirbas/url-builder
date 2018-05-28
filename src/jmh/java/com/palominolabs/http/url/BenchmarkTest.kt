package com.palominolabs.http.url

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.CharacterCodingException
import org.openjdk.jmh.annotations.*

abstract class BenchmarkTest {
    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeTinyMix() = UrlPart.UnstructuredQuery.encode(TINY_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeSmallMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeLargeMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeSmallSafe() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_ALL_SAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeLargeSafe() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_ALL_SAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeSmallUnsafe() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_ALL_UNSAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeLargeUnsafe() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_ALL_UNSAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeSmallNoOpMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeLargeNoOpMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeSmallAccumXorMix() = UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentEncodeLargeAccumXorMix() = UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentDecodeSmall() = decode(UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX))

    @Benchmark
    @Throws(CharacterCodingException::class)
    open fun testPercentDecodeLarge() = decode(UrlPart.UnstructuredQuery.encode(LARGE_STRING_MIX))

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    open fun testUrlEncodeSmall() = URLEncoder.encode(SMALL_STRING_MIX, "UTF-8")!!

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    open fun testUrlEncodeLarge() = URLEncoder.encode(LARGE_STRING_MIX, "UTF-8")!!

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
    open fun testUrlDecodeSmall() = URLDecoder.decode(UrlPart.UnstructuredQuery.encode(SMALL_STRING_MIX), "UTF-8")!!

    @Benchmark
    @Throws(CharacterCodingException::class, UnsupportedEncodingException::class)
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
