/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androiddevchallenge.ui.theme.MyTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

// Start building your app here!
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyApp() {
    var isSelectingTime by remember { mutableStateOf(true) }
    var isRunning by remember { mutableStateOf(false) }
    var initialTime by remember { mutableStateOf(0L) }
    var totalTime by remember { mutableStateOf(0L) }
    var timeLeft by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            startTime = SystemClock.uptimeMillis()
            while (timeLeft > 0 && isActive && isRunning) {
                timeLeft = totalTime - (SystemClock.uptimeMillis() - startTime)
                withFrameMillis {  }

                if (timeLeft <= 0) {
                    isRunning = false
                    isSelectingTime = true
                }
            }
        }
    }

    Surface(color = MaterialTheme.colors.background) {
        Crossfade(targetState = isSelectingTime) { value ->
            if (value) {
                TimeSelector(
                    onSelectTime = {
                        initialTime = it * 1000L
                        totalTime = initialTime
                        timeLeft = totalTime
                        isSelectingTime = false
                        isRunning = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CountDown(
                    initialTime = initialTime,
                    timeLeft = timeLeft,
                    isRunning = isRunning,
                    onPause = {
                        isRunning = false
                        totalTime = timeLeft
                    },
                    onResume = { isRunning = true },
                    onReset = {
                        isRunning = false
                        isSelectingTime = true
                    }
                )
            }
        }
    }
}

private val presetTimes = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60)

@Composable
fun TimeSelector(onSelectTime: (Int) -> Unit, modifier: Modifier = Modifier) {
    val pagerState by remember { mutableStateOf(PagerState(currentPage = presetTimes.indexOf(30), minPage = 0, maxPage = presetTimes.size - 1)) }
    val scope = rememberCoroutineScope()

    Box(modifier) {
        Pager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            offscreenLimit = 4
        ) {
            val color by animateColorAsState(targetValue = MaterialTheme.colors.onBackground.copy(alpha = if (page == currentPage) 1f else 0.5f))
            Text(
                text = "${presetTimes[page]}",
                color = color,
                style = MaterialTheme.typography.h1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .defaultMinSize(minWidth = 120.dp)
                    .padding(16.dp)
            )
        }

        val background = MaterialTheme.colors.background
        val transparentBackground = background.copy(alpha = 0f)
        val gradientBrush = Brush.horizontalGradient(
            0.0f to background,
            0.2f to transparentBackground,
            0.8f to transparentBackground,
            1.0f to background,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.Center)
                .background(gradientBrush)
        )

        FloatingActionButton(
            onClick = {
                scope.launch { onSelectTime(presetTimes[pagerState.currentPage]) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun CountDown(
    initialTime: Long,
    timeLeft: Long,
    isRunning: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    var showAnimatedProgress by remember { mutableStateOf(true) }
    val animatedProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialTime) {
        showAnimatedProgress = true
        animatedProgress.animateTo((timeLeft - 300f).coerceAtLeast(0f) / initialTime, animationSpec = tween(300))
        showAnimatedProgress = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Wave(
            isRunning = isRunning,
            progress = if (showAnimatedProgress) animatedProgress.value else timeLeft.toFloat() / initialTime.toFloat(),
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "${(timeLeft / 1000f).toInt()}",
            style = MaterialTheme.typography.h1,
            modifier = Modifier.align(Alignment.Center)
        )

        FloatingActionButton(
            onClick = { if (isRunning) onPause() else onResume() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp)
        ) {
            if (isRunning) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
            }
        }

        IconButton(
            onClick = {
                scope.launch {
                    animatedProgress.snapTo(timeLeft.toFloat() / initialTime.toFloat())
                    showAnimatedProgress = true
                    animatedProgress.animateTo(0f, animationSpec = tween(300))
                    onReset()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = (-16).dp)
        ) {
            Icon(Icons.Default.Replay, contentDescription = "Reset")
        }
    }
}

@Composable
fun Wave(
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    progress: Float = 0.8f,
) {
    val transition = rememberInfiniteTransition()
    val animDuration by animateIntAsState(if (isRunning) 1500 else 5000)
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(animDuration, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val scaledProgress = animProgress * Math.PI * 2

    val waveHeight by animateFloatAsState(if (isRunning) 16f else 8f, animationSpec = tween(if (isRunning) 300 else 2000))
    val topPath = remember { Path() }.apply { reset() }
    val bottomPath = remember { Path() }.apply { reset() }

    Canvas(modifier = modifier) {
        val topOffset = (size.height + 32f) * (1f - progress)

        topPath.reset()
        bottomPath.reset()
        topPath.moveTo(0f, topOffset)
        bottomPath.moveTo(0f, topOffset)
        for (x in IntProgression.fromClosedRange(0, size.width.toInt(), 10)) {
            val scaledX = x * Math.PI * 2 / size.width
            val topY = (waveHeight * sin(scaledX + scaledProgress) + waveHeight).toFloat()
            topPath.lineTo(x.toFloat(), topOffset - topY)

            val bottomY = (waveHeight * sin(scaledX + scaledProgress + 8f) + waveHeight).toFloat()
            bottomPath.lineTo(x.toFloat(), topOffset - bottomY)
        }
        topPath.lineTo(size.width, topOffset)
        bottomPath.lineTo(size.width, topOffset)

        drawRect(color, topLeft = Offset(0f, topOffset), alpha = 0.5f)
        drawRect(color, topLeft = Offset(0f, topOffset), alpha = 0.3f)
        drawPath(topPath, color, alpha = 0.5f)
        drawPath(bottomPath, color, alpha = 0.3f)
    }
}

@Preview("Wave", widthDp = 360, heightDp = 640)
@Composable
fun WaveTest() {
    MyTheme {
        Wave(isRunning = true)
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}
