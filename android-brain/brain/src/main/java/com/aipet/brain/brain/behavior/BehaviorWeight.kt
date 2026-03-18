package com.aipet.brain.brain.behavior

data class BehaviorWeight(
    val source: String,
    val delta: Float
) {
    init {
        require(source.isNotBlank()) { "source cannot be blank." }
    }
}
