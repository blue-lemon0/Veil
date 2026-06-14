package com.lemon.veil

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

data class ReorderDragInfo(
    val visualOffset: Float,
    val isDragged: Boolean,
    val onDragStart: (itemHeight: Float) -> Unit,
    val onDrag: (totalOffset: Float) -> Unit,
    val onDragEnd: () -> Unit
)

@Composable
fun <T> ReorderColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    itemContent: @Composable (item: T, index: Int, dragInfo: ReorderDragInfo) -> Unit
) {
    data class DragState(val fromIndex: Int, val totalOffset: Float, val itemHeight: Float)
    var dragState by remember { mutableStateOf<DragState?>(null) }

    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val visualOffset = dragState?.let { ds ->
                if (index == ds.fromIndex) {
                    ds.totalOffset
                } else {
                    val shift = (ds.totalOffset / ds.itemHeight).roundToInt()
                    val targetIndex = (ds.fromIndex + shift).coerceIn(0, items.size - 1)
                    when {
                        ds.fromIndex < targetIndex && index in (ds.fromIndex + 1)..targetIndex -> -ds.itemHeight
                        ds.fromIndex > targetIndex && index in targetIndex until ds.fromIndex -> ds.itemHeight
                        else -> 0f
                    }
                }
            } ?: 0f
            val isDragged = dragState?.fromIndex == index

            itemContent(
                item, index,
                ReorderDragInfo(
                    visualOffset = visualOffset,
                    isDragged = isDragged,
                    onDragStart = { itemHeight -> dragState = DragState(index, 0f, itemHeight) },
                    onDrag = { totalOffset ->
                        dragState?.let { if (it.fromIndex == index) dragState = it.copy(totalOffset = totalOffset) }
                    },
                    onDragEnd = {
                        val ds = dragState
                        if (ds != null && ds.fromIndex == index && ds.itemHeight > 0f) {
                            val shift = (ds.totalOffset / ds.itemHeight).roundToInt()
                            val targetIndex = (index + shift).coerceIn(0, items.size - 1)
                            if (targetIndex != index) onReorder(index, targetIndex)
                        }
                        dragState = null
                    }
                )
            )
        }
    }
}
