package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AchievementState
import com.example.data.InventoryItem
import com.example.data.PlayerStats
import com.example.data.SkillPower
import com.example.ui.viewmodel.FloatingDamage
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Japanese Theme Colors ---
val InkBlack = Color(0xFF080808)
val BloodRed = Color(0xFFDC2626)
val GoldAccent = Color(0xFFEAB308)
val SoftPeach = Color(0xFFF1F5F9)
val SpiritCyan = Color(0xFF00E5FF)
val ShadowGrey = Color(0xFF121214)
val SpiritPurple = Color(0xFF9C27B0)

// Pre-defined weather particle state
data class Particle(
    var x: Float,
    var y: Float,
    var speedY: Float,
    var speedX: Float,
    var size: Float,
    var angle: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameUi(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()
    val showDailyDialog by viewModel.showDailyDialog.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    // Surface holder with full edge-to-edge cinematic background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
    ) {
        // Atmospheric radial/linear gradients (Immersive Cinematic Background)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Top red ambient radial gradient (corresponds to radial-[circle_at_50%_40%] from-red-900/20)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7F1D1D).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.width * 0.9f
                )
            )

            // 2. Middle red silhouette glow (corresponds to blur-3xl bg-red-600 rounded-full)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDC2626).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.55f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f
            )

            // 3. Bottom overlay vignette to pure black
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF080808)),
                    startY = size.height * 0.45f,
                    endY = size.height
                )
            )
        }

        // Render Screens within safe drawing inset padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.MAIN_MENU -> MainMenuScreen(viewModel, onOpenSettings = { showSettings = true })
                    Screen.COMBAT -> CombatScreen(viewModel)
                    Screen.UPGRADES -> UpgradesScreen(viewModel)
                    Screen.STORE -> StoreScreen(viewModel)
                    Screen.ACHIEVEMENTS -> AchievementsScreen(viewModel)
                    Screen.BATTLE_PASS -> BattlePassScreen(viewModel)
                    Screen.LUCKY_SPIN -> LuckySpinScreen(viewModel)
                    Screen.CHALLENGE_SELECT -> ChallengeSelectScreen(viewModel)
                    Screen.PRACTICE -> PracticeScreen(viewModel)
                }
            }
        }

        // Daily Rewards Login Dialog
        if (showDailyDialog && playerStats != null) {
            DailyLoginDialog(
                streak = playerStats!!.dailyStreak,
                onClaim = { viewModel.claimDailyReward(playerStats!!.dailyStreak) },
                onDismiss = { viewModel.closeDailyDialog() }
            )
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(onDismiss = { showSettings = false })
        }
    }
}

