package com.metrolist.music.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text


@Composable
fun MarqueeText(
    text: String,
    fontColor: Color = MaterialTheme.colors.onSurface,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "",
        modifier = modifier,
    ) { title ->
        Text(
            text = title,
            color = fontColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(),
        )
    }
}

//            AnimatedContent(
//                targetState = mediaMetadata.title,
//                transitionSpec = { fadeIn() togetherWith fadeOut() },
//                label = "",
//            ) { title ->
//                Text(
//                    text = title,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.basicMarquee(),
//                )
//            }