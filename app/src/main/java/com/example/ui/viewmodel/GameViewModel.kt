package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AchievementState
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.InventoryItem
import com.example.data.PlayerStats
import com.example.data.SkillPower
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen {
    MAIN_MENU,
    COMBAT,
    UPGRADES,
    STORE,
    ACHIEVEMENTS,
    BATTLE_PASS,
    LUCKY_SPIN,
    CHALLENGE_SELECT,
    PRACTICE
}

data class FloatingDamage(
    val id: Long,
    val damage: String,
    val x: Float,
    val y: Float,
    val isCrit: Boolean,
    val color: String = "WHITE" // "RED", "YELLOW", "CYAN", "PURPLE"
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository Initialization
    private val database: GameDatabase by lazy {
        Room.databaseBuilder(
            application,
            GameDatabase::class.java,
            "shin_ken_game.db"
        ).fallbackToDestructiveMigration().build()
    }

    val repository: GameRepository by lazy {
        GameRepository(database.gameDao())
    }

    // --- Core State Flows ---
    val playerStats: StateFlow<PlayerStats?> by lazy {
        repository.playerStats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    val allItems: StateFlow<List<InventoryItem>> by lazy {
        repository.allItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val allSkills: StateFlow<List<SkillPower>> by lazy {
        repository.allSkills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val allAchievements: StateFlow<List<AchievementState>> by lazy {
        repository.allAchievements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // --- UI Screen Navigation ---
    private val _currentScreen = MutableStateFlow(Screen.MAIN_MENU)
    val currentScreen = _currentScreen.asStateFlow()

    // --- Combat Mode Engine States ---
    private val _isFighting = MutableStateFlow(false)
    val isFighting = _isFighting.asStateFlow()

    private val _isBossBattle = MutableStateFlow(false)
    val isBossBattle = _isBossBattle.asStateFlow()

    private val _combatLevel = MutableStateFlow(1)
    val combatLevel = _combatLevel.asStateFlow()

    private val _combatRound = MutableStateFlow(1)
    val combatRound = _combatRound.asStateFlow()

    // Fighter health & resource values
    private val _playerHp = MutableStateFlow(1000f)
    val playerHp = _playerHp.asStateFlow()

    private val _playerMaxHp = MutableStateFlow(1000f)
    val playerMaxHp = _playerMaxHp.asStateFlow()

    private val _enemyHp = MutableStateFlow(1000f)
    val enemyHp = _enemyHp.asStateFlow()

    private val _enemyMaxHp = MutableStateFlow(1000f)
    val enemyMaxHp = _enemyMaxHp.asStateFlow()

    private val _playerEnergy = MutableStateFlow(100f)
    val playerEnergy = _playerEnergy.asStateFlow()

    private val _playerMaxEnergy = MutableStateFlow(100f)
    val playerMaxEnergy = _playerMaxEnergy.asStateFlow()

    private val _playerStamina = MutableStateFlow(100f)
    val playerStamina = _playerStamina.asStateFlow()

    private val _ultimateMeter = MutableStateFlow(0f)
    val ultimateMeter = _ultimateMeter.asStateFlow()

    private val _comboCount = MutableStateFlow(0)
    val comboCount = _comboCount.asStateFlow()

    // Weather, Arena & Environments
    private val _arenaName = MutableStateFlow("Ancient Dojo")
    val arenaName = _arenaName.asStateFlow()

    private val _weather = MutableStateFlow("Cherry Blossom")
    val weather = _weather.asStateFlow()

    private val _enemyName = MutableStateFlow("Ashigaru Spearman")
    val enemyName = _enemyName.asStateFlow()

    private val _combatLog = mutableStateListOf<String>()
    val combatLog: List<String> get() = _combatLog

    // Action/Timing States for Counters
    private val _enemyTelegraphAction = MutableStateFlow<String?>(null) // "SWING", "FIRE_BREATH", "HEAVY_STRIKE", null
    val enemyTelegraphAction = _enemyTelegraphAction.asStateFlow()

    private val _playerStateVfx = MutableStateFlow("READY") // "ATTACKING", "DODGING", "COUNTERING", "READY", "ULTIMATE"
    val playerStateVfx = _playerStateVfx.asStateFlow()

    private val _enemyStateVfx = MutableStateFlow("READY") // "BLOCKING", "STUNNED", "ATTACKING", "READY"
    val enemyStateVfx = _enemyStateVfx.asStateFlow()

    // Floating Numbers & VFX particles
    val floatingDamage = mutableStateListOf<FloatingDamage>()

    // Cool downs (seconds remaining)
    private val _power1Cooldown = MutableStateFlow(0f)
    val power1Cooldown = _power1Cooldown.asStateFlow()

    private val _power2Cooldown = MutableStateFlow(0f)
    val power2Cooldown = _power2Cooldown.asStateFlow()

    private val _power3Cooldown = MutableStateFlow(0f)
    val power3Cooldown = _power3Cooldown.asStateFlow()

    // Time Freeze Active state
    private val _isTimeFrozen = MutableStateFlow(false)
    val isTimeFrozen = _isTimeFrozen.asStateFlow()

    // Combat finish states
    private val _combatFinished = MutableStateFlow(false)
    val combatFinished = _combatFinished.asStateFlow()

    private val _combatVictory = MutableStateFlow(false)
    val combatVictory = _combatVictory.asStateFlow()

    // Current Active Mode: "STORY", "SURVIVAL", "BOSS_RUSH", "PRACTICE"
    private val _activeMode = MutableStateFlow("STORY")
    val activeMode = _activeMode.asStateFlow()

    private var combatJob: Job? = null

    // Daily Login Status
    private val _showDailyDialog = MutableStateFlow(false)
    val showDailyDialog = _showDailyDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndInitializeData()
            checkDailyLoginStatus()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen != Screen.COMBAT) {
            stopCombatSimulation()
        }
    }

    // --- Daily Login Logic ---
    private suspend fun checkDailyLoginStatus() {
        val stats = repository.playerStats.firstOrNull()
        if (stats == null) return
        val lastLogin = stats.lastLoginTimestamp
        val current = System.currentTimeMillis()

        // If last login is more than 16 hours ago, show daily login claim
        if (current - lastLogin > 16 * 60 * 60 * 1000L) {
            _showDailyDialog.value = true
        }
    }

    fun claimDailyReward(day: Int) {
        viewModelScope.launch {
            val (coins, gems) = when (day) {
                1 -> 500L to 5
                2 -> 800L to 8
                3 -> 1200L to 10
                4 -> 1500L to 12
                5 -> 2000L to 15
                6 -> 3000L to 20
                7 -> 5000L to 50
                else -> 500L to 5
            }
            repository.claimDailyLogin(day, coins, gems)
            _showDailyDialog.value = false
        }
    }

    fun closeDailyDialog() {
        _showDailyDialog.value = false
    }

    // --- Economy Transactions ---
    fun upgradePlayerStat(statName: String) {
        viewModelScope.launch {
            repository.upgradeStat(statName)
        }
    }

    fun purchaseStoreItem(itemId: String) {
        viewModelScope.launch {
            repository.buyItem(itemId)
        }
    }

    fun equipStoreItem(itemId: String) {
        viewModelScope.launch {
            repository.equipItem(itemId)
        }
    }

    fun upgradeSkillPower(skillId: String, isDamage: Boolean) {
        viewModelScope.launch {
            repository.upgradeSkill(skillId, isDamage)
        }
    }

    fun claimAchievementReward(achievementId: String) {
        viewModelScope.launch {
            repository.claimAchievement(achievementId)
        }
    }

    fun purchaseBattlePassPremium() {
        viewModelScope.launch {
            repository.unlockPremiumBattlePass()
        }
    }

    // --- Lucky Spin Wheel ---
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning = _isSpinning.asStateFlow()

    private val _spinResult = MutableStateFlow<String?>(null)
    val spinResult = _spinResult.asStateFlow()

    fun playLuckySpin() {
        if (_isSpinning.value) return
        val stats = playerStats.value ?: return
        if (stats.coins < 200) return // Costs 200 coins

        viewModelScope.launch {
            _isSpinning.value = true
            _spinResult.value = null
            
            // Deduct cost
            repository.savePlayerStats(stats.copy(coins = stats.coins - 200))

            // Spin animation delay
            delay(1500)

            val rewards = listOf(
                "300 Coins", "1000 Coins", "5 Gems", "15 Gems", "Super Elixir", "Double EXP Scroll", "Mystery Katana Box"
            )
            val index = Random.nextInt(rewards.size)
            val won = rewards[index]
            _spinResult.value = won

            // Grant actual reward
            val currentStats = repository.playerStats.firstOrNull() ?: stats
            val rewardStats = when (won) {
                "300 Coins" -> currentStats.copy(coins = currentStats.coins + 300)
                "1000 Coins" -> currentStats.copy(coins = currentStats.coins + 1000)
                "5 Gems" -> currentStats.copy(gems = currentStats.gems + 5)
                "15 Gems" -> currentStats.copy(gems = currentStats.gems + 15)
                else -> currentStats.copy(coins = currentStats.coins + 500) // Default bonus
            }
            repository.savePlayerStats(rewardStats)
            _isSpinning.value = false
        }
    }

    // --- Battle Pass Tier Advancement ---
    fun buyBattlePassTier() {
        val stats = playerStats.value ?: return
        if (stats.coins < 500) return
        viewModelScope.launch {
            repository.savePlayerStats(stats.copy(coins = stats.coins - 500))
            repository.advanceBattlePassTier(100)
        }
    }

    // --- COMBAT GAME LOOP ENGINE ---
    fun startCombat(mode: String = "STORY", customLevel: Int? = null) {
        stopCombatSimulation()
        _activeMode.value = mode

        viewModelScope.launch {
            val stats = repository.playerStats.firstOrNull() ?: PlayerStats()
            val currentLvl = customLevel ?: stats.level
            val round = if (mode == "STORY") stats.round else 1

            _combatLevel.value = currentLvl
            _combatRound.value = round

            // Check if level is Boss Battle (every 3rd round is a Boss? Wait, the prompt: "Every Level contains 3 Rounds. After every 3rd Round, a Boss Battle appears.")
            // Level 1: Round 1, Round 2, Round 3 -> Boss 1.
            // Level 2: Round 4, Round 5, Round 6 -> Boss 2.
            // This means we are facing regular enemies in rounds 1, 2, 3, and a Boss Battle after Round 3 of that Level!
            val isBoss = (round == 4) || (mode == "BOSS_RUSH")
            _isBossBattle.value = isBoss

            // 1. Determine Enemy attributes based on level scaling
            val difficultyMultiplier = 1.0f + (currentLvl - 1) * 0.15f + (if (isBoss) 2.0f else 0.0f)
            val enemyBaseHp = 400f * difficultyMultiplier
            val enemyBaseAttack = 20f * difficultyMultiplier

            // Enemy details
            if (isBoss) {
                val bosses = listOf("Oni King", "Shadow Samurai", "Demon Monk", "Fire Dragon Warrior", "Thunder Beast", "Dark Ninja Emperor", "Ghost General", "Blood Shogun", "Inferno Titan", "Celestial Demon")
                _enemyName.value = bosses[(currentLvl - 1) % bosses.size] + " (Lvl $currentLvl Boss)"
                _enemyMaxHp.value = enemyBaseHp * 2.0f
                _enemyHp.value = enemyBaseHp * 2.0f
            } else {
                val normalEnemies = listOf("Ashigaru Spearman", "Sengoku Bandit", "Rogue Ronin", "Yari Samurai", "Kabuki Assassin", "Shadow Scout")
                _enemyName.value = normalEnemies[Random.nextInt(normalEnemies.size)] + " [Round $round]"
                _enemyMaxHp.value = enemyBaseHp
                _enemyHp.value = enemyBaseHp
            }

            // 2. Set Player Attributes from upgrades & equipment
            val playerBaseHp = 500f + (stats.healthLevel - 1) * 75f
            // Apply armor boost
            var armorBonus = 0f
            when (stats.equippedArmor) {
                "shadow_yoroi" -> armorBonus = 0.15f
                "crimson_dragon" -> armorBonus = 0.25f
                "shogun_armor" -> armorBonus = 0.40f
                "celestial_robes" -> armorBonus = 0.50f
            }
            val finalPlayerHp = playerBaseHp * (1.0f + armorBonus)
            _playerMaxHp.value = finalPlayerHp
            _playerHp.value = finalPlayerHp

            _playerMaxEnergy.value = 100f + (stats.energyLevel - 1) * 10f
            _playerEnergy.value = _playerMaxEnergy.value

            _playerStamina.value = 100f

            // Cooldowns reset
            _power1Cooldown.value = 0f
            _power2Cooldown.value = 0f
            _power3Cooldown.value = 0f
            _isTimeFrozen.value = false
            _ultimateMeter.value = 0f
            _comboCount.value = 0

            // Choose scenic Japanese Arena
            val arenas = listOf("Kyoto Temple", "Samurai Village", "Bamboo Forest", "Japanese Castle", "Snow Mountains", "Volcano Shrine", "Night Sakura Garden", "Ancient Dojo", "Burning Temple", "Floating Japanese Island", "Demon Castle", "Moonlit Pagoda")
            _arenaName.value = if (isBoss) "Volcano Shrine" else arenas[(currentLvl - 1) % arenas.size]

            // Choose Dynamic Weather
            val weatherOptions = listOf("Rain", "Snow", "Wind", "Thunderstorm", "Fog", "Cherry Blossom", "Sunset", "Night")
            _weather.value = weatherOptions[Random.nextInt(weatherOptions.size)]

            // Clear logs
            _combatLog.clear()
            floatingDamage.clear()
            _combatLog.add("⛩️ Entered ${_arenaName.value} under a raging ${_weather.value}! ⛩️")
            _combatLog.add("⚔️ Facing off against ${_enemyName.value}! GET READY! ⚔️")

            // Activate visual state
            _playerStateVfx.value = "READY"
            _enemyStateVfx.value = "READY"
            _enemyTelegraphAction.value = null
            _combatFinished.value = false
            _combatVictory.value = false

            _isFighting.value = true

            // Launch active Battle Loop
            runCombatSimulationLoop(enemyBaseAttack)
        }
    }

    private fun runCombatSimulationLoop(enemyAttack: Float) {
        combatJob = viewModelScope.launch {
            var tick = 0
            while (_isFighting.value && _playerHp.value > 0 && _enemyHp.value > 0) {
                delay(100) // 100ms game engine ticks
                tick++

                // Handle cooldown tickdowns every 1s
                if (tick % 10 == 0) {
                    if (_power1Cooldown.value > 0) _power1Cooldown.value = maxOf(0f, _power1Cooldown.value - 1f)
                    if (_power2Cooldown.value > 0) _power2Cooldown.value = maxOf(0f, _power2Cooldown.value - 1f)
                    if (_power3Cooldown.value > 0) _power3Cooldown.value = maxOf(0f, _power3Cooldown.value - 1f)

                    // Passive Stamina & Energy Regen (modified by Pets)
                    val kitsuneActive = playerStats.value?.equippedPet == "kitsune"
                    val energyRegen = 5f + (if (kitsuneActive) 10f else 0f)
                    _playerEnergy.value = minOf(_playerMaxEnergy.value, _playerEnergy.value + energyRegen)
                    _playerStamina.value = minOf(100f, _playerStamina.value + 15f)
                }

                // If Time Freeze is active, the enemy is completely paralyzed!
                if (_isTimeFrozen.value) {
                    _enemyStateVfx.value = "FROZEN"
                    continue
                }

                // Random AI Decisions every 2.5 seconds (25 ticks)
                if (tick % 25 == 0 && _playerStateVfx.value != "DODGING" && _enemyStateVfx.value != "STUNNED") {
                    val decision = Random.nextFloat()
                    if (decision < 0.45f) {
                        // Enemy winds up a powerful attack! Give player reaction window!
                        val moves = listOf("SWING", "FIRE_BREATH", "HEAVY_STRIKE")
                        val telegraphed = moves[Random.nextInt(moves.size)]
                        _enemyTelegraphAction.value = telegraphed
                        _enemyStateVfx.value = "ATTACKING"
                        _combatLog.add("⚠️ ${_enemyName.value} telegraphs a ${telegraphed.replace("_", " ")}! (DODGE / COUNTER!)")
                    } else if (decision < 0.70f) {
                        // Enemy goes into standard defensive blocking posture
                        _enemyStateVfx.value = "BLOCKING"
                        delay(1200)
                        if (_enemyStateVfx.value == "BLOCKING") _enemyStateVfx.value = "READY"
                    } else {
                        // Standard quick punch landing directly if player isn't dodging!
                        landEnemyQuickAttack(enemyAttack)
                    }
                }

                // Handle Telegraph windows: if enemy telegraphed, they strike after 12 ticks (1.2 seconds)
                if (_enemyTelegraphAction.value != null && tick % 12 == 0) {
                    executeTelegraphedEnemyStrike(enemyAttack)
                }
            }

            // Game over check
            if (_playerHp.value <= 0) {
                executeDefeatSequence()
            } else if (_enemyHp.value <= 0) {
                executeVictorySequence()
            }
        }
    }

    private fun executeTelegraphedEnemyStrike(enemyAttack: Float) {
        val action = _enemyTelegraphAction.value ?: return
        _enemyTelegraphAction.value = null // window closed

        // Check if player evaded
        if (_playerStateVfx.value == "DODGING") {
            _combatLog.add("💨 You fully evaded the ${_enemyName.value}'s $action!")
            _comboCount.value++
            triggerFloatingNumber("EVADE!", 200f, 150f, false, "CYAN")
            _playerStateVfx.value = "READY"
            return
        }

        // Check if player countered successfully (handled separately when user presses counter)
        // If not evaded, deal heavy telegraphed damage!
        val rawDamage = enemyAttack * 1.5f
        // Apply player upgrades defense reduction
        val stats = playerStats.value ?: PlayerStats()
        val defenseMultiplier = 1.0f / (1.0f + (stats.defenseLevel - 1) * 0.08f)
        val finalDamage = rawDamage * defenseMultiplier

        _playerHp.value = maxOf(0f, _playerHp.value - finalDamage)
        _comboCount.value = 0 // Break combo
        _combatLog.add("💥 CRITICAL HIT! ${_enemyName.value} landed the $action dealing ${finalDamage.toInt()} damage!")
        triggerFloatingNumber("-${finalDamage.toInt()}", 100f, 200f, true, "RED")
        _enemyStateVfx.value = "READY"
    }

    private fun landEnemyQuickAttack(enemyAttack: Float) {
        if (_playerStateVfx.value == "DODGING") return

        val stats = playerStats.value ?: PlayerStats()
        val defenseMultiplier = 1.0f / (1.0f + (stats.defenseLevel - 1) * 0.08f)
        val finalDamage = enemyAttack * defenseMultiplier

        _playerHp.value = maxOf(0f, _playerHp.value - finalDamage)
        _comboCount.value = 0
        _combatLog.add("🥋 ${_enemyName.value} lands a quick punch dealing ${finalDamage.toInt()} damage.")
        triggerFloatingNumber("-${finalDamage.toInt()}", 100f, 210f, false, "RED")
    }

    // --- PLAYER ACTION MOVES ---

    fun performPunch() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 10) {
            _combatLog.add("⚡ Out of stamina! Let it regenerate!")
            return
        }

        viewModelScope.launch {
            _playerStamina.value -= 10f
            _playerStateVfx.value = "ATTACKING"

            // Calculate weapon/stats damage
            val baseDmg = calculatePlayerBaseDamage()
            var damage = baseDmg * 0.8f

            // Check if enemy is blocking
            var msg = ""
            if (_enemyStateVfx.value == "BLOCKING") {
                damage *= 0.2f
                msg = "🛡️ ${_enemyName.value} BLOCKED! Damage reduced."
                _enemyStateVfx.value = "READY"
            } else {
                msg = "👊 You land a precision strike!"
            }

            // Critical hit calculation
            val (isCrit, critDamage) = rollCriticalChance(damage)
            applyDamageToEnemy(critDamage, isCrit)

            _combatLog.add("$msg Dealt ${critDamage.toInt()} damage.")
            delay(200)
            _playerStateVfx.value = "READY"
        }
    }

    fun performKick() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 18) {
            _combatLog.add("⚡ Not enough stamina for a sweeping kick!")
            return
        }

        viewModelScope.launch {
            _playerStamina.value -= 18f
            _playerStateVfx.value = "ATTACKING"

            val baseDmg = calculatePlayerBaseDamage()
            var damage = baseDmg * 1.3f

            var stunned = false
            if (_enemyStateVfx.value == "BLOCKING") {
                // Kick partially breaks blocks
                damage *= 0.5f
                _combatLog.add("🥋 Sweeping Kick partially broke the enemy block!")
                _enemyStateVfx.value = "READY"
            } else {
                // 25% chance to stun regular enemy, 10% for boss
                val stunChance = if (_isBossBattle.value) 0.10f else 0.25f
                if (Random.nextFloat() < stunChance) {
                    stunned = true
                }
            }

            val (isCrit, critDamage) = rollCriticalChance(damage)
            applyDamageToEnemy(critDamage, isCrit)

            if (stunned) {
                _enemyStateVfx.value = "STUNNED"
                _combatLog.add("💫 STUNNED! ${_enemyName.value} is dazed by your impact!")
                triggerFloatingNumber("STUNNED!", 350f, 120f, true, "YELLOW")
                // Auto reset stun after 2.5s
                launch {
                    delay(2500)
                    if (_enemyStateVfx.value == "STUNNED") _enemyStateVfx.value = "READY"
                }
            } else {
                _combatLog.add("👞 Sweeping Kick landed for ${critDamage.toInt()} damage.")
            }

            delay(300)
            _playerStateVfx.value = "READY"
        }
    }

    fun performDodge() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 5) return

        viewModelScope.launch {
            _playerStamina.value -= 5f
            _playerStateVfx.value = "DODGING"
            _combatLog.add("💨 Fast Ninja Dodge! Temporarily invulnerable!")
            delay(800)
            _playerStateVfx.value = "READY"
        }
    }

    fun performRoll() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 12) return

        viewModelScope.launch {
            _playerStamina.value -= 12f
            _playerStateVfx.value = "DODGING"
            _combatLog.add("🌀 Combat Roll out of danger!")
            delay(1000)
            _playerStateVfx.value = "READY"
        }
    }

    fun performGrab() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 25) {
            _combatLog.add("⚡ Stamina too low to execute a throw grab!")
            return
        }

        viewModelScope.launch {
            _playerStamina.value -= 25f
            _playerStateVfx.value = "ATTACKING"

            // Grab ignores blocks entirely!
            val baseDmg = calculatePlayerBaseDamage()
            val damage = baseDmg * 1.1f

            val (isCrit, critDamage) = rollCriticalChance(damage)
            applyDamageToEnemy(critDamage, isCrit)

            _combatLog.add("🤼 GRAB COMPLETED! Bypassed armor and executed a devastating back-throw for ${critDamage.toInt()} damage!")
            _enemyStateVfx.value = "READY" // Breaks any block state
            delay(400)
            _playerStateVfx.value = "READY"
        }
    }

    fun performJumpAttack() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_playerStamina.value < 30) {
            _combatLog.add("⚡ Need more stamina to jump attack!")
            return
        }

        viewModelScope.launch {
            _playerStamina.value -= 30f
            _playerStateVfx.value = "ATTACKING"

            val baseDmg = calculatePlayerBaseDamage()
            // High risk, high reward: breaks blocks
            var damage = baseDmg * 1.6f
            if (_enemyStateVfx.value == "BLOCKING") {
                _combatLog.add("🛡️ Jump Attack fully CRUSHED the opponent's block defense!")
                _enemyStateVfx.value = "READY"
            }

            val (isCrit, critDamage) = rollCriticalChance(damage)
            applyDamageToEnemy(critDamage, isCrit)

            _combatLog.add("🦅 Air-dive strike smashed ${_enemyName.value} for ${critDamage.toInt()} damage!")
            delay(500)
            _playerStateVfx.value = "READY"
        }
    }

    fun performCounter() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return

        viewModelScope.launch {
            _playerStateVfx.value = "COUNTERING"

            // If the enemy has an active wind-up / telegraph, we parry!
            val activeTelegraph = _enemyTelegraphAction.value
            if (activeTelegraph != null) {
                _enemyTelegraphAction.value = null // consumed
                val stats = playerStats.value ?: PlayerStats()
                val parryDmg = calculatePlayerBaseDamage() * 2.5f

                _combatLog.add("⚔️ PERFECT PARRY! You deflected $activeTelegraph and slashed back for ${parryDmg.toInt()} damage!")
                applyDamageToEnemy(parryDmg, true)
                _enemyStateVfx.value = "STUNNED"
                _comboCount.value += 2

                triggerFloatingNumber("PERFECT PARRY!", 220f, 100f, true, "CYAN")

                launch {
                    delay(2000)
                    if (_enemyStateVfx.value == "STUNNED") _enemyStateVfx.value = "READY"
                }
            } else {
                _combatLog.add("🛡️ Missed counter timing! Left exposed.")
                _playerStateVfx.value = "READY"
                // Deduct stamina as penalty
                _playerStamina.value = maxOf(0f, _playerStamina.value - 15f)
            }
        }
    }

    // --- EQUIPPED POWER MOVES (3 SLOTS) ---

    fun castPower1() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_power1Cooldown.value > 0) return
        if (_playerEnergy.value < 30f) {
            _combatLog.add("🔥 Not enough spirit energy for Dragon Flame Slash!")
            return
        }

        viewModelScope.launch {
            _playerEnergy.value -= 30f
            // Get power level from skill db if available, otherwise default base 150
            val skill = allSkills.value.find { it.skillId == "dragon_flame_slash" }
            val baseDmg = skill?.baseDamage ?: 150f
            val cooldown = skill?.baseCooldown ?: 8f

            _power1Cooldown.value = cooldown
            _playerStateVfx.value = "ATTACKING"

            val finalDmg = baseDmg + (calculatePlayerBaseDamage() * 0.5f)
            _combatLog.add("🔥 DRAGON FLAME SLASH! Giant shockwave of sacred dragon flames burns ${_enemyName.value}!")
            applyDamageToEnemy(finalDmg, true)

            triggerFloatingNumber("FLAME SLASH!", 250f, 140f, true, "RED")

            delay(400)
            _playerStateVfx.value = "READY"
        }
    }

    fun castPower2() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_power2Cooldown.value > 0) return
        if (_playerEnergy.value < 25f) {
            _combatLog.add("⚡ Spirit Energy too low for Thunder Dash!")
            return
        }

        viewModelScope.launch {
            _playerEnergy.value -= 25f
            val skill = allSkills.value.find { it.skillId == "thunder_dash" }
            val baseDmg = skill?.baseDamage ?: 100f
            val cooldown = skill?.baseCooldown ?: 6f

            _power2Cooldown.value = cooldown
            _playerStateVfx.value = "ATTACKING"

            val finalDmg = baseDmg + (calculatePlayerBaseDamage() * 0.3f)
            _combatLog.add("⚡ THUNDER DASH! Pierced right through defense with electric lightning speed!")
            applyDamageToEnemy(finalDmg, false)

            _enemyStateVfx.value = "STUNNED"
            triggerFloatingNumber("LIGHTNING DASH!", 200f, 120f, false, "YELLOW")

            launch {
                delay(1500)
                if (_enemyStateVfx.value == "STUNNED") _enemyStateVfx.value = "READY"
            }

            delay(300)
            _playerStateVfx.value = "READY"
        }
    }

    fun castPower3() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_power3Cooldown.value > 0) return
        if (_playerEnergy.value < 40f) {
            _combatLog.add("🌀 Shadow Time Freeze needs more spirit energy!")
            return
        }

        viewModelScope.launch {
            _playerEnergy.value -= 40f
            val skill = allSkills.value.find { it.skillId == "shadow_time_freeze" }
            val cooldown = skill?.baseCooldown ?: 15f

            _power3Cooldown.value = cooldown
            _isTimeFrozen.value = true
            _combatLog.add("❄️ SHADOW TIME FREEZE! The surrounding world slows down into absolute silence for 4 seconds!")

            triggerFloatingNumber("TIME FROZEN!", 300f, 80f, true, "CYAN")

            delay(4000)
            _isTimeFrozen.value = false
            _enemyStateVfx.value = "READY"
            _combatLog.add("❄️ Time flow returns to normal.")
        }
    }

    // --- ULTIMATE FATAL STRIKE ---

    fun castUltimate() {
        if (!_isFighting.value || _playerHp.value <= 0 || _combatFinished.value) return
        if (_ultimateMeter.value < 100f) {
            _combatLog.add("🔥 Ultimate meter is not fully charged!")
            return
        }

        viewModelScope.launch {
            _ultimateMeter.value = 0f
            _playerStateVfx.value = "ULTIMATE"
            _enemyStateVfx.value = "STUNNED"

            _combatLog.add("🗡️ SHIN-KEN SECRET TECHNIQUE: 10,000 SOUL SLASH! Cinematic slow-motion cuts shred the screen!")

            // Massive damage
            val stats = playerStats.value ?: PlayerStats()
            val baseDmg = calculatePlayerBaseDamage()
            val ultimateDmg = (baseDmg * 5.0f) + (stats.ultimateLevel * 100f)

            delay(1200) // Cinematic slow-mo visual delay
            applyDamageToEnemy(ultimateDmg, true)
            triggerFloatingNumber("10,000 SOUL SLASH!", 280f, 150f, true, "PURPLE")

            _playerStateVfx.value = "READY"
            _enemyStateVfx.value = "READY"
        }
    }

    // --- INTERNAL MATHEMATICS & CONSEQUENCES ---

    private fun calculatePlayerBaseDamage(): Float {
        val stats = playerStats.value ?: return 30f
        val levelAttack = 30f + (stats.attackLevel - 1) * 10f

        // Apply weapon boost
        var weaponBonus = 0f
        when (stats.equippedWeapon) {
            "muramasa" -> weaponBonus = 0.35f
            "raijin" -> weaponBonus = 0.20f
            "shadowfang" -> weaponBonus = 0.15f
            "masamune" -> weaponBonus = 0.55f
            "kusanagi" -> weaponBonus = 0.70f
        }
        return levelAttack * (1.0f + weaponBonus)
    }

    private fun rollCriticalChance(rawDamage: Float): Pair<Boolean, Float> {
        val stats = playerStats.value ?: return false to rawDamage
        val critRate = 0.05f + (stats.critRateLevel - 1) * 0.015f
        val critMultiplier = 1.5f + (stats.critDamageLevel - 1) * 0.1f

        return if (Random.nextFloat() < critRate) {
            true to (rawDamage * critMultiplier)
        } else {
            false to rawDamage
        }
    }

    private fun applyDamageToEnemy(dmg: Float, isCrit: Boolean) {
        _enemyHp.value = maxOf(0f, _enemyHp.value - dmg)
        _comboCount.value++

        // Grant Ultimate Charge (base 3% per punch, up to 8% if crit)
        val stats = playerStats.value ?: PlayerStats()
        val bonusMult = 1.0f + (stats.ultimateLevel - 1) * 0.05f
        val charge = (if (isCrit) 10f else 6f) * bonusMult
        _ultimateMeter.value = minOf(100f, _ultimateMeter.value + charge)

        // Floating Damage placement
        val xRandom = 100f + Random.nextFloat() * 250f
        val yRandom = 80f + Random.nextFloat() * 100f
        val color = if (isCrit) "YELLOW" else "WHITE"
        triggerFloatingNumber("${dmg.toInt()}", xRandom, yRandom, isCrit, color)
    }

    private fun triggerFloatingNumber(text: String, x: Float, y: Float, isCrit: Boolean, color: String) {
        val f = FloatingDamage(System.currentTimeMillis() + Random.nextInt(1000), text, x, y, isCrit, color)
        floatingDamage.add(f)
        viewModelScope.launch {
            delay(1200)
            floatingDamage.remove(f)
        }
    }

    private fun executeDefeatSequence() {
        stopCombatSimulation()
        _combatFinished.value = true
        _combatVictory.value = false
        _combatLog.add("💀 DEFEATED! The enemy dealt a lethal slash. Train your stats and equip better weapons in Kyoto Temple!")
    }

    private suspend fun executeVictorySequence() {
        stopCombatSimulation()
        _combatFinished.value = true
        _combatVictory.value = true

        val stats = playerStats.value ?: PlayerStats()
        val isBoss = _isBossBattle.value

        // Rewards scale with level
        val baseCoins = if (isBoss) 300L else 80L
        val baseGems = if (isBoss) 5 else 1
        val baseExp = if (isBoss) 200L else 50L

        val scale = 1.0f + (_combatLevel.value - 1) * 0.12f
        
        // Pet multipliers (Lucky Tanuki gives +30% gold boost, Celestial Robes etc)
        var goldMult = 1.0f
        if (stats.equippedPet == "tanuki" || stats.equippedPet == "kitsune") goldMult += 0.30f
        if (stats.equippedArmor == "celestial_robes") goldMult += 0.20f

        val finalCoins = (baseCoins * scale * goldMult).toLong()
        val finalGems = (baseGems * scale).toInt()
        val finalExp = (baseExp * scale).toLong()

        _combatLog.add("🏆 VICTORY! Earned $finalCoins Coins, $finalGems Gems, and $finalExp EXP!")

        // Save in Room Database
        repository.completeBattle(isBoss, finalCoins, finalGems, finalExp)
    }

    fun stopCombatSimulation() {
        _isFighting.value = false
        combatJob?.cancel()
        combatJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCombatSimulation()
    }
}
