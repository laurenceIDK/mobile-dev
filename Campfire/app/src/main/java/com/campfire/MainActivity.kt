package com.campfire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.campfire.presentation.ui.ChatScreen
import com.campfire.presentation.ui.HomeScreen
import com.campfire.presentation.ui.LoginScreen
import com.campfire.presentation.viewmodel.LoginViewModel
import com.campfire.ui.theme.CampfireTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Campfire
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            CampfireTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampfireApp()
                }
            }
        }
    }
}

@Composable
fun CampfireApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val currentUser by loginViewModel.currentUser.collectAsState()
    
    // Determine start destination based on authentication state
    val startDestination = if (currentUser != null) "home" else "login"
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onNavigateToChat = { groupId ->
                    navController.navigate("chat/$groupId")
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        
        composable("chat/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            ChatScreen(
                groupId = groupId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation routes for Campfire
 */
object CampfireRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHAT = "chat/{groupId}"
    
    fun chatRoute(groupId: String) = "chat/$groupId"
}
