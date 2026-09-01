package com.motoristapro.capture

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OfferAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OfferCapture"
        private const val MIN_INTERVAL_MS = 500L
        private const val MAX_NODES = 400
    }

    private var lastProcessedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val now = System.currentTimeMillis()
                if (now - lastProcessedAt < MIN_INTERVAL_MS) return
                lastProcessedAt = now
                scanAllWindows()
            }
        }
    }

    /**
     * Varre TODAS as janelas visíveis no momento (não só a "ativa"/focada).
     * Isso é essencial porque o card de oferta do Uber aparece como um
     * overlay flutuante por cima de outro app (ex: a tela inicial) e
     * NÃO tem foco de input — rootInActiveWindow() nunca o encontraria.
     */
    private fun scanAllWindows() {
        val windowList = windows ?: return
        for (window in windowList) {
            val root = window.root ?: continue
            val pkg = root.packageName?.toString() ?: "desconhecido"

            val texts = mutableListOf<String>()
            collectText(root, texts)
            root.recycle()

            if (texts.isEmpty()) continue

            val joined = texts.joinToString(" | ")
            Log.d(TAG, "[$pkg] $joined")

            val parsed = RideOfferParser.parse(texts)
            if (parsed != null) {
                CaptureEventBridge.emit(parsed)
            } else {
                CaptureEventBridge.emitDebug("[$pkg] $joined")
            }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        if (out.size >= MAX_NODES) return // proteção contra árvores muito profundas (ex: mapas)

        val text = node.text?.toString()
        if (!text.isNullOrBlank()) out.add(text)
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) out.add(desc)

        for (i in 0 until node.childCount) {
            if (out.size >= MAX_NODES) break
            val child = node.getChild(i) ?: continue
            collectText(child, out)
            child.recycle()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço interrompido")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "OfferAccessibilityService conectado")
        CaptureEventBridge.emitDebug("(serviço conectado — aguardando telas)")
    }
}
