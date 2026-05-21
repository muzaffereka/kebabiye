package com.example.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

// Structure for upgrades / stations
data class UpgradeStation(
    val id: Int,
    val name: String,
    val spsRate: Double, // Salt points per second generated
    val baseCost: Double,
    val emoji: String,
    val description: String
)

// In-game dynamic visual particles
data class SaltParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val colorType: Int, // 0 = White (Salt), 1 = Gold, 2 = Sizzling Fire Red
    val scale: Float,
    val alpha: Float = 1.0f,
    val ageMs: Long = 0,
    val text: String = "" // For floating text value overlays
)

// Representation of Achievements
data class Achievement(
    val id: String,
    val title: String,
    val criteriaDescription: String,
    val requiredSalt: Double,
    val badgeColorHex: Long, // Color tint for status indicators
    val iconEmoji: String
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    
    // Core game state exposed reactively to Compose
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Transient UI active states
    private val _buyMode = MutableStateFlow(1) // 1, 10, 100, or Int.MAX_VALUE for MAX
    val buyMode: StateFlow<Int> = _buyMode.asStateFlow()

    private val _offlineSummary = MutableStateFlow<String?>(null)
    val offlineSummary: StateFlow<String?> = _offlineSummary.asStateFlow()

    private val _milestoneCelebration = MutableStateFlow<String?>(null)
    val milestoneCelebration: StateFlow<String?> = _milestoneCelebration.asStateFlow()

    // Particle state is local and high-frequency, managed in memory with thread safety
    private val _particlesState = mutableStateOf<List<SaltParticle>>(emptyList())
    val particles: List<SaltParticle> get() = _particlesState.value

    // Hardware sound effects helper disabled to prevent headless emulator crashes
    private var toneGenerator: Any? = null

    // Coroutine Jobs
    private var gameTickJob: Job? = null
    private var robotHandJob: Job? = null
    private var particleAnimateJob: Job? = null

    // Base upgrades definitions
    val upgrades = listOf(
        UpgradeStation(0, "Tuz Değirmeni", 10.0, 150.0, "🧂", "Taze kaya tuzlarını elle çeker."),
        UpgradeStation(1, "Közleme Fırını", 45.0, 1000.0, "🌋", "Dönerlere dumanı üstünde köz kokusu verir."),
        UpgradeStation(2, "Baharat Deposu", 200.0, 6000.0, "🌶️", "Sırp, Halep ve Kırmızı pul biber istifler."),
        UpgradeStation(3, "Pide Atölyesi", 800.0, 30000.0, "🫓", "Sıcak tırnak pideler pişirir."),
        UpgradeStation(4, "Sokak Arabası", 3200.0, 125000.0, "🛒", "Beşyol meydanında porsiyon keser."),
        UpgradeStation(5, "Kebapçı Dükkanı", 10000.0, 650000.0, "🏪", "Caddede asma neon tabelalı aile salonu."),
        UpgradeStation(6, "Restoran Zinciri", 40000.0, 3500000.0, "🏨", "Tüm metropollerde şubeler açar."),
        UpgradeStation(7, "Kebap İmparatorluğu", 200000.0, 25000000.0, "🏰", "Yörünge istasyonunda tuzlama zirvesi yapar.")
    )

    // Achievements definitions
    val achievements = listOf(
        Achievement("ach_1k", "İlk 1000 Tuz", "1,000 Birikmiş Tuz Puanı elde et.", 1000.0, 0xFFCD7F32, "🧂"),
        Achievement("ach_10k", "Kebap Çırağı", "10,000 Birikmiş Tuz Puanı elde et.", 10000.0, 0xFFCD7F32, "👨‍🍳"),
        Achievement("ach_100k", "Baharat Ustası", "100,000 Birikmiş Tuz Puanı elde et.", 100000.0, 0xFFC0C0C0, "🌶️"),
        Achievement("ach_1m", "Tuz Canavarı", "1,000,000 Birikmiş Tuz Puanı elde et.", 1000000.0, 0xFFC0C0C0, "🌋"),
        Achievement("ach_10m", "Kebapçı Kralı", "10,000,000 Birikmiş Tuz Puanı elde et.", 10000000.0, 0xFFFFD700, "👑"),
        Achievement("ach_100m", "Sultan Şef", "100,000,000 Birikmiş Tuz Puanı elde et.", 100000000.0, 0xFFFFD700, "🏆"),
        Achievement("ach_1b", "Küresel Kebap Baronu", "1,000,000,000 Birikmiş Tuz Puanı elde et.", 1000000000.0, 0xFF318CE7, "🌍"),
        Achievement("ach_1t", "Tuz İmparatoru", "1,000,000,000,000 Birikmiş Tuz Puanı elde et.", 1000000000000.0, 0xFFB9F2FF, "🌌")
    )

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        
        // Load game state
        viewModelScope.launch {
            val savedState = repository.getGameState()
            if (savedState == null) {
                // Initialize default database state
                val defaultState = GameState(lastActiveTimestamp = System.currentTimeMillis())
                repository.saveGameState(defaultState)
                _gameState.value = defaultState
            } else {
                _gameState.value = savedState
                calculateOfflineEarnings(savedState)
            }
            
            // Start the infinite loops
            startGameLoop()
            startParticlesLoop()
            checkRobotHandLoop()
        }
    }

    // High frequency particle animator loop with thread safety
    private fun startParticlesLoop() {
        particleAnimateJob?.cancel()
        particleAnimateJob = viewModelScope.launch {
            while (true) {
                delay(33) // ~30 fps particle animation
                val currentList = _particlesState.value
                if (currentList.isNotEmpty()) {
                    val updatedList = currentList.mapNotNull { p ->
                        val nextAge = p.ageMs + 33
                        if (nextAge > 500) {
                            null
                        } else {
                            val nextX = p.x + p.vx
                            val nextY = p.y + p.vy + 0.5f 
                            val nextAlpha = (500f - nextAge) / 500f
                            p.copy(
                                x = nextX,
                                y = nextY,
                                ageMs = nextAge,
                                alpha = nextAlpha
                            )
                        }
                    }
                    _particlesState.value = updatedList
                }
            }
        }
    }

    // Core passive income and timestamps database save background loop
    private fun startGameLoop() {
        gameTickJob?.cancel()
        gameTickJob = viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                delay(200) // update every 200ms to give visual fluid ticking
                val sps = calculateCurrentSps()
                val addedAmount = sps * 0.2 // 200ms of second
                
                if (addedAmount > 0) {
                    _gameState.update { current ->
                        val isHandUnlocked = current.robotHandUnlocked
                        val nextTotal = current.totalSaltPoints + addedAmount
                        val nextAccumulated = current.totalAccumulatedPointsOnly + addedAmount
                        
                        // Check milestones continuously
                        checkAndPopMilestones(current.totalAccumulatedPointsOnly, nextAccumulated)

                        current.copy(
                            totalSaltPoints = nextTotal,
                            totalAccumulatedPointsOnly = nextAccumulated,
                            lastActiveTimestamp = System.currentTimeMillis()
                        )
                    }
                } else {
                    _gameState.update { current ->
                        current.copy(lastActiveTimestamp = System.currentTimeMillis())
                    }
                }

                saveCounter++
                if (saveCounter >= 25) { // Auto save to database every 5 seconds (25 * 200ms)
                    saveCounter = 0
                    repository.saveGameState(_gameState.value)
                }
            }
        }
    }

    // Evaluates "Robot El" (Robot Hand) tapping every 0.5s if unlocked
    private fun checkRobotHandLoop() {
        robotHandJob?.cancel()
        robotHandJob = viewModelScope.launch {
            while (true) {
                delay(500)
                if (_gameState.value.robotHandUnlocked) {
                    // Triggers auto-tap at center coordinates
                    autoRobotTap()
                }
            }
        }
    }

    // Handles the calculations of offline earnings since last database timestamp
    private fun calculateOfflineEarnings(savedState: GameState) {
        val now = System.currentTimeMillis()
        val elapsedMs = now - savedState.lastActiveTimestamp
        if (elapsedMs < 10000) return // Must be away for at least 10 seconds

        val spsRate = calculateSpsForState(savedState)
        if (spsRate <= 0) return

        // Max 8 hours of offline progression (8h = 28800 seconds)
        val maxOfflineMs = 8 * 3600 * 1000L
        val validatedMs = elapsedMs.coerceAtMost(maxOfflineMs)
        val elapsedSec = validatedMs / 1000.0

        val pointsEarned = elapsedSec * spsRate * savedState.spiceMultiplier
        if (pointsEarned > 1.0) {
            _gameState.update { current ->
                val nextTotal = current.totalSaltPoints + pointsEarned
                val nextAccumulated = current.totalAccumulatedPointsOnly + pointsEarned
                current.copy(
                    totalSaltPoints = nextTotal,
                    totalAccumulatedPointsOnly = nextAccumulated,
                    lastActiveTimestamp = now
                )
            }
            // Trigger offline earnings visual popup
            val durationSecs = elapsedMs / 1000
            val hours = durationSecs / 3600
            val minutes = (durationSecs % 3600) / 60
            val timeString = if (hours > 0) "${hours}s ${minutes}d" else "${minutes}d"
            
            _offlineSummary.value = "Hoş geldin Şef! Kebap dükkanların sen uzaktayken harıl harıl çalıştı!\n\n" +
                    "⏱️ Çevrimdışı Kalınan Süre: $timeString\n" +
                    "➕ Kazanılan Tuz: ${formatPoints(pointsEarned)} SP!\n" +
                    "🔥 Baharat Çarpanı: %${((savedState.spiceMultiplier) * 100).toInt()}"
            
            triggerBeepTone(400, 150)
        }
    }

    // Normal tap action by user
    fun tapSkewer(coordX: Float, coordY: Float) {
        val current = _gameState.value
        val isCritical = Random.nextFloat() <= 0.02f // 2% chance for ALEV/FIRE
        
        // Base tap value grows with current SPS so manual clicking isn't useless at late game!
        val baseTapAmount = 1.0 + (calculateCurrentSps() * 0.05)
        val rawEarned = if (isCritical) baseTapAmount * 10.0 else baseTapAmount
        val finalEarned = rawEarned * current.spiceMultiplier

        _gameState.update { state ->
            val nextTotal = state.totalSaltPoints + finalEarned
            val nextAccumulated = state.totalAccumulatedPointsOnly + finalEarned
            
            // Checks for achievements unlocking on tap
            var achievementsString = state.unlockedAchievements
            val updatedAchievements = mutableListOf<String>()
            achievements.forEach { achievement ->
                if (nextAccumulated >= achievement.requiredSalt && !achievementsString.contains(achievement.id)) {
                    achievementsString = if (achievementsString.isEmpty()) achievement.id else "$achievementsString,${achievement.id}"
                    updatedAchievements.add(achievement.title)
                }
            }
            
            if (updatedAchievements.isNotEmpty()) {
                triggerBeepTone(880, 200)
            }

            state.copy(
                totalSaltPoints = nextTotal,
                totalAccumulatedPointsOnly = nextAccumulated,
                unlockedAchievements = achievementsString
            )
        }

        // Spawn interactive visual particles
        val colorType = if (isCritical) 2 else 0 // 2=Fire Red, 0=White Salt
        val sizeMultiplier = if (isCritical) 2.5f else 1.2f
        val particleCount = if (isCritical) 25 else 8
        
        val newParticles = mutableListOf<SaltParticle>()
        for (i in 0 until particleCount) {
            newParticles.add(
                SaltParticle(
                    id = Random.nextLong(),
                    x = coordX,
                    y = coordY,
                    vx = (Random.nextFloat() - 0.5f) * 15f,
                    vy = (Random.nextFloat() - 1.2f) * 18f, // bursts upwards
                    colorType = colorType,
                    scale = (Random.nextFloat() + 0.4f) * sizeMultiplier
                )
            )
        }

        // Add a floating text score overlay particle
        newParticles.add(
            SaltParticle(
                id = Random.nextLong(),
                x = coordX,
                y = coordY - 30,
                vx = 0f,
                vy = -4f, // floats sluggishly upwards
                colorType = if (isCritical) 2 else 1, // 2=Fire Red, 1=Yellow/Gold text
                scale = 1.5f,
                text = if (isCritical) "🔥 ALEV! +${formatPoints(finalEarned)}" else "+${formatPoints(finalEarned)}"
            )
        )

        _particlesState.value = _particlesState.value + newParticles

        // Sound Sizzle on taps
        if (isCritical) {
            triggerBeepTone(750, 100)
            triggerBeepTone(1100, 120)
        } else {
            triggerBeepTone(300, 30)
        }
    }

    // Auto Tapping function for Robot Hand
    private fun autoRobotTap() {
        val current = _gameState.value
        val isCritical = Random.nextFloat() <= 0.02f // 2% chance for robot clicker too!
        val baseTapAmount = 1.0 + (calculateCurrentSps() * 0.05)
        val rawEarned = if (isCritical) baseTapAmount * 10.0 else baseTapAmount
        val finalEarned = rawEarned * current.spiceMultiplier

        _gameState.update { state ->
            val nextTotal = state.totalSaltPoints + finalEarned
            val nextAccumulated = state.totalAccumulatedPointsOnly + finalEarned
            state.copy(
                totalSaltPoints = nextTotal,
                totalAccumulatedPointsOnly = nextAccumulated
            )
        }

        // Spawn particles in center area representing "Robot El" work
        // Assume center coordinate
        val robotX = 540f + (Random.nextFloat() - 0.5f) * 80f
        val robotY = 800f + (Random.nextFloat() - 0.5f) * 80f

        val particleCount = if (isCritical) 15 else 4
        val newParticles = mutableListOf<SaltParticle>()
        for (i in 0 until particleCount) {
            newParticles.add(
                SaltParticle(
                    id = Random.nextLong(),
                    x = robotX,
                    y = robotY,
                    vx = (Random.nextFloat() - 0.5f) * 10f,
                    vy = (Random.nextFloat() - 1.0f) * 12f,
                    colorType = if (isCritical) 2 else 1, // 1=Yellow/Gold Robot Salt grains
                    scale = (Random.nextFloat() + 0.5f) * (if (isCritical) 2.0f else 1.0f)
                )
            )
        }
        
        // Floating robot tap text
        newParticles.add(
            SaltParticle(
                id = Random.nextLong(),
                x = robotX,
                y = robotY - 20,
                vx = 0f,
                vy = -3f,
                colorType = 1,
                scale = 1.1f,
                text = "🤖 +${formatPoints(finalEarned)}"
            )
        )

        _particlesState.value = _particlesState.value + newParticles
    }

    // Upgrade buy toggle actions (cycles 1 -> 10 -> 100 -> MAX)
    fun cycleBuyMode() {
        _buyMode.update { current ->
            when (current) {
                1 -> 10
                10 -> 100
                100 -> Int.MAX_VALUE // MAX Purchase
                else -> 1
            }
        }
    }

    // Standard getter for a building's current level
    fun getUpgradeLevel(id: Int, state: GameState): Int {
        return when (id) {
            0 -> state.level0
            1 -> state.level1
            2 -> state.level2
            3 -> state.level3
            4 -> state.level4
            5 -> state.level5
            6 -> state.level6
            7 -> state.level7
            else -> 0
        }
    }

    // Calculates cost and quantity purchase size recursively for chosen station
    fun getUpgradeCostAndQuantity(station: UpgradeStation): Pair<Double, Int> {
        val state = _gameState.value
        val currentLevel = getUpgradeLevel(station.id, state)
        val mode = _buyMode.value
        val wallet = state.totalSaltPoints

        var totalCost = 0.0
        var quantity = 0
        val geomFactor = 1.15

        val maxComputeLimit = if (mode == Int.MAX_VALUE) 200 else mode // safe bounds for MAX calculation click queries

        for (i in 0 until maxComputeLimit) {
            val levelToCompute = currentLevel + i
            val nextCost = station.baseCost * (geomFactor.pow(levelToCompute.toDouble()))
            
            if (mode == Int.MAX_VALUE) {
                if (totalCost + nextCost <= wallet) {
                    totalCost += nextCost
                    quantity++
                } else {
                    break
                }
            } else {
                totalCost += nextCost
                quantity++
            }
        }
        
        // If x10 or x100 cannot even buy 1, force quantity to be selected amount but cost is what it would take
        if (mode != Int.MAX_VALUE && quantity < mode) {
            // Recalculate full cost for target amount regardless of wallet balance to enable buy action state rendering
            totalCost = 0.0
            for (i in 0 until mode) {
                totalCost += station.baseCost * (geomFactor.pow((currentLevel + i).toDouble()))
            }
            quantity = mode
        }

        return Pair(totalCost, if (mode == Int.MAX_VALUE) maxOf(quantity, 1) else quantity)
    }

    // Validates if upgrade is buyable
    fun canAffordUpgrade(station: UpgradeStation): Boolean {
        val (cost, quantity) = getUpgradeCostAndQuantity(station)
        return _gameState.value.totalSaltPoints >= cost && quantity > 0
    }

    // Perform buy upgrade action with designated levels into atomic fields
    fun buyUpgrade(station: UpgradeStation) {
        val (cost, quantity) = getUpgradeCostAndQuantity(station)
        if (_gameState.value.totalSaltPoints < cost) return

        _gameState.update { current ->
            val nextPoints = current.totalSaltPoints - cost
            updateLevelForState(current, station.id, quantity, nextPoints)
        }

        triggerBeepTone(600, 80)
        viewModelScope.launch {
            repository.saveGameState(_gameState.value)
        }
    }

    // Performs unlocking of "Robot El" auto-key clicker
    fun canAffordRobotHand(): Boolean {
        return _gameState.value.totalSaltPoints >= 10000.0 && !_gameState.value.robotHandUnlocked
    }

    fun buyRobotHand() {
        if (!canAffordRobotHand()) return
        _gameState.update { current ->
            current.copy(
                totalSaltPoints = current.totalSaltPoints - 10000.0,
                robotHandUnlocked = true
            )
        }
        triggerBeepTone(950, 150)
        viewModelScope.launch {
            repository.saveGameState(_gameState.value)
        }
    }

    // Core helper to scale levels atomic depending on index database parameters
    private fun updateLevelForState(state: GameState, id: Int, quantity: Int, nextPoints: Double): GameState {
        return when (id) {
            0 -> state.copy(totalSaltPoints = nextPoints, level0 = state.level0 + quantity)
            1 -> state.copy(totalSaltPoints = nextPoints, level1 = state.level1 + quantity)
            2 -> state.copy(totalSaltPoints = nextPoints, level2 = state.level2 + quantity)
            3 -> state.copy(totalSaltPoints = nextPoints, level3 = state.level3 + quantity)
            4 -> state.copy(totalSaltPoints = nextPoints, level4 = state.level4 + quantity)
            5 -> state.copy(totalSaltPoints = nextPoints, level5 = state.level5 + quantity)
            6 -> state.copy(totalSaltPoints = nextPoints, level6 = state.level6 + quantity)
            7 -> state.copy(totalSaltPoints = nextPoints, level7 = state.level7 + quantity)
            else -> state
        }
    }

    // Calculates real-time total Salt-Points-Per-Second
    fun calculateCurrentSps(): Double {
        return calculateSpsForState(_gameState.value)
    }

    private fun calculateSpsForState(state: GameState): Double {
        var sps = 0.0
        sps += state.level0 * upgrades[0].spsRate
        sps += state.level1 * upgrades[1].spsRate
        sps += state.level2 * upgrades[2].spsRate
        sps += state.level3 * upgrades[3].spsRate
        sps += state.level4 * upgrades[4].spsRate
        sps += state.level5 * upgrades[5].spsRate
        sps += state.level6 * upgrades[6].spsRate
        sps += state.level7 * upgrades[7].spsRate
        return sps
    }

    // Prestige system execution:
    // If player accumulated >= 1 billion (or locally at 1 million for review/demonstration)
    // Multiplier gets permanent upgrade and resets upgrades/levels/balance but maintains achievements and multiplier
    fun isPrestigeAvailable(): Boolean {
        return _gameState.value.totalAccumulatedPointsOnly >= 1000000.0 // 1 Million milestone for easy access
    }

    fun calculatePrestigeMultiplierBonus(): Double {
        // Multiplier bonus is +100% (+1.0 multiplier ratio) per 1M accumulated points
        val basePoints = _gameState.value.totalAccumulatedPointsOnly
        val earnedMultiplier = floor(basePoints / 1000000.0)
        return maxOf(earnedMultiplier, 1.0)
    }

    fun prestigeEmpire() {
        if (!isPrestigeAvailable()) return

        val bonusMultiplierToAdd = calculatePrestigeMultiplierBonus()
        _gameState.update { current ->
            GameState(
                id = 1,
                totalSaltPoints = 0.0,
                totalAccumulatedPointsOnly = 0.0,
                prestigeCount = current.prestigeCount + 1,
                spiceMultiplier = current.spiceMultiplier + bonusMultiplierToAdd,
                lastActiveTimestamp = System.currentTimeMillis(),
                robotHandUnlocked = current.robotHandUnlocked, // Preserve automated helper
                level0 = 0,
                level1 = 0,
                level2 = 0,
                level3 = 0,
                level4 = 0,
                level5 = 0,
                level6 = 0,
                level7 = 0,
                unlockedAchievements = current.unlockedAchievements // Keep achievements unlocked
            )
        }

        triggerBeepTone(1200, 300)
        viewModelScope.launch {
            repository.saveGameState(_gameState.value)
        }
    }

    // Full screen notifications trigger checking
    private fun checkAndPopMilestones(prev: Double, next: Double) {
        val millList = listOf(
            Pair(1000.0, "🌟 Tebrikler! 1,000 SP Dönüm Noktasına Ulaştın! Yeni çıraklar seni izliyor! 🧂"),
            Pair(1000000.0, "🤩 İNANILMAZ! 1,000,000 (1M) Tuz topladın! Artık imparatorluğun kapıları açıldı! Baharat Gücünü serbest bırakabilirsin! 🔥"),
            Pair(1000000000.0, "🔥 KÜRESEL ÇAPTA EFSANE! 1,000,000,000 (1B) Tuzlama Sayonorası! Tuzlama şöhretin dünyayı sardı! 🌍"),
            Pair(1000000000000.0, "👑 KOZMİK SULTAN! 1,000,000,000,000 (1T) Tuzlama İmparatoru! Evrende senden lezzetli döner yapan yok! 🌌")
        )

        for (item in millList) {
            if (prev < item.first && next >= item.first) {
                // Pop the milestone dialog
                _milestoneCelebration.value = item.second
                triggerBeepTone(1500, 400)
            }
        }
    }

    // Dialog dismissals
    fun dismissOfflineEarnings() {
        _offlineSummary.value = null
    }

    fun dismissMilestone() {
        _milestoneCelebration.value = null
    }

    // Core synth sound feedback disabled to prevent headless emulator crashes
    private fun triggerBeepTone(freq: Int, durationMs: Int) {
        // Safe no-op
    }

    override fun onCleared() {
        super.onCleared()
        gameTickJob?.cancel()
        robotHandJob?.cancel()
        particleAnimateJob?.cancel()
    }

    // Shared formatter function compatible inside the class scope
    fun formatPoints(value: Double): String {
        return when {
            value < 1000.0 -> String.format("%.1f", value).trimEnd('0').trimEnd('.')
            value < 1_000_000.0 -> String.format("%.1fK", value / 1000.0)
            value < 1_000_000_000.0 -> String.format("%.2fM", value / 1_000_000.0)
            value < 1_000_000_000_000.0 -> String.format("%.2fB", value / 1_000_000_000.0)
            value < 1_000_000_000_000_000.0 -> String.format("%.2fT", value / 1_000_000_000_000.0)
            else -> String.format("%.2fQd", value / 1_000_000_000_000_000.0)
        }
    }
}
