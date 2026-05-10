package com.peter.fitness.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peter.fitness.feature.calculator.PlateCalculatorRoute
import com.peter.fitness.feature.equipment.EquipmentRoute
import com.peter.fitness.feature.exercises.ExerciseListRoute
import com.peter.fitness.feature.home.HomeRoute
import com.peter.fitness.feature.session.ActiveSessionRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            HomeRoute(
                onEquipmentClick = { navController.navigate(EquipmentDestination) },
                onCalculatorClick = { navController.navigate(PlateCalculatorDestination) },
                onExercisesClick = { navController.navigate(ExerciseListDestination) },
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
            ActiveSessionRoute(onFinished = { navController.popBackStack() })
        }
    }
}
