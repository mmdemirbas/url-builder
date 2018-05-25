/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.util.BitSet
import javax.annotation.concurrent.ThreadSafe

import com.google.common.base.Charsets.UTF_8
import java.nio.charset.CodingErrorAction.REPLACE

/**
 * See RFC 3986, RFC 1738 and http://www.lunatech-research.com/archives/2009/02/03/what-every-web-developer-must-know-about-url-encoding.
 */
@ThreadSafe
object UrlPercentEncoders {

    /**
     * an encoder for RFC 3986 reg-names
     */

    private val REG_NAME_BIT_SET = BitSet()

    private val PATH_BIT_SET = BitSet()
    private val MATRIX_BIT_SET = BitSet()
    private val UNSTRUCTURED_QUERY_BIT_SET = BitSet()
    private val QUERY_PARAM_BIT_SET = BitSet()
    private val FRAGMENT_BIT_SET = BitSet()

    val regNameEncoder: PercentEncoder
        get() = PercentEncoder(REG_NAME_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    val pathEncoder: PercentEncoder
        get() = PercentEncoder(PATH_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    val matrixEncoder: PercentEncoder
        get() = PercentEncoder(MATRIX_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    val unstructuredQueryEncoder: PercentEncoder
        get() = PercentEncoder(UNSTRUCTURED_QUERY_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    val queryParamEncoder: PercentEncoder
        get() = PercentEncoder(QUERY_PARAM_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    val fragmentEncoder: PercentEncoder
        get() = PercentEncoder(FRAGMENT_BIT_SET,
                               UTF_8.newEncoder().onMalformedInput(REPLACE).onUnmappableCharacter(REPLACE))

    init {
        // RFC 3986 'reg-name'. This is not very aggressive... it's quite possible to have DNS-illegal names out of this.
        // Regardless, it will at least be URI-compliant even if it's not HTTP URL-compliant.
        addUnreserved(REG_NAME_BIT_SET)
        addSubdelims(REG_NAME_BIT_SET)

        // Represents RFC 3986 'pchar'. Remove delimiter that starts matrix section.
        addPChar(PATH_BIT_SET)
        PATH_BIT_SET.clear(';'.toInt())

        // Remove delims for HTTP matrix params as per RFC 1738 S3.3. The other reserved chars ('/' and '?') are already excluded.
        addPChar(MATRIX_BIT_SET)
        MATRIX_BIT_SET.clear(';'.toInt())
        MATRIX_BIT_SET.clear('='.toInt())

        /*
         * At this point it represents RFC 3986 'query'. http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1 also
         * specifies that "+" can mean space in a query, so we will make sure to say that '+' is not safe to leave as-is
         */
        addQuery(UNSTRUCTURED_QUERY_BIT_SET)
        UNSTRUCTURED_QUERY_BIT_SET.clear('+'.toInt())

        /*
         * Create more stringent requirements for HTML4 queries: remove delimiters for HTML query params so that key=value
         * pairs can be used.
         */
        QUERY_PARAM_BIT_SET.or(UNSTRUCTURED_QUERY_BIT_SET)
        QUERY_PARAM_BIT_SET.clear('='.toInt())
        QUERY_PARAM_BIT_SET.clear('&'.toInt())

        addFragment(FRAGMENT_BIT_SET)
    }

    /**
     * Add code points for 'fragment' chars
     *
     * @param fragmentBitSet bit set
     */
    private fun addFragment(fragmentBitSet: BitSet) {
        addPChar(fragmentBitSet)
        fragmentBitSet.set('/'.toInt())
        fragmentBitSet.set('?'.toInt())
    }

    /**
     * Add code points for 'query' chars
     *
     * @param queryBitSet bit set
     */
    private fun addQuery(queryBitSet: BitSet) {
        addPChar(queryBitSet)
        queryBitSet.set('/'.toInt())
        queryBitSet.set('?'.toInt())
    }

    /**
     * Add code points for 'pchar' chars.
     *
     * @param bs bitset
     */
    private fun addPChar(bs: BitSet) {
        addUnreserved(bs)
        addSubdelims(bs)
        bs.set(':'.toInt())
        bs.set('@'.toInt())
    }

    /**
     * Add codepoints for 'unreserved' chars
     *
     * @param bs bitset to add codepoints to
     */
    private fun addUnreserved(bs: BitSet) {

        run {
            var i: Int = 'a'.toInt()
            while (i <= 'z'.toInt()) {
                bs.set(i)
                i++
            }
        }
        run {
            var i: Int = 'A'.toInt()
            while (i <= 'Z'.toInt()) {
                bs.set(i)
                i++
            }
        }
        var i: Int = '0'.toInt()
        while (i <= '9'.toInt()) {
            bs.set(i)
            i++
        }
        bs.set('-'.toInt())
        bs.set('.'.toInt())
        bs.set('_'.toInt())
        bs.set('~'.toInt())
    }

    /**
     * Add codepoints for 'sub-delims' chars
     *
     * @param bs bitset to add codepoints to
     */
    private fun addSubdelims(bs: BitSet) {
        bs.set('!'.toInt())
        bs.set('$'.toInt())
        bs.set('&'.toInt())
        bs.set('\''.toInt())
        bs.set('('.toInt())
        bs.set(')'.toInt())
        bs.set('*'.toInt())
        bs.set('+'.toInt())
        bs.set(','.toInt())
        bs.set(';'.toInt())
        bs.set('='.toInt())
    }
}
