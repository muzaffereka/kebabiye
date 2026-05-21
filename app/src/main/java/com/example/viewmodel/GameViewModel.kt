package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

// Structure for upgrades / stations definitions in memory
data class UpgradeStation(
    val id: Int,
    val name: String,
    val baseSpsRate: Double,
    val baseCost: Double,
    val emoji: String,
    val description: String
)

// Dynamic visual particles
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
    val text: String = "" // Floating text values
)

// Achievements config data class (30 total)
data class Achievement(
    val id: String,
    val category: String, // "tuz", "insaat", "sehir", "prestige", "ozel"
    val title: String,
    val criteriaDescription: String,
    val targetValue: Double,
    val iconEmoji: String
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    // Core state
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Loaded buildings map: key is "cityIndex_buildingId" -> level status
    private val _buildingsMap = MutableStateFlow<Map<String, BuildingState>>(emptyMap())
    val buildingsMap: StateFlow<Map<String, BuildingState>> = _buildingsMap.asStateFlow()

    // Loaded achievements table
    private val _achievementsList = MutableStateFlow<List<AchievementState>>(emptyList())
    val achievementsList: StateFlow<List<AchievementState>> = _achievementsList.asStateFlow()

    // Active achievements pop-up event
    private val _achievementCelebration = MutableStateFlow<String?>(null)
    val achievementCelebration: StateFlow<String?> = _achievementCelebration.asStateFlow()

    // General popup/notif celebrating progress
    private val _milestoneCelebration = MutableStateFlow<String?>(null)
    val milestoneCelebration: StateFlow<String?> = _milestoneCelebration.asStateFlow()

    // Leaderboard
    private val _leaderboardList = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardList: StateFlow<List<LeaderboardEntry>> = _leaderboardList.asStateFlow()

    // Active Ramadan Season Event details
    private val _ramadaEventState = MutableStateFlow<SeasonEventState?>(null)
    val ramadanEventState: StateFlow<SeasonEventState?> = _ramadaEventState.asStateFlow()

    // Offline-capable Social state parameters
    private val _friendsState = MutableStateFlow<List<FriendProfile>>(emptyList())
    val friendsState: StateFlow<List<FriendProfile>> = _friendsState.asStateFlow()

    val visitingFriend = MutableStateFlow<FriendProfile?>(null)
    val lastGeneratedGiftCode = MutableStateFlow<String?>(null)

    private val _seasonEventsList = MutableStateFlow<List<SeasonEventState>>(emptyList())
    val seasonEventsList: StateFlow<List<SeasonEventState>> = _seasonEventsList.asStateFlow()

    // Buy quantity modes: 1, 10, 100, or Int.MAX_VALUE for MAX
    private val _buyMode = MutableStateFlow(1)
    val buyMode: StateFlow<Int> = _buyMode.asStateFlow()

    private val _offlineSummary = MutableStateFlow<String?>(null)
    val offlineSummary: StateFlow<String?> = _offlineSummary.asStateFlow()

    // Particles state
    private val _particlesState = mutableStateOf<List<SaltParticle>>(emptyList())
    val particles: List<SaltParticle> get() = _particlesState.value

    // Dev Mode settings (grants free IAPs and speeds up testing)
    val isDevModeEnabled = mutableStateOf(false)
    val soundEnabled = mutableStateOf(true)

    // Cooldown Debounces
    private val lastTapTimestamp = AtomicLong(0L)
    
    // Coroutine Jobs
    private var gameTickJob: Job? = null
    private var robotHandJob: Job? = null
    private var particleAnimateJob: Job? = null
    private var achievementsMonitorJob: Job? = null

    // Base upgrade statistics for Istanbul (City index 0)
    val baseCosts = doubleArrayOf(10.0, 100.0, 1100.0, 12000.0, 130000.0, 1400000.0, 20000000.0, 330000000.0)
    val baseSpsRates = doubleArrayOf(0.1, 0.5, 3.0, 15.0, 80.0, 400.0, 2000.0, 10000.0)

    // 50 Achievements list definition
    val achievementsDefinitions = listOf(
        // Tuz Ustası (8)
        Achievement("ach_salt_1", "tuz", "İlk Adım", "1,000 Toplam Tuz Puanı elde et.", 1000.0, "🧂"),
        Achievement("ach_salt_2", "tuz", "Kebap Kalfa", "10,000 Toplam Tuz Puanı elde et.", 10000.0, "👨‍🍳"),
        Achievement("ach_salt_3", "tuz", "Baharat Tüccarı", "100,000 Toplam Tuz Puanı elde et.", 100000.0, "🌶️"),
        Achievement("ach_salt_4", "tuz", "Tuz Deposu", "1M Toplam Tuz Puanı elde et.", 1000000.0, "🏔️"),
        Achievement("ach_salt_5", "tuz", "Sodyum Çılgınlığı", "10M Toplam Tuz Puanı elde et.", 10000000.0, "⚡"),
        Achievement("ach_salt_6", "tuz", "Kristal Kralı", "100M Toplam Tuz Puanı elde et.", 100000000.0, "💎"),
        Achievement("ach_salt_7", "tuz", "Kebabın Efendisi", "1B Toplam Tuz Puanı elde et.", 1000000000.0, "👑"),
        Achievement("ach_salt_8", "tuz", "Kozmik Dönerci", "1T Toplam Tuz Puanı elde et.", 1000000000000.0, "🌌"),

        // İnşaatçılar (8)
        Achievement("ach_build_10", "insaat", "Dükkan Kurucu", "Yarım düzine bina satın al (10 bina).", 10.0, "🏚️"),
        Achievement("ach_build_50", "insaat", "Mahalle Esnafı", "En az 50 bina sahibi ol.", 50.0, "🏬"),
        Achievement("ach_build_100", "insaat", "Kebap Sektörü", "En az 100 bina sahibi ol.", 100.0, "🏢"),
        Achievement("ach_build_200", "insaat", "Sosyete Kebapçısı", "En az 200 bina sahibi ol.", 200.0, "🏨"),
        Achievement("ach_build_300", "insaat", "Tuzlama Fabrikası", "En az 300 bina sahibi ol.", 300.0, "🏭"),
        Achievement("ach_build_400", "insaat", "Lezzet Karteli", "En az 400 bina sahibi ol.", 400.0, "🕹️"),
        Achievement("ach_build_500", "insaat", "Döner Karteli", "En az 500 bina sahibi ol.", 500.0, "🏰"),
        Achievement("ach_build_600", "insaat", "İmparatorluk Sınırları", "En az 600 toplam bina sahibi ol.", 600.0, "🪐"),

        // Şehir Fatihleri (12)
        Achievement("ach_city_1", "sehir", "İstanbul Hatırası", "İstanbul Sokağı'nı keşfet.", 0.0, "🇹🇷"),
        Achievement("ach_city_2", "sehir", "Başkent Rüzgarı", "Ankara Çarşısı'nı aç.", 1.0, "🕌"),
        Achievement("ach_city_3", "sehir", "Ottoman Klasik", "Bursa Lokantası'nı aç.", 2.0, "🏔️"),
        Achievement("ach_city_4", "sehir", "Zengin Döner", "Dubai Kebap Tower'ı aç.", 3.0, "🏙️"),
        Achievement("ach_city_5", "sehir", "Uzak Doğu Sentezi", "Tokyo Fusion Grill'i aç.", 4.0, "🇯🇵"),
        Achievement("ach_city_6", "sehir", "Yıldızlararası Tuz", "Uzay İstasyonu'na yerleş.", 5.0, "🚀"),
        Achievement("ach_city_7", "sehir", "Nil Güneşi", "Kahire Çarşısı'nı aç.", 6.0, "🐫"),
        Achievement("ach_city_8", "sehir", "Roma Ateşi", "Roma Piazzası'nı aç.", 7.0, "🏛️"),
        Achievement("ach_city_9", "sehir", "Özgürlük Döneri", "New York Deli'yi aç.", 8.0, "🗽"),
        Achievement("ach_city_10", "sehir", "Han Baharatı", "Şangay Mutfağı'nı aç.", 9.0, "🇨🇳"),
        Achievement("ach_city_11", "sehir", "Vahşi Lezzet", "Amazon Ormanı'nı aç.", 10.0, "🌴"),
        Achievement("ach_city_12", "sehir", "Boyutlarüstü Şef", "Paralel Evren'i aç.", 11.0, "🌀"),

        // Prestij (7)
        Achievement("ach_pres_1", "prestige", "Sıfırdan Başlamak", "İmparatorluğunu 1 kez yeniden aç.", 1.0, "💫"),
        Achievement("ach_pres_2", "prestige", "Ardı Ardına Lezzet", "İmparatorluğunu 2 kez yeniden aç.", 2.0, "🌀"),
        Achievement("ach_pres_3", "prestige", "Deneyimli Gurme", "İmparatorluğunu 3 kez yeniden aç.", 3.0, "🌟"),
        Achievement("ach_pres_5", "prestige", "Tuzlama Sanatkarı", "İmparatorluğunu 5 kez yeniden aç.", 5.0, "🔮"),
        Achievement("ach_pres_10", "prestige", "Yüce Kebap Gurusu", "İmparatorluğunu 10 kez yeniden aç.", 10.0, "⚜️"),
        Achievement("ach_pres_20", "prestige", "Zaman Bükücü Şef", "Prestij düzeyini 20 yap.", 20.0, "⏳"),
        Achievement("ach_spice_god", "prestige", "Baharat Tanrısı", "Prestige level 10'a ulas.", 10.0, "👑"),

        // Özel / Sosyal / Yeni Genişlemeler (15)
        Achievement("ach_spec_fire", "ozel", "Alevlerin Ustası", "Vurucu critical 'ALEV!' taplerinden 100 kez tetikle.", 100.0, "🔥"),
        Achievement("ach_spec_mill", "ozel", "Akış Gücü", "Tek bir oturumda 1M Tuz biriktir.", 1000000.0, "📈"),
        Achievement("ach_spec_days", "ozel", "Sabırlı Kebapçı", "Loyalty: Oyunu 7 gün oyna.", 1.0, "📆"),
        
        Achievement("ach_social_1", "ozel", "Sosyal Şef I", "1 arkadaş ekle.", 1.0, "👥"),
        Achievement("ach_social_5", "ozel", "Sosyal Şef II", "5 arkadaş ekle.", 5.0, "👥"),
        Achievement("ach_social_10", "ozel", "Sosyal Şef III", "10 arkadaş ekle.", 10.0, "👑"),

        Achievement("ach_gift_10", "ozel", "Cömert Usta I", "10 günlük hediye gönder.", 10.0, "🎁"),
        Achievement("ach_gift_50", "ozel", "Cömert Usta II", "50 günlük hediye gönder.", 50.0, "🎁"),
        Achievement("ach_gift_100", "ozel", "Cömert Usta III", "100 günlük hediye gönder.", 100.0, "💎"),

        Achievement("ach_visit_5", "ozel", "Gezgin Şef I", "5 arkadaş şehri ziyaret et.", 5.0, "✈️"),
        Achievement("ach_visit_20", "ozel", "Gezgin Şef II", "20 arkadaş şehri ziyaret et.", 20.0, "✈️"),
        Achievement("ach_visit_50", "ozel", "Gezgin Şef III", "50 arkadaş şehri ziyaret et.", 50.0, "🌍"),

        Achievement("ach_world_fator", "sehir", "Dünya Fatihi", "Şehirleri 12'ye kadar aç.", 11.0, "🗺️"),
        Achievement("ach_parallel_lord", "sehir", "Paralel Efendi", "Paralel Evren kilidini aç.", 11.0, "🌌"),
        Achievement("ach_time_traveler", "ozel", "Zaman Yolcusu", "Çevrimdışı kazancı 30 kez topla.", 30.0, "⏳"),
        Achievement("ach_tap_monster", "ozel", "Tık Canavarı", "10,000 kez kebap skewers tıkla.", 10000.0, "⚡"),
        Achievement("ach_fire_master", "ozel", "Alev Ustası", "500 critical alev tetiği bas.", 500.0, "🔥"),
        Achievement("ach_emperor", "tuz", "İmparator", "Toplamda 1Qt (10^18) Tuz Puanı biriktir.", 1_000_000_000_000_000_000.0, "👑")
    )

    private var criticalTapCount = 0

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var firestoreSyncJob: Job? = null

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        viewModelScope.launch {
            // Load core GameState
            val savedState = repository.getGameState()
            val finalState = if (savedState == null) {
                val defaultState = GameState(lastSessionTimestamp = System.currentTimeMillis())
                repository.saveGameState(defaultState)
                defaultState
            } else {
                savedState
            }
            // Generate stable friend code if missing
            val stateWithCode = if (finalState.friendCode.isEmpty()) {
                val generatedCode = "TUZK-" + (finalState.displayName.hashCode().coerceAtLeast(0) % 9000 + 1000)
                val updated = finalState.copy(friendCode = generatedCode)
                repository.saveGameState(updated)
                updated
            } else {
                finalState
            }
            _gameState.value = stateWithCode

            // Load all buildings levels database records
            val savedBuildings = repository.getAllBuildings()
            val buildingsMapTmp = mutableMapOf<String, BuildingState>()
            savedBuildings.forEach { b ->
                buildingsMapTmp[b.compoundId] = b
            }
            _buildingsMap.value = buildingsMapTmp

            // Load achievements status
            val savedAch = repository.getAllAchievements()
            _achievementsList.value = savedAch

            // Load and init all seasonal events
            val savedEvents = repository.getAllSeasonEvents().associateBy { it.eventId }
            val defaultEventsList = listOf(
                SeasonEventState("ramadan_event", true, System.currentTimeMillis() + 30L * 24 * 3600 * 1000, 0, 0.0),
                SeasonEventState("spring_event", true, System.currentTimeMillis() + 30L * 24 * 3600 * 1000, 0, 0.0),
                SeasonEventState("sports_event", true, System.currentTimeMillis() + 30L * 24 * 3600 * 1000, 0, 0.0),
                SeasonEventState("winter_event", true, System.currentTimeMillis() + 30L * 24 * 3600 * 1000, 0, 0.0)
            )
            defaultEventsList.forEach { dev ->
                if (!savedEvents.containsKey(dev.eventId)) {
                    repository.saveSeasonEvent(dev)
                }
            }
            _seasonEventsList.value = repository.getAllSeasonEvents()

            // Keep ramadan event reference updated for backward compatibility
            _ramadaEventState.value = repository.getSeasonEvent("ramadan_event")

            // Flow listener to keep friends state updated
            launch {
                repository.allFriendsFlow.collect { dbFriends ->
                    _friendsState.value = dbFriends
                }
            }

            // Calculate offline idle earnings if any
            _gameState.value?.let { calculateOfflineEarnings(it) }

            // Start Firebase Anonymous auth and remote database syncing
            loginAnonymouslyAndSync()

            // Run core threads loops
            startGameLoop()
            startParticlesLoop()
            checkRobotHandLoop()
            startAchievementsMonitor()
        }
    }

    private fun startParticlesLoop() {
        particleAnimateJob?.cancel()
        particleAnimateJob = viewModelScope.launch {
            while (true) {
                delay(33)
                val currentList = _particlesState.value
                if (currentList.isNotEmpty()) {
                    val updatedList = currentList.mapNotNull { p ->
                        val nextAge = p.ageMs + 33
                        if (nextAge > 500) {
                            null
                        } else {
                            val nextX = p.x + p.vx
                            val nextY = p.y + p.vy + 0.6f
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

    // Core passive profit calculation and auto save loop (every 3 seconds)
    private fun startGameLoop() {
        gameTickJob?.cancel()
        gameTickJob = viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                delay(200)
                val sps = calculateCurrentSps()
                val addedAmount = sps * 0.2 // 200ms part

                _gameState.update { current ->
                    val nextTotal = current.saltPoints + addedAmount
                    val nextAccumulated = current.totalEverEarned + addedAmount

                    current.copy(
                        saltPoints = nextTotal,
                        totalEverEarned = nextAccumulated,
                        lastSessionTimestamp = System.currentTimeMillis()
                    )
                }

                saveCounter++
                if (saveCounter >= 15) { // 15 * 200ms = 3 seconds auto-save requirement
                    saveCounter = 0
                    saveAllDataState()
                }
            }
        }
    }

    private fun checkRobotHandLoop() {
        robotHandJob?.cancel()
        robotHandJob = viewModelScope.launch {
            while (true) {
                delay(500) // fires every 500ms
                if (_gameState.value.robotHandUnlocked) {
                    autoRobotTap()
                }
            }
        }
    }

    // Achievements continuous collector running in background once per second
    private fun startAchievementsMonitor() {
        achievementsMonitorJob?.cancel()
        achievementsMonitorJob = viewModelScope.launch {
            while (true) {
                delay(1000) // check once a second
                val state = _gameState.value
                val bMap = _buildingsMap.value
                val listAchSaved = _achievementsList.value.associateBy { it.id }

                val updatedAchList = mutableListOf<AchievementState>()
                var newlyUnlockedTitle: String? = null

                achievementsDefinitions.forEach { def ->
                    val saved = listAchSaved[def.id]
                    if (saved == null || !saved.isUnlocked) {
                        var isCriteriaMet = false
                        var currentProgress = 0.0

                        when (def.category) {
                            "tuz" -> {
                                currentProgress = state.totalEverEarned
                                isCriteriaMet = state.totalEverEarned >= def.targetValue
                            }
                            "insaat" -> {
                                val totalBuilt = bMap.values.sumOf { it.level }
                                currentProgress = totalBuilt.toDouble()
                                isCriteriaMet = totalBuilt >= def.targetValue
                            }
                            "sehir" -> {
                                currentProgress = state.currentCityIndex.toDouble()
                                isCriteriaMet = state.currentCityIndex >= def.targetValue
                            }
                            "prestige" -> {
                                currentProgress = state.prestigeCount.toDouble()
                                isCriteriaMet = state.prestigeCount >= def.targetValue
                            }
                            "ozel" -> {
                                when (def.id) {
                                    "ach_spec_fire" -> {
                                        currentProgress = criticalTapCount.toDouble()
                                        isCriteriaMet = criticalTapCount >= def.targetValue
                                    }
                                    "ach_spec_mill" -> {
                                        currentProgress = state.totalEverEarned
                                        isCriteriaMet = state.totalEverEarned >= def.targetValue
                                    }
                                    "ach_spec_days" -> {
                                        currentProgress = 1.0
                                        isCriteriaMet = true // Already played
                                    }
                                    "ach_social_1", "ach_social_5", "ach_social_10" -> {
                                        val count = _friendsState.value.size
                                        currentProgress = count.toDouble()
                                        isCriteriaMet = count >= def.targetValue
                                    }
                                    "ach_gift_10", "ach_gift_50", "ach_gift_100" -> {
                                        currentProgress = state.totalGiftsSentCount.toDouble()
                                        isCriteriaMet = state.totalGiftsSentCount >= def.targetValue
                                    }
                                    "ach_visit_5", "ach_visit_20", "ach_visit_50" -> {
                                        val visitedCount = _friendsState.value.count { it.dailyVisitTapsUsed > 0 }.toDouble()
                                        currentProgress = visitedCount
                                        isCriteriaMet = visitedCount >= def.targetValue
                                    }
                                    "ach_time_traveler" -> {
                                        currentProgress = state.totalOfflineCollectsCount.toDouble()
                                        isCriteriaMet = state.totalOfflineCollectsCount >= def.targetValue
                                    }
                                    "ach_tap_monster" -> {
                                        currentProgress = state.totalTapCount.toDouble()
                                        isCriteriaMet = state.totalTapCount >= def.targetValue
                                    }
                                    "ach_fire_master" -> {
                                        currentProgress = criticalTapCount.toDouble()
                                        isCriteriaMet = criticalTapCount >= def.targetValue
                                    }
                                }
                            }
                        }

                        if (isCriteriaMet) {
                            val newAch = AchievementState(def.id, true, currentProgress, System.currentTimeMillis())
                            updatedAchList.add(newAch)
                            newlyUnlockedTitle = def.title
                        } else {
                            // Update progress only if changed
                            if (saved == null || saved.progress != currentProgress) {
                                updatedAchList.add(AchievementState(def.id, false, currentProgress, 0L))
                            }
                        }
                    }
                }

                if (updatedAchList.isNotEmpty()) {
                    repository.saveAchievements(updatedAchList)
                    // Reload achievement list
                    val savedAch = repository.getAllAchievements()
                    _achievementsList.value = savedAch

                    if (newlyUnlockedTitle != null) {
                        _achievementCelebration.value = "🏆 BAŞARI KAZANILDI!\n\n'$newlyUnlockedTitle' kilidi açıldı!"
                    }
                }
            }
        }
    }

    // High fidelity offline calculations
    private fun calculateOfflineEarnings(savedState: GameState) {
        val now = System.currentTimeMillis()
        val elapsedMs = now - savedState.lastSessionTimestamp
        if (elapsedMs < 10000) return // At least 10 secs away

        val spsRate = calculateCurrentSps()
        if (spsRate <= 0) return

        // Max 8 hours of offline progression (28800 seconds)
        val maxOfflineMs = 8 * 3600 * 1000L
        val validatedMs = elapsedMs.coerceAtMost(maxOfflineMs)
        val elapsedSec = validatedMs / 1000.0

        // In-app purchase offline bonus modifier
        var offlineMult = if (isDevModeEnabled.value) 3.0 else 1.0

        // Season constraint: Winter event active doubles offline earnings
        if (savedState.activeSeasonEventId == "winter_event" || savedState.hasSeasonPass) {
            offlineMult *= 2.0
        }

        val pointsEarned = elapsedSec * spsRate * offlineMult

        if (pointsEarned > 1.0) {
            _gameState.update { current ->
                current.copy(
                    saltPoints = current.saltPoints + pointsEarned,
                    totalEverEarned = current.totalEverEarned + pointsEarned,
                    totalOfflineCollectsCount = current.totalOfflineCollectsCount + 1,
                    lastSessionTimestamp = now
                )
            }

            val durationSecs = elapsedMs / 1000
            val hours = durationSecs / 3600
            val minutes = (durationSecs % 3600) / 60
            val timeString = if (hours > 0) "${hours}s ${minutes}d" else "${minutes}d"

            _offlineSummary.value = "Hoş geldin Şef! Kebap zincirlerin çevrimdışıyken boş durmadı!\n\n" +
                    "⏱️ Ayrılık Süresi: $timeString\n" +
                    "💰 Kazanılan Tuz: ${formatPoints(pointsEarned)} SP!"
        }
    }

    // Game Core Tappings with Bug 2 Debounce (atomic timestamps lock)
    fun tapSkewer(coordX: Float, coordY: Float) {
        val now = System.currentTimeMillis()
        val prev = lastTapTimestamp.get()
        if (now - prev < 100) return // Strict 100ms Debounce Window
        if (!lastTapTimestamp.compareAndSet(prev, now)) return

        triggerTapLogic(coordX, coordY, false)
    }

    private fun autoRobotTap() {
        val now = System.currentTimeMillis()
        val prev = lastTapTimestamp.get()
        // If human tapped very recently (<100ms), let's skip or adjust robot tap to prevent overlap overlap
        if (now - prev < 100) return
        if (!lastTapTimestamp.compareAndSet(prev, now)) return

        val robotX = 350f + (Random.nextFloat() * 300f)
        val robotY = 550f + (Random.nextFloat() * 300f)
        triggerTapLogic(robotX, robotY, true)
    }

    private fun triggerTapLogic(x: Float, y: Float, isRobot: Boolean) {
        val current = _gameState.value
        val isCritical = Random.nextFloat() <= 0.02f // 2% chance for ALEV!

        // Base tap value scales with SPS + prestige multiplier + offline stubs
        val currentSps = calculateCurrentSps()
        val baseTapAmount = 1.0 + (currentSps * 0.05)
        var rawEarned = if (isCritical) baseTapAmount * 10.0 else baseTapAmount

        // Apply IAP Tuz Baronu perk (x2 tap multiplier) if enabled
        if (current.isVIP || isDevModeEnabled.value) {
            rawEarned *= 2.0
        }

        // Apply Event currency multiplier if tap hours active
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isEventActiveHours = hour in 18..20
        val finalMultiplier = current.spicePowerMultiplier

        val finalEarned = rawEarned * finalMultiplier

        _gameState.update { state ->
            val eventCurrencyEarned = if (isEventActiveHours) state.eventCurrency + 5.0 else state.eventCurrency
            val nextTotal = state.saltPoints + finalEarned
            val nextAccumulated = state.totalEverEarned + finalEarned
            val nextTapCount = if (isRobot) state.totalTapCount else state.totalTapCount + 1

            // Continuous milestones checking
            checkAndPopMilestones(state.totalEverEarned, nextAccumulated)

            state.copy(
                saltPoints = nextTotal,
                totalEverEarned = nextAccumulated,
                totalTapCount = nextTapCount,
                eventCurrency = eventCurrencyEarned
            )
        }

        if (isCritical) {
            criticalTapCount++
        }

        // Generate customized colorized particles
        val colorType = if (isCritical) 2 else if (isRobot) 1 else 0
        val sizeMultiplier = if (isCritical) 2.5f else 1.2f
        val particleCount = if (isCritical) 20 else 6

        val newParticles = mutableListOf<SaltParticle>()
        for (i in 0 until particleCount) {
            newParticles.add(
                SaltParticle(
                    id = Random.nextLong(),
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 12f,
                    vy = (Random.nextFloat() - 1.2f) * 16f,
                    colorType = colorType,
                    scale = (Random.nextFloat() + 0.4f) * sizeMultiplier
                )
            )
        }

        // Add float text
        val displayText = when {
            isCritical -> "🔥 ALEV! +${formatPoints(finalEarned)}"
            isRobot -> "🤖 +${formatPoints(finalEarned)}"
            else -> "+${formatPoints(finalEarned)}"
        }

        newParticles.add(
            SaltParticle(
                id = Random.nextLong(),
                x = x,
                y = y - 30,
                vx = 0f,
                vy = -3f,
                colorType = colorType,
                scale = 1.3f,
                text = displayText
            )
        )

        // Stacking up with thread safety
        _particlesState.value = _particlesState.value + newParticles
    }

    // Multi modes buys cyclical function: 1 -> 10 -> 100 -> MAX
    fun cycleBuyMode() {
        _buyMode.update { current ->
            when (current) {
                1 -> 10
                10 -> 100
                100 -> Int.MAX_VALUE
                else -> 1
            }
        }
    }

    // Standard buildings definitions lookup
    fun getUpgradeLevel(cityIndex: Int, id: Int): Int {
        val compoundKey = "${cityIndex}_$id"
        return _buildingsMap.value[compoundKey]?.level ?: 0
    }

    // Dynamic cost & volume estimator to resolve Bug 1 Max Purchases
    fun getUpgradeCostAndQuantity(cityIndex: Int, id: Int): Pair<Double, Int> {
        val currentLevel = getUpgradeLevel(cityIndex, id)
        val mode = _buyMode.value
        val wallet = _gameState.value.saltPoints

        val cityScale = 1000.0.pow(cityIndex.toDouble())
        val baseCost = baseCosts[id] * cityScale

        var totalCost = 0.0
        var quantity = 0
        val geomFactor = 1.15

        val maxComputeLimit = if (mode == Int.MAX_VALUE) 250 else mode

        for (i in 0 until maxComputeLimit) {
            val levelToCompute = currentLevel + i
            val nextCost = baseCost * (geomFactor.pow(levelToCompute.toDouble()))

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

        // Force stubs if mode is 10/100 but player cannot afford 10/100.
        // It should match the total cost required to buy that quantity.
        if (mode != Int.MAX_VALUE && quantity < mode) {
            totalCost = 0.0
            for (i in 0 until mode) {
                totalCost += baseCost * (geomFactor.pow((currentLevel + i).toDouble()))
            }
            quantity = mode
        }

        return Pair(totalCost, quantity)
    }

    // Resolves Bug 1 completely: Check affordability and toast/error on 0
    fun canAffordUpgrade(cityIndex: Int, id: Int): Boolean {
        if (isDevModeEnabled.value) return true
        val (cost, qty) = getUpgradeCostAndQuantity(cityIndex, id)
        if (qty == 0) return false
        return _gameState.value.saltPoints >= cost
    }

    fun buyUpgrade(cityIndex: Int, id: Int) {
        val (cost, qty) = getUpgradeCostAndQuantity(cityIndex, id)
        val currentSP = _gameState.value.saltPoints

        if (qty == 0) {
            _milestoneCelebration.value = "⚠️ Yetersiz Tuz! Satın alacak kadar SP puanınız yok."
            return
        }

        if (currentSP < cost && !isDevModeEnabled.value) {
            _milestoneCelebration.value = "⚠️ Yetersiz Tuz! Bu miktarda bina alacak kadar SP puanınız yok."
            return
        }

        // Deduct cost and update State
        _gameState.update { current ->
            current.copy(saltPoints = if (isDevModeEnabled.value) current.saltPoints else current.saltPoints - cost)
        }

        val compoundKey = "${cityIndex}_$id"
        val existing = _buildingsMap.value[compoundKey]
        val updatedBuilding = if (existing == null) {
            BuildingState(compoundKey, id, cityIndex, qty, qty)
        } else {
            BuildingState(compoundKey, id, cityIndex, existing.level + qty, existing.totalPurchased + qty)
        }

        // Save map update
        _buildingsMap.update { currentMap ->
            val next = currentMap.toMutableMap()
            next[compoundKey] = updatedBuilding
            next
        }

        viewModelScope.launch {
            repository.saveBuilding(updatedBuilding)
            repository.saveGameState(_gameState.value)
        }
    }

    // Robot hand cost handling
    fun canAffordRobotHand(): Boolean {
        if (isDevModeEnabled.value) return true
        return _gameState.value.saltPoints >= 10000.0 && !_gameState.value.robotHandUnlocked
    }

    fun buyRobotHand() {
        if (!canAffordRobotHand() && !isDevModeEnabled.value) return
        _gameState.update { current ->
            current.copy(
                saltPoints = if (isDevModeEnabled.value) current.saltPoints else current.saltPoints - 10000.0,
                robotHandUnlocked = true
            )
        }
        viewModelScope.launch {
            repository.saveGameState(_gameState.value)
        }
    }

    // World Map City Unlock logic
    fun unlockCity(cityIndex: Int) {
        val state = _gameState.value
        val required = getCityUnlockThreshold(cityIndex)
        if (state.totalEverEarned >= required && cityIndex > state.currentCityIndex) {
            _gameState.update { current ->
                current.copy(currentCityIndex = cityIndex)
            }
            _milestoneCelebration.value = "🗺️ MÜKEMMEL TERFİ!\n\nYeni Şehir Açıldı! Artık ${getCityName(cityIndex)} pazarındayız! 🌟"
            viewModelScope.launch {
                repository.saveGameState(_gameState.value)
            }
        }
    }

    fun getCityUnlockThreshold(cityIndex: Int): Double {
        return when (cityIndex) {
            0 -> 0.0
            1 -> 1_000_000.0                    // 1M
            2 -> 50_000_000.0                   // 50M
            3 -> 500_000_000.0                  // 500M
            4 -> 10_000_000_000.0               // 10B
            5 -> 1_000_000_000_000.0            // 1T
            6 -> 10_000_000_000_000.0           // 10T (City 7)
            7 -> 100_000_000_000_000.0          // 100T (City 8)
            8 -> 1_000_000_000_000_000.0        // 1Qd (City 9)
            9 -> 10_000_000_000_000_000.0       // 10Qd (City 10)
            10 -> 100_000_000_000_000_000.0     // 100Qd (City 11)
            11 -> 1_000_000_000_000_000_000.0   // 1Qt (City 12)
            else -> 0.0
        }
    }

    fun getCityName(cityIndex: Int): String {
        return when (cityIndex) {
            0 -> "İstanbul Sokağı"
            1 -> "Ankara Çarşısı"
            2 -> "Bursa Lokantası"
            3 -> "Dubai Kebap Tower"
            4 -> "Tokyo Fusion Grill"
            5 -> "Uzay İstasyonu"
            6 -> "Kahire Çarşısı"
            7 -> "Roma Piazzası"
            8 -> "New York Deli"
            9 -> "Şangay Mutfağı"
            10 -> "Amazon Ormanı"
            11 -> "Paralel Evren"
            else -> "Yeni Şehir"
        }
    }

    // Evaluates SPS: Sum of all buildings across all unlockable cities multiplied by VIP/Prestiges
    fun calculateCurrentSps(): Double {
        var rawSps = 0.0
        // sum level * buildingSPS rate for all cities 0..11
        for (cityIdx in 0..11) {
            val cityScale = 1000.0.pow(cityIdx.toDouble())
            for (buildId in 0..7) {
                val level = getUpgradeLevel(cityIdx, buildId)
                val buildingSpsRate = baseSpsRates[buildId] * cityScale
                rawSps += level * buildingSpsRate
            }
        }

        // Include Ramadan Event special building "İftar Sofrası" if active
        _ramadaEventState.value?.let { event ->
            if (event.isActive) {
                rawSps += event.specialBuildingLevel * 500000.0
            }
        }

        // Extra: Include any other seasonal active special buildings if active
        _seasonEventsList.value.forEach { event ->
            if (event.isActive && event.eventId != "ramadan_event") {
                rawSps += event.specialBuildingLevel * 1000000.0 // Custom thematic multiplier
            }
        }

        val prestigeMult = 2.0.pow(_gameState.value.prestigeCount.toDouble())
        var finalSps = rawSps * prestigeMult

        // VIP subscription x5 income modifier
        if (_gameState.value.isVIP || isDevModeEnabled.value) {
            finalSps *= 5.0
        }

        return finalSps
    }

    // Prestige thresholds and recalculation updates (Resolves Bug 3 perfectly by checking totalEverEarned)
    fun getNextPrestigeThreshold(): Double {
        val count = _gameState.value.prestigeCount
        return when (count) {
            0 -> 1_000_000.0 // 1M
            1 -> 1_000_000_000.0 // 1B
            2 -> 1_000_000_000_000.0 // 1T
            3 -> 1_000_000_000_000_000.0 // 1Qd
            else -> 1_000_000_000_000_000.0 * 1000.0.pow((count - 3).toDouble())
        }
    }

    fun canPrestige(): Boolean {
        // Bug 3 check: totalEverEarned check, NOT current balance!
        return _gameState.value.totalEverEarned >= getNextPrestigeThreshold()
    }

    fun performPrestige() {
        if (!canPrestige()) return

        val originalState = _gameState.value
        val nextPrestigeCount = originalState.prestigeCount + 1
        
        // Multiplier breakdown calculation clear display
        val nextMult = originalState.spicePowerMultiplier * 2.0 // double the power

        _gameState.update { current ->
            current.copy(
                saltPoints = 0.0, // Resets balance but totalEverEarned keeps growing or accumulates
                prestigeCount = nextPrestigeCount,
                spicePowerMultiplier = nextMult
            )
        }

        // Reset Istanbul and all city buildings levels
        val clearedBuildingsList = mutableListOf<BuildingState>()
        _buildingsMap.value.forEach { (key, b) ->
            clearedBuildingsList.add(b.copy(level = 0))
        }

        _buildingsMap.value = clearedBuildingsList.associateBy { it.compoundId }

        _milestoneCelebration.value = "💥 PRESTİJ GERÇEKLEŞTİ!\n\nİmparatorluk Sıfırlandı. Baharat Gücü x2 olarak katlandı! Enerji zirve yaptı! 🧂🔥"

        viewModelScope.launch {
            repository.saveBuildings(clearedBuildingsList)
            repository.saveGameState(_gameState.value)
            val uid = auth.currentUser?.uid
            if (uid != null) {
                syncGameStateToFirestoreImmediate(uid, _gameState.value)
                clearedBuildingsList.forEach { syncBuildingToFirestoreImmediate(it) }
            }
        }
    }

    // Ramadan Special Building upgrade triggers
    fun buyRamadanBuilding() {
        val eventObj = _ramadaEventState.value ?: return
        val currentLevel = eventObj.specialBuildingLevel
        val cost = 100.0 * (1.15.pow(currentLevel.toDouble()))

        if (_gameState.value.eventCurrency >= cost || isDevModeEnabled.value) {
            _gameState.update { current ->
                current.copy(eventCurrency = if (isDevModeEnabled.value) current.eventCurrency else current.eventCurrency - cost)
            }
            val updatedEvent = eventObj.copy(specialBuildingLevel = currentLevel + 1)
            _ramadaEventState.value = updatedEvent

            viewModelScope.launch {
                repository.saveSeasonEvent(updatedEvent)
                repository.saveGameState(_gameState.value)
            }
        }
    }

    fun getRamadanBuildingCost(): Double {
        val eventObj = _ramadaEventState.value ?: return 99999.0
        return 100.0 * (1.15.pow(eventObj.specialBuildingLevel.toDouble()))
    }

    // In-App purchase Simulated structures
    fun purchaseItem(itemCode: String) {
        _gameState.update { current ->
            when (itemCode) {
                "baron" -> {
                    // permanent x2 tap multiplier simulation
                    current.copy(isVIP = true)
                }
                "baharat" -> {
                    // Unlock robot hand immediately
                    current.copy(robotHandUnlocked = true)
                }
                "harita" -> {
                    // Unlock city 2 and 3 immediately (Cairo/Ankara and Bursa index 1 and 2)
                    current.copy(currentCityIndex = maxOf(current.currentCityIndex, 2))
                }
                "harita_7_9" -> {
                    current.copy(
                        hasWorldTourExpansion = true,
                        currentCityIndex = maxOf(current.currentCityIndex, 8)
                    )
                }
                "harita_10_12" -> {
                    current.copy(
                        hasParallelPackage = true,
                        currentCityIndex = maxOf(current.currentCityIndex, 11)
                    )
                }
                "sosyal_paketi" -> {
                    current.copy(hasSocialPackage = true)
                }
                "sezon_pass" -> {
                    current.copy(
                        hasSeasonPass = true,
                        eventCurrency = current.eventCurrency + 1000.0
                    )
                }
                "all_access" -> {
                    current.copy(
                        isVIP = true,
                        robotHandUnlocked = true,
                        hasWorldTourExpansion = true,
                        hasParallelPackage = true,
                        hasSocialPackage = true,
                        hasSeasonPass = true,
                        eventCurrency = current.eventCurrency + 5000.0,
                        spicePowerMultiplier = current.spicePowerMultiplier * 5.0
                    )
                }
                "vip_sub" -> {
                    current.copy(isVIP = true)
                }
                "emp_sub" -> {
                    current.copy(isVIP = true, spicePowerMultiplier = current.spicePowerMultiplier * 5.0)
                }
                else -> current
            }
        }
        _milestoneCelebration.value = "🎉 TEBRİKLER!\n\nSatın alım başarılı! Faydalar derhal hesabınıza yansıdı. 👑"
        saveAllDataState()
    }

    // Leaderboard display name setting
    fun setPlayerDisplayName(name: String) {
        if (name.isNotBlank()) {
            _gameState.update { it.copy(displayName = name) }
            saveAllDataState()
            viewModelScope.launch {
                loadOrInitLeaderboard()
            }
        }
    }

    private fun loadOrInitLeaderboard() {
        // Obsoleted by real-time Firestore Snapshot Listener
    }

    // Complete Reset Game option with dual confirmation checks
    fun resetFully() {
        viewModelScope.launch {
            repository.clearLeaderboard()
            _gameState.value = GameState()
            _buildingsMap.value = emptyMap()
            _achievementsList.value = emptyList()

            val d = GameDatabase.getDatabase(getApplication())
            d.clearAllTables()

            _gameState.value = GameState(lastSessionTimestamp = System.currentTimeMillis())
            repository.saveGameState(_gameState.value)

            loadOrInitLeaderboard()
            _ramadaEventState.value = SeasonEventState("ramadan_event", true, System.currentTimeMillis() + 30L * 24 * 3600 * 1000, 0, 0.0)
            repository.saveSeasonEvent(_ramadaEventState.value!!)

            _milestoneCelebration.value = "🗑️ Her Şey Temizlendi!\n\nTüm ilerleme sıfırlandı ve dükkanınız ilk günkü haline döndü."
        }
    }

    // Synchronous save trigger for onStop/onPause safely using runBlocking
    fun saveAllDataStateSync() {
        runBlocking {
            saveAllDataState()
        }
    }

    private fun saveAllDataState() {
        viewModelScope.launch {
            _gameState.value?.let { repository.saveGameState(it) }
            _buildingsMap.value.values.toList().let { repository.saveBuildings(it) }
            _seasonEventsList.value.forEach { repository.saveSeasonEvent(it) }
            _ramadaEventState.value?.let { repository.saveSeasonEvent(it) }
        }
    }

    // --- OFFLINE SOCIAL MANAGEMENT METHODS ---

    fun generateLocalProfileString(): String {
        val state = _gameState.value
        val compactBuildings = JSONObject()
        _buildingsMap.value.forEach { (key, b) ->
            if (b.level > 0) {
                compactBuildings.put(b.compoundId, b.level)
            }
        }

        val json = JSONObject()
        json.put("code", state.friendCode)
        json.put("name", state.displayName)
        json.put("earned", state.totalEverEarned)
        json.put("city", state.currentCityIndex)
        json.put("prestige", state.prestigeCount)
        json.put("sps", calculateCurrentSps())
        json.put("buildings", compactBuildings)
        json.put("timestamp", System.currentTimeMillis())

        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "KEBAP-PROFIL-[$b64]"
    }

    fun addFriendFromCodeOrSnapshot(codeOrSnapshot: String) {
        val trimmed = codeOrSnapshot.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                // Query Firestore users to see if friendCode matches
                firestore.collection("users")
                    .whereEqualTo("friendCode", trimmed)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val doc = querySnapshot.documents[0]
                            val otherUid = doc.id
                            val fCode = doc.getString("friendCode") ?: ""
                            val fName = doc.getString("displayName") ?: ""
                            val fEarned = doc.getDouble("totalEarned") ?: 0.0
                            val fCity = doc.getLong("cityIndex")?.toInt() ?: 0
                            val fPrestige = doc.getLong("prestigeCount")?.toInt() ?: 0

                            // Fetch companion buildings list from doc subcollection
                            doc.reference.collection("buildings").get()
                                .addOnSuccessListener { buildingDocs ->
                                    val dummyBuildings = JSONObject()
                                    for (bDoc in buildingDocs) {
                                        val level = bDoc.getLong("level")?.toInt() ?: 0
                                        if (level > 0) {
                                            dummyBuildings.put(bDoc.id, level)
                                        }
                                    }
                                    val profile = FriendProfile(
                                        friendCode = fCode,
                                        displayName = fName,
                                        totalEarned = fEarned,
                                        cityIndex = fCity,
                                        prestigeCount = fPrestige,
                                        buildingSnapshot = dummyBuildings.toString(),
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                    viewModelScope.launch {
                                        repository.saveFriend(profile)
                                        syncFriendToFirestoreImmediate(profile)
                                    }
                                    _milestoneCelebration.value = "👥 ARKADAŞ EKLENDİ!\n\n${fName} (${fCode}) başarıyla listenize eklendi! Şehirlerini ziyaret edebilir ve hediyeleşebilirsiniz. 🎉"
                                }
                        } else {
                            // If not found online, fallback to legacy Base64 parsing or localized code creation
                            if (trimmed.startsWith("KEBAP-PROFIL-[") && trimmed.endsWith("]")) {
                                parseLegacyFriendSnapshot(trimmed)
                            } else if (trimmed.matches(Regex("^[A-Z0-9]{4}-[0-9]{4}$")) || trimmed.startsWith("TUZK-")) {
                                val code = trimmed
                                val suffix = code.takeLast(4)
                                val generatedName = if (code.startsWith("TUZK-")) "Tuzcu Usta $suffix" else "Kebapçı Gurme $suffix"
                                val parentEarned = maxOf(1000000.0, _gameState.value.totalEverEarned * (0.5 + Random.nextDouble() * 1.5))
                                val generatedCity = if (parentEarned < 1_000_000.0) 0 else if (parentEarned < 50_000_000.0) 1 else 3
                                
                                val dummyBuildings = JSONObject()
                                dummyBuildings.put("${generatedCity}_0", 15)
                                dummyBuildings.put("${generatedCity}_1", 8)

                                val profile = FriendProfile(
                                    friendCode = code,
                                    displayName = generatedName,
                                    totalEarned = parentEarned,
                                    cityIndex = generatedCity,
                                    prestigeCount = Random.nextInt(0, 3),
                                    buildingSnapshot = dummyBuildings.toString(),
                                    lastUpdated = System.currentTimeMillis()
                                )

                                viewModelScope.launch {
                                    repository.saveFriend(profile)
                                    syncFriendToFirestoreImmediate(profile)
                                }
                                _milestoneCelebration.value = "👥 OFFLINE ARKADAŞ EKLENDİ!\n\n${profile.displayName} kodundan offline snapshot başarıyla üretildi! 🎉"
                            } else {
                                _milestoneCelebration.value = "⚠️ Geçersiz Kebap Kodu! Lütfen kodu kontrol edin."
                            }
                        }
                    }
                    .addOnFailureListener {
                        _milestoneCelebration.value = "❌ Firestore bağlantı hatası oluştu."
                    }
            } catch (e: Exception) {
                _milestoneCelebration.value = "❌ Arkadaş ekleme sırasında bilinmeyen hata."
            }
        }
    }

    private fun parseLegacyFriendSnapshot(trimmed: String) {
        try {
            val rawB64 = trimmed.substring("KEBAP-PROFIL-[".length, trimmed.length - 1)
            val jsonStr = String(Base64.decode(rawB64, Base64.NO_WRAP), Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            val code = json.getString("code")
            val name = json.getString("name")
            val earned = json.getDouble("earned")
            val city = json.getInt("city")
            val prestige = json.getInt("prestige")
            val buildingsString = json.getJSONObject("buildings").toString()

            val profile = FriendProfile(
                friendCode = code,
                displayName = name,
                totalEarned = earned,
                cityIndex = city,
                prestigeCount = prestige,
                buildingSnapshot = buildingsString,
                lastUpdated = System.currentTimeMillis()
            )

            viewModelScope.launch {
                repository.saveFriend(profile)
                syncFriendToFirestoreImmediate(profile)
            }
            _milestoneCelebration.value = "👥 ENKODE ARKADAŞ EKLENDİ!\n\n${name} (${code}) başarıyla profil verisinden kuruldu! 🍢"
        } catch (e: Exception) {
            _milestoneCelebration.value = "❌ Kod Çözme Hatası! Kopyalanan profil verisi bozuk."
        }
    }

    fun sendGiftToFriend(friend: FriendProfile) {
        val now = System.currentTimeMillis()
        if (now - friend.lastGiftSentTimestamp < 24 * 3600 * 1000L && !isDevModeEnabled.value) {
            _milestoneCelebration.value = "⏳ Zaten Gönderildi!\n\nBu arkadaşınıza son 24 saat içinde günlük hediye gönderdiniz."
            return
        }

        // Gift amount is exactly 1% of sender's totalEverEarned
        val giftValue = _gameState.value.totalEverEarned * 0.01
        
        try {
            val json = JSONObject()
            json.put("code", "GIFT-${Random.nextInt(1000, 9999)}-${System.currentTimeMillis()}")
            json.put("sender", _gameState.value.displayName)
            json.put("amount", giftValue)
            json.put("expires", now + 48 * 3600 * 1000L) // Valid for 48 hours

            val bytes = json.toString().toByteArray(Charsets.UTF_8)
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val finalGiftString = "KEBAP-HEDIYE-[$b64]"

            lastGeneratedGiftCode.value = finalGiftString

            // Update friend profil sent state
            viewModelScope.launch {
                val updatedFriend = friend.copy(lastGiftSentTimestamp = now)
                repository.saveFriend(updatedFriend)

                _gameState.update { state ->
                    state.copy(totalGiftsSentCount = state.totalGiftsSentCount + 1)
                }
                repository.saveGameState(_gameState.value)
            }

            _milestoneCelebration.value = "🎁 TUZ HEDİYESİ HAZIR! ✉️\n\nArkadaşınız için %1 feda edilerek ${formatPoints(giftValue)} SP gücünde Hediye Kodu üretildi! Paylaşmak için kopyalayabilirsiniz."
        } catch (e: Exception) {
            _milestoneCelebration.value = "❌ Hediye üretilemedi!"
        }
    }

    fun redeemGiftCode(rawCode: String) {
        val codeClean = rawCode.trim()
        if (codeClean.isEmpty()) return

        try {
            if (codeClean.startsWith("KEBAP-HEDIYE-[") && codeClean.endsWith("]")) {
                val rawB64 = codeClean.substring("KEBAP-HEDIYE-[".length, codeClean.length - 1)
                val jsonStr = String(Base64.decode(rawB64, Base64.NO_WRAP), Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                val uniqueCode = json.getString("code")
                val sender = json.getString("sender")
                val amount = json.getDouble("amount")
                val expires = json.getLong("expires")

                if (System.currentTimeMillis() > expires) {
                    _milestoneCelebration.value = "⏳ Hediye Süresi Dolmuş!\n\nBu kodun 48 saatlik geçerlilik süresi tükenmiştir."
                    return
                }

                viewModelScope.launch {
                    val existing = repository.getGiftCode(uniqueCode)
                    if (existing != null && existing.isRedeemed) {
                        _milestoneCelebration.value = "⚠️ Zaten Kullanılmış!\n\nBu tuz hediyesini daha önce zaten kabul ettiniz."
                    } else {
                        // Mark as redeemed
                        repository.saveGiftCode(GiftCode(uniqueCode, sender, amount, expires, isRedeemed = true))

                        // Credit Player
                        _gameState.update { state ->
                            state.copy(
                                saltPoints = state.saltPoints + amount,
                                totalEverEarned = state.totalEverEarned + amount
                            )
                        }
                        repository.saveGameState(_gameState.value)

                        _milestoneCelebration.value = "🎁 HEDİYE KABUL EDİLDİ! 🎉\n\n${sender} tarafından gönderilen ${formatPoints(amount)} SP değerindeki Tuz Hediyesi dükkanınıza döküldü!"
                    }
                }
            } else {
                _milestoneCelebration.value = "⚠️ Geçersiz Hediye Kodu Formatı! Kod 'KEBAP-HEDIYE-[' ile başlamalıdır."
            }
        } catch (e: Exception) {
            _milestoneCelebration.value = "❌ Çözümleme Hatası! Lütfen kopyalanan hediye verisini eksiksiz yapıştırın."
        }
    }

    fun tapFriendSkewerInVisitMode() {
        val friendObj = visitingFriend.value ?: return
        if (friendObj.dailyVisitTapsUsed >= 30 && !isDevModeEnabled.value) {
            _milestoneCelebration.value = "🛑 Günlük Ziyaret Limiti!\n\nBu arkadaşınızın dönerini bugün 30 kez tıklandınız. Yarın tekrar gelebilirsiniz!"
            return
        }

        // Visitor earns 0.5% of friend's sps rate as bonus
        // Parse friend snapshot to calculate realistic SPS
        val friendSps = calculateFriendSps(friendObj.cityIndex, friendObj.prestigeCount, friendObj.buildingSnapshot)
        val bonusReward = maxOf(1.0, friendSps * 0.005)

        _gameState.update { state ->
            state.copy(
                saltPoints = state.saltPoints + bonusReward,
                totalEverEarned = state.totalEverEarned + bonusReward
            )
        }

        val updatedFriend = friendObj.copy(dailyVisitTapsUsed = friendObj.dailyVisitTapsUsed + 1)
        visitingFriend.value = updatedFriend

        viewModelScope.launch {
            repository.saveFriend(updatedFriend)
        }

        // Generate Gold Taps Particles over the screen
        val randomX = 200f + Random.nextFloat() * 400f
        val randomY = 600f + Random.nextFloat() * 300f
        val list = mutableListOf<SaltParticle>()
        for (i in 0 until 5) {
            list.add(
                SaltParticle(
                    id = Random.nextLong(),
                    x = randomX,
                    y = randomY,
                    vx = (Random.nextFloat() - 0.5f) * 10f,
                    vy = (Random.nextFloat() - 1f) * 12f,
                    colorType = 1, // Gold
                    scale = 1.5f
                )
            )
        }
        list.add(
            SaltParticle(
                id = Random.nextLong(),
                x = randomX,
                y = randomY - 40f,
                vx = 0f,
                vy = -2f,
                colorType = 1,
                scale = 1.4f,
                text = "+${formatPoints(bonusReward)} 👥"
            )
        )
        _particlesState.value = _particlesState.value + list
    }

    private fun calculateFriendSps(cityIdx: Int, prestige: Int, buildingJson: String): Double {
        var baseRate = 0.0
        try {
            if (buildingJson.isNotEmpty()) {
                val json = JSONObject(buildingJson)
                val cityScale = 1000.0.pow(cityIdx.toDouble())
                for (i in 0..7) {
                    val key = "${cityIdx}_$i"
                    if (json.has(key)) {
                        val lvl = json.getInt(key)
                        baseRate += lvl * baseSpsRates[i] * cityScale
                    }
                }
            }
        } catch (e: Exception) {
            baseRate = 50.0 * 1000.0.pow(cityIdx.toDouble()) // decent default
        }
        val prestigePower = 2.0.pow(prestige.toDouble())
        return baseRate * prestigePower
    }

    fun selectActiveSeasonEvent(eventId: String) {
        _gameState.update { state ->
            state.copy(activeSeasonEventId = eventId)
        }
        viewModelScope.launch {
            repository.saveGameState(_gameState.value)
        }
        _seasonEventsList.value = _seasonEventsList.value.map { ev ->
            if (ev.eventId == eventId) ev.copy(isActive = true) else ev // keep others running if premium Sezon Pass is purchased
        }
    }

    fun buySeasonEventBuilding(eventId: String) {
        val eventObj = _seasonEventsList.value.find { it.eventId == eventId } ?: return
        val currentLevel = eventObj.specialBuildingLevel
        val cost = 150.0 * (1.18.pow(currentLevel.toDouble()))

        val wallet = _gameState.value.eventCurrency
        val multiplier = if (_gameState.value.hasSeasonPass) 2.0 else 1.0

        if (wallet >= cost || isDevModeEnabled.value) {
            _gameState.update { state ->
                state.copy(eventCurrency = if (isDevModeEnabled.value) state.eventCurrency else state.eventCurrency - cost)
            }
            val updatedEvent = eventObj.copy(specialBuildingLevel = currentLevel + 1)
            
            _seasonEventsList.value = _seasonEventsList.value.map { ev ->
                if (ev.eventId == eventId) updatedEvent else ev
            }

            if (eventId == "ramadan_event") {
                _ramadaEventState.value = updatedEvent
            }

            viewModelScope.launch {
                repository.saveSeasonEvent(updatedEvent)
                repository.saveGameState(_gameState.value)
            }
        }
    }

    fun getSeasonBuildingCost(eventId: String): Double {
        val eventObj = _seasonEventsList.value.find { it.eventId == eventId } ?: return 999.0
        return 150.0 * (1.18.pow(eventObj.specialBuildingLevel.toDouble()))
    }

    fun closeFriendVisit() {
        visitingFriend.value = null
    }

    fun removeFriend(code: String) {
        viewModelScope.launch {
            repository.deleteFriend(code)
            loadOrInitLeaderboard()
            _milestoneCelebration.value = "🗑️ Arkadaş silindi!"
        }
    }

    // Displays Pop Check
    private fun checkAndPopMilestones(prev: Double, next: Double) {
        val millList = listOf(
            Pair(1000.0, "🌟 Tebrikler! 1,000 SP Dönüm Noktasına Ulaştın! Yeni çıraklar seni izliyor! 🧂"),
            Pair(100000.0, "🌶️ Harika! 100,000 SP oldu! Baharatların gücünü pazar tezgahlarında hissetmeye başladık!"),
            Pair(1000000.0, "🤩 İNANILMAZ! 1,000,000 (1M) Tuz topladın! Artık imparatorluğun kapıları açıldı! Baharat Gücünü serbest bırakabilirsin! 🔥"),
            Pair(1000000000.0, "🔥 KÜRESEL ÇAPTA EFSANE! 1,000,000,000 (1B) Tuzlama Sayonorası! Tuzlama şöhretin dünyayı sardı! 🌍"),
            Pair(1000000000000.0, "👑 KOZMİK SULTAN! 1,000,000,000,000 (1T) Tuzlama İmparatoru! Evrende senden lezzetli döner yapan yok! 🌌")
        )

        for (item in millList) {
            if (prev < item.first && next >= item.first) {
                _milestoneCelebration.value = item.second
            }
        }
    }

    fun dismissOfflineEarnings() {
        _offlineSummary.value = null
    }

    fun dismissMilestone() {
        _milestoneCelebration.value = null
    }

    fun dismissAchievementCelebration() {
        _achievementCelebration.value = null
    }

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

    // --- FIREBASE SYNC METHODS ---

    private fun loginAnonymouslyAndSync() {
        auth.signInAnonymously().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    Log.d("GameViewModel", "Logged in anonymously with UID: $uid")
                    setupFirebaseSync(uid)
                }
            } else {
                Log.e("GameViewModel", "Failed to login anonymously", task.exception)
            }
        }
    }

    private fun setupFirebaseSync(uid: String) {
        // 1. Listen to user document for GameState updates
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GameViewModel", "Listen to user failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val rawState = snapshot.get("gameState") as? Map<String, Any?>
                    if (rawState != null) {
                        val remoteState = mapToGameState(rawState)
                        val localState = _gameState.value
                        if (localState == null || remoteState.totalEverEarned > localState.totalEverEarned) {
                            Log.d("GameViewModel", "Firestore has newer state (${remoteState.totalEverEarned} > ${localState?.totalEverEarned}). Syncing to local.")
                            _gameState.value = remoteState
                            viewModelScope.launch {
                                repository.saveGameState(remoteState)
                            }
                        }
                    }
                } else {
                    // Document does not exist, upload current state to populate
                    val localState = _gameState.value
                    if (localState != null) {
                        syncGameStateToFirestoreImmediate(uid, localState)
                    }
                }
            }

        // 2. Listen to buildings subcollection for offline resilience sync
        firestore.collection("users").document(uid).collection("buildings")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val currentMap = _buildingsMap.value.toMutableMap()
                var updated = false
                snapshot.documents.forEach { doc ->
                    val compoundId = doc.id
                    val level = doc.getLong("level")?.toInt() ?: 0
                    val totalPurchased = doc.getLong("totalPurchased")?.toInt() ?: 0
                    val existing = currentMap[compoundId]
                    if (existing == null || existing.level != level || existing.totalPurchased != totalPurchased) {
                        val parts = compoundId.split("_")
                        val id = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val cityIndex = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val bState = BuildingState(compoundId, id, cityIndex, level, totalPurchased)
                        currentMap[compoundId] = bState
                        updated = true
                        viewModelScope.launch { repository.saveBuilding(bState) }
                    }
                }
                if (updated) {
                    _buildingsMap.value = currentMap
                }
            }

        // 3. Listen to achievements subcollection
        firestore.collection("users").document(uid).collection("achievements")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val currentAch = _achievementsList.value.toMutableList()
                var updated = false
                snapshot.documents.forEach { doc ->
                    val achState = doc.toObject(AchievementState::class.java) ?: return@forEach
                    val idx = currentAch.indexOfFirst { it.id == achState.id }
                    if (idx == -1) {
                        currentAch.add(achState)
                        updated = true
                    } else if (currentAch[idx].isUnlocked != achState.isUnlocked || currentAch[idx].progress != achState.progress) {
                        currentAch[idx] = achState
                        updated = true
                    }
                    viewModelScope.launch { repository.saveAchievement(achState) }
                }
                if (updated) {
                    _achievementsList.value = currentAch
                }
            }

        // 4. Listen to friends subcollection
        firestore.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val friendsListTmp = snapshot.documents.mapNotNull { doc -> doc.toObject(FriendProfile::class.java) }
                if (friendsListTmp.isNotEmpty()) {
                    _friendsState.value = friendsListTmp
                    viewModelScope.launch {
                        friendsListTmp.forEach { repository.saveFriend(it) }
                    }
                }
            }

        // 5. Subscribe to dynamic real-time leaderboard
        firestore.collection("leaderboard")
            .orderBy("totalEarned", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val entry = doc.toObject(LeaderboardEntry::class.java) ?: return@mapNotNull null
                    val isLocal = doc.id == uid
                    entry.copy(isLocalPlayer = isLocal)
                }
                _leaderboardList.value = list
            }

        // 6. Start background 30 second gameState synchronizer loop
        firestoreSyncJob?.cancel()
        firestoreSyncJob = viewModelScope.launch {
            while (true) {
                delay(30000)
                _gameState.value?.let { state ->
                    syncGameStateToFirestoreImmediate(uid, state)
                }
            }
        }
    }

    private fun syncGameStateToFirestoreImmediate(uid: String, state: GameState) {
        val map = gameStateToMap(state)
        val dataPayload = mapOf(
            "friendCode" to state.friendCode,
            "displayName" to state.displayName,
            "totalEarned" to state.totalEverEarned,
            "cityIndex" to state.currentCityIndex,
            "prestigeCount" to state.prestigeCount,
            "gameState" to map
        )
        firestore.collection("users").document(uid)
            .set(dataPayload, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("GameViewModel", "Failed to sync gameState to Firestore", e)
            }

        // Also update local player row in global leaderboard instantly
        val leaderboardMap = mapOf(
            "rank" to 0, // dynamic rank computed on read
            "playerName" to state.displayName,
            "totalEarned" to state.totalEverEarned,
            "cityReached" to getCityName(state.currentCityIndex),
            "prestigeCount" to state.prestigeCount,
            "isLocalPlayer" to true
        )
        firestore.collection("leaderboard").document(uid)
            .set(leaderboardMap, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("GameViewModel", "Failed to update leaderboard row", e)
            }
    }

    private fun syncBuildingToFirestoreImmediate(building: BuildingState) {
        val uid = auth.currentUser?.uid ?: return
        val map = mapOf(
            "compoundId" to building.compoundId,
            "id" to building.id,
            "cityIndex" to building.cityIndex,
            "level" to building.level,
            "totalPurchased" to building.totalPurchased
        )
        firestore.collection("users").document(uid)
            .collection("buildings").document(building.compoundId)
            .set(map, SetOptions.merge())
    }

    private fun syncAchievementToFirestoreImmediate(achievement: AchievementState) {
        val uid = auth.currentUser?.uid ?: return
        val map = mapOf(
            "id" to achievement.id,
            "isUnlocked" to achievement.isUnlocked,
            "progress" to achievement.progress,
            "unlockedTimestamp" to achievement.unlockedTimestamp
        )
        firestore.collection("users").document(uid)
            .collection("achievements").document(achievement.id)
            .set(map, SetOptions.merge())
    }

    private fun syncFriendToFirestoreImmediate(friend: FriendProfile) {
        val uid = auth.currentUser?.uid ?: return
        val map = mapOf(
            "friendCode" to friend.friendCode,
            "displayName" to friend.displayName,
            "totalEarned" to friend.totalEarned,
            "cityIndex" to friend.cityIndex,
            "prestigeCount" to friend.prestigeCount,
            "buildingSnapshot" to friend.buildingSnapshot,
            "lastUpdated" to friend.lastUpdated,
            "lastGiftSentTimestamp" to friend.lastGiftSentTimestamp,
            "dailyVisitTapsUsed" to friend.dailyVisitTapsUsed
        )
        firestore.collection("users").document(uid)
            .collection("friends").document(friend.friendCode)
            .set(map, SetOptions.merge())
    }

    private fun gameStateToMap(state: GameState): Map<String, Any> {
        return mapOf(
            "id" to state.id,
            "saltPoints" to state.saltPoints,
            "totalEverEarned" to state.totalEverEarned,
            "lastSessionTimestamp" to state.lastSessionTimestamp,
            "prestigeCount" to state.prestigeCount,
            "spicePowerMultiplier" to state.spicePowerMultiplier,
            "currentCityIndex" to state.currentCityIndex,
            "displayName" to state.displayName,
            "isVIP" to state.isVIP,
            "eventCurrency" to state.eventCurrency,
            "robotHandUnlocked" to state.robotHandUnlocked,
            "friendCode" to state.friendCode,
            "totalGiftsSentCount" to state.totalGiftsSentCount,
            "totalOfflineCollectsCount" to state.totalOfflineCollectsCount,
            "totalTapCount" to state.totalTapCount,
            "activeSeasonEventId" to state.activeSeasonEventId,
            "hasSocialPackage" to state.hasSocialPackage,
            "hasSeasonPass" to state.hasSeasonPass,
            "hasWorldTourExpansion" to state.hasWorldTourExpansion,
            "hasParallelPackage" to state.hasParallelPackage
        )
    }

    private fun mapToGameState(map: Map<String, Any?>): GameState {
        return GameState(
            id = (map["id"] as? Number)?.toInt() ?: 1,
            saltPoints = (map["saltPoints"] as? Number)?.toDouble() ?: 0.0,
            totalEverEarned = (map["totalEverEarned"] as? Number)?.toDouble() ?: 0.0,
            lastSessionTimestamp = (map["lastSessionTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            prestigeCount = (map["prestigeCount"] as? Number)?.toInt() ?: 0,
            spicePowerMultiplier = (map["spicePowerMultiplier"] as? Number)?.toDouble() ?: 1.0,
            currentCityIndex = (map["currentCityIndex"] as? Number)?.toInt() ?: 0,
            displayName = map["displayName"] as? String ?: "Kebap Sever",
            isVIP = map["isVIP"] as? Boolean ?: false,
            eventCurrency = (map["eventCurrency"] as? Number)?.toDouble() ?: 0.0,
            robotHandUnlocked = map["robotHandUnlocked"] as? Boolean ?: false,
            friendCode = map["friendCode"] as? String ?: "",
            totalGiftsSentCount = (map["totalGiftsSentCount"] as? Number)?.toInt() ?: 0,
            totalOfflineCollectsCount = (map["totalOfflineCollectsCount"] as? Number)?.toInt() ?: 0,
            totalTapCount = (map["totalTapCount"] as? Number)?.toInt() ?: 0,
            activeSeasonEventId = map["activeSeasonEventId"] as? String ?: "ramadan_event",
            hasSocialPackage = map["hasSocialPackage"] as? Boolean ?: false,
            hasSeasonPass = map["hasSeasonPass"] as? Boolean ?: false,
            hasWorldTourExpansion = map["hasWorldTourExpansion"] as? Boolean ?: false,
            hasParallelPackage = map["hasParallelPackage"] as? Boolean ?: false
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameTickJob?.cancel()
        robotHandJob?.cancel()
        particleAnimateJob?.cancel()
        achievementsMonitorJob?.cancel()
    }
}
