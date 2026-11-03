package app.basirah.noorlens

/** Liaison vers whisper.cpp (voir cpp/whisper_jni.cpp). */
object Whisper {
    init { System.loadLibrary("basirahwhisper") }

    external fun nativeInit(path: String): Boolean
    external fun nativeTranscribe(audio: FloatArray, threads: Int): String
    external fun nativeFree()
}
