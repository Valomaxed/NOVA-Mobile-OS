package com.nova.runtime.ai.model

import java.io.File

/** Validates on-disk model files against [ModelDownloadCatalog] minimum sizes. */
object ModelFileValidator {
    fun minimumValidBytes(fileName: String): Long =
        if (fileName == ModelAssetPaths.GEMMA_LEGACY_TASK_MODEL) ModelAssetPaths.GEMMA_TASK_MIN_BYTES
        else ModelDownloadCatalog.entryFor(fileName)?.minimumValidBytes ?: 1L

    fun isValid(fileName: String, file: File): Boolean =
        file.isFile && file.length() >= minimumValidBytes(fileName)

    fun isCorrupt(fileName: String, file: File): Boolean =
        file.isFile && file.length() < minimumValidBytes(fileName)
}
