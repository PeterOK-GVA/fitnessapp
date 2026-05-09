package com.peter.fitness.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peter.fitness.feature.equipment.EquipmentRoute
import com.peter.fitness.feature.home.HomeRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            HomeRoute(
                onEquipmentClick = { navController.navigate(EquipmentDestination) },
            )
        }
        composable<EquipmentDestination> {
            EquipmentRoute(onBack = { navController.popBackStack() })
        }
    }
}
