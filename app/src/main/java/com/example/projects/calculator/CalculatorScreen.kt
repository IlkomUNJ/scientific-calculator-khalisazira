package com.example.projects.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.expression) {
        coroutineScope.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }
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
                if (text == "Inv" && state.inverseModeEnabled) {
                    Color(0xFF808080)
                } else {
                    DarkScientificGray
                }
            }

            else -> DarkButtonGray
        }
    }

    val modeToggleLabel = if (state.scientificModeEnabled) "BASIC" else "SCI"
    val basicBottomRow = listOf(modeToggleLabel, "0", ",", "=")

    val sinLabel = if (state.inverseModeEnabled) "sin⁻¹" else "sin"
    val cosLabel = if (state.inverseModeEnabled) "cos⁻¹" else "cos"
    val tanLabel = if (state.inverseModeEnabled) "tan⁻¹" else "tan"

    val scientificRows = listOf(
        listOf("Inv", "1/x", "xʸ", "x!", "√"),
        listOf(sinLabel, cosLabel, tanLabel, "log", "ln")
    )

    val standardRows = listOf(
        listOf(viewModel.acButtonText, "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
    )

    val allButtonTexts = if (state.scientificModeEnabled) {
        scientificRows + standardRows + listOf(basicBottomRow)
    } else {
        standardRows + listOf(basicBottomRow)
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

                    text = state.expression
                        .replace(".", ",")
                        .replace("*", "×")
                        .replace("/", "÷")
                        .replace("^", "ʸ"),
                    modifier = Modifier.padding(end = 8.dp),
                    fontSize = when {
                        state.expression.length > 30 -> 24.sp
                        state.expression.length > 25 -> 32.sp
                        state.expression.length > 20 -> 40.sp
                        state.expression.length > 15 -> 56.sp
                        state.expression.length > 10 -> 72.sp
                        state.expression.length > 5 -> 85.sp
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

                                onClick = { viewModel.handleButtonClick(buttonText) },
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
    CalculatorScreen()
}
