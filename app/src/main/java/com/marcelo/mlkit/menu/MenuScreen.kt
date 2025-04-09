package com.marcelo.mlkit.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MenuScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToTextRecognition: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onNavigateToScanner) {
            Text("Abrir Scanner de Documento")
        }

        Button(onClick = onNavigateToTextRecognition) {
            Text("Abrir Reconhecimento de Texto")
        }
    }
}
