package com.dongflix

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.roundToInt

interface OnLiquidNavSelectedListener {
    fun onTabSelected(index: Int)
    fun onSearchClicked()
}

object LiquidGlassNavBridge {
    private val selectedTabState = mutableIntStateOf(0)

    @JvmStatic
    fun setSelectedTab(index: Int) {
        selectedTabState.intValue = index.coerceIn(0, 2)
    }

    @JvmStatic
    fun attach(
        composeView: ComposeView,
        initialIndex: Int,
        listener: OnLiquidNavSelectedListener
    ) {
        selectedTabState.intValue = initialIndex.coerceIn(0, 2)

        composeView.setContent {
            val selectedIndex by selectedTabState
            val backdrop = rememberLayerBackdrop()
            val density = LocalDensity.current
            var barWidthPx by remember { mutableFloatStateOf(0f) }
            var dragOffsetPx by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            val tabCount = 3
            val tabWidthPx = if (barWidthPx > 0f) barWidthPx / tabCount else 0f

            val targetOffsetPx = selectedIndex * tabWidthPx
            val animatedOffsetPx by animateFloatAsState(
                targetValue = if (isDragging) dragOffsetPx else targetOffsetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "liquidPillOffset"
            )

            val pillScale by animateFloatAsState(
                targetValue = if (isDragging) 1.08f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "liquidPillScale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kapsul Utama Liquid Glass (Beranda, Pustaka, Akun)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(CircleShape)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(16.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0x55121622))
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0x77FFFFFF),
                                    Color(0x1AFFFFFF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .onSizeChanged { size ->
                            barWidthPx = size.width.toFloat()
                        }
                        .pointerInput(tabCount, barWidthPx) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffsetPx = selectedIndex * tabWidthPx
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (tabWidthPx > 0f) {
                                        val snapped = (dragOffsetPx / tabWidthPx)
                                            .roundToInt()
                                            .coerceIn(0, tabCount - 1)
                                        selectedTabState.intValue = snapped
                                        listener.onTabSelected(snapped)
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val maxOffset = (tabCount - 1) * tabWidthPx
                                    dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxOffset)
                                }
                            )
                        }
                ) {
                    // Pill Kaca yang Bisa Digeser-Geser
                    if (tabWidthPx > 0f) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = animatedOffsetPx.roundToInt(),
                                        y = 0
                                    )
                                }
                                .width(with(density) { tabWidthPx.toDp() })
                                .fillMaxHeight()
                                .padding(4.dp)
                                .graphicsLayer {
                                    scaleX = pillScale
                                    scaleY = pillScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x52FFFFFF),
                                            Color(0x22FFFFFF)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0x88FFFFFF),
                                    shape = CircleShape
                                )
                        )
                    }

                    val labels = listOf("Beranda", "Pustaka", "Akun")
                    Row(modifier = Modifier.fillMaxSize()) {
                        labels.forEachIndexed { idx, title ->
                            val isSelected = selectedIndex == idx
                            val contentColor = if (isSelected) Color.White else Color(0xFFCBD5E1)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedTabState.intValue = idx
                                        listener.onTabSelected(idx)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (idx) {
                                        0 -> if (isSelected) Icons.Rounded.Home else Icons.Outlined.Home
                                        1 -> if (isSelected) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder
                                        else -> if (isSelected) Icons.Rounded.Person else Icons.Outlined.PersonOutline
                                    },
                                    contentDescription = title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = title,
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Tombol Cari Bulat Liquid Glass di Kanan
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(16.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0x55121622))
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0x77FFFFFF),
                                    Color(0x1AFFFFFF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { listener.onSearchClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
