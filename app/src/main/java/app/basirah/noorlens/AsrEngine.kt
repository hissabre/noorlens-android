package app.basirah.noorlens

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Écoute le micro en continu (16 kHz mono) et transcrit des fenêtres glissantes avec Whisper (natif, hors ligne).
 * La fenêtre s'adapte à la vitesse mesurée ; les recoupements sont alignés côté page (addWin).
 */
class AsrEngine(
    private val onText: (String) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val sr = 16000
    private val maxSec = 24
    private val ring = FloatArray(sr * maxSec)
    private var wp = 0          // position d'écriture
    private var filled = 0      // nombre d'échantillons valides
    private val lock = Any()

    @Volatile private var running = false
    private var rec: AudioRecord? = null
    private var tRec: Thread? = null
    private var tDec: Thread? = null
    @Volatile var lastLatency = 0.0
    @Volatile var ready = false

    fun load(modelPath: String): Boolean {
        ready = Whisper.nativeInit(modelPath)
        return ready
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !ready) return
        val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val size = max(minBuf, sr * 2)
        var r = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, size)
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            r = AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, size)
        }
        if (r.state != AudioRecord.STATE_INITIALIZED) { onStatus("Micro indisponible"); r.release(); return }
        rec = r
        synchronized(lock) { wp = 0; filled = 0 }
        running = true
        r.startRecording()
        tRec = Thread({
            val chunk = FloatArray(sr / 10)
            while (running) {
                val n = r.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (n <= 0) continue
                synchronized(lock) {
                    for (i in 0 until n) { ring[wp] = chunk[i]; wp = (wp + 1) % ring.size }
                    filled = min(ring.size, filled + n)
                }
            }
        }, "noor-rec").also { it.start() }
        tDec = Thread({ decodeLoop() }, "noor-asr").also { it.start() }
    }

    fun stop() {
        running = false
        try { rec?.stop() } catch (_: Throwable) {}
        try { rec?.release() } catch (_: Throwable) {}
        rec = null
    }

    private fun lastSamples(n: Int): FloatArray {
        synchronized(lock) {
            val k = min(n, filled)
            val out = FloatArray(k)
            var p = (wp - k + ring.size) % ring.size
            for (i in 0 until k) { out[i] = ring[p]; p = (p + 1) % ring.size }
            return out
        }
    }

    private fun decodeLoop() {
        var firstSilence = true
        while (running) {
            try { Thread.sleep(600) } catch (_: InterruptedException) { return }
            if (!running) return
            val sec = min(maxSec.toDouble(), max(6.0, lastLatency + 3.0))
            val a = lastSamples((sec * sr).toInt())
            if (a.size < sr * 2) continue
            var e = 0.0
            for (v in a) e += v * v
            val rms = sqrt(e / a.size)
            if (rms < 0.0006) {
                if (firstSilence) onStatus("Micro très faible (niveau ${"%.1f".format(rms * 1000)}) : parlez plus fort ou rapprochez-vous.")
                continue
            }
            firstSilence = false
            if (rms < 0.08) {
                val g = min(40.0, 0.08 / rms).toFloat()
                for (i in a.indices) a[i] = max(-1f, min(1f, a[i] * g))
            }
            val t0 = System.nanoTime()
            val text = try { Whisper.nativeTranscribe(a, 4) } catch (t: Throwable) { onStatus("Erreur Whisper : ${t.message}"); "" }
            lastLatency = (System.nanoTime() - t0) / 1e9
            if (!running) return
            onStatus("Écoute · analyse ${"%.1f".format(lastLatency)} s")
            if (text.isNotBlank()) onText(text.trim())
        }
    }
}
