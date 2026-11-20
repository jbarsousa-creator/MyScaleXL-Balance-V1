package com.example.myapplication.util

import androidx.annotation.StringRes
import com.example.myapplication.R

object BodyMetrics {
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    @StringRes
    fun bmiCategory(bmi: Float): Int = when {
        bmi < 18.5 -> R.string.bmi_cat_under
        bmi < 25.0 -> R.string.bmi_cat_normal
        bmi < 30.0 -> R.string.bmi_cat_over
        bmi < 35.0 -> R.string.bmi_cat_ob1
        bmi < 40.0 -> R.string.bmi_cat_ob2
        else -> R.string.bmi_cat_ob3
    }

    fun estimateBmr(weightKg: Float, heightCm: Float, age: Int, isMale: Boolean): Int {
        return if (isMale) {
            (88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age)).toInt()
        } else {
            (447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age)).toInt()
        }
    }

    fun estimateBodyFat(bmi: Float, age: Int, isMale: Boolean): Float {
        val base = if (isMale) 1.20f * bmi + 0.23f * age - 16.2f
                   else 1.20f * bmi + 0.23f * age - 5.4f
        return base.coerceIn(5f, 50f)
    }

    // ---- Estimativas adicionais (fórmulas; sem impedância real) ----

    fun estimateWaterPercent(bodyFat: Float): Float =
        ((100f - bodyFat) * 0.73f).coerceIn(35f, 70f)

    fun estimateBoneKg(weightKg: Float, isMale: Boolean): Float =
        (weightKg * (if (isMale) 0.045f else 0.038f)).coerceIn(1.5f, 5f)

    fun estimateMuscleKg(weightKg: Float, bodyFat: Float, boneKg: Float): Float =
        ((weightKg * (1f - bodyFat / 100f) - boneKg) * 0.80f).coerceAtLeast(0f)

    fun estimateVisceralFat(bmi: Float, age: Int, isMale: Boolean): Float =
        ((bmi - 18f) * 0.9f + age * 0.08f + (if (isMale) 1.5f else 0f)).coerceIn(1f, 30f)

    fun idealWeightRange(heightCm: Float): Pair<Float, Float> {
        val h = heightCm / 100f
        return Pair(18.5f * h * h, 24.9f * h * h)
    }

    @StringRes
    fun fatCategory(fat: Float, isMale: Boolean): Int {
        val limits = if (isMale) Triple(8f, 20f, 25f) else Triple(15f, 30f, 35f)
        return when {
            fat < limits.first -> R.string.level_low
            fat < limits.second -> R.string.level_normal
            fat < limits.third -> R.string.level_high
            else -> R.string.level_very_high
        }
    }

    @StringRes
    fun visceralCategory(v: Float): Int = when {
        v < 10f -> R.string.level_healthy
        v < 15f -> R.string.level_high
        else -> R.string.level_very_high
    }

    /** Meta diária de calorias: metabolismo basal x 1.4 (atividade ligeira), ajustada ao objetivo. */
    fun dailyCalorieGoal(bmr: Int, weightKg: Float, targetKg: Float?): Int {
        val tdee = bmr * 1.4f
        val adjusted = when {
            targetKg != null && targetKg < weightKg - 0.5f -> tdee * 0.85f
            targetKg != null && targetKg > weightKg + 0.5f -> tdee * 1.10f
            else -> tdee
        }
        return (Math.round(adjusted / 10f) * 10).coerceAtLeast(1200)
    }
}
