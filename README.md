# ♠️ Aventurine's Adventures: Java Casino Project

A comprehensive Java console-based casino application featuring highly detailed implementations of **Texas Hold'em Poker** and **Blackjack**.

---

## 🕹️ Project Architecture

The system is built on a modular, inheritance-based framework that allows for seamless integration of different card games while sharing a universal "Primogem" ✨ economy.

### Core Systems
- **The Economy**: Players start with a user-defined buy-in between **500 and 1000** primogems. Chips are universal; your poker winnings can be taken immediately to the blackjack table.
- **The Casino Engine (`Casino.java`)**: Manages the high-level game loop, player name registration, and seat allocation.
- **Player Hierarchy**: 
  - `Player`: Handles name normalization and chip arithmetic.
  - `PokerPlayer` & `BJPlayer`: Extend specialized action methods for game-specific decisions.
  - `PokerBot` & `BJBot`: AI implementations that override human action prompts with algorithmic logic.

---

## 🃏 Texas Hold'em Poker Logic

The poker engine simulates a "No Limit" environment with complex pot management and hand evaluation.

### 1. Table Dynamics & Game Flow
- **Seat Capacity**: Supports **6 to 12 players**.
- **The Orbit System**: The game tracks dealer rotation. A "round" or "orbit" is complete when every player has acted as the dealer once.
- **Blinds Progression**: 
  - Initial Small Blind: **10** | Big Blind: **20**.
  - **$\le$ 8 players**: Blinds double every **3 rounds**.
  - **$\ge$ 9 players**: Blinds double every **2 rounds**.
  - Blinds cap at **320** (at which point the "Round finished" state is reached).
- **Randomized Traffic**: Every hand has a **7% chance** for a bot to randomly join or leave, simulating the atmosphere of a busy casino.

### 2. Pot & Side Pot Logic (`PokerPot.java`)
The system correctly handles the complex math of all-ins using a **Threshold Reconstruction** algorithm.
- If multiple players go all-in with different stack sizes, the system calculates "thresholds" representing the maximum contribution each player can make to a specific pot.
- It dynamically splits the money into a **Main Pot** and multiple **Side Pots**.
- **Eligibility Tracking**: Only players who were not "priced out" of a pot and are still in the hand are eligible to win it.

### 3. Hand Evaluation Engine (`PokerDeck.java`)
Uses a brute-force combinatorics approach ($C(7,5) = 21$ combinations) to find the absolute best 5-card hand out of the 7 available.
- **Ranking System (1-9)**:
  1. Straight Flush
  2. Four of a Kind
  3. Full House
  4. Flush
  5. Straight
  6. Three of a Kind
  7. Two Pair
  8. One Pair
  9. High Card
- **Tie-Breaking**: Implements full "Kicker" logic. If two players have identical ranks (e.g., both have 1 Pair of Kings), the code compares the highest remaining cards in their 5-card hand sequentially.

### 4. Bot AI Strategy (`PokerBot.java`)
Bots randomly spawn with one of two personalities:
#### **A. "Dumb" Mode (Probability Based)**
- **75%** chance to Call/Check.
- **10%** chance to Raise (randomized between minimum and 10% of stack).
- **12%** chance to Call or Fold depending on current bet.
- **3%** chance to go all-in regardless of hand strength.

#### **B. "Smart" Mode (Evaluative Logic)**
- **Preflop Ranges**: Only plays 32 specific "high-value" hand combinations (pocket pairs, high-suited connectors, etc.) unless the stack is deep.
- **Draw Detection**: Analyzes the board for "Straight Draws" (4 numerical cards) or "Flush Draws" (4 cards of same suit).
- **Rank-Based Betting**: 
  - **Quads/Straight Flushes**: 60% chance to bet 300%+ of the pot to extract value.
  - **Full Houses**: 45% chance to bet 150-300% of the pot.
  - **Straight/Flush**: 55% chance to bet 100-150% of the pot.

---

## 🂡 Blackjack Logic (`Blackjack.java`)

A faithful recreation of casino blackjack with professional house rules.

### Rules & Specifics
- **Minimum Bet**: **50 primogems** (or the player's remaining stack if below 50).
- **Payouts**:
  - **Natural Blackjack**: Pays **1.5x** the bet.
  - **Standard Win**: Pays **1x** the bet.
  - **Surrender**: Returns **0.5x** the bet to the player.
- **House Rules**: 
  - **Dealer "Stay on 17"**: The dealer (`BJBot`) must hit if their sum is $\le 16$ and must stand if it is $\ge 17$.
  - **Ties**: "Push" favors the dealer (house rules), except in the case of a natural blackjack tie where the bet is returned.
- **Ace Logic**: Dynamically evaluates Aces as **1 or 11**. The system calculates the sum with Aces as 11 first, and then converts them to 1 one-by-one if the total exceeds 21.

---

## 🛠️ Technical Implementation Details

- **Input Safety**: Uses `Player.getValidInt` and `Player.getValidStr` to prevent crashes from invalid user input or empty strings.
- **Visuals**: Employs `Utils.clearScreen()` (ANSI escape `\033[H\033[2J`) for a clean interface and `Utils.sleep()` for dramatic timing during card flips.
- **Persistence**: The `PlayerStat` class aggregates data using a `merge()` method, allowing the game to track long-term gains/losses even as you swap between Poker and Blackjack tables.
