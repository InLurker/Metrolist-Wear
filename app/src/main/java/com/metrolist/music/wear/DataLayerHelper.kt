package com.metrolist.music.wear

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.CaseMap.Title
import android.util.Size
import androidx.media3.common.MediaItem
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.wear.enumerated.DataLayerPathEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoilApi::class)
@Singleton
class DataLayerHelper @Inject constructor(context: Context) {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val commandListener = AtomicReference<(String) -> Unit>()
    @Inject
    lateinit var database: MusicDatabase

    lateinit var musicService: MusicService

    private val coil = context.imageLoader

    fun initializeMusicService(musicService: MusicService) {
        this.musicService = musicService
    }

    fun sendMediaInfo(
        title: String,
        artist: String,
        album: String,
        artworkUrl: String,
        isPlaying: Boolean
    ) {
        scope.launch {
            try {
                val putDataRequest = PutDataMapRequest.create(DataLayerPathEnum.SONG_INFO.path).apply {
                    dataMap.putString("trackName", title)
                    dataMap.putString("artistName", artist)
                    dataMap.putString("albumName", album)
                    dataMap.putString("artworkUrl", artworkUrl)
                    dataMap.putAsset("artworkAsset", fetchBitmapByteFromCoil(artworkUrl, 450))
                    dataMap.putBoolean("isPlaying", isPlaying)
                }.asPutDataRequest()

                // Send data through Data Layer
                dataClient.putDataItem(putDataRequest).addOnSuccessListener {
                    Timber.tag("DataLayerHelper").d("Song info sent via Data Layer: ${putDataRequest.data}")
                }.addOnFailureListener { e ->
                    Timber.tag("DataLayerHelper").e(e, "Failed to send song info via Data Layer")
                }
            } catch (e: Exception) {
                Timber.tag("DataLayerHelper").e(e, "Failed to send song info")
            }
        }
    }

    private fun sendLikedAlbums() {
        scope.launch {
            try {
                // Collect the albumsLiked flow
                database.albumsLiked(AlbumSortType.CREATE_DATE, true).collect { albums ->

                    if (albums.isEmpty()) {
                        Timber.d("No liked albums found to send.")
                        return@collect
                    }

                    // Convert the list of albums into a DataMapArrayList
                    val albumDataMapList = ArrayList<DataMap>()
                    albums.forEach { album ->
                        val albumDataMap = DataMap().apply {
                            putString("id", album.album.id)
                            putString("title", album.album.title)
                            putString("artist", album.artists.map { it.name }.joinToString(", "))
                            putString("artworkUrl", album.album.thumbnailUrl ?: "")
                            putAsset("artworkBitmap", fetchBitmapByteFromCoil(album.album.thumbnailUrl, 100))
                            putInt("songCount", album.album.songCount)
                        }
                        albumDataMapList.add(albumDataMap)
                    }

                    // Prepare and send the DataMapArrayList
                    val putDataRequest = PutDataMapRequest.create(DataLayerPathEnum.ALBUM_INFO.path).apply {
                        dataMap.putDataMapArrayList("albums", albumDataMapList)
                    }.asPutDataRequest()

                    // Send the data through the Data Layer
                    dataClient.putDataItem(putDataRequest).addOnSuccessListener {
                        Timber.d("Successfully sent liked album details.")
                    }.addOnFailureListener { e ->
                        Timber.e(e, "Failed to send liked album details")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while sending liked albums")
            }
        }
    }

    private fun sendBookmarkedArtists() {
        scope.launch {
            val artists = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
                .map { list -> list.map { it.artist.name } }
                .firstOrNull() ?: emptyList()

            val putDataRequest = PutDataMapRequest.create(DataLayerPathEnum.ALBUM_INFO.path).apply {
                dataMap.putStringArrayList("artists", ArrayList(artists))
            }.asPutDataRequest()

            dataClient.putDataItem(putDataRequest).addOnSuccessListener { _ ->
                Timber.tag("DataLayerHelper").d("Bookmarked artists sent via Data Layer")
            }.addOnFailureListener { e ->
                Timber.tag("DataLayerHelper").e(e, "Failed to send bookmarked artists via Data Layer")
            }
        }
    }


    fun sendAlbumDetails(albumId: String) {
        scope.launch {
            try {
                // Fetch album with songs from the database
                val albumWithSongs = database.albumWithSongs(albumId).firstOrNull()

                if (albumWithSongs != null) {
                    val album = albumWithSongs.album
                    val songs = albumWithSongs.songs

                    // Prepare data to be sent
                    val putDataRequest = PutDataMapRequest.create(DataLayerPathEnum.ALBUM_INFO.path).apply {
                        dataMap.putString("albumTitle", album.title)
                        dataMap.putString("albumYear", album.year?.toString() ?: "")
                        dataMap.putString("albumThumbnail", album.thumbnailUrl ?: "")
                        dataMap.putString("albumId", album.id)
                        dataMap.putStringArrayList("songTitles", ArrayList(songs.map { it.song.title }))
                        dataMap.putStringArrayList("songIds", ArrayList(songs.map { it.song.id }))
                    }.asPutDataRequest()

                    // Send data through Data Layer
                    dataClient.putDataItem(putDataRequest).addOnSuccessListener {
                        Timber.tag("DataLayerHelper").d("Album details sent via Data Layer for albumId: $albumId")
                    }.addOnFailureListener { e ->
                        Timber.tag("DataLayerHelper").e(e, "Failed to send album details via Data Layer")
                    }
                } else {
                    Timber.tag("DataLayerHelper").e("Album with ID $albumId not found in the database.")
                }
            } catch (e: Exception) {
                Timber.tag("DataLayerHelper").e(e, "Failed to send album details")
            }
        }
    }



    fun fetchBitmapByteFromCoil(url: String?, size: Int): Asset {
        val bitmap = url?.let {
            coil.diskCache?.openSnapshot(it)?.use {
                val imageFile = it.data.toFile().readBytes()
                if (imageFile.isEmpty()) {
                    null
                } else {
                    resizeBitmap(
                        size,
                        BitmapFactory.decodeByteArray(imageFile, 0, imageFile.size)
                    )
                }
            }
        }

        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return Asset.createFromBytes(stream.toByteArray())
    }
}

fun resizeBitmap(maxDimension: Int, bitmap: Bitmap): Bitmap {
    if (maxDimension <= 0) return bitmap

    var width = bitmap.width
    var height = bitmap.height

    // Avoid unnecessary resizing
    if (width <= maxDimension && height <= maxDimension) {
        return bitmap
    }

    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 1) {
        // Landscape
        width = maxDimension
        height = (width / bitmapRatio).toInt()
    } else {
        // Portrait or square
        height = maxDimension
        width = (height * bitmapRatio).toInt()
    }

    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}