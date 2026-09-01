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
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = System.currentTimeMillis()
                if (now - lastProcessedAt < MIN_INTERVAL_MS) return
                lastProcessedAt = now

                val pkg = event.packageName?.toString() ?: "desconhecido"

                val root = rootInActiveWindow ?: run {
                    Log.d(TAG, "[$pkg] rootInActiveWindow nulo")
                    return
                }

                // rootInActiveWindow() pode ficar "atrasado" durante troca de
                // apps e devolver a janela anterior. Só aceitamos o texto se
                // o pacote da raiz lida bate com o pacote do evento — senão é
                // leitura obsoleta (ex: ler a tela do próprio Leitorapp
                // enquanto o evento diz que veio do Uber).
                val rootPkg = root.packageName?.toString()
                if (rootPkg != pkg) {
                    Log.d(TAG, "[$pkg] descartado: raiz lida pertence a '$rootPkg' (leitura obsoleta)")
                    root.recycle()
                    return
                }

                val texts = mutableListOf<String>()
                collectText(root, texts)
                root.recycle()

                if (texts.isEmpty()) {
                    return
                }

                val joined = texts.joinToString(" | ")
                Log.d(TAG, "[$pkg] Texto extraído: $joined")

                val parsed = RideOfferParser.parse(texts)
                if (parsed != null) {
                    CaptureEventBridge.emit(parsed)
                } else {
                    // Mesmo sem reconhecer um padrão de oferta, publicamos o
                    // pacote+texto bruto no log de debug — é o que permite
                    // descobrir o package name certo e ver o que a tela expõe,
                    // sem precisar de adb.
                    CaptureEventBridge.emitDebug("[$pkg] $joined")
                }
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
