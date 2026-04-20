import java.util.*;
import java.util.concurrent.*;

public class PokerSimulator {
    private static Scanner sc = Player.sc;
    private static int dumbCount, smartCount, godCount;
    private static boolean isProtectedMode = false;
    private static boolean isNeuralProtectedMode = false;
    private static int nightmareIntensity = 2;
    private static boolean parallelEnabled = false;
    private static int parallelThreads = Math.max(1, Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        setup();
        while (true) {
            System.out.println("\n--- POKER SIMULATOR MENU ---");
            System.out.println("[Parallel Mode: " + (parallelEnabled ? ("ON @ " + parallelThreads + " threads") : "OFF") + "]");
            String neuralStatus = isProtectedMode
                ? (isNeuralProtectedMode ? "ON" : "OFF")
                : "LOCKED (requires Protected Mode)";
            System.out.println("[Neural Protected Mode: " + neuralStatus + "]");
            System.out.println("[1] Simulate 1 Hand (Interactive)");
            System.out.println("[2] Simulate 1 Game (Standard)");
            System.out.println("[3] Simulate N Hands (Replacement Mode)");
            System.out.println("[4] Simulate N Games (Macro Sim)");
            System.out.println("[5] Simulate N Games (Macro Replacement Sim)");
            System.out.println("[6] Simulate N Duplicate Pairs (Duplicate Duel)");
            System.out.println("[7] Simulate N Individual Hands (True EV Mode)");
            System.out.println("[8] Edit Game Setup");
            System.out.println("[9] Exit");
            
            int choice = Player.getValidInt("Choice: ", 1, 9);
            if (choice == 9) break;
            
            switch (choice) {
                case 1: runOneHand(); break;
                case 2: runOneGame(); break;
                case 3: runNHands(); break;
                case 4: runNGames(); break;
                case 5: runMacroReplacement(); break;
                case 6: runDuplicateDuel(); break;
                case 7: runIndependentHands(); break;
                case 8: setup(); break;
            }
        }
    }

    private static void setup() {
        System.out.println("\n--- GAME SETUP ---");
        dumbCount = Player.getValidInt("How many Dumb Bots? ", 0, 12);
        smartCount = Player.getValidInt("How many Smart Bots? ", 0, 12 - dumbCount);
        godCount = Player.getValidInt("How many God Bots? ", 0, 12 - dumbCount - smartCount);
        nightmareIntensity = 2;
        
        System.out.print("Enable Protected Mode for all bots? (Strips exploits) [y/N]: ");
        String pIn = sc.nextLine();
        isProtectedMode = pIn.equalsIgnoreCase("y");

        // Neural sandbox is only valid under Protected Mode to avoid conflicting with base God logic.
        isNeuralProtectedMode = false;
        if (isProtectedMode) {
            System.out.print("Enable Neural Protected Mode? (Simulator-only profile sandbox) [y/N]: ");
            String nIn = sc.nextLine();
            isNeuralProtectedMode = nIn.equalsIgnoreCase("y");
        } else {
            System.out.println("Neural Protected Mode is unavailable while Protected Mode is OFF.");
        }

        System.out.print("Enable Parallel Mode for bulk simulations? [y/N]: ");
        String parallelIn = sc.nextLine();
        parallelEnabled = parallelIn.equalsIgnoreCase("y");
        if (parallelEnabled) {
            int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
            parallelThreads = Player.getValidInt("Thread count (1-" + maxThreads + "): ", 1, maxThreads);
        } else {
            parallelThreads = 1;
        }

        if (dumbCount + smartCount + godCount < 2) {
            System.out.println("Wait, you need at least 2 bots to play!");
            setup();
        }
    }

    private static class SimulationTotals {
        private final int[] gameWins = new int[3];
        private final int[] gameBusts = new int[3];
        private final long[] totalNetGold = new long[3];
        private final long[] tierNetProfits = new long[3];
        private final double[] tierProfitSquares = new double[3];
        private final long[] tierHands = new long[3];
        private final int[] handWins = new int[3];
        private long totalHandsProcessed = 0;

        void absorbEngine(SimEngine engine, Integer gameWinnerLevel) {
            if (gameWinnerLevel != null && gameWinnerLevel >= 0) {
                gameWins[gameWinnerLevel]++;
            }

            int[] busts = engine.getBusts();
            long[] netGold = engine.getTierNetGold();
            long[] handProfits = engine.getTierNetProfits();
            double[] handSquares = engine.getTierProfitSquares();
            long[] handCounts = engine.getTierHandsPlayed();
            int[] wins = engine.getWinsByTier();

            for (int lvl = 0; lvl < 3; lvl++) {
                gameBusts[lvl] += busts[lvl];
                totalNetGold[lvl] += netGold[lvl];
                tierNetProfits[lvl] += handProfits[lvl];
                tierProfitSquares[lvl] += handSquares[lvl];
                tierHands[lvl] += handCounts[lvl];
                handWins[lvl] += wins[lvl];
            }
            totalHandsProcessed += engine.getHandCount();
        }

        void merge(SimulationTotals other) {
            for (int lvl = 0; lvl < 3; lvl++) {
                gameWins[lvl] += other.gameWins[lvl];
                gameBusts[lvl] += other.gameBusts[lvl];
                totalNetGold[lvl] += other.totalNetGold[lvl];
                tierNetProfits[lvl] += other.tierNetProfits[lvl];
                tierProfitSquares[lvl] += other.tierProfitSquares[lvl];
                tierHands[lvl] += other.tierHands[lvl];
                handWins[lvl] += other.handWins[lvl];
            }
            totalHandsProcessed += other.totalHandsProcessed;
        }
    }

    private static int getWorkerCount(int workload) {
        if (!parallelEnabled || workload <= 1) {
            return 1;
        }
        return Math.max(1, Math.min(parallelThreads, workload));
    }

