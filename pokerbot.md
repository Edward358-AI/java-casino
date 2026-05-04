# God Bot Architecture: Technical Rundown

The God Bot is a tiered decision engine that layers human-like "gut feelings" (Exploitative Logic) on top of a "Pure Math" (GTO) foundation.

## 1. The Three Operating Modes
The bot's behavior is dictated by how you configure the `protectedMode` and `neuralProtectedMode` flags in `PokerSimulator.java`.

| Mode | Configuration | Behavior |
| :--- | :--- | :--- |
| **Pure GTO** | `Protected: ON`, `Neural: OFF` | **The Mathematician.** Ignores all telemetry. Plays purely based on equity, pot odds, and board texture. Unbeatable in a long-term vacuum but never exploits opponent mistakes. |
| **Neural Sandbox** | `Protected: ON`, `Neural: ON` | **The Profiler.** Tracks archetypes (NIT, TAG, etc.) to adjust its ranges, but keeps "GTO safety rails" active. It will value bet thinner against a station, but won't commit "suicide bluffs." |
| **Nightmare Mode** | `Protected: OFF` | **The Predator.** Full exploitative logic enabled. Uses "Nightmare Intensity" (1 or 2) and "Predatory Intent" (Aggressive bluffs). This is the "Spicy" version that tries to "soul-read" opponents. |

---

## 2. The Two Tracking Layers (The "Eyes")
The bot uses two different specialized profiles to watch opponents.

### Layer A: The Cognitive Matrix (Profile-Based)
*   **Purpose:** Categorizes you into a playstyle (Archetype).
*   **Alpha ($\alpha$):** **0.35** (Very fast; reacts to the last ~5 hands).
*   **Triggers:** If your `vIndex` (volatility) spikes, you are instantly tagged as **ELITE_REG**.
*   **Usage:** Determines global strategies (e.g., "Always bluff this guy" or "Never slow-play against this guy").

### Layer B: Smart Leak Detection (Action-Based)
*   **Purpose:** Only active in Heads-Up pots against Smart/God bots.
*   **Alpha ($\alpha$):** **0.30** (Slightly smoother).
*   **Usage:** Tracks specific mathematical leaks like "How often does he fold to 3-bets?" or "Does he check back the turn too often?".

---

## 3. The Decision Engine (The "Brain")
When it's the God Bot's turn to act, it follows this priority list:

1.  **Blocker Check:** Does it hold an "Ace of Spades" on a 3-spade board? If so, it might bluff regardless of its hand rank.
2.  **Archetype Override:**
    *   **vs. NIT:** Jack up bluff frequency to 100%.
    *   **vs. MANIAC:** Jack up "Trap" frequency; stop bluffing and wait for the Maniac to spew chips.
    *   **vs. STATION:** Stop bluffing entirely; only bet for value.
3.  **GTO Baseline:** If no strong exploit is found, it calculates `Equity vs. Pot Odds`.
4.  **Humanoid Noise:** In non-protected mode, it adds +/- 5 chips to its bets to look less like a robot.

---

## 4. Current Design "Gotchas"
As we discovered in your diagnostics:
*   **The 3-Hand Wall:** The bot is "blind" for the first 2 hands of a simulation. It assumes everyone is `UNKNOWN` until Hand 3.
*   **Flicker Sensitivity:** Because the alphais high (0.35), a single fold can make the bot think a TAG is a NIT.
*   **GTO is the Ceiling:** In Duel Mode, the "Pure GTO" bot often wins because it doesn't make the "Human" mistakes that the "Exploitative" bot makes when it tries too hard to be clever.
