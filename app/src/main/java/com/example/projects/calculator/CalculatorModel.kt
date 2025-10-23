package com.example.projects.calculator

import androidx.compose.ui.graphics.Color
data class CalculatorState(
    val expression: String = "0",
    val lastButtonIsOperatorOrEquals: Boolean = false,
    val scientificModeEnabled: Boolean = false,
    val inverseModeEnabled: Boolean = false,
    val isTypingActive: Boolean = false,
)

val DarkScientificGray = Color(0xFF2A2A2A)
val DarkButtonGray = Color(0xFF3A3A3A)
val TopRowGray = Color(0xFFD4D4D2)
val AccentOrange = Color(0xFFFF9F0A)
val White = Color.White
val Black = Color(0xFF1C1C1C)

val functionButtons = listOf("sin", "cos", "tan", "log", "ln")
val inverseFunctionButtons = listOf("sin⁻¹", "cos⁻¹", "tan⁻¹")
val allFunctionButtons = functionButtons + inverseFunctionButtons
