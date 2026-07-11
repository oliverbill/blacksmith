# Mongo e Drongo — Corrida Maluca

Jogo HTML autocontido (todos os recursos embutidos em base64, sem dependências externas), hospedado no **GitHub Pages**.

## Arquivos

- `index.html` — o jogo completo
- `icon.png` — ícone 180×180 (usado no manifest e como `apple-touch-icon`)
- `manifest.webmanifest` — manifesto PWA (Android / navegadores compatíveis)

## Publicação (GitHub Pages)

O deploy é automático via GitHub Actions (`.github/workflows/deploy-game.yml`) a cada push na branch `main` que altere `game/**`.

**Ativação (uma vez só):** em **Settings → Pages**, defina **Build and deployment → Source = "GitHub Actions"**.

Após o deploy, a URL do jogo será:

```
https://oliverbill.github.io/blacksmith/
```

## Adicionar à tela inicial do iPhone

O jogo já inclui as meta tags do iOS (`apple-mobile-web-app-capable`, `apple-mobile-web-app-title` = "Corrida Maluca" e `apple-touch-icon`), então basta:

1. Abra a URL acima no **Safari** (o "Adicionar à Tela de Início" só funciona no Safari).
2. Toque no botão **Compartilhar** (quadrado com seta para cima).
3. Selecione **Adicionar à Tela de Início**.
4. Confirme — o atalho aparece com o nome **"Corrida Maluca"** e o ícone do jogo, abrindo em tela cheia (sem a barra do navegador).
