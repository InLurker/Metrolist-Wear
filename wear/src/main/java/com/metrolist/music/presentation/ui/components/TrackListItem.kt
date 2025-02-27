package com.metrolist.music.presentation.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.metrolist.music.common.models.TrackInfo

@Composable
fun TrackListItem(
    trackInfo: TrackInfo,
    isPlaying: Boolean,
    passiveColor: Color,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .background(
                color = if (isPlaying) activeColor else passiveColor
            )
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(trackInfo.artworkUrl)
                .crossfade(1000)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .memoryCacheKey(trackInfo.artworkUrl)
                .diskCacheKey(trackInfo.artworkUrl)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Album Artwork",
            modifier = Modifier
                .padding(8.dp)
                .height(32.dp)
                .width(32.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = trackInfo.trackName,
                fontSize = 10.sp,
                maxLines = 1,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
            val albumArtistText = listOfNotNull(
                trackInfo.artistName.takeIf { it.isNotEmpty()},
                trackInfo.albumName.takeIf { it.isNotEmpty() }
            ).joinToString(" - ")
            albumArtistText.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = albumArtistText,
                    fontSize = 8.sp,
                    maxLines = 1,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}