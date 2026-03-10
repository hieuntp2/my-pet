package com.aipet.brain.core.common.config

data class KeywordSpottingConfig(
    val enabled: Boolean,
    val provider: KeywordSpottingProvider
) {
    companion object {
        val DEFAULT = KeywordSpottingConfig(
            enabled = false,
            provider = KeywordSpottingProvider.NONE
        )
    }
}
