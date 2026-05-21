package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameState
import com.example.ui.theme.*
import com.example.viewmodel.Achievement
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.UpgradeStation
import kotlinx.coroutines.delay

@Composable
fun AppContent(viewModel: GameViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val buyMode by viewModel.buyMode.collectAsState()
    val offlineSummary by viewModel.offlineSummary.collectAsState()
    val milestoneCelebration by viewModel.milestoneCelebration.collectAsState()

    var currentTab by remember { mutableStateOf(0) } // 0 = Izgara, 1 = İşletmeler, 2 = Başarılar
    var isSidePanelOpen by remember { mutableStateOf(false) }
    var showPrestigeConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Primary Scaffold holding main views and typical screen layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = DeepBlack,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalDark)
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kebap Ustası 🧂",
                            fontWeight = FontWeight.Bold,
                            color = SaffronOrange,
                            fontSize = 20.sp
                        )

                        // High Visibility Badge Crown trigger for Prestige Side Panel
                        IconButton(
                            onClick = { isSidePanelOpen = !isSidePanelOpen },
                            modifier = Modifier.testTag("menu_badge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Bilgiler ve Prestij Menüsü",
                                tint = SoftGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = CharcoalDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Text("🔥", fontSize = 22.sp) },
                            label = { Text("Izgara", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SizzlingRed,
                                selectedTextColor = SizzlingRed,
                                indicatorColor = SlateGrey,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Text("🏗️", fontSize = 22.sp) },
                            label = { Text("İşletmeler", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaffronOrange,
                                selectedTextColor = SaffronOrange,
                                indicatorColor = SlateGrey,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Text("🏆", fontSize = 22.sp) },
                            label = { Text("Başarılar", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonYellow,
                                selectedTextColor = NeonYellow,
                                indicatorColor = SlateGrey,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(DeepBlack, CharcoalDark)
                            )
                        )
                ) {
                    when (currentTab) {
                        0 -> IzgaraScreen(viewModel, gameState)
                        1 -> IsletmelerScreen(viewModel, gameState, buyMode)
                        2 -> BasarilarScreen(viewModel, gameState)
                    }
                }
            }

            // Custom Floating Slide-Over Side Panel
            AnimatedVisibility(
                visible = isSidePanelOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(310.dp)
                    .align(Alignment.CenterEnd)
                    .zOrder(10f)
            ) {
                SideMenuPanel(
                    viewModel = viewModel,
                    gameState = gameState,
                    onClose = { isSidePanelOpen = false },
                    onPrestigeClick = { showPrestigeConfirm = true }
                )
            }

            // Dark semi-transparent shield cover when side panel is open
            if (isSidePanelOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { isSidePanelOpen = false }
                        .zOrder(5f)
                )
            }

            // Offline earnings summary popup dialog
            offlineSummary?.let { summary ->
                OfflineDialog(
                    summary = summary,
                    onDismiss = { viewModel.dismissOfflineEarnings() }
                )
            }

            // Milestone full screen celebratory pop-up dialog
            milestoneCelebration?.let { celebrationText ->
                MilestoneCelebrationDialog(
                    text = celebrationText,
                    onDismiss = { viewModel.dismissMilestone() }
                )
            }

            // Prestige warning confirmation dialog
            if (showPrestigeConfirm) {
                PrestigeConfirmDialog(
                    viewModel = viewModel,
                    gameState = gameState,
                    onDismiss = { showPrestigeConfirm = false },
                    onConfirm = {
                        viewModel.prestigeEmpire()
                        showPrestigeConfirm = false
                        isSidePanelOpen = false
                    }
                )
            }
        }
    }
}

// Z-index helper
private fun Modifier.zOrder(z: Float): Modifier = this.then(
    Modifier.pointerInput(Unit) { /* prevent pass-through click events */ }
)

