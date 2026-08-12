package com.ascend.feature.ascended

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.presentation.AscendedStatusField

/*
 * The production "Your Ascended" page. Shows the player's figure — their real class art (berserker /
 * monk / mage) or the neutral base body when unbound — on the self-contained dark field. On first
 * open (no cosmetic body base chosen yet) it presents a lightweight body-base chooser. Presentation-
 * only: it reads class + the cosmetic body-base preference and can persist the body-base choice, but
 * never mutates progression. Self-contained styling — no dependency on any prototype code.
 */
private val BG = Color(0xFF04050B)
private val INK = Color(0xFFEAF0FF)
private val MUTED = Color(0xFF8A93B5)
private val ACCENT = Color(0xFFA88BFF)

@Composable
fun YourAscendedScreen(viewModel: YourAscendedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    YourAscendedContent(state = state, onChooseBodyBase = viewModel::chooseBodyBase)
}

@Composable
fun YourAscendedContent(
    state: YourAscendedUiState,
    onChooseBodyBase: (BodyBase) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reChoose by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
        when {
            state.loading -> CircularProgressIndicator(color = ACCENT)
            state.needsBodyBaseChoice || reChoose ->
                BodyBaseChooser(
                    onChoose = {
                        onChooseBodyBase(it)
                        reChoose = false
                    },
                )
            else ->
                AscendedContent(
                    state = state,
                    onChangeBaseForm = { reChoose = true },
                )
        }
    }
}

@Composable
private fun AscendedContent(
    state: YourAscendedUiState,
    onChangeBaseForm: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("YOUR ASCENDED", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text(state.figureClass?.displayName ?: "Unbound", color = INK, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(state.bodyBase?.label ?: "", color = ACCENT, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        AscendedStatusField(
            ascendedClass = state.figureClass,
            bodyBase = state.bodyBase ?: BodyBase.MALE,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.62f),
            reducedMotion = state.reducedMotion,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onChangeBaseForm) {
            Text("Change base form", color = INK)
        }
    }
}

@Composable
private fun BodyBaseChooser(onChoose: (BodyBase) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("YOUR ASCENDED", color = MUTED, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))
        Text("Choose your base form.", color = INK, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BodyBase.entries.forEach { base ->
                Button(
                    onClick = { onChoose(base) },
                    modifier = Modifier.widthIn(min = 120.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                ) {
                    Text(base.label, color = BG, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "This only changes your Ascended's visual form. You can change it later.",
            color = MUTED,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}
