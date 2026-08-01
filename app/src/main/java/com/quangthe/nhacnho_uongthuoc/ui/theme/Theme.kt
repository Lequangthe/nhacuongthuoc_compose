package com.quangthe.nhacnho_uongthuoc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.quangthe.nhacnho_uongthuoc.Simpill

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF00639B),
    secondary = Color(0xFF4F616E),
    tertiary = Color(0xFF65587A),
    background = Color(0xFFFCFCFF),
    surface = Color(0xFFFCFCFF),
    surfaceVariant = Color(0xFFDEE3EA),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FCDFF),
    secondary = Color(0xFFB5C8D6),
    tertiary = Color(0xFFD5BDF1),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF42474B),
    onPrimary = Color(0xFF00344F),
    onBackground = Color(0xFFE1E2E5),
    onSurface = Color(0xFFE1E2E5)
)

private val GreyLightColorScheme = lightColorScheme(
    primary = Color(0xFF006783),
    secondary = Color(0xFF4F616E),
    tertiary = Color(0xFF65587A),
    background = Color(0xFFFCFCFF),
    surface = Color(0xFFFCFCFF),
    surfaceVariant = Color(0xFFDEE3EA),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

private val GreyDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FCDFF),
    secondary = Color(0xFFB5C8D6),
    tertiary = Color(0xFFD5BDF1),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF42474B),
    onPrimary = Color(0xFF00344F),
    onBackground = Color(0xFFE1E2E5),
    onSurface = Color(0xFFE1E2E5)
)

private val BlackColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF9AA0A6),
    tertiary = Color(0xFF03DAC6),
    background = Color(0xFF141414),
    surface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun SimpillTheme(
    themeType: Int = Simpill.PURPLE_THEME,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        Simpill.BLUE_THEME -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        Simpill.GREY_THEME -> if (darkTheme) GreyDarkColorScheme else GreyLightColorScheme
        Simpill.BLACK_THEME -> BlackColorScheme
        Simpill.PURPLE_THEME -> if (darkTheme) DarkColorScheme else LightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
