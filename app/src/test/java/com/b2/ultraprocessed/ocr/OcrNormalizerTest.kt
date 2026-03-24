package com.b2.ultraprocessed.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrNormalizerTest {

    @Test
    fun `removes INGREDIENTS prefix`() {
        val input = "INGREDIENTS: Water, Sugar, Salt"
        val result = OcrNormalizer.normalize(input)
        assertEquals("water, sugar, salt", result)
    }

    @Test
    fun `converts to lowercase`() {
        val input = "WATER SUGAR SALT"
        val result = OcrNormalizer.normalize(input)
        assertEquals("water sugar salt", result)
    }

    @Test
    fun `removes special characters`() {
        val input = "Water!! Sugar@@ Salt##"
        val result = OcrNormalizer.normalize(input)
        assertEquals("water sugar salt", result)
    }

    @Test
    fun `collapses multiple spaces`() {
        val input = "water   sugar    salt"
        val result = OcrNormalizer.normalize(input)
        assertEquals("water sugar salt", result)
    }
}
