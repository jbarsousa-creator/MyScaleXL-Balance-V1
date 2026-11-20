# MyScale XL

Aplicação Android para conectar a balança **Bewell MyScale XL (BW-SC4W)** via Bluetooth Low Energy.

## Funcionalidades
- Ligação BLE à balança BW-SC4W
- Leitura de peso em tempo real
- Deteção de peso estável
- Múltiplos perfis de utilizador
- Histórico de pesagens
- Cálculo de IMC, BMR e estimativa de gordura corporal
- Gráfico de evolução de peso
- Integração com Health Connect (Google Fit)

## Protocolo da balança
- Serviço: `0xFFE0`
- Característica: `0xFFE1` (Notify + Write)
- Formato do pacote: `08 07 07 B0 00 [high] [low]`
- Peso (kg) = `((kg) = `((high << 8) | low) / 200.0`

## Como abrir no Android Studio
1. File → Open → seleciona a pasta `MyScaleXL`
2. Aguarda a sincronização do Gradle
3. Liga o telemóvel com Depuração USB ativada
4. Clica em Run ▶

## Requisitos
- Android 8.0 (API 26) ou superior
- Bluetooth Low Energy
- Health Connect (opcional, para sincronizar com Google Fit)

## Gerar o APK no GitHub
1. Cria um repositório no GitHub e faz push do conteúdo desta pasta (branch `main`).
2. Vai ao separador **Actions** → workflow **Build APK** (corre automaticamente a cada push, ou clica em *Run workflow*).
3. Quando terminar, abre a execução e descarrega o artefacto **MyScaleXL-apk** (contém `MyScaleXL.apk`).
4. Opcional: cria uma tag `v1.0` (`git tag v1.0 && git push --tags`) para publicar o APK numa *Release*.

O APK gerado é de *debug* (assinado automaticamente) e pode ser instalado diretamente no telemóvel.

## Análise de calorias por foto (gratuita)

Na página de **Refeições** podes fotografar ou escolher uma imagem da comida.

A análise usa a **API gratuita do Google Gemini** (modelo `gemini-2.0-flash`):

1. Vai a [Google AI Studio](https://aistudio.google.com/app/apikey) e cria uma chave API gratuita (não precisa de cartão de crédito).
2. Na app: **Definições** → cola a chave → Guardar.
3. Na página de Refeições, tira uma foto ou escolhe da galeria.

As fotos só são enviadas para a Google quando pedes a análise. A chave fica guardada apenas no teu telemóvel.
