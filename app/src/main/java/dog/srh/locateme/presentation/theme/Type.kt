package dog.srh.locateme.presentation.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.Typography
import dog.srh.locateme.R

val fontFamily = FontFamily(
    Font(R.font.funnelsans_regular, FontWeight.Normal),
    Font(R.font.funnelsans_medium, FontWeight.Medium)
)

val AppTypography = Typography(
    defaultFontFamily = fontFamily
)
