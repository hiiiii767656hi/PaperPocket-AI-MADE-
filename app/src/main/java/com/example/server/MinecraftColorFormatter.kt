package com.example.server

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object MinecraftColorFormatter {

    private val COLOR_MAP = mapOf(
        '0' to Color(0xFF000000), // Black
        '1' to Color(0xFF0000AA), // Dark Blue
        '2' to Color(0xFF00AA00), // Dark Green
        '3' to Color(0xFF00AAAA), // Dark Aqua
        '4' to Color(0xFFAA0000), // Dark Red
        '5' to Color(0xFFAA00AA), // Dark Purple
        '6' to Color(0xFFFFAA00), // Gold
        '7' to Color(0xFFAAAAAA), // Gray
        '8' to Color(0xFF555555), // Dark Gray
        '9' to Color(0xFF5555FF), // Blue
        'a' to Color(0xFF55FF55), // Green
        'b' to Color(0xFF55FFFF), // Aqua
        'c' to Color(0xFFFF5555), // Red
        'd' to Color(0xFFFF55FF), // Light Purple
        'e' to Color(0xFFFFFF55), // Yellow
        'f' to Color(0xFFFFFFFF)  // White
    )

    fun format(input: String, defaultColor: Color = Color(0xFFCCCCCC)): AnnotatedString {
        return buildAnnotatedString {
            var currentColor = defaultColor
            var isBold = false
            var isItalic = false

            var i = 0
            while (i < input.length) {
                val c = input[i]
                if ((c == '§' || c == '&') && i + 1 < input.length) {
                    val code = input[i + 1].lowercaseChar()
                    when {
                        COLOR_MAP.containsKey(code) -> {
                            currentColor = COLOR_MAP[code] ?: defaultColor
                            isBold = false
                            isItalic = false
                        }
                        code == 'l' -> isBold = true
                        code == 'o' -> isItalic = true
                        code == 'r' -> {
                            currentColor = defaultColor
                            isBold = false
                            isItalic = false
                        }
                    }
                    i += 2
                } else {
                    pushStyle(
                        SpanStyle(
                            color = currentColor,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                        )
                    )
                    append(c)
                    pop()
                    i++
                }
            }
        }
    }

    fun stripColor(input: String): String {
        return input.replace(Regex("[§&][0-9a-fk-orA-FK-OR]"), "")
    }
}
