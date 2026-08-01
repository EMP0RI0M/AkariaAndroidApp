#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "AkariaEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_akaria_agent_AkariaEngine_testModelInference(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPathStr,
        jstring promptStr) {
        
    const char *model_path_chars = env->GetStringUTFChars(modelPathStr, nullptr);
    std::string model_path = model_path_chars;
    env->ReleaseStringUTFChars(modelPathStr, model_path_chars);
    
    const char *prompt_chars = env->GetStringUTFChars(promptStr, nullptr);
    std::string prompt = prompt_chars;
    env->ReleaseStringUTFChars(promptStr, prompt_chars);

    // Step 2: Initialize llama.cpp backend
    LOGI("Initializing llama.cpp backend...");
    llama_backend_init();
    
    // Step 3: Load GGUF model
    LOGI("Loading model from: %s", model_path.c_str());
    llama_model_params model_params = llama_model_default_params();
    llama_model* model = llama_model_load_from_file(model_path.c_str(), model_params);

    if (!model) {
        LOGE("Model failed to load");
        return env->NewStringUTF("Error: Model failed to load");
    }
    
    LOGI("Model loaded successfully! Initializing context...");
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512;
    llama_context* ctx = llama_init_from_model(model, ctx_params);
    
    if (!ctx) {
        LOGE("Context failed to initialize");
        llama_model_free(model);
        return env->NewStringUTF("Error: Context failed to initialize");
    }

    LOGI("Tokenizing prompt: %s", prompt.c_str());
    
    const struct llama_vocab* vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens_list;
    tokens_list.resize(prompt.size() + 4);
    
    // llama_tokenize returns number of tokens
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens_list.data(), tokens_list.size(), true, true);
    }
    tokens_list.resize(n_tokens);

    LOGI("Tokenization complete! Extracted %d tokens. Decoding...", n_tokens);

    // Prepare batch
    llama_batch batch = llama_batch_init(512, 0, 1);
    for (size_t i = 0; i < tokens_list.size(); i++) {
        akaria_batch_add(batch, tokens_list[i], i, { 0 }, false);
    }
    batch.logits[batch.n_tokens - 1] = true; 
    
    if (llama_decode(ctx, batch) != 0) {
        LOGE("llama_decode failed");
        return env->NewStringUTF("Error: llama_decode failed");
    }

    int n_cur = batch.n_tokens;
    std::string result = "Output: ";
    
    LOGI("Generating 10-20 tokens...");
    for (int i = 0; i < 15; i++) {
        auto * logits = llama_get_logits_ith(ctx, batch.n_tokens - 1);
        
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
            result += std::string(buf, n);
        }
        
        batch.n_tokens = 0; // Clear batch
        akaria_batch_add(batch, new_token_id, n_cur, { 0 }, true);
        n_cur += 1;
        
        if (llama_decode(ctx, batch) != 0) {
            break;
        }
    }
    
    LOGI("Generation complete: %s", result.c_str());
    
    llama_batch_free(batch);
    llama_free(ctx);
    llama_model_free(model);
    llama_backend_free();

    return env->NewStringUTF(result.c_str());
}
