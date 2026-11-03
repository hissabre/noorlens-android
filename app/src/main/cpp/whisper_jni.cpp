// Pont JNI minimal vers whisper.cpp : charger un modèle, transcrire un tampon audio 16 kHz mono (float).
#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include "whisper.h"
#ifdef __ANDROID__
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NoorLens", __VA_ARGS__)
#else
#include <cstdio>
#define LOGI(...) (std::printf(__VA_ARGS__), std::printf("\n"))
#endif

static whisper_context *g_ctx = nullptr;
static std::mutex g_mu;

extern "C" JNIEXPORT jboolean JNICALL
Java_app_basirah_noorlens_Whisper_nativeInit(JNIEnv *env, jobject, jstring path) {
    std::lock_guard<std::mutex> lk(g_mu);
    if (g_ctx) { whisper_free(g_ctx); g_ctx = nullptr; }
    const char *p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    g_ctx = whisper_init_from_file_with_params(p, cp);
    env->ReleaseStringUTFChars(path, p);
    LOGI("whisper init: %s", g_ctx ? "ok" : "échec");
    return g_ctx != nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_basirah_noorlens_Whisper_nativeTranscribe(JNIEnv *env, jobject, jfloatArray audio, jint threads) {
    std::lock_guard<std::mutex> lk(g_mu);
    if (!g_ctx) return env->NewStringUTF("");
    const jsize n = env->GetArrayLength(audio);
    std::vector<float> pcm(n);
    env->GetFloatArrayRegion(audio, 0, n, pcm.data());

    whisper_full_params wp = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wp.language = "ar";
    wp.translate = false;
    wp.detect_language = false;
    wp.n_threads = threads > 0 ? threads : 4;
    wp.no_context = true;
    wp.no_timestamps = true;
    wp.single_segment = false;
    wp.print_progress = false;
    wp.print_realtime = false;
    wp.print_special = false;
    wp.print_timestamps = false;
    wp.suppress_blank = true;
    wp.temperature = 0.0f;
    wp.temperature_inc = 0.0f;   // pas de re-décodage lent en cas de doute
    wp.no_speech_thold = 0.6f;

    std::string out;
    if (whisper_full(g_ctx, wp, pcm.data(), (int) pcm.size()) == 0) {
        const int ns = whisper_full_n_segments(g_ctx);
        for (int i = 0; i < ns; ++i) {
            const char *t = whisper_full_get_segment_text(g_ctx, i);
            if (t) { if (!out.empty()) out += ' '; out += t; }
        }
    }
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_app_basirah_noorlens_Whisper_nativeFree(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lk(g_mu);
    if (g_ctx) { whisper_free(g_ctx); g_ctx = nullptr; }
}
