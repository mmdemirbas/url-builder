package com.palominolabs.http.url

import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.LARGE_STRING_MIX
import com.palominolabs.http.url.PercentEncoderBenchmark.Companion.SMALL_STRING_MIX
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets

class PercentDecoderBenchmark {
    @State(Scope.Thread)
    class ThreadState {
        internal var decoder = PercentDecoder(StandardCharsets.UTF_8.newDecoder())
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentDecodeSmall(state: ThreadState) = state.decoder.decode(SMALL_STRING_ENCODED)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentDecodeLarge(state: ThreadState) = state.decoder.decode(LARGE_STRING_ENCODED)

    companion object {
        internal val SMALL_STRING_ENCODED: String
        internal val LARGE_STRING_ENCODED: String

        init {
            val encoder = PercentEncoders.newUnstructuredQueryEncoder()
            SMALL_STRING_ENCODED = encoder.encode(SMALL_STRING_MIX)
            LARGE_STRING_ENCODED = encoder.encode(LARGE_STRING_MIX)
        }
    }
}
