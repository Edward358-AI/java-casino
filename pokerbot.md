# 🔱 PokerBot AI: The Technical Bible

This document details the GTO mathematics, exploitative hacks, and strategic matchups of the Java Casino AI hierarchy.

---

## 🏛️ AI Tier Hierarchy
*   **🟢 Level 0: The Dumb Bot (44%)** - Random-walk gambler.
*   **🔵 Level 1: The Smart Bot (44%)** - Linear heuristic grinder.
*   **🔴 Level 2: The God Bot (12%)** - GTO Predator.
*   **🌑 Nightmare Mode**: Triggered by user `edjiang1234`. Enforces 100% God Bot spawn rate with Two-Faced personalities.

---

## 🔴 Level 2: The God Bot (Apex Predator)
The God Bot is the final iteration of the "GTO Hardened" architecture (v3.0). It combines perfect mathematical defense with extreme exploitative aggression.

### 🛡️ Defensive Mechanics (Fortress Defense)
*   **Sticky Blind Shield**: Defends the Big Blind with ~45% of its hand range against raises <= 4.5x. It will call with any Ace, Pair, or Suited Connector to protect its territory.
*   **Positional Flatting**: In late positions, the God Bot "Flats" (calls) raises with pairs and suited wheel aces to see a flop and out-play opponents post-flop rather than folding.
*   **Iron Chin Resilience**: When facing a deep-stack "Bully" (opponent has >2x chips), the bot ignores "Scary Board" textures (flush/straight scares) and snap-calls shoves with as little as a Mid-Pair.

### 🗡️ Offensive Mechanics (GTO Hardening)
*   **Balanced Ranges**: UTG range includes all pairs and **Suited Wheel Aces (A2s-A5s)** to prevent being "priced out."
*   **Balanced 3-Betting**: Faces raises with a 35% "Light 3-bet" frequency, re-raising with marginal hands to keep opponents off-balance.
*   **Mixed Strategy Anomaly**: Explicit 15% chance to raise "GTO Gappers" (7-8s, 5-6s) from any position to ensure its range is mathematically un-solvable.

### 🧠 Split-Brain Contextual Logic
The God Bot adapts its "fearsomeness" based on the table's intelligence:
*   **Safe Accountant Mode**: Active if any Smart/Dumb bots are in the pot. The bot plays 0% bluffs, shuts down traps, and only value-hammers.
*   **Spicy Predator Mode**: Active ONLY against Humans or other Gods. Unlocks All-in bluffs, semi-bluffs, and monster trapping.

---

## ⚔️ The Strategic Battleground: Matchup Matrix

| Matchup | Winner (Win %) | The Winning Logic / Mechanic |
| :--- | :---: | :--- |
| **God vs. Dumb** | 🔴 **God (99.9%)** | **"Minus One" Exploit**: Prices overbets to exactly `Stack - 1` to bypass all-in fold logic. Uses "Nuclear Overlord" (100% VPIP) to exterminate the fish. |
| **God vs. Smart** | 🔴 **God (92.2%)** | **"Soul-Reading" Scanner**: Detects the Smart Bot's linear sizing to know their hand strength. Uses "Draw-Chase Taxation" to bleed them dry on boards with draws. |
| **God vs. God** | **Stalemate** | **Fortress Warfare**: Hands are won on thin margins. The winner is usually decided by variance tolerance and "Iron Chin" snap-calls during 3-bet wars. |
| **Smart vs. Human** | 🔵 **Smart (?)** | **The Chaos Factor**: Smart bots are "Unbluffable" because they chase draws in 50% of cases regardless of odds. They win by being accidental calling-stations. |
| **God vs. Human** | 🔴 **God (?)** | **Split-Brain Deception**: The bot assumes you are a Meta-God and hides its intent. It uses GTO math to minimize your edges and "Iron Chin" to punish your bluffs. |

---

## 🔬 Core Exploitative Hacks

### 1. Heuristic Sizing Scanner (Soul Reading)
Detects Smart Bot's linear mapping. If a Smart Bot bets `>= 2.6x`, the God Bot instantaneously concludes the Smart Bot has a Full House+ and folds, even if the God Bot has a Straight. It "reads the soul" of the code without breaking a single rule.

### 2. Nut Blockers
If the God Bot holds the Ace of a flush-suit, it will shove into a flush-scare board even if it has nothing. It knows you *cannot* have the nut-flush, so it leverages that mathematical certainty to steal the pot.

### 3. Big Stack Bully Protocol
If the God Bot has 1.5x your stack, it increases its bluff frequency by +15% and expands its value betting. It uses its chips as a weapon to "tax" your tournament survival instinct.

---
*This ecosystem was engineered for complete Hegemony. Enter at your own risk.*
