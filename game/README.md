# 🦕 Dino Bros — Aventura no Bosque

Um jogo de plataforma em HTML5 no estilo **Super Mario Bros**, protagonizado por
dois dinossauros **recortados diretamente da imagem enviada**: **Rex** (verde,
jaqueta vermelha, laço amarelo) e **Lima** (verde-claro, camisa amarela, laço
amarelo). Os sprites em `assets/rex.png` e `assets/lima.png` foram extraídos da
foto com remoção de fundo (segmentação GrabCut), preservando a arte original.

## Como jogar

**Basta dar duplo-clique em `index.html`** — abre em qualquer navegador moderno,
sem servidor, sem build e sem dependências. Os sprites dos personagens vêm
embutidos (base64, em `assets.js`), então não há arquivos externos que o
navegador possa bloquear no modo `file://`.

Se preferir, também funciona por HTTP local (opcional):

```bash
cd game
python3 -m http.server 8000   # ou: npx serve
# acesse http://localhost:8000
```

## Controles

| Ação    | Teclado              | Toque            |
|---------|----------------------|------------------|
| Mover   | ← / →                | Botões ◀ ▶       |
| Pular   | ↑ ou Barra de espaço | Botão ▲          |
| Atirar 🔥 | F ou X             | Botão 🔥 (aparece com a flor de fogo) |

## Power-ups (estilo Mario)

Espalhados pelas fases, dão novos poderes ao personagem (mostrados no HUD "Poder"):

- 🍄 **Cogumelo** — faz o personagem **crescer** e **pular mais alto**.
- 🔥 **Flor de fogo** — permite **atirar bolas de fogo** (F / X ou o botão 🔥) que
  derrotam inimigos à distância.
- 🪽 **Flor voadora** — permite **voar**: segure o botão de pular para subir.

Ao tomar dano com um poder ativo, o personagem **perde o poder** em vez de morrer
(fica invulnerável por um instante). O poder é mantido entre as fases e salvo no
progresso.

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
- `assets.js` — sprites embutidos em base64 (permite abrir sem servidor).
- `assets/rex.png`, `assets/lima.png` — sprites originais recortados da foto (fonte do base64).
