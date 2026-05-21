package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val gameStateFlow: Flow<GameState?> = gameDao.getGameStateFlow()

    suspend fun getGameState(): GameState? = gameDao.getGameState()

    suspend fun saveGameState(state: GameState) {
        gameDao.insertOrUpdate(state)
    }

    suspend fun getAllBuildings(): List<BuildingState> = gameDao.getAllBuildings()

    suspend fun getBuildingsForCity(cityIndex: Int): List<BuildingState> = gameDao.getBuildingsForCity(cityIndex)

    suspend fun saveBuildings(buildings: List<BuildingState>) {
        gameDao.insertBuildings(buildings)
    }

    suspend fun saveBuilding(building: BuildingState) {
        gameDao.insertBuilding(building)
    }

    suspend fun getAllAchievements(): List<AchievementState> = gameDao.getAllAchievements()

    suspend fun saveAchievements(achievements: List<AchievementState>) {
        gameDao.insertAchievements(achievements)
    }

    suspend fun saveAchievement(achievement: AchievementState) {
        gameDao.insertAchievement(achievement)
    }

    suspend fun getAllSeasonEvents(): List<SeasonEventState> = gameDao.getAllSeasonEvents()

    suspend fun getSeasonEvent(eventId: String): SeasonEventState? = gameDao.getSeasonEvent(eventId)

    suspend fun saveSeasonEvent(event: SeasonEventState) {
        gameDao.insertSeasonEvent(event)
    }

    suspend fun getLeaderboard(): List<LeaderboardEntry> = gameDao.getLeaderboard()

    suspend fun saveLeaderboard(entries: List<LeaderboardEntry>) {
        gameDao.insertLeaderboard(entries)
    }

    suspend fun clearLeaderboard() {
        gameDao.clearLeaderboard()
    }

    // Social Friends Flow
    val allFriendsFlow: Flow<List<FriendProfile>> = gameDao.getAllFriendsFlow()

    suspend fun getAllFriends(): List<FriendProfile> = gameDao.getAllFriends()

    suspend fun getFriendProfile(code: String): FriendProfile? = gameDao.getFriendProfile(code)

    suspend fun saveFriend(friend: FriendProfile) {
        gameDao.insertFriend(friend)
    }

    suspend fun deleteFriend(code: String) {
        gameDao.deleteFriend(code)
    }

    // Gift Codes handling
    suspend fun getGiftCode(code: String): GiftCode? = gameDao.getGiftCode(code)

    suspend fun saveGiftCode(giftCode: GiftCode) {
        gameDao.insertGiftCode(giftCode)
    }
}
