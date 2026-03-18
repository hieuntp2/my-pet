package com.aipet.brain.brain.pet

data class PetProfile(
    val id: String,
    val name: String,
    val createdAt: Long
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(name.isNotBlank()) { "name cannot be blank." }
        require(createdAt > 0L) { "createdAt must be greater than zero." }
    }
}
