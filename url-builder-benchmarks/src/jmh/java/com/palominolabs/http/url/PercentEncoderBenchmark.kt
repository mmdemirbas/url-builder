package com.palominolabs.http.url

import com.google.common.base.Strings
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

import java.nio.charset.CharacterCodingException

class PercentEncoderBenchmark {

    @State(Scope.Thread)
    class ThreadState {
        internal var encoder = UrlPercentEncoders.unstructuredQueryEncoder
        internal var noOpHandler: PercentEncoderOutputHandler = NoOpOutputHandler()
        internal var accumXorHandler = AccumXorOutputHandler()
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeTinyMix(state: ThreadState): String {
        return state.encoder.encode(TINY_STRING_MIX)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallMix(state: ThreadState): String {
        return state.encoder.encode(SMALL_STRING_MIX)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeMix(state: ThreadState): String {
        return state.encoder.encode(LARGE_STRING_MIX)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallSafe(state: ThreadState): String {
        return state.encoder.encode(SMALL_STRING_ALL_SAFE)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeSafe(state: ThreadState): String {
        return state.encoder.encode(LARGE_STRING_ALL_SAFE)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallUnsafe(state: ThreadState): String {
        return state.encoder.encode(SMALL_STRING_ALL_UNSAFE)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeUnsafe(state: ThreadState): String {
        return state.encoder.encode(LARGE_STRING_ALL_UNSAFE)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallNoOpMix(state: ThreadState) {
        state.encoder.encode(SMALL_STRING_MIX, state.noOpHandler)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeNoOpMix(state: ThreadState) {
        state.encoder.encode(LARGE_STRING_MIX, state.noOpHandler)
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeSmallAccumXorMix(state: ThreadState): Char {
        state.encoder.encode(SMALL_STRING_MIX, state.accumXorHandler)
        return state.accumXorHandler.c
    }

    @Benchmark
    @Throws(CharacterCodingException::class)
    fun testPercentEncodeLargeAccumXorMix(state: ThreadState): Char {
        state.encoder.encode(LARGE_STRING_MIX, state.accumXorHandler)
        return state.accumXorHandler.c
    }

    internal class NoOpOutputHandler : PercentEncoderOutputHandler {

        override fun onOutputChar(c: Char) {
            // no op
        }
    }

    /**
     * A handler that doesn't allocate, but can't be optimized away
     */
    internal class AccumXorOutputHandler : PercentEncoderOutputHandler {
        var c: Char = ' '

        override fun onOutputChar(c: Char) {
            this.c = this.c.toInt().xor(c.toInt()).toChar()
        }
    }

    companion object {

        // safe and unsafe
        internal val TINY_STRING_MIX = "foo bar baz"
        internal val SMALL_STRING_MIX = "small value !@#$%^&*()???????????????!@#$%^&*()"
        // no characters escaped
        internal val SMALL_STRING_ALL_SAFE = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        // all characters escaped
        internal val SMALL_STRING_ALL_UNSAFE = "???????????????????????????????????????????????"

        internal val LARGE_STRING_MIX: String
        internal val LARGE_STRING_ALL_SAFE: String
        internal val LARGE_STRING_ALL_UNSAFE: String

        init {
            LARGE_STRING_MIX = Strings.repeat(SMALL_STRING_MIX, 1000)
            LARGE_STRING_ALL_SAFE = Strings.repeat(SMALL_STRING_ALL_SAFE, 1000)
            LARGE_STRING_ALL_UNSAFE = Strings.repeat(SMALL_STRING_ALL_UNSAFE, 1000)
        }
    }
}
