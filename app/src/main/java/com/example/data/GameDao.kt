package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // GameState
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameState?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameState(): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: GameState)

    // Buildings
    @Query("SELECT * FROM buildings")
    suspend fun getAllBuildings(): List<BuildingState>

    @Query("SELECT * FROM buildings WHERE cityIndex = :cityIndex")
    suspend fun getBuildingsForCity(cityIndex: Int): List<BuildingState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(buildings: List<BuildingState>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilding(building: BuildingState)

    // Achievements
    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementState>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementState)

    // Season Event
    @Query("SELECT * FROM season_event")
    suspend fun getAllSeasonEvents(): List<SeasonEventState>

    @Query("SELECT * FROM season_event WHERE eventId = :eventId LIMIT 1")
    suspend fun getSeasonEvent(eventId: String): SeasonEventState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasonEvent(event: SeasonEventState)

    // Leaderboard
    @Query("SELECT * FROM leaderboard ORDER BY rank ASC")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboard(entries: List<LeaderboardEntry>)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()

    // Friend Profiles
    @Query("SELECT * FROM friend_profiles ORDER BY totalEarned DESC")
    fun getAllFriendsFlow(): Flow<List<FriendProfile>>

    @Query("SELECT * FROM friend_profiles ORDER BY totalEarned DESC")
    suspend fun getAllFriends(): List<FriendProfile>

    @Query("SELECT * FROM friend_profiles WHERE friendCode = :code LIMIT 1")
    suspend fun getFriendProfile(code: String): FriendProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendProfile)

    @Query("DELETE FROM friend_profiles WHERE friendCode = :code")
    suspend fun deleteFriend(code: String)

    // Gift Codes
    @Query("SELECT * FROM gift_codes WHERE code = :code LIMIT 1")
    suspend fun getGiftCode(code: String): GiftCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiftCode(giftCode: GiftCode)
}
