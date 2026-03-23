package com.aipet.brain.perception.vision.objectdetection

internal fun resolveObjectDetectionModelName(modelAssetPath: String?): String? {
    val normalizedPath = modelAssetPath?.trim().orEmpty()
    if (normalizedPath.isBlank()) {
        return null
    }
    return normalizedPath.substringAfterLast('/')
        .trim()
        .ifBlank { normalizedPath }
}
