# Customer Discovery Kit — Portuguese Insurance Brokers

**Goal:** run 5+ discovery conversations with SMB insurance brokers in Lisbon/Porto to answer the one question desk research could not — *does the missed-call pain actually exist, and what is it worth?* Companion to `voice-agent-niches-portugal-europe.md`.

---

## Golden rules (read once before you dial)

1. **Never mention the product.** No "AI voice agent", no pitch. The moment they smell a sale, they get polite instead of honest. You're a founder researching how brokerages handle calls.
2. **Ask about the past, not the future.** "What happened to the last after-hours call?" beats "would you use X?" Hypotheticals produce flattering lies (The Mom Test).
3. **Trade value for their time.** You're not asking a favor — you're giving them peer intel they can't get anywhere else. See "What's in it for them" below.
4. **Shut up after each question.** The real answer comes in the pause after the polished first answer. Count to five.
5. **Decision rule:** if 3 of 5 come back 🟢 hot, build the wedge prototype. If mostly 🟡/🔴, the pain isn't sharp enough at SMB brokers — drop down the niche list before writing code.

---

## What's in it for them (why they'll say yes)

Most won't reply — cold discovery is ~1 in 5, which is why the list has 19. What moves the odds, strongest first:

1. **A peer benchmark.** You're interviewing 10+ brokerages about the same thing. Promise each participant a short summary — *"how 10 Lisbon/Porto brokerages handle after-hours calls, and where you stand."* Competitive intel nobody publishes, costs you nothing extra. This is the biggest lever.
2. **Ask for advice, not a meeting.** "Pick your brain / your advice" gets a yes where "book a meeting" gets ignored. Owner-operators rarely get asked about their own problems; being the expert is pleasant.
3. **Warm > cold by 5x.** One hour finding two warm intros (APROSE, LinkedIn 2nd-degree, a shared insurer contact) beats the whole cold list.
4. **Local and human.** In their language, offer to come by with a coffee. Small brokers run on relationships.
5. **For 🟢 hot leads — design-partner status.** Early access, free for the first few months, product built around their workflow. Not a pitch — a free fix for their missed-calls problem. This converts an interview into a first customer. Don't lead with it; offer it once the pain is confirmed.

### Warm-intro playbook
- **APROSE** (Associação Nacional de Agentes e Corretores de Seguros) — the brokers' association. Find members, ask for an intro, show up at an event.
- **LinkedIn** — search these firms for 2nd-degree connections; ask the mutual to introduce you.
- **Shared insurers** — anyone you know at Fidelidade, Allianz, Tranquilidade, Generali, Ageas knows dozens of brokers.
- **Walk in** — the single-storefront shops below take walk-ins. "Tenho 15 minutos do seu tempo?"

---

## Cold email (benchmark + advice framing)

Short, no product, asks for advice, promises the benchmark back. Send one at a time from your real name, follow up once after 3 days.

### 🇵🇹 European Portuguese
> **Assunto:** Um conselho sobre as chamadas na sua corretora? (15 min)
>
> Olá [Nome],
>
> Chamo-me [o teu nome], sou de [Lisboa/Porto]. Estou a falar com várias corretoras da região para perceber como gerem as chamadas dos clientes — sobretudo fora de horas e nos picos.
>
> Não vendo nada. Queria só o conselho de quem faz isto todos os dias. Em troca, junto o que aprender com cerca de 10 corretoras e partilho consigo o resumo — para ver como se compara com as outras.
>
> Tem 15 minutos esta semana? Posso ligar, ou passar aí e ofereço o café — o que lhe der mais jeito.
>
> Obrigado,
> [o teu nome] · [telefone]

### 🇬🇧 English
> **Subject:** A bit of advice on how your brokerage handles calls? (15 min)
>
> Hi [Name],
>
> I'm [your name], based in [Lisbon/Porto]. I'm speaking with brokerages across the region to understand how they handle client calls — especially after hours and at peak times.
>
> I'm not selling anything. I just want the advice of someone who does this daily. In return, I'll compile what I learn from ~10 brokerages and share the summary with you — so you can see how you compare.
>
> Do you have 15 minutes this week? I can call, or come by with a coffee — whatever's easiest.
>
> Thanks,
> [your name] · [phone]

---

## Discovery script

Ask, then stay silent. The gold is in the pause.

### Q1 — Does the pain exist (status quo)
> 🇵🇹 *"Quando entra uma chamada depois das 18h, ou à hora de almoço, o que é que lhe acontece na prática?"*
> 🇬🇧 "When a call comes in after 6pm, or during lunch — what actually happens to it?"
> **Probes:** Who picks up? Voicemail nobody checks? Rings out? *When was the last time that happened?*

### Q2 — The missed-call number (the refuted figure — get the real one)
> 🇵🇹 *"Numa semana normal, quantas chamadas acha que ficam sem resposta? E quando perde a chamada de um cliente novo, quanto é que isso vale, mais ou menos?"*
> 🇬🇧 "In a normal week, how many calls go unanswered? And when you miss a new client's call — roughly what's that worth to you?"
> **Probes:** Push gently for a number. "Lembra-se da última apólice que perdeu assim?" A specific lost deal beats an average.

