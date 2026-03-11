package com.aipet.brain.perception.vision.model

import android.graphics.Bitmap

data class FaceCropResult(
    val bitmap: Bitmap?,
    val cropWidth: Int,
    val cropHeight: Int,
    val failureReason: FaceCropFailureReason?
) {
    val isSuccess: Boolean
        get() = bitmap != null && failureReason == null && cropWidth > 0 && cropHeight > 0

    companion object {
        fun success(bitmap: Bitmap): FaceCropResult {
            return FaceCropResult(
                bitmap = bitmap,
                cropWidth = bitmap.width,
                cropHeight = bitmap.height,
                failureReason = null
            )
        }

        fun failure(reason: FaceCropFailureReason): FaceCropResult {
            return FaceCropResult(
                bitmap = null,
                cropWidth = 0,
                cropHeight = 0,
                failureReason = reason
            )
        }
    }
}

enum class FaceCropFailureReason {
    NO_FRAME_AVAILABLE,
    NO_FACE_DETECTED,
    INVALID_SOURCE,
    INVALID_BOUNDS,
    FRAME_DECODE_FAILED,
    FRAME_ROTATION_FAILED,
    CROP_FAILED
}
