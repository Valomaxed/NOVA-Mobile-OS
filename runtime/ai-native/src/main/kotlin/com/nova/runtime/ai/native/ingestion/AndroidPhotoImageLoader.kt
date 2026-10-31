package com.nova.runtime.ai.native.ingestion

import android.content.Context
import android.net.Uri
import com.nova.runtime.ai.io.readBoundedBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPhotoImageLoader(
    private val context: Context,
) : PhotoImageLoader {
    override suspend fun loadBytes(uri: String): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    stream.readBoundedBytes(MAX_IMAGE_BYTES)
                }
            }.getOrNull()
        }

    companion object {
        private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    }
}
