package com.motoristapro.capture

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * OfferAccessibilityService
 *
 * Escuta mudanças de tela nos apps de motorista configurados em
 * offer_accessibility_config.xml e tenta extrair o texto de uma
 * possível oferta de corrida (valor, distância, tempo) percorrendo
 * a árvore de acessibilidade.
 *
 * FASE 1 (este arquivo): só detectar e logar o texto bruto que
 * conseguimos ler. Isso serve para VALIDAR se o app alvo expõe
 * dados suficientes via accessibility tree antes de investir em
 * parsing/normalização/OCR fallback.
 *
 * Não clica, não interage, não modifica nada — apenas leitura.
 */
class OfferAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OfferCapture"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: run {
                    Log.d(TAG, "rootInActiveWindow nulo — app pode estar bloqueando acessibilidade nesta tela")
                    return
                }
                val texts = mutableListOf<String>()
                collectText(root, texts)
                root.recycle()

                if (texts.isEmpty()) {
                    Log.d(TAG, "Nenhum texto extraído da árvore (possível FLAG_SECURE ou view customizada/Canvas)")
                    return
                }

                Log.d(TAG, "Texto extraído (${texts.size} nodes): ${texts.joinToString(" | ")}")

                val parsed = RideOfferParser.parse(texts)
                if (parsed != null) {
                    CaptureEventBridge.emit(parsed)
                }
                // Se parsed == null, o texto não bateu com nenhum padrão conhecido
                // (ex: tela de navegação, não de oferta) — ignorado por design,
                // não é erro.
            }
        }
    }

    /** Percorre a árvore de nodes recursivamente coletando texto visível. */
    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) out.add(text)
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) out.add(desc)

        for (i in 0 until node.childCount) {
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
    }
}
