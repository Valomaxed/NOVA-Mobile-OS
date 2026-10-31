package com.nova.runtime.ai.native.indexing

/** Metadata keys stored alongside vectors in [com.nova.runtime.ai.native.storage.CosineVectorIndex]. */
object EmbeddingMetadata {
    const val OBJECT_ID = "objectId"
    const val OBJECT_TYPE = "objectType"
    const val EMBEDDING_KIND = "embeddingKind"

    const val KIND_OCR = "ocr"
    const val KIND_IMAGE = "image"
    /** Document discovery vector: filename + extractive summary (Stage A). */
    const val KIND_SUMMARY = "summary"
    const val KIND_DOCUMENT_V2 = "document-summary-v2"
    const val PASSAGE_PREFIX = "passage-v1:"
    const val PASSAGE_START = "passageStart"
    const val PASSAGE_END = "passageEnd"

    fun passageMetadata(kind: String?): Map<String, String> {
        if (kind?.startsWith(PASSAGE_PREFIX) != true) return emptyMap()
        val offsets = kind.removePrefix(PASSAGE_PREFIX).split(':')
        val start = offsets.getOrNull(0)?.toIntOrNull() ?: return emptyMap()
        val end = offsets.getOrNull(1)?.toIntOrNull() ?: return emptyMap()
        if (start < 0 || end <= start) return emptyMap()
        return mapOf(PASSAGE_START to start.toString(), PASSAGE_END to end.toString())
    }
}
