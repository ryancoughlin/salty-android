package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import android.graphics.Rect as AndroidRect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DatasetUnit
import com.example.saltyoffshore.data.TemperatureUnits
import androidx.compose.material3.MaterialTheme
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.utils.GradientScaleUtil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Dual-handle gradient filter bar.
 * Matches iOS FilterGradientBar exactly.
 *
 * Layout (top to bottom):
 * 1. Min/Max text inputs (monospace 16sp)
 * 2. Gradient bar (30dp) with checkerboard + gradient + drag handles (18x36dp)
 */
@Composable
fun FilterGradientBar(
    selectedRange: ClosedFloatingPointRange<Double>?,
    valueRange: ClosedFloatingPointRange<Double>,
    colorscale: Colorscale,
    onRangeChanged: (ClosedFloatingPointRange<Double>?) -> Unit,
    onDragRangeChanged: ((Float, Float) -> Unit)? = null,
    datasetType: DatasetType? = null,
    apiUnit: DatasetUnit = DatasetUnit.FAHRENHEIT,
    temperatureUnits: TemperatureUnits = TemperatureUnits.FAHRENHEIT,
    decimalPlaces: Int = 1,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Local drag state — only used during active drag for 60fps updates
    var isDragging by remember { mutableStateOf(false) }
    var dragMin by remember { mutableDoubleStateOf(valueRange.start) }
    var dragMax by remember { mutableDoubleStateOf(valueRange.endInclusive) }

    // Effective values: use drag state during drag, committed state otherwise
    val effectiveMin = if (isDragging) dragMin else (selectedRange?.start ?: valueRange.start)
    val effectiveMax = if (isDragging) dragMax else (selectedRange?.endInclusive ?: valueRange.endInclusive)

    val gradientColors = remember(colorscale) {
        colorscale.hexColors.map { hex ->
            Color(android.graphics.Color.parseColor(hex))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Min/Max text inputs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterValueField(
                value = effectiveMin,
                apiUnit = apiUnit,
                temperatureUnits = temperatureUnits,
                decimalPlaces = decimalPlaces,
                textAlign = TextAlign.Start,
                onCommit = { newValue ->
                    val clamped = newValue.coerceIn(valueRange.start, effectiveMax)
                    val isDefault = clamped == valueRange.start && effectiveMax == valueRange.endInclusive
                    onRangeChanged(if (isDefault) null else clamped..effectiveMax)
                }
            )

            FilterValueField(
                value = effectiveMax,
                apiUnit = apiUnit,
                temperatureUnits = temperatureUnits,
                decimalPlaces = decimalPlaces,
                textAlign = TextAlign.End,
                onCommit = { newValue ->
                    val clamped = newValue.coerceIn(effectiveMin, valueRange.endInclusive)
                    val isDefault = effectiveMin == valueRange.start && clamped == valueRange.endInclusive
                    onRangeChanged(if (isDefault) null else effectiveMin..clamped)
                }
            )
        }

        // Gradient bar with handles
        GradientBarWithHandles(
            effectiveMin = effectiveMin,
            effectiveMax = effectiveMax,
            valueRange = valueRange,
            gradientColors = gradientColors,
            datasetType = datasetType,
            onDragStart = { isLower ->
                isDragging = true
                dragMin = effectiveMin
                dragMax = effectiveMax
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onDragUpdate = { isLower, newValue ->
                val snapped = GradientScaleUtil.snapValue(newValue, datasetType)
                if (isLower) {
                    val clamped = snapped.coerceIn(valueRange.start, dragMax)
                    if (clamped != dragMin) {
                        dragMin = clamped
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } else {
                    val clamped = snapped.coerceIn(dragMin, valueRange.endInclusive)
                    if (clamped != dragMax) {
                        dragMax = clamped
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                onDragRangeChanged?.invoke(dragMin.toFloat(), dragMax.toFloat())
            },
            onDragEnd = {
                isDragging = false
                val isDefault = dragMin == valueRange.start && dragMax == valueRange.endInclusive
                onRangeChanged(if (isDefault) null else dragMin..dragMax)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        )
    }
}

// ── Gradient Bar with Handles ────────────────────────────────────────────────

private val HANDLE_WIDTH = 18.dp
private val HANDLE_HEIGHT = 36.dp
private val BAR_HEIGHT = 30.dp

@Composable
private fun GradientBarWithHandles(
    effectiveMin: Double,
    effectiveMax: Double,
    valueRange: ClosedFloatingPointRange<Double>,
    gradientColors: List<Color>,
    datasetType: DatasetType?,
    onDragStart: (isLower: Boolean) -> Unit,
    onDragUpdate: (isLower: Boolean, newValue: Double) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current

    val view = LocalView.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(HANDLE_HEIGHT)
            .onGloballyPositioned { coordinates ->
                // Tell Android: don't trigger system back gesture in this area.
                // Without this, edge-swipe back steals horizontal drags from handles.
                val posInWindow = coordinates.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
                val rect = AndroidRect(
                    posInWindow.x.toInt(),
                    posInWindow.y.toInt(),
                    (posInWindow.x + coordinates.size.width).toInt(),
                    (posInWindow.y + coordinates.size.height).toInt()
                )
                view.systemGestureExclusionRects = listOf(rect)
            }
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val handleWidthPx = with(density) { HANDLE_WIDTH.toPx() }
        val availableWidthPx = totalWidthPx - handleWidthPx  // usable space for positioning

        fun valueToFraction(value: Double): Float {
            return GradientScaleUtil.calculatePosition(value, valueRange, datasetType)
                .toFloat().coerceIn(0f, 1f)
        }

        fun fractionToValue(fraction: Float): Double {
            return GradientScaleUtil.calculateValue(
                fraction.toDouble().coerceIn(0.0, 1.0), valueRange, datasetType
            )
        }

        fun valueToOffsetPx(value: Double): Float {
            return valueToFraction(value) * availableWidthPx
        }

        val lowerOffsetPx = valueToOffsetPx(effectiveMin)
        val upperOffsetPx = valueToOffsetPx(effectiveMax)

        // Layer 1: Checkerboard background (filtered areas visible)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(4.dp))
        ) {
            CheckerboardPattern(
                size = CheckerboardSize.LARGE,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: Gradient clipped to handle range
        val gradientLeftDp = with(density) { (lowerOffsetPx + handleWidthPx / 2).toDp() }
        val gradientWidthDp = with(density) { max(0f, upperOffsetPx - lowerOffsetPx).toDp() }

        if (gradientWidthDp > 0.dp) {
            Box(
                modifier = Modifier
                    .width(gradientWidthDp)
                    .height(BAR_HEIGHT)
                    .align(Alignment.CenterStart)
                    .offset(x = gradientLeftDp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (gradientColors.isNotEmpty()) gradientColors
                            else listOf(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surface)
                        )
                    )
            )
        }

        // Layer 3: Lower drag handle
        DragHandle(
            offsetPx = lowerOffsetPx,
            handleWidthPx = handleWidthPx,
            availableWidthPx = availableWidthPx,
            onDragStart = { onDragStart(true) },
            onDrag = { deltaPx ->
                val currentFraction = valueToFraction(effectiveMin)
                val deltaFraction = deltaPx / availableWidthPx
                val newValue = fractionToValue(currentFraction + deltaFraction)
                onDragUpdate(true, newValue)
            },
            onDragEnd = onDragEnd
        )

        // Layer 4: Upper drag handle
        DragHandle(
            offsetPx = upperOffsetPx,
            handleWidthPx = handleWidthPx,
            availableWidthPx = availableWidthPx,
            onDragStart = { onDragStart(false) },
            onDrag = { deltaPx ->
                val currentFraction = valueToFraction(effectiveMax)
                val deltaFraction = deltaPx / availableWidthPx
                val newValue = fractionToValue(currentFraction + deltaFraction)
                onDragUpdate(false, newValue)
            },
            onDragEnd = onDragEnd
        )
    }
}

// ── Drag Handle ──────────────────────────────────────────────────────────────

/**
 * Drag handle with 44dp touch target (visual 18dp centered inside).
 * Sheet dismiss is disabled via confirmValueChange, so standard
 * detectHorizontalDragGestures works without gesture conflicts.
 */
@Composable
private fun DragHandle(
    offsetPx: Float,
    handleWidthPx: Float,
    availableWidthPx: Float,
    onDragStart: () -> Unit,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val touchTargetWidth = 44.dp
    val touchTargetOffset = with(LocalDensity.current) {
        ((touchTargetWidth - HANDLE_WIDTH) / 2).toPx()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((offsetPx - touchTargetOffset).roundToInt(), 0) }
            .size(touchTargetWidth, HANDLE_HEIGHT)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(HANDLE_WIDTH, HANDLE_HEIGHT)
                .shadow(2.dp, RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                GripLine()
                GripLine()
            }
        }
    }
}

@Composable
private fun GripLine() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = HANDLE_HEIGHT * 0.3f)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    )
}

