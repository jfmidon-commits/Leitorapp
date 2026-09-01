package com.motoristapro.capture

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * RNCaptureModule
 *
 * Ponte JS <-> Android. Do lado JS:
 *   import { NativeEventEmitter, NativeModules } from 'react-native';
 *   const emitter = new NativeEventEmitter(NativeModules.RNCaptureModule);
 *   emitter.addListener('rawOfferCaptured', (payload) => { ... });
 *
 * O payload chega em centavos/metros/segundos (mesma unidade do Kotlin)
 * para o RideOfferNormalizer decidir como converter, sem perda de
 * precisão por float na travessia da bridge.
 */
class RNCaptureModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), CaptureEventBridge.Listener {

    override fun getName() = "RNCaptureModule"

    private var listenerCount = 0

    @ReactMethod
    fun addListener(eventName: String) {
        listenerCount += 1
        if (listenerCount == 1) {
            CaptureEventBridge.setListener(this)
        }
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        listenerCount -= count
        if (listenerCount <= 0) {
            listenerCount = 0
            CaptureEventBridge.setListener(null)
        }
    }

    @ReactMethod
    fun isAccessibilityServiceEnabled(promise: com.facebook.react.bridge.Promise) {
        // TODO: checar via Settings.Secure.ACCESSIBILITY_ENABLED +
        // ENABLED_ACCESSIBILITY_SERVICES se o pacote do serviço está na lista.
        // Deixado como stub explícito — não adivinhar o comportamento aqui.
        promise.resolve(false)
    }

    override fun onRawOffer(offer: RawOfferData) {
        val map = Arguments.createMap().apply {
            offer.valueCents?.let { putDouble("valueCents", it.toDouble()) }
            offer.distanceMeters?.let { putDouble("distanceMeters", it.toDouble()) }
            offer.durationSeconds?.let { putDouble("durationSeconds", it.toDouble()) }
            putString("rawText", offer.rawText)
            putDouble("capturedAtMillis", System.currentTimeMillis().toDouble())
        }
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("rawOfferCaptured", map)
    }
}
