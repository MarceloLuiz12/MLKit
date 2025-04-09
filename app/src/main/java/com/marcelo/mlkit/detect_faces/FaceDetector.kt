package com.marcelo.mlkit.detect_faces

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

/**
 * Classe responsável por analisar quadros de imagem em tempo real e detectar rostos dentro de uma área oval definida.
 *
 * Utiliza a API de detecção de faces do Google ML Kit em conjunto com a análise de imagem da CameraX
 * para identificar se um rosto está presente dentro de uma determinada região oval da tela, com base
 * em posição e tamanho esperados.
 *
 * ### Principais funcionalidades:
 * - Detecta rostos com base em posição e tamanho.
 * - Verifica se o centro do rosto detectado está dentro de um oval definido.
 * - Limita a frequência de detecção para reduzir uso de recursos (throttle).
 * - Executa análise em background com uso de corrotinas (CoroutineScope).
 *
 * @param ovalCenter Centro do oval (em coordenadas da imagem) dentro do qual um rosto válido deve estar.
 * @param ovalRadiusX Raio horizontal (em pixels) do oval.
 * @param ovalRadiusY Raio vertical (em pixels) do oval.
 * @param onFaceDetected Callback que retorna `true` se um rosto for detectado dentro do oval, `false` caso contrário.
 *
 * ### Como funciona:
 * A cada frame da câmera, a classe:
 * 1. Converte a imagem para `InputImage` do ML Kit.
 * 2. Processa a imagem com o detector de faces do ML Kit.
 * 3. Verifica se algum dos rostos detectados atende aos critérios:
 *    - O centro do rosto está dentro da área oval.
 *    - O tamanho do rosto está dentro de limites mínimos e máximos.
 * 4. Retorna o resultado via `onFaceDetected`.
 *
 * ### Requisitos:
 * - Dependência ML Kit Face Detection.
 * - CameraX configurada com ImageAnalysis.
 *
 * ### Exemplos de uso:
 *
 * ```
 * val analyzer = FaceDetector(
 *     ovalCenter = Offset(500f, 800f),
 *     ovalRadiusX = 300f,
 *     ovalRadiusY = 400f
 * ) { isDetected ->
 *     if (isDetected) {
 *         Log.d("Face", "Rosto dentro do oval!")
 *     }
 * }
 * ```
 *
 * ### Observações:
 * - Essa classe aplica um pequeno delay (`THROTTLE_TIMEOUT_MS`) entre análises para reduzir consumo de CPU.
 * - É importante chamar `imageProxy.close()` após o processamento para liberar o frame da câmera.
 *
 * @author Marcelo
 */
class FaceDetector(
    private val ovalCenter: Offset,
    private val ovalRadiusX: Float,
    private val ovalRadiusY: Float,
    private val onFaceDetected: (Boolean) -> Unit,
) : ImageAnalysis.Analyzer {

    companion object {
        private const val THROTTLE_TIMEOUT_MS = 4_00L
        private const val MIN_FACE_SIZE = 50f
        private const val FACE_SIZE_MULTIPLIER = 3.5f
        private const val FACE_POSITION_ACCURACY = 0.3f
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val faceDetector: FaceDetector = FaceDetection.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return@launch
            }

            val inputImage = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees,
            )

            suspendCoroutine<Unit> { continuation ->
                faceDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        val isFaceDetected = faces.any {
                            isFaceInsideOval(
                                Offset(it.boundingBox.centerX().toFloat(), it.boundingBox.centerY().toFloat()),
                                it.boundingBox.width().toFloat(),
                                it.boundingBox.height().toFloat()
                            )
                        }
                        onFaceDetected(isFaceDetected)
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
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

private fun isFaceInsideOval(
    faceCenter: Offset,
    faceWidth: Float,
    faceHeight: Float
): Boolean {

    val verticalFaceCenter = Offset(
        faceCenter.x,
        faceCenter.y - ovalRadiusY
    )

    val xInsideOval = abs(verticalFaceCenter.x - ovalCenter.x) <= (ovalRadiusX * FACE_POSITION_ACCURACY)
    val yInsideOval = abs(verticalFaceCenter.y - ovalCenter.y) <= (ovalRadiusY * FACE_POSITION_ACCURACY)

    val isCenterInsideOval = xInsideOval && yInsideOval

    val faceFitsInOval = faceWidth in (MIN_FACE_SIZE)..(ovalRadiusX * FACE_SIZE_MULTIPLIER) &&
            faceHeight in MIN_FACE_SIZE..(ovalRadiusY * FACE_SIZE_MULTIPLIER)

    return isCenterInsideOval && faceFitsInOval
}

}