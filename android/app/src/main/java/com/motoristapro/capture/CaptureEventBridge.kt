package com.motoristapro.capture

/**
 * CaptureEventBridge
 *
 * O AccessibilityService roda no processo Android puro e não conhece
 * o React Native diretamente. Este singleton é o ponto de desacoplamento:
 * o Service publica eventos aqui, e o RNCaptureModule (quando existe e
 * está com listener ativo) escuta e repassa para o JS.
 *
 * Isso também permite testar o Service isoladamente (sem RN) e permite
 * futuras origens de captura (Notification listener, etc.) reusarem o
 * mesmo canal de saída.
 */
object CaptureEventBridge {

    fun interface Listener {
        fun onRawOffer(offer: RawOfferData)
    }

    @Volatile
    private var listener: Listener? = null

    fun setListener(l: Listener?) {
        listener = l
    }

    fun emit(offer: RawOfferData) {
        listener?.onRawOffer(offer)
        // Se não houver listener (app JS não montado ainda / backgrounded),
        // o evento é perdido silenciosamente por design nesta fase — não
        // fazemos buffer aqui. Se isso virar problema real (ofertas perdidas
        // enquanto o app está fechado), o próximo passo é persistir localmente
        // via SQLite direto do Kotlin antes do bridge, não bufferizar em memória.
    }
}
