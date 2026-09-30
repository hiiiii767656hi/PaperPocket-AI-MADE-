package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.server.MinecraftColorFormatter

@Composable
fun MinecraftText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    defaultColor: Color = Color(0xFFDDDDDD)
) {
    val annotated = remember(text, defaultColor) {
        MinecraftColorFormatter.format(text, defaultColor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style
    )
}
