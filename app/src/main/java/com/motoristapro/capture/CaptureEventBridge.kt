package com.motoristapro.capture

import androidx.lifecycle.MutableLiveData

/**
 * CaptureEventBridge
 *
 * Nesta fase (app nativo, sem React Native), o bridge é só um canal
 * observável simples: o OfferAccessibilityService publica aqui, e a
 * MainActivity observa para mostrar o log na tela — já que sem PC
 * não dá para usar `adb logcat`.
 *
 * `debugLog` recebe TODA leitura (mesmo sem parse reconhecido), para
 * diagnosticar o que a árvore de acessibilidade está expondo.
 * `lastOffer` recebe só os casos em que o parser reconheceu algum
 * campo (valor/distância/duração).
 */
object CaptureEventBridge {
    val debugLog = MutableLiveData<String>()
    val lastOffer = MutableLiveData<RawOfferData>()

    fun emitDebug(line: String) {
        debugLog.postValue(line)
    }

    fun emit(offer: RawOfferData) {
        lastOffer.postValue(offer)
        val valor = offer.valueCents?.let { "R$ ${"%.2f".format(it / 100.0)}" } ?: "?"
        val dist = offer.distanceMeters?.let { "${"%.1f".format(it / 1000.0)} km" } ?: "?"
        val dur = offer.durationSeconds?.let { "${it / 60} min" } ?: "?"
        emitDebug("✅ OFERTA RECONHECIDA: $valor | $dist | $dur")
    }
}
