package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.example.ui.theme.bouncyClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.IronLogRepository
import com.example.ui.home.HomeScreen
import com.example.ui.debug.DiagnosticsScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.login.LoginScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.progress.PlateCalculatorScreen
import com.example.ui.workout.ActiveWorkoutScreen
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.*

@Composable
fun IronLogApp(repository: IronLogRepository) {
    com.example.ui.error.ErrorBoundary {
        val navController = rememberNavController()
        val authContext = com.example.ui.auth.LocalAuthProvider.current

        if (!authContext.isAuthResolved) {
            Box(modifier = Modifier.fillMaxSize().background(BgColor))
            return@ErrorBoundary
        }

        val startDestination = remember(authContext.currentUser) {
            if (authContext.currentUser != null) "main" else "login"
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("main") {
                ProtectedRoute(navController = navController) {
                    MainScreenWrapper(repository, navController)
                }
            }
            composable("active_workout") {
                ProtectedRoute(navController = navController) {
                    ActiveWorkoutScreen(
                        repository = repository,
                        onNavigateToPlateCalc = { navController.navigate("plate_calculator") },
                        onFinish = {
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable("plate_calculator") {
                ProtectedRoute(navController = navController) {
                    PlateCalculatorScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("diagnostics") {
                DiagnosticsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun ProtectedRoute(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val authContext = com.example.ui.auth.LocalAuthProvider.current
    
    if (!authContext.isAuthResolved) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.width(100.dp).height(8.dp).skeleton().clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "RESOLVING IDENTITY STATE...",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    } else if (authContext.currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.width(40.dp).height(40.dp).skeleton().clip(RoundedCornerShape(20.dp)))
        }
    } else {
        content()
    }
}

@Composable
fun MainScreenWrapper(
    repository: IronLogRepository,
    rootNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    
    val items = listOf(
        Triple("home", Icons.Outlined.Home, "Home"),
        Triple("programs", Icons.Outlined.Assignment, "Program"),
        Triple("plate_calculator", Icons.Outlined.Calculate, "Plates"),
        Triple("progress", Icons.Outlined.Timeline, "Progress"),
        Triple("prs", Icons.Outlined.Star, "PRs"),
        Triple("history", Icons.Outlined.History, "History")
    )

    Scaffold(
        containerColor = BgColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x8)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                        .padding(horizontal = IronSpacing.x12, vertical = IronSpacing.x8),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
                    
                    items.forEach { (route, icon, label) ->
                        val isSelected = currentRoute == route
                        val contentColor = if (isSelected) TextPrimaryColor else TextPrimaryColor.copy(alpha = 0.35f)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .bouncyClick {
                                    if (currentRoute != route) {
                                        bottomNavController.navigate(route) {
                                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                .padding(vertical = IronSpacing.x4)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(IronSpacing.x4))
                            Text(
                                text = label,
                                style = IronTypography.Micro.copy(color = contentColor),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val auth = FirebaseAuth.getInstance()
                HomeScreen(
                    repository = repository,
                    onStartWorkout = { generatedWorkout ->
                        coroutineScope.launch {
                            repository.saveWorkout(generatedWorkout)
                            rootNavController.navigate("active_workout")
                        }
                    },
                    onResumeWorkout = {
                        rootNavController.navigate("active_workout")
                    },
                    onProfileClick = {
                        bottomNavController.navigate("profile") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTab = { route ->
                        bottomNavController.navigate(route) {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToDiagnostics = {
                        rootNavController.navigate("diagnostics")
                    }
                )
            }
            composable("programs") {
                com.example.ui.programs.ProgramsScreen(
                    repository = repository,
                    onProgramStarted = {
                        // Navigate home FIRST so it's the backstack under active_workout
                        bottomNavController.navigate("home") {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                        // Then navigate to active workout on the root controller
                        rootNavController.navigate("active_workout")
                    }
                )
            }
            composable("progress") {
                ProgressScreen(repository)
            }
            composable("prs") {
                com.example.ui.prs.PRsScreen(repository = repository)
            }
            composable("history") {
                HistoryScreen(repository)
            }
            composable("plate_calculator") {
                PlateCalculatorScreen(onBack = { 
                    bottomNavController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                })
            }
            composable("profile") {
                com.example.ui.profile.ProfileScreen(
                    repository = repository,
                    onSignOutClick = {
                        coroutineScope.launch {
                            repository.signOut()
                            rootNavController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onLoginClick = {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPlateCalc = {
                        rootNavController.navigate("plate_calculator")
                    }
                )
            }
        }
    }
}
