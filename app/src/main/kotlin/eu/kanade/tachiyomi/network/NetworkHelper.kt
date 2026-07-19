package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient
import okhttp3.CookieJar

/**
 * Mihon-compatible NetworkHelper class.
 * Provides access to OkHttpClient for extensions.
 */
open class NetworkHelper {
    
    /**
     * The default OkHttpClient with CloudFlare bypassing.
     */
    open val client: OkHttpClient
        get() = throw UnsupportedOperationException("client not implemented in base NetworkHelper")

    /**
     * Cookie jar used by the client.
     */
    open val cookieJar: CookieJar
        get() = throw UnsupportedOperationException("cookieJar not implemented in base NetworkHelper")
    
    /**
     * @deprecated Since extension-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare by default")
    open val cloudflareClient: OkHttpClient
        get() = client
    
    /**
     * Returns the default user agent string.
     */
    open fun defaultUserAgentProvider(): String {
        throw UnsupportedOperationException("defaultUserAgentProvider not implemented in base NetworkHelper")
    }
}
