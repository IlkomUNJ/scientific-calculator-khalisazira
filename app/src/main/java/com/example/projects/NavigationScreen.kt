package com.example.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Assuming these imports exist in your project structure
import com.example.projects.calculator.CalculatorScreen
import com.example.projects.basics.BasicsCodelab
import com.example.projects.notepad.NotepadScreen

// --- Route Definitions ---
object Routes {
    const val HOME = "home"
    const val CALCULATOR = "calculator"
    const val BASIC_CODELAB = "basic_codelab" // Fixed typo
    const val NOTEPAD = "notepad"
}

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Fixed: Suppress experimental API warning for Scaffold
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToCalculator = { navController.navigate(Routes.CALCULATOR) },
                    onNavigateToCodelab = { navController.navigate(Routes.BASIC_CODELAB) },
                    onNavigateToNotepad = { navController.navigate(Routes.NOTEPAD) }
                )
            }

            composable(Routes.CALCULATOR) {
                CalculatorScreen()
            }

            composable(Routes.BASIC_CODELAB) {
                BasicsCodelab()
            }

            composable(Routes.NOTEPAD) {
                NotepadScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToCalculator: () -> Unit,
    onNavigateToCodelab: () -> Unit,
    onNavigateToNotepad: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Project Hub", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onNavigateToCalculator, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Calculator")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToCodelab, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Basic Codelab")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToNotepad, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Notepad")
        }
    }
}