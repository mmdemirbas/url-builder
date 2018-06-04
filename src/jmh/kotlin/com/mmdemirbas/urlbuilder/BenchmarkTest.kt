package com.mmdemirbas.urlbuilder

import org.openjdk.jmh.annotations.Benchmark
import java.net.URLDecoder
import java.net.URLEncoder

open class BenchmarkTest {
    companion object {
        const      val TINY_MIX          = "foo bar baz"
        const      val SMALL_MIX         = "small value !@#$%^&*()???????????????!@#$%^&*()"
        const      val SMALL_SAFE        = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        const      val SMALL_UNSAFE      = "???????????????????????????????????????????????"

        @JvmStatic val LARGE_MIX        = SMALL_MIX.repeat(1000)
        @JvmStatic val LARGE_SAFE       = SMALL_SAFE.repeat(1000)
        @JvmStatic val LARGE_UNSAFE     = SMALL_UNSAFE.repeat(1000)

        @JvmStatic val SMALL_MIX_ENCODED= doEncode(SMALL_MIX)
        @JvmStatic val LARGE_MIX_ENCODED= doEncode(LARGE_MIX)

        fun doEncode(s: String) = s.encodePercent(SafeChars.UnstructuredQuery)
        fun doDecode(s: String) = s.decodePercent()
    }

    @Benchmark open fun encodeTinyMix    () = doEncode(TINY_MIX)
    @Benchmark open fun encodeSmallMix   () = doEncode(SMALL_MIX)
    @Benchmark open fun encodeLargeMix   () = doEncode(LARGE_MIX)
    @Benchmark open fun encodeSmallSafe  () = doEncode(SMALL_SAFE)
    @Benchmark open fun encodeLargeSafe  () = doEncode(LARGE_SAFE)
    @Benchmark open fun encodeSmallUnsafe() = doEncode(SMALL_UNSAFE)
    @Benchmark open fun encodeLargeUnsafe() = doEncode(LARGE_UNSAFE)
    @Benchmark open fun decodeSmallMix   () = doDecode(SMALL_MIX_ENCODED)
    @Benchmark open fun decodeLargeMix   () = doDecode(LARGE_MIX_ENCODED)
    @Benchmark open fun urlEncodeSmallMix() = URLEncoder.encode(SMALL_MIX, "UTF-8")!!
    @Benchmark open fun urlEncodeLargeMix() = URLEncoder.encode(LARGE_MIX, "UTF-8")!!
    @Benchmark open fun urlDecodeSmallMix() = URLDecoder.decode(SMALL_MIX_ENCODED, "UTF-8")!!
    @Benchmark open fun urlDecodeLargeMix() = URLDecoder.decode(LARGE_MIX_ENCODED, "UTF-8")!!
}
