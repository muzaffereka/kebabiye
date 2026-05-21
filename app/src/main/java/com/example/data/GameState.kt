package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val totalSaltPoints: Double = 0.0,
    val totalAccumulatedPointsOnly: Double = 0.0, // Used for achievements and prestige target checking
    val prestigeCount: Int = 0,
    val spiceMultiplier: Double = 1.0,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val robotHandUnlocked: Boolean = false,
    
    // Levels of the 8 business upgrades
    val level0: Int = 0, // Tuz Değirmeni
    val level1: Int = 0, // Közleme Fırını
    val level2: Int = 0, // Baharat Deposu
    val level3: Int = 0, // Pide Atölyesi
    val level4: Int = 0, // Sokak Arabası
    val level5: Int = 0, // Kebapçı Dükkanı
    val level6: Int = 0, // Restoran Zinciri
    val level7: Int = 0, // Kebap İmparatorluğu
    
    // Unlocked achievements: comma-separated list of IDs (e.g., "1_1000,1_1000000")
    val unlockedAchievements: String = ""
)
