# Java Casino: Complete PokerBot AI Documentation

The `PokerBot.java` structure in the Java Casino framework utilizes a three-tier hierarchical AI progression system based on simulated personality profiles and varying degrees of Game Theory Optimal (GTO) play algorithms. When an AI entity joins the table, it securely initializes its personality via a core randomly generated integer (`botLevel`).

## Probability & Core Infrastructure
*   **0: Dumb Bot** - 44% Spawn Rate
*   **1: Smart Bot** - 44% Spawn Rate
*   **2: God Bot** - 12% Spawn Rate
*   **The Aventurine Flag**: If a bot randomly inherits the name "Aventurine", an `opMode` boolean is triggered. In the specific case of an `opMode` Dumb Bot, its logic is violently overridden: it has a 100% execution rate to shove **All-In** every single turn, regardless of board or hand state.

---

## 🟢 Level 0: The Dumb Bot
**Archetype: Erratic Gambler & Calling Station**
The Dumb Bot operates entirely independently of its cards, the board, or poker theory. It physically cannot recognize straights safely or calculate pot odds. It functions via a strict RNG algorithm.

### Core Mechanics
*   **Small Pots (Bet < Stack)**:
    *   **75% Probability**: Flat calls any bet or checks. (Bleeds chips in small increments).
    *   **10% Probability**: Attempts to raise up to 10% of its stack (if legal). If it mathematically cannot make an undersized raise, it has a 15% chance to shove All-In instead.
    *   **12% Probability**: Folds to a bet without calling.
    *   **3% Probability**: Randomly shoves All-In instantly.
*   **Extinction Protocol (Facing All-In)**:
    *   When faced with a bet that costs its entire stack, its gambler logic peaks: **It immediately calls the All-In 60% of the time** with completely random cards, and folds 40% of the time.

---

## 🔵 Level 1: The Smart Bot
**Archetype: Disciplined Aggressor ("Tilty")**
The Smart Bot operates off rigid threshold charts and heuristic mapping. It heavily prioritizes top-pair and straight/flush draws, but it possesses engineered "human flaws" that make it susceptible to massive variance over long sessions.

### Core Mechanics
*   **Preflop Matrix**: Understands premium hands via an array of 32 acceptable preflop holdings (`JJ+`, `AK`, suited broadways, and pocket pairs). Plays premiums 100% of the time.
*   **The 50% Draw Gamble**: If the bot identifies an open-ended straight draw or a 4-card flush draw on the board, it heavily overvalues it. Even if facing a massive bet that ruins its pot odds, it will **call the bet 50% of the time** to chase the card.
*   **Heuristic Sizing Constraints**: The Smart Bot communicates its hand strength through its bet size mathematically:
    *   *Small Raise (2.0x - 2.6x)*: Standard Top Pairs and weak combinations.
    *   *Medium Raise (2.5x - 4.1x)*: Assigned specifically to Straights, Flushes, or better.
    *   *Massive Raise (> 4.0x/All-In)*: Exclusively used for Full Houses and Quads.

---

## 🔴 Level 2: The God Bot
**Archetype: GTO Apex Predator & Exploitative Optimizer**
The God Bot is a mathematically rigorous entity that assesses Game Theory metrics, calculates dynamically shifting equity bounds, and actively reads opponent structures (`PokerBot` typing vs. `PokerPlayer`) to exploit hardcoded weaknesses.

### General Mechanics (Game Theory / GTO)
*   Computes true **Equity %** using the Rule of 4 & 2 equations.
*   Concludes actions exclusively on $+EV$ logic by mapping calculated equity directly against exact **Pot Odds (`Cost / Pot`)**.
*   **Elite Board Awareness**: Scans aggressively for board textures (Paired boards, 4-Straights, 4-Flushes). If the board creates a massive scare and it only holds Top Pair, it performs defensive check-folds against big bets.
*   **Multi-Street Aggression**: Maintains memory of its actions. C-bets 90% of the time on Ace-High flops. If it raised the flop, it executes **Triple Barrelling** (firing 70% of the time on Turn face-cards) to bully opponents regardless of its actual hand.
*   **Nut Blockers**: If the God bot holds the Ace of a flush suit on board (but does not have the flush itself), it will intentionally shove its entire stack, blocking the opponent from holding the nuts.

