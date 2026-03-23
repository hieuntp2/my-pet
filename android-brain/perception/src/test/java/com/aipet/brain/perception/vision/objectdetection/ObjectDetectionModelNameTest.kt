package com.aipet.brain.perception.vision.objectdetection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObjectDetectionModelNameTest {
    @Test
    fun `resolveObjectDetectionModelName returns file name for nested asset path`() {
        assertEquals(
            "efficientdet.tflite",
            resolveObjectDetectionModelName("models/vision/efficientdet.tflite")
        )
    }

    @Test
    fun `resolveObjectDetectionModelName returns original name for bare file`() {
        assertEquals(
            "efficientdet.tflite",
            resolveObjectDetectionModelName("efficientdet.tflite")
        )
    }

    @Test
    fun `resolveObjectDetectionModelName returns null for blank path`() {
        assertNull(resolveObjectDetectionModelName("   "))
        assertNull(resolveObjectDetectionModelName(null))
    }
}
