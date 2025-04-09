package com.marcelo.mlkit.scanner

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

@Composable
fun ScannerScreen(
    filesDir: File,
    contentResolver: ContentResolver,
    activity: ComponentActivity,
) {

    //Instance of GMS Document Scanner
    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(2)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()
    val scanner = GmsDocumentScanning.getClient(options)

    var imageUris by remember {
        mutableStateOf<List<Uri>>(listOf())
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) {
                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                imageUris = result?.pages?.map { page -> page.imageUri }.orEmpty()

                result?.pdf?.let { pdf ->
                    val fos = FileOutputStream(File(filesDir, "scan.pdf"))
                    contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
                        inputStream.copyTo(fos)
                    }
                }
            }
        }
    )


    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        imageUris.forEach { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = {
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intent ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intent).build()
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            activity,
                            "Error ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        ) {
            Text(text = "Scan Document")
        }
    }
}