### Advanced Exploitation Suites
*   **The "Calling Station Bypass"**: Dynamically counts Dumb Bots in the hand. If `dumbBotCount > 0`, it intentionally slashes its Range-Advantage C-betting from 90% down to 20%, realizing there is zero psychological fold equity against calling stations.
*   **Greed Protocol**: If `dumbBotCount > 0` and the God Bot holds a Full House or better, it abandons standard Value-Bet sizing formulas (`pot * 0.75`) and instantly pushes a massive overbet **All-In**. It intentionally exploits the Dumb Bot's hardcoded 75% flat-call loop to extract maximum tournament chips.
*   **The "50%+1" Exploit**: Scans for Level 1 Smart Bots. Recognizes their innate weakness in the codebase (folding marginal draws to half-stack bets). It scientifically prices its bluffs to precisely `(SmartBot_Stack / 2) + 1` intentionally forcing the algorithm out of the pot.
*   **Level 3 Meta-Bluffing**: Uses recursive logic when facing other God Bots. Introduces a sudden 12% anomaly variant capable of executing massive polarized-overbets (1.5x - 2.5x pot) to break the other God Bot's strict math calculations.
*   **Heuristic Sizing Scanner (Soul Reading)**: Executes literal code-reading. The God Bot knows that Smart Bots structurally cannot bet above a 2.5x scale unless they hold the physical Nuts (Full House+). If a Smart Bot bets `>= 2.6x`, the God Bot instantaneously overrides its own math routines and folds any non-Nut hands, knowing with 100% algorithmic certainty that it is beat.

*(Note: God Bots treat human players (`PokerPlayer`) identically to Level 3 Meta-God Bots: they use standard GTO pot sizing, assume competence, and do not execute specific codebase hacks like Soul Reading, as Human intent cannot be reverse-engineered.)*

---

## 🎲 Long-Term Mega-Simulation Statistics
A 10,000 game simulation (1 God Bot, 2 Smart Bots, 3 Dumb Bots - completely randomized decks over 100,000 hands) yielded the definitive sustainability curve of the codebase:

| AI Tier | Metric Category | **Simulated Bust Rate** | Survival Concept |
| :--- | :--- | :--- | :--- |
| **Dumb (Level 0)** | *The Table Economy* | **90.65%** | Over-commits blindly |
| **Smart (Level 1)** | *The Tilty Aggressor* | **13.67%** | Survives most action, vulnerable to exploitation |
| **God (Level 2)** | *The Apex Predator* | **6.97%** | Only busts when mathematically correct draws inherently miss the deck |

---

## 🏆 Heads-Up Competitive Metrics (1v1 To The Death)
To verify the raw power gaps between bot tiers, a series of **30,000 matches** (10,000 per matchup) was conducted. Each match was played "Heads-Up" (1v1) starting with 1,000 chips each, playing until one player was completely busted.

| Matchup | Winner | **Win %** | Loser | **Win %** |
| :--- | :--- | :--- | :--- | :--- |
| **Dumb vs. God** | 🔴 God Bot | **95.97%** | 🟢 Dumb Bot | 4.03% |
| **Dumb vs. Smart** | 🔵 Smart Bot | **93.62%** | 🟢 Dumb Bot | 6.38% |
| **Smart vs. God** | 🔴 God Bot | **81.19%** | 🔵 Smart Bot | 18.81% |

### Key Takeaways:
- **The Variance Floor**: Even the most erratic bot (Level 0) manages a ~4-6% win rate due to the inherent luck involved in "All-In" showdowns.
- **The Soul-Reading Advantage**: The God Bot's **81.19% dominance** over the Smart Bot proves that structural exploitative code-reading is significantly more powerful than static heuristic strategies.

