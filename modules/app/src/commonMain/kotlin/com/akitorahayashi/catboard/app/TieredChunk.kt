package com.akitorahayashi.catboard.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

private data class Cell(val x: Int, val y: Int, val span: Int)

private val cells = buildList {
    repeat(3) { add(Cell(it, 0, 1)) }
    add(Cell(0, 1, 2))
    repeat(2) { add(Cell(2, 1 + it, 1)) }
    repeat(3) { add(Cell(it, 3, 1)) }
    add(Cell(0, 4, 3))
}

@Composable
internal fun TieredChunk(images: List<GalleryImage>, firstIndex: Int, imageLoader: ImageLoader, imageModel: (String) -> Any) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            images.forEachIndexed { index, image ->
                AsyncImage(
                    model = imageModel(image.imageURL),
                    imageLoader = imageLoader,
                    contentDescription = "猫の画像 ${firstIndex + index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(2.dp).testTag("galleryImage_${firstIndex + index}"),
                )
            }
        },
    ) { measurables, constraints ->
        val unit = constraints.maxWidth / 3f
        val visible = cells.take(images.size)
        val height = ((visible.maxOfOrNull { it.y + it.span } ?: 0) * unit).roundToInt()
        val placeables = measurables.mapIndexed { index, measurable ->
            val cell = visible[index]
            val width = ((cell.x + cell.span) * unit).roundToInt() - (cell.x * unit).roundToInt()
            val cellHeight = ((cell.y + cell.span) * unit).roundToInt() - (cell.y * unit).roundToInt()
            measurable.measure(Constraints.fixed(width, cellHeight))
        }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val cell = visible[index]
                placeable.place((cell.x * unit).roundToInt(), height - ((cell.y + cell.span) * unit).roundToInt())
            }
        }
    }
}
