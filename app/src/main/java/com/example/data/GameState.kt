package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val saltPoints: Double = 0.0,
    val totalEverEarned: Double = 0.0,
    val lastSessionTimestamp: Long = System.currentTimeMillis(),
    val prestigeCount: Int = 0,
    val spicePowerMultiplier: Double = 1.0,
    val currentCityIndex: Int = 0,
    val displayName: String = "Kebap Sever",
    val isVIP: Boolean = false,
    val eventCurrency: Double = 0.0,
    val robotHandUnlocked: Boolean = false,
    val friendCode: String = "",
    val totalGiftsSentCount: Int = 0,
    val totalOfflineCollectsCount: Int = 0,
    val totalTapCount: Int = 0,
    val activeSeasonEventId: String = "ramadan_event",
    val hasSocialPackage: Boolean = false,
    val hasSeasonPass: Boolean = false,
    val hasWorldTourExpansion: Boolean = false,
    val hasParallelPackage: Boolean = false
)

@Entity(tableName = "buildings")
data class BuildingState(
    @PrimaryKey val compoundId: String = "", // format: "cityIndex_buildingId"
    val id: Int = 0, // building index 0..7
    val cityIndex: Int = 0,
    val level: Int = 0,
    val totalPurchased: Int = 0
)

@Entity(tableName = "achievements")
data class AchievementState(
    @PrimaryKey val id: String = "",
    val isUnlocked: Boolean = false,
    val progress: Double = 0.0,
    val unlockedTimestamp: Long = 0L
)

@Entity(tableName = "season_event")
data class SeasonEventState(
    @PrimaryKey val eventId: String = "",
    val isActive: Boolean = false,
    val endTimestamp: Long = 0L,
    val specialBuildingLevel: Int = 0,
    val altinTuzEarned: Double = 0.0
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey val rank: Int = 0,
    val playerName: String = "",
    val totalEarned: Double = 0.0,
    val cityReached: String = "",
    val prestigeCount: Int = 0,
    val isLocalPlayer: Boolean = false
)

@Entity(tableName = "friend_profiles")
data class FriendProfile(
    @PrimaryKey val friendCode: String = "",
    val displayName: String = "",
    val totalEarned: Double = 0.0,
    val cityIndex: Int = 0,
    val prestigeCount: Int = 0,
    val buildingSnapshot: String = "", // JSON compact snapshot of buildings
    val lastUpdated: Long = 0L,
    val lastGiftSentTimestamp: Long = 0L,
    val dailyVisitTapsUsed: Int = 0
)

@Entity(tableName = "gift_codes")
data class GiftCode(
    @PrimaryKey val code: String = "",
    val senderName: String = "",
    val saltAmount: Double = 0.0,
    val expiryTimestamp: Long = 0L,
    val isRedeemed: Boolean = false
)
