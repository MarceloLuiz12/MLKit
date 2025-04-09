package com.marcelo.mlkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marcelo.mlkit.detect_faces.FaceDetectionScreen
import com.marcelo.mlkit.menu.MenuScreen
import com.marcelo.mlkit.scanner.ScannerScreen
import com.marcelo.mlkit.text_recognition.TextRecognitionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.MENU) }
            BackHandler {
                currentScreen = Screen.MENU
            }
            when (currentScreen) {
                Screen.MENU -> MenuScreen(
                    onNavigateToScanner = { currentScreen = Screen.SCANNER },
                    onNavigateToTextRecognition = { currentScreen = Screen.TEXT_RECOGNITION },
                    onNavigateToFaceDetection = { currentScreen = Screen.FACE_DETECTION}
                )

                Screen.SCANNER -> ScannerScreen(
                    filesDir = filesDir,
                    contentResolver = contentResolver,
                    activity = this
                )

                Screen.TEXT_RECOGNITION -> TextRecognitionScreen()

                Screen.FACE_DETECTION -> FaceDetectionScreen()
            }
        }
    }
}

enum class Screen {
    MENU, SCANNER, TEXT_RECOGNITION, FACE_DETECTION
}