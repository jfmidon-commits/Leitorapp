import { useEffect, useRef } from 'react';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

/**
 * Payload cru vindo do Kotlin (ver RNCaptureModule.onRawOffer).
 * Unidades: centavos / metros / segundos — conversão para as unidades
 * de domínio (BRL, km, min) acontece no normalizer, não aqui.
 */
export type RawOfferPayload = {
  valueCents?: number;
  distanceMeters?: number;
  durationSeconds?: number;
  rawText: string;
  capturedAtMillis: number;
};

export type CaptureSource = 'accessibility' | 'ocr' | 'manual';

/**
 * useRideOfferCapture
 *
 * Hook fino: só assina o evento nativo e repassa pro callback.
 * Não faz normalização nem decisão de negócio aqui — isso é
 * responsabilidade do RideOfferNormalizer / DecisionEngine já
 * planejados na arquitetura do MotoristaPro. Este hook é só o
 * ponto de entrada do CaptureAdapter para a origem 'accessibility'.
 */
export function useRideOfferCapture(
  onRawOffer: (payload: RawOfferPayload, source: CaptureSource) => void
) {
  const callbackRef = useRef(onRawOffer);
  callbackRef.current = onRawOffer;

  useEffect(() => {
    if (Platform.OS !== 'android') {
      // iOS não tem AccessibilityService equivalente com este nível de
      // acesso — captura automática nesta plataforma provavelmente
      // não é viável do mesmo jeito. Fica como não-implementado aqui,
      // não como assunção silenciosa.
      return;
    }

    const { RNCaptureModule } = NativeModules;
    if (!RNCaptureModule) {
      console.warn(
        '[useRideOfferCapture] RNCaptureModule não encontrado — ' +
        'RNCapturePackage foi registrado em MainApplication?'
      );
      return;
    }

    const emitter = new NativeEventEmitter(RNCaptureModule);
    const subscription = emitter.addListener(
      'rawOfferCaptured',
      (payload: RawOfferPayload) => {
        callbackRef.current(payload, 'accessibility');
      }
    );

    return () => subscription.remove();
  }, []);
}

/**
 * Helper para checar (assíncrono) se o usuário já ativou o serviço de
 * acessibilidade do app nas configurações do sistema. Use isso para
 * mostrar um CTA "Ativar captura automática" em vez de assumir que
 * está ligado.
 */
export async function isCaptureServiceEnabled(): Promise<boolean> {
  if (Platform.OS !== 'android') return false;
  const { RNCaptureModule } = NativeModules;
  if (!RNCaptureModule) return false;
  return RNCaptureModule.isAccessibilityServiceEnabled();
}