// ==========================================
// 🔥 SCREEN 1: IZGARA (Main tap skewer window)
// ==========================================
@Composable
fun IzgaraScreen(viewModel: GameViewModel, state: GameState) {
    var rawTapX by remember { mutableStateOf(0f) }
    var rawTapY by remember { mutableStateOf(0f) }

    // Infinite rotation transition for the Giant Döner Skewer
    val infiniteTransition = rememberInfiniteTransition()
    val skewerRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Giant Skewer pulsating size transition to mimic cooking sizzle
    val skewerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        rawTapX = offset.x
                        rawTapY = offset.y
                        viewModel.tapSkewer(offset.x, offset.y)
                    }
                )
            }
    ) {
        // Starry night grill environment background glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SizzlingRed.copy(alpha = 0.08f),
                radius = 350.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2 + 50.dp.toPx())
            )
            drawCircle(
                color = SaffronOrange.copy(alpha = 0.04f),
                radius = 550.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2 + 50.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Stats Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${viewModel.formatPoints(state.totalSaltPoints)} SP",
                    color = NeonYellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.testTag("sp_display_text")
                )
                Text(
                    text = "TUZ PUANI",
                    color = SoftWhite.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Rates Box with spice multiplier summary
                Row(
                    modifier = Modifier
                        .background(SlateGrey, RoundedCornerShape(20.dp))
                        .border(1.dp, SizzlingRed.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚀 saniyede +${viewModel.formatPoints(viewModel.calculateCurrentSps() * state.spiceMultiplier)} TUZ",
                        color = SaffronOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    if (state.spiceMultiplier > 1.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(SizzlingRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "x${state.spiceMultiplier.toInt()}",
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Giant Rotating Döner Skewer drawing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Interactive helper text beneath skewer
                Text(
                    text = "Tuzlamak için Döneri Yumrukla!\n🧂👆🍔",
                    color = SoftWhite.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                )

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(skewerPulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom skewer rendering
                    DonerSkewerVector(rotationAngle = skewerRotationAngle)
                }
            }

            // Standard Bottom Panel Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateGrey, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Baharat Gücü",
                        color = SoftWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "x${state.spiceMultiplier.toInt()} Çarpan",
                        color = SoftGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Robot El",
                        color = SoftWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.robotHandUnlocked) "AKTİF 🤖" else "KAPALI 🔌",
                        color = if (state.robotHandUnlocked) MintGreen else SizzlingRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // --- PARTICLE EMITTER OVERLAY DRAWINGS ---
        // Grains of salt sparkles
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (p in viewModel.particles) {
                if (p.text.isEmpty()) {
                    val color = when (p.colorType) {
                        2 -> Brush.radialGradient(
                            listOf(SizzlingRed, SaffronOrange, Color.Transparent),
                            center = Offset(p.x, p.y),
                            radius = 20f * p.scale
                        )
                        1 -> Brush.radialGradient(
                            listOf(SoftGold, FlameYellow, Color.Transparent),
                            center = Offset(p.x, p.y),
                            radius = 12f * p.scale
                        )
                        else -> Brush.radialGradient(
                            listOf(Color.White, SoftWhite.copy(alpha = 0.7f), Color.Transparent),
                            center = Offset(p.x, p.y),
                            radius = 8f * p.scale
                        )
                    }
                    if (p.colorType == 2) {
                        // Fire star/spark shape
                        drawCircle(
                            brush = color,
                            radius = 15f * p.scale,
                            center = Offset(p.x, p.y),
                            alpha = p.alpha
                        )
                    } else {
                        // Standard salt dot
                        drawCircle(
                            color = if (p.colorType == 1) SoftGold else Color.White,
                            radius = 4f * p.scale,
                            center = Offset(p.x, p.y),
                            alpha = p.alpha
                        )
                    }
                }
            }
        }

        // Render Score Texts Overlay (Floating letters)
        viewModel.particles.forEach { p ->
            if (p.text.isNotEmpty()) {
                Box(
                    modifier = Modifier.offset { IntOffset(p.x.toInt() - 100, p.y.toInt() - 30) }
                ) {
                    Text(
                        text = p.text,
                        color = if (p.colorType == 2) SizzlingRed else if (p.colorType == 1) SoftGold else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (14.sp * p.scale),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(200.dp),
                        style = LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                blurRadius = 6f,
                                offset = Offset(2f, 2f)
                            )
                        )
                    )
                }
            }
        }
    }
}

