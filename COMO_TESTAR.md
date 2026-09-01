# Como testar — 100% pelo celular, sem PC

## O que mudou
Como você não tem PC, esse projeto agora compila sozinho na nuvem
(GitHub Actions) toda vez que eu faço push. Você só baixa o `.apk`
pronto direto no navegador do celular.

## Passo a passo

1. **Aguardar o build terminar.** Depois que eu confirmar o push,
   acesse pelo navegador do celular:
   `https://github.com/jfmidon-commits/Leitorapp/actions`
   Vai aparecer um workflow "Build APK" rodando (ícone amarelo) —
   leva uns 2-4 minutos até ficar verde.

2. **Baixar o APK.** Depois de verde, acesse:
   `https://github.com/jfmidon-commits/Leitorapp/releases`
   Vai ter uma release "Leitorapp build #N" com o arquivo
   `app-debug.apk` anexado — baixa ele.

3. **Instalar.** O Android vai avisar que é de "fonte desconhecida" —
   isso é normal para APK fora da Play Store. Autorize a instalação
   quando pedir.

4. **Abrir o app "Leitorapp (Fase 1)".**
   - Vai mostrar "Servico INATIVO"
   - Toque no botão "Abrir Configurações de Acessibilidade"
   - Ative "Leitorapp (Fase 1)" na lista
   - Volte pro app — deve virar "Servico ATIVO"

5. **Abrir o app do Uber ou 99** (o app de motorista) e navegar pelas
   telas normalmente, esperando uma oferta de corrida aparecer.

6. **Voltar pro Leitorapp** e olhar o "Log de captura" na tela — cada
   linha mostra [nome.do.pacote] texto extraído da tela. Isso serve
   pra duas coisas:
   - Confirmar o package name real do app do Uber/99 no seu device
     (algo como com.ubercab.driver)
   - Ver se o texto da oferta aparece no log (Resultado A) ou não
     aparece nada quando a oferta está na tela (Resultado B)

## O que fazer com o resultado

- **Apareceu o texto da oferta no log** -> ótimo, a árvore de
  acessibilidade expõe os dados. Próximo passo: eu reintroduzo o
  filtro de pacote (usando o package name que você viu no log) e
  ligo o parser de valor/km/min pra virar "OFERTA RECONHECIDA".

- **Nada aparece quando a oferta está na tela** (mas outras telas do
  app aparecem no log normalmente) -> sinal de que essa tela
  específica tem FLAG_SECURE ativo. Nesse caso OCR também não
  funcionaria (screenshot vem em branco), e o caminho vira captura
  por notificação (se o app notificar a oferta) ou entrada manual.

Me manda print da tela do Leitorapp com o log depois de testar contra
o Uber/99 de verdade.
