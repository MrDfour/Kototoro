package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Wraps unchecked exceptions from downstream interceptors into [IOException].
 *
 * OkHttp's Dispatcher crashes the app if an interceptor throws an unchecked exception.
 * Required by KeiSource (Keiyoushi extensions) which asserts its presence by simpleName.
 */
class UncaughtExceptionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            return chain.proceed(chain.request())
        } catch (e: Throwable) {
            if (e is IOException) throw e
            throw IOException(e.message, e)
        }
    }
}
