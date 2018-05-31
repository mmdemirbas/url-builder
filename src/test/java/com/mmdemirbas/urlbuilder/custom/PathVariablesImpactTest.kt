package com.mmdemirbas.urlbuilder.custom

import com.mmdemirbas.urlbuilder.UrlPart
import com.mmdemirbas.urlbuilder.encode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * @author Muhammed Demirbaş
 * @since 2018-05-25 14:17
 */
class PathVariablesImpactTest {
    @Disabled("not an actual test")
    @Test
    fun `report encoding impact`() {
        val files = (0..46).map { "/$it.csv" }
        val texts = files.map {
            javaClass.getResource(it).readText().split('\n').map { line ->
                val s = when {
                    line.startsWith('"') && line.endsWith('"') -> line.substring(1, line.length - 1)
                    else                                       -> line
                }
                s.split(',')

            }.flatten().distinct()
        }.flatten().filter { it.isNotEmpty() }.distinct()
        val values = texts.mapNotNull { line ->
            val keyValue = line.split('=', limit = 2)
            if (keyValue.size != 2) println("Invalid input: $line")
            keyValue.getOrNull(1)
        }
        val allChars = values.toSet().map { it.toCharArray().toSet() }.flatten().map { it.toString() }.toSet()
        report(values)

        println("all chars: " + allChars.sorted().joinToString(""))
        report(allChars)
    }

    private fun report(values: Collection<String>) {
        val distinct = values.distinct()
        val valueToEncoded = distinct.associate { it to UrlPart.Path.encode(it) }
        val notChanged = valueToEncoded.filter { (raw, encoded) -> raw == encoded }
        val changed = valueToEncoded.filter { (raw, encoded) -> raw != encoded }

        println()
        println("Among ${distinct.size} samples, ${notChanged.size} are not changed and ${changed.size} are changed:")
        changed.forEach { raw, encoded ->
            println("  raw    : $raw")
            println("  encoded: $encoded")
            println()
        }
    }
}