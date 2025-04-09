package com.marcelo.mlkit.text_recognition

import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Classe responsável por realizar o reconhecimento de texto em tempo real utilizando a câmera e o ML Kit.
 *
 * Essa implementação usa a API de `TextRecognition` do Google ML Kit em conjunto com `ImageAnalysis` da CameraX
 * para detectar texto de imagens capturadas pela câmera e retornar o conteúdo em formato de `String`.
 *
 * ### Funcionalidades principais:
 * - Captura frames da câmera e realiza OCR (Optical Character Recognition).
 * - Utiliza reconhecimento de texto baseado no modelo padrão para textos latinos (inglês, português, etc).
 * - Throttling embutido para reduzir a frequência de chamadas e evitar sobrecarga de processamento.
 *
 * @param onDetectedTextUpdated Callback chamado com o texto detectado. Será chamado somente se o texto não estiver em branco.
 *
 * ### Como funciona:
 * A cada frame recebido:
 * 1. A imagem é convertida em um `InputImage` do ML Kit.
 * 2. O texto é processado pelo `TextRecognizer`.
 * 3. Se o texto detectado não estiver em branco, é retornado via o callback `onDetectedTextUpdated`.
 * 4. Um delay de 10 segundos (`THROTTLE_TIMEOUT_MS`) é aplicado entre os frames analisados.
 *
 * ### Requisitos:
 * - CameraX configurado com um `ImageAnalysis.Analyzer`.
 * - Dependência do ML Kit Text Recognition com `TextRecognizerOptions`.
 *
 * ### Exemplo de uso:
 *
 * ```
 * val analyzer = TextRecognitionAnalyzer { text ->
 *     Log.d("TextRecognition", "Texto detectado: $text")
 * }
 *
 * val imageAnalysis = ImageAnalysis.Builder()
 *     .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
 *     .build()
 *     .also {
 *         it.setAnalyzer(cameraExecutor, analyzer)
 *     }
 * ```
 *
 * ### Observações:
 * - `imageProxy.close()` é chamado após cada processamento, garantindo o consumo correto do frame.
 * - A estratégia de throttle (`delay`) serve para evitar chamadas excessivas ao ML Kit.
 *
 * ### Possíveis extensões:
 * - Adicionar filtragem de palavras específicas.
 * - Retornar as posições dos blocos de texto detectados.
 * - Suporte a múltiplos idiomas.
 *
 *
 * @author Marcelo
 */
class TextRecognitionAnalyzer(
    private val onDetectedTextUpdated: (String) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        const val THROTTLE_TIMEOUT_MS = 10_000L
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage: Image = imageProxy.image ?: run { imageProxy.close(); return@launch }
            val inputImage: InputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            suspendCoroutine { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText: Text ->
                        val detectedText: String = visionText.text
                        if (detectedText.isNotBlank()) {
                            onDetectedTextUpdated(detectedText)
                        }
                    }
                    .addOnCompleteListener {
                        continuation.resume(Unit)
                    }
            }

            delay(THROTTLE_TIMEOUT_MS)
        }.invokeOnCompletion { exception ->
            exception?.printStackTrace()
            imageProxy.close()
        }
    }
}