    private static void runOneHand() {
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, true, isProtectedMode, nightmareIntensity);
        engine.runHand(1, true);
    }

    private static void runOneGame() {
        int maxHands = Player.getValidInt("Max hands for this game? (Enter for Last Man Standing)", 1, 1000000, false, true);
        if (maxHands <= 0) maxHands = Integer.MAX_VALUE;
        
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
        engine.runGame(maxHands, true);
    }

    private static void runNHands() {
        int n = Player.getValidInt("How many hands to simulate?", 1, 1000000);
        if (n <= 0) n = Integer.MAX_VALUE;
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
        engine.runContinuous(n, false);
        engine.printFinalReport(null, true);
    }

    private static void runNGames() {
        int n = Player.getValidInt("How many games to simulate?", 1, 1000000);
        if (n <= 0) n = 1; // Safeguard
        
        int maxHands = Player.getValidInt("Max hands per game? (Enter for Last Man Standing)", 1, 1000000, false, true);
        if (maxHands <= 0) maxHands = Integer.MAX_VALUE;

        int workers = getWorkerCount(n);
        long startNs = System.nanoTime();
        SimulationTotals totals = (workers == 1)
            ? runNGamesSequential(n, maxHands)
            : runNGamesParallel(n, maxHands, workers);
        double elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0;

        System.out.println("\n--- MACRO SIMULATION RESULTS (" + n + " games) ---");
        if (workers > 1) {
            System.out.printf("Parallel workers: %d | Runtime: %.2fs\n", workers, elapsedSec);
        }
        int totalGames = n;
        System.out.printf("Win Rates   -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%\n", 
            (totals.gameWins[0]*100.0/totalGames), (totals.gameWins[1]*100.0/totalGames), (totals.gameWins[2]*100.0/totalGames));
        System.out.printf("Bust Freq   -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G\n",
            (double)totals.gameBusts[0]/totalGames, (double)totals.gameBusts[1]/totalGames, (double)totals.gameBusts[2]/totalGames);
        System.out.printf("Net Gold    -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
            (totals.totalNetGold[0]>=0?"+":""), totals.totalNetGold[0], (totals.totalNetGold[1]>=0?"+":""), totals.totalNetGold[1], (totals.totalNetGold[2]>=0?"+":""), totals.totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n", (double)(totals.gameBusts[0]+totals.gameBusts[1]+totals.gameBusts[2])/totalGames);
        printAdvancedTierTelemetry("MACRO ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits, totals.tierProfitSquares, totals.tierHands, 20);
    }

    private static SimulationTotals runNGamesSequential(int n, int maxHands) {
        SimulationTotals totals = new SimulationTotals();
        PokerBot.resetThreadCognitiveDB();
        try {
            for (int i = 0; i < n; i++) {
                PokerBot.resetThreadCognitiveDB();
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
                int winnerLevel = engine.runGame(maxHands, false);
                // System.out.println("Game #" + (i + 1) + " Winner: " + (winnerLevel == 0 ? "Dumb" : (winnerLevel == 1 ? "Smart" : "God")));
                totals.absorbEngine(engine, winnerLevel);
            }
        } finally {
            PokerBot.clearThreadCognitiveDB();
        }
        return totals;
    }

    private static SimulationTotals runNGamesParallel(int n, int maxHands, int workers) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<SimulationTotals>> futures = new ArrayList<>();
        int base = n / workers;
        int rem = n % workers;

        for (int w = 0; w < workers; w++) {
            int gamesForWorker = base + (w < rem ? 1 : 0);
            futures.add(executor.submit(() -> {
                SimulationTotals local = new SimulationTotals();
                PokerBot.resetThreadCognitiveDB();
                try {
                    for (int j = 0; j < gamesForWorker; j++) {
                        PokerBot.resetThreadCognitiveDB();
                        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
                        int winnerLevel = engine.runGame(maxHands, false);
                        local.absorbEngine(engine, winnerLevel);
                    }
                } finally {
                    PokerBot.clearThreadCognitiveDB();
                }
                return local;
            }));
        }

        SimulationTotals totals = new SimulationTotals();
        try {
            for (Future<SimulationTotals> f : futures) {
                totals.merge(f.get());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel NGames simulation was interrupted", ie);
        } catch (ExecutionException ee) {
            throw new RuntimeException("Parallel NGames simulation failed", ee.getCause());
        } finally {
            executor.shutdownNow();
        }
        return totals;
    }

    private static void runMacroReplacement() {
        int n = Player.getValidInt("How many replacement games to simulate?", 1, 1000000);
        int handsPerGame = Player.getValidInt("Hands per game?", 1, 1000000);

        int workers = getWorkerCount(n);
        long startNs = System.nanoTime();
        SimulationTotals totals = (workers == 1)
            ? runMacroReplacementSequential(n, handsPerGame)
            : runMacroReplacementParallel(n, handsPerGame, workers);
        double elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0;

        System.out.println("\n--- MACRO REPLACEMENT RESULTS (" + n + " games, " + handsPerGame + " hands/ea) ---");
        if (workers > 1) {
            System.out.printf("Parallel workers: %d | Runtime: %.2fs\n", workers, elapsedSec);
        }
        int totalGames = n;
        System.out.printf("Session Win Rates (By Profit) -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%\n", 
            (totals.gameWins[0]*100.0/totalGames), (totals.gameWins[1]*100.0/totalGames), (totals.gameWins[2]*100.0/totalGames));
        System.out.printf("Bust Freq -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G\n",
            (double)totals.gameBusts[0]/totalGames, (double)totals.gameBusts[1]/totalGames, (double)totals.gameBusts[2]/totalGames);
        System.out.printf("Total Net Gold -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
            (totals.totalNetGold[0]>=0?"+":""), totals.totalNetGold[0], (totals.totalNetGold[1]>=0?"+":""), totals.totalNetGold[1], (totals.totalNetGold[2]>=0?"+":""), totals.totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n", (double)(totals.gameBusts[0]+totals.gameBusts[1]+totals.gameBusts[2])/totalGames);
        printAdvancedTierTelemetry("MACRO REPLACEMENT ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits, totals.tierProfitSquares, totals.tierHands, 20);
        if (isProtectedMode) System.out.println("Note: Simulation run in PROTECTED MODE (Exploits disabled).");
    }

    private static SimulationTotals runMacroReplacementSequential(int n, int handsPerGame) {
        SimulationTotals totals = new SimulationTotals();
        PokerBot.resetThreadCognitiveDB();
        try {
            for (int i = 0; i < n; i++) {
                PokerBot.resetThreadCognitiveDB();
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
                engine.runContinuous(handsPerGame, false);
                int winnerLevel = engine.getWinningTierByProfit();
                // System.out.println("Replacement Game #" + (i + 1) + " Most Profitable: " + (winnerLevel == 0 ? "Dumb" : (winnerLevel == 1 ? "Smart" : "God")));
                totals.absorbEngine(engine, winnerLevel);
            }
        } finally {
            PokerBot.clearThreadCognitiveDB();
        }
        return totals;
    }

    private static SimulationTotals runMacroReplacementParallel(int n, int handsPerGame, int workers) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<SimulationTotals>> futures = new ArrayList<>();
        int base = n / workers;
        int rem = n % workers;

        for (int w = 0; w < workers; w++) {
            int gamesForWorker = base + (w < rem ? 1 : 0);
            futures.add(executor.submit(() -> {
                SimulationTotals local = new SimulationTotals();
                PokerBot.resetThreadCognitiveDB();
                try {
                    for (int j = 0; j < gamesForWorker; j++) {
                        PokerBot.resetThreadCognitiveDB();
                        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
                        engine.runContinuous(handsPerGame, false);
                        int winnerLevel = engine.getWinningTierByProfit();
                        local.absorbEngine(engine, winnerLevel);
                    }
                } finally {
                    PokerBot.clearThreadCognitiveDB();
                }
                return local;
            }));
        }

        SimulationTotals totals = new SimulationTotals();
        try {
            for (Future<SimulationTotals> f : futures) {
                totals.merge(f.get());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel macro replacement simulation was interrupted", ie);
        } catch (ExecutionException ee) {
            throw new RuntimeException("Parallel macro replacement simulation failed", ee.getCause());
        } finally {
            executor.shutdownNow();
        }
        return totals;
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
        double t1ProfitSquares = 0;
        double t2ProfitSquares = 0;

        int pairCount = 0;
        SimEngine engine = new SimEngine(new int[]{t1, t2}, false, isProtectedMode, nightmareIntensity);
        engine.setPerHandNeuralResetEnabled(isProtectedMode && isNeuralProtectedMode);
        PokerBot botA = engine.bots.get(0);
        
        System.out.println("Executing " + pairs + " duplicate pairs...");
        
        while (pairCount < pairs) {
            pairCount++;
            // Preserve model memory across pairs while resetting chip state.
            // Canonicalize order so pass-1 accounting remains Bot A index 0 / Bot B index 1.
            if (engine.bots.get(0) != botA) {
                PokerBot tempBot = engine.bots.get(0);
                engine.bots.set(0, engine.bots.get(1));
                engine.bots.set(1, tempBot);
            }
            
            for(PokerBot b : engine.bots) {
                b.removeChips(b.getChips());
                b.addChips(400);
            }

            PokerDeck tempDeck = new PokerDeck();
            Card[][] holeCards = tempDeck.deal(2);
            Card[] board = tempDeck.deal();
            
            // Pass 1: Bot A at 0, Bot B at 1
            int winner1 = engine.runHand(1, false, false, holeCards, board);
            int p1A = engine.bots.get(0).getChips() - 400;
            int p1B = engine.bots.get(1).getChips() - 400;
            t1TotalGold += p1A;
            t2TotalGold += p1B;
            t1ProfitSquares += (double)p1A * p1A;
            t2ProfitSquares += (double)p1B * p1B;
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
            // Use the original dealt holes with swapped bot order so identities swap private cards,
            // while id=1 ensures they also alternate SB/BB once per pair.
            int winner2 = engine.runHand(1, false, false, holeCards, board);
            
            // Note: In Pass 2, engine.bots.get(0) is now Bot B, and bots.get(1) is now Bot A
            int p2B = engine.bots.get(0).getChips() - 400;
            int p2A = engine.bots.get(1).getChips() - 400;
            t2TotalGold += p2B;
            t1TotalGold += p2A;
            t2ProfitSquares += (double)p2B * p2B;
            t1ProfitSquares += (double)p2A * p2A;
            if (engine.bots.get(0).getChips() == 0) t2Busts++;
            if (engine.bots.get(1).getChips() == 0) t1Busts++;
            if (winner2 == 0) t2Wins++; else if (winner2 == 1) t1Wins++;
            else if (winner2 == -1) { t1Wins++; t2Wins++; }

            // Restore canonical order for next pair.
            if (engine.bots.get(0) != botA) {
                PokerBot swapBack = engine.bots.get(0);
                engine.bots.set(0, engine.bots.get(1));
                engine.bots.set(1, swapBack);
            }
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

        double t1MeanProfit = (double)t1TotalGold / totalHands;
        double t2MeanProfit = (double)t2TotalGold / totalHands;
        double t1Variance = (t1ProfitSquares / totalHands) - (t1MeanProfit * t1MeanProfit);
        double t2Variance = (t2ProfitSquares / totalHands) - (t2MeanProfit * t2MeanProfit);
        double t1Stdev = Math.sqrt(Math.max(0, t1Variance));
        double t2Stdev = Math.sqrt(Math.max(0, t2Variance));
        double t1Ci = 1.96 * (t1Stdev / Math.sqrt(totalHands));
        double t2Ci = 1.96 * (t2Stdev / Math.sqrt(totalHands));
        double t1Bb100 = (t1MeanProfit / 20.0) * 100.0;
        double t2Bb100 = (t2MeanProfit / 20.0) * 100.0;
        double t1CiBb100 = (t1Ci / 20.0) * 100.0;
        double t2CiBb100 = (t2Ci / 20.0) * 100.0;
        System.out.println("\n--- DUPLICATE DUEL ADVANCED TELEMETRY ---");
        System.out.printf("Bot A [%s] BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name1, t1Bb100, t1Bb100 - t1CiBb100, t1Bb100 + t1CiBb100);
        System.out.printf("Bot B [%s] BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name2, t2Bb100, t2Bb100 - t2CiBb100, t2Bb100 + t2CiBb100);
        
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

    private static void runIndependentHands() {
        System.out.println("\n--- TRUE EV MODE: INDIVIDUAL HANDS ---");
        int n = Player.getValidInt("How many independent hands to simulate?", 1, 10000000);

        int workers = getWorkerCount(n);
        if (workers == 1) {
            PokerBot.resetThreadCognitiveDB();
            try {
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity, false);
                engine.setPerHandNeuralResetEnabled(isProtectedMode && isNeuralProtectedMode);
                engine.runIndividualContinuous(n, false);
                SimulationTotals totals = new SimulationTotals();
                totals.absorbEngine(engine, null);
                printAggregatedTrueEvReport(n, totals);
            } finally {
                PokerBot.clearThreadCognitiveDB();
            }
            return;
        }

        long startNs = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<SimulationTotals>> futures = new ArrayList<>();
        int base = n / workers;
        int rem = n % workers;

        for (int w = 0; w < workers; w++) {
            int handsForWorker = base + (w < rem ? 1 : 0);
            futures.add(executor.submit(() -> {
                SimulationTotals local = new SimulationTotals();
                PokerBot.resetThreadCognitiveDB();
                try {
                    SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity, false);
                    engine.setPerHandNeuralResetEnabled(isProtectedMode && isNeuralProtectedMode);
                    engine.runIndividualContinuous(handsForWorker, false);
                    local.absorbEngine(engine, null);
                } finally {
                    PokerBot.clearThreadCognitiveDB();
                }
                return local;
            }));
        }

        SimulationTotals totals = new SimulationTotals();
        try {
            for (Future<SimulationTotals> f : futures) {
                totals.merge(f.get());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel independent-hands simulation was interrupted", ie);
        } catch (ExecutionException ee) {
            throw new RuntimeException("Parallel independent-hands simulation failed", ee.getCause());
        } finally {
            executor.shutdownNow();
        }

        double elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0;
        System.out.printf("Parallel workers: %d | Runtime: %.2fs\n", workers, elapsedSec);
        printAggregatedTrueEvReport(n, totals);
        if (isProtectedMode) {
            System.out.println("Note: Simulation run in PROTECTED MODE (Exploits disabled).");
        }
    }

    private static void printAggregatedTrueEvReport(int totalHands, SimulationTotals totals) {
        System.out.println("\n--- FINAL TELEMETRY REPORT ---");
        System.out.println("Total Hands Processed: " + totalHands);

        if (totalHands > 0) {
            System.out.printf("Bust Rates -> Dumb: %d (%.2f%%), Smart: %d (%.2f%%), God: %d (%.2f%%)\n",
                totals.gameBusts[0], (totals.gameBusts[0] * 100.0 / totalHands),
                totals.gameBusts[1], (totals.gameBusts[1] * 100.0 / totalHands),
                totals.gameBusts[2], (totals.gameBusts[2] * 100.0 / totalHands));

            System.out.printf("Net Gold   -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d\n",
                (totals.tierNetProfits[0] >= 0 ? "+" : ""), totals.tierNetProfits[0],
                (totals.tierNetProfits[1] >= 0 ? "+" : ""), totals.tierNetProfits[1],
                (totals.tierNetProfits[2] >= 0 ? "+" : ""), totals.tierNetProfits[2]);

            if (totals.tierHands[0] > 0) {
                printAdvancedTierTelemetry("ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits, totals.tierProfitSquares, totals.tierHands, 20);
            }
        }

        int totalWins = totals.handWins[0] + totals.handWins[1] + totals.handWins[2];
        if (totalWins > 0) {
            System.out.printf("Win Rate %% (By Hands Won): God (%.1f%%), Smart (%.1f%%), Dumb (%.1f%%)\n",
                (totals.handWins[2] * 100.0 / totalWins),
                (totals.handWins[1] * 100.0 / totalWins),
                (totals.handWins[0] * 100.0 / totalWins));
        }
    }

    private static void printAdvancedTierTelemetry(String title, long[] tierNetProfits, double[] tierProfitSquares, long[] tierHands, int bigBlind) {
        if (tierHands[0] <= 0) return;

        System.out.println("\n--- " + title + " ---");
        for (int lvl = 0; lvl < 3; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : "God");
            double meanProfit = (double)tierNetProfits[lvl] / tierHands[lvl];
            double bb100 = (meanProfit / bigBlind) * 100.0;
            double variance = (tierProfitSquares[lvl] / tierHands[lvl]) - (meanProfit * meanProfit);
            double stdev = Math.sqrt(Math.max(0, variance));
            double ciMargin = 1.96 * (stdev / Math.sqrt(tierHands[lvl]));
            double ciMarginBb100 = (ciMargin / bigBlind) * 100.0;
            System.out.printf("%s BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name, bb100, bb100 - ciMarginBb100, bb100 + ciMarginBb100);
        }
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
        private boolean neuralProtectedMode = false;
        private PokerDeck deck = new PokerDeck();
        private boolean shouldShuffle = true;
        private int blinds = 20;
        private int[] duelLevels = null;
        private int nightmareIntensity;
        private boolean resetNeuralMemoryEachHand = false;

        private long[] tierNetProfits = new long[3];
        private double[] tierSumSquaresOfProfits = new double[3];
        private long[] tierHandsPlayed = new long[3];
        private boolean isTrueEvMode = false;
        private static final double PREFLOP_EMA_ALPHA = 0.35;
        private static final double POSTFLOP_EMA_ALPHA = 0.35;
        private boolean[] preflopVPIPFlags;
        private boolean[] preflopPFRFlags;
        private int[][] postflopAggressionActions;
        private int[][] postflopAggressionOpportunities;
        private boolean[] sawFlopThisHand;
        private boolean[] foldToCbetOpportunity;
        private boolean[] foldedToCbet;
        private boolean cbetFiredOnFlop = false;
        private int cbetAggressorIndex = -1;

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode) {
            this(d, s, g, interactive, protectedMode, 1, true);
        }

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode, int nightmareIntensity) {
            this(d, s, g, interactive, protectedMode, nightmareIntensity, true);
        }

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode, int nightmareIntensity, boolean shouldShuffle) {
            PokerBot.resetThreadCognitiveDB();
            this.dumbCount = d; this.smartCount = s; this.godCount = g;
            this.interactive = interactive;
            this.protectedMode = protectedMode;
            this.neuralProtectedMode = protectedMode && PokerSimulator.isNeuralProtectedMode;
            this.nightmareIntensity = nightmareIntensity;
            this.shouldShuffle = shouldShuffle;
            initializeBots();
        }

        public SimEngine(int[] levels, boolean interactive, boolean protectedMode, int nightmareIntensity) {
            PokerBot.resetThreadCognitiveDB();
            this.duelLevels = levels;
            this.dumbCount = 0; this.smartCount = 0; this.godCount = 0;
            for (int l : levels) {
                if (l == 0) dumbCount++; else if (l == 1) smartCount++; else if (l == 2) godCount++;
            }
            this.interactive = interactive;
            this.protectedMode = protectedMode;
            this.neuralProtectedMode = protectedMode && PokerSimulator.isNeuralProtectedMode;
            this.nightmareIntensity = nightmareIntensity;
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
            b.setNeuralProtectedMode(neuralProtectedMode);
            b.setNightmareIntensity(nightmareIntensity);
            b.refreshNameTag(unlocker); // Force refresh with the unlocker
            b.removeChips(b.getChips());
            b.addChips(400); // 20bb
            bots.add(b);
        }

        private boolean shouldTrackCognitiveBot(PokerBot bot) {
            if (bot == null) return false;
            if (protectedMode && !neuralProtectedMode) return false;
            if (neuralProtectedMode) return true;
            return bot.getBotLevel() == 2;
        }

        private int getStreetIndex(String street) {
            if ("preflop".equals(street)) return 0;
            if ("flop".equals(street)) return 1;
            if ("turn".equals(street)) return 2;
            if ("river".equals(street)) return 3;
            return -1;
        }

        private void initCognitiveTelemetryBuffers(int numPlayers) {
            preflopVPIPFlags = new boolean[numPlayers];
            preflopPFRFlags = new boolean[numPlayers];
            postflopAggressionActions = new int[numPlayers][4];
            postflopAggressionOpportunities = new int[numPlayers][4];
            sawFlopThisHand = new boolean[numPlayers];
            foldToCbetOpportunity = new boolean[numPlayers];
            foldedToCbet = new boolean[numPlayers];
            cbetFiredOnFlop = false;
            cbetAggressorIndex = -1;
        }

        private void markPreflopActionForTelemetry(int playerIndex, int preActionTableBet, int preActionContribution, int paid, int actionCode, int postActionContribution) {
            if (preflopVPIPFlags == null || preflopPFRFlags == null) return;
            if (playerIndex < 0 || playerIndex >= bots.size()) return;
            PokerBot actor = bots.get(playerIndex);
            if (!shouldTrackCognitiveBot(actor)) return;

            boolean contributed = paid > 0 && postActionContribution > preActionContribution;
            boolean isAggressiveAction = (actionCode == 3 || actionCode == 4);
            boolean isRaise = isAggressiveAction && postActionContribution > preActionTableBet;

            if (contributed && (actionCode == 1 || isAggressiveAction)) {
                preflopVPIPFlags[playerIndex] = true;
            }
            if (isRaise) {
                preflopPFRFlags[playerIndex] = true;
                preflopVPIPFlags[playerIndex] = true;
            }
        }

        private void finalizePreflopTelemetry() {
            if (preflopVPIPFlags == null || preflopPFRFlags == null) return;
            for (int i = 0; i < bots.size(); i++) {
                PokerBot actor = bots.get(i);
                if (!shouldTrackCognitiveBot(actor)) continue;
                PokerBot.updatePreflopTelemetryTracked(actor.getName(), preflopVPIPFlags[i], preflopPFRFlags[i], PREFLOP_EMA_ALPHA);
            }
        }

        private void markPostflopActionForTelemetry(int streetIndex, int playerIndex, int preActionTableBet, int preActionContribution, int paid, int actionCode, int postActionContribution, int preflopAggressorIndex) {
            if (streetIndex < 1 || streetIndex > 3) return;
            if (postflopAggressionActions == null || postflopAggressionOpportunities == null) return;
            if (playerIndex < 0 || playerIndex >= bots.size()) return;
            PokerBot actor = bots.get(playerIndex);
            if (!shouldTrackCognitiveBot(actor)) return;

            boolean isAggressiveAction = (actionCode == 3 || actionCode == 4) && postActionContribution > preActionTableBet;
            boolean isCall = (actionCode == 1 && paid > 0);
            boolean isFoldFacingBet = (actionCode == 2 && preActionTableBet > preActionContribution);

            if (isAggressiveAction || isCall || isFoldFacingBet) {
                postflopAggressionOpportunities[playerIndex][streetIndex]++;
                if (isAggressiveAction) postflopAggressionActions[playerIndex][streetIndex]++;
            }

            if (streetIndex == 1) {
                if (!cbetFiredOnFlop && preActionTableBet == 0 && playerIndex == preflopAggressorIndex && isAggressiveAction) {
                    cbetFiredOnFlop = true;
                    cbetAggressorIndex = playerIndex;
                }
                if (cbetFiredOnFlop && playerIndex != cbetAggressorIndex && preActionTableBet > preActionContribution) {
                    foldToCbetOpportunity[playerIndex] = true;
                    if (actionCode == 2) foldedToCbet[playerIndex] = true;
                }
            }
        }

        private String getAFqStreetStatKey(int street) {
            if (street == 1) return "AFq_Flop";
            if (street == 2) return "AFq_Turn";
            if (street == 3) return "AFq_River";
            return "AFq_Preflop";
        }

        private void finalizePostflopTelemetry(boolean reachedShowdown, boolean[] folded) {
            if (postflopAggressionActions == null || postflopAggressionOpportunities == null) return;

            for (int i = 0; i < bots.size(); i++) {
                PokerBot actor = bots.get(i);
                if (!shouldTrackCognitiveBot(actor)) continue;
                String name = actor.getName();

                for (int street = 1; street <= 3; street++) {
                    int opps = postflopAggressionOpportunities[i][street];
                    if (opps > 0) {
                        double afqValue = (double) postflopAggressionActions[i][street] / opps;
                        PokerBot.updateCognitiveStatTracked(name, getAFqStreetStatKey(street), afqValue, POSTFLOP_EMA_ALPHA);
                    }
                }

                if (foldToCbetOpportunity[i]) {
                    PokerBot.updateCognitiveStatTracked(name, "FoldToCBet", foldedToCbet[i] ? 1.0 : 0.0, POSTFLOP_EMA_ALPHA);
                }

                if (sawFlopThisHand != null && sawFlopThisHand[i]) {
                    boolean survivedToShowdown = reachedShowdown && !folded[i];
                    PokerBot.updateCognitiveStatTracked(name, "WTSD", survivedToShowdown ? 1.0 : 0.0, POSTFLOP_EMA_ALPHA);
                }
            }
        }

        public void setPerHandNeuralResetEnabled(boolean enabled) {
            this.resetNeuralMemoryEachHand = enabled;
        }

        public int[] getBusts() { return bustsByTier; }

        public long[] getTierNetProfits() { return tierNetProfits.clone(); }

        public double[] getTierProfitSquares() { return tierSumSquaresOfProfits.clone(); }

        public long[] getTierHandsPlayed() { return tierHandsPlayed.clone(); }

        public int[] getWinsByTier() { return winsByTier.clone(); }

        public int getHandCount() { return handCount; }

        public long[] getTierNetGold() {
            if (isTrueEvMode) {
                return tierNetProfits.clone();
            }
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

        public void runIndividualContinuous(int n, boolean verbose) {
            this.isTrueEvMode = true;
            this.replacementMode = true;
            while (handCount < n) {
                for (PokerBot b : bots) {
                    b.removeChips(b.getChips());
                    b.addChips(400); // 20bb fresh
                }
                Collections.shuffle(bots); // randomize seating
                handCount++;
                runHand(handCount, verbose, true);
            }
            if (verbose) printFinalReport(null, true);
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

        public void printFinalReport(PokerBot overallWinner, boolean showBusts) {
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
                    
                if (tierHandsPlayed[0] > 0) {
                    System.out.println("\n--- ADVANCED TELEMETRY (Tier Overall) ---");
                    for (int lvl=0; lvl<3; lvl++) {
                        String name = lvl==0?"Dumb":(lvl==1?"Smart":"God");
                        double meanProfit = (double)tierNetProfits[lvl] / tierHandsPlayed[lvl];
                        double bb100 = (meanProfit / blinds) * 100.0;
                        double variance = (tierSumSquaresOfProfits[lvl] / tierHandsPlayed[lvl]) - (meanProfit * meanProfit);
                        double stdev = Math.sqrt(Math.max(0, variance));
                        double ciMargin = 1.96 * (stdev / Math.sqrt(tierHandsPlayed[lvl]));
                        double ciMarginBb100 = (ciMargin / blinds) * 100.0;
                        System.out.printf("%s BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name, bb100, bb100 - ciMarginBb100, bb100 + ciMarginBb100);
                    }
                }
            }
            
            if (!isTrueEvMode) {
                System.out.println("\nFinal Stacks:");
                for (PokerBot b : bots) {
                    System.out.println(b.getName() + ": ✨" + b.getChips());
                }
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
            if (resetNeuralMemoryEachHand && protectedMode && neuralProtectedMode) {
                PokerBot.resetThreadCognitiveDB();
            }
            deck.reset();
            int numPlayers = bots.size();
            long[] initialChips = new long[numPlayers];
            for (int i = 0; i < numPlayers; i++) initialChips[i] = bots.get(i).getChips();
            initCognitiveTelemetryBuffers(numPlayers);

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
            int bbIdx = (sbIdx + 1) % numPlayers;
            safety = 0;
            while (bots.get(bbIdx).getChips() <= 0 && safety++ < numPlayers) bbIdx = (bbIdx + 1) % numPlayers;
            
            int preflopAggressorIndex = bbIdx;

            payBlind(sbIdx, blinds/2, contributions, pot);
            payBlind(bbIdx, blinds, contributions, pot);

            String[] streetNames = {"preflop", "flop", "turn", "river"};
            Card[] board = new Card[0];
            int currentBet = blinds;
            int lastRaise = blinds;

            int preflopRaiseCount = 0;
            int preflopLastRaiser = -1;
            boolean huSmartFacing3BetPending = false;
            boolean huFlopCbetResponsePending = false;
            boolean huGodCbetFlopSeen = false;
            boolean huTurnBarrelResponsePending = false;
            boolean huTurnBarrelSeen = false;
            boolean huRiverLargeBetResponsePending = false;
            boolean huRiverLargeBetSeen = false;
            int turnFirstActor = -1;
            boolean turnFirstActorChecked = false;
            boolean turnCheckBackObserved = false;

            for (String street : streetNames) {
                if (!street.equals("preflop")) {
                    if (street.equals("flop")) {
                        if (fixedBoard == null) deck.deal();
                        board = new Card[3];
                        Card[] actualBoard = (fixedBoard != null) ? fixedBoard : deck.getBoard();
                        System.arraycopy(actualBoard, 0, board, 0, 3);
                        for (int pIdx = 0; pIdx < numPlayers; pIdx++) {
                            sawFlopThisHand[pIdx] = !folded[pIdx] && bots.get(pIdx).getChips() > 0;
                        }
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

                    if (street.equals("flop")) {
                        huFlopCbetResponsePending = false;
                    } else if (street.equals("turn")) {
                        huTurnBarrelResponsePending = false;
                        huTurnBarrelSeen = false;
                        turnFirstActor = -1;
                        turnFirstActorChecked = false;
                        turnCheckBackObserved = false;
                    } else if (street.equals("river")) {
                        huRiverLargeBetResponsePending = false;
                        huRiverLargeBetSeen = false;
                    }
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
                    startPlayer = (street.equals("preflop")) ? (bbIdx + 1) % numPlayers : sbIdx;
                }

                int i = startPlayer;
                int lastAggressor = -1;
                int playersActed = 0;

                while (!roundDone) {
                    if (!folded[i] && bots.get(i).getChips() > 0) {
                        int[] huPair = getHeadsUpSmartGodPair(folded);
                        int huSmartIdx = huPair[0];
                        int huGodIdx = huPair[1];
                        boolean huSmartGod = (huSmartIdx >= 0 && huGodIdx >= 0);
                        String huSmartName = huSmartGod ? bots.get(huSmartIdx).getName() : null;

                        if (street.equals("preflop") && !huSmartGod) {
                            huSmartFacing3BetPending = false;
                        } else if (street.equals("flop") && !huSmartGod) {
                            huFlopCbetResponsePending = false;
                        } else if (street.equals("turn") && !huSmartGod) {
                            huTurnBarrelResponsePending = false;
                        } else if (street.equals("river") && !huSmartGod) {
                            huRiverLargeBetResponsePending = false;
                        }

                        PokerBot actor = bots.get(i);
                        int preActionCurrentBet = currentBet;
                        int preActionContribution = contributions[i];
                        int preActionPot = pot.getTotalPot();
                        int paid = 0;
                        // FIXED: Pass the actual preflopAggressorIndex
                        int[] action = actor.action(street, contributions[i], currentBet, blinds, lastRaise, board, pot.getTotalPot(), participants, i, preflopAggressorIndex, sbIdx, bbIdx);

                        boolean actionIsFold = (action[0] == 2);
                        boolean actionIsCheck = false;
                        boolean actionIsAggressive = false;
                        boolean actorIsHuSmart = huSmartGod && i == huSmartIdx;
                        boolean actorIsHuGod = huSmartGod && i == huGodIdx;
                        
                        if (actionIsFold) {
                            folded[i] = true;
                            if (interactive) System.out.println(actor.getName() + ": FOLDS");
                        } else {
                            int totalContribution = (action[0] == 1) ? (currentBet - contributions[i]) : action[1];
                            paid = Math.min(actor.getChips(), totalContribution);
                            contributions[i] += paid;
                            pot.addPlayerContribution(i, paid);

                            actionIsCheck = (action[0] == 1 && preActionCurrentBet == 0 && contributions[i] == preActionContribution);
                            actionIsAggressive = (contributions[i] > preActionCurrentBet);
                            
                            if (interactive) {
                                String actionVerb = (action[0] == 4 || actor.getChips() == 0) ? "ALL-IN ✨" : (currentBet == 0 ? "BETS ✨" : "RAISES to ✨");
                                if (action[0] == 1) {
                                    boolean isBigBlindCheck = (paid == 0 && street.equals("preflop") && i == bbIdx && currentBet == blinds);
                                    System.out.println(actor.getName() + ": " + ((paid == 0 && currentBet == 0) || isBigBlindCheck ? "CHECKS" : "CALLS ✨" + contributions[i]));
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

                        if (huSmartGod && !protectedMode) {
                            if (street.equals("preflop")) {
                                if (actionIsAggressive) {
                                    preflopRaiseCount++;
                                    if (preflopRaiseCount >= 2 && actorIsHuGod && preflopLastRaiser == huSmartIdx) {
                                        huSmartFacing3BetPending = true;
                                    }
                                    preflopLastRaiser = i;
                                }

                                if (huSmartFacing3BetPending && actorIsHuSmart) {
                                    PokerBot.observeSmartFoldTo3BetHU(huSmartName, actionIsFold);
                                    huSmartFacing3BetPending = false;
                                }
                            } else if (street.equals("flop")) {
                                if (actorIsHuGod && preflopAggressorIndex == huGodIdx && preActionCurrentBet == 0 && actionIsAggressive) {
                                    huFlopCbetResponsePending = true;
                                    huGodCbetFlopSeen = true;
                                }

                                if (huFlopCbetResponsePending && actorIsHuSmart) {
                                    PokerBot.observeSmartFlopCbetResponseHU(huSmartName, actionIsFold, actionIsAggressive);
                                    huFlopCbetResponsePending = false;
                                }
                            } else if (street.equals("turn")) {
                                if (turnFirstActor == -1) {
                                    turnFirstActor = i;
                                    turnFirstActorChecked = actionIsCheck;
                                } else if (!turnCheckBackObserved && actorIsHuSmart && turnFirstActor == huGodIdx && turnFirstActorChecked) {
                                    PokerBot.observeSmartTurnCheckBackHU(huSmartName, actionIsCheck);
                                    turnCheckBackObserved = true;
                                }

                                if (!huTurnBarrelSeen && huGodCbetFlopSeen && actorIsHuGod && actionIsAggressive) {
                                    huTurnBarrelResponsePending = true;
                                    huTurnBarrelSeen = true;
                                }

                                if (huTurnBarrelResponsePending && actorIsHuSmart) {
                                    PokerBot.observeSmartTurnBarrelResponseHU(huSmartName, actionIsFold);
                                    huTurnBarrelResponsePending = false;
                                }
                            } else if (street.equals("river")) {
                                if (!huRiverLargeBetSeen && actorIsHuGod && actionIsAggressive) {
                                    int wagerSize = contributions[i] - preActionCurrentBet;
                                    int largeBetThreshold = Math.max(blinds * 2, (int) (preActionPot * 0.75));
                                    if (wagerSize >= largeBetThreshold) {
                                        huRiverLargeBetResponsePending = true;
                                        huRiverLargeBetSeen = true;
                                    }
                                }

                                if (huRiverLargeBetResponsePending && actorIsHuSmart) {
                                    PokerBot.observeSmartRiverLargeBetResponseHU(huSmartName, actionIsFold);
                                    huRiverLargeBetResponsePending = false;
                                }
                            }
                        }

                        int streetIndex = getStreetIndex(street);
                        if (streetIndex == 0) {
                            markPreflopActionForTelemetry(i, preActionCurrentBet, preActionContribution, paid, action[0], contributions[i]);
                        } else {
                            markPostflopActionForTelemetry(streetIndex, i, preActionCurrentBet, preActionContribution, paid, action[0], contributions[i], preflopAggressorIndex);
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
                if (street.equals("preflop")) {
                    finalizePreflopTelemetry();
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
                winsByTier[bots.get(winnerIdx).getBotLevel()]++;
                winningHand = "FOLD-OUT";
            } else {
                int[] totalContributions = pot.getContributions().clone();
                List<SidePot> sidePots = buildSidePots(totalContributions, folded);
                Set<Integer> handWinners = new HashSet<>();
                List<Integer> mainPotWinners = new ArrayList<>();
                int paidOut = 0;

                for (int pIdx = 0; pIdx < sidePots.size(); pIdx++) {
                    SidePot sidePot = sidePots.get(pIdx);
                    ShowdownOutcome outcome = resolveShowdownForEligible(sidePot.eligibleIndices, board);
                    List<Integer> winnerIndices = outcome.winnerIndices;
                    if (winnerIndices.isEmpty()) {
                        continue;
                    }

                    if (pIdx == 0) {
                        mainPotWinners = new ArrayList<>(winnerIndices);
                        winningHand = getRankingName(outcome.bestRank);
                    }

                    int share = sidePot.amount / winnerIndices.size();
                    int remainder = sidePot.amount % winnerIndices.size();

                    // Randomize remainder distribution start to avoid seat-index bias.
                    int startWinner = (int)(Math.random() * winnerIndices.size());
                    for (int k = 0; k < winnerIndices.size(); k++) {
                        int winnerIdxInList = (startWinner + k) % winnerIndices.size();
                        int idx = winnerIndices.get(winnerIdxInList);

                        int amount = share + (remainder > 0 ? 1 : 0);
                        if (remainder > 0) {
                            remainder--;
                        }

                        bots.get(idx).addChips(amount);
                        handWinners.add(idx);
                    }
                    paidOut += sidePot.amount;
                }

                // Safety net: preserve chip conservation even if a rare edge case misses a slice.
                int delta = winAmount - paidOut;
                if (delta != 0) {
                    if (!mainPotWinners.isEmpty()) {
                        bots.get(mainPotWinners.get(0)).addChips(delta);
                        handWinners.add(mainPotWinners.get(0));
                    } else {
                        for (int i = 0; i < numPlayers; i++) {
                            if (!folded[i]) {
                                bots.get(i).addChips(delta);
                                handWinners.add(i);
                                break;
                            }
                        }
                    }
                }

                for (int idx : handWinners) {
                    winsByTier[bots.get(idx).getBotLevel()]++;
                }
                winnerIdx = (mainPotWinners.size() == 1) ? mainPotWinners.get(0) : -1;
            }

            boolean reachedShowdown = (countActive(folded) > 1);
            finalizePostflopTelemetry(reachedShowdown, folded);
                
            // Track hand-level profit for BB/100 and variance
            long[] handProfits = new long[3];
            for (int i=0; i<numPlayers; i++) {
                long profit = bots.get(i).getChips() - initialChips[i];
                int lvl = bots.get(i).getBotLevel();
                handProfits[lvl] += profit;
            }
            for (int lvl=0; lvl<3; lvl++) {
                tierNetProfits[lvl] += handProfits[lvl];
                tierSumSquaresOfProfits[lvl] += (double)handProfits[lvl] * handProfits[lvl];
                tierHandsPlayed[lvl]++;
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
                        newBot.setBotLevel(lvl);
                        newBot.setProtectedMode(protectedMode);
                        newBot.setNeuralProtectedMode(neuralProtectedMode);
                        newBot.setNightmareIntensity(nightmareIntensity);
                        newBot.refreshNameTag(unlocker);
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

        private static class SidePot {
            final int amount;
            final List<Integer> eligibleIndices;

            SidePot(int amount, List<Integer> eligibleIndices) {
                this.amount = amount;
                this.eligibleIndices = eligibleIndices;
            }
        }

        private static class ShowdownOutcome {
            final List<Integer> winnerIndices;
            final int bestRank;

            ShowdownOutcome(List<Integer> winnerIndices, int bestRank) {
                this.winnerIndices = winnerIndices;
                this.bestRank = bestRank;
            }
        }

        private List<SidePot> buildSidePots(int[] totalContributions, boolean[] folded) {
            List<SidePot> sidePots = new ArrayList<>();
            TreeSet<Integer> thresholds = new TreeSet<>();
            for (int c : totalContributions) {
                if (c > 0) {
                    thresholds.add(c);
                }
            }

            int previousThreshold = 0;
            for (int threshold : thresholds) {
                int potAmount = 0;
                List<Integer> eligibleIndices = new ArrayList<>();

                for (int i = 0; i < totalContributions.length; i++) {
                    int currentSlice = Math.min(totalContributions[i], threshold)
                        - Math.min(totalContributions[i], previousThreshold);
                    if (currentSlice > 0) {
                        potAmount += currentSlice;
                    }
                    if (totalContributions[i] >= threshold && !folded[i]) {
                        eligibleIndices.add(i);
                    }
                }

                if (potAmount > 0 && !eligibleIndices.isEmpty()) {
                    sidePots.add(new SidePot(potAmount, eligibleIndices));
                }
                previousThreshold = threshold;
            }

            return sidePots;
        }

        private ShowdownOutcome resolveShowdownForEligible(List<Integer> eligibleIndices, Card[] board) {
            List<Integer> winnerIndices = new ArrayList<>();
            int bestRank = 10;
            Card[] absoluteBest = null;

            for (int idx : eligibleIndices) {
                Card[] best = deck.getBestHand(bots.get(idx).getHand(), board);
                int rank = deck.getRanking(best);

                if (rank < bestRank) {
                    bestRank = rank;
                    winnerIndices.clear();
                    winnerIndices.add(idx);
                    absoluteBest = best;
                } else if (rank == bestRank) {
                    if (absoluteBest == null) {
                        winnerIndices.add(idx);
                        absoluteBest = best;
                    } else {
                        int comparison = deck.compareHands(absoluteBest, best);
                        if (comparison == 2) { // 'best' is better
                            winnerIndices.clear();
                            winnerIndices.add(idx);
                            absoluteBest = best;
                        } else if (comparison == 0) { // Exact tie
                            winnerIndices.add(idx);
                        }
                    }
                }
            }

            return new ShowdownOutcome(winnerIndices, bestRank);
        }

        private int[] getHeadsUpSmartGodPair(boolean[] folded) {
            int first = -1;
            int second = -1;

            for (int i = 0; i < folded.length; i++) {
                if (folded[i]) continue;
                if (first == -1) first = i;
                else if (second == -1) second = i;
                else return new int[] { -1, -1 };
            }

            if (first == -1 || second == -1) return new int[] { -1, -1 };

            int firstLevel = bots.get(first).getBotLevel();
            int secondLevel = bots.get(second).getBotLevel();

            if (firstLevel == 1 && secondLevel == 2) return new int[] { first, second };
            if (firstLevel == 2 && secondLevel == 1) return new int[] { second, first };
            return new int[] { -1, -1 };
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
