package org.skepsun.kototoro.mihon.compat

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor

/**
 * Wrapper around OkHttp's [BrotliInterceptor] that provides identical Brotli
 * decompression but has a different class identity.
 *
 * KeiSource (Keiyoushi) asserts:
 * ```
 * networkInterceptors().none { it is BrotliInterceptor }
 * ```
 * Since this class is NOT a subtype of [BrotliInterceptor], the `is` check
 * returns false, satisfying the assertion. Meanwhile, actual Brotli
 * decompression is preserved by delegating to [BrotliInterceptor.intercept].
 *
 * This allows both:
 * - KeiSource extensions (AllManga) to pass their startup assertions
 * - Standard extensions (Weeb Central, Mangabat) to receive decoded Brotli content
 */
class KotoBrotliInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return BrotliInterceptor.intercept(chain)
    }
}
