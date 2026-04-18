# ♠️ Aventurine's Adventures: Java Casino Project

A comprehensive Java console-based casino application featuring high-fidelity implementations of **Texas Hold'em Poker** and **Blackjack**, powered by an industry-leading **God Bot AI** ecosystem.

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

The poker engine simulates a "No Limit" environment with complex pot management, hand evaluation, and predatory AI behavior.

### 1. Table Dynamics & Game Flow
- **Seat Capacity**: Supports **6 to 12 players**.
- **The Orbit System**: Dealer rotation is tracked via the orbit system. Blinds double every 2-3 orbits.
- **Skip Mode**: When only bots remain in a hand, human players can toggle **"Skip Mode"** to fast-forward the simulation and see the results instantly.
- **Bot Stickiness**: High-tier "God Bots" are immune to the random table-leaving logic if they are the last predator remaining, ensuring a persistent "Final Boss" at every table.

### 2. The Overlord AI Hierarchy (`PokerBot.java`)
Bots spawn with dynamically assigned intelligence levels, ranging from chaotic to mathematically perfect.

#### **🔴 Level 2: The God Bot (Heuristic Overlord)**
The pinnacle of the engine, designed for **100% win-rate dominance** over lower tiers.
- **Nuclear 1v1 Protocol**: In heads-up play, it executes 100% VPIP/PFR, raising every hand to bully opponents into submission.
- **Soul Reading**: Uses meta-code analysis to detect "Nuts-only" betting patterns from other AI.
- **Greed Protocol "Minus One"**: Detects calling stations and perfectly prices bets at `Target_Stack - 1` to bypass "All-In" fold logic.
- **Nut Blockers**: Intelligently shoves with the Nut Ace of a suit to block opponents from realizing their equity.

#### **🔵 Level 1: The Smart Bot (GTO Lite)**
- **Evaluative Logic**: Plays 32 high-value hand ranges.
- **Draw Awareness**: Calculates Straight/Flush draws and stays "sticky" in 1v1 pots.
- **Simplified Heuristics**: Detects Ace-high boards and executes 65-90% C-bet frequencies.

#### **🟢 Level 0: The Dumb Bot (Chaotic Neutral)**
- **Randomized Actions**: Primarily used as "liquidity providers" for higher-tier bots.
- **Fixed Frequencies**: 75% Call rate, 3% Blind-shove rate.

### 3. Pot & Side Pot Logic (`PokerPot.java`)
The system correctly handles the complex math of all-ins using a **Threshold Reconstruction** algorithm.
- Dynamically splits contributions into a **Main Pot** and multiple **Side Pots**.
- Tracks eligibility thresholds to ensure players can only win the portions of the pot they were not "priced out" of.

---

## 📈 The Poker Hegemony (Performance Metrics)

Verified through 10,000-game "Full-Street" simulations:

| Matchup | Winner | **Win %** | Loser | **Win %** |
| :--- | :--- | :--- | :--- | :--- |
| **Dumb vs. God** | 🔴 **God Bot** | **99.92%** | 🟢 Dumb Bot | **0.08%** |
| **Dumb vs. Smart** | 🔵 **Smart Bot** | **96.64%** | 🟢 Dumb Bot | **3.36%** |
| **Smart vs. God** | 🔴 **God Bot** | **92.42%** | 🔵 Smart Bot | **7.58%** |

---

## 🂡 Blackjack Logic (`Blackjack.java`)

A faithful recreation of casino blackjack with professional house rules.

### Rules & Specifics
- **Minimum Bet**: **50 primogems**.
- **Payouts**: Natural Blackjack pays **1.5x**; Standard wins pay **1x**.
- **Dealer Rules**: Mandatory "Stay on 17" for the `BJBot` house dealer.
- **Ace Logic**: Dynamically evaluates Aces as **1 or 11** to maximize the hand total without busting.

---

## 🛠️ Technical Details & QoL
- **Skip Mode**: Viewport-fast-forwarding for bot-only duels.
- **Input Resilience**: Total validation on all numeric and string inputs.
- **Persistence**: `PlayerStat` merging allows global tracking of "Aventurine's Adventures" across sessions.
- **Clean Dev Environment**: Custom `.gitignore` management for AI simulation scripts.

---
© 2026 Java Casino Project - Sammie Z & Edward J.
