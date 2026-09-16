package com.ror.nms2go.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFFFFF)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0x121212)
annotation class ThemedPreview

@Composable
fun ThemedPreview(content: @Composable () -> Unit) {
    Nms2GoTheme {
        content()
    }
}
