package com.ascend.feature.ascended.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage

private val INK = Color(0xFF23262E)
private val MUTED = Color(0xFF6A6E78)

/**
 * A "Your Ascended" figure: the imported class artwork (per class + body base, with an optional
 * per-stage variant) shown as a drawable, with the aura behind and the Perception manifestation in
 * front. When the art has not been supplied yet, a labelled placeholder names the exact drawable to
 * drop in — so the system is reviewable now and becomes the reference art the moment images land.
 */
@Composable
fun AscendedFigure(
    ascendedClass: AscendedClass?,
    bodyBase: BodyBase,
    modifier: Modifier = Modifier,
    stage: EvolutionStage = EvolutionStage.BASE,
    perceptionLevel: Int = 0,
) {
    val context = LocalContext.current
    val bodyName = AscendedArt.baseBodyResourceName(bodyBase)
    // Only a bound class has class-specific art; an unbound player resolves straight to the base body.
    val stagedName = ascendedClass?.let { AscendedArt.figureResourceName(it, bodyBase, stage) }
    val classBaseName = ascendedClass?.let { AscendedArt.figureResourceName(it, bodyBase) }
    val stagedId = remember(stagedName) { stagedName?.let { AscendedArt.resolveDrawable(context, it) } ?: 0 }
    val classBaseId = remember(classBaseName) { classBaseName?.let { AscendedArt.resolveDrawable(context, it) } ?: 0 }
    val bodyId = remember(bodyName) { AscendedArt.resolveDrawable(context, bodyName) }
    // Resolve most-specific → least: staged class art → class base art → neutral body → placeholder.
    val resolvedId = listOf(stagedId, classBaseId, bodyId).firstOrNull { it != 0 } ?: 0
    val label = ascendedClass?.displayName ?: "Base"

    Box(modifier, contentAlignment = Alignment.Center) {
        AuraOverlay(stage, Modifier.matchParentSize())
        if (resolvedId != 0) {
            Image(
                painter = painterResource(resolvedId),
                contentDescription = "$label ${bodyBase.label} figure",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            FigurePlaceholder(label, bodyBase, classBaseName ?: bodyName, bodyName, Modifier.fillMaxSize())
        }
        PerceptionOverlay(perceptionLevel, Modifier.matchParentSize())
    }
}

@Composable
private fun FigurePlaceholder(
    label: String,
    bodyBase: BodyBase,
    classResourceName: String,
    bodyResourceName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .border(1.dp, Color(0x556A6E78), RoundedCornerShape(10.dp))
            .padding(16.dp)
            .semantics { contentDescription = "$label ${bodyBase.label} figure placeholder" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$label · ${bodyBase.label}", color = INK, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("Add figure art:", color = MUTED, fontSize = 12.sp)
            Text("$classResourceName.png", color = INK, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("or base body: $bodyResourceName.png", color = MUTED, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text("app/src/debug/res/drawable/", color = MUTED, fontSize = 10.sp)
        }
    }
}
