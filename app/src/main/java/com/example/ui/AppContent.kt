package com.example.ui

import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.BuildingState
import com.example.data.LeaderboardEntry
import com.example.ui.theme.*
import com.example.viewmodel.Achievement
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.pow
import kotlin.random.Random
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AppContent(viewModel: GameViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = EaseInOutQuart)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = EaseInOutQuart)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = EaseInOutQuart)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = EaseInOutQuart)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        composable("main") {
            MainGameScreen(viewModel = viewModel, navController = navController)
        }
        composable("prestige") {
            PrestigePageScreen(viewModel = viewModel, navController = navController)
        }
    }
}

@Composable
fun MainGameScreen(viewModel: GameViewModel, navController: NavController) {
    val gameState by viewModel.gameState.collectAsState()
    val buildingsMap by viewModel.buildingsMap.collectAsState()
    val achievementsList by viewModel.achievementsList.collectAsState()
    val ramadanEvent by viewModel.ramadanEventState.collectAsState()
    val leaderboardList by viewModel.leaderboardList.collectAsState()
    val buyMode by viewModel.buyMode.collectAsState()

    val offlineSummary by viewModel.offlineSummary.collectAsState()
    val milestoneCelebration by viewModel.milestoneCelebration.collectAsState()
    val achievementCelebration by viewModel.achievementCelebration.collectAsState()

    var currentTab by remember { mutableStateOf(0) } // 0 = Izgara, 1 = İşletmeler, 2 = Harita, 3 = Başarılar, 4 = Dükkan, 5 = Liderlik
    var showSettings by remember { mutableStateOf(false) }
    var showSeasonPanel by remember { mutableStateOf(false) }

    // Visual tracking of tap ticks for parallax decay
    var tapSlightOffset by remember { mutableStateOf(0f) }
    LaunchedEffect(tapSlightOffset) {
        if (tapSlightOffset > 0f) {
            delay(120)
            tapSlightOffset = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalDark)
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🍢 KEBAP USTASI",
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronOrange,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (gameState.isVIP) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(NeonYellow, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("VIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepBlack)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Core Prestige Button Crown trigger
                        val canPrestigeNow = viewModel.canPrestige()
                        IconButton(
                            onClick = {
                                navController.navigate("prestige")
                            },
                            modifier = Modifier
                                .testTag("menu_badge_button")
                                .padding(end = 4.dp)
                                .scale(if (canPrestigeNow) 1.2f else 1.0f)
                        ) {
                            Icon(
                                imageVector = if (canPrestigeNow) Icons.Default.MilitaryTech else Icons.Default.MilitaryTech,
                                contentDescription = "Prestij Menüsü",
                                tint = if (canPrestigeNow) NeonYellow else SoftGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Seasonal Panel trigger
                        IconButton(
                            onClick = { showSeasonPanel = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 23.sp
                            )
                        }

                        // Settings trigger
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = SoftWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = CharcoalDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val tabs = listOf(
                        Triple("Izgara", "🔥", 0),
                        Triple("İşletmeler", "🏗️", 1),
                        Triple("Harita", "🗺️", 2),
                        Triple("Başarılar", "🏆", 3),
                        Triple("Dükkan", "🛒", 4),
                        Triple("Sıralama", "🏅", 5)
                    )
                    tabs.forEach { (title, symbol, index) ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Text(symbol, fontSize = 20.sp) },
                            label = { Text(title, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SizzlingRed,
                                selectedTextColor = SizzlingRed,
                                indicatorColor = SlateGrey,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> GridIzgaraView(
                        viewModel = viewModel,
                        gameState = gameState,
                        tapOffsetShift = tapSlightOffset,
                        onTapPerformed = { tapSlightOffset = 8f }
                    )
                    1 -> UpgradesListView(
                        viewModel = viewModel,
                        gameState = gameState,
                        buyMode = buyMode
                    )
                    2 -> WorldMapView(
                        viewModel = viewModel,
                        gameState = gameState
                    )
                    3 -> AchievementsListView(
                        viewModel = viewModel,
                        achievementsList = achievementsList
                    )
                    4 -> ShopTabScreen(
                        viewModel = viewModel,
                        gameState = gameState
                    )
                    5 -> LeaderboardView(
                        viewModel = viewModel,
                        leaderboardList = leaderboardList,
                        gameState = gameState
                    )
                }
            }
        }

        // --- Dialogs overlays ---

        // Offline earnings report
        offlineSummary?.let { summary ->
            Dialog(
                onDismissRequest = { viewModel.dismissOfflineEarnings() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(1.dp, SaffronOrange, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👨‍🍳 KEBAP RAPORU 🌯", color = SaffronOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = summary,
                            color = SoftWhite,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.dismissOfflineEarnings() },
                            colors = ButtonDefaults.buttonColors(containerColor = SizzlingRed)
                        ) {
                            Text("DÖNER OCAĞINA GEÇ!", color = SoftWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Milestone achievement celebration modal popup
        milestoneCelebration?.let { milestoneText ->
            Dialog(
                onDismissRequest = { viewModel.dismissMilestone() }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateGrey),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, SoftGold, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 DÖNÜM NOKTASI 🎉", color = SoftGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = milestoneText,
                            color = SoftWhite,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.dismissMilestone() },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                        ) {
                            Text("EYVALLAH ŞEF", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Achievements unlocked banner pop
        achievementCelebration?.let { bannerText ->
            Dialog(
                onDismissRequest = { viewModel.dismissAchievementCelebration() }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, Colors.MintGreen, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 BAŞARI KİLİDİ 🏆", color = Colors.MintGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = bannerText,
                            color = SoftWhite,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.dismissAchievementCelebration() },
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.MintGreen)
                        ) {
                            Text("HARİKA!", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Active Season Panel Overlay ---
        if (showSeasonPanel) {
            Dialog(
                onDismissRequest = { showSeasonPanel = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .border(1.dp, SaffronOrange, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val seasonList by viewModel.seasonEventsList.collectAsState()
                    val selectedActiveId = gameState.activeSeasonEventId
                    val devModeActive = viewModel.isDevModeEnabled.value

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌙 SEZONLUK KEBAP ŞENLİKLERİ", color = SaffronOrange, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Tıklamalarla özel etkinlik para birimleri toplayın ve şenlik stantlarınızı kurun!",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Senin Şenlik Kasasındakiler:", color = Color.Gray, fontSize = 11.sp)
                                Text("${String.format("%.0f", gameState.eventCurrency)} Altın Tuz 🟡", color = SaffronOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            if (gameState.hasSeasonPass) {
                                Box(
                                    modifier = Modifier
                                        .background(NeonYellow, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("SEASON PASS: 2X ETKİNLİK", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        currentTab = 4 // Direct to shop
                                        showSeasonPanel = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Bilet Al", color = SoftWhite, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.height(350.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(seasonList.size) { i ->
                                val event = seasonList[i]
                                val eventId = event.eventId
                                val isSelected = selectedActiveId == eventId || gameState.hasSeasonPass

                                val eventInfo = when (eventId) {
                                    "ramadan_event" -> Triple("🌙", "Ramazan Şenliği", "Ziyaret İftar Sofrası ve Ramazan pidesi") to Pair("İftar Sofrası Lvl ${event.specialBuildingLevel}", "Altın Tuz 🟡")
                                    "spring_event" -> Triple("🌸", "Nevruz Bahar Şenliği", "Uğurlu çiğ köfteler ve odun ateşi") to Pair("Bahar Sofrası Lvl ${event.specialBuildingLevel}", "Kiraz Çiçeği 🌸")
                                    "sports_event" -> Triple("⚽", "Kebap Kupası", "Seyyar taraftar köfte tezgahları") to Pair("Stad Büfesi Lvl ${event.specialBuildingLevel}", "Futbol Topu ⚽")
                                    "winter_event" -> Triple("❄️", "Kış Mangal Şöleni", "Karlar altında sıcak tuzlama keyfi") to Pair("Kar Fırını Lvl ${event.specialBuildingLevel}", "Kar Tanesi ❄️")
                                    else -> Triple("🎈", "Gizemli Şenlik", "Saklı dürümler") to Pair("Özel Stant Lvl ${event.specialBuildingLevel}", "Altın 🟡")
                                }
                                val icon = eventInfo.first.first
                                val title = eventInfo.first.second
                                val desc = eventInfo.first.third
                                val bldName = eventInfo.second.first
                                val currencySym = eventInfo.second.second

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) SlateGrey else Color(0xFF0F0B0B)
                                    ),
                                    modifier = Modifier.fillMaxWidth().border(
                                        1.dp,
                                        if (isSelected) SizzlingRed else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(icon, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(title, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Colors.MintGreen, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("AKTİF", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        viewModel.selectActiveSeasonEvent(eventId)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("Etkinleştir", color = SoftWhite, fontSize = 9.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(desc, color = Color.Gray, fontSize = 11.sp)

                                        Spacer(modifier = Modifier.height(4.dp))
                                        val remainingMs = event.endTimestamp - System.currentTimeMillis()
                                        val days = maxOf(0L, remainingMs / (24 * 3600 * 1000L))
                                        val hours = maxOf(0L, (remainingMs % (24 * 3600 * 1000L)) / (3600 * 1000L))
                                        val remainingText = if (remainingMs > 0) "⏱️ Kalan Süre: ${days}g ${hours}sa" else "⏱️ Etkinlik Sona Erdi"
                                        Text(remainingText, color = SaffronOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        val cost = viewModel.getSeasonBuildingCost(eventId)
                                        val canAfford = gameState.eventCurrency >= cost || devModeActive

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = bldName,
                                                color = SaffronOrange,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.buySeasonEventBuilding(eventId)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (canAfford) SizzlingRed else Color.DarkGray
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = "Yükselt: ${cost.toInt()} 🟡",
                                                    color = SoftWhite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showSeasonPanel = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DÖNER OCAĞINA DÖN", color = DeepBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Visiting Friend full-screen interactive overlay ---
        val visitingFriend by viewModel.visitingFriend.collectAsState()
        if (visitingFriend != null) {
            val friend = visitingFriend!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Text(
                            text = "✈️ ZİYARET: ${friend.displayName.uppercase()}",
                            color = NeonYellow,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val friendCityName = viewModel.getCityName(friend.cityIndex)
                        Text(
                            text = "$friendCityName Şubesi",
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Burada yaptığın tıklamalar arkadaşına ve sana kazandırır!",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Large glowing interactive skewer circle
                    var skewerScale by remember { mutableStateOf(1f) }
                    LaunchedEffect(skewerScale) {
                        if (skewerScale > 1f) {
                            delay(100)
                            skewerScale = 1f
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .scale(skewerScale)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(SaffronOrange.copy(alpha = 0.35f), Color.Transparent)))
                                .border(4.dp, SaffronOrange, CircleShape)
                                .clickable {
                                    skewerScale = 1.12f
                                    viewModel.tapFriendSkewerInVisitMode()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍢", fontSize = 80.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("TIKLA!", color = SaffronOrange, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Bugünkü Ziyaret Tıklamaların: ${friend.dailyVisitTapsUsed} / 30",
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Maksimum 30 tıklamaya ulaştığında muazzam bonus tuz kazanırsın!",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = { viewModel.closeFriendVisit() },
                        colors = ButtonDefaults.buttonColors(containerColor = SizzlingRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🚪 KENDİ DÜKKANIMA GERİ DÖN", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Settings Screen Drawer Modal
        if (showSettings) {
            Dialog(
                onDismissRequest = { showSettings = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .border(1.dp, SizzlingRed, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    var inputName by remember { mutableStateOf(gameState.displayName) }
                    var confirmationLevel by remember { mutableStateOf(0) }

                    LazyColumn(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text("🔧 AYARLAR & YAPILANDIRMA", color = SizzlingRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Display name configurations
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Şef İsmi Belirle", color = SaffronOrange) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaffronOrange,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = SoftWhite,
                                    unfocusedTextColor = SoftWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setPlayerDisplayName(inputName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Şef İsmini Güncelle", color = DeepBlack, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Sound toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ses Efektleri (Simüle)", color = SoftWhite)
                                Switch(
                                    checked = viewModel.soundEnabled.value,
                                    onCheckedChange = { viewModel.soundEnabled.value = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Notification simulation settings
                            Button(
                                onClick = {
                                    viewModel.dismissMilestone()
                                    viewModel.setPlayerDisplayName("Usta_$inputName")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Bildirim İznini Simüle Et", color = SoftWhite)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Divider(color = SlateGrey)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Dev Mode configuration toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🧪 Üretici 'Dev Mode' (Hile)", color = NeonYellow, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = viewModel.isDevModeEnabled.value,
                                    onCheckedChange = { viewModel.isDevModeEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonYellow)
                                )
                            }
                            if (viewModel.isDevModeEnabled.value) {
                                Text(
                                    text = "Aktifken: Tüm bina alımları, IAP kilitleri ücretsizdir. Çevrimdışı çarpan x3 olur.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = SlateGrey)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Reset fully buttons dialog with double checking safety limits
                            if (confirmationLevel == 0) {
                                Button(
                                    onClick = { confirmationLevel = 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("OYUNU SIFIRLA (DİKKAT!)", color = SoftWhite, fontWeight = FontWeight.Bold)
                                }
                            } else if (confirmationLevel == 1) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚠️ TÜM VERİLERİNİ SİLECEKSİN! Emin misin?", color = ErrorRed, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        Button(
                                            onClick = { viewModel.resetFully(); confirmationLevel = 0; showSettings = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("EVET, SİL", color = SoftWhite, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { confirmationLevel = 0 },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("İPTAL", color = DeepBlack)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showSettings = false },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ayarları Kapat", color = SoftWhite)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Kebap Ustası v2.0", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------ VIEW 0: GRILL/IZGARA MAIN MANIPULATIONS ------------------

@Composable
fun GridIzgaraView(
    viewModel: GameViewModel,
    gameState: com.example.data.GameState,
    tapOffsetShift: Float,
    onTapPerformed: () -> Unit
) {
    var skewerTappedState by remember { mutableStateOf(false) }

    // Parallax values generated from tap feedback offset
    val parallaxLayer1 by animateFloatAsState(targetValue = tapOffsetShift * 0.3f, label = "parallax city")
    val parallaxLayer2 by animateFloatAsState(targetValue = tapOffsetShift * 0.7f, label = "parallax booths")

    // Ramadan Event hours checker
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isRamazanHours = hour in 18..20

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // LAYER 1 Parallax: Distant dark skyline
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-20).dp + parallaxLayer1.dp)
        ) {
            val skylineBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF07040B), Color(0xFF13091B))
            )
            drawRect(brush = skylineBrush)
            // Draw skyline block shadows
            val skyPoints = listOf(
                Offset(100f, 600f), Offset(100f, 350f), Offset(180f, 350f), Offset(180f, 600f),
                Offset(240f, 600f), Offset(240f, 250f), Offset(350f, 250f), Offset(350f, 600f),
                Offset(480f, 600f), Offset(480f, 400f), Offset(580f, 400f), Offset(580f, 600f),
                Offset(680f, 600f), Offset(680f, 180f), Offset(820f, 180f), Offset(820f, 600f)
            )
            drawRect(color = Color(0xFF0D0814), size = size)
        }

        // LAYER 2 Parallax: Middleground market/stalls shadows
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = parallaxLayer2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw bottom gradient charcoal layout
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF160E08), Color(0xFF221102)),
                        startY = size.height * 0.6f,
                        endY = size.height
                    )
                )
            }
        }

        // AMBIENT FLOATING SALT CRYSTALS DRIFTERS
        // Speeds proportional to current SPS
        val infiniteTransition = rememberInfiniteTransition(label = "drifting")
        val driftY by infiniteTransition.animateFloat(
            initialValue = 1000f,
            targetValue = -100f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "driftY"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val spsRate = viewModel.calculateCurrentSps()
            val particleCount = minOf(15, (spsRate / 10).toInt() + 3)
            val randomSeeded = Random(42)
            for (i in 0 until particleCount) {
                val seedX = randomSeeded.nextFloat() * size.width
                val waveOffset = sin((driftY / 50f) + i) * 30f
                val sizeD = (randomSeeded.nextFloat() * 4f) + 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = sizeD,
                    center = Offset(seedX + waveOffset, (driftY + (i * 120)) % size.height)
                )
            }
        }

        // MAIN CONTENT UPPER COLUMN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scores, balances, and real-time passive SPS generators indicators
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalDark.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .border(1.dp, SaffronOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${viewModel.formatPoints(gameState.saltPoints)} SP",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonYellow,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("sp_display")
                    )
                    Text(
                        text = "Aktif Kazanç Gücü: +${viewModel.formatPoints(viewModel.calculateCurrentSps())} SP/sn",
                        fontSize = 14.sp,
                        color = SaffronOrange,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "Biriken: ${viewModel.formatPoints(gameState.totalEverEarned)}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Prestij: x${2.0.pow(gameState.prestigeCount.toDouble()).toInt()} ",
                            fontSize = 11.sp,
                            color = SoftGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Ramadan seasonal active promotion banner if appropriate
            if (isRamazanHours) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SizzlingRed.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, SaffronOrange, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("İftar Kültürü Aktif! (18:00 - 21:00)", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Şu an her tıkla Altın Tuz (+5) kazanırsın!", color = SoftWhite, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2.5D Skewer space on the grill styled with graphicsLayers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                skewerTappedState = true
                                viewModel.tapSkewer(offset.x, offset.y)
                                onTapPerformed()
                                tryAwaitRelease()
                                skewerTappedState = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val skewerRotationY by animateFloatAsState(targetValue = if (skewerTappedState) 18f else 15f, label = "rotateSkewer")
                val skewerScale by animateFloatAsState(
                    targetValue = if (skewerTappedState) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "scaleSkewer"
                )

                // Back lighting and charcoal flames
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(skewerScale)
                        .graphicsLayer {
                            rotationY = skewerRotationY
                            cameraDistance = 10f * density
                        }
                        .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = SizzlingRed, spotColor = SaffronOrange)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF421605), Color.Transparent),
                                radius = 280f
                            )
                        )
                )

                // Döner Render Structure with Perspective shift skewing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(skewerScale)
                        .graphicsLayer {
                            rotationY = skewerRotationY
                            transformOrigin = TransformOrigin.Center
                        }
                ) {
                    // Meat layers drop-shadow vector shapes
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF6E2319)),
                        modifier = Modifier
                            .width(80.dp)
                            .height(28.dp)
                            .border(1.dp, SizzlingRed, RoundedCornerShape(12.dp))
                    ) {}
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF86341F)),
                        modifier = Modifier
                            .width(110.dp)
                            .height(32.dp)
                            .border(1.dp, SaffronOrange, RoundedCornerShape(14.dp))
                    ) {}
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFA74526)),
                        modifier = Modifier
                            .width(134.dp)
                            .height(36.dp)
                            .border(2.dp, SoftGold, RoundedCornerShape(16.dp))
                    ) {}
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF86341F)),
                        modifier = Modifier
                            .width(100.dp)
                            .height(30.dp)
                    ) {}

                    // Base metal rod skewer handle
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(60.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Gray, Color.White, Color.DarkGray)
                                )
                            )
                    )
                }

                // High performance grain/popup particle simulation overlay using canvas native drawing
                HighPerformanceParticleOverlay(particlesProvider = { viewModel.particles })
            }

            // Bottom guide info
            Text(
                text = "TUZLAMAK İÇİN TIKLA!",
                fontWeight = FontWeight.Black,
                color = SizzlingRed,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun HighPerformanceParticleOverlay(particlesProvider: () -> List<com.example.viewmodel.SaltParticle>) {
    val particles = particlesProvider()
    if (particles.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val pColor = when (p.colorType) {
                1 -> NeonYellow.copy(alpha = p.alpha)
                2 -> SizzlingRed.copy(alpha = p.alpha)
                else -> SoftWhite.copy(alpha = p.alpha)
            }
            if (p.text.isNotEmpty()) {
                val textLayoutResult = textMeasurer.measure(
                    text = p.text,
                    style = TextStyle(
                        color = pColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (13f * p.scale).sp
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(p.x - 30f, p.y)
                )
            } else {
                drawCircle(
                    color = pColor,
                    radius = 4f * p.scale,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

// ------------------ VIEW 1: BUILDING UPGRADES TAB LISTING ------------------

@Composable
fun UpgradesListView(
    viewModel: GameViewModel,
    gameState: com.example.data.GameState,
    buyMode: Int
) {
    val currentCityIndex = gameState.currentCityIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // City Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = viewModel.getCityName(currentCityIndex),
                    color = NeonYellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text("Şehre Bağlı Aktif İşletmeler", color = Color.Gray, fontSize = 12.sp)
            }

            // Buy Mode selectors cycles 1 -> 10 -> 100 -> MAX
            Button(
                onClick = { viewModel.cycleBuyMode() },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
            ) {
                Text(
                    text = if (buyMode == Int.MAX_VALUE) "Miktar: MAKS" else "Miktar: x$buyMode",
                    color = DeepBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multiplier breakdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateGrey, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val totalMultRaw = 2.0.pow(gameState.prestigeCount.toDouble()) * (if (gameState.isVIP) 5.0 else 1.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Toplam Pasif Çarpan:", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("x${String.format("%.1f", totalMultRaw)}", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Upgrades Listing
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(8) { id ->
                val level = viewModel.getUpgradeLevel(currentCityIndex, id)
                val (cost, qtyToBuy) = viewModel.getUpgradeCostAndQuantity(currentCityIndex, id)
                val canAfford = viewModel.canAffordUpgrade(currentCityIndex, id)

                val buildingName = getBuildingName(currentCityIndex, id)
                val buildingDescription = getBuildingDescription(currentCityIndex, id)
                val emoji = getBuildingEmoji(currentCityIndex, id)

                val cityScale = 1000.0.pow(currentCityIndex.toDouble())
                val buildingSpsContribution = viewModel.baseSpsRates[id] * cityScale * level.toDouble()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (level > 0) SlateGrey else CharcoalDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Isometric skew transform design icon box
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .graphicsLayer {
                                    rotationZ = -5f
                                    transformOrigin = TransformOrigin.Center
                                }
                                .background(SizzlingRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, SizzlingRed, RoundedCornerShape(8.dp))
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = buildingName,
                                    color = SoftWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SaffronOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Lvl $level", color = SaffronOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(buildingDescription, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            
                            if (level > 0) {
                                Text(
                                    text = "Üretim: +${viewModel.formatPoints(buildingSpsContribution)} SP/sn",
                                    color = Colors.MintGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // BUY BTN
                        Button(
                            onClick = {
                                viewModel.buyUpgrade(currentCityIndex, id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) SizzlingRed else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (buyMode == Int.MAX_VALUE) "AL ($qtyToBuy)" else "AL x$buyMode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) SoftWhite else Color.LightGray
                                )
                                Text(
                                    text = viewModel.formatPoints(cost),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canAfford) NeonYellow else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Robot Hand auto clicker buy station card
            item {
                val robotUnlocked = gameState.robotHandUnlocked
                val canAffordRobot = viewModel.canAffordRobotHand()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Robot El (Otomatik Tıklatıcı)", color = SoftWhite, fontWeight = FontWeight.Bold)
                            Text("Merkez tabağa her 500ms'de otomatik tuz serper.", color = Color.Gray, fontSize = 11.sp)
                        }
                        if (robotUnlocked) {
                            Box(
                                modifier = Modifier
                                    .background(Colors.MintGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("AKTİF", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.buyRobotHand() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (canAffordRobot) NeonYellow else Color.DarkGray)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("BAĞLA", fontSize = 11.sp, color = DeepBlack, fontWeight = FontWeight.Bold)
                                    Text("10.0K SP", fontSize = 10.sp, color = DeepBlack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------ VIEW 2: WORLD PROGRESS MAP NODES ------------------

@Composable
fun WorldMapView(
    viewModel: GameViewModel,
    gameState: com.example.data.GameState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("🗺️ DİKİLİ TUZ PARKLARI HARİTASI", color = SoftGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Şehirleri fetheterek döner zenginliğini x1000 katla!", color = Color.Gray, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(12) { index ->
                val cityName = viewModel.getCityName(index)
                val threshold = viewModel.getCityUnlockThreshold(index)
                val isUnlocked = gameState.totalEverEarned >= threshold
                val isCurrent = gameState.currentCityIndex == index

                // High contrast nodes card representation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isUnlocked) {
                            if (!isCurrent) {
                                viewModel.unlockCity(index)
                            }
                        }
                        .border(
                            width = 2.dp,
                            color = if (isCurrent) SizzlingRed else if (isUnlocked) SaffronOrange.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) SlateGrey else if (isUnlocked) CharcoalDark else Color(0xFF0F0B0B)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // City state indicators
                        Text(
                            text = if (isCurrent) "📍" else if (isUnlocked) "✅" else "🔒",
                            fontSize = 26.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Şehir ${index + 1}: $cityName",
                                color = if (isUnlocked) SoftWhite else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            val themeDesc = when (index) {
                                0 -> "Seyyar araba tezgâhıyla sokak lezzeti."
                                1 -> "Merkez çarşıların lezzetli kokusu."
                                2 -> "Meşhur Uludağ eteklerinde tereyağı."
                                3 -> "Lüks gökdelen katında altın döner."
                                4 -> "Wagyu ve yeşil wasabi sentezi."
                                5 -> "Uzay üssünde yerçekimsiz döner."
                                6 -> "Giza Piramitleri gölgesinde antika köz kokusu."
                                7 -> "Colosseum meydanında taş fırın ateşi."
                                8 -> "Times meydanında devasa porsiyonlar."
                                9 -> "Tapınak bahçelerinde ördek soslu lezzetler."
                                10 -> "Egzotik yağmur ormanlarında vahşi ızgara."
                                11 -> "Sonsuz boyutlarda sicim teorisi dürümleri."
                                else -> "Bilinmeyen saklı diyar dönercisi."
                            }
                            Text(
                                text = themeDesc,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            if (!isUnlocked) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = (gameState.totalEverEarned / threshold).toFloat().coerceIn(0f, 1f),
                                    color = SizzlingRed,
                                    trackColor = Color.DarkGray,
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                                Text(
                                    text = "Açmak için gereken toplam SP: ${viewModel.formatPoints(threshold)} (Senin: ${viewModel.formatPoints(gameState.totalEverEarned)})",
                                    color = SaffronOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isUnlocked && !isCurrent) {
                            Button(
                                onClick = { viewModel.unlockCity(index) },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                            ) {
                                Text("GİT", color = DeepBlack, fontWeight = FontWeight.Bold)
                            }
                        } else if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .background(SizzlingRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("BURADASIN", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------ VIEW 3: ACHIEVEMENTS PROGRESS ------------------

@Composable
fun AchievementsListView(
    viewModel: GameViewModel,
    achievementsList: List<com.example.data.AchievementState>
) {
    val unlockedMap = achievementsList.associateBy { it.id }
    val categories = listOf("Tümü", "Tuz", "İnşaat", "Şehir", "Prestige", "Özel")
    var selectedCategoryTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("🏆 GELİŞMİŞ ŞEF BAŞARILARI", color = NeonYellow, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Dönercilik kariyerindeki her kilometre taşını gör (30 Toplam Başarı)", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // Categories selector scroll row
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryTab,
            containerColor = Color.Transparent,
            contentColor = NeonYellow,
            edgePadding = 0.dp
        ) {
            categories.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedCategoryTab == idx,
                    onClick = { selectedCategoryTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter definitions
        val filteredDefinitions = remember(selectedCategoryTab) {
            val key = when (selectedCategoryTab) {
                1 -> "tuz"
                2 -> "insaat"
                3 -> "sehir"
                4 -> "prestige"
                5 -> "ozel"
                else -> "all"
            }
            if (key == "all") viewModel.achievementsDefinitions else viewModel.achievementsDefinitions.filter { it.category == key }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDefinitions) { def ->
                val state = unlockedMap[def.id]
                val isUnlocked = state?.isUnlocked ?: false
                val dateText = if (isUnlocked && state?.unlockedTimestamp != null && state.unlockedTimestamp > 0L) {
                    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(state.unlockedTimestamp))
                    "Kazanıldı: $dateStr"
                } else "Kilitli"

                val targetProgressRatio = if (isUnlocked) 1.0f else {
                    val progressValue = state?.progress ?: 0.0
                    (progressValue / def.targetValue).toFloat().coerceIn(0f, 1f)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) SlateGrey else CharcoalDark
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing badge indicators
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (isUnlocked) SoftGold.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isUnlocked) SoftGold else Color.Gray,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(def.iconEmoji, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = def.title,
                                color = if (isUnlocked) SoftGold else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(def.criteriaDescription, color = Color.Gray, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            // Ratio progress indicator
                            LinearProgressIndicator(
                                progress = targetProgressRatio,
                                color = if (isUnlocked) Colors.MintGreen else SaffronOrange,
                                trackColor = Color.DarkGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .background(if (isUnlocked) Colors.MintGreen.copy(alpha = 0.2f) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isUnlocked) "KAZANILDI" else "${(targetProgressRatio * 100).toInt()}%",
                                color = if (isUnlocked) Colors.MintGreen else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------ VIEW 4: SIMULATED IN-APP DÜKKAN SHOP ------------------

@Composable
fun ShopTabScreen(
    viewModel: GameViewModel,
    gameState: com.example.data.GameState
) {
    val ramadanEvent by viewModel.ramadanEventState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("🛒 KEBAPÇI DÜKKANI (STUBLU)", color = SaffronOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Gerçek Google Play Billing altyapısı monteli stüdyo modülleri", color = Color.Gray, fontSize = 11.sp)
            
            // Dev toggles reminder
            if (viewModel.isDevModeEnabled.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(NeonYellow.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, NeonYellow, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "🧪 HİLE MODU AKTİF: Aşağıdaki tüm ürünler ücretsiz tıklandığı an hesabınıza geçer!",
                        color = NeonYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Seasonal Event Section inside shop
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, SizzlingRed, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 Sezonluk: Ramazan Festivali", color = NeonYellow, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Box(
                            modifier = Modifier
                                .background(SizzlingRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Özel Etkinlik", color = SoftWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Altın Tuz para birimi ile özel 'İftar Sofrası' kurarak imparatorluğun SPS gelirine 500,000 SP/sn ek pasif kazanç sağla!",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Altın Tuz Miktarı: ${String.format("%.0f", gameState.eventCurrency)} 🟡", color = SaffronOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Fiyat: ${String.format("%.0f", viewModel.getRamadanBuildingCost())} Altın Tuz", color = Color.Gray, fontSize = 11.sp)
                        }

                        val upgradeCost = viewModel.getRamadanBuildingCost()
                        val canAffordRamadan = gameState.eventCurrency >= upgradeCost || viewModel.isDevModeEnabled.value

                        Button(
                            onClick = { viewModel.buyRamadanBuilding() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAffordRamadan) SizzlingRed else Color.DarkGray
                            )
                        ) {
                            Text("SEVİYE YÜKSELT (Lvl ${ramadanEvent?.specialBuildingLevel ?: 0})", fontSize = 11.sp, color = SoftWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Permanent Purchases
        item {
            Text("🎁 TEK SEFERLİK PAKETLER & EK PAKETLER (DLC)", color = SoftGold, fontWeight = FontWeight.Bold)
        }

        item {
            ShopItemRow(
                title = "👑 Tuz Baronu Paketi",
                desc = "Uygulama reklamlarını tamamen kaldırır ve normal tık kazancını kalıcı olarak ikiye katlar (+X2 Tapping).",
                price = "$2.99",
                onPurchase = { viewModel.purchaseItem("baron") }
            )
        }

        item {
            ShopItemRow(
                title = "⚡ Gurme Baharat Seti",
                desc = "Otomatik tıklatıcı 'Robot El' donanımını derhal ücretsiz bağlar + oyundan uzak kaldığınız saatlerdeki offline çarpanı x3 katına taşır.",
                price = "$1.99",
                onPurchase = { viewModel.purchaseItem("baharat") }
            )
        }

        item {
            ShopItemRow(
                title = "🗺️ Klasik Döner Turu Bileti (DLC 1)",
                desc = "Gereksinimleri beklemeden Şehir 2 (Ankara) ve Şehir 3 (Bursa) lokantalarını anında açarak x1,000,000 global bonus kazandırır.",
                price = "$3.99",
                onPurchase = { viewModel.purchaseItem("harita") }
            )
        }

        item {
            ShopItemRow(
                title = "🕌 Dünya Fatihi Paketi (DLC 2)",
                desc = "Kahire Çarşısı, Roma Piazzası ve New York Deli (Şehir 7, 8 ve 9) lokasyonlarını derhal engelsiz açarak kozmik tuz gücü kazandırır.",
                price = "$2.49",
                onPurchase = { viewModel.purchaseItem("harita_7_9") }
            )
        }

        item {
            ShopItemRow(
                title = "🌌 Boyutlararası Paralel Evren Bileti (DLC 3)",
                desc = "Şangay Mutfağı, Amazon Ormanı ve Gizli Paralel Evren boyutu (Şehir 10, 11 ve 12) kapılarını anında kalıcı olarak aralar.",
                price = "$3.49",
                onPurchase = { viewModel.purchaseItem("harita_10_12") }
            )
        }

        item {
            ShopItemRow(
                title = "👥 Sosyal Şef Seti",
                desc = "Arkadaş kodu snapshot oluşturucuları ve anlık paylaşım asistanı şebekesini dükkanına bağlar.",
                price = "$1.49",
                onPurchase = { viewModel.purchaseItem("sosyal_paketi") }
            )
        }

        item {
            ShopItemRow(
                title = "🎟️ Premium Sezon Bileti (Season Pass)",
                desc = "Nevruz, Kebap Kupası, Ramazan ve Kış Şampiyonası etkinliklerinin hepsine tek seferde katılım, 2x Altın Tuz para kazanma çarpanı sağlar.",
                price = "$3.99",
                onPurchase = { viewModel.purchaseItem("sezon_pass") }
            )
        }

        item {
            ShopItemRow(
                title = "💎 ⭐ HER ŞEY DAHİL ALL-ACCESS PAKETİ",
                desc = "Tuz Baronu, Gurme Baharat Seti, Tüm Bölge DLC Harita Genişlemeleri, Sosyal Şef Kodu Seti, Season Pass ve 5,000 Altın Tuz başlangıç hediyesi!",
                price = "$7.99",
                onPurchase = { viewModel.purchaseItem("all_access") }
            )
        }

        item {
            Text("🌟 AYLIK ABONELİK SİSTEMLERİ", color = SoftGold, fontWeight = FontWeight.Bold)
        }

        item {
            ShopItemRow(
                title = "💎 Kebap VIP Üyelik",
                desc = "Her gün oturum açtığında birikmiş toplam SP tutarının %1'i kadar özel hediye tuz. Döner şişi için parıldayan altın kaplama görünümü ve global liderlikte krallık VIP rozeti verir.",
                price = "$4.99/ay",
                onPurchase = { viewModel.purchaseItem("vip_sub") }
            )
        }

        item {
            ShopItemRow(
                title = "👑 Kebap İmparatoru Aboneliği",
                desc = "Tüm işletmelerin ve tıklamaların toplam pasif gelirini kalıcı x5 çarpanla fırlatır, VIP sezon biletine (Season Pass) anında tam erişim sağlar.",
                price = "$9.99/ay",
                onPurchase = { viewModel.purchaseItem("emp_sub") }
            )
        }
    }
}

@Composable
fun ShopItemRow(
    title: String,
    desc: String,
    price: String,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(price, color = NeonYellow, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                ) {
                    Text("SATIN AL", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ------------------ VIEW 5: LIDERLER MOCK LEADERBOARD ------------------

@Composable
fun LeaderboardView(
    viewModel: GameViewModel,
    leaderboardList: List<LeaderboardEntry>,
    gameState: com.example.data.GameState
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Liderlik, 1 = Arkadaşlarım, 2 = Hediye Kodları
    val friendsList by viewModel.friendsState.collectAsState()
    val lastGift by viewModel.lastGeneratedGiftCode.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("👥 SOSYAL KEBAP MERKEZİ", color = SoftGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Çevrimdışı arkadaşlar edinin, dükkanları gezin ve hediyeleşin!", color = Color.Gray, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // 3-way Segment controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { activeSubTab = 0 },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 0) SizzlingRed else SlateGrey
                )
            ) {
                Text("🏅 Sıralama", color = SoftWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { activeSubTab = 1 },
                modifier = Modifier.weight(1.05f),
                contentPadding = PaddingValues(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 1) SizzlingRed else SlateGrey
                )
            ) {
                Text("👥 Arkadaşlar (${friendsList.size})", color = SoftWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { activeSubTab = 2 },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == 2) SizzlingRed else SlateGrey
                )
            ) {
                Text("🎁 Hediyeleşme", color = SoftWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSubTab) {
            0 -> {
                var isInitiallyLoading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(800)
                    isInitiallyLoading = false
                }

                if (isInitiallyLoading && leaderboardList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(72.dp),
                                colors = CardDefaults.cardColors(containerColor = CharcoalDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                } else if (leaderboardList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🍢", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sıralama Boş", color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Henüz kimse yok — ilk sen ol! 🍢",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    items(leaderboardList) { entry ->
                        val isSelf = entry.isLocalPlayer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelf) SlateGrey else CharcoalDark
                            ),
                            border = BorderStroke(
                                width = if (isSelf) 2.dp else 0.dp,
                                color = if (isSelf) NeonYellow else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = if (entry.rank <= 3) SoftGold else Color.Gray,
                                    modifier = Modifier.width(36.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isSelf) "${entry.playerName} (Sen)" else entry.playerName,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelf) NeonYellow else SoftWhite,
                                            fontSize = 14.sp
                                        )
                                        if (isSelf && gameState.isVIP) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("👑", fontSize = 12.sp)
                                        }
                                    }
                                    Text(
                                        text = "Şehir: ${entry.cityReached} | Prestij: ${entry.prestigeCount}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "${viewModel.formatPoints(entry.totalEarned)} SP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (isSelf) NeonYellow else SaffronOrange
                                )
                            }
                        }
                    }
                }
                }
            }
            1 -> {
                // Friends management listings sub-view
                Column(modifier = Modifier.fillMaxSize()) {
                    var friendCodeInput by remember { mutableStateOf("") }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = friendCodeInput,
                            onValueChange = { friendCodeInput = it },
                            placeholder = { Text("Kebap Kodu / Snapshot Yapıştır", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = SoftWhite, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronOrange,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (friendCodeInput.isNotBlank()) {
                                    viewModel.addFriendFromCodeOrSnapshot(friendCodeInput)
                                    friendCodeInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Ekle", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Senin Kebap Kodun:", color = Color.Gray, fontSize = 11.sp)
                            Text(gameState.friendCode, color = NeonYellow, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                val out = viewModel.generateLocalProfileString()
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(out))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Profilimi Paylaş (Kopyala)", color = SoftWhite, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (friendsList.isEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Arkadaş Listeniz Boş", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kebap Kodu girerek hemen offline ya da online oyuncularla hediyeleşmeye başlayın!",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(friendsList) { friend ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(friend.displayName, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Kod: ${friend.friendCode}", color = Color.Gray, fontSize = 10.sp)
                                            }
                                            Box(
                                                modifier = Modifier.background(SizzlingRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Şehir ${friend.cityIndex + 1}", color = SizzlingRed, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Toplam Kazanılan: ${viewModel.formatPoints(friend.totalEarned)} SP | Prestij: ${friend.prestigeCount}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.visitingFriend.value = friend
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Colors.MintGreen),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("✈️ Seyahat Et (${friend.dailyVisitTapsUsed}/30 Tık)", color = DeepBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.sendGiftToFriend(friend)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("🎁 %1 Tuz Gönder", color = DeepBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeFriend(friend.friendCode) },
                                                modifier = Modifier.size(36.dp).background(SlateGrey, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = SizzlingRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Gifts screen pane
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("🎁 HEDİYE REDEEM & PAYLAŞIM MERKEZİ", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Arkadaşlarından gelen hediye kodlarını burada bozdurarak dev SP bonusları elde et!", color = Color.Gray, fontSize = 11.sp)
                    }

                    item {
                        var giftCodeInput by remember { mutableStateOf("") }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Hediye Kodunu Çöz & SP Yükle (KEBAP-HEDIYE-...)", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = giftCodeInput,
                                    onValueChange = { giftCodeInput = it },
                                    placeholder = { Text("Kodu buraya yapıştırın", color = Color.Gray, fontSize = 11.sp) },
                                    textStyle = TextStyle(color = SoftWhite, fontSize = 12.sp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SaffronOrange, unfocusedBorderColor = Color.DarkGray),
                                    modifier = Modifier.fillMaxWidth().height(80.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.redeemGiftCode(giftCodeInput)
                                        giftCodeInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SizzlingRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🎁 Kodu Bozdur!", color = SoftWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Display last copy code if generated
                    if (lastGift != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                                modifier = Modifier.fillMaxWidth().border(1.dp, NeonYellow, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Son Ürettiğin Paylaşılabilir Hediye Kodun:", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = lastGift!!,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.background(DeepBlack, RoundedCornerShape(4.dp)).padding(6.dp).fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(lastGift!!))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                                        modifier = Modifier.align(Alignment.End),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Kopyala", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------ DEDICATED PRESTIGE SCREEN PAGE USING NAV ------------------

@Composable
fun PrestigePageScreen(viewModel: GameViewModel, navController: NavController) {
    val gameState by viewModel.gameState.collectAsState()
    val canPrestige = viewModel.canPrestige()
    val threshold = viewModel.getNextPrestigeThreshold()
    val totalEver = gameState.totalEverEarned
    val powerMultiplier = 2.0.pow(gameState.prestigeCount.toDouble())

    // Swipe clean dramatic dark layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with back arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri Dön",
                        tint = SoftWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRESTİJ MERKEZİ",
                    color = SoftGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Top: current statistics
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(SizzlingRed.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, SoftGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌀", fontSize = 48.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prestij Seviyesi: ${gameState.prestigeCount}",
                    color = SoftGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Mevcut Baharat Gücü Çarpanı: x$powerMultiplier",
                    color = SaffronOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Middle: visual preview of benefits
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SaffronOrange, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔮 BİR SONRAKİ PRESTİJ GRANTI:",
                        color = NeonYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Yeni Pasif Büyüme Gücü Çarpanı: x${powerMultiplier * 2.0}",
                        color = SoftWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        "• Yeni Seviye Başlığı: ${if (gameState.prestigeCount == 0) "Döner Çırağı" else if (gameState.prestigeCount == 1) "Sos Ustası" else "Kebap Gurusu"}",
                        color = SoftWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        "• Not: Tüm bina seviyeleriniz ve SP nakit bakiyeniz sıfırlanır, ancak kazanılmış başarılarınız, display nameleriniz ve biriken toplam puanınız korunur!",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Bottom: Huge button with custom progress bars
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val progressRatio = (totalEver / threshold).toFloat().coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = progressRatio,
                    color = SoftGold,
                    trackColor = Color.DarkGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${viewModel.formatPoints(totalEver)} / ${viewModel.formatPoints(threshold)} SP Birikti",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.performPrestige()
                        navController.popBackStack()
                    },
                    enabled = canPrestige,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPrestige) SoftGold else Color.DarkGray,
                        disabledContainerColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = if (canPrestige) 2.dp else 0.dp,
                            color = if (canPrestige) NeonYellow else Color.Transparent,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (canPrestige) "İM-PA-RA-TOR-LU-ĞU SIRADAKİ SEVİYEYE TAŞI!" else "YETERSİZ BİRİKMİŞ SP (KİLİTLİ)",
                        color = if (canPrestige) DeepBlack else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Support definitions for some color properties
object Colors {
    val MintGreen = Color(0xFF34C759)
}

fun getBuildingName(cityIndex: Int, buildingId: Int): String {
    return when (cityIndex) {
        0 -> when (buildingId) {
            0 -> "Tuz Değirmeni"
            1 -> "Közleme Fırını"
            2 -> "Baharat Deposu"
            3 -> "Pide Atölyesi"
            4 -> "Sokak Arabası"
            5 -> "Kebapçı Dükkanı"
            6 -> "Restoran Zinciri"
            7 -> "Kebap İmparatorluğu"
            else -> "Bilinmeyen Yapı"
        }
        1 -> when (buildingId) {
            0 -> "Ankara Tuz Taşı"
            1 -> "Ulus Merkez Köz Ocağı"
            2 -> "Samanpazarı Ambarı"
            3 -> "Başkent Tandır Fırını"
            4 -> "Suni Göl Arabası"
            5 -> "Kızılay Kebap Salonu"
            6 -> "Atatürk Orman Çiftliği Bayisi"
            7 -> "Atakule Zirve Kebapçısı"
            else -> "Ankara Yapısı"
        }
        2 -> when (buildingId) {
            0 -> "Uludağ Kaya Tuzu"
            1 -> "Yeşil Bursa Mangalı"
            2 -> "İpek Han Deposu"
            3 -> "Tarihi Tırnak Pidesi"
            4 -> "Tophane Seyyar Tezgahı"
            5 -> "Çekirge Termal Izgara"
            6 -> "İskender Sarayı"
            7 -> "Osmanlık Kebap Hanı"
            else -> "Bursa Yapısı"
        }
        3 -> when (buildingId) {
            0 -> "Altın Toz Karıştırıcı"
            1 -> "Desert Safari Lav Mangalı"
            2 -> "Safran ve Musk Deposu"
            3 -> "Trüf Mantarlı Pide Atölyesi"
            4 -> "Marina Yat Döner Tezgahı"
            5 -> "Burj Kebap Salonu"
            6 -> "Palmiye Adaları Restoranı"
            7 -> "Emirlik Kebap Sarayı"
            else -> "Dubai Yapısı"
        }
        4 -> when (buildingId) {
            0 -> "Wasabi Tuz Değirmeni"
            1 -> "Cyber Izgara Gözü"
            2 -> "Wagyu Sos Sığınağı"
            3 -> "Gyoza Pide Fırını"
            4 -> "Shibuya Sokak Arabası"
            5 -> "Sushi-Kebap Fusion Cafe"
            6 -> "Skytree Oteli Restoranı"
            7 -> "Samuray Kebabı Ocağı"
            else -> "Tokyo Yapısı"
        }
        5 -> when (buildingId) {
            0 -> "Kozmik Kristal Değirmeni"
            1 -> "Plazma Lazer Ocağı"
            2 -> "Kargo Mekik Ambarı"
            3 -> "Yerçekimsiz Pide Fırını"
            4 -> "Asteroit Seyyar Kapsülü"
            5 -> "Kozmopolit Uzay Kebapçısı"
            6 -> "Yıldızlararası Döner İstasyonu"
            7 -> "Galaksi Fatihleri Sarayı"
            else -> "Uzay Yapısı"
        }
        else -> "Döner Yapısı"
    }
}

fun getBuildingEmoji(cityIndex: Int, buildingId: Int): String {
    return when (buildingId) {
        0 -> "🧂"
        1 -> "🌋"
        2 -> "🌶️"
        3 -> "🫓"
        4 -> "🛒"
        5 -> "🏪"
        6 -> "🏨"
        7 -> "🏰"
        else -> "🔥"
    }
}

fun getBuildingDescription(cityIndex: Int, buildingId: Int): String {
    return when (buildingId) {
        0 -> "Temel tuzlama taneleri öğütür."
        1 -> "Ateş közlerinin lezzetini dönerle buluşturur."
        2 -> "Baharat stoklarını depolayıp korur."
        3 -> "Sıcak tırnak pidelerini çıtır fırınlar."
        4 -> "Kalabalık caddelerde porsiyon keser."
        5 -> "Genis salonunda döner sunar."
        6 -> "Devasa şehirlere uzanan dev bayiler zinciri."
        7 -> "Ülke sınırlarını aşan döner gücü."
        else -> "Leziz tatlar üretir."
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1C1C1E),
                Color(0xFF2C2C2E),
                Color(0xFF1C1C1E),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}
