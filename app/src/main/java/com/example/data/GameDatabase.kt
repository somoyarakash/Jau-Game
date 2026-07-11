package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ------------------ ENTITIES ------------------

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,              // Up to 100,000
    val round: Int = 1,              // 1, 2, or 3
    val coins: Long = 1500,          // Starting coins
    val gems: Int = 50,              // Starting gems
    val exp: Long = 0,
    val maxExp: Long = 100,
    val powerLevel: Long = 120,
    
    // Upgrades Levels
    val healthLevel: Int = 1,
    val attackLevel: Int = 1,
    val defenseLevel: Int = 1,
    val speedLevel: Int = 1,
    val critRateLevel: Int = 1,
    val critDamageLevel: Int = 1,
    val energyLevel: Int = 1,
    val staminaLevel: Int = 1,
    val comboLevel: Int = 1,
    val ultimateLevel: Int = 1,
    
    // Equipment
    val equippedWeapon: String = "katana_basic",
    val equippedArmor: String = "ronin_shabby",
    val equippedPet: String = "none",
    val equippedMount: String = "none",
    
    // Skills Equipped
    val power1: String = "Dragon Flame Slash",
    val power2: String = "Thunder Dash",
    val power3: String = "Shadow Time Freeze",
    
    // Economy and Engagement
    val dailyStreak: Int = 1,
    val lastLoginTimestamp: Long = 0L,
    val battlePassXp: Int = 0,
    val battlePassLevel: Int = 1,
    val battlePassPremiumUnlocked: Boolean = false
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val itemId: String,
    val name: String,
    val itemType: String,            // "WEAPON", "ARMOR", "PET", "MOUNT"
    val statBoostPercent: Float,     // e.g. 15.0f for +15%
    val statType: String,            // "ATTACK", "DEFENSE", "HEALTH", "SPEED", "CRIT_DMG", "GOLD_BOOST"
    val costCoins: Long,
    val costGems: Int,
    val isUnlocked: Boolean = false,
    val description: String,
    val rarity: String = "COMMON"    // "COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"
)

@Entity(tableName = "skill_powers")
data class SkillPower(
    @PrimaryKey val skillId: String,
    val name: String,
    val level: Int = 1,
    val baseDamage: Float,
    val baseCooldown: Float,
    val damageUpgradeCost: Long = 200,
    val cooldownUpgradeCost: Long = 300,
    val description: String
)

@Entity(tableName = "achievements")
data class AchievementState(
    @PrimaryKey val achievementId: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val targetCount: Int,
    val currentCount: Int = 0,
    val rewardCoins: Long = 500,
    val rewardGems: Int = 10
)

// ------------------ DAO ------------------

@Dao
interface GameDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStats)

    @Update
    suspend fun updatePlayerStats(stats: PlayerStats)

    // Items
    @Query("SELECT * FROM inventory_items")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE itemId = :itemId")
    suspend fun getItemById(itemId: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    // Skills
    @Query("SELECT * FROM skill_powers")
    fun getAllSkills(): Flow<List<SkillPower>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillPower>)

    @Update
    suspend fun updateSkill(skill: SkillPower)

    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementState>)

    @Update
    suspend fun updateAchievement(achievement: AchievementState)
}

// ------------------ DATABASE ------------------

@Database(
    entities = [
        PlayerStats::class,
        InventoryItem::class,
        SkillPower::class,
        AchievementState::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
