package com.blinkchat

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
import com.blinkchat.presentation.ui.ChatScreen
import com.blinkchat.presentation.ui.HomeScreen
import com.blinkchat.presentation.ui.LoginScreen
import com.blinkchat.presentation.viewmodel.LoginViewModel
import com.blinkchat.ui.theme.BlinkChatTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for BlinkChat
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BlinkChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlinkChatApp()
                }
            }
        }
    }
}

@Composable
fun BlinkChatApp() {
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
 * Navigation routes for BlinkChat
 */
object BlinkChatRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHAT = "chat/{groupId}"
    
    fun chatRoute(groupId: String) = "chat/$groupId"
}
