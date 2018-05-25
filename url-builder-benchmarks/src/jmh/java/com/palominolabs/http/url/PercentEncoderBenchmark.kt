package com.palominolabs.http.url

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.nio.charset.CharacterCodingException

class PercentEncoderBenchmark {

    @State(Scope.Thread)
    class ThreadState {
        internal var encoder = PercentEncoders.newUnstructuredQueryEncoder()
        internal var noOpHandler = { c: Char -> }

        var c: Char = ' '
        /**
         * A handler that doesn't allocate, but can't be optimized away
         */
        internal var accumXorHandler = { c: Char -> this.c = this.c.toInt().xor(c.toInt()).toChar() }
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeTinyMix(state: ThreadState) = state.encoder.encode(TINY_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallMix(state: ThreadState) = state.encoder.encode(SMALL_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeMix(state: ThreadState) = state.encoder.encode(LARGE_STRING_MIX)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallSafe(state: ThreadState) = state.encoder.encode(SMALL_STRING_ALL_SAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeSafe(state: ThreadState) = state.encoder.encode(LARGE_STRING_ALL_SAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallUnsafe(state: ThreadState) = state.encoder.encode(SMALL_STRING_ALL_UNSAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeUnsafe(state: ThreadState) = state.encoder.encode(LARGE_STRING_ALL_UNSAFE)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallNoOpMix(state: ThreadState) = state.encoder.encode(SMALL_STRING_MIX, state.noOpHandler)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeNoOpMix(state: ThreadState) = state.encoder.encode(LARGE_STRING_MIX, state.noOpHandler)

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallAccumXorMix(state: ThreadState): Char {
        state.encoder.encode(SMALL_STRING_MIX, state.accumXorHandler)
        return state.c
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeAccumXorMix(state: ThreadState): Char {
        state.encoder.encode(LARGE_STRING_MIX, state.accumXorHandler)
        return state.c
    }

    companion object {
        // safe and unsafe
        internal const val TINY_STRING_MIX = "foo bar baz"
        internal const val SMALL_STRING_MIX = "small value !@#$%^&*()???????????????!@#$%^&*()"

        // no characters escaped
        internal const val SMALL_STRING_ALL_SAFE = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

        // all characters escaped
        internal const val SMALL_STRING_ALL_UNSAFE = "???????????????????????????????????????????????"

        internal val LARGE_STRING_MIX: String
        internal val LARGE_STRING_ALL_SAFE: String
        internal val LARGE_STRING_ALL_UNSAFE: String

        init {
            LARGE_STRING_MIX = SMALL_STRING_MIX.repeat(1000)
            LARGE_STRING_ALL_SAFE = SMALL_STRING_ALL_SAFE.repeat(1000)
            LARGE_STRING_ALL_UNSAFE = SMALL_STRING_ALL_UNSAFE.repeat(1000)
        }
    }
}
