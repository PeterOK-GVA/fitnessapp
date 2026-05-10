package com.peter.fitness.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peter.fitness.feature.calculator.PlateCalculatorRoute
import com.peter.fitness.feature.equipment.EquipmentRoute
import com.peter.fitness.feature.exercises.ExerciseListRoute
import com.peter.fitness.feature.exercises.ExercisePickerRoute
import com.peter.fitness.feature.history.HistoryRoute
import com.peter.fitness.feature.history.detail.SessionDetailRoute
import com.peter.fitness.feature.home.HomeRoute
import com.peter.fitness.feature.session.ActiveSessionRoute
import com.peter.fitness.feature.session.logset.LogSetRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            HomeRoute(
                onEquipmentClick = { navController.navigate(EquipmentDestination) },
                onCalculatorClick = { navController.navigate(PlateCalculatorDestination) },
                onExercisesClick = { navController.navigate(ExerciseListDestination) },
                onHistoryClick = { navController.navigate(HistoryDestination) },
                onSessionStarted = { sessionId ->
                    navController.navigate(ActiveSessionDestination(sessionId))
                },
            )
        }
        composable<EquipmentDestination> {
            EquipmentRoute(onBack = { navController.popBackStack() })
        }
        composable<PlateCalculatorDestination> {
            PlateCalculatorRoute(onBack = { navController.popBackStack() })
        }
        composable<ExerciseListDestination> {
            ExerciseListRoute(onBack = { navController.popBackStack() })
        }
        composable<ActiveSessionDestination> {
            ActiveSessionRoute(
                onFinished = { navController.popBackStack() },
                onAddSet = { sessionId ->
                    navController.navigate(ExercisePickerDestination(sessionId))
                },
                onEditSet = { sessionId, setEntryId ->
                    navController.navigate(EditSetDestination(sessionId, setEntryId))
                },
            )
        }
        composable<ExercisePickerDestination> { entry ->
            val sessionId = requireNotNull(entry.arguments?.getString("sessionId"))
            ExercisePickerRoute(
                onBack = { navController.popBackStack() },
                onExercisePicked = { exerciseId ->
                    navController.navigate(
                        LogNewSetDestination(sessionId = sessionId, exerciseId = exerciseId.value),
                    ) {
                        popUpTo(ExercisePickerDestination(sessionId)) { inclusive = true }
                    }
                },
            )
        }
        composable<LogNewSetDestination> {
            LogSetRoute(onDone = { navController.popBackStack() })
        }
        composable<EditSetDestination> {
            LogSetRoute(onDone = { navController.popBackStack() })
        }
        composable<HistoryDestination> {
            HistoryRoute(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(SessionDetailDestination(sessionId))
                },
            )
        }
        composable<SessionDetailDestination> {
            SessionDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
