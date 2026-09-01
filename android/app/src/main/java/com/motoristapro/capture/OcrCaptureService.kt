package com.motoristapro.capture

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * OcrCaptureService
 *
 * FALLBACK apenas — só deve ser usado quando OfferAccessibilityService
 * repetidamente não encontra texto (Resultado B do COMO_TESTAR.md).
 *
 * IMPORTANTE: se a tela de oferta do app-alvo estiver marcada com
 * FLAG_SECURE, o ImageReader vai receber frames pretos/em branco.
 * Isso não é um bug para contornar — é a proteção do Android
 * funcionando como projetada. Se isso acontecer, este fallback
 * simplesmente não vai funcionar e a única saída legítima é o
 * fallback manual (widget de entrada rápida) ou eventual notificação,
 * se o app-alvo notificar a oferta.
 *
 * Requer permissão de captura de tela concedida pelo usuário via
 * MediaProjectionManager (prompt do sistema, precisa ser solicitado
 * a cada sessão — não pode ser assumido como concedido).
 */
class OcrCaptureService : Service() {

    companion object {
        private const val TAG = "OfferCapture-OCR"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode == -1 || resultData == null) {
            Log.e(TAG, "Sem permissão de captura de tela concedida — encerrando")
            stopSelf()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, resultData)
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        val display = getSystemService(DisplayManager::class.java).displays.firstOrNull()
        display?.getRealMetrics(metrics)
        val width = metrics.widthPixels.takeIf { it > 0 } ?: 1080
        val height = metrics.heightPixels.takeIf { it > 0 } ?: 1920
        val density = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MotoristaProCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            processFrame(image)
        }, null)
    }

    private fun processFrame(image: Image) {
        try {
            val bitmap = imageToBitmap(image)
            if (bitmap == null) {
                Log.d(TAG, "Frame veio nulo/preto — possível FLAG_SECURE ativo na tela alvo")
                return
            }
            val input = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(input)
                .addOnSuccessListener { result ->
                    val texts = result.textBlocks.map { it.text }
                    if (texts.isNotEmpty()) {
                        Log.d(TAG, "OCR extraiu ${texts.size} blocos")
                        val parsed = RideOfferParser.parse(texts)
                        if (parsed != null) CaptureEventBridge.emit(parsed)
                    }
                }
                .addOnFailureListener { e -> Log.e(TAG, "Falha no ML Kit OCR", e) }
        } finally {
            image.close()
        }
    }

    private fun imageToBitmap(image: Image): android.graphics.Bitmap? {
        // Implementação padrão de conversão ImageReader(RGBA_8888) -> Bitmap
        // via planes/rowStride. Omitido aqui por brevidade — é boilerplate
        // conhecido do Android (buscar "ImageReader to Bitmap rowPadding"
        // se quiser a implementação linha a linha); posso escrever completo
        // quando chegarmos nesta fase, se o teste da Fase 1 confirmar que
        // o OCR fallback é realmente necessário.
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
    }
}
