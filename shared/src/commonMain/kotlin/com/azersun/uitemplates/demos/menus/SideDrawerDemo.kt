package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DrawerItems = listOf(
    Glyph.Home to "Home",
    Glyph.User to "Profile",
    Glyph.Bookmark to "Saved",
    Glyph.Bell to "Notifications",
    Glyph.Gear to "Settings",
)

@Composable
fun SideDrawerDemo() {
    var open by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        if (open) 1f else 0f,
        spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "drawer",
    )

    MenuStage(2, "Side Drawer", "Drawer springs in from the left as the page dims. Rows stagger in.") {
        BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp).clip(RoundedCornerShape(20.dp))) {
            val drawerWidth = maxWidth * 0.78f
            MockScreen(
                topBar = {
                    MockTopBar(
                        leading = {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).clickable { open = true },
                                contentAlignment = Alignment.Center,
                            ) { MenuBarsIcon(MenuColors.Text) }
                        },
                        trailing = { Box(Modifier.size(28.dp).clip(CircleShape).background(MenuColors.TileLight)) },
                    )
                },
            )
            if (progress > 0.001f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false },
                )
            }
            Column(
                Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        // Clamp the spring's overshoot so no gap opens at the left edge.
                        translationX = (-(1f - progress) * drawerWidth.toPx()).coerceAtMost(0f)
                    }
                    .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                    .background(Color(0xFF1C1A3A))
                    .padding(18.dp),
            ) {
                StaggerIn(index = 0, visible = open) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MenuColors.Violet, MenuColors.Cyan))),
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Alex Rivera", color = MenuColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("alex@studio.io", color = MenuColors.Muted, fontSize = 11.sp)
                        }
                    }
                }
                DrawerItems.forEachIndexed { i, (glyph, label) ->
                    StaggerIn(index = i + 1, visible = open) {
                        val selected = i == 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MenuColors.Violet.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { open = false }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlyphIcon(glyph, if (selected) MenuColors.Violet else MenuColors.Muted)
                            Text(
                                label,
                                color = if (selected) MenuColors.Text else MenuColors.Muted,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 14.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                StaggerIn(index = DrawerItems.size + 1, visible = open) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                        GlyphIcon(Glyph.LogOut, MenuColors.Red)
                        Text("Log out", color = MenuColors.Red, fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
