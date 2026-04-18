import java.util.*;

public class PokerSimulator {
    private static Scanner sc = Player.sc;
    private static int dumbCount, smartCount, godCount;
    private static boolean isProtectedMode = false;

    public static void main(String[] args) {
        setup();
        while (true) {
            System.out.println("\n--- POKER SIMULATOR MENU ---");
            System.out.println("[1] Simulate 1 Hand (Interactive)");
            System.out.println("[2] Simulate 1 Game (Standard)");
            System.out.println("[3] Simulate N Hands (Replacement Mode)");
            System.out.println("[4] Simulate N Games (Macro Sim)");
            System.out.println("[5] Simulate N Games (Macro Replacement Sim)");
            System.out.println("[6] Simulate N Duplicate Pairs (Duplicate Duel)");
            System.out.println("[7] Edit Game Setup");
            System.out.println("[8] Exit");
            
            int choice = Player.getValidInt("Choice: ", 1, 8);
            if (choice == 8) break;
            
            switch (choice) {
                case 1: runOneHand(); break;
                case 2: runOneGame(); break;
                case 3: runNHands(); break;
                case 4: runNGames(); break;
                case 5: runMacroReplacement(); break;
                case 6: runDuplicateDuel(); break;
                case 7: setup(); break;
            }
        }
    }

    private static void setup() {
        System.out.println("\n--- GAME SETUP ---");
        dumbCount = Player.getValidInt("How many Dumb Bots? ", 0, 12);
        smartCount = Player.getValidInt("How many Smart Bots? ", 0, 12 - dumbCount);
        godCount = Player.getValidInt("How many God Bots? ", 0, 12 - dumbCount - smartCount);
        
        System.out.print("Enable Protected Mode for all bots? (Strips exploits) [y/N]: ");
        String pIn = sc.nextLine();
        isProtectedMode = pIn.equalsIgnoreCase("y");

        if (dumbCount + smartCount + godCount < 2) {
            System.out.println("Wait, you need at least 2 bots to play!");
            setup();
        }
    }

