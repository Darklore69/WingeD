package com.example.pdfreader

import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionTest {
    @Test
    fun testPureYellowBlend() {
        fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        // Light mode: pure neon yellow (255, 255, 0) with alpha = 180
        val lightColor = argb(180, 255, 255, 0)
        val alpha = (lightColor ushr 24) and 0xFF
        val red = (lightColor ushr 16) and 0xFF
        val green = (lightColor ushr 8) and 0xFF
        val blue = lightColor and 0xFF

        println("Light mode -> Alpha: $alpha, R: $red, G: $green, B: $blue")

        // Dark mode: (0, 0, 180) inverted by matrix s=-0.88f, t=235f
        val s = -0.88f
        val t = 235f
        val darkInR = 0f
        val darkInG = 0f
        val darkInB = 180f

        val outR = (darkInR * s + t).coerceIn(0f, 255f)
        val outG = (darkInG * s + t).coerceIn(0f, 255f)
        val outB = (darkInB * s + t).coerceIn(0f, 255f)

        println("Dark mode inverted color -> R: $outR, G: $outG, B: $outB")
        assertTrue(outR > 220 && outG > 220 && outB < 100)
    }
}