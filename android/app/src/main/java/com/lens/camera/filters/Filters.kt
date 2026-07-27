package com.lens.camera.filters

import com.lens.camera.filters.FilterOp.Brightness
import com.lens.camera.filters.FilterOp.Contrast
import com.lens.camera.filters.FilterOp.Grayscale
import com.lens.camera.filters.FilterOp.HueRotate
import com.lens.camera.filters.FilterOp.Saturate
import com.lens.camera.filters.FilterOp.Sepia

/**
 * The 38 built-in filters. Order matches R.array.filter_names 1:1 (index is the
 * only link between this list and its localized display name), so don't reorder
 * one without the other.
 */
val FILTERS: List<List<FilterOp>> = listOf(
    emptyList(), // Original
    listOf(Saturate(1.45f), Contrast(1.1f)), // Vívido
    listOf(Grayscale(1f), Contrast(1.25f), Brightness(1.05f)), // Noir
    listOf(Sepia(.55f), Contrast(.95f), Saturate(1.2f), HueRotate(-8f)), // Retro
    listOf(HueRotate(18f), Saturate(1.1f), Brightness(1.03f)), // Frío
    listOf(Sepia(.25f), Saturate(1.3f), HueRotate(-12f)), // Cálido
    listOf(Contrast(1.2f), Saturate(.85f), Brightness(.95f), Sepia(.12f)), // Cine
    listOf(Saturate(1.9f), Contrast(1.3f), HueRotate(8f)), // Neón
    listOf(Brightness(1.12f), Contrast(.85f), Saturate(.8f)), // Bruma
    listOf(Brightness(.85f), Contrast(1.35f), Saturate(1.1f)), // Sombra
    listOf(Grayscale(1f), Brightness(1.15f), Contrast(.9f)), // Plata
    listOf(Grayscale(1f), Contrast(1.6f), Brightness(.9f)), // Carbón
    listOf(Sepia(.9f), Contrast(1.05f)), // Sepia
    listOf(Sepia(.4f), Saturate(1.5f), HueRotate(-18f), Brightness(1.08f)), // Dorado
    listOf(HueRotate(-25f), Saturate(1.25f), Brightness(1.06f)), // Rosa
    listOf(HueRotate(35f), Saturate(1.15f), Brightness(1.05f), Contrast(.95f)), // Lavanda
    listOf(HueRotate(60f), Saturate(1.2f), Contrast(1.05f)), // Esmeralda
    listOf(HueRotate(150f), Saturate(1.3f), Brightness(.98f)), // Océano
    listOf(Sepia(.35f), Saturate(1.6f), HueRotate(-20f), Contrast(1.1f)), // Atardecer
    listOf(Brightness(1.15f), Sepia(.2f), Saturate(1.35f), HueRotate(-6f)), // Amanecer
    listOf(Brightness(1.2f), Saturate(.6f), HueRotate(15f), Contrast(.9f)), // Polar
    listOf(Sepia(.5f), Saturate(1.4f), Contrast(1.15f), Brightness(1.05f)), // Desierto
    listOf(Saturate(1.8f), HueRotate(-5f), Brightness(1.08f), Contrast(1.05f)), // Tropical
    listOf(Sepia(.45f), Saturate(.9f), Contrast(.9f), Brightness(1.05f)), // Vintage
    listOf(Contrast(.85f), Brightness(1.15f), Saturate(.75f), Sepia(.15f)), // Polaroid
    listOf(Contrast(1.4f), Saturate(1.5f), Brightness(.92f), HueRotate(-4f)), // Lomo
    listOf(Contrast(1.25f), Saturate(1.15f), Brightness(1.05f)), // Cromo
    listOf(Contrast(.8f), Brightness(1.1f), Saturate(.65f)), // Fade
    listOf(Contrast(1.5f), Saturate(.7f), Brightness(.9f)), // Drama
    listOf(Saturate(2.4f), Contrast(1.4f), HueRotate(25f)), // Punk
    listOf(HueRotate(180f), Saturate(1.5f), Contrast(1.15f)), // Infrarrojo
    listOf(HueRotate(90f), Saturate(2f), Contrast(1.2f)), // Ácido
    listOf(HueRotate(-60f), Saturate(1.35f), Brightness(.98f)), // Púrpura
    listOf(Sepia(.3f), Saturate(1.45f), HueRotate(-14f), Brightness(1.1f), Contrast(1.05f)), // Miel
    listOf(Brightness(.75f), Contrast(1.3f), Saturate(.85f), HueRotate(12f)), // Nocturno
    listOf(Brightness(1.25f), Contrast(.7f), Saturate(.55f)), // Niebla
    listOf(Brightness(1.1f), Saturate(.8f), HueRotate(28f), Contrast(1.05f)), // Escarcha
    listOf(Sepia(.5f), Saturate(2f), HueRotate(-25f), Contrast(1.2f)) // Fuego
)

/** Extra PRO-mode adjustments layered on top of the selected filter, in this order. */
data class ProAdjust(val brightness: Float = 1f, val contrast: Float = 1f, val saturate: Float = 1f, val hueRotate: Float = 0f) {
    fun toOps(): List<FilterOp> = listOf(
        FilterOp.Brightness(brightness),
        FilterOp.Contrast(contrast),
        FilterOp.Saturate(saturate),
        FilterOp.HueRotate(hueRotate)
    )
    val isIdentity: Boolean get() = brightness == 1f && contrast == 1f && saturate == 1f && hueRotate == 0f
}
