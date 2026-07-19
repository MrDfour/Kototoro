package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.util.awaitSingle
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.skepsun.kototoro.mihon.compat.KotoNetworkHelper
import org.skepsun.kototoro.mihon.compat.MihonRequestContext
import org.skepsun.kototoro.parsers.model.ContentSource
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * A simple implementation for sources from a website.
 * Ported from Mihon source-api for extension compatibility.
 */
@Suppress("unused")
abstract class HttpSource : CatalogueSource {

    /**
     * Network service.
     */
    protected val network: NetworkHelper by injectLazy()

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    abstract val baseUrl: String

    /**
     * Version id used to generate the source id. If the site completely changes and urls are
     * incompatible, you may increase this value and it'll be considered as a new source.
     */
    open val versionId = 1

    /**
     * ID of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string `"${name.lowercase()}/$lang/$versionId"`.
     */
    override val id by lazy { generateId(name, lang, versionId) }

    /**
     * Headers used for requests.
     */
    val headers: Headers by lazy { headersBuilder().build() }

    /**
     * Default network client for doing requests.
     *
     * Legacy Mihon sources historically received Brotli decoding through
     * NetworkHelper.cloudflareClient. KeiSource overrides this property and
     * uses the Brotli-free NetworkHelper.client required by its own
     * CompressionInterceptor contract.
     */
    open val client: OkHttpClient
        get() = network.client

    /**
     * Generates a unique ID for the source.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
    }

    /**
     * Visible name of the source.
     */
    override fun toString() = "$name (${lang.uppercase()})"

    /**
     * URL opened when browsing the source in a WebView.
     */
    open fun getHomeUrl(): String = baseUrl

    // ======== Popular manga ========

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return legacyFetch(
            request = { popularMangaRequest(page) },
            parser = ::popularMangaParse,
        )
    }



    protected abstract fun popularMangaRequest(page: Int): Request

    protected abstract fun popularMangaParse(response: Response): MangasPage

    // ======== Search manga ========

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> {
        return legacyFetch(
            request = { searchMangaRequest(page, query, filters) },
            parser = ::parseSearchResponse,
        )
    }



    protected abstract fun searchMangaRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request

    protected abstract fun searchMangaParse(response: Response): MangasPage

    private fun parseSearchResponse(response: Response): MangasPage {
        val responseBody = response.body.string()
        val contentType = response.body.contentType()
        val parseResponse = response.newBuilder()
            .body(responseBody.toResponseBody(contentType))
            .build()
        return runCatching {
            searchMangaParse(parseResponse)
        }.getOrElse { error ->
            val fallbackResponse = response.newBuilder()
                .body(responseBody.toResponseBody(contentType))
                .build()
            parseSearchRedirectedToDetails(fallbackResponse, error)
        }
    }

    private fun parseSearchRedirectedToDetails(response: Response, error: Throwable): MangasPage {
        val finalUrl = response.header(KotoNetworkHelper.WEBVIEW_FINAL_URL_HEADER)
            ?: response.request.url.toString()
        val finalPath = runCatching { URI(finalUrl.replace(" ", "%20")).path.orEmpty() }.getOrDefault("")
        if (!response.request.url.encodedPath.startsWith("/search/") || !finalPath.startsWith("/detail/")) {
            throw error
        }
        val manga = try {
            mangaDetailsParse(response).apply {
                setUrlWithoutDomain(finalUrl)
                initialized = true
            }
        } catch (_: Throwable) {
            SManga.create().apply {
                setUrlWithoutDomain(finalUrl)
                title = finalPath.substringAfterLast('/').substringBefore('.').ifBlank { finalUrl }
                initialized = true
            }
        }
        return MangasPage(listOf(manga), hasNextPage = false)
    }

    // ======== Latest updates ========

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return legacyFetch(
            request = { latestUpdatesRequest(page) },
            parser = ::latestUpdatesParse,
        )
    }



    protected abstract fun latestUpdatesRequest(page: Int): Request

	protected abstract fun latestUpdatesParse(response: Response): MangasPage

	// ======== Related manga ========

	override val supportsRelatedMangas: Boolean
		get() = false

	protected open fun relatedMangaListRequest(manga: SManga): Request {
		throw UnsupportedOperationException("Related manga request is not implemented")
	}

	protected open fun relatedMangaListParse(response: Response): List<SManga> {
		throw UnsupportedOperationException("Related manga parser is not implemented")
	}

    // ======== Content details ========

    @Suppress("DEPRECATION")
    @Deprecated("Use the combined suspend API instead", replaceWith = ReplaceWith("getMangaUpdate"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return legacyFetch(
            request = { mangaDetailsRequest(manga) },
            parser = { response -> mangaDetailsParse(response).apply { initialized = true } },
        )
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    open fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    // ======== Chapter list ========

    @Suppress("DEPRECATION")
    @Deprecated("Use the combined suspend API instead", replaceWith = ReplaceWith("getMangaUpdate"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return legacyFetch(
            request = { chapterListRequest(manga) },
            parser = ::chapterListParse,
        )
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun chapterListRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()

    // ======== Page list ========

    @Suppress("DEPRECATION")
    @Deprecated("Use the suspend API instead", ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return legacyFetch(
            request = { pageListRequest(chapter) },
            parser = ::pageListParse,
        )
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()

    // ======== Image URL ========

    @Suppress("DEPRECATION")
    open suspend fun getImageUrl(page: Page): String = fetchImageUrl(page).awaitSingle()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    open fun fetchImageUrl(page: Page): Observable<String> {
        return legacyFetch(
            request = { imageUrlRequest(page) },
            parser = ::imageUrlParse,
        )
    }

    open fun imageUrlRequest(page: Page): Request {
        return GET(page.url, headers)
    }

    protected abstract fun imageUrlParse(response: Response): String

    private fun tagRequest(request: Request): Request {
        if (request.tag(ContentSource::class.java) != null) {
            return request
        }
        return request.newBuilder()
            .tag(
                ContentSource::class.java,
                mihonContentSource(),
            )
            .build()
    }

    private fun <T> sourceObservable(block: () -> T): Observable<T> {
        return Observable.fromCallable {
            MihonRequestContext.withSourceBlocking(mihonContentSource(), block)
        }
    }

    private fun <T> legacyFetch(
        request: () -> Request,
        parser: (Response) -> T,
    ): Observable<T> {
        return sourceObservable {
            client.newCall(tagRequest(request())).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HttpException(response.code)
                }
                parser(response)
            }
        }
    }

    private fun mihonContentSource(): ContentSource {
        return org.skepsun.kototoro.core.model.ContentSource("MIHON_$id")
    }



    // ======== Image request ========

    open suspend fun getImage(page: Page): Response {
        val request = tagRequest(imageRequest(page))
        return client.newCachelessCallWithProgress(request, page).awaitSuccess()
    }

    open fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    /**
     * Public helper to get headers for a page.
     */
    fun getPageHeaders(page: Page): Headers {
        return imageRequest(page).headers
    }

    // ======== URL helpers ========

    fun SChapter.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    fun SManga.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    open fun getMangaUrl(manga: SManga): String {
        return mangaDetailsRequest(manga).url.toString()
    }

    open fun getChapterUrl(chapter: SChapter): String {
        return pageListRequest(chapter).url.toString()
    }

    open fun prepareNewChapter(chapter: SChapter, manga: SManga) {}

    override fun getFilterList() = FilterList()
}
