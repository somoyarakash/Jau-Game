package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(private val gameDao: GameDao) {

    val playerStats: Flow<PlayerStats?> = gameDao.getPlayerStats()
    val allItems: Flow<List<InventoryItem>> = gameDao.getAllItems()
    val allSkills: Flow<List<SkillPower>> = gameDao.getAllSkills()
    val allAchievements: Flow<List<AchievementState>> = gameDao.getAllAchievements()

    /**
     * Initializes database with standard records if it is empty.
     */
    suspend fun checkAndInitializeData() {
        val stats = gameDao.getPlayerStatsSync()
        if (stats == null) {
            gameDao.insertPlayerStats(PlayerStats())
        }

        // Check and pre-populate inventory items
        val currentItems = gameDao.getAllItems().firstOrNull()
        if (currentItems.isNullOrEmpty()) {
            val defaultItems = listOf(
                // Weapons
                InventoryItem("katana_basic", "Novice Katana", "WEAPON", 0f, "ATTACK", 0, 0, true, "Standard iron sword given to rookie samurai.", "COMMON"),
                InventoryItem("muramasa", "Muramasa Blade", "WEAPON", 35f, "ATTACK", 12000, 50, false, "A cursed katana that drinks enemy blood. Massive attack power boost.", "LEGENDARY"),
                InventoryItem("raijin", "Raijin's Edge", "WEAPON", 20f, "SPEED", 8000, 30, false, "Infused with raw crackling lightning. Speeds up sword swings.", "EPIC"),
                InventoryItem("shadowfang", "Shadowfang", "WEAPON", 15f, "CRIT_DMG", 4500, 15, false, "Dark ninja dagger for precise, lethal critical cuts.", "RARE"),
                InventoryItem("masamune", "Honjo Masamune", "WEAPON", 55f, "ATTACK", 25000, 120, false, "A legendary sacred blade of absolute purity and strength.", "MYTHIC"),
                InventoryItem("kusanagi", "Kusanagi-no-Tsurugi", "WEAPON", 70f, "GOLD_BOOST", 40000, 200, false, "Imperial snake sword. Unleashes golden divine aura.", "MYTHIC"),

                // Armors
                InventoryItem("ronin_shabby", "Worn Kimono", "ARMOR", 0f, "DEFENSE", 0, 0, true, "Ragged travel clothes offering basic coverage.", "COMMON"),
                InventoryItem("shadow_yoroi", "Shadow Yoroi", "ARMOR", 15f, "DEFENSE", 5000, 20, false, "Lightweight dark armor woven with steel fibers.", "RARE"),
                InventoryItem("crimson_dragon", "Crimson Dragon Mail", "ARMOR", 25f, "HEALTH", 9500, 40, false, "Ornate heavy armor forged with real red dragon scales.", "EPIC"),
                InventoryItem("shogun_armor", "Blood Shogun Armor", "ARMOR", 40f, "DEFENSE", 18000, 80, false, "Commanding heavy armor of supreme clan commanders.", "LEGENDARY"),
                InventoryItem("celestial_robes", "Celestial Robes", "ARMOR", 50f, "GOLD_BOOST", 30000, 150, false, "Ethereal silk robes infused with spirits of the ancestors.", "MYTHIC"),

                // Pets
                InventoryItem("kitsune", "9-Tailed Kitsune", "PET", 25f, "GOLD_BOOST", 10000, 50, false, "Mythical fox companion that enhances monetary gains.", "EPIC"),
                InventoryItem("jade_dragon", "Jade Whelp", "PET", 20f, "ATTACK", 15000, 75, false, "Small flying jade dragon boosting your attack energy.", "LEGENDARY"),
                InventoryItem("spirit_wolf", "Okami Spirit", "PET", 12f, "SPEED", 6000, 25, false, "Ghostly white forest wolf leading your movements.", "RARE"),
                InventoryItem("tanuki", "Lucky Tanuki", "PET", 30f, "GOLD_BOOST", 8500, 40, false, "Chubby raccoon-dog carrying a coin sack.", "EPIC"),

                // Mounts
                InventoryItem("ebony_steed", "Ebony Stallion", "MOUNT", 15f, "HEALTH", 7000, 30, false, "A majestic jet-black warhorse with heavy iron shoeings.", "RARE"),
                InventoryItem("relic_panther", "Onxy Shadowcat", "MOUNT", 18f, "CRIT_DMG", 11000, 55, false, "A swift, terrifying black panther from the nether realms.", "EPIC"),
                InventoryItem("golden_kirin", "Golden Kirin", "MOUNT", 25f, "SPEED", 20000, 100, false, "A holy unicorn-like dragon horse glowing with divine power.", "LEGENDARY")
            )
            gameDao.insertItems(defaultItems)
        }

        // Check and pre-populate skills
        val currentSkills = gameDao.getAllSkills().firstOrNull()
        if (currentSkills.isNullOrEmpty()) {
            val defaultSkills = listOf(
                SkillPower("dragon_flame_slash", "Dragon Flame Slash", 1, 150f, 8f, 200, 300, "Unleash a fiery visual wave with your katana, burning enemies."),
                SkillPower("thunder_dash", "Thunder Dash", 1, 100f, 6f, 150, 250, "Dash forward clad in pure lightning, piercing defense and stunning."),
                SkillPower("shadow_time_freeze", "Shadow Time Freeze", 1, 50f, 15f, 300, 400, "Freeze space and time for 4s, slowing down and hacking enemies.")
            )
            gameDao.insertSkills(defaultSkills)
        }

        // Check and pre-populate achievements
        val currentAchievements = gameDao.getAllAchievements().firstOrNull()
        if (currentAchievements.isNullOrEmpty()) {
            val defaultAchievements = listOf(
                AchievementState("ach_first_blood", "First Blood", "Defeat 3 regular combat rounds", false, false, 3, 0, 500, 10),
                AchievementState("ach_boss_slayer", "Boss Executioner", "Defeat 5 terrifying level bosses", false, false, 5, 0, 2000, 25),
                AchievementState("ach_gold_hoarder", "Treasure Collector", "Earn 15,000 Free Coins", false, false, 15000, 0, 1500, 15),
                AchievementState("ach_power_up", "Ascendant Warrior", "Reach level level 10 of any stat upgrade", false, false, 10, 0, 1000, 20),
                AchievementState("ach_battle_master", "Gladiator Of Japan", "Unlock and claim level 5 on the Battle Pass", false, false, 5, 1, 1200, 30)
            )
            gameDao.insertAchievements(defaultAchievements)
        }
    }

    // ------------------ BUSINESS ACTIONS ------------------

    suspend fun savePlayerStats(stats: PlayerStats) {
        gameDao.updatePlayerStats(stats)
    }

    /**
     * Upgrades a specific player stat, checking coin/gem costs.
     */
    suspend fun upgradeStat(statName: String): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        val currentLevel = when (statName) {
            "health" -> stats.healthLevel
            "attack" -> stats.attackLevel
            "defense" -> stats.defenseLevel
            "speed" -> stats.speedLevel
            "critRate" -> stats.critRateLevel
            "critDmg" -> stats.critDamageLevel
            "energy" -> stats.energyLevel
            "stamina" -> stats.staminaLevel
            "combo" -> stats.comboLevel
            "ultimate" -> stats.ultimateLevel
            else -> 1
        }

        // Cost formula: base 150 + level * 75
        val cost = 150 + (currentLevel - 1) * 75L
        if (stats.coins < cost) return false

        val updatedStats = stats.copy(
            coins = stats.coins - cost,
            healthLevel = if (statName == "health") currentLevel + 1 else stats.healthLevel,
            attackLevel = if (statName == "attack") currentLevel + 1 else stats.attackLevel,
            defenseLevel = if (statName == "defense") currentLevel + 1 else stats.defenseLevel,
            speedLevel = if (statName == "speed") currentLevel + 1 else stats.speedLevel,
            critRateLevel = if (statName == "critRate") currentLevel + 1 else stats.critRateLevel,
            critDamageLevel = if (statName == "critDmg") currentLevel + 1 else stats.critDamageLevel,
            energyLevel = if (statName == "energy") currentLevel + 1 else stats.energyLevel,
            staminaLevel = if (statName == "stamina") currentLevel + 1 else stats.staminaLevel,
            comboLevel = if (statName == "combo") currentLevel + 1 else stats.comboLevel,
            ultimateLevel = if (statName == "ultimate") currentLevel + 1 else stats.ultimateLevel
        )
        
        // Recalculate power level
        val updatedWithPower = updatedStats.copy(
            powerLevel = recalculatePowerLevel(updatedStats)
        )
        gameDao.updatePlayerStats(updatedWithPower)

        // Track upgrade achievements
        val maxStatLvl = maxOf(
            updatedWithPower.healthLevel, updatedWithPower.attackLevel, updatedWithPower.defenseLevel,
            updatedWithPower.speedLevel, updatedWithPower.critRateLevel, updatedWithPower.critDamageLevel,
            updatedWithPower.energyLevel, updatedWithPower.staminaLevel, updatedWithPower.comboLevel,
            updatedWithPower.ultimateLevel
        )
        incrementAchievementCount("ach_power_up", maxStatLvl)
        return true
    }

    /**
     * Buys a shop item.
     */
    suspend fun buyItem(itemId: String): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        val item = gameDao.getItemById(itemId) ?: return false

        if (item.isUnlocked) return false // Already purchased

        if (stats.coins >= item.costCoins && stats.gems >= item.costGems) {
            val updatedStats = stats.copy(
                coins = stats.coins - item.costCoins,
                gems = stats.gems - item.costGems
            )
            gameDao.updatePlayerStats(updatedStats)
            gameDao.updateItem(item.copy(isUnlocked = true))
            return true
        }
        return false
    }

    /**
     * Equips a purchased item.
     */
    suspend fun equipItem(itemId: String): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        val item = gameDao.getItemById(itemId) ?: return false

        if (!item.isUnlocked) return false // Cannot equip locked item

        val updatedStats = when (item.itemType) {
            "WEAPON" -> stats.copy(equippedWeapon = itemId)
            "ARMOR" -> stats.copy(equippedArmor = itemId)
            "PET" -> stats.copy(equippedPet = itemId)
            "MOUNT" -> stats.copy(equippedMount = itemId)
            else -> stats
        }
        
        val statsWithPower = updatedStats.copy(powerLevel = recalculatePowerLevel(updatedStats))
        gameDao.updatePlayerStats(statsWithPower)
        return true
    }

    /**
     * Upgrades skill damage or cooldown level.
     */
    suspend fun upgradeSkill(skillId: String, isDamage: Boolean): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        val skillsList = gameDao.getAllSkills().firstOrNull() ?: return false
        val skill = skillsList.find { it.skillId == skillId } ?: return false

        val cost = if (isDamage) skill.damageUpgradeCost else skill.cooldownUpgradeCost
        if (stats.coins < cost) return false

        val updatedSkill = if (isDamage) {
            skill.copy(
                level = skill.level + 1,
                baseDamage = skill.baseDamage + 25f,
                damageUpgradeCost = skill.damageUpgradeCost + 150
            )
        } else {
            skill.copy(
                baseCooldown = maxOf(3f, skill.baseCooldown - 0.2f),
                cooldownUpgradeCost = skill.cooldownUpgradeCost + 200
            )
        }

        val updatedStats = stats.copy(
            coins = stats.coins - cost,
            powerLevel = stats.powerLevel + 15
        )

        gameDao.updateSkill(updatedSkill)
        gameDao.updatePlayerStats(updatedStats)
        return true
    }

    /**
     * Completes a combat round or boss round. Updates level, round, earns rewards.
     */
    suspend fun completeBattle(isBoss: Boolean, earnedCoins: Long, earnedGems: Int, earnedXp: Long) {
        val stats = gameDao.getPlayerStatsSync() ?: return

        // Gain currencies & xp
        var newXp = stats.exp + earnedXp
        var newPlayerLevel = stats.level
        var newRound = stats.round
        var newMaxXp = stats.maxExp

        // Update levels/rounds progress
        if (isBoss) {
            // Defeated Level Boss! Advance to next Level, reset round to 1.
            newPlayerLevel = minOf(100000, stats.level + 1)
            newRound = 1
            incrementAchievementCount("ach_boss_slayer", 1)
        } else {
            // Defeated regular round. Advance round (1 -> 2 -> 3 -> Boss)
            newRound = stats.round + 1
            incrementAchievementCount("ach_first_blood", 1)
        }

        // Level up if XP exceeds maxXP
        while (newXp >= newMaxXp) {
            newXp -= newMaxXp
            newMaxXp = (newMaxXp * 1.2f).toLong()
        }

        val updatedStats = stats.copy(
            level = newPlayerLevel,
            round = newRound,
            coins = stats.coins + earnedCoins,
            gems = stats.gems + earnedGems,
            exp = newXp,
            maxExp = newMaxXp,
            battlePassXp = stats.battlePassXp + 20, // Earn battle pass XP on combat
            powerLevel = recalculatePowerLevel(stats)
        )

        // Update Battle Pass tier
        val finalStats = checkBattlePassLevelUp(updatedStats)
        gameDao.updatePlayerStats(finalStats)

        // Accumulate coins earned for achievements
        incrementAchievementCount("ach_gold_hoarder", earnedCoins.toInt())
    }

    /**
     * Claims daily reward.
     */
    suspend fun claimDailyLogin(day: Int, coinsReward: Long, gemsReward: Int) {
        val stats = gameDao.getPlayerStatsSync() ?: return
        val currentStreak = if (System.currentTimeMillis() - stats.lastLoginTimestamp < 86400000 * 2) {
            stats.dailyStreak + 1
        } else {
            1
        }

        val updatedStats = stats.copy(
            coins = stats.coins + coinsReward,
            gems = stats.gems + gemsReward,
            dailyStreak = if (currentStreak > 7) 1 else currentStreak,
            lastLoginTimestamp = System.currentTimeMillis()
        )
        gameDao.updatePlayerStats(updatedStats)
    }

    /**
     * Claims achievement rewards.
     */
    suspend fun claimAchievement(achievementId: String): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        val achievements = gameDao.getAllAchievements().firstOrNull() ?: return false
        val ach = achievements.find { it.achievementId == achievementId } ?: return false

        if (ach.isCompleted && !ach.isClaimed) {
            val updatedStats = stats.copy(
                coins = stats.coins + ach.rewardCoins,
                gems = stats.gems + ach.rewardGems
            )
            gameDao.updatePlayerStats(updatedStats)
            gameDao.updateAchievement(ach.copy(isClaimed = true))
            return true
        }
        return false
    }

    /**
     * Unlocks Premium Battle Pass
     */
    suspend fun unlockPremiumBattlePass(): Boolean {
        val stats = gameDao.getPlayerStatsSync() ?: return false
        if (stats.gems < 50) return false // Costs 50 gems
        val updated = stats.copy(
            gems = stats.gems - 50,
            battlePassPremiumUnlocked = true
        )
        gameDao.updatePlayerStats(updated)
        return true
    }

    /**
     * Progress battle pass manually
     */
    suspend fun advanceBattlePassTier(amount: Int) {
        val stats = gameDao.getPlayerStatsSync() ?: return
        val updated = stats.copy(
            battlePassXp = stats.battlePassXp + amount
        )
        val finalStats = checkBattlePassLevelUp(updated)
        gameDao.updatePlayerStats(finalStats)
    }

    // ------------------ HELPER METHODS ------------------

    private fun checkBattlePassLevelUp(stats: PlayerStats): PlayerStats {
        var xp = stats.battlePassXp
        var bpLevel = stats.battlePassLevel
        val xpPerLevel = 100
        while (xp >= xpPerLevel) {
            xp -= xpPerLevel
            bpLevel = minOf(100, bpLevel + 1)
        }
        return stats.copy(
            battlePassXp = xp,
            battlePassLevel = bpLevel
        )
    }

    private suspend fun incrementAchievementCount(id: String, increment: Int) {
        val achievements = gameDao.getAllAchievements().firstOrNull() ?: return
        val ach = achievements.find { it.achievementId == id } ?: return
        if (ach.isCompleted) return

        val newCount = minOf(ach.targetCount, ach.currentCount + increment)
        val isNowCompleted = newCount >= ach.targetCount
        gameDao.updateAchievement(
            ach.copy(
                currentCount = newCount,
                isCompleted = isNowCompleted
            )
        )
    }

    /**
     * Calculates total power level based on stats upgrades + equipped items multipliers.
     */
    private fun recalculatePowerLevel(stats: PlayerStats): Long {
        val baseMultiplier = 1.0f
        val sumOfUpgradeLevels = stats.healthLevel + stats.attackLevel + stats.defenseLevel +
                stats.speedLevel + stats.critRateLevel + stats.critDamageLevel +
                stats.energyLevel + stats.staminaLevel + stats.comboLevel + stats.ultimateLevel

        // We can estimate power level dynamically
        val itemMultiplier = 1.0f // We could read all item effects, but a fast dynamic computation is easier:
        var weaponBonus = 0f
        var armorBonus = 0f
        var petBonus = 0f
        var mountBonus = 0f

        when (stats.equippedWeapon) {
            "muramasa" -> weaponBonus = 35f
            "raijin" -> weaponBonus = 20f
            "shadowfang" -> weaponBonus = 15f
            "masamune" -> weaponBonus = 55f
            "kusanagi" -> weaponBonus = 70f
        }
        when (stats.equippedArmor) {
            "shadow_yoroi" -> armorBonus = 15f
            "crimson_dragon" -> armorBonus = 25f
            "shogun_armor" -> armorBonus = 40f
            "celestial_robes" -> armorBonus = 50f
        }
        when (stats.equippedPet) {
            "kitsune" -> petBonus = 25f
            "jade_dragon" -> petBonus = 20f
            "spirit_wolf" -> petBonus = 12f
            "tanuki" -> petBonus = 30f
        }
        when (stats.equippedMount) {
            "ebony_steed" -> mountBonus = 15f
            "relic_panther" -> mountBonus = 18f
            "golden_kirin" -> mountBonus = 25f
        }

        val itemBonusTotal = weaponBonus + armorBonus + petBonus + mountBonus
        return (100L + (sumOfUpgradeLevels * 10) + itemBonusTotal.toLong())
    }
}
