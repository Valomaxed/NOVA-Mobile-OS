package com.nova.runtime.ai.native.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.nova.runtime.ai.model.GroundedPromptBudget
import com.nova.runtime.ai.model.LocalLlmEngine
import com.nova.runtime.ai.model.ModelAssetPaths
import com.nova.runtime.ai.model.ModelLoader
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * MediaPipe LLM Inference backed by a local Gemma `.task` model.
 * Used only for grounded RAG over retrieved document text — never as a free-form oracle.
 */
class MediaPipeLocalLlmEngine(
    private val context: Context,
    private val modelLoader: ModelLoader,
    private val modelFileName: String = ModelAssetPaths.GEMMA_TASK_MODEL,
) : LocalLlmEngine {
    private val mutex = Mutex()
    private val inferenceRef = AtomicReference<LlmInference?>(null)

    private var loadedModelName: String? = null

    private fun selectedModelName(): String = candidates().firstOrNull { name ->
        val file = modelFile(name)
        (file.isFile && file.length() >= ModelAssetPaths.GEMMA_TASK_MIN_BYTES) || hasBundledAsset(name)
    } ?: modelFileName

    private fun candidates(): List<String> = if (modelFileName == ModelAssetPaths.GEMMA_TASK_MODEL) {
        listOf(modelFileName, ModelAssetPaths.GEMMA_LEGACY_TASK_MODEL)
    } else listOf(modelFileName)

    override val contextWindowTokens: Int
        get() = if (!isAvailable()) 0 else contextTokens(selectedModelName())

    override fun isAvailable(): Boolean = candidates().any { name ->
        val file = modelFile(name)
        (file.isFile && file.length() >= ModelAssetPaths.GEMMA_TASK_MIN_BYTES) || hasBundledAsset(name)
    }

    private fun contextTokens(name: String): Int =
        if (name == ModelAssetPaths.GEMMA_TASK_MODEL) ModelAssetPaths.GEMMA_CONTEXT_TOKENS
        else ModelAssetPaths.GEMMA_LEGACY_CONTEXT_TOKENS

    override suspend fun generate(prompt: String, maxTokens: Int): String? =
        generateInternal(maxTokens) { llm, contextLimit, outputBudget ->
            prompt.takeIf {
                it.isNotBlank() && llm.sizeInTokens(it) + outputBudget +
                    GroundedPromptBudget.CONTROL_TOKEN_RESERVE <= contextLimit
            }
        }

    override suspend fun generateGrounded(
        context: String,
        buildPrompt: (String) -> String,
        maxTokens: Int,
    ): String? = generateInternal(maxTokens) { llm, contextLimit, outputBudget ->
        GroundedPromptBudget.fit(context, buildPrompt, llm::sizeInTokens, contextLimit, outputBudget)
    }

    private suspend fun generateInternal(
        maxTokens: Int,
        fitPrompt: (LlmInference, Int, Int) -> String?,
    ): String? = withContext(Dispatchers.Default) {
        val name = selectedModelName()
        modelLoader.resolvePath(name)
        if (!modelFile(name).isFile || modelFile(name).length() < ModelAssetPaths.GEMMA_TASK_MIN_BYTES) {
            return@withContext null
        }
        mutex.withLock {
            runCatching {
                val llm = ensureInference(name) ?: return@runCatching null
                val outputBudget = maxTokens.coerceIn(1, MAX_OUTPUT_TOKENS)
                val prompt = fitPrompt(llm, contextTokens(name), outputBudget) ?: return@runCatching null
                val answer = llm.generateResponse(prompt).trim()
                if (llm.sizeInTokens(answer) <= outputBudget) answer.takeIf { it.isNotBlank() } else {
                    // This API has a total context limit, not a separate decode limit.
                    com.nova.runtime.ai.tokenizer.TextPassages.split(
                        answer, tokenCount = llm::sizeInTokens, maxTokens = outputBudget, overlapTokens = 0,
                    ).firstOrNull()?.text?.trim()
                }
            }.onFailure { e ->
                Log.w(TAG, "MediaPipe generate failed: ${e.message}")
                closeInference()
            }.getOrNull()
        }
    }

    suspend fun preWarm() {
        withContext(Dispatchers.Default) {
            if (!isAvailable()) return@withContext
            mutex.withLock {
                val name = selectedModelName()
                modelLoader.resolvePath(name)
                ensureInference(name)
            }
        }
    }

    private fun ensureInference(name: String): LlmInference? {
        inferenceRef.get()?.let { if (loadedModelName == name) return it else closeInference() }
        val path = modelFile(name).absolutePath
        return runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(contextTokens(name))
                .build()
            val created = LlmInference.createFromOptions(context.applicationContext, options)
            loadedModelName = name
            inferenceRef.set(created)
            Log.i(TAG, "MediaPipe LLM ready path=$path")
            created
        }.onFailure { e ->
            Log.w(TAG, "MediaPipe LLM init failed: ${e.message}")
        }.getOrNull()
    }

    private fun closeInference() {
        val current = inferenceRef.getAndSet(null) ?: return
        loadedModelName = null
        runCatching { current.close() }
    }

    private fun modelFile(name: String): File = File(File(context.filesDir, "models"), name)

    private fun hasBundledAsset(name: String): Boolean =
        runCatching {
            context.assets.open("models/$name").use { }
            true
        }.getOrDefault(false)

    companion object {
        private const val MAX_OUTPUT_TOKENS = 1536
        private const val TAG = "NOVA/MediaPipeLLM"
    }
}
