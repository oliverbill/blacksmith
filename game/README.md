# 🦕 Dino Bros — Aventura no Bosque

Um jogo de plataforma em HTML5 no estilo **Super Mario Bros**, protagonizado por
dois dinossauros **recortados diretamente da imagem enviada**: **Rex** (verde,
jaqueta vermelha, laço amarelo) e **Lima** (verde-claro, camisa amarela, laço
amarelo). Os sprites em `assets/rex.png` e `assets/lima.png` foram extraídos da
foto com remoção de fundo (segmentação GrabCut), preservando a arte original.

## Como jogar

Abra `index.html` em qualquer navegador moderno — não há dependências, build ou
servidor. Também funciona por HTTP:

```bash
cd game
python3 -m http.server 8000
# acesse http://localhost:8000
```

## Controles

| Ação    | Teclado              | Toque            |
|---------|----------------------|------------------|
| Mover   | ← / →                | Botões ◀ ▶       |
| Pular   | ↑ ou Barra de espaço | Botão ▲          |

Segure o botão de pulo para pular mais alto (altura variável).

## Objetivo

- 🍎 Colete maçãs para ganhar pontos (50 cada).
- 👾 Pule em cima dos inimigos para derrotá-los (100 cada). Encostar de lado
  tira uma vida.
- 🚩 Alcance a bandeira no fim da fase (+500) para avançar.
- ❤ Você começa com 3 vidas. Cair em buracos ou ser atingido custa uma vida.
- São **3 fases**; conclua todas para vencer.

## Opções

- **Escolha do personagem**: selecione **Rex** ou **Lima** na tela inicial. A
  escolha é lembrada entre as sessões.
- **♾️ Vidas infinitas**: ative o botão na tela inicial (ou durante o jogo) para
  jogar sem perder — ao cair ou ser atingido, você simplesmente renasce. O HUD
  mostra `∞`.
- **💾 Salvar progresso**: o jogo salva automaticamente (fase, pontuação, vidas,
  personagem e modo) no `localStorage` do navegador. Se houver um jogo em
  andamento, o botão **Continuar** aparece na tela inicial e retoma exatamente de
  onde você parou. **Novo jogo** recomeça da fase 1 mantendo suas preferências.

## Detalhes técnicos

- Canvas 2D — os personagens jogáveis usam os sprites reais recortados da foto
  (`assets/*.png`); inimigos, moedas e cenário são desenhados proceduralmente.
- Física de plataforma com gravidade, atrito, pulo de altura variável, colisão
  por AABB, câmera com rolagem lateral e fundo em parallax.
- Inimigos patrulham plataformas e mudam de direção nas bordas.
- Suporte a teclado e a controles de toque (mobile).
- Progresso e preferências persistidos em `localStorage` (chave
  `dinobros_save_v1`).

Arquivos:
- `index.html` — estrutura, telas (início, seleção de personagem, mensagens) e HUD.
- `game.js` — motor do jogo, física, níveis e renderização.
- `assets/rex.png`, `assets/lima.png` — sprites dos personagens recortados da foto.
