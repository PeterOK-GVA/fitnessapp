package com.peter.fitness.data.db.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.peter.fitness.data.db.entity.EquipmentProfileEntity
import com.peter.fitness.domain.model.ConditioningSuitability
import com.peter.fitness.domain.model.LoadType
import com.peter.fitness.domain.model.MovementPattern
import com.peter.fitness.domain.model.TechniqueDemand

/**
 * Seeds the barbell catalogue and a default equipment profile when the database is first created.
 * Runs exactly once per fresh install — re-running the app does not re-seed.
 */
internal object BarbellSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        seedExercises(db, now)
        seedDefaultEquipmentProfile(db, now)
    }

    private fun seedExercises(db: SupportSQLiteDatabase, now: Long) {
        BARBELL_CATALOGUE.forEach { seed ->
            db.execSQL(
                INSERT_EXERCISE_SQL,
                arrayOf(
                    seed.id,
                    seed.name,
                    seed.movementPattern.name,
                    seed.loadType.name,
                    seed.techniqueDemand.name,
                    seed.conditioningSuitability.name,
                    seed.requiresRack,
                    seed.requiresBench,
                    seed.requiresPullUpBar,
                    seed.barWeightAware,
                    seed.supportsTempo,
                    now,
                ),
            )
        }
    }

    private fun seedDefaultEquipmentProfile(db: SupportSQLiteDatabase, now: Long) {
        db.execSQL(
            INSERT_PROFILE_SQL,
            arrayOf(EquipmentProfileEntity.DEFAULT_ID, DEFAULT_BAR_KG, true, true, false, now),
        )
        DEFAULT_PLATE_SET.forEach { (denomKg, pairCount) ->
            db.execSQL(
                INSERT_PLATE_SQL,
                arrayOf(EquipmentProfileEntity.DEFAULT_ID, denomKg, pairCount),
            )
        }
    }

    private const val DEFAULT_BAR_KG: Double = 20.0

    private val DEFAULT_PLATE_SET: List<Pair<Double, Int>> = listOf(
        20.0 to 4,
        15.0 to 2,
        10.0 to 4,
        5.0 to 4,
        2.5 to 4,
        1.25 to 2,
    )

    private const val INSERT_EXERCISE_SQL = """
        INSERT INTO exercise (
            id, name, movement_pattern, load_type, technique_demand, conditioning_suitability,
            requires_rack, requires_bench, requires_pull_up_bar,
            bar_weight_aware, supports_tempo, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """

    private const val INSERT_PROFILE_SQL = """
        INSERT INTO equipment_profile (id, bar_kg, has_rack, has_bench, has_pull_up_bar, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
    """

    private const val INSERT_PLATE_SQL = """
        INSERT INTO plate_pair (profile_id, denomination_kg, pair_count) VALUES (?, ?, ?)
    """

    private data class ExerciseSeed(
        val id: String,
        val name: String,
        val movementPattern: MovementPattern,
        val loadType: LoadType,
        val techniqueDemand: TechniqueDemand,
        val conditioningSuitability: ConditioningSuitability,
        val requiresRack: Boolean = false,
        val requiresBench: Boolean = false,
        val requiresPullUpBar: Boolean = false,
        val barWeightAware: Boolean = true,
        val supportsTempo: Boolean = true,
    )

    private val BARBELL_CATALOGUE: List<ExerciseSeed> = listOf(
        ExerciseSeed(
            id = "back-squat",
            name = "Back Squat",
            movementPattern = MovementPattern.SQUAT,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
            requiresRack = true,
        ),
        ExerciseSeed(
            id = "front-squat",
            name = "Front Squat",
            movementPattern = MovementPattern.SQUAT,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
            requiresRack = true,
        ),
        ExerciseSeed(
            id = "conventional-deadlift",
            name = "Conventional Deadlift",
            movementPattern = MovementPattern.HIP_HINGE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
        ),
        ExerciseSeed(
            id = "sumo-deadlift",
            name = "Sumo Deadlift",
            movementPattern = MovementPattern.HIP_HINGE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
        ),
        ExerciseSeed(
            id = "romanian-deadlift",
            name = "Romanian Deadlift",
            movementPattern = MovementPattern.HIP_HINGE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
        ),
        ExerciseSeed(
            id = "bench-press",
            name = "Bench Press",
            movementPattern = MovementPattern.HORIZONTAL_PUSH,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
            requiresBench = true,
            requiresRack = true,
        ),
        ExerciseSeed(
            id = "push-press",
            name = "Push Press",
            movementPattern = MovementPattern.VERTICAL_PUSH,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.LIMITED,
            requiresRack = true,
        ),
        ExerciseSeed(
            id = "bent-row",
            name = "Bent Row",
            movementPattern = MovementPattern.HORIZONTAL_PULL,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.MODERATE,
            conditioningSuitability = ConditioningSuitability.GOOD,
        ),
        ExerciseSeed(
            id = "upright-row",
            name = "Upright Row",
            movementPattern = MovementPattern.VERTICAL_PULL,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.MODERATE,
            conditioningSuitability = ConditioningSuitability.GOOD,
        ),
        ExerciseSeed(
            id = "thruster",
            name = "Thruster",
            movementPattern = MovementPattern.EXPLOSIVE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.IDEAL,
        ),
        ExerciseSeed(
            id = "clean-and-jerk",
            name = "Clean & Jerk",
            movementPattern = MovementPattern.EXPLOSIVE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.VERY_HIGH,
            conditioningSuitability = ConditioningSuitability.GOOD,
        ),
        ExerciseSeed(
            id = "sumo-deadlift-high-pull",
            name = "Sumo Deadlift High Pull",
            movementPattern = MovementPattern.EXPLOSIVE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.HIGH,
            conditioningSuitability = ConditioningSuitability.IDEAL,
        ),
        ExerciseSeed(
            id = "barbell-lunge",
            name = "Barbell Lunge",
            movementPattern = MovementPattern.LUNGE,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.MODERATE,
            conditioningSuitability = ConditioningSuitability.GOOD,
        ),
        ExerciseSeed(
            id = "barbell-curl",
            name = "Barbell Curl",
            movementPattern = MovementPattern.HORIZONTAL_PULL,
            loadType = LoadType.BARBELL,
            techniqueDemand = TechniqueDemand.LOW,
            conditioningSuitability = ConditioningSuitability.GOOD,
            barWeightAware = false,
        ),
        ExerciseSeed(
            id = "plate-cocoon",
            name = "Plate Cocoon",
            movementPattern = MovementPattern.CORE_ROTATION,
            loadType = LoadType.WEIGHT_PLATE,
            techniqueDemand = TechniqueDemand.MODERATE,
            conditioningSuitability = ConditioningSuitability.GOOD,
            barWeightAware = false,
            supportsTempo = false,
        ),
    )
}
