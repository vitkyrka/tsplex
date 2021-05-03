package `in`.rab.tsplex

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheUtil
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File

class Lexikon(context: Context) {
    private val cache: com.google.android.exoplayer2.upstream.cache.Cache
    private val httpDataSourceFactory: DataSource.Factory
    val dataSourceFactory: DataSource.Factory
    val extractorsFactory: ExtractorsFactory

    init {
        cache = SimpleCache(File(context.cacheDir, "exoplayer"),
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024))
        httpDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(context, "in.rab.tsplex"))
        dataSourceFactory = CacheDataSourceFactory(cache, httpDataSourceFactory)
        extractorsFactory = DefaultExtractorsFactory()
    }

    fun cacheVideo(url: String): Boolean {
        val upstream = httpDataSourceFactory.createDataSource()
        val dataSpec = DataSpec(Uri.parse(url))
        val counters = CacheUtil.CachingCounters()

        try {
            CacheUtil.cache(dataSpec, cache, upstream, counters)
        } catch (e: Exception) {
            return false
        }

        return counters.totalCachedBytes() == counters.contentLength
    }

    companion object {
        @Volatile
        private var INSTANCE: Lexikon? = null

        fun getInstance(context: Context): Lexikon =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Lexikon(context).also { INSTANCE = it }
                }
    }
}
