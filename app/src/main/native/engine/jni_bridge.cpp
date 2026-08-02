#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "AkariaEngine_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static std::atomic<bool> g_cancel_generation(false);
static bool g_backend_initialized = false;

// Helper for adding a token to a batch
static void akaria_batch_add(struct llama_batch & batch, llama_token id, llama_pos pos, const std::vector<llama_seq_id> & seq_ids, bool logits) {
    batch.token   [batch.n_tokens] = id;
    batch.pos     [batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = seq_ids.size();
    for (size_t i = 0; i < seq_ids.size(); ++i) {
        batch.seq_id[batch.n_tokens][i] = seq_ids[i];
    }
    batch.logits  [batch.n_tokens] = logits;
    batch.n_tokens++;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_akaria_agent_AkariaEngine_loadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPathStr,
        jint contextSize) {
        
    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    if (g_model != nullptr) {
        LOGE("Model is already loaded. Unload first.");
        return JNI_FALSE;
    }

    const char *model_path_chars = env->GetStringUTFChars(modelPathStr, nullptr);
    std::string model_path = model_path_chars;
    env->ReleaseStringUTFChars(modelPathStr, model_path_chars);

    LOGI("Loading model from: %s", model_path.c_str());
    llama_model_params model_params = llama_model_default_params();
    g_model = llama_model_load_from_file(model_path.c_str(), model_params);

    if (!g_model) {
        LOGE("Model failed to load");
        return JNI_FALSE;
    }
    
    LOGI("Model loaded successfully! Initializing context...");
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    g_ctx = llama_init_from_model(g_model, ctx_params);
    
    if (!g_ctx) {
        LOGE("Context failed to initialize");
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_akaria_agent_AkariaEngine_unloadModel(
        JNIEnv* env,
        jobject /* this */) {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    LOGI("Model unloaded.");
}

extern "C" JNIEXPORT void JNICALL
Java_com_akaria_agent_AkariaEngine_stopGeneration(
        JNIEnv* env,
        jobject /* this */) {
    g_cancel_generation = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_akaria_agent_AkariaEngine_generate(
        JNIEnv* env,
        jobject /* this */,
        jstring promptStr,
        jint maxTokens,
        jobject callback) {
        
    if (!g_model || !g_ctx) {
        LOGE("Cannot generate: Model or context not loaded.");
        return;
    }

    g_cancel_generation = false;

    const char *prompt_chars = env->GetStringUTFChars(promptStr, nullptr);
    std::string prompt = prompt_chars;
    env->ReleaseStringUTFChars(promptStr, prompt_chars);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");

    LOGI("Tokenizing prompt...");
    
    const struct llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens_list;
    tokens_list.resize(prompt.size() + 4);
    
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens_list.data(), tokens_list.size(), true, true);
    }
    tokens_list.resize(n_tokens);

    LOGI("Tokenization complete! Extracted %d tokens. Decoding...", n_tokens);

    llama_batch batch = llama_batch_init(llama_n_ctx(g_ctx), 0, 1);
    for (size_t i = 0; i < tokens_list.size(); i++) {
        akaria_batch_add(batch, tokens_list[i], i, { 0 }, false);
    }
    batch.logits[batch.n_tokens - 1] = true; 
    
    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("llama_decode failed on prompt");
        llama_batch_free(batch);
        return;
    }

    int n_cur = batch.n_tokens;
    
    LOGI("Starting generation loop...");
    for (int i = 0; i < maxTokens; i++) {
        if (g_cancel_generation) {
            LOGI("Generation cancelled by user.");
            break;
        }

        auto * logits = llama_get_logits_ith(g_ctx, batch.n_tokens - 1);
        
        llama_token new_token_id = 0;
        float max_logit = -1e9;
        int n_vocab = llama_vocab_n_tokens(vocab);
        for (int j = 0; j < n_vocab; j++) {
            if (logits[j] > max_logit) {
                max_logit = logits[j];
                new_token_id = j;
            }
        }
        
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }
        
        char buf[128];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            std::string token_str(buf, n);
            jstring jToken = env->NewStringUTF(token_str.c_str());
            env->CallVoidMethod(callback, onTokenMethod, jToken);
            env->DeleteLocalRef(jToken);
        }
        
        batch.n_tokens = 0;
        akaria_batch_add(batch, new_token_id, n_cur, { 0 }, true);
        n_cur += 1;
        
        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }
    }
    
    LOGI("Generation complete.");
    llama_batch_free(batch);
}
