package com.aipet.brain.perception.vision.objectdetection.utils

internal object CocoLabelMapper {
    fun labelForClassId(classId: Int): String {
        val direct = COCO_LABELS.getOrNull(classId)
        if (direct != null && direct != UNKNOWN_LABEL) {
            return direct
        }

        // Some model variants emit zero-based class IDs while labels are one-based.
        val shifted = COCO_LABELS.getOrNull(classId + 1)
        if (shifted != null && shifted != UNKNOWN_LABEL) {
            return shifted
        }

        return "class_$classId"
    }

    // Label ordering matches common TFLite COCO object-detection models.
    private val COCO_LABELS = listOf(
        "???",
        "person",
        "bicycle",
        "car",
        "motorcycle",
        "airplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "???",
        "stop sign",
        "parking meter",
        "bench",
        "bird",
        "cat",
        "dog",
        "horse",
        "sheep",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "???",
        "backpack",
        "umbrella",
        "???",
        "???",
        "handbag",
        "tie",
        "suitcase",
        "frisbee",
        "skis",
        "snowboard",
        "sports ball",
        "kite",
        "baseball bat",
        "baseball glove",
        "skateboard",
        "surfboard",
        "tennis racket",
        "bottle",
        "???",
        "wine glass",
        "cup",
        "fork",
        "knife",
        "spoon",
        "bowl",
        "banana",
        "apple",
        "sandwich",
        "orange",
        "broccoli",
        "carrot",
        "hot dog",
        "pizza",
        "donut",
        "cake",
        "chair",
        "couch",
        "potted plant",
        "bed",
        "???",
        "dining table",
        "???",
        "???",
        "toilet",
        "???",
        "tv",
        "laptop",
        "mouse",
        "remote",
        "keyboard",
        "cell phone",
        "microwave",
        "oven",
        "toaster",
        "sink",
        "refrigerator",
        "???",
        "book",
        "clock",
        "vase",
        "scissors",
        "teddy bear",
        "hair drier",
        "toothbrush"
    )

    private const val UNKNOWN_LABEL = "???"
}
