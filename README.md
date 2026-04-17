# ♠️ Aventurine's Adventures: Java Casino Project

A comprehensive Java console-based casino application featuring fully functional **Texas Hold'em Poker** and **Blackjack** games.

## 🕹️ Project Overview

The project is structured as a modular suite of casino games managed by a central `Casino` engine. It implements complex game logic, including betting systems, bot AI, side pot management in poker, and inheritance-based player hierarchies.

### Core File Structure
- **Main.java**: The application entry point.
- **Casino.java**: The central hub that manages universal chip counts (Primogems ✨), user input, and navigation between games.
- **Player Hierarchy**: 
  - `Player`: Base class for name and chip management.
  - `PokerPlayer` & `BJPlayer`: Specialized subclasses for specific game requirements.
  - `PokerBot` & `BJBot`: AI-driven implementations of the player classes.

---

## 🃏 Poker Logic (Texas Hold'em)

The Poker engine is the most complex part of the project, spanning several specialized classes to handle the nuances of "No Limit" betting and hand evaluation.

### 1. Game Flow (`PokerGame.java`)
Manages the standard poker hand cycle:
- **Blinds**: Automatic Small and Big blind collection.
- **Betting Rounds**: Preflop, Flop (3 cards), Turn (1 card), River (1 card).
- **Orbits**: Tracks rotation so every player acts as the Dealer (D), Small Blind (SB), and Big Blind (BB).
- **Dynamic Players**: A randomized system where bots join or leave the table (7% chance per hand).
- **Blind Progression**: Increases blinds every 2-3 rounds depending on table size to keep the game moving.

### 2. Pot Management (`PokerPot.java`)
Handles the "Side Pot" logic, which is notoriously difficult to implement in poker software. 
- If a player goes **All-In**, the system creates a side pot.
- It tracks **Eligibility**: Only players who contributed to a specific side pot can win that pot.
- It calculates "thresholds" to split the total money gathered into distinct pots based on individual player stacks.

### 3. Hand Evaluation (`PokerDeck.java`)
A robust evaluation engine that handles:
- **Combinatorics**: Brute-forces all possible 5-card combinations from the 7 available (hole cards + board).
- **Rankings**: Correctly identifies Straight Flushes, Quads, Full Houses, etc.
- **Tie-Breaking**: Implements detailed comparison logic including "Kickers" (if two players have the same Pair, the next highest card determines the winner).

### 4. Bot AI (`PokerBot.java`)
Bots feature two modes:
- **Dumb Mode**: Plays based on fixed probabilities (e.g., 75% chance to call).
- **Smart Mode**: Evaluates its hand ranking and "drawing" potential (calculating if it's one card away from a straight or flush) to decide whether to bluff, fold, or raise.
- **Unique Names**: The `Names` class ensures that no two bots (or real players) share the same name at the table.

---

## 🂡 Blackjack Logic

A faithful implementation of the classic dealer-vs-player game.

### Logic Flow (`Blackjack.java`)
- **Initial Deal**: Two cards to both dealer and player; checks for natural 11-value cards (Blackjacks).
- **Player Actions**: Stand, Hit, or Surrender (recover 50% of the bet).
- **Dealer AI**: Implements the "Stay on 17" rule. The dealer must draw cards until their sum is at least 17.
- **Ace Handling**: The `getSum` method dynamically calculates the value of Aces (1 or 11) to maximize the hand without busting.

---

## 📈 Statistics & Utilities

### Statistics Tracking (`PlayerStat.java`)
The project maintains persistent stats selama a session:
- **Wins/Losses**: Counts and monetary values.
- **Efficiency**: Tracks "All-In" frequency and hands played.
- **Session Gains**: Aggregates primogems across Poker and Blackjack so that your balance carries over between tables.

### Utility System (`Utils.java` & `Deck.java`)
- **CLI Polish**: Uses ANSI escape codes for screen clearing and terminal delays.
- **Card Primitives**: The `Card` and `Deck` classes handle the fundamental shuffling and dealing logic shared by all games.

---

### 🛠️ Technical Highlights
1. **Recursion & State**: The game loop uses controlled recursion and `do-while` loops to handle turns without overflowing the stack.
2. **Abstracted Betting**: The betting system is decoupled from hand logic, allowing for complex scenarios like split pots in poker.
3. **Array Manipulation**: Extensive use of sorting and mode-finding algorithms to evaluate card rankings efficiently.
