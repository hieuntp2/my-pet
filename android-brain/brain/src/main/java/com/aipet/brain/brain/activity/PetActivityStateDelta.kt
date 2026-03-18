package com.aipet.brain.brain.activity

data class PetActivityStateDelta(
    val energyDelta: Int = 0,
    val hungerDelta: Int = 0,
    val sleepinessDelta: Int = 0,
    val socialDelta: Int = 0,
    val bondDelta: Int = 0
)