    private static void runOneHand() {
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, true, isProtectedMode);
        engine.runHand(1, true);
    }

    private static void runOneGame() {
        int maxHands = Player.getValidInt("Max hands for this game? (Enter for Last Man Standing)", 1, 1000000, false, true);
        if (maxHands <= 0) maxHands = Integer.MAX_VALUE;
        
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode);
        engine.runGame(maxHands, true);
    }

    private static void runNHands() {
        int n = Player.getValidInt("How many hands to simulate?", 1, 1000000);
        if (n <= 0) n = Integer.MAX_VALUE;
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode);
        engine.runContinuous(n);
    }

    private static void runNGames() {
        int n = Player.getValidInt("How many games to simulate?", 1, 1000000);
        if (n <= 0) n = 1; // Safeguard
        
        int maxHands = Player.getValidInt("Max hands per game? (Enter for Last Man Standing)", 1, 1000000, false, true);
        if (maxHands <= 0) maxHands = Integer.MAX_VALUE;
        
        int[] gameWins = new int[3]; // D, S, G
        int[] gameBusts = new int[3]; 
        long[] totalNetGold = new long[3];

        for (int i = 0; i < n; i++) {
            SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode);
            int winnerLevel = engine.runGame(maxHands, false);
            System.out.println("Game #" + (i + 1) + " Winner: " + (winnerLevel == 0 ? "Dumb" : (winnerLevel == 1 ? "Smart" : "God")));
            if (winnerLevel != -1) gameWins[winnerLevel]++;
            
            int[] busts = engine.getBusts();
            gameBusts[0] += busts[0]; gameBusts[1] += busts[1]; gameBusts[2] += busts[2];
            
            long[] profits = engine.getTierNetGold();
            totalNetGold[0] += profits[0]; totalNetGold[1] += profits[1]; totalNetGold[2] += profits[2];
        }

        System.out.println("\n--- MACRO SIMULATION RESULTS (" + n + " games) ---");
        int totalGames = n;
        System.out.printf("Win Rates   -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%\n", 
            (gameWins[0]*100.0/totalGames), (gameWins[1]*100.0/totalGames), (gameWins[2]*100.0/totalGames));
        System.out.printf("Bust Freq   -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G\n",
            (double)gameBusts[0]/totalGames, (double)gameBusts[1]/totalGames, (double)gameBusts[2]/totalGames);
        System.out.printf("Net Gold    -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
            (totalNetGold[0]>=0?"+":""), totalNetGold[0], (totalNetGold[1]>=0?"+":""), totalNetGold[1], (totalNetGold[2]>=0?"+":""), totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n", (double)(gameBusts[0]+gameBusts[1]+gameBusts[2])/totalGames);
    }

    private static void runMacroReplacement() {
        int n = Player.getValidInt("How many replacement games to simulate?", 1, 1000000);
        int handsPerGame = Player.getValidInt("Hands per game?", 1, 1000000);
        
        int[] gameWins = new int[3]; // Tier with most profit in session
        int[] gameBusts = new int[3]; 
        long[] totalNetGold = new long[3];

        for (int i = 0; i < n; i++) {
            SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode);
            engine.runContinuous(handsPerGame, false);
            
            int winnerLevel = engine.getWinningTierByProfit();
            System.out.println("Replacement Game #" + (i + 1) + " Most Profitable: " + (winnerLevel == 0 ? "Dumb" : (winnerLevel == 1 ? "Smart" : "God")));
            gameWins[winnerLevel]++;
            
            int[] busts = engine.getBusts();
            gameBusts[0] += busts[0]; gameBusts[1] += busts[1]; gameBusts[2] += busts[2];
            
            long[] profits = engine.getTierNetGold();
            totalNetGold[0] += profits[0]; totalNetGold[1] += profits[1]; totalNetGold[2] += profits[2];
        }

        System.out.println("\n--- MACRO REPLACEMENT RESULTS (" + n + " games, " + handsPerGame + " hands/ea) ---");
        int totalGames = n;
        System.out.printf("Session Win Rates (By Profit) -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%\n", 
            (gameWins[0]*100.0/totalGames), (gameWins[1]*100.0/totalGames), (gameWins[2]*100.0/totalGames));
        System.out.printf("Bust Freq -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G\n",
            (double)gameBusts[0]/totalGames, (double)gameBusts[1]/totalGames, (double)gameBusts[2]/totalGames);
        System.out.printf("Total Net Gold -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
            (totalNetGold[0]>=0?"+":""), totalNetGold[0], (totalNetGold[1]>=0?"+":""), totalNetGold[1], (totalNetGold[2]>=0?"+":""), totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n", (double)(gameBusts[0]+gameBusts[1]+gameBusts[2])/totalGames);
        if (isProtectedMode) System.out.println("Note: Simulation run in PROTECTED MODE (Exploits disabled).");
    }

    private static void runDuplicateDuel() {
        System.out.println("\n--- DUPLICATE DUEL: THE ULTIMATE SKILL TEST ---");
        System.out.println("Select two bot tiers to compete (0:Dumb, 1:Smart, 2:God)");
        int t1 = Player.getValidInt("Tier for Bot A: ", 0, 2);
        int t2 = Player.getValidInt("Tier for Bot B: ", 0, 2);
        int pairs = Player.getValidInt("Number of duplicate pairs to simulate?", 1, 1000000);

        long t1TotalGold = 0;
        long t2TotalGold = 0;
        int t1Busts = 0, t2Busts = 0;
        int t1Wins = 0, t2Wins = 0;

        int pairCount = 0;
        
        System.out.println("Executing " + pairs + " duplicate pairs...");
        
        while (pairCount < pairs) {
            pairCount++;
            SimEngine engine = new SimEngine(new int[]{t1, t2}, false, isProtectedMode);
            
            for(PokerBot b : engine.bots) {
                b.removeChips(b.getChips());
                b.addChips(400);
            }

            PokerDeck tempDeck = new PokerDeck();
            Card[][] holeCards = tempDeck.deal(2);
            Card[] board = tempDeck.deal();
            
            // Pass 1: Bot A at 0, Bot B at 1
            int winner1 = engine.runHand(1, false, false, holeCards, board);
            t1TotalGold += (engine.bots.get(0).getChips() - 400);
            t2TotalGold += (engine.bots.get(1).getChips() - 400);
            if (engine.bots.get(0).getChips() == 0) t1Busts++;
            if (engine.bots.get(1).getChips() == 0) t2Busts++;
            if (winner1 == 0) t1Wins++; else if (winner1 == 1) t2Wins++;
            else if (winner1 == -1) { t1Wins++; t2Wins++; }

            // Pass 2: RESET AND SWAP PHYSICALLY
            // Swap Bot A and Bot B in the bots list to cancel any Slot-0 bias
            PokerBot tempBot = engine.bots.get(0);
            engine.bots.set(0, engine.bots.get(1));
            engine.bots.set(1, tempBot);

            for(PokerBot b : engine.bots) {
                b.removeChips(b.getChips());
                b.addChips(400);
            }
            Card[][] swappedHole = new Card[2][2];
            swappedHole[0] = holeCards[1];
            swappedHole[1] = holeCards[0];

            // Use id=2 to swap SB and BB roles
            int winner2 = engine.runHand(2, false, false, swappedHole, board);
            
            // Note: In Pass 2, engine.bots.get(0) is now Bot B, and bots.get(1) is now Bot A
            t2TotalGold += (engine.bots.get(0).getChips() - 400);
            t1TotalGold += (engine.bots.get(1).getChips() - 400);
            if (engine.bots.get(0).getChips() == 0) t2Busts++;
            if (engine.bots.get(1).getChips() == 0) t1Busts++;
            if (winner2 == 0) t2Wins++; else if (winner2 == 1) t1Wins++;
            else if (winner2 == -1) { t1Wins++; t2Wins++; }
        }

        String name1 = (t1==0?"Dumb":(t1==1?"Smart":"God"));
        String name2 = (t2==0?"Dumb":(t2==1?"Smart":"God"));
        
        System.out.println("\n--- DUPLICATE DUEL RESULTS (" + pairs + " pairs) ---");
        System.out.println("Bot A [" + name1 + "] vs Bot B [" + name2 + "]");
        int totalHands = pairs * 2;
        System.out.printf("Bot A [%s] -> Win Rate: %.1f%% (%d) | Bust Rate: %.2f%% (%d) | Net Advantage: %s✨%d\n", 
            name1, (t1Wins*100.0/totalHands), t1Wins, (t1Busts*100.0/totalHands), t1Busts, (t1TotalGold >= 0 ? "+" : ""), t1TotalGold);
        System.out.printf("Bot B [%s] -> Win Rate: %.1f%% (%d) | Bust Rate: %.2f%% (%d) | Net Advantage: %s✨%d\n", 
            name2, (t2Wins*100.0/totalHands), t2Wins, (t2Busts*100.0/totalHands), t2Busts, (t2TotalGold >= 0 ? "+" : ""), t2TotalGold);
        
        long diff = t1TotalGold - t2TotalGold;
        if (diff > 0) {
            System.out.println("\n🏆 SKILL LEADER: Bot A [" + name1 + "] outperformed Bot B [" + name2 + "] by ✨" + diff);
        } else if (diff < 0) {
            System.out.println("\n🏆 SKILL LEADER: Bot B [" + name2 + "] outperformed Bot A [" + name1 + "] by ✨" + (-diff));
        } else {
            System.out.println("\n🤝 DRAW: Perfect parity across both sessions.");
        }
        if (isProtectedMode) System.out.println("Note: Duel conducted in PROTECTED MODE (Exploits disabled).");
    }

    // --- ENGINE CLASSES ---

    static class SimEngine {
        List<PokerBot> bots = new ArrayList<>();
        private int dumbCount, smartCount, godCount;
        private int handCount = 0;
        private int[] bustsByTier = new int[3];
        private int[] winsByTier = new int[3];
        private boolean interactive;
        private boolean replacementMode = false;
        private boolean protectedMode = false;
        private PokerDeck deck = new PokerDeck();
        private boolean shouldShuffle = true;
        private int blinds = 20;
        private int[] duelLevels = null;

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode) {
            this(d, s, g, interactive, protectedMode, true);
        }

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode, boolean shouldShuffle) {
            this.dumbCount = d; this.smartCount = s; this.godCount = g;
            this.interactive = interactive;
            this.protectedMode = protectedMode;
            this.shouldShuffle = shouldShuffle;
            initializeBots();
        }

        public SimEngine(int[] levels, boolean interactive, boolean protectedMode) {
            this.duelLevels = levels;
            this.dumbCount = 0; this.smartCount = 0; this.godCount = 0;
            for (int l : levels) {
                if (l == 0) dumbCount++; else if (l == 1) smartCount++; else if (l == 2) godCount++;
            }
            this.interactive = interactive;
            this.protectedMode = protectedMode;
            this.shouldShuffle = false;
            initializeBots();
        }

        private void initializeBots() {
            bots.clear();
            if (duelLevels != null) {
                for (int level : duelLevels) createBot(level);
            } else {
                for (int i = 0; i < dumbCount; i++) createBot(0);
                for (int i = 0; i < smartCount; i++) createBot(1);
                for (int i = 0; i < godCount; i++) createBot(2);
            }
            if (shouldShuffle) Collections.shuffle(bots);
        }

        private void createBot(int level) {
            // Passing a dummy player named "edj" to the constructor to force-reveal tags for the simulator
            PokerPlayer[] unlocker = { new PokerPlayer("edj") };
            PokerBot b = new PokerBot(unlocker);
            b.setBotLevel(level);
            b.setProtectedMode(protectedMode);
            b.refreshNameTag(unlocker); // Force refresh with the unlocker
            b.removeChips(b.getChips());
            b.addChips(400); // 20bb
            bots.add(b);
        }

        public int[] getBusts() { return bustsByTier; }

        public long[] getTierNetGold() {
            long[] netGold = new long[3];
            for (PokerBot b : bots) {
                netGold[b.getBotLevel()] += b.getChips();
            }
            
            // Standard: Only subtract initial buy-in. 
            // Replacement: Subtract initial + every re-buy (bust).
            long dCost = dumbCount * 400L;
            long sCost = smartCount * 400L;
            long gCost = godCount * 400L;

            if (replacementMode) {
                dCost += bustsByTier[0] * 400L;
                sCost += bustsByTier[1] * 400L;
                gCost += bustsByTier[2] * 400L;
            }

            netGold[0] -= dCost;
            netGold[1] -= sCost;
            netGold[2] -= gCost;
            return netGold;
        }

        public int getWinningTierByProfit() {
            long[] net = getTierNetGold();
            int best = 0;
            if (net[1] > net[0]) best = 1;
            if (net[2] > net[best]) best = 2;
            return best;
        }

        public int runGame(int maxHands, boolean verbose) {
            while (handCount < maxHands) {
                int alive = 0;
                PokerBot winner = null;
                for (PokerBot b : bots) {
                    if (b.getChips() > 0) {
                        alive++;
                        winner = b;
                    }
                }
                if (alive <= 1) {
                    if (verbose) {
                        System.out.println("\n🏆 WINNER: " + (winner != null ? winner.getName() : "None") + " takes the game!");
                        printFinalReport(winner, false);
                    }
                    return (winner != null) ? winner.getBotLevel() : -1;
                }

                handCount++;
                runHand(handCount, verbose);
            }
            if (verbose) {
                System.out.println("\n⏰ Max hands reached!");
                printFinalReport(null, false);
            }
            return -1;
        }

        public void runContinuous(int n) {
            runContinuous(n, true);
        }

        public void runContinuous(int n, boolean verbose) {
            this.replacementMode = true;
            while (handCount < n) {
                handCount++;
                runHand(handCount, verbose, true);
            }
            if (verbose) printFinalReport(null, true);
        }

        private void printFinalReport(PokerBot overallWinner, boolean showBusts) {
            System.out.println("\n--- FINAL TELEMETRY REPORT ---");
            if (overallWinner != null) {
                System.out.println("Winner: " + overallWinner.getName() + " [" + getTierCode(overallWinner.getBotLevel()) + "]");
            }
            System.out.println("Total Hands Processed: " + handCount);
            
            if (showBusts && handCount > 0) {
                System.out.printf("Bust Rates -> Dumb: %d (%.2f%%), Smart: %d (%.2f%%), God: %d (%.2f%%)\n", 
                    bustsByTier[0], (bustsByTier[0]*100.0/handCount),
                    bustsByTier[1], (bustsByTier[1]*100.0/handCount),
                    bustsByTier[2], (bustsByTier[2]*100.0/handCount));
                
                long[] net = getTierNetGold();
                System.out.printf("Net Gold   -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
                    (net[0]>=0?"+":""), net[0], (net[1]>=0?"+":""), net[1], (net[2]>=0?"+":""), net[2]);
            }
            
            System.out.println("\nFinal Stacks:");
            for (PokerBot b : bots) {
                System.out.println(b.getName() + ": ✨" + b.getChips());
            }

            int totalWins = winsByTier[0] + winsByTier[1] + winsByTier[2];
            if (totalWins > 0) {
                System.out.printf("Win Rate %% (By Hands Won): God (%.1f%%), Smart (%.1f%%), Dumb (%.1f%%)\n",
                    (winsByTier[2]*100.0/totalWins), (winsByTier[1]*100.0/totalWins), (winsByTier[0]*100.0/totalWins));
            }
        }

        public int runHand(int id, boolean verbose) {
            return runHand(id, verbose, false, null, null);
        }

        public int runHand(int id, boolean verbose, boolean replacement) {
            return runHand(id, verbose, replacement, null, null);
        }

        public int runHand(int id, boolean verbose, boolean replacement, Card[][] fixedHole, Card[] fixedBoard) {
            if (fixedHole == null) {
                deck.reset();
            }
            int numPlayers = bots.size();
            PokerPlayer[] participants = bots.toArray(new PokerPlayer[0]);
            PokerPot pot = new PokerPot(participants);
            
            boolean[] folded = new boolean[numPlayers];
            int[] contributions = new int[numPlayers];
            
            for (int i = 0; i < numPlayers; i++) {
                bots.get(i).setInHand(bots.get(i).getChips() > 0);
                folded[i] = (bots.get(i).getChips() <= 0);
            }

            Card[][] holeCards = (fixedHole != null) ? fixedHole : deck.deal(numPlayers);
            for (int i = 0; i < numPlayers; i++) bots.get(i).setHand(holeCards[i]);

            int sbIdx = (id - 1) % numPlayers;
            int safety = 0;
            while (bots.get(sbIdx).getChips() <= 0 && safety++ < numPlayers) sbIdx = (sbIdx + 1) % numPlayers;
            int bbIdx = id % numPlayers;
            safety = 0;
            while ((bots.get(bbIdx).getChips() <= 0 || bbIdx == sbIdx) && safety++ < numPlayers) bbIdx = (bbIdx + 1) % numPlayers;
            
            int preflopAggressorIndex = -1;

            payBlind(sbIdx, blinds/2, contributions, pot);
            payBlind(bbIdx, blinds, contributions, pot);

            String[] streetNames = {"preflop", "flop", "turn", "river"};
            Card[] board = new Card[0];
            int currentBet = blinds;
            int lastRaise = blinds;

            for (String street : streetNames) {
                if (!street.equals("preflop")) {
                    if (street.equals("flop")) {
                        if (fixedBoard == null) deck.deal();
                        board = new Card[3];
                        Card[] actualBoard = (fixedBoard != null) ? fixedBoard : deck.getBoard();
                        System.arraycopy(actualBoard, 0, board, 0, 3);
                    } else if (street.equals("turn")) {
                        board = new Card[4];
                        Card[] actualBoard = (fixedBoard != null) ? fixedBoard : deck.getBoard();
                        System.arraycopy(actualBoard, 0, board, 0, 4);
                    } else if (street.equals("river")) {
                        board = new Card[5];
                        Card[] actualBoard = (fixedBoard != null) ? fixedBoard : deck.getBoard();
                        System.arraycopy(actualBoard, 0, board, 0, 5);
                    }
                    currentBet = 0; lastRaise = blinds; 
                    for (int i=0; i<numPlayers; i++) contributions[i] = 0;
                }
                if (countActive(folded) <= 1) break;

                if (interactive) {
                    System.out.println("\n--- " + street.toUpperCase() + " ---");
                    if (board.length > 0) {
                        System.out.print("Board: ");
                        for (Card c : board) System.out.print(c.getValue() + " ");
                        System.out.println();
                    }
                }

                boolean roundDone = false;
                int startPlayer;
                if (numPlayers == 2) {
                    startPlayer = (street.equals("preflop")) ? sbIdx : bbIdx;
                } else {
                    startPlayer = (street.equals("preflop")) ? (bbIdx + 1) % numPlayers : (sbIdx + 1) % numPlayers;
                }

                int i = startPlayer;
                int lastAggressor = -1;
                int playersActed = 0;

                while (!roundDone) {
                    if (!folded[i] && bots.get(i).getChips() > 0) {
                        PokerBot actor = bots.get(i);
                        // FIXED: Pass the actual preflopAggressorIndex
                        int[] action = actor.action(street, contributions[i], currentBet, blinds, lastRaise, board, pot.getTotalPot(), participants, i, preflopAggressorIndex, sbIdx, bbIdx);
                        
                        if (action[0] == 2) {
                            folded[i] = true;
                            if (interactive) System.out.println(actor.getName() + ": FOLDS");
                        } else {
                            int totalContribution = (action[0] == 1) ? (currentBet - contributions[i]) : action[1];
                            int paid = Math.min(actor.getChips(), totalContribution);
                            contributions[i] += paid;
                            pot.addPlayerContribution(i, paid);
                            
                            if (interactive) {
                                String actionVerb = (action[0] == 4 || actor.getChips() == 0) ? "ALL-IN ✨" : (currentBet == 0 ? "BETS ✨" : "RAISES to ✨");
                                if (action[0] == 1) {
                                    System.out.println(actor.getName() + ": " + (paid == 0 && currentBet == 0 ? "CHECKS" : "CALLS ✨" + contributions[i]));
                                } else {
                                    System.out.println(actor.getName() + ": " + actionVerb + contributions[i]);
                                }
                            }

                            if (contributions[i] > currentBet) {
                                int increment = contributions[i] - currentBet;
                                if (increment >= lastRaise) {
                                    lastRaise = increment;
                                }
                                currentBet = contributions[i];
                                lastAggressor = i;
                                playersActed = 0; // RESET Action counter on raise
                                if (street.equals("preflop")) preflopAggressorIndex = i; // Track for C-bets
                            }
                        }
                    }
                    playersActed++;
                    i = (i + 1) % numPlayers;

                    if (countActive(folded) <= 1) break;
                    
                    // Betting Round End Condition: 
                    // 1. Everyone has had a chance to act (playersActed >= numPlayers)
                    // 2. The next player to act has already matched the current bet (or is out/all-in)
                    if (playersActed >= numPlayers) {
                        if (lastAggressor == -1 || contributions[i] == currentBet || bots.get(i).getChips() == 0 || folded[i]) {
                            roundDone = true;
                        }
                    }
                }
                if (interactive) {
                    System.out.println("[Enter to continue, or 'skip' for showdown]");
                    String in = sc.nextLine();
                    if (in.equalsIgnoreCase("skip")) { interactive = false; }
                }
            }

            int winnerIdx = -1;
            int winAmount = pot.getTotalPot();
            String winningHand = "N/A";

            if (countActive(folded) == 1) {
                for (int i=0; i<numPlayers; i++) if (!folded[i]) winnerIdx = i;
                bots.get(winnerIdx).addChips(winAmount);
                winningHand = "FOLD-OUT";
            } else {
                List<Integer> winnerIndices = new ArrayList<>();
                int bestRank = 10;
                Card[] absoluteBest = new Card[5];
                
                for (int i=0; i<numPlayers; i++) {
                    if (!folded[i]) {
                        Card[] best = deck.getBestHand(bots.get(i).getHand(), board);
                        int rank = deck.getRanking(best);
                        
                        if (rank < bestRank) {
                            bestRank = rank; 
                            winnerIndices.clear();
                            winnerIndices.add(i);
                            absoluteBest = best;
                            winningHand = getRankingName(rank);
                        } else if (rank == bestRank) {
                            if (winnerIndices.isEmpty()) {
                                winnerIndices.add(i);
                                absoluteBest = best;
                            } else {
                                int comparison = deck.compareHands(absoluteBest, best);
                                if (comparison == 2) { // 'best' is better
                                    winnerIndices.clear();
                                    winnerIndices.add(i);
                                    absoluteBest = best;
                                    winningHand = getRankingName(rank);
                                } else if (comparison == 0) { // Exact tie
                                    winnerIndices.add(i);
                                }
                            }
                        }
                    }
                }
                
                if (!winnerIndices.isEmpty()) {
                    int share = winAmount / winnerIndices.size();
                    int remainder = winAmount % winnerIndices.size();
                    
                    // Standard Tie-Breaker: Award remainder chips to players in order of index (earliest position)
                    // Randomize the starting winner to prevent persistent Slot 0 bias
                    int startWinner = (int)(Math.random() * winnerIndices.size());
                    for (int k = 0; k < winnerIndices.size(); k++) {
                        int winnerIdxInList = (startWinner + k) % winnerIndices.size();
                        int idx = winnerIndices.get(winnerIdxInList);
                        
                        int amount = share + (remainder > 0 ? 1 : 0);
                        if (remainder > 0) remainder--;
                        
                        bots.get(idx).addChips(amount);
                        winsByTier[bots.get(idx).getBotLevel()]++;
                    }
                    winnerIdx = (winnerIndices.size() > 1) ? -1 : winnerIndices.get(0); // Return -1 for ties
                }
            }

            if (winnerIdx != -1 && countActive(folded) == 1) {
                winsByTier[bots.get(winnerIdx).getBotLevel()]++;
            }
                
            // Always report if hand finished (including ties)
            String rep = "";
            for (int i=0; i<numPlayers; i++) {
                if (bots.get(i).getChips() <= 0 && !folded[i]) { // Track eliminations
                    int lvl = bots.get(i).getBotLevel(); bustsByTier[lvl]++;
                    if (replacement) {
                        String oldName = bots.get(i).getName();
                        PokerPlayer[] unlocker = { new PokerPlayer("edj") };
                        PokerBot newBot = new PokerBot(unlocker);
                        newBot.setBotLevel(lvl); newBot.refreshNameTag(unlocker);
                        newBot.removeChips(newBot.getChips()); newBot.addChips(400);
                        bots.set(i, newBot);
                        if (verbose) rep += " | [Bust: " + oldName + " -> New " + newBot.getName() + "]";
                    } else {
                        if (verbose) rep += " | [Bust: " + bots.get(i).getName() + "]";
                        folded[i] = true; // Mark as permanently folded/out for this game
                    }
                }
            }

            if (verbose) {
                String winnerName = (winnerIdx == -1) ? "MULTIPLE PLAYERS" : bots.get(winnerIdx).getName();
                System.out.println("Hand #" + id + ": " + winnerName + " won ✨" + winAmount + " with " + winningHand + rep);
            }
            return winnerIdx;
        }

        private int countActive(boolean[] folded) {
            int count = 0;
            for (boolean f : folded) if (!f) count++;
            return count;
        }

        private String getRankingName(int rank) {
            String[] names = {"", "STRAIGHT FLUSH", "FOUR OF A KIND", "FULL HOUSE", "FLUSH", "STRAIGHT", "THREE OF A KIND", "TWO PAIR", "ONE PAIR", "HIGH CARD"};
            return (rank >= 1 && rank <= 9) ? names[rank] : "Unknown";
        }

        private String getTierCode(int lvl) {
            return (lvl == 0 ? "D" : (lvl == 1 ? "S" : "G"));
        }

        private void payBlind(int idx, int amount, int[] conts, PokerPot pot) {
            int paid = Math.min(bots.get(idx).getChips(), amount);
            conts[idx] += paid;
            pot.addPlayerContribution(idx, paid);
    }
}
}
