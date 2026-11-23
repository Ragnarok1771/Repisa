package com.repisa.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.i18n.AppLanguage

/**
 * Primera pantalla: elegir idioma tocando la bandera. Los títulos se muestran en varios idiomas
 * porque todavía no sabemos cuál habla el usuario.
 */
@Composable
fun WelcomeScreen(onPick: (AppLanguage) -> Unit) {
    val suggested = AppLanguage.guess()
    Column(
        Modifier.fillMaxSize().roomBackground().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Repisa", color = Color(0xFFFFD27A), fontSize = 46.sp, fontWeight = FontWeight.SemiBold, fontFamily = DisplayFont)
        Text("Elige tu idioma · Choose your language · Choisissez votre langue",
            color = Color(0xFFBFA98A), fontSize = 14.sp)
        Text("Sprache wählen · Scegli la lingua · Escolha o idioma", color = Color(0xFFBFA98A), fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.widthIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageList(selected = suggested, onPick = onPick)
        }
    }
}
