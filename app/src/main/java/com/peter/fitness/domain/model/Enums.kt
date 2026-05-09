package com.peter.fitness.domain.model

enum class MovementPattern {
    SQUAT,
    HIP_HINGE,
    HORIZONTAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PUSH,
    VERTICAL_PULL,
    LUNGE,
    CARRY,
    EXPLOSIVE,
    CORE_FLEXION,
    CORE_ROTATION,
    CORE_ANTI_EXTENSION,
}

enum class LoadType {
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    WEIGHT_PLATE,
    BODYWEIGHT,
    MACHINE,
}

enum class TechniqueDemand {
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
}

enum class ConditioningSuitability {
    NONE,
    LIMITED,
    GOOD,
    IDEAL,
}

enum class SubjectiveLoad {
    MUCH_TOO_LIGHT,
    TOO_LIGHT,
    OK,
    TOO_HEAVY,
    MUCH_TOO_HEAVY,
}

enum class TechniqueRating {
    POOR,
    OK,
    GOOD,
    EXCELLENT,
}

enum class SessionFocus {
    STRENGTH,
    CONDITIONING,
    ENDURANCE,
    TECHNIQUE,
    BEAT_YOUR_PB,
    FREE_LOG,
}
