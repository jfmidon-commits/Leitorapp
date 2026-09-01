import { useRideOfferCapture, RawOfferPayload, CaptureSource } from './useRideOfferCapture';

/**
 * Exemplo de uso — adaptar para o componente/screen real onde o
 * MotoristaPro já monta o CaptureAdapter na arquitetura planejada:
 *
 *   CaptureAdapter -> RideOfferNormalizer -> RideOffer -> ProfitEngine
 *   -> DecisionEngine -> DecisionResult
 *
 * Este arquivo só ilustra o encaixe; o normalizer/profitEngine reais
 * ficam no seu domínio já existente — não estou reimplementando eles.
 */
export function useAccessibilityCaptureAdapter(
  normalizeAndDecide: (raw: RawOfferPayload, source: CaptureSource) => void
) {
  useRideOfferCapture((payload, source) => {
    // Sanidade mínima antes de repassar: se não veio nenhum dos 3
    // campos, não vale a pena acionar o pipeline de decisão.
    const hasAnyField =
      payload.valueCents != null ||
      payload.distanceMeters != null ||
      payload.durationSeconds != null;

    if (!hasAnyField) return;

    normalizeAndDecide(payload, source);
  });
}
