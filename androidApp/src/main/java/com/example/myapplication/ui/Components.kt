package com.example.myapplication.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AppHeader(title: String, subtitle: String?, onSettingsClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun CalorieSummary(goal: Int, consumed: Int, modifier: Modifier = Modifier) {
    val remaining = goal - consumed
    val over = remaining < 0
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val accent = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.cal_today), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (over) stringResource(R.string.cal_over_fmt, -remaining) else stringResource(R.string.cal_left_fmt, remaining),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.cal_eaten), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.kcal_value, consumed), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.cal_goal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.kcal_value, goal), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FoodThumb(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) {
        if (path == null) {
            null
        } else {
            try {
                val opts = BitmapFactory.Options()
                opts.inSampleSize = 4
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(container).padding(14.dp)
    ) {
        Text(label, color = contentColor.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = contentColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(hint, color = contentColor.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
    }
}

/** Escala do mostrador: janela de 50 kg que contém o peso atual. */
fun gaugeRange(weight: Float): Pair<Float, Float> {
    val lo = (floor(weight / 10f) * 10f - 20f).coerceAtLeast(0f)
    return Pair(lo, lo + 50f)
}

@Composable
fun WeightGauge(weight: Float, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = MaterialTheme.colorScheme.primary
    val needle = MaterialTheme.colorScheme.onSurface
    val (lo, hi) = gaugeRange(weight)
    val frac = ((weight - lo) / (hi - lo)).coerceIn(0f, 1f)

    Canvas(modifier) {
        val stroke = 16.dp.toPx()
        val diameter = min(size.width - stroke, (size.height - stroke / 2f) * 2f)
        val topLeft = Offset((size.width - diameter) / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(track, 180f, 180f, false, topLeft, arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawArc(progress, 180f, 180f * frac, false, topLeft, arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val cx = size.width / 2f
        val cy = topLeft.y + diameter / 2f
        val angle = Math.toRadians((180f + 180f * frac).toDouble())
        val len = diameter / 2f - stroke * 1.6f
        val end = Offset(cx + (len * cos(angle)).toFloat(), cy + (len * sin(angle)).toFloat())
        drawLine(needle, Offset(cx, cy), end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(needle, 8.dp.toPx(), Offset(cx, cy))
    }
}

@Composable
fun BmiScale(bmi: Float, modifier: Modifier = Modifier) {
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height(34.dp)) {
        val minV = 15f
        val maxV = 45f
        val bounds = listOf(15f, 18.5f, 25f, 30f, 35f, 40f, 45f)
        val colors = listOf(
            Color(0xFF81D4FA), Color(0xFF66BB6A), Color(0xFFFDD835),
            Color(0xFFFFA726), Color(0xFFEF6C00), Color(0xFFE53935)
        )
        val barTop = 14.dp.toPx()
        val barH = 14.dp.toPx()
        for (i in colors.indices) {
            val x0 = (bounds[i] - minV) / (maxV - minV) * size.width
            val x1 = (bounds[i + 1] - minV) / (maxV - minV) * size.width
            drawRect(colors[i], topLeft = Offset(x0, barTop), size = Size(x1 - x0, barH))
        }
        val mx = ((bmi.coerceIn(minV, maxV) - minV) / (maxV - minV)) * size.width
        val p = Path()
        p.moveTo(mx, barTop - 1.dp.toPx())
        p.lineTo(mx - 7.dp.toPx(), 0f)
        p.lineTo(mx + 7.dp.toPx(), 0f)
        p.close()
        drawPath(p, marker)
    }
}

@Composable
fun CompositionPie(fatKg: Float, muscleKg: Float, boneKg: Float, totalKg: Float, modifier: Modifier = Modifier) {
    val other = (totalKg - fatKg - muscleKg - boneKg).coerceAtLeast(0f)
    val parts = listOf(
        Pair(fatKg, BwColors.Fat),
        Pair(muscleKg, BwColors.Muscle),
        Pair(boneKg, BwColors.Bone),
        Pair(other, BwColors.Other)
    )
    var sum = 0f
    parts.forEach { sum += it.first }
    val total = sum.coerceAtLeast(0.001f)

    Canvas(modifier) {
        val d = min(size.width, size.height)
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        var start = -90f
        parts.forEach { part ->
            val sweep = part.first / total * 360f
            drawArc(part.second, start, sweep, true, topLeft, Size(d, d))
            start += sweep
        }
    }
}

@Composable
fun WeightChart(weights: List<Float>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = line.copy(alpha = 0.15f)
    Canvas(modifier.padding(8.dp)) {
        if (weights.size < 2) return@Canvas
        val minW = weights.min()
        val maxW = weights.max()
        val range = (maxW - minW).coerceAtLeast(1f)
        val stepX = size.width / (weights.size - 1)
        val h = size.height

        val path = Path()
        val area = Path()
        weights.forEachIndexed { i, w ->
            val x = i * stepX
            val y = h - ((w - minW) / range) * h
            if (i == 0) {
                path.moveTo(x, y)
                area.moveTo(x, h)
                area.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(size.width, h)
        area.close()

        drawPath(area, fill)
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        weights.forEachIndexed { i, w ->
            val y = h - ((w - minW) / range) * h
            drawCircle(line, 4.dp.toPx(), Offset(i * stepX, y))
        }
    }
}
