package com.example.myapplication

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.util.BodyMetrics
import com.example.myapplication.util.DateUtils
import com.example.myapplication.util.FoodAnalyzer
import com.example.myapplication.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    class Result(val items: List<FoodAnalyzer.FoodItem>, val jpeg: ByteArray) : AnalysisState()
    class Error(val kind: Kind, val detail: String = "") : AnalysisState()
    enum class Kind { NO_KEY, NETWORK, PARSE, NO_FOOD }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val userDao = db.userDao()
    private val measurementDao = db.measurementDao()
    private val healthConnectManager = HealthConnectManager(app)
    private val foodDao = FoodDatabase.get(app).foodDao()
    private val prefs = Prefs(app)

    val users = userDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeUser = userDao.getActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val measurements = activeUser.flatMapLatest { user ->
        if (user != null) measurementDao.getByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestMeasurement = activeUser.flatMapLatest { user ->
        if (user != null) measurementDao.getLatest(user.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---------- Calorias e refeições ----------

    private val goalOverride = MutableStateFlow(prefs.dailyGoal)

    val dailyGoal: StateFlow<Int> = combine(activeUser, latestMeasurement, goalOverride) { user, latest, manual ->
        if (manual > 0) {
            manual
        } else if (user == null) {
            0
        } else {
            val age = Calendar.getInstance().get(Calendar.YEAR) - user.birthYear
            val weight = latest?.weightKg ?: (user.targetWeight ?: 70f)
            val bmr = latest?.bmr ?: BodyMetrics.estimateBmr(weight, user.heightCm, age, user.gender == "M")
            BodyMetrics.dailyCalorieGoal(bmr, weight, user.targetWeight)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Meta definida manualmente (0 = automática). */
    val manualGoal: Int get() = goalOverride.value

    fun setDailyGoal(kcal: Int) {
        prefs.dailyGoal = kcal
        goalOverride.value = kcal
    }

    private val _selectedDay = MutableStateFlow(DateUtils.startOfDay(System.currentTimeMillis()))
    val selectedDay: StateFlow<Long> = _selectedDay

    fun changeDay(delta: Int) {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val next = DateUtils.startOfDay(DateUtils.addDays(_selectedDay.value, delta))
        if (next <= today) _selectedDay.value = next
    }

    val foodsForSelectedDay = combine(activeUser, _selectedDay) { user, day -> Pair(user, day) }
        .flatMapLatest { pair ->
            val user = pair.first
            val day = pair.second
            if (user != null) foodDao.getBetween(user.id, day, DateUtils.addDays(day, 1)) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodsLast7Days = activeUser.flatMapLatest { user ->
        if (user != null) {
            val from = DateUtils.addDays(DateUtils.startOfDay(System.currentTimeMillis()), -6)
            foodDao.getSince(user.id, from)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysis = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysis: StateFlow<AnalysisState> = _analysis

    fun analyzePhoto(uri: Uri) {
        val key = prefs.apiKey
        if (key.isBlank()) {
            _analysis.value = AnalysisState.Error(AnalysisState.Kind.NO_KEY)
            return
        }
        _analysis.value = AnalysisState.Loading
        viewModelScope.launch {
            try {
                val result = FoodAnalyzer.analyze(getApplication<Application>(), uri, key, Locale.getDefault().language)
                _analysis.value = if (result.items.isEmpty()) {
                    AnalysisState.Error(AnalysisState.Kind.NO_FOOD)
                } else {
                    AnalysisState.Result(result.items, result.jpeg)
                }
            } catch (e: FoodAnalyzer.AnalysisException) {
                _analysis.value = AnalysisState.Error(
                    if (e.isParse) AnalysisState.Kind.PARSE else AnalysisState.Kind.NETWORK,
                    e.message ?: ""
                )
            } catch (e: Exception) {
                _analysis.value = AnalysisState.Error(AnalysisState.Kind.NETWORK, e.message ?: "")
            }
        }
    }

    fun dismissAnalysis() {
        _analysis.value = AnalysisState.Idle
    }

    fun saveFoods(mealType: String, items: List<FoodAnalyzer.FoodItem>, jpeg: ByteArray?) {
        val user = activeUser.value ?: return
        val day = _selectedDay.value
        val now = System.currentTimeMillis()
        val timestamp = if (day == DateUtils.startOfDay(now)) now else day + 12L * 3600L * 1000L
        viewModelScope.launch {
            var photoPath: String? = null
            if (jpeg != null) {
                photoPath = withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().filesDir, "meal_photos")
                    dir.mkdirs()
                    val file = File(dir, "meal_$now.jpg")
                    file.writeBytes(jpeg)
                    file.absolutePath
                }
            }
            foodDao.insertAll(
                items.map {
                    FoodEntry(
                        userId = user.id,
                        timestamp = timestamp,
                        mealType = mealType,
                        name = it.name,
                        calories = it.calories,
                        proteinG = it.proteinG,
                        carbsG = it.carbsG,
                        fatG = it.fatG,
                        photoPath = photoPath
                    )
                }
            )
            _analysis.value = AnalysisState.Idle
        }
    }

    fun addManualFood(mealType: String, name: String, kcal: Int) {
        saveFoods(mealType, listOf(FoodAnalyzer.FoodItem(name, kcal)), null)
    }

    fun updateFood(entry: FoodEntry, name: String, kcal: Int, mealType: String) {
        viewModelScope.launch {
            foodDao.update(entry.copy(name = name, calories = kcal, mealType = mealType))
        }
    }

    fun deleteFood(entry: FoodEntry) {
        viewModelScope.launch { foodDao.delete(entry) }
    }

    // ---------- Perfis e pesagens ----------

    fun addUser(name: String, gender: String, birthYear: Int, heightCm: Float, target: Float?) {
        viewModelScope.launch {
            val id = userDao.insert(User(name = name, gender = gender, birthYear = birthYear, heightCm = heightCm, targetWeight = target))
            userDao.clearActive()
            userDao.setActive(id)
        }
    }

    fun setActiveUser(id: Long) {
        viewModelScope.launch {
            userDao.clearActive()
            userDao.setActive(id)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch { userDao.delete(user) }
    }

    fun saveMeasurement(weightKg: Float, impedance: Int? = null) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            val age = Calendar.getInstance().get(Calendar.YEAR) - user.birthYear
            val isMale = user.gender == "M"
            val bmi = BodyMetrics.calculateBmi(weightKg, user.heightCm)
            val bmr = BodyMetrics.estimateBmr(weightKg, user.heightCm, age, isMale)
            val bodyFat = BodyMetrics.estimateBodyFat(bmi, age, isMale)
            val boneKg = BodyMetrics.estimateBoneKg(weightKg, isMale)
            val muscleKg = BodyMetrics.estimateMuscleKg(weightKg, bodyFat, boneKg)
            val water = BodyMetrics.estimateWaterPercent(bodyFat)
            val visceral = BodyMetrics.estimateVisceralFat(bmi, age, isMale)

            measurementDao.insert(
                Measurement(
                    userId = user.id,
                    weightKg = weightKg,
                    bmi = bmi,
                    bodyFatPercent = bodyFat,
                    muscleMassKg = muscleKg,
                    boneMassKg = boneKg,
                    waterPercent = water,
                    visceralFat = visceral,
                    bmr = bmr,
                    impedance = impedance
                )
            )

            try {
                if (healthConnectManager.isAvailable() && healthConnectManager.hasAllPermissions()) {
                    healthConnectManager.writeMeasurement(weightKg, bodyFat)
                }
            } catch (_: Exception) { }
        }
    }

    fun deleteMeasurement(m: Measurement) {
        viewModelScope.launch { measurementDao.delete(m) }
    }
}
