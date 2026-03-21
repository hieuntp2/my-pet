package com.aipet.brain.ui.avatar.pixel.selection

class PixelVariantSelectionContext(
    recentVariantIds: List<String> = emptyList()
) {
    private val boundedRecentVariantIds = ArrayDeque(
        recentVariantIds
            .onEach { variantId ->
                require(variantId.isNotBlank()) { "recentVariantIds must not contain blanks." }
            }
            .takeLast(MAX_HISTORY_SIZE)
    )

    val lastVariantId: String?
        get() = boundedRecentVariantIds.lastOrNull()

    val recentVariantIds: List<String>
        get() = boundedRecentVariantIds.toList()

    fun recentVariantIds(limit: Int): List<String> {
        require(limit >= 0) { "limit must be greater than or equal to 0." }
        return boundedRecentVariantIds.takeLast(limit)
    }

    fun recordSelection(variantId: String) {
        require(variantId.isNotBlank()) { "variantId must not be blank." }

        if (boundedRecentVariantIds.size == MAX_HISTORY_SIZE) {
            boundedRecentVariantIds.removeFirst()
        }
        boundedRecentVariantIds.addLast(variantId)
    }

    companion object {
        const val MAX_HISTORY_SIZE: Int = 3
    }
}
