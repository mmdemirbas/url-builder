package com.palominolabs.http.url

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.text.Charsets.UTF_8

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object DecodeTest {
    @ParameterizedTest
    @MethodSource("okCases")
    fun Case.decode() = assert(expected == decode(input))

    @ParameterizedTest
    @MethodSource("throwingCases")
    fun Case.decodeThrows() = expectThrows<IllegalArgumentException>(expected) { decode(input) }

    fun okCases() = listOf(Case("decodesWithoutPercents", "asdf", "asdf"), Case("decodeSingleByte", "#", "%23"))

    fun throwingCases() = listOf(Case("incompletePercentPairNoNumbers",
                                      "Could not percent decode <%>: incomplete %-pair at position 0",
                                      "%"),
                                 Case("incompletePercentPairOneNumber",
                                      "Could not percent decode <%2>: incomplete %-pair at position 0",
                                      "%2"),
                                 Case("invalidHex", "Invalid %-tuple <%xz>", "%xz"))

    data class Case(val name: String, val expected: String, val input: String)

    @Test
    fun `random strings`() {
        val rand = Random()
        val seed = rand.nextLong()
        rand.setSeed(seed)

        val charBuf = CharArray(2)

        (1..10000).forEach {
            val buf = StringBuilder()
            val codePoints = ArrayList<Int>()
            randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000))

            val origBytes = buf.toString().toByteArray(UTF_8)
            var decodedBytes: ByteArray? = null
            val codePointsHex = codePoints.map {
                Integer.toHexString(it)
            }

            try {
                decodedBytes = decode(UrlPart.UnstructuredQuery.encode(buf.toString())).toByteArray(UTF_8)
            } catch (e: IllegalArgumentException) {
                val charHex = (0 until buf.toString().length).map { Integer.toHexString(buf.toString()[it].toInt()) }
                fail("seed: $seed code points: $codePointsHex chars $charHex ${e.message}")
            }

            assertEquals("Seed: $seed Code points: $codePointsHex", origBytes.toHex(), decodedBytes!!.toHex())
        }
    }

    private fun ByteArray.toHex() = map { Integer.toHexString(it.toInt() and 0xFF)!! }

    /**
     * Generate a random string
     *
     * @param buf        buffer to write into
     * @param codePoints list of code points to write into
     * @param charBuf    char buf for temporary char wrangling (size 2)
     * @param rand       random source
     * @param length     max string length
     */
    private fun randString(buf: StringBuilder,
                           codePoints: MutableList<Int>,
                           charBuf: CharArray,
                           rand: Random,
                           length: Int) {
        val CODE_POINT_IN_SUPPLEMENTARY = 2
        val CODE_POINT_IN_BMP = 1

        buf.setLength(0)
        codePoints.clear()

        while (buf.length < length) {
            // pick something in the range of all 17 unicode planes
            val codePoint = rand.nextInt(17 * 65536)
            if (Character.isDefined(codePoint)) {
                val res = Character.toChars(codePoint, charBuf, 0)

                if (res == CODE_POINT_IN_BMP && (Character.isHighSurrogate(charBuf[0]) || Character.isLowSurrogate(
                                charBuf[0]))) {
                    // isDefined is true even if it's a standalone surrogate in the D800-DFFF range, but those are not legal
                    // single unicode code units (that is, a single char)
                    continue
                }

                buf.append(charBuf[0])
                // whether it's a pair or not, we want the only char (or high surrogate)
                codePoints.add(codePoint)
                if (res == CODE_POINT_IN_SUPPLEMENTARY) {
                    // it's a surrogate pair, so we care about the second char
                    buf.append(charBuf[1])
                }
            }
        }
    }
}