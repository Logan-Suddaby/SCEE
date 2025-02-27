package de.westnordost.streetcomplete.data.maptiles

import android.content.Context
import android.os.Environment
import de.westnordost.streetcomplete.ApplicationConstants.DELETE_OLD_DATA_AFTER_DAYS
import de.westnordost.streetcomplete.Prefs
import de.westnordost.streetcomplete.util.prefs.Preferences
import okhttp3.Cache
import okhttp3.CacheControl
import java.io.File
import java.util.concurrent.TimeUnit

/** Configuration for the common cache shared by tangram-es and the map tile ("pre"-)downloader
 *  integrated into the normal map download process */
class MapTilesDownloadCacheConfig(context: Context, prefs: Preferences) {

    val cacheControl: CacheControl = CacheControl.Builder()
        .maxAge(12, TimeUnit.HOURS)
        .maxStale(DELETE_OLD_DATA_AFTER_DAYS * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS)
        .build()

    /* use separate cache control for tangram with large maxStale value to always show available
    *  map tiles when panning the map, even without (or with bad) internet connection */
    val tangramCacheControl: CacheControl = CacheControl.Builder()
        .maxAge(12, TimeUnit.HOURS)
        .maxStale(10 * 365, TimeUnit.DAYS) // ten years
        .build()

    val cache: Cache?

    init {
        val cacheDir = if (prefs.getBoolean(Prefs.PREFER_EXTERNAL_SD, false))
                context.externalCacheDirs
                    .firstOrNull { Environment.isExternalStorageRemovable(it) } ?: context.externalCacheDir
            else
                context.externalCacheDir
        val tileCacheDir: File?
        if (cacheDir != null) {
            tileCacheDir = File(cacheDir, TILE_CACHE_DIR)
            if (!tileCacheDir.exists()) tileCacheDir.mkdir()
        } else {
            tileCacheDir = null
        }

        cache = if (tileCacheDir?.exists() == true) {
            val mbs = prefs.getInt(Prefs.MAP_TILECACHE_IN_MB, 50)
            Cache(tileCacheDir, mbs * 1000L * 1000L)
        } else {
            null
        }
    }

    companion object {
        private const val TILE_CACHE_DIR = "tile_cache"
    }
}
