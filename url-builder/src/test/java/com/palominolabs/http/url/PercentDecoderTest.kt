package com.palominolabs.http.url

import com.palominolabs.http.url.SafeChars
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.text.Charsets.UTF_8

class PercentDecoderTest {
    @Before
    fun setUp() {
        decoder = PercentDecoder(UTF_8.newDecoder())
    }

    @Test
    fun testDecodesWithoutPercents() {
        assert("asdf" == decoder.decode("asdf"))
    }

    @Test
    fun testDecodeSingleByte() {
        assert("#" == decoder.decode("%23"))
    }

    @Test
    fun testIncompletePercentPairNoNumbers() {
        try {
            decoder.decode("%")
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            assert("Could not percent decode <%>: incomplete %-pair at position 0" == e.message)
        }
    }

    @Test
    fun testIncompletePercentPairOneNumber() {
        try {
            decoder.decode("%2")
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            assert("Could not percent decode <%2>: incomplete %-pair at position 0" == e.message)
        }
    }

    @Test
    fun testInvalidHex() {
        try {
            decoder.decode("%xz")
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            assert("Invalid %-tuple <%xz>" == e.message)
        }
    }

    @Test
    fun testRandomStrings() {
        val encoder = SafeChars.UNSTRUCTURED_QUERY.newEncoder()
        val rand = Random()

        val seed = rand.nextLong()
        rand.setSeed(seed)

        val charBuf = CharArray(2)
        val codePoints = ArrayList<Int>()
        val buf = StringBuilder()

        (1..10000).forEach {
            buf.setLength(0)
            codePoints.clear()

            randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000))

            val origBytes = buf.toString().toByteArray(UTF_8)
            var decodedBytes: ByteArray? = null
            val codePointsHex = codePoints.map {
                Integer.toHexString(it)
            }

            try {
                decodedBytes = decoder.decode(encoder.encode(buf.toString())).toByteArray(UTF_8)
            } catch (e: IllegalArgumentException) {
                val charHex = (0 until buf.toString().length).map { Integer.toHexString(buf.toString()[it].toInt()) }
                Assert.fail("seed: $seed code points: $codePointsHex chars $charHex ${e.message}")
            }

            Assert.assertEquals("Seed: $seed Code points: $codePointsHex", origBytes.toHex(), decodedBytes!!.toHex())
        }
    }

    private fun ByteArray.toHex() = map { Integer.toHexString(it.toInt() and 0xFF)!! }

    private lateinit var decoder: PercentDecoder

    companion object {
        private const val CODE_POINT_IN_SUPPLEMENTARY = 2
        private const val CODE_POINT_IN_BMP = 1

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
}