package com.example.memly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private class BottomBarCutoutShape(
    private val cutoutRadiusPx: Float,
    private val fabMarginPx: Float,
    private val cornerRadiusPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val totalRadius = cutoutRadiusPx + fabMarginPx
        val curveDepth = totalRadius * 1.0f
        val cr = cornerRadiusPx

        // Start at top-left corner (after rounding)
        path.moveTo(0f, cr)
        // Top-left rounded corner
        path.cubicTo(0f, 0f, 0f, 0f, cr, 0f)

        path.lineTo(centerX - totalRadius - 16f, 0f)

        // Wider, deeper smooth curve into the cutout
        path.cubicTo(
            centerX - totalRadius, 0f,
            centerX - totalRadius * 0.55f, curveDepth,
            centerX, curveDepth
        )

        // Wider, deeper smooth curve out of the cutout
        path.cubicTo(
            centerX + totalRadius * 0.55f, curveDepth,
            centerX + totalRadius, 0f,
            centerX + totalRadius + 16f, 0f
        )

        // Top-right rounded corner
        path.lineTo(size.width - cr, 0f)
        path.cubicTo(size.width, 0f, size.width, 0f, size.width, cr)

        // Bottom-right rounded corner
        path.lineTo(size.width, size.height - cr)
        path.cubicTo(size.width, size.height, size.width, size.height, size.width - cr, size.height)

        // Bottom-left rounded corner
        path.lineTo(cr, size.height)
        path.cubicTo(0f, size.height, 0f, size.height, 0f, size.height - cr)

        path.close()

        return Outline.Generic(path)
    }
}

@Composable
fun MemlyBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    onAddClick: () -> Unit,
    showFab: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val barColor = colorScheme.surfaceContainerHighest
    val selectedBg = colorScheme.primary
    val selectedIconColor = colorScheme.onPrimary
    val unselectedIconColor = colorScheme.onSurfaceVariant
    val addButtonColor = colorScheme.tertiary

    val fabRadius = 28.dp
    val fabMargin = 12.dp
    val cornerRadius = 64.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),

        contentAlignment = Alignment.BottomCenter
    ) {
        // The nav bar — always has the cutout shape
        val cutoutShape = with(LocalDensity.current) {
            remember(fabRadius, fabMargin, cornerRadius) {
                BottomBarCutoutShape(
                    cutoutRadiusPx = fabRadius.toPx(),
                    fabMarginPx = fabMargin.toPx(),
                    cornerRadiusPx = cornerRadius.toPx()
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(cutoutShape)
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val midIndex = items.size / 2

            items.forEachIndexed { index, item ->
                if (index == midIndex) {
                    // Spacer for the cutout area — always present
                    Spacer(modifier = Modifier.width(56.dp))
                }

                val isSelected = currentRoute == item.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(selectedBg)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemClick(item) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) selectedIconColor else unselectedIconColor
                    )
                }
            }
        }

        // FAB — visible on Timeline and CollectionList
        AnimatedVisibility(
            visible = showFab,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.offset(y = (-22).dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = addButtonColor,
                contentColor = colorScheme.onTertiary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
