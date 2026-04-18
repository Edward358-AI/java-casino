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
    *   When faced with a bet that costs its entire stack, its logic is perfectly balanced: **It flips a coin**, calling 50% of the time and folding 50% of the time. This gives them a significantly higher lifespan than the pure gambler variant.

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
*   **GTO Hardening (Balanced Ranges)**: To eliminate range transparency, the God Bot no longer plays a purely tight early game.
    *   **Syncopated Early Range**: Adopts the Smart Bot's baseline (all pairs, wide premiums) but adds **Suited Wheel Aces (A2s-A9s)** to its Under-The-Gun (UTG) open-raise range. This prevents human players from "pricing out" the bot on low-card or wheel-straight boards.
    *   **15% Mixed Strategy Anomaly**: Explicitly introduces a 15% `rNG` chance to raise with "GTO Gappers" (suited connectors like 7-8s or 5-6s) from any position. This ensures the bot's range is mathematically "un-solvable" by human observation.
*   **Post-Flop Deception Suite**:
    *   **Aggressive Semi-Bluffing**: Identifies 4-to-a-flush or 4-to-a-straight draws. In 40% of cases, the bot will execute a lead-bet or aggressive check-raise rather than a passive call, maximizing its fold equity and "bluffing" with high outs.
    *   **Monster Trapping (The Slow Play)**: If the bot holds a hand of Rank 3 or lower (Full House+), it triggers a 20% "Check-Trap" frequency. It will check-call the flop or turn to induce "Maniac" bluffs from opponents, only unleashing its massive overbets on the river.
*   **Elite Board Awareness**: Scans aggressively for board textures (Paired boards, 4-Straights, 4-Flushes). If the board creates a massive scare and it only holds Top Pair, it performs defensive check-folds against big bets.
*   **Multi-Street Aggression**: Maintains memory of its actions. C-bets 90% of the time on Ace-High flops. If it raised the flop, it executes **Triple Barrelling** (firing 70% of the time on Turn face-cards) to bully opponents regardless of its actual hand.
*   **Nut Blockers**: If the God bot holds the Ace of a flush suit on board (but does not have the flush itself), it will intentionally shove its entire stack, blocking the opponent from holding the nuts.
*   **Humanoid Noise Injection**: When raising, the God Bot adds a random **+/- 5 chip offset** to its bets. This intentional "messiness" breaks predictable clean denominations (like 200 or 500) and mimics human imperfection to throw off professional opponents. (Automatically disabled during precision exploits like *Minus One*).

