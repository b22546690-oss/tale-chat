package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize local persistence database, DAO, and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()
        val repository = ChatRepository(chatDao)
        val viewModelFactory = ChatViewModelFactory(repository)

        setContent {
            // Instantiate centralized ViewModel
            val viewModel: ChatViewModel = viewModel(factory = viewModelFactory)
            
            // Reactively bind app theme to dark/light toggle in user settings
            val isDarkTheme by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: ChatViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    // Observe authState changes to steer navigation
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.PhoneInput -> {
                navController.navigate("auth_phone") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.EmailInput -> {
                navController.navigate("auth_email") {
                    popUpTo("auth_phone") { inclusive = false }
                }
            }
            is AuthState.NameInput -> {
                navController.navigate("auth_name") {
                    popUpTo("auth_email") { inclusive = false }
                }
            }
            is AuthState.Authenticated -> {
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "auth_phone"
    ) {
        composable("auth_phone") {
            PhoneScreen(
                viewModel = viewModel,
                onContinue = { /* Navigation handled by AuthState observer */ }
            )
        }
        composable("auth_email") {
            EmailScreen(
                viewModel = viewModel,
                onContinue = { /* Navigation handled by AuthState observer */ },
                onBack = { viewModel.logout() }
            )
        }
        composable("auth_name") {
            NameScreen(
                viewModel = viewModel,
                onComplete = { /* Navigation handled by AuthState observer */ },
                onBack = { viewModel.submitEmail(viewModel.tempEmail) } // returns to email screen state
            )
        }
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToChat = { chatId ->
                    navController.navigate("chat_detail/$chatId")
                }
            )
        }
        composable(
            route = "chat_detail/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(
                chatId = chatId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
