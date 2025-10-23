package com.example.projects.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import java.text.DecimalFormat
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()


    val acButtonText: String
        get() = if (_state.value.isTypingActive) "⌫" else "AC"

    private fun evaluateExpression(exp: String): String {
        return try {

            val sanitizedExp = exp
                .replace("×", "*")
                .replace("÷", "/")
                .replace(",", ".")
                .replace("sin(", "sind(")
                .replace("cos(", "cosd(")
                .replace("tan(", "tand(")
                .replace("log(", "log10(")
                .replace("ln(", "log(")
                .replace("sin⁻¹(", "asind(")
                .replace("cos⁻¹(", "acosd(")
                .replace("tan⁻¹(", "atand(")

            val expressionBuilder = ExpressionBuilder(sanitizedExp)

                .function(object : Function("sind", 1) {
                    override fun apply(vararg args: Double): Double = sin(Math.toRadians(args[0]))
                })
                .function(object : Function("cosd", 1) {
                    override fun apply(vararg args: Double): Double = cos(Math.toRadians(args[0]))
                })
                .function(object : Function("tand", 1) {
                    override fun apply(vararg args: Double): Double = tan(Math.toRadians(args[0]))
                })
                .function(object : Function("asind", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(asin(args[0]))
                })
                .function(object : Function("acosd", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(acos(args[0]))
                })
                .function(object : Function("atand", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(atan(args[0]))
                })
                .build()

            val result = expressionBuilder.evaluate()
            val df = DecimalFormat("#.#########")
            df.format(result).toString().replace(".", ",")
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun applyUnaryFunction(buttonText: String) {
        val currentState = _state.value
        if (!currentState.lastButtonIsOperatorOrEquals && !currentState.expression.contains(Regex("[+\\-×÷^]")) && currentState.expression != "Error") {
            try {
                val currentNumStr = currentState.expression.replace(",", ".")
                val currentValue = currentNumStr.toDouble()
                val result: Double = when (buttonText) {
                    "√" -> sqrt(currentValue)
                    "1/x" -> 1.0 / currentValue
                    "x!" -> {
                        if (currentValue.toLong().toDouble() == currentValue && currentValue >= 0 && currentValue <= 20) {
                            (1..currentValue.toLong()).fold(1.0) { acc, i -> acc * i }
                        } else { Double.NaN }
                    }
                    else -> currentValue
                }

                _state.update {
                    if (result.isNaN() || result.isInfinite()) {
                        it.copy(expression = "Error", lastButtonIsOperatorOrEquals = true)
                    } else {
                        val df = DecimalFormat("#.#########")
                        it.copy(
                            expression = df.format(result).toString().replace(".", ","),
                            lastButtonIsOperatorOrEquals = true
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(expression = "Error", lastButtonIsOperatorOrEquals = true) }
            }
        } else {
            _state.update { it.copy(lastButtonIsOperatorOrEquals = true) }
        }
    }

    fun handleButtonClick(buttonText: String) {
        val currentState = _state.value

        when (buttonText) {
            "SCI", "BASIC" -> {
                _state.update {
                    it.copy(
                        scientificModeEnabled = !it.scientificModeEnabled,
                        inverseModeEnabled = false,
                        expression = "0",
                        isTypingActive = false,
                        lastButtonIsOperatorOrEquals = false
                    )
                }
                return
            }
            "Inv" -> {
                _state.update { it.copy(inverseModeEnabled = !it.inverseModeEnabled) }
                return
            }
        }

        if (currentState.scientificModeEnabled && listOf("√", "x!", "1/x").contains(buttonText)) {
            applyUnaryFunction(buttonText)
            return
        }

        if (currentState.scientificModeEnabled && allFunctionButtons.contains(buttonText)) {
            val baseFunction = when {
                currentState.inverseModeEnabled && buttonText in functionButtons -> buttonText + "⁻¹"
                !currentState.inverseModeEnabled && buttonText in inverseFunctionButtons -> buttonText.removeSuffix("⁻¹")
                else -> buttonText
            }

            _state.update {
                val newExpression = if (it.expression == "0" || it.lastButtonIsOperatorOrEquals) {
                    ""
                } else {
                    it.expression
                }
                it.copy(
                    expression = newExpression + baseFunction + "()",
                    lastButtonIsOperatorOrEquals = true,
                    isTypingActive = true
                )
            }
            return
        }

        _state.update { current ->
            var newExpression = current.expression
            var newLastButtonIsOperatorOrEquals = current.lastButtonIsOperatorOrEquals
            var newIsTypingActive = current.isTypingActive

            when (buttonText) {
                acButtonText -> {
                    if (newIsTypingActive) {
                        if (newExpression.length > 1 && newExpression != "Error") {
                            if (newExpression.endsWith("()")) {
                                if (newExpression.lastIndex > 1 && newExpression[newExpression.lastIndex-1] != '(') {
                                    newExpression = newExpression.dropLast(2) + ")"
                                } else {
                                    newExpression = newExpression.replaceFirst(Regex("(sin|cos|tan|log|ln|sin⁻¹|cos⁻¹|tan⁻¹)\\(\\)"), "")
                                    if (newExpression.isEmpty()) newExpression = "0"
                                }
                            } else {
                                newExpression = newExpression.dropLast(1)
                            }
                        } else {
                            newExpression = "0"
                            newIsTypingActive = false
                        }
                        if (newExpression.isEmpty()) newExpression = "0"
                    } else { // AC
                        newExpression = "0"
                        newLastButtonIsOperatorOrEquals = false
                        newIsTypingActive = false
                    }
                }
                "=" -> {
                    if (newExpression.isNotEmpty() && newExpression != "0" && !newLastButtonIsOperatorOrEquals) {
                        var finalExpression = newExpression
                        val trailingOperatorRegex = Regex("[+\\-×÷^]$")
                        if (finalExpression.contains(trailingOperatorRegex)) {
                            finalExpression = finalExpression.dropLast(1)
                        }

                        val openParentheses = finalExpression.count { it == '(' }
                        val closeParentheses = finalExpression.count { it == ')' }

                        repeat(openParentheses - closeParentheses) {
                            finalExpression += ")"
                        }

                        val result = evaluateExpression(finalExpression)
                        newExpression = if (result != "Error") result else "Error"
                        newLastButtonIsOperatorOrEquals = true
                        newIsTypingActive = false
                    }
                }
                "÷", "×", "-", "+", "^", "xʸ" -> {
                    val operator = when (buttonText) {
                        "×" -> "×"
                        "÷" -> "÷"
                        "xʸ" -> "^"
                        else -> buttonText
                    }

                    if (newExpression != "Error") {
                        if (newLastButtonIsOperatorOrEquals && newExpression.lastOrNull() !in listOf('+', '-', '×', '÷', '^')) {
                            newExpression += operator
                        } else if (newExpression.lastOrNull() in listOf('+', '-', '×', '÷', '^')) {
                            newExpression = newExpression.dropLast(1) + operator
                        } else {
                            newExpression += operator
                        }
                        newLastButtonIsOperatorOrEquals = true
                        newIsTypingActive = true
                    }
                }
                "±", "%" -> {
                    if (newExpression == "Error") return@update current
                    newIsTypingActive = true

                    if (buttonText == "±") {
                        if (!newExpression.contains(Regex("[+\\-×÷^()]")) && newExpression != "0") {
                            try {
                                val currentValue = newExpression.replace(",", ".").toDouble()
                                val resultValue = -currentValue
                                val df = DecimalFormat("#.#########")
                                newExpression = df.format(resultValue).toString().replace(".", ",")
                            } catch (e: NumberFormatException) { }
                        }
                    } else { // %
                        if (!newExpression.contains(Regex("[+\\-×÷^()]")) && newExpression != "0") {
                            try {
                                val currentValue = newExpression.replace(",", ".").toDouble()
                                val resultValue = currentValue / 100
                                val df = DecimalFormat("#.#########")
                                newExpression = df.format(resultValue).toString().replace(".", ",")
                            } catch (e: NumberFormatException) { }
                        }
                    }
                }
                "," -> {
                    if (newExpression == "Error") return@update current

                    val endsWithOperator = newExpression.lastOrNull() in listOf('+', '-', '×', '÷', '^')

                    if (endsWithOperator) {
                        newExpression += "0,"
                    }
                    else if (newExpression.endsWith("()")) {
                        newExpression = newExpression.dropLast(1) + "0,)"
                    }
                    else if (newLastButtonIsOperatorOrEquals || newExpression == "0") {
                        newExpression = "0,"
                    }
                    else {
                        val lastNumberMatch = Regex("([+\\-×÷^()]?)(\\d+)(,?)(\\d*)$").find(newExpression)
                        val lastNumberString = lastNumberMatch?.groupValues?.get(0) ?: ""

                        if (!lastNumberString.contains(',')) {
                            if (newExpression.endsWith(")")) {
                                newExpression = newExpression.dropLast(1) + "," + ")"
                            } else {
                                newExpression += ","
                            }
                        }
                    }
                    newLastButtonIsOperatorOrEquals = false
                    newIsTypingActive = true
                }
                else -> { // Digits
                    val digit = buttonText
                    if (newExpression == "Error") newExpression = "0"

                    val endsWithOperator = newExpression.lastOrNull() in listOf('+', '-', '×', '÷', '^')

                    if (newExpression == "0" || newLastButtonIsOperatorOrEquals) {
                        if (endsWithOperator) {
                            newExpression += digit
                        }
                        else if (newExpression.endsWith("()")) {
                            newExpression = newExpression.dropLast(1) + digit + ")"
                        }
                        else {
                            newExpression = digit
                        }
                        newLastButtonIsOperatorOrEquals = false
                    } else {
                        if (newExpression.endsWith(")")) {
                            newExpression = newExpression.dropLast(1) + digit + ")"
                        } else {
                            newExpression += digit
                        }
                        newLastButtonIsOperatorOrEquals = false
                    }
                    newIsTypingActive = true
                }
            }

            current.copy(
                expression = newExpression,
                lastButtonIsOperatorOrEquals = newLastButtonIsOperatorOrEquals,
                isTypingActive = newIsTypingActive
            )
        }
    }
}
