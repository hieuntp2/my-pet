package com.aipet.brain.brain.pet

enum class PetEmotion {
    IDLE,
    HAPPY,
    CURIOUS,
    SLEEPY,
    SAD,
    EXCITED,
    HUNGRY,
    THINKING,
    // v2 additions
    RELIEVED,   // after repair / soothe / safe care
    NEEDY,      // seeking comfort; quiet longing
    DISTANT,    // hesitant / pulled back from low trust
    WITHDRAWN,  // mild overstimulated withdrawal
    STARTLED,   // brief surprise reaction
    SHY         // soft, slightly reserved warmth
}