### Advanced Exploitation Suites
*   **Greed Protocol "Minus One" Exploit**: If `dumbBotCount > 0` and the God Bot holds a Full House or better, it abandons standard Value-Bet formulas. Instead of shoving All-In (which triggers the Dumb Bot's 50% "Coward" fold logic), the God Bot perfectly prices its massive overbet to exactly **`DumbBot_Stack - 1`**. This mathematically bypasses the All-In threshold and forces the Dumb Bot into its 75% flat-call loop, extracting absolute maximum tournament chips.
*   **Hyper-Thin Value Betting**: If a Dumb Bot is present, the God Bot expands its maximum value range from Full Houses down to Two Pairs (`rank <= 7`). Given that Dumb Bots call blindly, the God Bot mercilessly hammers Pot-Sized bets with marginal made hands, extracting value that GTO standards would normally check down.
*   **Absolute Bluff Eradication**: Because it is neurologically impossible to bluff a calling station, the God Bot tracks the `dumbBotCount` and drops its C-betting and barreling frequencies to absolute **0.0%** if it does not hold a made hand. Zero chips are ever risked as bluffs against Dumb Bots.
*   **Dynamic Stack-Depth Aggression (ESR)**: The God Bot actively tracks its **Effective Stack Ratio** against the largest active threat in the hand.
    *   **Big Stack Bully**: If it has 1.5x the chips of its nearest competitor, it widens its value betting (charging pot instead of 75%) and increases its baseline C-Bet/Barrel bluffs by +15% to leverage fold equity.
    *   **Short Stack Survival**: If it has less than 0.5x the chips of the leader, it drops all bluff/c-bet frequencies to an absolute 0.0%. It plays completely mathematically, ensuring it never risks tournament life on marginal draws or air.
*   **The "50%+1" Exploit**: Scans for Level 1 Smart Bots. Recognizes their innate weakness in the codebase (folding marginal draws to half-stack bets). It scientifically prices its bluffs to precisely `(SmartBot_Stack / 2) + 1` intentionally forcing the algorithm out of the pot.
*   **Level 3 Meta-Bluffing**: Uses recursive logic when facing other God Bots. Introduces a sudden 12% anomaly variant capable of executing massive polarized-overbets (1.5x - 2.5x pot) to break the other God Bot's strict math calculations.
*   **Heuristic Sizing Scanner (Soul Reading)**: Executes literal code-reading. The God Bot knows that Smart Bots structurally cannot bet above a 2.5x scale unless they hold the physical Nuts (Full House+). If a Smart Bot bets `>= 2.6x`, the God Bot instantaneously overrides its own math routines and folds any non-Nut hands, knowing with 100% algorithmic certainty that it is beat. (Note: This scanner is automatically disabled in Heads-Up situations where Smart Bots are expected to widen their ranges).
*   **Nuclear Overlord Protocol (1v1 Dominance)**: In isolated 1v1 scenarios against Dumb Bots, the God Bot switches from GTO to **Predatory Exploitation**.
    *   **100% VPIP / 100% PFR**: It never folds pre-flop. It raises every single hand to force immediate folds or maximum chip commitment from the fish.
    *   **100% C-Bet Frequency**: It fires into every flop regardless of hand strength, exploiting the Dumb Bot's fold frequency to steal the "Dead Money".
    *   **Iron Chin Policy**: It refuses to be bullied off Top-Pair or better, capturing 100% of the Dumb Bot's random air-shoves.
    *   **99.9% Win-Rate Efficiency**: Corrected chip math and aggressive value-betting ensure the Dumb Bot is deleted with surgical precision.

*(Note: God Bots treat human players (`PokerPlayer`) identically to Level 3 Meta-God Bots: they use standard GTO pot sizing, assume competence, and do not execute specific codebase hacks like Soul Reading, as Human intent cannot be reverse-engineered.)*

---

### 📊 Simulated Survival Metrics (20bb / 100 Hands)
The following table tracks the "Tournament Death March"—a 100-hand simulation across 10,000 games on a 20bb starting stack—representing the definitive performance of the current AI architecture:

| AI Tier | Metric Category | **Simulated Bust Rate** | Survival Concept |
| :--- | :--- | :--- | :--- |
| **Dumb (Level 0)** | *The Table Economy* | **72.27%** | Extreme vulnerability to God Bot aggression |
| **Smart (Level 1)** | *The Rational Grinder* | **0.48%** | Highly resilient but mathematically capped |
| **God (Level 2)** | *The Apex Predator* | **0.00%** | Absolute mathematical immortality |

---

## 🏆 Heads-Up Competitive Metrics (1v1 To The Death)
To verify the raw power gaps between bot tiers, a series of **30,000 matches** (10,000 per matchup) was conducted. Each match was played "Heads-Up" (1v1) starting with 1,000 chips each, playing until one player was completely busted.

| Matchup | Winner | **Win %** | Loser | **Win %** |
| :--- | :--- | :--- | :--- | :--- |
| **Dumb vs. God** | 🔴 **God Bot** | **99.96%** | 🟢 Dumb Bot | **0.04%** |
| **Dumb vs. Smart** | 🔵 **Smart Bot** | **97.29%** | 🟢 Dumb Bot | **2.71%** |
| **Smart vs. God** | 🔴 **God Bot** | **90.45%** | 🔵 Smart Bot | **9.55%** |

### 👑 The "Nuclear Hegemony" Fix
Following advanced testing and simulation re-calibration, the God Bot has been upgraded to **Nuclear Overlord** status.
*   **The Problem**: Previously, elite bots were surrendering blinds in 1v1 due to over-tight ranges.
*   **The Fix**: God Bots now execute 100% blind-defense and relentless post-flop aggression in 1v1 duels. This has pushed the win-rate to the physical limit of the game (99.9%).
*   **God vs. Smart Outcome**: The God Bot now maintains a **90.45% dominance** over the Smart Bot when isolated. Even with its wider, "Balanced" pre-flop range and humanoid betting noise, the God Bot's superior deceptive logic (Traps and Semi-Bluffs) ensures it remains the definitive final boss of the system.

---

## ⚔️ Strategic Battlegrounds: 1v1 Matchup Breakdown

This section details the specific algorithmic interactions and "Hacks" that allow each tier to dominate its respective matchups in a 1v1 environment.

### 1. 🔴 God Bot vs. 🔵 Smart Bot (90.45% Dominance)
The God Bot dominates the Smart Bot by treating its code as an "Open Book." It ignores psychological games and focuses on two brutal technical exploits:
*   **Heuristic Sizing Scanner (Soul Reading)**: The God Bot recognizes that the Smart Bot is **Linear**. It knows that a Smart Bot *never* bets >= 2.6x unless it holds a Full House or better. The God Bot uses this sizing trigger to perform "Perfect Folds," saving its stack whenever the Smart Bot stumbles into a monster.
*   **Draw-Chase Taxation**: The God Bot identifies when a board has flush/straight possibilities. It knows the Smart Bot will chase any draw with a 50% frequency regardless of price. The God Bot intentionally over-bets these boards, charging the Smart Bot a massive "Math Tax" that drains its stack over time.

### 2. 🔴 God Bot vs. 🟢 Dumb Bot (99.96% Dominance)
In this matchup, the God Bot abandons GTO safety and switches to **"Predatory Exploitation"** once isolated:
*   **Predatory Bluffing (1v1 Only)**: When heads-up against a Dumb Bot, the God Bot spikes its C-bet frequency to **60%** (up from **0%** in multi-way pots). It knows the Dumb Bot folds "Air" enough to make this profitable in isolation, but it maintains **0% bluffs** in multi-way pots to avoid bleeding chips to calling stations.
*   **The "Minus One" Exploit**: The God Bot bypasses the Dumb Bot's 50% all-in fold logic. It prices its value-bets to exactly `Opponent_Stack - 1`. This forces the Dumb Bot into its 75% "Flat Call" loop, ensuring the God Bot extracts 99.9% of the stack without ever triggering a coin-flip fold.

### 3. 🔵 Smart Bot vs. 🟢 Dumb Bot (97.29% Dominance)
The Smart Bot wins through **Disciplined Value-Targeting**. While it lacks the "Hacks" of the God Bot, it possesses a massive edge over the Dumb Bot through simple consistency:
*   **Linear Value Betting**: The Smart Bot only bets when it hits its charts (Top Pair+). Because the Dumb Bot is a "Calling Station" that never folds, the Smart Bot simply waits to hit a better hand and bets it for value.
*   **Chaos Immunity**: The Dumb Bot's random actions (like random all-ins) often confuse humans, but the Smart Bot is immune to this "Chaos." It follows its threshold charts; if its cards are good, it calls. If not, it folds. This lack of emotion allows it to eventually capture the Dumb Bot's stack through superior card-selection alone.