// ==================== MAIN MENU SCREEN ====================
@Composable
fun MainMenuScreen(viewModel: GameViewModel, onOpenSettings: () -> Unit) {
    val stats by viewModel.playerStats.collectAsState()
    val context = LocalContext.current

    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BloodRed)
        }
        return
    }

    val currentStats = stats!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar + Name info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.dp, BloodRed.copy(alpha = 0.6f), CircleShape)
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1599305090598-fe179d501227?auto=format&fit=crop&q=80&w=100",
                            contentDescription = "Shadow Ronin Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(
                            text = "SHADOW RONIN",
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Rank: Master II",
                            color = BloodRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Currencies info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFEAB308), CircleShape) // yellow-500 glow
                            )
                            Text(
                                text = "${currentStats.coins}",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF34D399), CircleShape) // emerald-400
                            )
                            Text(
                                text = "${currentStats.gems}",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Settings icon
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("settings_btn")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Title card
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SHIN-KEN",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = BloodRed,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "SAMURAI SHOWDOWN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                )
            }
        }

        // Beautiful Interactive 3D Character Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BloodRed.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Loaded Dynamic Generated Hero Image
                    AsyncImage(
                        model = R.drawable.shin_ken_hero_1783736875078,
                        contentDescription = "Shin-Ken Japanese Warrior",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.9f)
                                    ),
                                    startY = 0f
                                )
                            )
                    )

                    // Hero Info Badge Top Center
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KAGEURA: THE RONIN",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Level ${currentStats.level}",
                            color = BloodRed,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }

                    // Glowing Katana Vertical Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1.5.dp)
                            .height(110.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, BloodRed, Color.Transparent)
                                )
                            )
                    )

                    // Power Rating Bottom Center
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CURRENT POWER LEVEL",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${currentStats.powerLevel}",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        // Experience progression bar
                        Spacer(modifier = Modifier.height(4.dp))
                        val xpProgress = currentStats.exp.toFloat() / currentStats.maxExp.toFloat()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text(
                                text = "XP",
                                color = SpiritCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = SpiritCyan,
                                trackColor = Color.White.copy(alpha = 0.1f),
                            )
                            Text(
                                text = "${currentStats.exp}/${currentStats.maxExp}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Quick Campaign & Game Modes Select Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SELECT BATTLE MODE",
                    color = GoldAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Campaign Play Button (BATTLE BOSS style)
                Button(
                    onClick = {
                        viewModel.startCombat("STORY")
                        viewModel.navigateTo(Screen.COMBAT)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .testTag("play_story_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BloodRed
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SportsKabaddi, contentDescription = "Sword Combat", modifier = Modifier.size(24.dp), tint = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "CAMPAIGN LEVEL ${currentStats.level}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Round ${currentStats.round}/3 • 100,000 Levels",
                                fontSize = 11.sp,
                                color = SoftPeach.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Grid of Secondary Modes (Survival, Boss Rush)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.startCombat("SURVIVAL")
                            viewModel.navigateTo(Screen.COMBAT)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .testTag("survival_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = "Survival", tint = SpiritCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = "SURVIVAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.startCombat("BOSS_RUSH")
                            viewModel.navigateTo(Screen.COMBAT)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .testTag("boss_rush_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Whatshot, contentDescription = "Boss Rush", tint = BloodRed, modifier = Modifier.size(20.dp))
                            Text(
                                text = "BOSS RUSH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub Navigation Menu Tabs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MainMenuNavIcon(
                        icon = Icons.Filled.MilitaryTech,
                        label = "Upgrades",
                        tag = "nav_upgrades",
                        onClick = { viewModel.navigateTo(Screen.UPGRADES) }
                    )
                    MainMenuNavIcon(
                        icon = Icons.Filled.Storefront,
                        label = "Store",
                        tag = "nav_store",
                        onClick = { viewModel.navigateTo(Screen.STORE) }
                    )
                    MainMenuNavIcon(
                        icon = Icons.Filled.EmojiEvents,
                        label = "Missions",
                        tag = "nav_achievements",
                        onClick = { viewModel.navigateTo(Screen.ACHIEVEMENTS) }
                    )
                    MainMenuNavIcon(
                        icon = Icons.Filled.Casino,
                        label = "Lucky Spin",
                        tag = "nav_lucky_spin",
                        onClick = { viewModel.navigateTo(Screen.LUCKY_SPIN) }
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MainMenuNavIcon(
                        icon = Icons.Filled.ConfirmationNumber,
                        label = "Battle Pass",
                        tag = "nav_battle_pass",
                        onClick = { viewModel.navigateTo(Screen.BATTLE_PASS) }
                    )
                    MainMenuNavIcon(
                        icon = Icons.Filled.Map,
                        label = "Arenas",
                        tag = "nav_arenas",
                        onClick = { viewModel.navigateTo(Screen.CHALLENGE_SELECT) }
                    )
                    MainMenuNavIcon(
                        icon = Icons.Filled.FitnessCenter,
                        label = "Practice",
                        tag = "nav_practice",
                        onClick = { viewModel.navigateTo(Screen.PRACTICE) }
                    )
                }
            }
        }

        // Footer copyright or promotional
        item {
            Text(
                text = "Kyoto Studios © 2026 • Cinematic High-End Realistic Physics",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MainMenuNavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, BloodRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = GoldAccent, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== COMBAT ARENA SCREEN ====================
@Composable
fun CombatScreen(viewModel: GameViewModel) {
    val playerHp by viewModel.playerHp.collectAsState()
    val playerMaxHp by viewModel.playerMaxHp.collectAsState()
    val enemyHp by viewModel.enemyHp.collectAsState()
    val enemyMaxHp by viewModel.enemyMaxHp.collectAsState()
    val playerEnergy by viewModel.playerEnergy.collectAsState()
    val playerMaxEnergy by viewModel.playerMaxEnergy.collectAsState()
    val playerStamina by viewModel.playerStamina.collectAsState()
    val ultimateMeter by viewModel.ultimateMeter.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()

    val arenaName by viewModel.arenaName.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val enemyName by viewModel.enemyName.collectAsState()
    val isBoss by viewModel.isBossBattle.collectAsState()

    val enemyTelegraph by viewModel.enemyTelegraphAction.collectAsState()
    val playerStateVfx by viewModel.playerStateVfx.collectAsState()
    val enemyStateVfx by viewModel.enemyStateVfx.collectAsState()

    val power1Cd by viewModel.power1Cooldown.collectAsState()
    val power2Cd by viewModel.power2Cooldown.collectAsState()
    val power3Cd by viewModel.power3Cooldown.collectAsState()
    val isTimeFrozen by viewModel.isTimeFrozen.collectAsState()

    val combatFinished by viewModel.combatFinished.collectAsState()
    val combatVictory by viewModel.combatVictory.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Particles simulation for Canvas
    val particles = remember { mutableStateListOf<Particle>() }
    LaunchedEffect(weather) {
        particles.clear()
        val count = when (weather) {
            "Rain" -> 50
            "Snow" -> 40
            "Cherry Blossom" -> 30
            else -> 15
        }
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * 800f,
                    speedY = if (weather == "Rain") 12f + Random.nextFloat() * 8f else 1.5f + Random.nextFloat() * 2f,
                    speedX = if (weather == "Cherry Blossom" || weather == "Wind") -1.5f - Random.nextFloat() * 3f else -0.5f + Random.nextFloat() * 1f,
                    size = if (weather == "Cherry Blossom") 8f + Random.nextFloat() * 10f else 3f + Random.nextFloat() * 5f,
                    angle = Random.nextFloat() * 360f
                )
            )
        }
    }

    // Dynamic Frame Ticker for Canvas Particles
    var tickCount by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            tickCount++
            for (p in particles) {
                p.y += p.speedY
                p.x += p.speedX
                if (weather == "Cherry Blossom") {
                    p.angle += 2f
                }
                // reset if fallen below
                if (p.y > 1000f) {
                    p.y = -20f
                    p.x = Random.nextFloat() * 1000f
                }
                if (p.x < -20f || p.x > 1020f) {
                    p.x = if (p.speedX < 0) 1020f else -20f
                    p.y = Random.nextFloat() * 1000f
                }
            }
        }
    }

    // Hit reaction shaking offsets
    val playerShakeOffset = remember { Animatable(0f) }
    val enemyShakeOffset = remember { Animatable(0f) }

    // Observe player damages to animate screen shake
    LaunchedEffect(playerHp) {
        playerShakeOffset.animateTo(
            targetValue = 15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
        )
        playerShakeOffset.animateTo(0f)
    }

    // Observe enemy damages to animate hit shake
    LaunchedEffect(enemyHp) {
        enemyShakeOffset.animateTo(
            targetValue = -15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
        )
        enemyShakeOffset.animateTo(0f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP STATUS BAR (HP Gauges, Level stats, Boss indicator)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ShadowGrey.copy(alpha = 0.9f))
                .padding(12.dp)
        ) {
            // Level & Arena Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit combat", tint = Color.White)
                    }
                    Column {
                        Text(text = arenaName, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = "Weather: $weather", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                if (isBoss) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "👹 BOSS SHOWDOWN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Level ${viewModel.combatLevel.value} - Round ${viewModel.combatRound.value}/3",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player HP bar vs Enemy HP bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player HP
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kageura (You)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${playerHp.toInt()} HP", color = Color.White, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val playerHpRatio = playerHp / playerMaxHp
                    LinearProgressIndicator(
                        progress = { playerHpRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BloodRed,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }

                // Enemy HP
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(enemyName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${enemyHp.toInt()} HP", color = Color.White, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val enemyHpRatio = enemyHp / enemyMaxHp
                    LinearProgressIndicator(
                        progress = { enemyHpRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resources: Stamina & Energy & Ultimate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stamina (Stamina represents action readiness, e.g. green)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.FlashOn, contentDescription = "Stamina", tint = Color.Green, modifier = Modifier.size(14.dp))
                    LinearProgressIndicator(
                        progress = { playerStamina / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color.Green,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text("${playerStamina.toInt()}", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Spirit Energy (glowing blue-cyan)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Energy", tint = SpiritCyan, modifier = Modifier.size(14.dp))
                    LinearProgressIndicator(
                        progress = { playerEnergy / playerMaxEnergy },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = SpiritCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text("${playerEnergy.toInt()}", color = SpiritCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // MIDDLE CINEMATIC BATTLEGROUND
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(InkBlack)
        ) {
            // Background Arena Image loaded dynamically
            AsyncImage(
                model = if (isBoss) R.drawable.shin_ken_boss_oni_1783736889233 else R.drawable.shin_ken_arena_sakura_1783736903431,
                contentDescription = "Showdown Arena background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Weather overlay color cast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (weather) {
                            "Rain", "Thunderstorm" -> InkBlack.copy(alpha = 0.5f)
                            "Snow" -> Color.White.copy(alpha = 0.1f)
                            "Sunset" -> BloodRed.copy(alpha = 0.2f)
                            "Night" -> SpiritPurple.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                    )
            )

            // Draw Dynamic Weather Particles on Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = when (weather) {
                    "Rain" -> Color.Cyan.copy(alpha = 0.6f)
                    "Snow" -> Color.White.copy(alpha = 0.8f)
                    "Cherry Blossom" -> Color(0xFFFFB7C5).copy(alpha = 0.7f)
                    else -> Color.Transparent
                }

                particles.forEach { p ->
                    if (weather == "Cherry Blossom") {
                        // Draw oval petals spinning
                        rotate(p.angle, Offset(p.x, p.y)) {
                            drawOval(
                                color = color,
                                topLeft = Offset(p.x - p.size / 2, p.y - p.size / 4),
                                size = Size(p.size, p.size / 2)
                            )
                        }
                    } else if (weather == "Rain") {
                        // Draw diagonal streaks
                        drawLine(
                            color = color,
                            start = Offset(p.x, p.y),
                            end = Offset(p.x - 4f, p.y + p.size * 2),
                            strokeWidth = 2f
                        )
                    } else {
                        // Snow
                        drawCircle(
                            color = color,
                            radius = p.size / 2,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }

            // Cinematic Lightning Flash for Thunderstorms
            if (weather == "Thunderstorm" && tickCount % 120 == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.45f))
                )
            }

            // Time Freeze screen blue-tint mask
            if (isTimeFrozen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpiritCyan.copy(alpha = 0.25f))
                )
            }

            // Telegraph alert flashing banner
            if (enemyTelegraph != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = BloodRed.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚡ ENEMY SWINGS ${enemyTelegraph!!.replace("_", " ")}! PARRY / DODGE! ⚡",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Combo Hits text
            if (comboCount > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "$comboCount HITS",
                        color = GoldAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = when {
                            comboCount > 15 -> "GODLIKE ACCEL!"
                            comboCount > 8 -> "BLADE MASTER!"
                            else -> "COMBO"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FIGHTER GRAPHICS (Drawn Side by Side)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Player Graphic Character Box
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = playerShakeOffset.value.dp, y = (-20).dp)
                        .size(130.dp)
                ) {
                    // Avatar overlay or visual stance
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, SpiritCyan, RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, SpiritCyan.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Player",
                            modifier = Modifier.size(90.dp),
                            colorFilter = ColorFilter.tint(
                                when (playerStateVfx) {
                                    "DODGING" -> SpiritCyan.copy(alpha = 0.4f)
                                    "COUNTERING" -> GoldAccent
                                    "ULTIMATE" -> SpiritPurple
                                    else -> Color.White
                                }
                            )
                        )
                        Text(
                            text = "KAGEURA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                // Enemy Graphic Character Box
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = enemyShakeOffset.value.dp, y = (-20).dp)
                        .size(130.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, BloodRed, RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, BloodRed.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Enemy",
                            modifier = Modifier.size(90.dp),
                            colorFilter = ColorFilter.tint(
                                when (enemyStateVfx) {
                                    "STUNNED" -> Color.Yellow
                                    "BLOCKING" -> Color.LightGray
                                    "FROZEN" -> SpiritCyan
                                    else -> BloodRed
                                }
                            )
                        )
                        Text(
                            text = if (isBoss) "BOSS" else "TARGET",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                // Render Floating Damage Numbers over the Canvas area
                viewModel.floatingDamage.forEach { fd ->
                    Box(
                        modifier = Modifier
                            .offset(x = fd.x.dp, y = fd.y.dp)
                    ) {
                        Text(
                            text = fd.damage,
                            fontSize = if (fd.isCrit) 28.sp else 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = when (fd.color) {
                                "RED" -> BloodRed
                                "YELLOW" -> GoldAccent
                                "CYAN" -> SpiritCyan
                                "PURPLE" -> SpiritPurple
                                else -> Color.White
                            },
                            style = LocalTextStyle.current.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = InkBlack,
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
            }
        }

        // COMBAT LOGS PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(InkBlack)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val logState = rememberScrollState()
            LaunchedEffect(viewModel.combatLog.size) {
                logState.animateScrollTo(logState.maxValue)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(logState)
            ) {
                viewModel.combatLog.forEach { log ->
                    Text(
                        text = log,
                        color = if (log.contains("VICTORY") || log.contains("🏆")) GoldAccent else if (log.contains("DEFEATED") || log.contains("💥")) BloodRed else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // BOTTOM COMBAT CONTROLS PANEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ShadowGrey)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Special Powers equipped (Dragon Flame Slash, Thunder Dash, Shadow Freeze) and Ultimate Finisher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Power 1 button
                SpecialPowerButton(
                    name = "Flame Slash",
                    cost = "30 SP",
                    cooldown = power1Cd,
                    color = BloodRed,
                    tag = "skill_flame_slash",
                    onClick = { viewModel.castPower1() }
                )

                // Power 2 button
                SpecialPowerButton(
                    name = "Thunder Dash",
                    cost = "25 SP",
                    cooldown = power2Cd,
                    color = GoldAccent,
                    tag = "skill_thunder_dash",
                    onClick = { viewModel.castPower2() }
                )

                // Power 3 button
                SpecialPowerButton(
                    name = "Time Freeze",
                    cost = "40 SP",
                    cooldown = power3Cd,
                    color = SpiritCyan,
                    tag = "skill_time_freeze",
                    onClick = { viewModel.castPower3() }
                )

                // ULTIMATE FINISHER
                Button(
                    onClick = { viewModel.castUltimate() },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("skill_ultimate"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ultimateMeter >= 100f) SpiritPurple else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = ultimateMeter >= 100f
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "FATAL STRIKE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "${ultimateMeter.toInt()}%", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Row 2: Standard Moves (Punch, Kick, Dodge, Roll, Counter, Grab, Jump)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Punch
                MoveActionButton(label = "PUNCH", tag = "move_punch", modifier = Modifier.weight(1f)) { viewModel.performPunch() }
                // Kick
                MoveActionButton(label = "KICK", tag = "move_kick", modifier = Modifier.weight(1f)) { viewModel.performKick() }
                // Grab
                MoveActionButton(label = "GRAB", tag = "move_grab", modifier = Modifier.weight(1f)) { viewModel.performGrab() }
                // Jump
                MoveActionButton(label = "JUMP", tag = "move_jump", modifier = Modifier.weight(1f)) { viewModel.performJumpAttack() }
                // Counter
                MoveActionButton(label = "COUNTER", tag = "move_counter", color = GoldAccent, modifier = Modifier.weight(1.2f)) { viewModel.performCounter() }
                // Dodge
                MoveActionButton(label = "DODGE", tag = "move_dodge", color = SpiritCyan, modifier = Modifier.weight(1f)) { viewModel.performDodge() }
            }
        }
    }

    // Battle Conclusion Summary Dialog
    if (combatFinished) {
        Dialog(onDismissRequest = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(1.dp, if (combatVictory) GoldAccent else BloodRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (combatVictory) "戰 鬥 勝 利" else "戰 敗",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (combatVictory) GoldAccent else BloodRed
                    )

                    Text(
                        text = if (combatVictory) "VICTORY ACHIEVED" else "DEFEATED IN BATTLE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // Rewards List Summary
                    if (combatVictory) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(InkBlack, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("BATTLE EARNINGS:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = GoldAccent, modifier = Modifier.size(16.dp))
                                Text("+${if (isBoss) 300 else 80} Coins", color = Color.White, fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Diamond, contentDescription = "Gems", tint = SpiritCyan, modifier = Modifier.size(16.dp))
                                Text("+${if (isBoss) 5 else 1} Premium Gems", color = Color.White, fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "XP", tint = SpiritCyan, modifier = Modifier.size(16.dp))
                                Text("+20 Battle Pass XP", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "You fought bravely, but the opponent's strategy was superior. Strengthen your Samurai armor and upgrade stats in the Temple!",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.navigateTo(Screen.MAIN_MENU)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("combat_finish_ok"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("BACK TO TEMPLE", color = Color.White, fontSize = 12.sp)
                        }

                        if (combatVictory) {
                            Button(
                                onClick = {
                                    viewModel.startCombat(viewModel.activeMode.value)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("combat_next_battle"),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                            ) {
                                Text("NEXT BATTLE", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialPowerButton(
    name: String,
    cost: String,
    cooldown: Float,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(50.dp)
            .clickable(enabled = cooldown <= 0f, onClick = onClick)
            .background(if (cooldown > 0f) Color.DarkGray else color, RoundedCornerShape(8.dp))
            .testTag(tag)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = if (cooldown > 0f) "${cooldown.toInt()}s CD" else cost, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun MoveActionButton(
    label: String,
    tag: String,
    color: Color = Color.DarkGray,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .height(46.dp)
            .testTag(tag)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ==================== STATS UPGRADES SCREEN ====================
@Composable
fun UpgradesScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    if (stats == null) return

    val currentStats = stats!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("UPGRADE CENTER", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            // Currency Indicators
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEAB308), CircleShape))
                    Text("${currentStats.coins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Strengthen attributes to survive higher levels. Max stats unlocked: 100,000",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Upgradeable Stats list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { StatUpgradeRow("health", "❤️ Health Multiplier", currentStats.healthLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("attack", "⚔️ Strike Attack", currentStats.attackLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("defense", "🛡️ Parry Defense", currentStats.defenseLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("speed", "💨 Speed & Dodge", currentStats.speedLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("critRate", "🎯 Critical Rate", currentStats.critRateLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("critDmg", "💥 Critical Damage", currentStats.critDamageLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("energy", "🔮 Spirit Energy Capacity", currentStats.energyLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("stamina", "⚡ Stamina Regen Speed", currentStats.staminaLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("combo", "Combo Multiplier", currentStats.comboLevel, currentStats.coins, viewModel) }
            item { StatUpgradeRow("ultimate", "Fatal Strike Impact", currentStats.ultimateLevel, currentStats.coins, viewModel) }
        }

        // Skill Powers upgrades (Active Powers damage and CD)
        val skills by viewModel.allSkills.collectAsState()
        if (skills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "UPGRADE SPECIAL ACTIVE POWERS",
                    color = GoldAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(skills) { skill ->
                        SkillUpgradeCard(skill, currentStats.coins, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun StatUpgradeRow(
    id: String,
    label: String,
    level: Int,
    userCoins: Long,
    viewModel: GameViewModel
) {
    val cost = 150 + (level - 1) * 75L
    val canAfford = userCoins >= cost

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Lvl $level", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Next: +${level * 10}% boost", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }

            Button(
                onClick = { viewModel.upgradePlayerStat(id) },
                colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) BloodRed else Color.DarkGray),
                enabled = canAfford,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("upgrade_${id}_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = GoldAccent, modifier = Modifier.size(14.dp))
                    Text("$cost", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun SkillUpgradeCard(
    skill: SkillPower,
    userCoins: Long,
    viewModel: GameViewModel
) {
    val canUpgrade = userCoins >= skill.damageUpgradeCost
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (canUpgrade) BloodRed.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .width(180.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = skill.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "Level ${skill.level}", color = GoldAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(text = skill.description, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, minLines = 2, maxLines = 2)

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Button(
                onClick = { viewModel.upgradeSkillPower(skill.skillId, true) },
                colors = ButtonDefaults.buttonColors(containerColor = if (canUpgrade) BloodRed else Color.DarkGray),
                enabled = canUpgrade,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text("Dmg Power: $${skill.damageUpgradeCost}", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==================== IN-GAME STORE SCREEN ====================
@Composable
fun StoreScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    val items by viewModel.allItems.collectAsState()

    if (stats == null) return

    val currentStats = stats!!
    var selectedTab by remember { mutableStateOf("WEAPON") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("KYOTO ARMORY STORE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Wallet view
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Text(" ${currentStats.coins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Diamond, contentDescription = "Gems", tint = SpiritCyan, modifier = Modifier.size(16.dp))
                    Text(" ${currentStats.gems}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Tab indicators for Item Categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("WEAPON", "ARMOR", "PET", "MOUNT").forEach { tab ->
                Button(
                    onClick = { selectedTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == tab) BloodRed else ShadowGrey
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("store_tab_$tab"),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = tab, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Shop Items
        val filteredItems = items.filter { it.itemType == selectedTab }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredItems) { item ->
                StoreItemRow(item, currentStats, viewModel)
            }
        }
    }
}

@Composable
fun StoreItemRow(
    item: InventoryItem,
    stats: PlayerStats,
    viewModel: GameViewModel
) {
    val isEquipped = when (item.itemType) {
        "WEAPON" -> stats.equippedWeapon == item.itemId
        "ARMOR" -> stats.equippedArmor == item.itemId
        "PET" -> stats.equippedPet == item.itemId
        "MOUNT" -> stats.equippedMount == item.itemId
        else -> false
    }

    val canAfford = stats.coins >= item.costCoins && stats.gems >= item.costGems

    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowGrey),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isEquipped) GoldAccent else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Badge(
                        containerColor = when (item.rarity) {
                            "MYTHIC" -> SpiritPurple
                            "LEGENDARY" -> GoldAccent
                            "EPIC" -> BloodRed
                            else -> Color.Gray
                        }
                    ) {
                        Text(item.rarity, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "+${item.statBoostPercent.toInt()}% ${item.statType.replace("_", " ")} Boost", color = SpiritCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.isUnlocked) {
                    Button(
                        onClick = { viewModel.equipStoreItem(item.itemId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEquipped) GoldAccent else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("equip_${item.itemId}")
                    ) {
                        Text(text = if (isEquipped) "EQUIPPED" else "EQUIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkBlack)
                    }
                } else {
                    Button(
                        onClick = { viewModel.purchaseStoreItem(item.itemId) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) BloodRed else Color.DarkGray),
                        enabled = canAfford,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buy_${item.itemId}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UNLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.costCoins > 0) {
                                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = GoldAccent, modifier = Modifier.size(10.dp))
                                    Text("${item.costCoins}", fontSize = 9.sp, color = Color.White)
                                }
                                if (item.costGems > 0) {
                                    Icon(Icons.Filled.Diamond, contentDescription = "Gems", tint = SpiritCyan, modifier = Modifier.size(10.dp))
                                    Text("${item.costGems}", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== MISSIONS & ACHIEVEMENTS SCREEN ====================
@Composable
fun AchievementsScreen(viewModel: GameViewModel) {
    val achievements by viewModel.allAchievements.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("MISSIONS & ACHIEVEMENTS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(achievements) { ach ->
                AchievementCard(ach, viewModel)
            }
        }
    }
}

@Composable
fun AchievementCard(
    ach: AchievementState,
    viewModel: GameViewModel
) {
    val progress = ach.currentCount.toFloat() / ach.targetCount.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowGrey),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = ach.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = ach.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }

                if (ach.isCompleted && !ach.isClaimed) {
                    Button(
                        onClick = { viewModel.claimAchievementReward(ach.achievementId) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("claim_${ach.achievementId}")
                    ) {
                        Text("CLAIM", color = InkBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else if (ach.isClaimed) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = Color.Green, modifier = Modifier.size(24.dp))
                }
            }

            // Progress Slider Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text("${ach.currentCount}/${ach.targetCount}", color = SpiritCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = SpiritCyan,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            // Rewards badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rewards:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = GoldAccent, modifier = Modifier.size(12.dp))
                    Text("${ach.rewardCoins}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Diamond, contentDescription = "Gems", tint = SpiritCyan, modifier = Modifier.size(12.dp))
                    Text("${ach.rewardGems}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== BATTLE PASS SCREEN ====================
@Composable
fun BattlePassScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()
    if (stats == null) return

    val currentStats = stats!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("SEASON BATTLE PASS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (!currentStats.battlePassPremiumUnlocked) {
                Button(
                    onClick = { viewModel.purchaseBattlePassPremium() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier.testTag("unlock_premium_bp"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("UNLOCK PREMIUM (50 💎)", color = InkBlack, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                }
            } else {
                Badge(containerColor = SpiritPurple) {
                    Text("👑 PREMIUM PASS", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level & Current Progression Display
        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowGrey),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tiers Completed: ${currentStats.battlePassLevel}/100", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("XP: ${currentStats.battlePassXp}/100", color = SpiritCyan, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { currentStats.battlePassXp / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SpiritCyan,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                Button(
                    onClick = { viewModel.buyBattlePassTier() },
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(32.dp)
                        .testTag("buy_bp_level_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Buy Tier (+100 XP) 500 Coins", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tier levels scroll
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items((1..10).toList()) { tier ->
                BattlePassTierRow(tier, currentStats.battlePassLevel, currentStats.battlePassPremiumUnlocked)
            }
        }
    }
}

@Composable
fun BattlePassTierRow(
    tier: Int,
    userBpLevel: Int,
    isPremiumUnlocked: Boolean
) {
    val isCompleted = userBpLevel >= tier

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) ShadowGrey else InkBlack),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCompleted) GoldAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TIER $tier", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (isCompleted) GoldAccent else Color.Gray)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Free Reward
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = Color.Gray) { Text("FREE", color = Color.White, fontSize = 8.sp) }
                    Text("100 Coins", color = Color.White, fontSize = 11.sp)
                    if (isCompleted) Icon(Icons.Filled.Check, contentDescription = "Claimed", tint = Color.Green, modifier = Modifier.size(14.dp))
                }

                // Premium Reward
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = SpiritPurple) { Text("PREM", color = Color.White, fontSize = 8.sp) }
                    Text("5 Gems", color = Color.White, fontSize = 11.sp)
                    if (isCompleted && isPremiumUnlocked) {
                        Icon(Icons.Filled.Check, contentDescription = "Claimed", tint = Color.Green, modifier = Modifier.size(14.dp))
                    } else if (isCompleted && !isPremiumUnlocked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ==================== LUCKY SPIN WHEEL SCREEN ====================
@Composable
fun LuckySpinScreen(viewModel: GameViewModel) {
    val isSpinning by viewModel.isSpinning.collectAsState()
    val spinResult by viewModel.spinResult.collectAsState()
    val stats by viewModel.playerStats.collectAsState()

    var wheelAngle by remember { mutableStateOf(0f) }

    // Spin animation logic
    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            // Spin rapidly then settle
            animate(
                initialValue = wheelAngle,
                targetValue = wheelAngle + 360f * 6 + Random.nextFloat() * 360f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            ) { valAngle, _ ->
                wheelAngle = valAngle
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("LUCKY SPIN WHEEL", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Draw Interactive Wheel of Fortune using Canvas
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(InkBlack)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Background gold ring border
                        drawCircle(
                            color = GoldAccent,
                            radius = size.width / 2,
                            center = center,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }
            ) {
                // Apply rotation transform
                rotate(wheelAngle, center) {
                    val colors = listOf(BloodRed, ShadowGrey, SpiritPurple, ShadowGrey, GoldAccent, ShadowGrey, SpiritCyan, ShadowGrey)
                    val segmentCount = colors.size
                    val sweepAngle = 360f / segmentCount

                    for (i in 0 until segmentCount) {
                        val startAngle = i * sweepAngle
                        drawArc(
                            color = colors[i],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size
                        )

                        // Draw separator lines
                        val radians = (startAngle * PI / 180f).toFloat()
                        drawLine(
                            color = InkBlack,
                            start = center,
                            end = Offset(
                                center.x + (size.width / 2) * cos(radians),
                                center.y + (size.width / 2) * sin(radians)
                            ),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            // Arrow Indicator facing down
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Arrow pointer", tint = BloodRed, modifier = Modifier.size(16.dp))
            }
        }

        // Result Dialog Box
        AnimatedVisibility(visible = spinResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(1.dp, GoldAccent),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎉 LUCKY REWARD 🎉", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("YOU WON: $spinResult", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }

        // Spin Button Control
        Button(
            onClick = { viewModel.playLuckySpin() },
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("spin_wheel_button"),
            enabled = !isSpinning && (stats?.coins ?: 0) >= 200
        ) {
            Text("SPIN WHEEL FOR 200 COINS", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== CHALLENGES / ARENAS SELECT SCREEN ====================
@Composable
fun ChallengeSelectScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("SAMURAI ARENAS SELECTION", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Select an Arena. Each has unique atmospheric weather modifiers.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        val arenas = listOf(
            "Kyoto Temple" to "Cherry Blossom",
            "Bamboo Forest" to "Wind",
            "Snow Mountains" to "Snow",
            "Volcano Shrine" to "Sunset",
            "Ancient Dojo" to "Rain",
            "Night Sakura Garden" to "Night",
            "Demon Castle" to "Thunderstorm"
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(arenas) { arena ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.startCombat("STORY")
                            viewModel.navigateTo(Screen.COMBAT)
                        }
                        .testTag("arena_${arena.first}"),
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(arena.first, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Weather Mode: ${arena.second}", color = GoldAccent, fontSize = 11.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Select", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ==================== PRACTICE MODE SCREEN ====================
@Composable
fun PracticeScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.FitnessCenter, contentDescription = "Dojo", tint = GoldAccent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("ANCIENT DOJO TRAINING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Practice combo strikes, counter-timings, and test equipped weapons safely. Enemies do not attack back.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.startCombat("PRACTICE")
                viewModel.navigateTo(Screen.COMBAT)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
            modifier = Modifier.testTag("start_practice_btn")
        ) {
            Text("ENTER TRAINING ARENA")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.navigateTo(Screen.MAIN_MENU) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("BACK TO TEMPLE")
        }
    }
}

// ==================== DAILY LOGIN REWARD DIALOG ====================
@Composable
fun DailyLoginDialog(
    streak: Int,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowGrey),
            border = BorderStroke(1.dp, GoldAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("⛩️ DAILY SAMURAI REWARDS ⛩️", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Welcome back, Noble Samurai. Maintain your streak to earn epic gems!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // Grid of 7 days rewards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val days = listOf("D1\n500🪙", "D2\n800🪙", "D3\n1k🪙", "D4\n1.5k🪙", "D5\n2k🪙", "D6\n3k🪙", "D7\n50💎")
                    days.forEachIndexed { idx, txt ->
                        val isToday = streak == (idx + 1)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isToday) BloodRed else InkBlack, RoundedCornerShape(4.dp))
                                .border(0.5.dp, if (isToday) GoldAccent else Color.Gray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = txt,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("claim_daily_btn")
                ) {
                    Text("CLAIM TODAY'S REWARD")
                }
            }
        }
    }
}

// ==================== CONFIGURATION SETTINGS DIALOG ====================
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var volume by remember { mutableStateOf(80f) }
    var sfxVolume by remember { mutableStateOf(90f) }
    var highGraphics by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowGrey),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("SYSTEM CONFIGURATIONS", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 18.sp)

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Music slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cinematic Music", color = Color.White, fontSize = 13.sp)
                        Text("${volume.toInt()}%", color = SpiritCyan, fontSize = 12.sp)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = BloodRed, activeTrackColor = BloodRed)
                    )
                }

                // SFX slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sword Slash Impact SFX", color = Color.White, fontSize = 13.sp)
                        Text("${sfxVolume.toInt()}%", color = SpiritCyan, fontSize = 12.sp)
                    }
                    Slider(
                        value = sfxVolume,
                        onValueChange = { sfxVolume = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = BloodRed, activeTrackColor = BloodRed)
                    )
                }

                // Quality toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Unreal Engine 5 Graphics Mode", color = Color.White, fontSize = 13.sp)
                        Text("Lumen Global Illumination, 4K Ray-tracing", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = highGraphics,
                        onCheckedChange = { highGraphics = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BloodRed, checkedTrackColor = BloodRed.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONFIRM & CLOSE", color = Color.White)
                }
            }
        }
    }
}
