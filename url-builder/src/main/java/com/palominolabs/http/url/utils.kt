/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url

import java.nio.charset.CoderResult
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException


/**
 * Throws if the given [CoderResult] is considered an error.
 *
 * @throws IllegalStateException        if result is overflow
 * @throws MalformedInputException      if result represents malformed input
 * @throws UnmappableCharacterException if result represents an unmappable character
 */
@Throws(MalformedInputException::class, UnmappableCharacterException::class)
fun CoderResult.throwIfError(ignoreOverflow: Boolean = false) {
    when {
        !ignoreOverflow && isOverflow -> throw IllegalStateException("Byte buffer overflow; this should not happen.")
        isMalformed                   -> throw MalformedInputException(length())
        isUnmappable                  -> throw UnmappableCharacterException(length())
    }
}
