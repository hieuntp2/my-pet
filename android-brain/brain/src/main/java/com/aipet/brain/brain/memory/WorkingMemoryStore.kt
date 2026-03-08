package com.aipet.brain.brain.memory

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkingMemoryStore(
    initialMemory: WorkingMemory = WorkingMemory()
) {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initialMemory)

    fun observe(): StateFlow<WorkingMemory> {
        return state.asStateFlow()
    }

    fun currentSnapshot(): WorkingMemory {
        return state.value
    }

    fun set(memory: WorkingMemory) {
        state.value = memory
    }

    fun update(transform: (WorkingMemory) -> WorkingMemory) {
        state.update(transform)
    }
}
