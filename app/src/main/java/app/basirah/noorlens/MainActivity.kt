package app.basirah.noorlens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MainActivity : android.app.Activity() {
    private lateinit var web: WebView
    private lateinit var asr: AsrEngine
    private var pendingStart = false

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }
    private fun q(s: String) = JSONObject.quote(s)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        asr = AsrEngine(
            onText = { t -> js("window.__nt&&window.__nt(${q(t)})") },
            onStatus = { s -> js("window.__ns&&window.__ns(${q(s)})") },
        )
        web = WebView(this).apply {
            setBackgroundColor(0xFF0B1512.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(Bridge(), "Native")
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        Thread { prepareModel() }.start()
    }

    /** Copie le modèle fourni dans l'APK vers le stockage interne (une seule fois), puis le charge. */
    private fun prepareModel() {
        try {
            val name = assets.list("models")?.firstOrNull { it.endsWith(".bin") }
            if (name == null) { js("window.__ns&&window.__ns('Aucun modèle Whisper dans l’application')"); return }
            val out = File(filesDir, name)
            val len = assets.openFd("models/$name").use { it.length }
            if (!out.exists() || out.length() != len) {
                js("window.__ns&&window.__ns('Préparation du modèle (première fois)…')")
                assets.open("models/$name").use { i -> FileOutputStream(out).use { o -> i.copyTo(o, 1 shl 20) } }
            }
            js("window.__ns&&window.__ns('Chargement du modèle…')")
            val ok = asr.load(out.absolutePath)
            js(if (ok) "window.__nready&&window.__nready(${q(name)})" else "window.__ns&&window.__ns('Échec du chargement du modèle')")
        } catch (t: Throwable) {
            js("window.__ns&&window.__ns(${q("Erreur modèle : " + t.message)})")
        }
    }

    inner class Bridge {
        @JavascriptInterface fun start() = runOnUiThread {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                asr.start(); js("window.__nstate&&window.__nstate('listening')")
            } else {
                pendingStart = true
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        }
        @JavascriptInterface fun stop() = runOnUiThread { asr.stop(); js("window.__nstate&&window.__nstate('stopped')") }
        @JavascriptInterface fun isReady(): Boolean = asr.ready
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (pendingStart) {
            pendingStart = false
            if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) { asr.start(); js("window.__nstate&&window.__nstate('listening')") }
            else js("window.__ns&&window.__ns('Micro refusé : autorisez-le dans les réglages du casque')")
        }
    }

    override fun onWindowFocusChanged(f: Boolean) {
        super.onWindowFocusChanged(f)
        if (f) window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    @Deprecated("") override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onDestroy() { asr.stop(); Whisper.nativeFree(); web.destroy(); super.onDestroy() }
}
