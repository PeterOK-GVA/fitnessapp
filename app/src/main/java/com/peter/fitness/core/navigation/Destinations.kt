package com.peter.fitness.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data object EquipmentDestination

@Serializable
data object PlateCalculatorDestination

@Serializable
data object ExerciseListDestination

@Serializable
data class ActiveSessionDestination(val sessionId: String)

@Serializable
data class ExercisePickerDestination(val sessionId: String)

@Serializable
data class LogNewSetDestination(val sessionId: String, val exerciseId: String)

@Serializable
data class EditSetDestination(val sessionId: String, val setEntryId: String)

@Serializable
data object HistoryDestination

@Serializable
data class SessionDetailDestination(val sessionId: String)

@Serializable
data object PrsDestination
