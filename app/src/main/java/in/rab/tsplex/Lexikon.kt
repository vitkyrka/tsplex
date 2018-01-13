package `in`.rab.tsplex

import android.content.Context
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class Lexikon {
    val client: OkHttpClient
    val dataSourceFactory: DataSource.Factory
    val extractorsFactory: ExtractorsFactory

    constructor(context: Context) {
        client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    // The server sets Cache-Control: no-cache on pages, so we need to
                    // override it to get some caching.  But we can't use a large age
                    // since the video URLs can change.
                    chain.proceed(chain.request())
                            .newBuilder()
                            .header("Cache-Control", "max-age=3600")
                            .build()
                }
                .cache(Cache(File(context.cacheDir, "okhttp"), 100 * 1024 * 1024))
                .build()

        val cache = SimpleCache(File(context.cacheDir, "exoplayer"),
                LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024))
        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(context, "in.rab.tsplex"))
        dataSourceFactory = CacheDataSourceFactory(cache, httpDataSourceFactory)
        extractorsFactory = DefaultExtractorsFactory()
    }

    companion object {
        @Volatile private var INSTANCE: Lexikon? = null

        fun getInstance(context: Context): Lexikon =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Lexikon(context).also { INSTANCE = it }
                }
    }
}
