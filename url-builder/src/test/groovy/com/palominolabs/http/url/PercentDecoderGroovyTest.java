package com.palominolabs.http.url;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Charsets.UTF_8;


public class PercentDecoderGroovyTest {
    @Before
    public void setUp() {
        decoder = new PercentDecoder(UTF_8.newDecoder());
    }

    @Test
    public void testDecodesWithoutPercents() {
        assert "asdf".equals(decoder.decode("asdf"));
    }

    @Test
    public void testDecodeSingleByte() {
        assert "#".equals(decoder.decode("%23"));
    }

    @Test
    public void testIncompletePercentPairNoNumbers() {
        try {
            decoder.decode("%");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assert "Could not percent decode <%>: incomplete %-pair at position 0".equals(e.getMessage());
        }

    }

    @Test
    public void testIncompletePercentPairOneNumber() {
        try {
            decoder.decode("%2");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assert "Could not percent decode <%2>: incomplete %-pair at position 0".equals(e.getMessage());
        }

    }

    @Test
    public void testInvalidHex() {
        try {
            decoder.decode("%xz");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assert "Invalid %-tuple <%xz>".equals(e.getMessage());
        }

    }

    @Test
    public void testRandomStrings() {
        final PercentEncoder encoder = UrlPercentEncoders.INSTANCE.getUnstructuredQueryEncoder();
        final Random         rand    = new Random();

        final long seed = rand.nextLong();
        rand.setSeed(seed);

        final char[]        charBuf    = new char[2];
        final List<Integer> codePoints = new ArrayList<Integer>();
        final StringBuilder buf        = new StringBuilder();

        DefaultGroovyMethods.times(10000, new Closure<Void>(this, this) {
            public void doCall(Integer it) {
                buf.setLength(0);
                ((ArrayList<Integer>) codePoints).clear();

                randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000));

                byte[] origBytes    = buf.toString().getBytes(UTF_8);
                byte[] decodedBytes = null;
                List<String> codePointsHex = DefaultGroovyMethods.collect(codePoints,
                                                                          new Closure<String>(PercentDecoderGroovyTest.this,
                                                                                              PercentDecoderGroovyTest.this) {
                                                                              public String doCall(int i) {
                                                                                  return Integer.toHexString(i);
                                                                              }

                                                                          });

                try {
                    decodedBytes = getDecoder().decode(encoder.encode(buf.toString())).getBytes(UTF_8);
                } catch (IllegalArgumentException e) {
                    List<String> charHex = new ArrayList<String>();
                    for (int i = 0; i < buf.toString().length(); i++) {
                        ((ArrayList<String>) charHex).add(Integer.toHexString((int) buf.toString().charAt(i)));
                    }

                    Assert.fail("seed: " + String.valueOf(seed) + " code points: " + String.valueOf(codePointsHex)
                                + " chars " + String.valueOf(charHex) + " " + e.getMessage());
                }


                Assert.assertEquals("Seed: " + String.valueOf(seed) + " Code points: " + String.valueOf(codePointsHex),
                                    toHex(origBytes),
                                    toHex(decodedBytes));
            }

            public void doCall() {
                doCall(null);
            }

        });
    }

    /**
     * Generate a random string
     *
     * @param buf        buffer to write into
     * @param codePoints list of code points to write into
     * @param charBuf    char buf for temporary char wrangling (size 2)
     * @param rand       random source
     * @param length     max string length
     */
    private static void randString(StringBuilder buf,
                                   List<Integer> codePoints,
                                   char[] charBuf,
                                   Random rand,
                                   int length) {
        while (buf.length() < length) {
            // pick something in the range of all 17 unicode planes
            int codePoint = rand.nextInt(17 * 65536);
            if (Character.isDefined(codePoint)) {
                int res = Character.toChars(codePoint, charBuf, 0);

                if (res == CODE_POINT_IN_BMP && (Character.isHighSurrogate(charBuf[0]) || Character.isLowSurrogate(
                        charBuf[0]))) {
                    // isDefined is true even if it's a standalone surrogate in the D800-DFFF range, but those are not legal
                    // single unicode code units (that is, a single char)
                    continue;
                }


                buf.append(charBuf[0]);
                // whether it's a pair or not, we want the only char (or high surrogate)
                codePoints.add(codePoint);
                if (res == CODE_POINT_IN_SUPPLEMENTARY) {
                    // it's a surrogate pair, so we care about the second char
                    buf.append(charBuf[1]);
                }

            }

        }

    }

    /**
     * @param bytes
     *
     * @return list of hex strings
     */
    public static List<String> toHex(byte[] bytes) {
        List list = new ArrayList();

        for (byte b : bytes) {
            list.add(Integer.toHexString(b & 0xFF));
        }


        return ((List<String>) (list));
    }

    public PercentDecoder getDecoder() {
        return decoder;
    }

    private static final int            CODE_POINT_IN_SUPPLEMENTARY = 2;
    private static final int            CODE_POINT_IN_BMP           = 1;
    private              PercentDecoder decoder;
}