// ── Filter Value Text Field ──────────────────────────────────────────────────

@Composable
private fun FilterValueField(
    value: Double,
    apiUnit: DatasetUnit,
    temperatureUnits: TemperatureUnits,
    decimalPlaces: Int,
    textAlign: TextAlign,
    onCommit: (Double) -> Unit
) {
    val displayValue = apiUnit.convertForDisplay(value, temperatureUnits)
    val formatted = remember(displayValue, decimalPlaces) {
        GradientScaleUtil.formatValue(displayValue, decimalPlaces)
    }

    var editingText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    val displayText = if (isFocused) editingText else formatted
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BasicTextField(
            value = displayText,
            onValueChange = { editingText = it },
            textStyle = SaltyType.mono(16).copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    editingText.toDoubleOrNull()?.let { typedValue ->
                        onCommit(apiUnit.convertFromDisplay(typedValue, temperatureUnits))
                    }
                }
            ),
            modifier = Modifier
                .width(80.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused && !isFocused) {
                        editingText = GradientScaleUtil.formatValue(
                            apiUnit.convertForDisplay(value, temperatureUnits),
                            decimalPlaces
                        )
                    }
                    if (!state.isFocused && isFocused) {
                        editingText.toDoubleOrNull()?.let { typedValue ->
                            onCommit(apiUnit.convertFromDisplay(typedValue, temperatureUnits))
                        }
                    }
                    isFocused = state.isFocused
                },
            decorationBox = { innerTextField ->
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        innerTextField()
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFocused) 2.dp else 1.dp)
                            .background(
                                if (isFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        )

        Text(
            text = apiUnit.displayUnitSuffix(temperatureUnits),
            style = SaltyType.mono(16).copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}
