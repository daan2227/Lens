package com.lens.camera.collage

/** A collage cell as fractional (x, y, width, height) within the collage canvas. */
data class Cell(val x: Float, val y: Float, val w: Float, val h: Float)

data class Layout(val cells: List<Cell>)

/** Order matches R.array.layout_names 1:1. */
val LAYOUTS: List<Layout> = listOf(
    Layout(listOf(Cell(0f, 0f, 1f, .5f), Cell(0f, .5f, 1f, .5f))), // 2V
    Layout(listOf(Cell(0f, 0f, .5f, 1f), Cell(.5f, 0f, .5f, 1f))), // 2H
    Layout(
        listOf(
            Cell(0f, 0f, 1f, 1f / 3f),
            Cell(0f, 1f / 3f, 1f, 1f / 3f),
            Cell(0f, 2f / 3f, 1f, 1f / 3f)
        )
    ), // 3T
    Layout(
        listOf(
            Cell(0f, 0f, .5f, .5f), Cell(.5f, 0f, .5f, .5f),
            Cell(0f, .5f, .5f, .5f), Cell(.5f, .5f, .5f, .5f)
        )
    ), // 4G
    Layout(listOf(Cell(0f, 0f, 1f, .6f), Cell(0f, .6f, .5f, .4f), Cell(.5f, .6f, .5f, .4f))), // 1+2
    Layout(listOf(Cell(0f, 0f, .5f, .4f), Cell(.5f, 0f, .5f, .4f), Cell(0f, .4f, 1f, .6f))), // 2+1
    Layout(listOf(Cell(0f, 0f, .6f, 1f), Cell(.6f, 0f, .4f, .5f), Cell(.6f, .5f, .4f, .5f))), // L3
    Layout(
        listOf(
            Cell(0f, 0f, .5f, 1f / 3f), Cell(.5f, 0f, .5f, 1f / 3f),
            Cell(0f, 1f / 3f, .5f, 1f / 3f), Cell(.5f, 1f / 3f, .5f, 1f / 3f),
            Cell(0f, 2f / 3f, .5f, 1f / 3f), Cell(.5f, 2f / 3f, .5f, 1f / 3f)
        )
    ) // 6G
)
