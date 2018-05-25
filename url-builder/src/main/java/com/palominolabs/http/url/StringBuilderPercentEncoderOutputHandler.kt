package com.palominolabs.http.url

/**
 * A PercentEncoderHandler implementation that accumulates chars in a buffer.
 */
@NotThreadSafe
class StringBuilderPercentEncoderOutputHandler : PercentEncoderOutputHandler {

    private val stringBuilder: StringBuilder

    /**
     * @return A string containing the chars accumulated since the last call to reset()
     */
    val contents: String
        get() = stringBuilder.toString()

    /**
     * Create a new handler with a default size StringBuilder.
     */
    init {
        stringBuilder = StringBuilder()
    }

    /**
     * Clear the buffer.
     */
    fun reset() {
        stringBuilder.setLength(0)
    }

    fun ensureCapacity(length: Int) {
        stringBuilder.ensureCapacity(length)
    }

    override fun onOutputChar(c: Char) {
        stringBuilder.append(c)
    }
}
