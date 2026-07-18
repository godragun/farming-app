package com.project.farmingapp.utilities

import android.content.Context
import android.os.Build
import android.util.LruCache
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Leaflet in a [WebView] often fails to load external HTTPS tile images regardless of the page
 * origin (file:// or appassets.androidplatform.net).  The fix: proxy every tile request through
 * OkHttp (which the rest of the app uses for all its API calls and is known to work), then hand
 * the raw bytes back to the WebView via [WebViewAssetLoader].
 *
 * Tile URL in the HTML  →  https://appassets.androidplatform.net/tiles/{z}/{y}/{x}
 * Asset loader matches /tiles/  →  [TileProxyPathHandler] fetches the REAL tile via OkHttp
 * WebView renders the bytes — no external CDN request from the WebView's restricted stack.
 */
object LeafletMapWebViewHelper {

    private const val MAP_ASSET_URL =
        "https://appassets.androidplatform.net/assets/leaflet_map.html"

    /** Esri World Imagery base URL (z/y/x row-major format, no API key required). */
    private const val ESRI_BASE =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile"

    /** OSM fallback base URL (z/x/y standard slippy format). */
    private const val OSM_BASE = "https://tile.openstreetmap.org"

    fun mapPageUrl(): String = MAP_ASSET_URL

    // ── OkHttp client shared across all tile fetches ─────────────────────────────────────
    private fun buildTileClient(cacheDir: File): OkHttpClient {
        val cache = Cache(File(cacheDir, "leaflet_tile_cache"), 50L * 1024L * 1024L) // 50MB
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var tileHttpClient: OkHttpClient? = null

    // A small in-memory cache to avoid re-downloading tiles during quick scroll/zoom.
    private val memoryTileCache: LruCache<String, ByteArray> =
        object : LruCache<String, ByteArray>(4 * 1024 * 1024) { // 4MB
            override fun sizeOf(key: String, value: ByteArray): Int = value.size
        }

    /**
     * PathHandler that proxies satellite / street tile requests through OkHttp.
     *
     * Registered under /tiles/  →  path looks like "7/60/96" (z/y/x for Esri) or
     * a path with a final .png suffix for the OSM fallback.
     */
    private class TileProxyPathHandler : WebViewAssetLoader.PathHandler {
        private fun client(context: Context): OkHttpClient {
            return tileHttpClient ?: synchronized(LeafletMapWebViewHelper) {
                tileHttpClient ?: buildTileClient(context.cacheDir).also { tileHttpClient = it }
            }
        }

        private lateinit var appContext: Context

        fun attachContext(context: Context) {
            appContext = context.applicationContext
        }

        override fun handle(path: String): WebResourceResponse? {
            // Strip any leading slash left by the asset loader
            val cleanPath = path.trimStart('/')

            // Try Esri World Imagery first (z/y/x format, JPEG)
            val esriUrl = "$ESRI_BASE/$cleanPath"
            fetchTile(esriUrl, "image/jpeg")?.let { return it }

            // Fall back to OSM street map (z/x/y format, PNG)
            // Re-map the path parts: Esri uses z/y/x, OSM uses z/x/y
            val parts = cleanPath.split("/")
            if (parts.size == 3) {
                val (z, y, x) = parts
                val osmUrl = "$OSM_BASE/$z/$x/$y.png"
                fetchTile(osmUrl, "image/png")?.let { return it }
            }
            return null
        }

        private fun fetchTile(url: String, mimeType: String): WebResourceResponse? {
            memoryTileCache.get(url)?.let { bytes ->
                return WebResourceResponse(mimeType, null, ByteArrayInputStream(bytes))
            }
            return try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .addHeader("Referer", "https://appassets.androidplatform.net/")
                    .addHeader("Cache-Control", "public, max-age=86400")
                    .build()

                val resp = client(appContext).newCall(req).execute()
                resp.use { r ->
                    if (!r.isSuccessful) return null
                    val bytes = r.body?.bytes() ?: return null
                    memoryTileCache.put(url, bytes)
                    WebResourceResponse(mimeType, null, ByteArrayInputStream(bytes))
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun createAssetLoader(context: Context): WebViewAssetLoader {
        val handler = TileProxyPathHandler().apply { attachContext(context) }
        return WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/tiles/", handler)
            .build()
    }

    fun applyLeafletWebSettings(wv: WebView) {
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.blockNetworkLoads = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mediaPlaybackRequiresUserGesture = true
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportZoom(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        }
        
        // Ensure network access is allowed
        wv.setNetworkAvailable(true)
    }

    /**
     * [WebViewClient] that serves `assets/` and proxied tiles via [assetLoader].
     * Only intercepts `appassets.androidplatform.net` — all other requests go through
     * the default WebView network stack.
     */
    fun createClient(
        assetLoader: WebViewAssetLoader,
        onMapPageReady: (WebView) -> Unit
    ): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url ?: return null
                if (url.host != "appassets.androidplatform.net") {
                    android.util.Log.d("LeafletMap", "External request: ${url}")
                    return null
                }
                val response = assetLoader.shouldInterceptRequest(url)
                android.util.Log.d("LeafletMap", "Intercepted request: ${url} -> ${response != null}")
                return response
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("LeafletMap", "Page finished loading: $url")
                view?.let { onMapPageReady(it) }
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                android.util.Log.e("LeafletMap", "Resource error: ${request?.url} - ${error?.description}")
            }
        }
    }

    /**
     * Enable WebView console messages for debugging JavaScript execution and errors.
     */
    fun enableConsoleLogging(view: WebView) {
        view.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                val level = when (consoleMessage.messageLevel()) {
                    android.webkit.ConsoleMessage.MessageLevel.TIP -> "TIP"
                    android.webkit.ConsoleMessage.MessageLevel.LOG -> "LOG"
                    android.webkit.ConsoleMessage.MessageLevel.WARNING -> "WARN"
                    android.webkit.ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("LeafletWebView", "[$level] ${consoleMessage.message()} (line ${consoleMessage.lineNumber()})")
                return true
            }
        }
    }

    fun runLeafletResizeAndCenter(view: WebView?) {
        view?.evaluateJavascript(
            """(function(){
                var tries = 0;
                function tick() {
                  try {
                    if (typeof map !== 'undefined' && map) {
                      map.invalidateSize(true);
                      map.setView([6.9271, 79.8612], 9);
                      return;
                    }
                  } catch (e) {}
                  tries++;
                  if (tries < 40) setTimeout(tick, 80);
                }
                tick();
            })();""",
            null
        )
    }
}
