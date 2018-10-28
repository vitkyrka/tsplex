package `in`.rab.tsplex

import okhttp3.*
import java.io.File
import java.io.IOException

class Scraper(private var client: OkHttpClient) {

    constructor(cacheDir: File) : this(
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
                    .cache(Cache(cacheDir, 100 * 1024 * 1024))
                    .build()
    )

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
}
