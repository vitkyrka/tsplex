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
import okhttp3.*
import java.io.File
import java.io.IOException

class Lexikon {
    private val client: OkHttpClient
    private val cache: com.google.android.exoplayer2.upstream.cache.Cache
    private val httpDataSourceFactory: DataSource.Factory
    val dataSourceFactory: DataSource.Factory
    val extractorsFactory: ExtractorsFactory

    constructor(context: Context) {
        client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    // The server sets Cache-Control: no-cache on pages, so we need to
                    // override it.  We force a refetch from network if any of the linked
                    // videos is broken, since they have moved at least once in the past.
                    chain.proceed(chain.request())
                            .newBuilder()
                            .header("Cache-Control", "max-age=31536000")
                            .build()
                }
                .cache(Cache(File(context.cacheDir, "okhttp"), 100 * 1024 * 1024))
                .build()

        cache = SimpleCache(File(context.cacheDir, "exoplayer"),
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024))
        httpDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(context, "in.rab.tsplex"))
        dataSourceFactory = CacheDataSourceFactory(cache, httpDataSourceFactory)
        extractorsFactory = DefaultExtractorsFactory()
    }

    fun getSignPage(id: Int, forceNetwork: Boolean): String? {
        val number = "%05d".format(id)
        var builder = Request.Builder()
                .url("https://teckensprakslexikon.su.se/ord/" + number)

        if (forceNetwork) {
            builder = builder.cacheControl(CacheControl.FORCE_NETWORK)
        }

        val request = builder.build()
        var response: Response? = null

        return try {
            response = client.newCall(request).execute()
            response.body()?.string()
        } catch (e: IOException) {
            null
        } finally {
            response?.body()?.close()
        }
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

    fun isDeadLink(videoUrl: String): Boolean {
        val request = Request.Builder()
                .url(videoUrl)
                .head()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

        return try {
            client.newCall(request).execute().code() == 404
        } catch (e: IOException) {
            false
        }
    }

    companion object {
        @Volatile private var INSTANCE: Lexikon? = null

        fun getInstance(context: Context): Lexikon =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Lexikon(context).also { INSTANCE = it }
                }
    }
}
