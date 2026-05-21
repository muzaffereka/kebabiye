package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val gameStateFlow: Flow<GameState?> = gameDao.getGameStateFlow()

    suspend fun getGameState(): GameState? = gameDao.getGameState()

    suspend fun saveGameState(state: GameState) {
        gameDao.insertOrUpdate(state)
    }
}