### Q3 — Who answers today (status quo cost + budget)
> 🇵🇹 *"Quem atende o telefone hoje em dia? É alguém aqui no escritório, é um call center? Quanto é que isso vos custa?"*
> 🇬🇧 "Who answers the phones today — someone in the office, a call center? What does that cost you?"
> **Probes:** How many people? When are they overwhelmed? What happens when everyone's on a call?

### Q4 — Wedge test (ask late, after rapport; the market is crowded)
There are already ≥5 PT-native "AI receptionist" tools (Voxial, VoiceFleet, Atendia, BrightAI). So test whether *generic* is good enough or whether insurance depth matters:
> 🇵🇹 *"Se houvesse um serviço que atendesse as chamadas automaticamente, o que é que ele teria mesmo de saber fazer? Bastava marcar um retorno, ou tinha de perceber de apólices e sinistros?"*
> 🇬🇧 "If a service answered the calls automatically — what would it *have* to be able to do? Is booking a callback enough, or does it need to actually understand policies and claims (sinistros)?"
> **Why:** if "booking a callback is fine," a generic tool already wins and you have no wedge. If "it has to handle a sinistro properly," that's your opening. Also listen for *"já uso o X"* — if they name a competitor, that's gold, ask what they hate about it.

### Q5 — Channel test (critical: every vertical competitor chose text, not voice)
Panora, Foliume, and virtualworkforce.ai all built for brokers on WhatsApp/email, not voice. Find out if you're betting on the wrong channel *before* you build:
> 🇵🇹 *"Quando um cliente não o consegue apanhar, o que é que ele prefere? Que alguém atenda mesmo o telefone, ou resolver por WhatsApp / mensagem?"*
> 🇬🇧 "When a client can't reach you — what do they actually prefer? Someone to *answer the phone*, or to sort it on *WhatsApp / message*?"
> **Why:** if brokers say "os meus clientes vivem no WhatsApp," your voice-first thesis is in trouble and you should know now. If they say "no sinistro, querem falar com uma pessoa," voice wins for the urgent moment. Listen for *which situations* need voice (claims/urgency) vs text (admin/documents).

### Q6 — AI acceptance (quick probe; MudeyPro markets "no AI" — sentiment may be a headwind)
A major PT broker-software vendor explicitly advertises no-AI. Test whether brokers/clients are AI-averse here:
> 🇵🇹 *"E se fosse um assistente automático, com IA, a atender essas chamadas — o que é que achava disso? E os seus clientes, como reagiriam?"*
> 🇬🇧 "And if it were an automated, AI assistant answering those calls — how would you feel about that? And your clients — how would they react?"
> **Why:** if the reaction is "os meus clientes odiariam falar com um robô," you have an adoption headwind and the pitch must lead with disclosure + "humano quando é preciso." If they're open, the no-AI positioning of others is a gap you can exploit. Either way, you need to know the market's AI temperature.

### Close (opens a pilot without pitching)
> 🇵🇹 *"Isto foi muito útil. Posso voltar a contactá-lo se descobrir alguma coisa que ajude com este problema das chamadas?"*
> 🇬🇧 "This was really helpful. Can I come back to you if I find something that helps with the missed-calls problem?"

A "yes, please do" is a soft demand signal. A shrug is data too.

---

## Capture tracker

Fill one row right after you hang up. The rightmost column is the whole game.

| Broker | Date | Q1: after-hours calls | Q2: missed/week + €value | Q3: who answers + cost | Come back? | 🌡️ Pain |
|--------|------|-----------------------|--------------------------|------------------------|-----------|---------|
| e.g. MEVI | 07-14 | rings out, no voicemail | "~10/wk, lost a €600 policy last month" | 2 staff, both on calls at lunch | "yes, call me" | 🟢 |
| | | | | | | |
| | | | | | | |
| | | | | | | |
| | | | | | | |

**🌡️ Scoring:** 🟢 Hot = gave a real missed-call number *and* it visibly bugs them (Q1 demand, found). 🟡 Warm = misses calls but shrugs. 🔴 Cold = no after-hours calls / doesn't care.

---

## Target list — Lisbon & Porto SMB brokers

Best-effort from public sources (directories + broker sites). "Verified" = quoted verbatim in a search snippet of that page, not personally rendered. **Confirm each firm's active ASF registration and re-check the number before dialing.** Top ~8 (MEVI, Canaverde, Albuquerque, Scalis, Abílio Teixeira, Boavista, A. Octávio, Optirisk) are high-confidence.