// Custom Draw skewer rendering
@Composable
fun DonerSkewerVector(rotationAngle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val width = size.width
        val height = size.height

        withTransform({
            rotate(rotationAngle, pivot = Offset(centerX, centerY))
        }) {
            // Draw central metallic skewer rod
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(centerX - 6.dp.toPx(), 10.dp.toPx()),
                size = Size(12.dp.toPx(), height - 20.dp.toPx())
            )
            
            // Draw secondary inner skewer shade
            drawRect(
                color = Color.Gray,
                topLeft = Offset(centerX + 1.dp.toPx(), 10.dp.toPx()),
                size = Size(3.dp.toPx(), height - 20.dp.toPx())
            )

            // Draw meaty layer inverted cone layers
            val meatColorTop = Color(0xFF6B2D1B)
            val meatColorMid = Color(0xFF8B3A22)
            val meatColorBottom = Color(0xFF501E11)
            
            // Draw 7 sliced layers of kebab meat mimicking realistic conical donor
            // Slices start wide at top and get slightly narrower towards bottom
            val layers = listOf(
                // Pair(Width, TopOffsetHeight)
                Pair(140.dp.toPx(), centerY - 90.dp.toPx()),
                Pair(155.dp.toPx(), centerY - 65.dp.toPx()),
                Pair(145.dp.toPx(), centerY - 40.dp.toPx()),
                Pair(130.dp.toPx(), centerY - 15.dp.toPx()),
                Pair(115.dp.toPx(), centerY + 10.dp.toPx()),
                Pair(90.dp.toPx(), centerY + 35.dp.toPx()),
                Pair(65.dp.toPx(), centerY + 60.dp.toPx())
            )

            for (i in layers.indices) {
                val layerWidth = layers[i].first
                val layerTopY = layers[i].second
                val layerHeight = 30.dp.toPx()

                val brush = Brush.horizontalGradient(
                    colors = listOf(meatColorBottom, meatColorMid, meatColorTop, meatColorMid, meatColorBottom)
                )

                // Rounded slice representation
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(centerX - layerWidth / 2, layerTopY),
                    size = Size(layerWidth, layerHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx(), 15.dp.toPx())
                )

                // Overlay sizzling grill marks on each layer slice
                for (markX in -2..2) {
                    val angleOffset = markX * (layerWidth / 6)
                    drawLine(
                        color = Color(0xFF240D07).copy(alpha = 0.70f),
                        start = Offset(centerX + angleOffset, layerTopY + 2.dp.toPx()),
                        end = Offset(centerX + angleOffset - 10f, layerTopY + layerHeight - 2.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            // Draw top metallic skewer stopper disc
            drawCircle(
                color = Color.DarkGray,
                radius = 18.dp.toPx(),
                center = Offset(centerX, centerY - 92.dp.toPx())
            )
            
            // Draw bottom base drip metal tray
            drawOval(
                color = Color.DarkGray,
                topLeft = Offset(centerX - 80.dp.toPx(), centerY + 85.dp.toPx()),
                size = Size(160.dp.toPx(), 18.dp.toPx())
            )
        }
    }
}


// ==========================================
// 🏗️ SCREEN 2: İŞLETMELER (Businesses list)
// ==========================================
@Composable
fun IsletmelerScreen(viewModel: GameViewModel, state: GameState, buyMode: Int) {
    val modeText = when (buyMode) {
        1 -> "AL x1"
        10 -> "AL x10"
        100 -> "AL x100"
        else -> "AL MAKS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle Panel Mode header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "İşletmeleri Genişlet",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Kazanç çarpanlarını tırmandır!",
                    color = SoftWhite.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // Buy Multiple Cycle button
            Button(
                onClick = { viewModel.cycleBuyMode() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SizzlingRed,
                    contentColor = SoftWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("buy_mode_cycle_button")
            ) {
                Text(
                    text = modeText,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            
            // SPECIAL UPGRADE: Robot El automation card
            item {
                RobotHandCard(viewModel = viewModel, state = state)
            }

            // All 8 standard upgrades
            items(viewModel.upgrades) { station ->
                val (totalCost, actualQuantity) = viewModel.getUpgradeCostAndQuantity(station)
                val currentLevel = viewModel.getUpgradeLevel(station.id, state)
                val isUnlocked = currentLevel > 0 || state.totalAccumulatedPointsOnly >= (station.baseCost * 0.4) // show if partially close
                val canAfford = state.totalSaltPoints >= totalCost

                if (isUnlocked) {
                    UpgradeItemCard(
                        station = station,
                        currentLevel = currentLevel,
                        totalCost = totalCost,
                        actualQuantity = actualQuantity,
                        canAfford = canAfford,
                        onBuyClick = { viewModel.buyUpgrade(station) },
                        viewModel = viewModel
                    )
                } else {
                    // Locked station hidden mode card placeholder to motivate users
                    LockedItemCard(
                        station = station,
                        requiredStartCost = station.baseCost
                    )
                }
            }
        }
    }
}

// Special display card representing "Robot El" Purchase/Unlock
@Composable
fun RobotHandCard(viewModel: GameViewModel, state: GameState) {
    val isUnlocked = state.robotHandUnlocked
    val canAfford = viewModel.canAffordRobotHand()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) MintGreen.copy(alpha = 0.5f) else SaffronOrange.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SlateGrey
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isUnlocked) MintGreen.copy(alpha = 0.15f) else SaffronOrange.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Robot El (Oto Süzme)",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Dönerleri her 0.5 saniyede otomatik tuzlar.",
                        color = SoftWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (isUnlocked) "AKTİVASYON TAMAM" else "Ücret: 10.0K SP",
                        color = if (isUnlocked) MintGreen else SaffronOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (!isUnlocked) {
                Button(
                    onClick = { viewModel.buyRobotHand() },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronOrange,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                        contentColor = DeepBlack,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("buy_robot_hand_button")
                ) {
                    Text(
                        text = "AÇ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else {
                IconButton(
                    onClick = {},
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Etkinleşti",
                        tint = MintGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Active upgrade element
@Composable
fun UpgradeItemCard(
    station: UpgradeStation,
    currentLevel: Int,
    totalCost: Double,
    actualQuantity: Int,
    canAfford: Boolean,
    onBuyClick: () -> Unit,
    viewModel: GameViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (canAfford) SizzlingRed.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SlateGrey
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Station emoji box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(SizzlingRed.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(station.emoji, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = station.name,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SaffronOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lvl $currentLevel",
                                color = SaffronOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = station.description,
                        color = SoftWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "⚡ Saniye: +${viewModel.formatPoints(station.spsRate * currentLevel)} SP " +
                                "(+${viewModel.formatPoints(station.spsRate)} SP/lvl)",
                        color = FlameYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Buy pricing action block
            Button(
                onClick = onBuyClick,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SizzlingRed,
                    disabledContainerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = SoftWhite,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .width(95.dp)
                    .testTag("buy_${station.id}_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+$actualQuantity AL",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Text(
                        text = viewModel.formatPoints(totalCost),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// Locked Upgrade station visual shadow card
@Composable
fun LockedItemCard(station: UpgradeStation, requiredStartCost: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.04f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Kilitli İstasyon...",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Açmak için daha çok Tuz biriktirmelisin.",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${requiredStartCost.toInt()} SP",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ==========================================
// 🏆 SCREEN 3: BAŞARILAR (Achievements grid)
// ==========================================
@Composable
fun BasarilarScreen(viewModel: GameViewModel, state: GameState) {
    val unlockedString = state.unlockedAchievements

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = "Kazanılan Rozetler",
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            // Percentage of achievements unlocked
            val countUnlocked = viewModel.achievements.count { unlockedString.contains(it.id) }
            Text(
                text = "Toplam Başarılar: $countUnlocked / ${viewModel.achievements.size} Unvan",
                color = SaffronOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.achievements) { achievement ->
                val isUnlocked = unlockedString.contains(achievement.id)
                AchievementCard(achievement = achievement, isUnlocked = isUnlocked)
            }
        }
    }
}

// Single achievement element
@Composable
fun AchievementCard(achievement: Achievement, isUnlocked: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(
                width = 1.dp,
                color = if (isUnlocked) Color(achievement.badgeColorHex).copy(alpha = 0.40f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) SlateGrey else SlateGrey.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (isUnlocked) Color(achievement.badgeColorHex).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) achievement.iconEmoji else "🔒",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.title,
                color = if (isUnlocked) SoftWhite else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = achievement.criteriaDescription,
                color = if (isUnlocked) SaffronOrange.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}


// ==========================================
// 👑 FLOATING DRAWER PANEL (Side Menu)
// ==========================================
@Composable
fun SideMenuPanel(
    viewModel: GameViewModel,
    gameState: GameState,
    onClose: () -> Unit,
    onPrestigeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(SlateGrey)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İmparatorluk Bilgisi",
                    color = SoftGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(onClick = onClose) {
                    Text("❌", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Card inside Drawer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Dükkan İstatistikleri",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    StatRow(label = "Toplam Tuzun:", value = viewModel.formatPoints(gameState.totalSaltPoints))
                    StatRow(label = "Tüm Zamanlar:", value = viewModel.formatPoints(gameState.totalAccumulatedPointsOnly))
                    StatRow(label = "Büyüme Çarpanı:", value = "%${(gameState.spiceMultiplier * 100).toInt()}")
                    StatRow(label = "Prestij Sayın:", value = "${gameState.prestigeCount} Reopen")
                }
            }
        }

        // Prestige System trigger card block
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prestigeReady = viewModel.isPrestigeAvailable()
            val bonusToAdd = viewModel.calculatePrestigeMultiplierBonus()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (prestigeReady) SaffronOrange.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = CharcoalDark
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✨ Reopen & Baharat Gücü ✨",
                        color = SoftGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "İmparatorluğu kapat ve yepyeni tariflerle yeniden aç! " +
                                "Karşılığında kalıcı çarpan kazanırsın.",
                        color = SoftWhite.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (prestigeReady) "Kazanılacak Saffron Çarpanı: +$bonusToAdd" else "En az 1.0M Tuz birikmeli!",
                        color = if (prestigeReady) MintGreen else SizzlingRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onPrestigeClick,
                        enabled = prestigeReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronOrange,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            contentColor = DeepBlack,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prestige_action_button")
                    ) {
                        Text(
                            text = "YENİDEN BAŞLAT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "Kebap Ustası v1.0 • AI Studio",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = SoftWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}


// ==========================================
// 💬 DIALOG OVERLAYS AND CELEBRATION
// ==========================================

// Offline Progression dialog
@Composable
fun OfflineDialog(summary: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, SaffronOrange, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👨‍🍳 DÜKKAN HARIL HARIL ÇALIŞTI!",
                    color = SoftGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = summary,
                    color = SoftWhite,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange, contentColor = DeepBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_offline_button")
                ) {
                    Text("TUZU CEBE AT!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Milestone célébration full viewport dialog
@Composable
fun MilestoneCelebrationDialog(text: String, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.90f)),
            contentAlignment = Alignment.Center
        ) {
            // Background sparkles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val offsetList = listOf(
                    Offset(size.width * 0.15f, size.height * 0.2f),
                    Offset(size.width * 0.85f, size.height * 0.15f),
                    Offset(size.width * 0.7f, size.height * 0.8f),
                    Offset(size.width * 0.3f, size.height * 0.75f)
                )
                for (of in offsetList) {
                    drawCircle(brush = Brush.radialGradient(listOf(SoftGold.copy(alpha = 0.3f), Color.Transparent)), radius = 100f, center = of)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎉 DÖNÜM NOKTASI 🎉",
                    color = SoftGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = text,
                    color = SoftWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier.scale(pulse)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .background(SizzlingRed, RoundedCornerShape(16.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 32.dp, vertical = 14.dp)
                        .testTag("dismiss_milestone_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DEVAM ET",
                        color = SoftWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// Prestige warnings Confirmation window
@Composable
fun PrestigeConfirmDialog(
    viewModel: GameViewModel,
    gameState: GameState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, SizzlingRed, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ RESTORANI KAPATIYOR MUSUN?",
                    color = SizzlingRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dikkat Şef!\n\n" +
                            "Yeniden açtığında tüm sahip olduğun işletme dükkanların, elemanların ve mevcut Tuz Puanın **SIFIRLANACAK**!\n\n" +
                            "Fakat kalıcı olarak:\n" +
                            "🔥 +${viewModel.calculatePrestigeMultiplierBonus().toInt()} Saffron Baharat Gücü Çarpanı kazanacaksın!\n\n" +
                            "Bu çarpan gelecekteki tüm kazancını fazlasıyla katlayacak!",
                    color = SoftWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prestige_dimiss_confirm_button")
                    ) {
                        Text("VAZGEÇ", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SizzlingRed, contentColor = SoftWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prestige_approve_confirm_button")
                    ) {
                        Text("KAPATIYORUM!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
