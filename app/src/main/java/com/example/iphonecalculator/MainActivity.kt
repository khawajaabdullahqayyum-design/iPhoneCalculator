package com.example.iphonecalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView

    private var currentInput = StringBuilder("0")
    private var firstOperand: Double? = null
    private var pendingOperator: String? = null
    private var isNewInput = false
    private var justEvaluated = false
    private var lastOperator: String? = null
    private var lastOperand: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full screen immersive
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)

        val btnIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )

        for ((id, value) in btnIds) {
            findViewById<Button>(id).setOnClickListener { onDigitPressed(value) }
        }

        findViewById<Button>(R.id.btnAC).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onToggleSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercent() }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator("÷") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperator("×") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator("−") }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEquals() }

        updateAcButton()
    }

    private fun onDigitPressed(digit: String) {
        if (justEvaluated && digit != ".") {
            currentInput = StringBuilder("0")
            justEvaluated = false
            isNewInput = false
        }

        if (isNewInput) {
            currentInput = StringBuilder("0")
            isNewInput = false
        }

        if (digit == ".") {
            if (!currentInput.contains(".")) {
                currentInput.append(".")
            }
        } else if (currentInput.toString() == "0") {
            currentInput = StringBuilder(digit)
        } else {
            if (currentInput.length < 9) {
                currentInput.append(digit)
            }
        }

        updateDisplay(currentInput.toString())
        updateAcButton()
    }

    private fun onOperator(op: String) {
        if (pendingOperator != null && !isNewInput) {
            val result = calculate(firstOperand!!, currentInput.toString().toDouble(), pendingOperator!!)
            firstOperand = result
            updateDisplay(formatResult(result))
            currentInput = StringBuilder(formatResult(result))
        } else {
            firstOperand = currentInput.toString().toDouble()
        }

        pendingOperator = op
        lastOperator = op
        isNewInput = true
        justEvaluated = false

        highlightOperator(op)
    }

    private fun onEquals() {
        if (justEvaluated) {
            if (lastOperator != null && lastOperand != null) {
                val result = calculate(currentInput.toString().toDouble(), lastOperand!!, lastOperator!!)
                updateDisplay(formatResult(result))
                currentInput = StringBuilder(formatResult(result))
            }
            return
        }

        if (firstOperand == null || pendingOperator == null) {
            justEvaluated = true
            return
        }

        lastOperand = currentInput.toString().toDouble()
        lastOperator = pendingOperator

        val result = calculate(firstOperand!!, lastOperand!!, pendingOperator!!)
        updateDisplay(formatResult(result))
        currentInput = StringBuilder(formatResult(result))

        firstOperand = null
        pendingOperator = null
        isNewInput = false
        justEvaluated = true

        clearOperatorHighlight()
    }

    private fun calculate(a: Double, b: Double, op: String): Double {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            else -> b
        }
    }

    private fun onClear() {
        if (currentInput.toString() != "0" && !justEvaluated) {
            currentInput = StringBuilder("0")
            updateDisplay("0")
        } else {
            currentInput = StringBuilder("0")
            firstOperand = null
            pendingOperator = null
            isNewInput = false
            justEvaluated = false
            lastOperator = null
            lastOperand = null
            updateDisplay("0")
            clearOperatorHighlight()
        }
        updateAcButton()
    }

    private fun onToggleSign() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        val toggled = value * -1
        currentInput = StringBuilder(formatResult(toggled))
        updateDisplay(currentInput.toString())
    }

    private fun onPercent() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        val result = if (firstOperand != null && (pendingOperator == "+" || pendingOperator == "−")) {
            firstOperand!! * value / 100.0
        } else {
            value / 100.0
        }
        currentInput = StringBuilder(formatResult(result))
        updateDisplay(currentInput.toString())
        justEvaluated = true
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Error"

        return if (value == Math.floor(value) && !value.isInfinite() && Math.abs(value) < 1_000_000_000.0) {
            val longVal = value.toLong()
            longVal.toString()
        } else {
            val rounded = BigDecimal(value).round(MathContext(9, RoundingMode.HALF_UP))
            val result = rounded.stripTrailingZeros().toPlainString()
            if (result.length > 9) {
                BigDecimal(value).round(MathContext(6, RoundingMode.HALF_UP))
                    .stripTrailingZeros().toPlainString()
            } else result
        }
    }

    private fun updateDisplay(text: String) {
        tvResult.text = text
        val length = text.length
        tvResult.textSize = when {
            length <= 6 -> 80f
            length <= 9 -> 60f
            else -> 40f
        }
    }

    private fun updateAcButton() {
        val btn = findViewById<Button>(R.id.btnAC)
        btn.text = if (currentInput.toString() == "0" || justEvaluated) "AC" else "C"
    }

    private fun highlightOperator(op: String) {
        clearOperatorHighlight()
        val btn = when (op) {
            "÷" -> findViewById<Button>(R.id.btnDivide)
            "×" -> findViewById<Button>(R.id.btnMultiply)
            "−" -> findViewById<Button>(R.id.btnMinus)
            "+" -> findViewById<Button>(R.id.btnPlus)
            else -> null
        }
        btn?.setBackgroundResource(R.drawable.btn_orange_selected)
    }

    private fun clearOperatorHighlight() {
        listOf(R.id.btnDivide, R.id.btnMultiply, R.id.btnMinus, R.id.btnPlus).forEach {
            findViewById<Button>(it).setBackgroundResource(R.drawable.btn_orange)
        }
    }
}