### Lisbon
| Name | Area | Phone | Email | Website |
|---|---|---|---|---|
| MEVI Seguros | Benfica | 211 340 819 | geral@meviseguros.pt | meviseguros.pt |
| Canaverde Seguros | Santos/Estrela | 21 390 17 97 / 96 577 34 36 | geral@canaverde.com | canaverde.com |
| Albuquerque Mediadores de Seguros, Lda | Lisbon | 217 542 090 | geral@albuquerque.pt | albuquerque.pt |
| Scalis — Mediação de Seguros | Lisbon | 210 945 601 | geral@scalis.pt | scalis.pt |
| Optirisk — Mediação de Seguros, Lda | Lisbon | — | info@optirisk.pt | optirisk.pt |
| R2 Seguros | Rua das Galés | — | — | r2seguros.pt |
| F2F — Mediação de Seguros, Lda | Coração de Jesus | 213 556 714 | — | — |
| Molinari, Lda | São Mamede | 213 826 140 | — | — |
| AVMS — Mediação de Seguros, Lda | Lumiar | 217 122 982 | — | — |
| Serenitas — Soc. Mediadora de Seguros, Lda | São João de Brito | 218 457 500 | — | — |
| Temposeguro — Mediação de Seguros, Lda | Lumiar | 218 453 630 | — | — |
| Meu Seguro — Mediação de Seguros | São Vicente | 212 435 736 | — | — |
| José Mata — Corretores e Consultores de Seguros, Lda | São Mamede | 213 817 500 | — | — |
| Sousa Mendes — Mediação Seguros, Lda | Olivais | — | — | — |

### Porto
| Name | Area | Phone | Email | Website |
|---|---|---|---|---|
| Abílio Teixeira — Mediação de Seguros, Lda | R. Dr. Adriano Paiva | 22 502 35 00 | geral@abilioteixeiraseguros.com | abilioteixeiraseguros.com |
| Boavista — Mediação de Seguros, Lda | Av. Fontes P. de Melo | 220 146 944 | geral@boavistaseguros.pt | boavistaseguros.pt |
| A. Octávio — Mediação de Seguros | Bonjardim | 222 018 311 / 925 768 450 | porto@aoctavioseguros.com | aoctavioseguros.pt |
| PAContas — Mediação de Seguros | Grande Porto | — | — | pacontas.pt |
| Agente SS — Sociedade de Mediação de Seguros | Porto | — | — | — |

**Data-quality caveats:** Phone-only Lisbon entries (F2F, Molinari, AVMS, Serenitas, Temposeguro, Meu Seguro, José Mata) are medium-confidence — numbers consistent across directories but emails unconfirmed. Porto side is thinner (5 vs 14); the **APROSE member directory** and the **ASF "Entidades Autorizadas" register** are the authoritative next step to confirm registrations and expand Porto. Large/multinational brokers (MDS, Marsh, Aon) were deliberately excluded to match the SMB wedge.

---

## Parallel track — talk to the rails-owners (Mudey / Gemese)

**Why this is separate:** the competitive teardown found your biggest threat isn't the horizontal voicebots — it's the broker-management software brokers already run on. **Mudey/MudeyPro** and **Milenia/Gemese** (~300 mediadores) own the rails. If either adds a voice layer, they beat any standalone. So you need to know: **partner or compete?** This is a different conversation than broker discovery — you're talking to a platform vendor, not a customer.

**Who to reach:** Mudey (mudey.pt / MudeyPro), Milenia (Gemese software). Aim for someone in product/partnerships, not sales.

**What you're trying to learn (don't tip your hand):**
1. *"Estão a trabalhar em atendimento automático / IA de voz para os mediadores que usam a vossa plataforma?"* — Are they already building voice? (If yes, the standalone play is much harder.)
2. *"A vossa plataforma permite integrações de terceiros? Como funciona?"* — Do they have an API / partner program you could integrate with? (If open → you can build *on* the rails instead of against them.)
3. *"Quantos mediadores usam a plataforma, e que tipo — pequenos independentes ou grandes?"* — Sizing the rails and whether your SMB target overlaps.

**Decision this informs:**
- **Rails are open + no voice plans →** build the voice layer *on top of* MudeyPro/Gemese. Integration becomes your moat, not your obstacle.
- **Rails are closed / they're building voice themselves →** you're competing with an incumbent that owns the broker's back office. Much harder. Reconsider the wedge, or find the brokers *not* on those platforms.

**Update (2026-07): MudeyPro explicitly markets that it does NOT use AI.** Good news — they are not building a competing voice layer, so the "eat you" threat drops and the integration path is more open. But the *positioning* is a warning: a rails-owner advertising "no AI" may be reading its brokers as AI-skeptical. **Verify the scope of their claim** — blanket "somos humanos, sem IA" (a real adoption headwind) vs a narrow "não usamos IA para subscrição/aconselhamento" (just IDD/advice-liability prudence, neutral for a phone-answering agent). This directly feeds Q6 in the script above.

**Sequencing:** do this **after** 2-3 broker calls (so you speak their language and know the pain), but **before** you write a line of code — it can change the entire architecture.

---

## Design-partner offer (for 🟢 hot leads only)

Once a broker confirms real missed-call pain, offer — not before:
> 🇵🇹 *"Estou a construir uma solução para este problema exato. Quer ser dos primeiros a testar, de graça nos primeiros meses, e ajudar a moldá-la ao que precisa?"*
> 🇬🇧 "I'm building something for this exact problem. Want to be one of the first to test it, free for the first few months, and help shape it to what you need?"

Free early access + influence over the product, in exchange for being your first real reference and data source.
