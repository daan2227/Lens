package com.lens.camera.frames

import android.graphics.Color

/**
 * A themed collage frame. [border] is a pair of gradient colors, or null for the
 * plain "Ninguno" frame (no border, no decorations, no banner).
 * Order matches R.array.frame_names / R.array.frame_banners 1:1.
 */
data class FrameTheme(
    val bg: Int,
    val border: Pair<Int, Int>?,
    val motifs: List<MotifId>
) {
    val hasBanner: Boolean get() = border != null
}

val FRAMES: List<FrameTheme> = listOf(
    FrameTheme(Color.parseColor("#0A0A0C"), null, emptyList()), // Ninguno
    FrameTheme( // Amor
        Color.parseColor("#2B0A14"),
        Color.parseColor("#FF4D6D") to Color.parseColor("#FF8FA3"),
        listOf(MotifId.CORAZON, MotifId.FLOR, MotifId.CORAZON, MotifId.CORAZON)
    ),
    FrameTheme( // Halloween
        Color.parseColor("#12081C"),
        Color.parseColor("#FF7B00") to Color.parseColor("#7B2CBF"),
        listOf(MotifId.CALABAZA, MotifId.FANTASMA, MotifId.MURCIELAGO, MotifId.ESTRELLA)
    ),
    FrameTheme( // Navidad
        Color.parseColor("#07140B"),
        Color.parseColor("#D62828") to Color.parseColor("#2A9D3A"),
        listOf(MotifId.ARBOL, MotifId.ESTRELLA, MotifId.COPO, MotifId.REGALO)
    ),
    FrameTheme( // 4 Julio
        Color.parseColor("#0A1030"),
        Color.parseColor("#C1121F") to Color.parseColor("#3A5BD9"),
        listOf(MotifId.BANDERA, MotifId.ESTRELLA, MotifId.FUEGO_ART, MotifId.FUEGO_ART)
    ),
    FrameTheme( // Independencia
        Color.parseColor("#101410"),
        Color.parseColor("#2A9D3A") to Color.parseColor("#D62828"),
        listOf(MotifId.CONFETI, MotifId.CONFETI, MotifId.ESTRELLA, MotifId.FUEGO_ART)
    ),
    FrameTheme( // Cumpleaños
        Color.parseColor("#1C1024"),
        Color.parseColor("#FFD54A") to Color.parseColor("#FF6EC7"),
        listOf(MotifId.PASTEL, MotifId.GLOBO, MotifId.REGALO, MotifId.CONFETI)
    ),
    FrameTheme( // Año Nuevo
        Color.parseColor("#141005"),
        Color.parseColor("#FFD700") to Color.parseColor("#F2F1EC"),
        listOf(MotifId.COPA, MotifId.FUEGO_ART, MotifId.ESTRELLA, MotifId.COPA)
    ),
    FrameTheme( // Boda
        Color.parseColor("#171512"),
        Color.parseColor("#E8D5B0") to Color.parseColor("#FFFFFF"),
        listOf(MotifId.ANILLO, MotifId.PALOMA, MotifId.FLOR, MotifId.CORAZON)
    ),
    FrameTheme( // Graduación
        Color.parseColor("#0C1220"),
        Color.parseColor("#FFD54A") to Color.parseColor("#3A5BD9"),
        listOf(MotifId.BIRRETE, MotifId.ESTRELLA, MotifId.ESTRELLA, MotifId.CONFETI)
    ),
    FrameTheme( // Bebé
        Color.parseColor("#101820"),
        Color.parseColor("#8ECAE6") to Color.parseColor("#FFB5C2"),
        listOf(MotifId.BIBERON, MotifId.GLOBO, MotifId.ESTRELLA, MotifId.ESTRELLA)
    ),
    FrameTheme( // Verano
        Color.parseColor("#061A1E"),
        Color.parseColor("#00B4D8") to Color.parseColor("#FFD54A"),
        listOf(MotifId.PALMERA, MotifId.SOL, MotifId.SANDIA, MotifId.SOL)
    ),
    FrameTheme( // Día Madres
        Color.parseColor("#1E0D18"),
        Color.parseColor("#FF8FA3") to Color.parseColor("#C9ADA7"),
        listOf(MotifId.FLOR, MotifId.FLOR, MotifId.CORAZON, MotifId.FLOR)
    ),
    FrameTheme( // San Valentín
        Color.parseColor("#25060F"),
        Color.parseColor("#E63946") to Color.parseColor("#FFCCD5"),
        listOf(MotifId.CORAZON, MotifId.FLOR, MotifId.REGALO, MotifId.CORAZON)
    ),
    FrameTheme( // Fiesta
        Color.parseColor("#120A1E"),
        Color.parseColor("#B5179E") to Color.parseColor("#4CC9F0"),
        listOf(MotifId.ESFERA, MotifId.NOTA, MotifId.ESTRELLA, MotifId.CONFETI)
    )
)
