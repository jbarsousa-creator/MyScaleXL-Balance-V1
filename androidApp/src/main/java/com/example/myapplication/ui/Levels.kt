package com.example.myapplication.ui

import androidx.compose.ui.graphics.Color

/** Verde = dentro do normal, amarelo = começa a afastar-se, laranja = afastado, vermelho = muito afastado. */
enum class Level(val color: Color, val onColor: Color) {
    GOOD(Color(0xFF52C77D), Color(0xFF0B2A18)),
    WATCH(Color(0xFFF6D04D), Color(0xFF2B2200)),
    HIGH(Color(0xFFF39A3B), Color(0xFF2B1600)),
    ALERT(Color(0xFFD93A3A), Color.White)
}

/** Limites usados para colorir cada métrica. Altera aqui se quiseres outros intervalos. */
object HealthLevels {

    // Distância (em pontos) ao intervalo normal: 0 = dentro, até 5 = amarelo, até 10 = laranja, acima = vermelho
    private fun byDistance(d: Float): Level = when {
        d <= 0f -> Level.GOOD
        d <= 5f -> Level.WATCH
        d <= 10f -> Level.HIGH
        else -> Level.ALERT
    }

    fun bmi(bmi: Float): Level = when {
        bmi < 16f -> Level.ALERT
        bmi < 17f -> Level.HIGH
        bmi < 18.5f -> Level.WATCH
        bmi < 25f -> Level.GOOD
        bmi < 30f -> Level.WATCH
        bmi < 35f -> Level.HIGH
        else -> Level.ALERT
    }

    /** Gordura corporal (%): normal 8-20 (homem) ou 15-30 (mulher). */
    fun bodyFat(fat: Float, isMale: Boolean): Level {
        val low = if (isMale) 8f else 15f
        val high = if (isMale) 20f else 30f
        val d = when {
            fat < low -> low - fat
            fat > high -> fat - high
            else -> 0f
        }
        return byDistance(d)
    }

    /** Gordura visceral (índice 1-30): saudável até 9. */
    fun visceral(v: Float): Level = when {
        v < 10f -> Level.GOOD
        v < 13f -> Level.WATCH
        v < 15f -> Level.HIGH
        else -> Level.ALERT
    }

    /** Músculo (% do peso): normal a partir de 58% (homem) ou 50% (mulher). */
    fun muscle(musclePct: Float, isMale: Boolean): Level {
        val normal = if (isMale) 58f else 50f
        return byDistance(normal - musclePct)
    }

    /** Osso (% do peso): normal entre 3% e 6%. */
    fun bone(bonePct: Float): Level {
        val d = when {
            bonePct < 3f -> 3f - bonePct
            bonePct > 6f -> bonePct - 6f
            else -> 0f
        }
        return when {
            d <= 0f -> Level.GOOD
            d <= 1f -> Level.WATCH
            d <= 2f -> Level.HIGH
            else -> Level.ALERT
        }
    }

    /** Água (%): normal 50-65 (homem) ou 45-60 (mulher). */
    fun water(water: Float, isMale: Boolean): Level {
        val low = if (isMale) 50f else 45f
        val high = if (isMale) 65f else 60f
        val d = when {
            water < low -> low - water
            water > high -> water - high
            else -> 0f
        }
        return byDistance(d)
    }
}
