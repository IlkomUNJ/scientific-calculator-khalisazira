package com.example.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.calculator.ui.theme.CalculatorTheme
import net.objecthunter.exp4j.ExpressionBuilder
import java.text.DecimalFormat
import kotlin.math.*
import kotlinx.coroutines.launch

val DarkScientificGray = Color(0xFF2A2A2A)
val DarkButtonGray = Color(0xFF3A3A3A)
val TopRowGray = Color(0xFFD4D4D2)
val AccentOrange = Color(0xFFFF9F0A)
val White = Color.White
val Black = Color(0xFF1C1C1C)

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
) {
    var expression by rememberSaveable { mutableStateOf("0") }
    var lastButtonIsOperatorOrEquals by rememberSaveable { mutableStateOf(false) }
    var scientificModeEnabled by rememberSaveable { mutableStateOf(false) }
    var inverseModeEnabled by rememberSaveable { mutableStateOf(false) }
    var isTypingActive by rememberSaveable { mutableStateOf(false) }

    val functionButtons = remember { listOf("sin", "cos", "tan", "log", "ln") }
    val inverseFunctionButtons = remember { listOf("sin⁻¹", "cos⁻¹", "tan⁻¹") }
    val allFunctionButtons = remember { functionButtons + inverseFunctionButtons }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(expression) {
        coroutineScope.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    fun evaluateExpression(exp: String): String {
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
                .function(object : net.objecthunter.exp4j.function.Function("sind", 1) {
                    override fun apply(vararg args: Double): Double = kotlin.math.sin(Math.toRadians(args[0]))
                })
                .function(object : net.objecthunter.exp4j.function.Function("cosd", 1) {
                    override fun apply(vararg args: Double): Double = kotlin.math.cos(Math.toRadians(args[0]))
                })
                .function(object : net.objecthunter.exp4j.function.Function("tand", 1) {
                    override fun apply(vararg args: Double): Double = kotlin.math.tan(Math.toRadians(args[0]))
                })
                .function(object : net.objecthunter.exp4j.function.Function("asind", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.asin(args[0]))
                })
                .function(object : net.objecthunter.exp4j.function.Function("acosd", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.acos(args[0]))
                })
                .function(object : net.objecthunter.exp4j.function.Function("atand", 1) {
                    override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.atan(args[0]))
                })
                .build()

            val result = expressionBuilder.evaluate()

            val df = DecimalFormat("#.#########")
            val formattedResult = df.format(result).toString()

            formattedResult.replace(".", ",")
        } catch (e: Exception) {
            "Error"
        }
    }

    val applyUnaryFunction: (String) -> Unit = { buttonText ->
        if (!lastButtonIsOperatorOrEquals && !expression.contains(Regex("[+\\-×÷^]")) && expression != "Error") {
            try {
                val currentNumStr = expression.replace(",", ".")
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

                if (result.isNaN() || result.isInfinite()) {
                    expression = "Error"
                } else {
                    val df = DecimalFormat("#.#########")
                    expression = df.format(result).toString().replace(".", ",")
                }
            } catch (e: Exception) {
                expression = "Error"
            }
        }
        lastButtonIsOperatorOrEquals = true
    }

    val acButtonText = if (isTypingActive) "⌫" else "AC"

    val onButtonClick: (String) -> Unit = buttonClick@{ buttonText ->
        if (buttonText == "SCI" || buttonText == "BASIC") {
            scientificModeEnabled = !scientificModeEnabled
            inverseModeEnabled = false
            expression = "0"
            isTypingActive = false
            lastButtonIsOperatorOrEquals = false
            return@buttonClick
        }

        if (buttonText == "Inv") {
            inverseModeEnabled = !inverseModeEnabled
            return@buttonClick
        }

        if (scientificModeEnabled && listOf("√", "x!", "1/x").contains(buttonText)) {
            applyUnaryFunction(buttonText)
            return@buttonClick
        }

        if (scientificModeEnabled && allFunctionButtons.contains(buttonText)) {
            val baseFunction = when {
                inverseModeEnabled && buttonText in functionButtons -> buttonText + "⁻¹"
                !inverseModeEnabled && buttonText in inverseFunctionButtons -> buttonText.removeSuffix("⁻¹")
                else -> buttonText
            }

            if (expression == "0" || lastButtonIsOperatorOrEquals) {
                expression = ""
            }
            expression += baseFunction + "()"
            lastButtonIsOperatorOrEquals = true
            isTypingActive = true
            return@buttonClick
        }

        when (buttonText) {
            acButtonText -> {
                if (isTypingActive) {
                    if (expression.length > 1 && expression != "Error") {
                        if (expression.endsWith("()")) {
                            if (expression.lastIndex > 1 && expression[expression.lastIndex-1] != '(') {
                                expression = expression.dropLast(2) + ")"
                            } else {
                                expression = expression.replaceFirst(Regex("(sin|cos|tan|log|ln|sin⁻¹|cos⁻¹|tan⁻¹)\\(\\)"), "")
                                if (expression.isEmpty()) expression = "0"
                            }
                        } else {
                            expression = expression.dropLast(1)
                        }
                    } else {
                        expression = "0"
                        isTypingActive = false
                    }
                    if (expression.isEmpty()) expression = "0"

                } else {
                    expression = "0"
                    lastButtonIsOperatorOrEquals = false
                    isTypingActive = false
                }
            }
            "=" -> {
                if (expression.isNotEmpty() && expression != "0" && !lastButtonIsOperatorOrEquals) {

                    var finalExpression = expression

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
                    expression = if (result != "Error") result else "Error"
                    lastButtonIsOperatorOrEquals = true
                    isTypingActive = false
                }
            }
            "÷", "×", "-", "+", "^", "xʸ" -> {
                val operator = when (buttonText) {
                    "×" -> "×"
                    "÷" -> "÷"
                    "xʸ" -> "^"
                    else -> buttonText
                }

                if (expression == "Error") return@buttonClick

                if (lastButtonIsOperatorOrEquals && expression.lastOrNull() !in listOf('+', '-', '×', '÷', '^')) {
                    expression += operator
                } else if (expression.lastOrNull() in listOf('+', '-', '×', '÷', '^')) {
                    expression = expression.dropLast(1) + operator
                } else {
                    expression += operator
                }
                lastButtonIsOperatorOrEquals = true
                isTypingActive = true
            }

            "±", "%" -> {
                if (expression == "Error") return@buttonClick
                isTypingActive = true

                if (buttonText == "±") {
                    if (!expression.contains(Regex("[+\\-×÷^()]")) && expression != "0") {
                        try {
                            val currentValue = expression.replace(",", ".").toDouble()
                            val resultValue = -currentValue
                            val df = DecimalFormat("#.#########")
                            expression = df.format(resultValue).toString().replace(".", ",")
                        } catch (e: NumberFormatException) { }
                    }
                } else {
                    if (!expression.contains(Regex("[+\\-×÷^()]")) && expression != "0") {
                        try {
                            val currentValue = expression.replace(",", ".").toDouble()
                            val resultValue = currentValue / 100
                            val df = DecimalFormat("#.#########")
                            expression = df.format(resultValue).toString().replace(".", ",")
                        } catch (e: NumberFormatException) { }
                    }
                }
            }

            "," -> {
                if (expression == "Error") return@buttonClick

                val lastNumberMatch = Regex("([+\\-×÷^()]?)(\\d+)(,?)(\\d*)$").find(expression)

                if (lastNumberMatch == null || lastButtonIsOperatorOrEquals || expression.lastOrNull() in listOf('+', '-', '×', '÷', '^')) {

                    if (expression.lastOrNull() in listOf('+', '-', '×', '÷', '^')) {
                        expression += "0,"
                    }
                    else if (expression.endsWith("()")) {
                        expression = expression.dropLast(1) + "0,)"
                    } else if (lastButtonIsOperatorOrEquals || expression == "0") {
                        expression = "0,"
                    } else {
                        expression += ","
                    }
                } else {
                    val lastNumberString = lastNumberMatch.groupValues[0]
                    if (!lastNumberString.contains(',')) {
                        if (expression.endsWith(")")) {
                            expression = expression.dropLast(1) + "," + ")"
                        } else {
                            expression += ","
                        }
                    }
                }
                lastButtonIsOperatorOrEquals = false
                isTypingActive = true
            }

            else -> {
                val digit = buttonText
                if (expression == "Error") expression = "0"

                val endsWithOperator = expression.lastOrNull() in listOf('+', '-', '×', '÷', '^')

                if (expression == "0" || lastButtonIsOperatorOrEquals) {

                    if (endsWithOperator) {
                        expression += digit
                        lastButtonIsOperatorOrEquals = false
                    }
                    else if (expression.endsWith("()")) {
                        expression = expression.dropLast(1) + digit + ")"
                        lastButtonIsOperatorOrEquals = false
                    }
                    else {
                        expression = digit
                        lastButtonIsOperatorOrEquals = false
                    }
                } else {
                    if (expression.endsWith(")")) {
                        expression = expression.dropLast(1) + digit + ")"
                    } else {
                        expression += digit
                    }
                    lastButtonIsOperatorOrEquals = false
                }
                isTypingActive = true
            }
        }
    }

    val sinLabel = if (inverseModeEnabled) "sin⁻¹" else "sin"
    val cosLabel = if (inverseModeEnabled) "cos⁻¹" else "cos"
    val tanLabel = if (inverseModeEnabled) "tan⁻¹" else "tan"

    val scientificRows = listOf(
        listOf("Inv", "1/x", "xʸ", "x!", "√"),
        listOf(sinLabel, cosLabel, tanLabel, "log", "ln")
    )

    val modeToggleLabel = if (scientificModeEnabled) "BASIC" else "SCI"
    val basicBottomRow = listOf(modeToggleLabel, "0", ",", "=")

    val buttonRowsBasic = listOf(
        listOf(acButtonText, "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        basicBottomRow
    )

    val allButtonTexts = if (scientificModeEnabled) {
        scientificRows + buttonRowsBasic
    } else {
        buttonRowsBasic
    }

    @Composable
    fun getButtonColor(text: String): Color {
        val scientificFunctionButtons = listOf(
            "Inv", "1/x", "xʸ", "x!", "sin", "cos", "tan", "√", "log", "ln",
            "sin⁻¹", "cos⁻¹", "tan⁻¹"
        )

        return when (text) {
            "AC", "⌫", "±", "%" -> TopRowGray
            "÷", "×", "-", "+", "=" -> AccentOrange

            "SCI", "BASIC" -> DarkButtonGray

            in scientificFunctionButtons -> {
                if (text == "Inv" && inverseModeEnabled) {
                    Color(0xFF808080)
                } else {
                    DarkScientificGray
                }
            }

            else -> DarkButtonGray
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = expression
                        .replace(".", ",")
                        .replace("*", "×")
                        .replace("/", "÷")
                        .replace("^", "ʸ"),
                    modifier = Modifier
                        .padding(end = 8.dp),
                    fontSize = when {
                        expression.length > 30 -> 24.sp
                        expression.length > 25 -> 32.sp
                        expression.length > 20 -> 40.sp
                        expression.length > 15 -> 56.sp
                        expression.length > 10 -> 72.sp
                        expression.length > 5 -> 85.sp
                        else -> 100.sp
                    },
                    fontWeight = FontWeight.Light,
                    color = White,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                allButtonTexts.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { buttonText ->
                            val weight = 1f
                            val aspectRatio = 1f

                            val buttonModifier = Modifier
                                .weight(weight)
                                .aspectRatio(aspectRatio)

                            CalculatorButton(
                                text = buttonText,
                                modifier = buttonModifier,
                                onClick = { onButtonClick(buttonText) },
                                backgroundColor = getButtonColor(buttonText)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    backgroundColor: Color,
) {
    val textColor = if (backgroundColor == TopRowGray) Black else White
    val fontSize = when (text) {
        "⌫" -> 30.sp
        "SCI", "BASIC" -> 16.sp
        else -> 20.sp
    }

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(1.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 600)
@Composable
fun CalculatorPreview() {
    CalculatorTheme {
        CalculatorScreen()
    }
}