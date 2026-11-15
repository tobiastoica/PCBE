package com.example.micromobility.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.micromobility.di.AppContainer
import com.example.micromobility.ui.auth.AuthViewModel
import com.example.micromobility.ui.auth.LoginScreen
import com.example.micromobility.ui.auth.RegisterScreen
import com.example.micromobility.ui.home.MainShellScreen
import com.example.micromobility.ui.map.MapDiscoveryViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNavGraph(container: AppContainer) {
    val isSessionLoaded by container.sessionStorage.isLoaded.collectAsStateWithLifecycle()
    val currentSession by container.sessionStorage.session.collectAsStateWithLifecycle()

    if (!isSessionLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val startDestination = remember {
        if (currentSession != null) Routes.HOME else Routes.LOGIN
    }

    LaunchedEffect(currentSession) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentSession != null && currentRoute != Routes.HOME) {
            navController.navigate(Routes.HOME) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentSession == null && currentRoute == Routes.HOME) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.provideFactory(container.authRepository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = uiState,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::login,
                onDemoLoginClick = viewModel::loginAsDemoUser,
                onNavigateToRegister = {
                    viewModel.clearFormErrors()
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.provideFactory(container.authRepository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            RegisterScreen(
                uiState = uiState,
                onFullNameChange = viewModel::onFullNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onRegisterClick = viewModel::register,
                onNavigateBackToLogin = {
                    viewModel.clearFormErrors()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            val session = currentSession
            if (session != null) {
                val mapViewModel: MapDiscoveryViewModel = viewModel(
                    factory = MapDiscoveryViewModel.provideFactory(
                        locationTracker = container.locationTracker,
                        fleetRepository = container.fleetRepository
                    )
                )
                MainShellScreen(
                    session = session,
                    mapViewModel = mapViewModel,
                    onLogoutClick = {
                        scope.launch { container.authRepository.logout() }
                    }
                )
            }
        }
    }
}