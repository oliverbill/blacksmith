/* ============================================================
   DINO BROS — Aventura no Bosque
   Um platformer estilo Super Mario Bros com personagens
   dinossauros desenhados no canvas (sem imagens externas).
   ============================================================ */
(() => {
  "use strict";

  const canvas = document.getElementById("game");
  const ctx = canvas.getContext("2d");
  const W = canvas.width, H = canvas.height;

  // ---- HUD ----
  const $score = document.getElementById("score");
  const $level = document.getElementById("level");
  const $lives = document.getElementById("lives");

  // ---- Screens ----
  const startScreen = document.getElementById("startScreen");
  const msgScreen   = document.getElementById("msgScreen");
  const msgTitle    = document.getElementById("msgTitle");
  const msgText     = document.getElementById("msgText");
  const msgBtn      = document.getElementById("msgBtn");
  const startBtn    = document.getElementById("startBtn");
  const touchLayer  = document.getElementById("touch");

  // ============================================================
  //  CHARACTERS
  // ============================================================
  // Two dinosaurs cut out directly from the reference photo (PNG sprites).
  // `spark` is an accent color used only for particle effects.
  // `nativeFacing` = the direction the source art already faces (1=right, -1=left);
  // the sprite is mirrored when the player moves the other way.
  const CHARACTERS = [
    { name:"Rex",  src:"assets/rex.png",  spark:"#ffd23f", nativeFacing:1, img:null, ready:false },
    { name:"Lima", src:"assets/lima.png", spark:"#ffd23f", nativeFacing:1, img:null, ready:false },
  ];
  let chosen = 0;

  // preload sprite images
  CHARACTERS.forEach(c => {
    const img = new Image();
    img.onload = () => { c.ready = true; };
    img.src = c.src;
    c.img = img;
  });

  // ============================================================
  //  PHYSICS CONSTANTS
  // ============================================================
  const GRAVITY   = 0.62;
  const MOVE      = 0.8;
  const FRICTION  = 0.82;
  const MAX_VX    = 4.6;
  const JUMP_VY   = -12.4;
  const TILE      = 40;

  // ============================================================
  //  LEVELS  (tile maps)
  //  Legend:
  //   . empty     G ground     B brick/platform
  //   ? coin      E enemy       F flag (goal)
  //   P player start
  // ============================================================
  const LEVELS = [
`................................................................................
................................................................................
................................................................................
..........?.?...................................................................
..............................BBBB..............................................
.................?..?...............................?.?.?.......................
............BBB.............BBB..............BBBB................................
.......................?.........................................F.............
..P.........E.......BB......E...........?..E.............E.......G..............
GGGGGGGGGG..GGGGGGGGGGGGGGGGGGGGGG...GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG`,

`................................................................................
................................................................................
.................?.?.?..........................................................
..............................BBB...............?...?...........................
.........?...................................BBBBB..............................
......BBBB.........E...............BB..............................F.............
...........................?..............E...............BBB.....G.............
..P.....E..........BBB.........E.......?.......E....?.....E.......G..............
GGGGGGGGGGGGGGGGGG....GGGGGGGGGGGGGGG..GGGGGGGGGGGGGG..GGGGGGGGGGGGGGGGGGGGGGGGGGG`,

`................................................................................
.............?..?..?..?.........................................................
...........BBBBBBBB...................?.?.?.....................................
......................................BBBBB.....................................
....?...............E.....E.......................E.....E.......F...............
...BB......?.................BBB..........?.?.........BBBB......G................
.........BBBB.....................E..............BBB...........G................
..P..E..........BB.....?.....E..........BB..E.........?...E....G................
GGGGGGGGGG..GGGGGGGGGGGGGGGGGG..GGGGGGGGGGGGGGGGGG..GGGGGGGGGGGGGGGGGGGGGGGGGGGGGG`,
  ];

  // ============================================================
  //  WORLD STATE
  // ============================================================
  let state = "start"; // start | play | dead | win | gameover
  let levelIdx = 0;
  let score = 0, lives = 3;
  let solids = [];   // {x,y,w,h}
  let coins = [];    // {x,y,w,h,taken,phase}
  let enemies = [];  // {x,y,w,h,vx,alive,squash}
  let flag = null;   // {x,y,w,h}
  let levelW = 0, levelH = 0;
  let cameraX = 0;
  let particles = [];

  const player = {
    x:0, y:0, w:30, h:38, vx:0, vy:0,
    onGround:false, face:1, walk:0, dead:false, deadT:0, blink:0, spawnX:0, spawnY:0
  };

  // ============================================================
  //  INPUT
  // ============================================================
  const keys = { left:false, right:false, jump:false, jumpHeld:false };

  addEventListener("keydown", e => {
    if (["ArrowLeft","ArrowRight","ArrowUp","ArrowDown"," "].includes(e.key)) e.preventDefault();
    if (e.key === "ArrowLeft") keys.left = true;
    if (e.key === "ArrowRight") keys.right = true;
    if (e.key === "ArrowUp" || e.key === " ") { if (!keys.jumpHeld) keys.jump = true; keys.jumpHeld = true; }
  });
  addEventListener("keyup", e => {
    if (e.key === "ArrowLeft") keys.left = false;
    if (e.key === "ArrowRight") keys.right = false;
    if (e.key === "ArrowUp" || e.key === " ") keys.jumpHeld = false;
  });

  // Touch buttons
  function bindTouch(id, on, off) {
    const el = document.getElementById(id);
    const start = e => { e.preventDefault(); on(); };
    const end   = e => { e.preventDefault(); off(); };
    el.addEventListener("touchstart", start, {passive:false});
    el.addEventListener("touchend", end);
    el.addEventListener("touchcancel", end);
    el.addEventListener("mousedown", start);
    el.addEventListener("mouseup", end);
    el.addEventListener("mouseleave", end);
  }
  bindTouch("btnLeft",  () => keys.left = true,  () => keys.left = false);
  bindTouch("btnRight", () => keys.right = true, () => keys.right = false);
  bindTouch("btnJump",  () => { keys.jump = true; keys.jumpHeld = true; }, () => keys.jumpHeld = false);

  if ("ontouchstart" in window) touchLayer.classList.add("on");

  // ============================================================
  //  LEVEL LOADING
  // ============================================================
  function loadLevel(idx) {
    const map = LEVELS[idx].split("\n");
    solids = []; coins = []; enemies = []; particles = []; flag = null;
    levelH = map.length * TILE;
    levelW = map[0].length * TILE;

    for (let r = 0; r < map.length; r++) {
      for (let c = 0; c < map[r].length; c++) {
        const ch = map[r][c];
        const x = c * TILE, y = r * TILE;
        if (ch === "G" || ch === "B") {
          solids.push({ x, y, w:TILE, h:TILE, type: ch === "G" ? "ground" : "brick" });
        } else if (ch === "?") {
          coins.push({ x:x+10, y:y+8, w:20, h:24, taken:false, phase:Math.random()*6.28 });
        } else if (ch === "E") {
          enemies.push({ x:x+4, y:y+6, w:32, h:32, vx:-0.9, alive:true, squash:0 });
        } else if (ch === "F") {
          flag = { x:x+16, y:y - TILE*2, w:8, h:TILE*3 };
        } else if (ch === "P") {
          player.spawnX = x; player.spawnY = y;
        }
      }
    }
    respawnPlayer();
    cameraX = 0;
  }

  function respawnPlayer() {
    player.x = player.spawnX; player.y = player.spawnY;
    player.vx = 0; player.vy = 0; player.dead = false; player.deadT = 0;
    player.face = 1; player.walk = 0; player.onGround = false;
  }

  // ============================================================
  //  GAME FLOW
  // ============================================================
  function startGame() {
    levelIdx = 0; score = 0; lives = 3;
    startScreen.classList.add("hidden");
    msgScreen.classList.add("hidden");
    loadLevel(levelIdx);
    state = "play";
    updateHUD();
  }

  function showMsg(title, text, btn) {
    msgTitle.textContent = title;
    msgText.textContent = text;
    msgBtn.textContent = btn;
    msgScreen.classList.remove("hidden");
  }

  function nextLevel() {
    levelIdx++;
    if (levelIdx >= LEVELS.length) {
      state = "win";
      showMsg("🏆 Você venceu!", `Parabéns! ${CHARACTERS[chosen].name} atravessou todo o bosque. Pontuação final: ${score} 🍎`, "🔁 Jogar de novo");
    } else {
      state = "levelend";
      showMsg("✔ Fase concluída!", `Rumo à fase ${levelIdx + 1}. Pontuação: ${score} 🍎`, "▶ Próxima fase");
    }
  }

  function loseLife() {
    lives--;
    updateHUD();
    if (lives <= 0) {
      state = "gameover";
      showMsg("💀 Fim de jogo", `Que pena! Pontuação: ${score} 🍎. Tente novamente.`, "🔁 Recomeçar");
    } else {
      respawnPlayer();
      state = "play";
    }
  }

  function updateHUD() {
    $score.textContent = score;
    $level.textContent = levelIdx + 1;
    $lives.textContent = lives;
  }

  // ============================================================
  //  UPDATE
  // ============================================================
  function update() {
    if (state !== "play") return;

    const p = player;

    if (p.dead) {
      p.deadT++;
      p.vy += GRAVITY;
      p.y += p.vy;
      if (p.deadT > 70) loseLife();
      return;
    }

    // Horizontal input
    if (keys.left)  { p.vx -= MOVE; p.face = -1; }
    if (keys.right) { p.vx += MOVE; p.face = 1; }
    if (!keys.left && !keys.right) p.vx *= FRICTION;
    p.vx = Math.max(-MAX_VX, Math.min(MAX_VX, p.vx));
    if (Math.abs(p.vx) < 0.05) p.vx = 0;

    // Jump
    if (keys.jump && p.onGround) {
      p.vy = JUMP_VY;
      p.onGround = false;
      spawnDust(p.x + p.w/2, p.y + p.h);
    }
    keys.jump = false;
    // Variable jump height
    if (!keys.jumpHeld && p.vy < -4) p.vy = -4;

    p.vy += GRAVITY;
    if (p.vy > 16) p.vy = 16;

    // Move + collide X
    p.x += p.vx;
    collide(p, "x");
    // Move + collide Y
    p.y += p.vy;
    p.onGround = false;
    collide(p, "y");

    // Walk animation
    if (p.onGround && Math.abs(p.vx) > 0.4) p.walk += Math.abs(p.vx) * 0.06;
    else p.walk = 0;
    p.blink = (p.blink + 1) % 220;

    // World bounds
    if (p.x < 0) { p.x = 0; p.vx = 0; }
    if (p.x + p.w > levelW) { p.x = levelW - p.w; p.vx = 0; }

    // Fell in a pit
    if (p.y > levelH + 60) killPlayer();

    updateEnemies();
    updateCoins();
    updateParticles();

    // Flag / goal
    if (flag && rectsOverlap(p, flag)) {
      score += 500;
      updateHUD();
      nextLevel();
      return;
    }

    // Camera follows player
    const target = p.x + p.w/2 - W/2;
    cameraX += (target - cameraX) * 0.12;
    cameraX = Math.max(0, Math.min(cameraX, levelW - W));
  }

  function collide(o, axis) {
    for (const s of solids) {
      if (!rectsOverlap(o, s)) continue;
      if (axis === "x") {
        if (o.vx > 0) o.x = s.x - o.w;
        else if (o.vx < 0) o.x = s.x + s.w;
        o.vx = 0;
      } else {
        if (o.vy > 0) { o.y = s.y - o.h; o.onGround = true; o.vy = 0; }
        else if (o.vy < 0) { o.y = s.y + s.h; o.vy = 0; }
      }
    }
  }

  function updateEnemies() {
    const p = player;
    for (const e of enemies) {
      if (!e.alive) { e.squash = Math.max(0, e.squash - 1); continue; }

      // Patrol with gravity
      e.vy = (e.vy || 0) + GRAVITY;
      if (e.vy > 14) e.vy = 14;

      e.x += e.vx;
      // turn at walls
      for (const s of solids) {
        if (rectsOverlap(e, s)) {
          if (e.vx > 0) e.x = s.x - e.w; else e.x = s.x + s.w;
          e.vx *= -1;
        }
      }
      e.y += e.vy;
      let grounded = false;
      for (const s of solids) {
        if (rectsOverlap(e, s)) {
          if (e.vy > 0) { e.y = s.y - e.h; e.vy = 0; grounded = true; }
          else { e.y = s.y + s.h; e.vy = 0; }
        }
      }
      // turn at ledges (avoid walking off edges)
      if (grounded) {
        const aheadX = e.vx > 0 ? e.x + e.w + 2 : e.x - 2;
        const footY = e.y + e.h + 4;
        let floor = false;
        for (const s of solids) {
          if (aheadX >= s.x && aheadX <= s.x + s.w && footY >= s.y && footY <= s.y + s.h) { floor = true; break; }
        }
        if (!floor) e.vx *= -1;
      }
      // fell off world
      if (e.y > levelH + 80) e.alive = false;

      // Collision with player
      if (rectsOverlap(p, e) && !p.dead) {
        const stomping = p.vy > 0 && (p.y + p.h) - e.y < 20;
        if (stomping) {
          e.alive = false; e.squash = 16;
          p.vy = JUMP_VY * 0.62;
          score += 100; updateHUD();
          spawnPop(e.x + e.w/2, e.y + e.h/2);
        } else {
          killPlayer();
        }
      }
    }
  }

  function updateCoins() {
    const p = player;
    for (const c of coins) {
      if (c.taken) continue;
      c.phase += 0.12;
      if (rectsOverlap(p, c)) {
        c.taken = true;
        score += 50; updateHUD();
        spawnSpark(c.x + c.w/2, c.y + c.h/2);
      }
    }
  }

  function killPlayer() {
    if (player.dead) return;
    player.dead = true;
    player.deadT = 0;
    player.vy = -9;
  }

  // ---- particles ----
  function spawnDust(x, y) {
    for (let i = 0; i < 5; i++)
      particles.push({ x, y, vx:(i-2)*0.6, vy:-Math.random()*1.5, life:22, col:"#e9d7a0", r:3 });
  }
  function spawnPop(x, y) {
    for (let i = 0; i < 10; i++)
      particles.push({ x, y, vx:(Math.random()-0.5)*4, vy:-Math.random()*3-1, life:26, col:"#b06a3a", r:3 });
  }
  function spawnSpark(x, y) {
    for (let i = 0; i < 10; i++)
      particles.push({ x, y, vx:(Math.random()-0.5)*4, vy:(Math.random()-0.5)*4, life:24, col:"#ffe15a", r:3 });
  }
  function updateParticles() {
    for (const pt of particles) { pt.x += pt.vx; pt.y += pt.vy; pt.vy += 0.18; pt.life--; }
    particles = particles.filter(pt => pt.life > 0);
  }

  // ============================================================
  //  HELPERS
  // ============================================================
  function rectsOverlap(a, b) {
    return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
  }

  // ============================================================
  //  RENDER
  // ============================================================
  function drawBackground() {
    // sky gradient
    const g = ctx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, "#5c94fc"); g.addColorStop(1, "#a8e0ff");
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);

    // parallax hills
    const off = cameraX * 0.3;
    ctx.fillStyle = "#6fae54";
    for (let i = -1; i < 6; i++) {
      const hx = i * 260 - (off % 260);
      hillArc(hx, H - 70, 150, 90);
    }
    ctx.fillStyle = "#5a9945";
    const off2 = cameraX * 0.5;
    for (let i = -1; i < 8; i++) {
      const hx = i * 200 - (off2 % 200);
      hillArc(hx, H - 55, 110, 70);
    }
    // clouds
    ctx.fillStyle = "rgba(255,255,255,.85)";
    const co = cameraX * 0.15;
    for (let i = -1; i < 6; i++) cloud(i * 240 - (co % 240) + 60, 60 + (i % 2) * 30);
  }
  function hillArc(x, y, w, h) {
    ctx.beginPath(); ctx.moveTo(x - w, y);
    ctx.quadraticCurveTo(x, y - h, x + w, y);
    ctx.closePath(); ctx.fill();
  }
  function cloud(x, y) {
    ctx.beginPath();
    ctx.arc(x, y, 18, 0, 7); ctx.arc(x + 20, y + 4, 22, 0, 7);
    ctx.arc(x + 44, y, 16, 0, 7); ctx.arc(x + 22, y - 8, 16, 0, 7);
    ctx.fill();
  }

  function drawSolids() {
    for (const s of solids) {
      const sx = s.x - cameraX;
      if (sx + s.w < 0 || sx > W) continue;
      if (s.type === "ground") {
        ctx.fillStyle = "#8a5a2b"; ctx.fillRect(sx, s.y, s.w, s.h);
        ctx.fillStyle = "#5fa83d"; ctx.fillRect(sx, s.y, s.w, 10);
        ctx.fillStyle = "#4d8a30"; ctx.fillRect(sx, s.y + 8, s.w, 4);
        ctx.fillStyle = "rgba(0,0,0,.12)";
        ctx.fillRect(sx + 6, s.y + 18, 5, 5); ctx.fillRect(sx + 24, s.y + 28, 5, 5);
      } else {
        ctx.fillStyle = "#c96f2e"; ctx.fillRect(sx, s.y, s.w, s.h);
        ctx.fillStyle = "#a9551d"; ctx.fillRect(sx, s.y, s.w, 4); ctx.fillRect(sx, s.y + s.h - 4, s.w, 4);
        ctx.fillStyle = "rgba(0,0,0,.18)"; ctx.fillRect(sx + s.w/2 - 1, s.y, 2, s.h);
        ctx.strokeStyle = "rgba(0,0,0,.25)"; ctx.strokeRect(sx + .5, s.y + .5, s.w - 1, s.h - 1);
      }
    }
  }

  function drawCoins() {
    for (const c of coins) {
      if (c.taken) continue;
      const cx = c.x - cameraX + c.w/2;
      if (cx < -20 || cx > W + 20) continue;
      const cy = c.y + c.h/2 + Math.sin(c.phase) * 3;
      const wobble = Math.abs(Math.cos(c.phase)); // fake spin
      // apple
      ctx.fillStyle = "#e63b2e";
      ctx.beginPath(); ctx.ellipse(cx, cy, 9 * (0.4 + 0.6 * wobble), 10, 0, 0, 7); ctx.fill();
      ctx.fillStyle = "#7a3b12"; ctx.fillRect(cx - 1, cy - 12, 2, 5);
      ctx.fillStyle = "#4caf50";
      ctx.beginPath(); ctx.ellipse(cx + 5, cy - 10, 5, 3, -0.5, 0, 7); ctx.fill();
      ctx.fillStyle = "rgba(255,255,255,.5)";
      ctx.beginPath(); ctx.ellipse(cx - 3, cy - 3, 2, 3, 0, 0, 7); ctx.fill();
    }
  }

  function drawFlag() {
    if (!flag) return;
    const fx = flag.x - cameraX;
    if (fx < -60 || fx > W + 60) return;
    // pole
    ctx.fillStyle = "#ddd"; ctx.fillRect(fx, flag.y, 6, flag.h);
    ctx.fillStyle = "#bbb"; ctx.fillRect(fx, flag.y, 2, flag.h);
    // ball on top
    ctx.fillStyle = "#ffd23f"; ctx.beginPath(); ctx.arc(fx + 3, flag.y, 8, 0, 7); ctx.fill();
    // waving flag
    const t = performance.now() / 200;
    ctx.fillStyle = "#e63b2e";
    ctx.beginPath();
    ctx.moveTo(fx + 6, flag.y + 8);
    ctx.lineTo(fx + 6 + 42, flag.y + 14 + Math.sin(t) * 3);
    ctx.lineTo(fx + 6, flag.y + 30);
    ctx.closePath(); ctx.fill();
  }

  function drawEnemies() {
    for (const e of enemies) {
      const ex = e.x - cameraX;
      if (ex + e.w < 0 || ex > W) continue;
      if (!e.alive) {
        if (e.squash > 0) {
          ctx.fillStyle = "#7a4a8a";
          ctx.beginPath(); ctx.ellipse(ex + e.w/2, e.y + e.h - 4, e.w/2, 6, 0, 0, 7); ctx.fill();
        }
        continue;
      }
      drawEnemy(ex, e.y, e.w, e.h, e.vx);
    }
  }

  // A grumpy spiky snail-beetle enemy
  function drawEnemy(x, y, w, h, vx) {
    const cx = x + w/2, by = y + h;
    const wig = Math.sin(performance.now() / 120) * 1.5;
    // body
    ctx.fillStyle = "#8e44ad";
    ctx.beginPath(); ctx.ellipse(cx, by - h*0.35, w*0.5, h*0.38, 0, 0, 7); ctx.fill();
    // shell top
    ctx.fillStyle = "#6c3483";
    ctx.beginPath(); ctx.ellipse(cx, by - h*0.5 + wig, w*0.42, h*0.3, 0, Math.PI, 0); ctx.fill();
    // spikes
    ctx.fillStyle = "#4a235a";
    for (let i = -1; i <= 1; i++) {
      ctx.beginPath();
      ctx.moveTo(cx + i*10 - 4, y + 6 + wig);
      ctx.lineTo(cx + i*10, y - 2 + wig);
      ctx.lineTo(cx + i*10 + 4, y + 6 + wig);
      ctx.closePath(); ctx.fill();
    }
    // feet
    ctx.fillStyle = "#4a235a";
    const fp = Math.sin(performance.now()/100) * 2;
    ctx.fillRect(cx - 12, by - 5 + fp, 7, 6);
    ctx.fillRect(cx + 5, by - 5 - fp, 7, 6);
    // eyes (angry)
    const dir = vx < 0 ? -1 : 1;
    ctx.fillStyle = "#fff";
    ctx.beginPath(); ctx.arc(cx - 6*dir, by - h*0.42, 5, 0, 7); ctx.arc(cx + 6*dir, by - h*0.42, 5, 0, 7); ctx.fill();
    ctx.fillStyle = "#000";
    ctx.beginPath(); ctx.arc(cx - 6*dir + dir*2, by - h*0.42, 2.4, 0, 7); ctx.arc(cx + 6*dir + dir*2, by - h*0.42, 2.4, 0, 7); ctx.fill();
    ctx.strokeStyle = "#000"; ctx.lineWidth = 1.6;
    ctx.beginPath();
    ctx.moveTo(cx - 11, by - h*0.55); ctx.lineTo(cx - 2, by - h*0.48);
    ctx.moveTo(cx + 11, by - h*0.55); ctx.lineTo(cx + 2, by - h*0.48);
    ctx.stroke();
  }

  // ---------- PLAYER DINO ----------
  // Player is rendered from the extracted photo sprite. The physics hitbox
  // (w,h) is smaller than the drawn sprite; the sprite is bottom-centered on
  // the hitbox so the feet line up with the ground.
  function drawDino(x, y, w, h, face, walk, ch, dead) {
    const c = CHARACTERS[ch];
    if (!c.ready) return;
    const dispH = h + 24;                 // draw a bit taller than the hitbox
    drawSprite(ctx, ch, x + w/2, y + h + 2, dispH, face, walk, dead);
  }

  function roundRect(x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function drawParticles() {
    for (const pt of particles) {
      ctx.globalAlpha = Math.max(0, pt.life / 26);
      ctx.fillStyle = pt.col;
      ctx.beginPath(); ctx.arc(pt.x - cameraX, pt.y, pt.r, 0, 7); ctx.fill();
    }
    ctx.globalAlpha = 1;
  }

  function render() {
    drawBackground();
    drawSolids();
    drawCoins();
    drawFlag();
    drawEnemies();
    drawParticles();
    if (state === "play" || state === "dead") {
      drawDino(player.x - cameraX, player.y, player.w, player.h, player.face, player.walk, chosen, player.dead);
    }
  }

  // Draw a character sprite (from the photo) into the given 2D context,
  // bottom-centered on (cx, footY), scaled to display height dispH.
  function drawSprite(g, ch, cx, footY, dispH, faceDir, walk, dead) {
    const c = CHARACTERS[ch];
    if (!c.ready) return;
    const ar = c.img.width / c.img.height;
    const dh = dispH;
    const dw = dh * ar;
    const bob = Math.abs(Math.sin(walk)) * 2;            // little walk bounce
    const tilt = Math.sin(walk) * 0.05;                  // subtle body sway
    const flip = (faceDir !== c.nativeFacing) ? -1 : 1;

    g.save();
    g.translate(cx, footY - bob);
    if (dead) {
      g.globalAlpha = 0.9;
      g.rotate(Math.PI);                                 // flip over when defeated
      g.drawImage(c.img, -dw/2, 0, dw, dh);
    } else {
      g.rotate(tilt);
      g.scale(flip, 1);
      g.drawImage(c.img, -dw/2, -dh, dw, dh);
    }
    g.restore();
  }

  // ============================================================
  //  MAIN LOOP
  // ============================================================
  let last = 0, acc = 0;
  const STEP = 1000 / 60;
  function loop(t) {
    if (!last) last = t;
    acc += Math.min(50, t - last); last = t;
    while (acc >= STEP) { update(); acc -= STEP; }
    render();
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);

  // ============================================================
  //  UI WIRING
  // ============================================================
  startBtn.addEventListener("click", startGame);

  msgBtn.addEventListener("click", () => {
    if (state === "levelend") {
      loadLevel(levelIdx);
      msgScreen.classList.add("hidden");
      state = "play";
      updateHUD();
    } else if (state === "win" || state === "gameover") {
      startScreen.classList.remove("hidden");
      msgScreen.classList.add("hidden");
      state = "start";
    }
  });

  // Character selection + preview thumbnails
  const charEls = document.querySelectorAll("#charPick .char");
  charEls.forEach(el => {
    el.addEventListener("click", () => {
      charEls.forEach(e => e.classList.remove("sel"));
      el.classList.add("sel");
      chosen = parseInt(el.dataset.char, 10);
    });
    // draw preview
    const idx = parseInt(el.dataset.char, 10);
    const pc = el.querySelector("canvas");
    const pctx = pc.getContext("2d");
    // temporarily borrow drawDino using an offscreen approach
    drawPreview(pctx, idx);
  });

  function drawPreview(pctx, idx) {
    const c = CHARACTERS[idx];
    const W2 = pctx.canvas.width, H2 = pctx.canvas.height;
    const render = () => {
      pctx.clearRect(0, 0, W2, H2);
      const ar = c.img.width / c.img.height;
      const dh = H2 - 6;
      const dw = dh * ar;
      pctx.drawImage(c.img, (W2 - dw) / 2, H2 - dh - 3, dw, dh);
    };
    if (c.ready) render();
    else c.img.addEventListener("load", render, { once:true });
  }

})();
