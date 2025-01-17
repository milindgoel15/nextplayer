package dev.anilbeesetti.nextplayer.core.data

import dev.anilbeesetti.nextplayer.core.common.di.ApplicationScope
import dev.anilbeesetti.nextplayer.core.common.extensions.prettyName
import dev.anilbeesetti.nextplayer.core.database.dao.DirectoryDao
import dev.anilbeesetti.nextplayer.core.database.dao.MediumDao
import dev.anilbeesetti.nextplayer.core.database.entities.DirectoryEntity
import dev.anilbeesetti.nextplayer.core.database.entities.MediumEntity
import dev.anilbeesetti.nextplayer.core.media.mediasource.MediaSource
import dev.anilbeesetti.nextplayer.core.media.model.MediaVideo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class MediaSynchronizer @Inject constructor(
    private val mediumDao: MediumDao,
    private val directoryDao: DirectoryDao,
    private val mediaSource: MediaSource,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private var mediaSyncingJob: Job? = null

    fun sync(scope: CoroutineScope = applicationScope) {
        if (mediaSyncingJob != null) return
        mediaSyncingJob = mediaSource.getMediaVideosFlow().onEach { media ->
            Timber.d("Syncing ${media.size} media ${this.hashCode()}")
            scope.launch { updateDirectories(media) }
            scope.launch { updateMedia(media) }
        }.launchIn(scope)
    }

    private suspend fun updateDirectories(media: List<MediaVideo>) = withContext(
        Dispatchers.Default
    ) {
        val directories = media.groupBy { File(it.data).parentFile!! }.map { (file, videos) ->
            DirectoryEntity(
                path = file.path,
                name = file.prettyName,
                mediaCount = videos.size,
                size = videos.sumOf { it.size },
                modified = file.lastModified()
            )
        }
        directoryDao.upsertAll(directories)

        val currentDirectoryPaths = directories.map { it.path }

        val unwantedDirectories = directoryDao.getAll().first()
            .map { it.path }
            .filterNot { it in currentDirectoryPaths }

        directoryDao.delete(unwantedDirectories)
    }

    private suspend fun updateMedia(media: List<MediaVideo>) = withContext(Dispatchers.Default) {
        val mediumEntities = media.map {
            val file = File(it.data)
            val mediumEntity = mediumDao.get(it.data)
            MediumEntity(
                path = it.data,
                uriString = it.uri.toString(),
                name = file.nameWithoutExtension,
                parentPath = file.parent!!,
                modified = it.dateModified,
                size = it.size,
                width = it.width,
                height = it.height,
                duration = it.duration,
                mediaStoreId = it.id,
                playbackPosition = mediumEntity?.playbackPosition ?: 0,
                audioTrackIndex = mediumEntity?.audioTrackIndex,
                subtitleTrackIndex = mediumEntity?.subtitleTrackIndex,
                playbackSpeed = mediumEntity?.playbackSpeed
            )
        }

        mediumDao.upsertAll(mediumEntities)

        val currentMediaPaths = mediumEntities.map { it.path }

        val unwantedMedia = mediumDao.getAll().first()
            .map { it.path }
            .filterNot { it in currentMediaPaths }

        mediumDao.delete(unwantedMedia)
    }
}
