package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.PlayStatus
import com.repisa.app.model.playStatus

/** Fila con los cinco estados; tocar el que ya está marcado lo quita. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPicker(game: Game, small: Boolean = false, onPick: (PlayStatus?) -> Unit) {
    val current = game.playStatus
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlayStatus.entries.forEach { st ->
            val selected = st == current
            FilterChip(
                selected = selected,
                onClick = { onPick(if (selected) null else st) },
                label = { Text("${st.icon} ${st.label}", fontSize = if (small) 11.sp else 13.sp) },
                leadingIcon = {
                    androidx.compose.foundation.layout.Box(Modifier.size(10.dp).let {
                        it.then(Modifier.background(Color(st.color), androidx.compose.foundation.shape.CircleShape))
                    })
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(st.color).copy(alpha = 0.35f),
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

/**
 * «Te faltan ~12 h para terminarlo» según HowLongToBeat (historia, o 100 % si lo estás completando),
 * o null si no aplica.
 */
@Composable
fun remainingText(game: Game): String? {
    val st = game.playStatus
    if (st != null && st != PlayStatus.BACKLOG && st != PlayStatus.PLAYING && st != PlayStatus.FINISHED) return null
    val target = (if (st == PlayStatus.FINISHED) game.hltbComplete else game.hltbMain) ?: return null
    val left = target - game.totalMinutes / 60f
    if (left < 0.5f) return null
    return stringResource(if (st == PlayStatus.FINISHED) R.string.remaining_complete else R.string.remaining_main,
        L10n.decimal(left.toDouble(), if (left < 10) 1 else 0))
}
