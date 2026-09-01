# Como validar a captura (Fase 1)

Objetivo: descobrir, no seu device real, se o app do Uber/99 expõe o
texto da oferta pela árvore de acessibilidade — antes de gastar tempo
com OCR.

## 1. Descobrir o package name real do app instalado
```
adb shell pm list packages | grep -i uber
adb shell pm list packages | grep -i 99
```
Ajuste `android:packageNames` em `offer_accessibility_config.xml` com
o valor exato retornado.

## 2. Integrar os 3 arquivos no projeto MotoristaPro
- Copiar `OfferAccessibilityService.kt` para
  `android/app/src/main/java/com/motoristapro/capture/`
  (ajustar o pacote se o seu `applicationId` for diferente)
- Copiar `offer_accessibility_config.xml` para
  `android/app/src/main/res/xml/`
- Mesclar `AndroidManifest_snippet.xml` dentro da tag `<application>`
  do seu `AndroidManifest.xml` (não sobrescrever)

## 3. Build e instalar
```
cd android && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 4. Ativar o serviço manualmente
Configurações do Android > Acessibilidade > Apps instalados >
MotoristaPro > ativar. (Isso é manual só nesta fase de teste; depois
dá pra abrir a tela de configuração direto do app via Intent.)

## 5. Monitorar o log em tempo real
```
adb logcat -s OfferCapture
```

## 6. Abrir o app do Uber/99 e simular (ou esperar) uma oferta aparecer

### Resultado A — aparece texto no log
Ótimo sinal: significa que o app NÃO está usando `FLAG_SECURE` nessa
tela e a árvore de acessibilidade expõe os dados. Nesse caso o
próximo passo é parsing (regex para `R$`, `km`, `min`) e integração
com o `RideOfferNormalizer`. Não precisamos de OCR.

### Resultado B — "rootInActiveWindow nulo" ou nenhum texto
Dois cenários possíveis:
- **FLAG_SECURE ativo**: nem accessibility tree nem screenshot vão
  funcionar nessa tela — precisaria de outra estratégia (ex.:
  detectar a notificação da oferta, se o app usa notificação).
- **View customizada (Canvas/Compose sem semantics)**: a árvore
  existe mas os textos não são expostos como `AccessibilityNodeInfo`
  legível. Nesse caso o fallback de screenshot + OCR (ML Kit)
  passa a ser necessário — e aí sim vale investir nisso.

Me manda o resultado do `adb logcat` que a gente decide o próximo
passo com base em dado real, não suposição.

---

# Arquivos entregues (visão geral)

| Arquivo | Papel |
|---|---|
| `OfferAccessibilityService.kt` | Escuta a tela, extrai texto bruto |
| `RideOfferParser.kt` | Regex: texto bruto -> valor/km/min |
| `CaptureEventBridge.kt` | Desacopla Service do módulo RN |
| `RNCaptureModule.kt` + `RNCapturePackage.kt` | Expõe evento `rawOfferCaptured` pro JS |
| `OcrCaptureService.kt` | Fallback OCR (MediaProjection + ML Kit) — só ativar se a Fase 1 mostrar Resultado B |
| `src/capture/useRideOfferCapture.ts` | Hook RN que assina o evento nativo |
| `src/capture/example-integration.ts` | Exemplo de encaixe no seu CaptureAdapter/Normalizer |

## Dependências a adicionar (se for usar o fallback OCR)

`android/app/build.gradle`:
```gradle
implementation 'com.google.mlkit:text-recognition:16.0.1'
```

## Pendências deixadas como stub explícito (não implementadas por decisão, não por esquecimento)

1. **`isAccessibilityServiceEnabled()`** em `RNCaptureModule.kt` — retorna
   `false` fixo. Implementação real precisa ler
   `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` e comparar com o
   nome do seu serviço.
2. **`imageToBitmap()`** em `OcrCaptureService.kt` — retorna `null` fixo.
   É boilerplate padrão de conversão `ImageReader` -> `Bitmap`, mas só
   vale implementar se o teste da Fase 1 confirmar Resultado B
   (accessibility tree não expõe texto). Implementar isso sem essa
   confirmação seria trabalho especulativo.
3. **Fluxo de permissão do `MediaProjectionManager`** (prompt do
   sistema pedindo autorização de captura de tela) — não incluído,
   também condicional ao Resultado B.
4. **`AndroidManifest_snippet.xml`** ainda precisa ser mesclado
   manualmente no seu manifest real — não tenho acesso ao arquivo
   atual do repo para mesclar automaticamente.
5. **`MainApplication.kt`** — registrar `RNCapturePackage()` em
   `getPackages()` manualmente, mesmo motivo do item 4.

## Próximo passo natural
Depois que você confirmar o resultado do teste da Fase 1 (log com
texto ou vazio), a peça que falta do lado do domínio é o
`RideOfferNormalizer` consumindo o `RawOfferPayload` — mas como você
já tem esse componente planejado/talvez iniciado no repo principal,
melhor eu ver o código existente antes de escrever algo que pode
duplicar ou conflitar com o que já existe lá.
