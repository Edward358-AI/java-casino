import java.util.*;
import java.util.concurrent.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PokerSimulator {
    private static Scanner sc = Player.sc;
    private static int dumbCount = 0, smartCount = 0, godCount = 1;
    private static boolean isProtectedMode = true;
    private static boolean isNeuralProtectedMode = true;
    private static int nightmareIntensity = 2;
    private static boolean parallelEnabled = false;
    private static int parallelThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
    public static double ltmAlpha = 0.001;
    // Phase 10: Archetype Bot counts
    private static int arcNitCount = 0, arcManiacCount = 0, arcStationCount = 0, arcTagCount = 0;
    private static int arcWhaleCount = 0, arcFishCount = 0, arcBullyCount = 0, arcShortStackerCount = 0;
    private static int arcLagCount = 0;
    private static int arcEliteRegCount = 0; // ELITE_REG = God Bot in pureProtectedGtoMode
    private static final String MODE6_TELEMETRY_OUTPUT_FILE = "mode6_telemetry_output.txt";
    private static final String MODE7_TELEMETRY_OUTPUT_FILE = "mode7_telemetry_output.txt";

    public static void main(String[] args) {
        setup();
        while (true) {
            System.out.println("\n--- POKER SIMULATOR MENU ---");
            System.out.println(
                    "[Parallel Mode: " + (parallelEnabled ? ("ON @ " + parallelThreads + " threads") : "OFF") + "]");
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
            if (choice == 9)
                break;

            switch (choice) {
                case 1:
                    runOneHand();
                    break;
                case 2:
                    runOneGame();
                    break;
                case 3:
                    runNHands();
                    break;
                case 4:
                    runNGames();
                    break;
                case 5:
                    runMacroReplacement();
                    break;
                case 6:
                    runDuplicateDuel();
                    break;
                case 7:
                    runIndependentHands();
                    break;
                case 8:
                    setup();
                    break;
            }
        }
    }

    private static int getArchetypeCount() {
        return arcNitCount + arcManiacCount + arcStationCount + arcTagCount +
                arcWhaleCount + arcFishCount + arcBullyCount + arcShortStackerCount + arcLagCount + arcEliteRegCount;
    }

    private static void setup() {
        while (true) {
            System.out.println("\n--- GAME SETUP ---");
            System.out.println("[1] Dumb Bots: " + dumbCount);
            System.out.println("[2] Smart Bots: " + smartCount);
            System.out.println("[3] God Bots: " + godCount);
            System.out.println("[4] Edit Archetype Bots (Total: " + getArchetypeCount() + ")");
            System.out.println("[5] LTM Alpha: " + ltmAlpha);
            System.out.println("[6] Protected Mode: " + (isProtectedMode ? "ON" : "OFF"));
            System.out.println("[7] Neural Protected Mode: " + (isNeuralProtectedMode ? "ON" : "OFF"));
            System.out.println("[8] Parallel Mode: " + (parallelEnabled ? "ON (" + parallelThreads + " threads)" : "OFF"));
            System.out.println("[9] Done");

            int choice = Player.getValidInt("Select setting to change (1-9): ", 1, 9);
            
            if (choice == 1) {
                dumbCount = Player.getValidInt("How many Dumb Bots? (0-12): ", 0, 12);
            } else if (choice == 2) {
                smartCount = Player.getValidInt("How many Smart Bots? (0-12): ", 0, 12);
            } else if (choice == 3) {
                godCount = Player.getValidInt("How many God Bots? (0-12): ", 0, 12);
            } else if (choice == 4) {
                System.out.println("\n--- ARCHETYPE BOTS SETUP ---");
                arcNitCount = Player.getValidInt("  NIT bots: ", 0, 12);
                arcManiacCount = Player.getValidInt("  MANIAC bots: ", 0, 12);
                arcStationCount = Player.getValidInt("  STATION bots: ", 0, 12);
                arcTagCount = Player.getValidInt("  TAG bots: ", 0, 12);
                arcWhaleCount = Player.getValidInt("  WHALE bots: ", 0, 12);
                arcFishCount = Player.getValidInt("  FISH bots: ", 0, 12);
                arcBullyCount = Player.getValidInt("  BULLY bots: ", 0, 12);
                arcLagCount = Player.getValidInt("  LAG bots: ", 0, 12);
                arcShortStackerCount = Player.getValidInt("  SHORT_STACKER bots: ", 0, 12);
                arcEliteRegCount = Player.getValidInt("  ELITE_REG bots: ", 0, 12);
            } else if (choice == 5) {
                ltmAlpha = Player.getValidDouble("LTM Alpha value (0.001 - 0.5): ", 0.001, 0.5, 0.01);
            } else if (choice == 6) {
                System.out.print("Enable Protected Mode? [y/N]: ");
                isProtectedMode = sc.nextLine().equalsIgnoreCase("y");
                if (!isProtectedMode) isNeuralProtectedMode = false;
            } else if (choice == 7) {
                if (!isProtectedMode) {
                    System.out.println("Neural Protected Mode is unavailable while Protected Mode is OFF.");
                } else {
                    System.out.print("Enable Neural Protected Mode? [y/N]: ");
                    isNeuralProtectedMode = sc.nextLine().equalsIgnoreCase("y");
                }
            } else if (choice == 8) {
                System.out.print("Enable Parallel Mode? [y/N]: ");
                parallelEnabled = sc.nextLine().equalsIgnoreCase("y");
                if (parallelEnabled) {
                    int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
                    parallelThreads = Player.getValidInt("Thread count (1-" + maxThreads + "): ", 1, maxThreads);
                } else {
                    parallelThreads = 1;
                }
            } else if (choice == 9) {
                int totalBots = dumbCount + smartCount + godCount + getArchetypeCount();
                if (totalBots < 2) {
                    System.out.println("Wait, you need at least 2 bots to play! Please add more bots.");
                } else if (totalBots > 12) {
                    System.out.println("Warning: You have " + totalBots + " bots configured. Maximum is 12. Please reduce bot counts.");
                } else {
                    nightmareIntensity = 2; // Always ensure this defaults to 2
                    break;
                }
            }
        }
    }

    private static class SimulationTotals {
        private final int[] gameWins = new int[4];
        private final int[] gameBusts = new int[4];
        private final long[] totalNetGold = new long[4];
        private final long[] tierNetProfits = new long[4];
        private final long[] tierFoldoutNetProfits = new long[4];
        private final long[] tierShowdownNetProfits = new long[4];
        private final double[] tierProfitSquares = new double[4];
        private final double[] tierAievNetProfits = new double[4];
        private final double[] tierAievProfitSquares = new double[4];
        private final long[] tierAievHands = new long[4];
        private final long[] tierHands = new long[4];
        private final int[] handWins = new int[4];
        private final CognitiveTelemetryAggregate cognitiveTelemetry = new CognitiveTelemetryAggregate();
        private final Map<String, Double> godOpponentNet = new HashMap<>();
        private final Map<String, Integer> playerLevelsByName = new HashMap<>();
        private final Map<String, Integer> archetypeShiftCounts = new HashMap<>();
        private final Map<String, List<Integer>> archetypeShiftHands = new HashMap<>();
        private double godVsDumbNet = 0.0;
        private double godVsSmartNet = 0.0;
        private double godVsArcNet = 0.0;
        private long convergenceSamples = 0;
        private double convergenceAbsVpipErrSum = 0.0;
        private double convergenceAbsPfrErrSum = 0.0;
        private long transferValidationHands = 0;
        private double transferAbsSeatErrorSum = 0.0;
        private double transferMaxSeatError = 0.0;
        private double transferAbsConservationErrorSum = 0.0;
        private double transferMaxConservationError = 0.0;
        private long aievAdjustedHands = 0;
        private long totalHandsProcessed = 0;

        void absorbEngine(SimEngine engine, Integer gameWinnerLevel) {
            if (gameWinnerLevel != null && gameWinnerLevel >= 0) {
                gameWins[gameWinnerLevel]++;
            }

            int[] busts = engine.getBusts();
            long[] netGold = engine.getTierNetGold();
            long[] handProfits = engine.getTierNetProfits();
            long[] foldoutProfits = engine.getTierFoldoutNetProfits();
            long[] showdownProfits = engine.getTierShowdownNetProfits();
            double[] handSquares = engine.getTierProfitSquares();
            double[] aievProfits = engine.getTierAievNetProfits();
            double[] aievSquares = engine.getTierAievProfitSquares();
            long[] aievHands = engine.getTierAievHandsPlayed();
            long[] handCounts = engine.getTierHandsPlayed();
            int[] wins = engine.getWinsByTier();

            for (int lvl = 0; lvl < 4; lvl++) {
                gameBusts[lvl] += busts[lvl];
                totalNetGold[lvl] += netGold[lvl];
                tierNetProfits[lvl] += handProfits[lvl];
                tierFoldoutNetProfits[lvl] += foldoutProfits[lvl];
                tierShowdownNetProfits[lvl] += showdownProfits[lvl];
                tierProfitSquares[lvl] += handSquares[lvl];
                tierAievNetProfits[lvl] += aievProfits[lvl];
                tierAievProfitSquares[lvl] += aievSquares[lvl];
                tierAievHands[lvl] += aievHands[lvl];
                tierHands[lvl] += handCounts[lvl];
                handWins[lvl] += wins[lvl];
            }

            godVsDumbNet += engine.getGodVsDumbNet();
            godVsSmartNet += engine.getGodVsSmartNet();
            godVsArcNet += engine.getGodVsArcNet();
            convergenceSamples += engine.getConvergenceSamples();
            convergenceAbsVpipErrSum += engine.getConvergenceAbsVpipErrSum();
            convergenceAbsPfrErrSum += engine.getConvergenceAbsPfrErrSum();
            transferValidationHands += engine.getTransferValidationHands();
            transferAbsSeatErrorSum += engine.getTransferAbsSeatErrorSum();
            transferMaxSeatError = Math.max(transferMaxSeatError, engine.getTransferMaxSeatError());
            transferAbsConservationErrorSum += engine.getTransferAbsConservationErrorSum();
            transferMaxConservationError = Math.max(transferMaxConservationError,
                    engine.getTransferMaxConservationError());

            mergeDoubleMap(godOpponentNet, engine.getGodOpponentNetSnapshot());

            Map<String, Integer> levelSnapshot = engine.getPlayerLevelByNameSnapshot();
            for (Map.Entry<String, Integer> entry : levelSnapshot.entrySet()) {
                playerLevelsByName.putIfAbsent(entry.getKey(), entry.getValue());
            }

            mergeCountMap(archetypeShiftCounts, engine.getArchetypeShiftCountsSnapshot());
            mergeShiftHands(archetypeShiftHands, engine.getArchetypeShiftHandsSnapshot());

            aievAdjustedHands += engine.getAievAdjustedHands();
            totalHandsProcessed += engine.getHandCount();
        }

        void absorbCognitiveSnapshot(Map<String, PokerBot.CognitiveProfile> profileMap) {
            cognitiveTelemetry.absorbProfiles(profileMap);
        }

        void merge(SimulationTotals other) {
            for (int lvl = 0; lvl < 4; lvl++) {
                gameWins[lvl] += other.gameWins[lvl];
                gameBusts[lvl] += other.gameBusts[lvl];
                totalNetGold[lvl] += other.totalNetGold[lvl];
                tierNetProfits[lvl] += other.tierNetProfits[lvl];
                tierFoldoutNetProfits[lvl] += other.tierFoldoutNetProfits[lvl];
                tierShowdownNetProfits[lvl] += other.tierShowdownNetProfits[lvl];
                tierProfitSquares[lvl] += other.tierProfitSquares[lvl];
                tierAievNetProfits[lvl] += other.tierAievNetProfits[lvl];
                tierAievProfitSquares[lvl] += other.tierAievProfitSquares[lvl];
                tierAievHands[lvl] += other.tierAievHands[lvl];
                tierHands[lvl] += other.tierHands[lvl];
                handWins[lvl] += other.handWins[lvl];
            }
            cognitiveTelemetry.merge(other.cognitiveTelemetry);
            mergeDoubleMap(godOpponentNet, other.godOpponentNet);
            for (Map.Entry<String, Integer> entry : other.playerLevelsByName.entrySet()) {
                playerLevelsByName.putIfAbsent(entry.getKey(), entry.getValue());
            }
            mergeCountMap(archetypeShiftCounts, other.archetypeShiftCounts);
            mergeShiftHands(archetypeShiftHands, other.archetypeShiftHands);
            godVsDumbNet += other.godVsDumbNet;
            godVsSmartNet += other.godVsSmartNet;
            godVsArcNet += other.godVsArcNet;
            convergenceSamples += other.convergenceSamples;
            convergenceAbsVpipErrSum += other.convergenceAbsVpipErrSum;
            convergenceAbsPfrErrSum += other.convergenceAbsPfrErrSum;
            transferValidationHands += other.transferValidationHands;
            transferAbsSeatErrorSum += other.transferAbsSeatErrorSum;
            transferMaxSeatError = Math.max(transferMaxSeatError, other.transferMaxSeatError);
            transferAbsConservationErrorSum += other.transferAbsConservationErrorSum;
            transferMaxConservationError = Math.max(transferMaxConservationError, other.transferMaxConservationError);
            aievAdjustedHands += other.aievAdjustedHands;
            totalHandsProcessed += other.totalHandsProcessed;
        }

        private void mergeDoubleMap(Map<String, Double> target, Map<String, Double> incoming) {
            if (incoming == null || incoming.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Double> entry : incoming.entrySet()) {
                target.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        private void mergeCountMap(Map<String, Integer> target, Map<String, Integer> incoming) {
            if (incoming == null || incoming.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : incoming.entrySet()) {
                target.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        private void mergeShiftHands(Map<String, List<Integer>> target, Map<String, List<Integer>> incoming) {
            if (incoming == null || incoming.isEmpty()) {
                return;
            }
            for (Map.Entry<String, List<Integer>> entry : incoming.entrySet()) {
                List<Integer> values = target.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                values.addAll(entry.getValue());
                Collections.sort(values);
                if (values.size() > 24) {
                    values.subList(24, values.size()).clear();
                }
            }
        }
    }

    private static class CognitiveTelemetryAggregate {
        private long profilesTracked = 0;
        private long weightedHands = 0;
        private double vpipWeightedSum = 0.0;
        private double pfrWeightedSum = 0.0;
        private double afqFlopWeightedSum = 0.0;
        private double afqTurnWeightedSum = 0.0;
        private double afqRiverWeightedSum = 0.0;
        private double wtsdWeightedSum = 0.0;
        private double foldToCbetWeightedSum = 0.0;
        private double vIndexWeightedSum = 0.0;
        private double styleShiftWeightedSum = 0.0;
        private long nitCount = 0;
        private long stationCount = 0;
        private long maniacCount = 0;
        private long tagCount = 0;
        private long whaleCount = 0;
        private long fishCount = 0;
        private long bullyCount = 0;
        private long lagCount = 0;
        private long shortStackerCount = 0;
        private long eliteRegCount = 0;
        private long unknownCount = 0;


        void absorbProfiles(Map<String, PokerBot.CognitiveProfile> profileMap) {
            if (profileMap == null || profileMap.isEmpty()) {
                return;
            }
            for (PokerBot.CognitiveProfile profile : profileMap.values()) {
                absorbProfile(profile);
            }
        }

        private void absorbProfile(PokerBot.CognitiveProfile profile) {
            if (profile == null || profile.handsPlayed <= 0) {
                return;
            }

            long handWeight = Math.max(1, profile.handsPlayed);
            profilesTracked++;
            weightedHands += handWeight;

            vpipWeightedSum += profile.vpipEMA * handWeight;
            pfrWeightedSum += profile.pfrEMA * handWeight;
            afqFlopWeightedSum += profile.afqFlopEMA * handWeight;
            afqTurnWeightedSum += profile.afqTurnEMA * handWeight;
            afqRiverWeightedSum += profile.afqRiverEMA * handWeight;
            wtsdWeightedSum += profile.wtsdEMA * handWeight;
            foldToCbetWeightedSum += profile.foldToCbetEMA * handWeight;
            vIndexWeightedSum += profile.vIndex * handWeight;
            styleShiftWeightedSum += profile.styleShiftEMA * handWeight;

            PokerBot.CognitiveArchetype archetype = profile.getArchetype();
            if (archetype == PokerBot.CognitiveArchetype.NIT) {
                nitCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.STATION) {
                stationCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.MANIAC) {
                maniacCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.TAG) {
                tagCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.WHALE) {
                whaleCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.FISH) {
                fishCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.BULLY) {
                bullyCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.LAG) {
                lagCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.SHORT_STACKER) {
                shortStackerCount++;
            } else if (archetype == PokerBot.CognitiveArchetype.ELITE_REG) {
                eliteRegCount++;
            } else {
                unknownCount++;
            }

        }

        void merge(CognitiveTelemetryAggregate other) {
            if (other == null) {
                return;
            }
            profilesTracked += other.profilesTracked;
            weightedHands += other.weightedHands;
            vpipWeightedSum += other.vpipWeightedSum;
            pfrWeightedSum += other.pfrWeightedSum;
            afqFlopWeightedSum += other.afqFlopWeightedSum;
            afqTurnWeightedSum += other.afqTurnWeightedSum;
            afqRiverWeightedSum += other.afqRiverWeightedSum;
            wtsdWeightedSum += other.wtsdWeightedSum;
            foldToCbetWeightedSum += other.foldToCbetWeightedSum;
            vIndexWeightedSum += other.vIndexWeightedSum;
            styleShiftWeightedSum += other.styleShiftWeightedSum;
            nitCount += other.nitCount;
            stationCount += other.stationCount;
            maniacCount += other.maniacCount;
            tagCount += other.tagCount;
            whaleCount += other.whaleCount;
            fishCount += other.fishCount;
            bullyCount += other.bullyCount;
            lagCount += other.lagCount;
            shortStackerCount += other.shortStackerCount;
            eliteRegCount += other.eliteRegCount;
            unknownCount += other.unknownCount;
        }

        boolean hasData() {
            return profilesTracked > 0 && weightedHands > 0;
        }

        private double mean(double weightedSum) {
            if (weightedHands <= 0) {
                return 0.0;
            }
            return weightedSum / weightedHands;
        }
    }

    private static class Mode7PairedStats {
        private final double[] duplicatePassNetProfits = new double[4];
        private final double[] duplicatePassProfitSquares = new double[4];
        private final long[] duplicatePassHands = new long[4];

        private final double[] combinedNetProfits = new double[4];
        private final double[] combinedProfitSquares = new double[4];
        private final long[] combinedHands = new long[4];

        private final Map<String, Double> duplicateGodOpponentNet = new HashMap<>();
        private final Map<String, Double> combinedGodOpponentNet = new HashMap<>();
        private final Map<String, Integer> playerLevelsByName = new HashMap<>();
        private double duplicateGodVsDumbNet = 0.0;
        private double duplicateGodVsSmartNet = 0.0;
        private double duplicateGodVsArcNet = 0.0;
        private double combinedGodVsDumbNet = 0.0;
        private double combinedGodVsSmartNet = 0.0;
        private double combinedGodVsArcNet = 0.0;

        private long pairDeals = 0;

        void addPairSamples(
                long[] rawA,
                long[] rawB,
                double[] aievA,
                double[] aievB,
                double handGodVsDumbA,
                double handGodVsDumbB,
                double handGodVsSmartA,
                double handGodVsSmartB,
                double handGodVsArcA,
                double handGodVsArcB,
                Map<String, Double> handGodOpponentNetA,
                Map<String, Double> handGodOpponentNetB,
                Map<String, Integer> levelsA,
                Map<String, Integer> levelsB) {
            for (int lvl = 0; lvl < 4; lvl++) {
                double duplicateSample = (rawA[lvl] + rawB[lvl]) * 0.5;
                duplicatePassNetProfits[lvl] += duplicateSample;
                duplicatePassProfitSquares[lvl] += duplicateSample * duplicateSample;
                duplicatePassHands[lvl]++;

                double combinedSample = (aievA[lvl] + aievB[lvl]) * 0.5;
                combinedNetProfits[lvl] += combinedSample;
                combinedProfitSquares[lvl] += combinedSample * combinedSample;
                combinedHands[lvl]++;
            }

            double duplicateDumbSample = (handGodVsDumbA + handGodVsDumbB) * 0.5;
            double duplicateSmartSample = (handGodVsSmartA + handGodVsSmartB) * 0.5;
            double duplicateArcSample = (handGodVsArcA + handGodVsArcB) * 0.5;
            duplicateGodVsDumbNet += duplicateDumbSample;
            duplicateGodVsSmartNet += duplicateSmartSample;
            duplicateGodVsArcNet += duplicateArcSample;

            // Source attribution is transfer-based and remains stable under AIEV runout
            // normalization.
            combinedGodVsDumbNet += duplicateDumbSample;
            combinedGodVsSmartNet += duplicateSmartSample;
            combinedGodVsArcNet += duplicateArcSample;

            mergeAveragedMapSample(duplicateGodOpponentNet, handGodOpponentNetA, handGodOpponentNetB);
            mergeAveragedMapSample(combinedGodOpponentNet, handGodOpponentNetA, handGodOpponentNetB);

            absorbLevels(levelsA);
            absorbLevels(levelsB);
            pairDeals++;
        }

        void merge(Mode7PairedStats other) {
            for (int lvl = 0; lvl < 4; lvl++) {
                duplicatePassNetProfits[lvl] += other.duplicatePassNetProfits[lvl];
                duplicatePassProfitSquares[lvl] += other.duplicatePassProfitSquares[lvl];
                duplicatePassHands[lvl] += other.duplicatePassHands[lvl];

                combinedNetProfits[lvl] += other.combinedNetProfits[lvl];
                combinedProfitSquares[lvl] += other.combinedProfitSquares[lvl];
                combinedHands[lvl] += other.combinedHands[lvl];
            }
            duplicateGodVsDumbNet += other.duplicateGodVsDumbNet;
            duplicateGodVsSmartNet += other.duplicateGodVsSmartNet;
            combinedGodVsDumbNet += other.combinedGodVsDumbNet;
            combinedGodVsSmartNet += other.combinedGodVsSmartNet;
            mergeDoubleMap(duplicateGodOpponentNet, other.duplicateGodOpponentNet);
            mergeDoubleMap(combinedGodOpponentNet, other.combinedGodOpponentNet);
            absorbLevels(other.playerLevelsByName);
            pairDeals += other.pairDeals;
        }

        private void mergeAveragedMapSample(Map<String, Double> target, Map<String, Double> mapA,
                Map<String, Double> mapB) {
            Set<String> keys = new HashSet<>();
            if (mapA != null)
                keys.addAll(mapA.keySet());
            if (mapB != null)
                keys.addAll(mapB.keySet());

            for (String key : keys) {
                double vA = (mapA == null) ? 0.0 : mapA.getOrDefault(key, 0.0);
                double vB = (mapB == null) ? 0.0 : mapB.getOrDefault(key, 0.0);
                target.merge(key, (vA + vB) * 0.5, Double::sum);
            }
        }

        private void mergeDoubleMap(Map<String, Double> target, Map<String, Double> incoming) {
            if (incoming == null || incoming.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Double> entry : incoming.entrySet()) {
                target.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        private void absorbLevels(Map<String, Integer> incoming) {
            if (incoming == null || incoming.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : incoming.entrySet()) {
                playerLevelsByName.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    private static int getWorkerCount(int workload) {
        if (!parallelEnabled || workload <= 1) {
            return 1;
        }
        return Math.max(1, Math.min(parallelThreads, workload));
    }

    private static void emitLine(StringBuilder capture, String line) {
        System.out.println(line);
        if (capture != null) {
            capture.append(line).append('\n');
        }
    }

    private static void emitFormat(StringBuilder capture, String format, Object... args) {
        String text = String.format(format, args);
        System.out.print(text);
        if (capture != null) {
            capture.append(text);
        }
    }

    private static void appendTelemetryOutputToFile(String fileName, String telemetryText) {
        if (telemetryText == null || telemetryText.isEmpty()) {
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write("===== " + new Date() + " =====");
            bw.newLine();
            bw.write(telemetryText);
            if (!telemetryText.endsWith("\n")) {
                bw.newLine();
            }
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Warning: Failed to write telemetry output to " + fileName + ": " + e.getMessage());
        }
    }

    private static void runOneHand() {
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, true, isProtectedMode, nightmareIntensity);
        engine.runHand(1, true);
    }

    private static void runOneGame() {
        int maxHands = Player.getValidInt("Max hands for this game? (Enter for Last Man Standing)", 1, 1000000, false,
                true);
        if (maxHands <= 0)
            maxHands = Integer.MAX_VALUE;

        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
        engine.runGame(maxHands, true);
    }

    private static void runNHands() {
        int n = Player.getValidInt("How many hands to simulate?", 1, 1000000);
        if (n <= 0)
            n = Integer.MAX_VALUE;
        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode, nightmareIntensity);
        engine.runContinuous(n, false);
        engine.printFinalReport(null, true);
    }

    private static void runNGames() {
        int n = Player.getValidInt("How many games to simulate?", 1, 1000000);
        if (n <= 0)
            n = 1; // Safeguard

        int maxHands = Player.getValidInt("Max hands per game? (Enter for Last Man Standing)", 1, 1000000, false, true);
        if (maxHands <= 0)
            maxHands = Integer.MAX_VALUE;

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
        System.out.printf("Win Rates   -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%, Arc: %.1f%%\n",
                (totals.gameWins[0] * 100.0 / totalGames), (totals.gameWins[1] * 100.0 / totalGames),
                (totals.gameWins[2] * 100.0 / totalGames));
        System.out.printf("Bust Freq   -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G, Arc: %.2f/G\n",
                (double) totals.gameBusts[0] / totalGames, (double) totals.gameBusts[1] / totalGames,
                (double) totals.gameBusts[2] / totalGames);
        System.out.printf("Net Gold    -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d, Arc: %s✨%d\n",
                (totals.totalNetGold[0] >= 0 ? "+" : ""), totals.totalNetGold[0],
                (totals.totalNetGold[1] >= 0 ? "+" : ""), totals.totalNetGold[1],
                (totals.totalNetGold[2] >= 0 ? "+" : ""), totals.totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n",
                (double) (totals.gameBusts[0] + totals.gameBusts[1] + totals.gameBusts[2]) / totalGames);
        printAdvancedTierTelemetry("MACRO ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits,
                totals.tierProfitSquares, totals.tierHands, 20);
    }

    private static SimulationTotals runNGamesSequential(int n, int maxHands) {
        SimulationTotals totals = new SimulationTotals();
        PokerBot.resetThreadCognitiveDB();
        try {
            for (int i = 0; i < n; i++) {
                PokerBot.resetThreadCognitiveDB();
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                        nightmareIntensity);
                int winnerLevel = engine.runGame(maxHands, false);
                // System.out.println("Game #" + (i + 1) + " Winner: " + (winnerLevel == 0 ?
                // "Dumb" : (winnerLevel == 1 ? "Smart" : (winnerLevel == 2 ? "God" : "Arc"))));
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
                        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                                nightmareIntensity);
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
        System.out.printf("Session Win Rates (By Profit) -> Dumb: %.1f%%, Smart: %.1f%%, God: %.1f%%, Arc: %.1f%%\n",
                (totals.gameWins[0] * 100.0 / totalGames), (totals.gameWins[1] * 100.0 / totalGames),
                (totals.gameWins[2] * 100.0 / totalGames));
        System.out.printf("Bust Freq -> Dumb: %.2f/G, Smart: %.2f/G, God: %.2f/G, Arc: %.2f/G\n",
                (double) totals.gameBusts[0] / totalGames, (double) totals.gameBusts[1] / totalGames,
                (double) totals.gameBusts[2] / totalGames);
        System.out.printf("Total Net Gold -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d, Arc: %s✨%d\n",
                (totals.totalNetGold[0] >= 0 ? "+" : ""), totals.totalNetGold[0],
                (totals.totalNetGold[1] >= 0 ? "+" : ""), totals.totalNetGold[1],
                (totals.totalNetGold[2] >= 0 ? "+" : ""), totals.totalNetGold[2]);
        System.out.printf("Global Bust Frequency: %.2f busts/game\n",
                (double) (totals.gameBusts[0] + totals.gameBusts[1] + totals.gameBusts[2]) / totalGames);
        printAdvancedTierTelemetry("MACRO REPLACEMENT ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits,
                totals.tierProfitSquares, totals.tierHands, 20);
        if (isProtectedMode)
            System.out.println("Note: Simulation run in PROTECTED MODE (Exploits disabled).");
    }

    private static SimulationTotals runMacroReplacementSequential(int n, int handsPerGame) {
        SimulationTotals totals = new SimulationTotals();
        PokerBot.resetThreadCognitiveDB();
        try {
            for (int i = 0; i < n; i++) {
                PokerBot.resetThreadCognitiveDB();
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                        nightmareIntensity);
                engine.runContinuous(handsPerGame, false);
                int winnerLevel = engine.getWinningTierByProfit();
                // System.out.println("Replacement Game #" + (i + 1) + " Most Profitable: " +
                // (winnerLevel == 0 ? "Dumb" : (winnerLevel == 1 ? "Smart" : (winnerLevel == 2 ? "God" : "Arc"))));
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
                        SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                                nightmareIntensity);
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

    /** Maps user-facing archetype index (0-7) to enum constants, skipping ELITE_REG/UNKNOWN. */
    private static final PokerBot.CognitiveArchetype[] SELECTABLE_ARCHETYPES = {
        PokerBot.CognitiveArchetype.NIT,            // 0
        PokerBot.CognitiveArchetype.STATION,         // 1
        PokerBot.CognitiveArchetype.MANIAC,          // 2
        PokerBot.CognitiveArchetype.TAG,             // 3
        PokerBot.CognitiveArchetype.WHALE,           // 4
        PokerBot.CognitiveArchetype.FISH,            // 5
        PokerBot.CognitiveArchetype.BULLY,           // 6
        PokerBot.CognitiveArchetype.SHORT_STACKER,    // 7
        PokerBot.CognitiveArchetype.LAG               // 8
    };

    private static PokerBot.CognitiveArchetype promptArchetypeSelection(String label) {
        System.out.println("Select Archetype for " + label + " (0:NIT, 1:STATION, 2:MANIAC, 3:TAG, 4:WHALE, 5:FISH, 6:BULLY, 7:SHORT_STACKER, 8:LAG)");
        int idx = Player.getValidInt("Archetype: ", 0, 8);
        return SELECTABLE_ARCHETYPES[idx];
    }

    private static void runDuplicateDuel() {
        System.out.println("\n--- DUPLICATE DUEL: THE ULTIMATE SKILL TEST ---");
        System.out.println("Select two bot tiers to compete (0:Dumb, 1:Smart, 2:God, 3:Archetype)");
        int t1 = Player.getValidInt("Tier for Bot A: ", 0, 3);
        PokerBot.CognitiveArchetype arc1 = null;
        if (t1 == 3) {
            arc1 = promptArchetypeSelection("Bot A");
        }

        int t2 = Player.getValidInt("Tier for Bot B: ", 0, 3);
        PokerBot.CognitiveArchetype arc2 = null;
        if (t2 == 3) {
            arc2 = promptArchetypeSelection("Bot B");
        }

        int pairs = Player.getValidInt("Number of duplicate pairs to simulate?", 1, 1000000);

        long t1TotalGold = 0;
        long t2TotalGold = 0;
        int t1Busts = 0, t2Busts = 0;
        int t1Wins = 0, t2Wins = 0;
        double t1ProfitSquares = 0;
        double t2ProfitSquares = 0;

        int pairCount = 0;
        SimEngine engine = new SimEngine(new int[] { t1, t2 }, new PokerBot.CognitiveArchetype[] { arc1, arc2 }, false, isProtectedMode, nightmareIntensity);

        engine.setPerHandNeuralResetEnabled(false);
        PokerBot botA = engine.bots.get(0);
        PokerBot botB = engine.bots.get(1);

        System.out.println("Executing " + pairs + " duplicate pairs...");

        while (pairCount < pairs) {
            pairCount++;
            // Preserve model memory across pairs while resetting chip state.
            // Canonicalize order so pass-1 accounting remains Bot A index 0 / Bot B index
            // 1.
            if (engine.bots.get(0) != botA) {
                PokerBot tempBot = engine.bots.get(0);
                engine.bots.set(0, engine.bots.get(1));
                engine.bots.set(1, tempBot);
            }

            // Stack assignment: SHORT_STACKER bots start with 499 chips (24.95 BB) — the
            // maximum that still falls under the classifier's currentStackBB < 25.0 threshold.
            // Without this, SS bots would have 100 BB stacks and their "always shove" strategy
            // would be wildly −EV for them; the duel becomes meaningless. Per-pair starting
            // chips are also recorded so the chip-delta accounting matches each bot's stack.
            int[] startingChips = new int[engine.bots.size()];
            for (int i = 0; i < engine.bots.size(); i++) {
                PokerBot b = engine.bots.get(i);
                int starting = (b.simulatedArchetype == PokerBot.CognitiveArchetype.SHORT_STACKER)
                    ? 499
                    : 2000;
                b.removeChips(b.getChips());
                b.addChips(starting);
                startingChips[i] = starting;
            }

            PokerDeck tempDeck = new PokerDeck();
            Card[][] holeCards = tempDeck.deal(2);
            Card[] board = tempDeck.deal();

            // Pass 1: Bot A at 0, Bot B at 1
            engine.setDiagnosticsContextLabels("MODE6", "PASS_A", -1);
            int winner1 = engine.runHand(1, false, false, holeCards, board);
            int p1A = engine.bots.get(0).getChips() - startingChips[0];
            int p1B = engine.bots.get(1).getChips() - startingChips[1];
            t1TotalGold += p1A;
            t2TotalGold += p1B;
            t1ProfitSquares += (double) p1A * p1A;
            t2ProfitSquares += (double) p1B * p1B;
            if (engine.bots.get(0).getChips() == 0)
                t1Busts++;
            if (engine.bots.get(1).getChips() == 0)
                t2Busts++;
            if (winner1 == 0)
                t1Wins++;
            else if (winner1 == 1)
                t2Wins++;
            else if (winner1 == -1) {
                t1Wins++;
                t2Wins++;
            }

            // Pass 2: RESET AND SWAP PHYSICALLY
            // Swap Bot A and Bot B in the bots list to cancel any Slot-0 bias
            PokerBot tempBot = engine.bots.get(0);
            engine.bots.set(0, engine.bots.get(1));
            engine.bots.set(1, tempBot);

            // Pass 2 stack reset — same SS-aware logic as pass 1.
            int[] startingChipsPass2 = new int[engine.bots.size()];
            for (int i = 0; i < engine.bots.size(); i++) {
                PokerBot b = engine.bots.get(i);
                int starting = (b.simulatedArchetype == PokerBot.CognitiveArchetype.SHORT_STACKER)
                    ? 499
                    : 2000;
                b.removeChips(b.getChips());
                b.addChips(starting);
                startingChipsPass2[i] = starting;
            }
            // Use the original dealt holes with swapped bot order so identities swap
            // private cards,
            // while id=1 ensures they also alternate SB/BB once per pair.
            Card[][] pass2HoleCards = buildDuelPass2HoleCards(holeCards, engine.bots, botA, botB);
            engine.setDiagnosticsContextLabels("MODE6", "PASS_B", -1);
            int winner2 = engine.runHand(1, false, false, pass2HoleCards, board);

            // Note: In Pass 2, engine.bots.get(0) is now Bot B, and bots.get(1) is now Bot
            // A
            int p2B = engine.bots.get(0).getChips() - startingChipsPass2[0];
            int p2A = engine.bots.get(1).getChips() - startingChipsPass2[1];
            t2TotalGold += p2B;
            t1TotalGold += p2A;
            t2ProfitSquares += (double) p2B * p2B;
            t1ProfitSquares += (double) p2A * p2A;
            if (engine.bots.get(0).getChips() == 0)
                t2Busts++;
            if (engine.bots.get(1).getChips() == 0)
                t1Busts++;
            if (winner2 == 0)
                t2Wins++;
            else if (winner2 == 1)
                t1Wins++;
            else if (winner2 == -1) {
                t1Wins++;
                t2Wins++;
            }

            // Restore canonical order for next pair.
            if (engine.bots.get(0) != botA) {
                PokerBot swapBack = engine.bots.get(0);
                engine.bots.set(0, engine.bots.get(1));
                engine.bots.set(1, swapBack);
            }
        }

        String name1 = (t1 == 0 ? "Dumb" : (t1 == 1 ? "Smart" : (t1 == 2 ? "God" : (arc1 != null ? "Arc-" + arc1.name() : "Arc"))));
        String name2 = (t2 == 0 ? "Dumb" : (t2 == 1 ? "Smart" : (t2 == 2 ? "God" : (arc2 != null ? "Arc-" + arc2.name() : "Arc"))));
        StringBuilder mode6TelemetryOutput = new StringBuilder();

        emitLine(mode6TelemetryOutput, "\n--- DUPLICATE DUEL RESULTS (" + pairs + " pairs) ---");
        emitLine(mode6TelemetryOutput, "Bot A [" + name1 + "] vs Bot B [" + name2 + "]");
        int totalHands = pairs * 2;
        emitFormat(mode6TelemetryOutput,
                "Bot A [%s] -> Win Rate: %.1f%% (%d) | Bust Rate: %.2f%% (%d) | Net Advantage: %s✨%d\n",
                name1, (t1Wins * 100.0 / totalHands), t1Wins, (t1Busts * 100.0 / totalHands), t1Busts,
                (t1TotalGold >= 0 ? "+" : ""), t1TotalGold);
        emitFormat(mode6TelemetryOutput,
                "Bot B [%s] -> Win Rate: %.1f%% (%d) | Bust Rate: %.2f%% (%d) | Net Advantage: %s✨%d\n",
                name2, (t2Wins * 100.0 / totalHands), t2Wins, (t2Busts * 100.0 / totalHands), t2Busts,
                (t2TotalGold >= 0 ? "+" : ""), t2TotalGold);

        double t1MeanProfit = (double) t1TotalGold / totalHands;
        double t2MeanProfit = (double) t2TotalGold / totalHands;
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
        double t1StdevBb100 = (t1Stdev / 20.0) * 100.0;
        double t2StdevBb100 = (t2Stdev / 20.0) * 100.0;
        emitLine(mode6TelemetryOutput, "\n--- DUPLICATE DUEL ADVANCED TELEMETRY ---");
        emitFormat(mode6TelemetryOutput, "Bot A [%s] BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name1, t1Bb100,
                t1Bb100 - t1CiBb100, t1Bb100 + t1CiBb100);
        emitFormat(mode6TelemetryOutput, "Bot B [%s] BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name2, t2Bb100,
                t2Bb100 - t2CiBb100, t2Bb100 + t2CiBb100);
        emitLine(mode6TelemetryOutput, "\n--- DUPLICATE DUEL STANDARD DEVIATION ---");
        emitFormat(mode6TelemetryOutput, "Bot A [%s] StdDev -> %.2f chips/hand | %.2f BB/100\n", name1, t1Stdev,
                t1StdevBb100);
        emitFormat(mode6TelemetryOutput, "Bot B [%s] StdDev -> %.2f chips/hand | %.2f BB/100\n", name2, t2Stdev,
                t2StdevBb100);

        long diff = t1TotalGold - t2TotalGold;
        if (diff > 0) {
            emitLine(mode6TelemetryOutput,
                    "\n🏆 SKILL LEADER: Bot A [" + name1 + "] outperformed Bot B [" + name2 + "] by ✨" + diff);
        } else if (diff < 0) {
            emitLine(mode6TelemetryOutput,
                    "\n🏆 SKILL LEADER: Bot B [" + name2 + "] outperformed Bot A [" + name1 + "] by ✨" + (-diff));
        } else {
            emitLine(mode6TelemetryOutput, "\n🤝 DRAW: Perfect parity across both sessions.");
        }

        CognitiveTelemetryAggregate mode6CognitiveTelemetry = new CognitiveTelemetryAggregate();
        mode6CognitiveTelemetry.absorbProfiles(PokerBot.getCognitiveDB());
        printCognitiveTelemetry(
                "PHASE A COGNITIVE TELEMETRY",
                mode6CognitiveTelemetry,
                mode6TelemetryOutput);
        printPhaseBCognitiveDiagnostics(
                "PHASE B COGNITIVE DIAGNOSTICS",
                mode6CognitiveTelemetry,
                mode6TelemetryOutput);

        if (isProtectedMode)
            emitLine(mode6TelemetryOutput, "Note: Duel conducted in PROTECTED MODE (Exploits disabled).");

        appendTelemetryOutputToFile(MODE6_TELEMETRY_OUTPUT_FILE, mode6TelemetryOutput.toString());
    }

    private static void runIndependentHands() {
        StringBuilder mode7TelemetryOutput = new StringBuilder();
        emitLine(mode7TelemetryOutput, "\n--- TRUE EV MODE: INDIVIDUAL HANDS ---");
        System.out.print("Enable Mode 7 single-hand mode? (Mode 1 style hand flow) [y/N]: ");
        String singleModeInput = sc.nextLine().trim();
        if (singleModeInput.equalsIgnoreCase("y")) {
            runIndependentHandsSingleMode();
            return;
        }

        int n = Player.getValidInt("How many independent hands to simulate?", 1, 10000000);

        int workers = getWorkerCount(n);

        long baselineStartNs = System.nanoTime();
        SimulationTotals totals = runIndependentHandsBaseline(n, workers);
        double baselineElapsedSec = (System.nanoTime() - baselineStartNs) / 1_000_000_000.0;

        long pairedStartNs = System.nanoTime();
        Mode7PairedStats pairedStats = runIndependentHandsPaired(n, workers);
        double pairedElapsedSec = (System.nanoTime() - pairedStartNs) / 1_000_000_000.0;

        if (workers > 1) {
            emitFormat(mode7TelemetryOutput, "Parallel workers: %d | Baseline runtime: %.2fs | Paired runtime: %.2fs\n",
                    workers, baselineElapsedSec, pairedElapsedSec);
        } else {
            emitFormat(mode7TelemetryOutput, "Runtime -> Baseline: %.2fs | Paired: %.2fs\n", baselineElapsedSec,
                    pairedElapsedSec);
        }

        printAggregatedTrueEvReport(n, totals, mode7TelemetryOutput);
        printCognitiveTelemetry(
                "PHASE A COGNITIVE TELEMETRY",
                totals.cognitiveTelemetry,
                mode7TelemetryOutput);
        printPhaseBCognitiveDiagnostics(
                "PHASE B COGNITIVE DIAGNOSTICS",
                totals.cognitiveTelemetry,
                mode7TelemetryOutput);
        printSourceOfProfitTelemetry(
                "SOURCE OF PROFIT (Tier Overall, Mode 7)",
                totals.tierFoldoutNetProfits,
                totals.tierShowdownNetProfits,
                totals.tierHands,
                totals.tierNetProfits,
                totals.tierAievNetProfits,
                20,
                mode7TelemetryOutput);
        printPhaseBMode7EdgeAttribution(
                "PHASE B EDGE ATTRIBUTION (Mode 7)",
                totals.tierFoldoutNetProfits,
                totals.tierShowdownNetProfits,
                totals.tierHands,
                totals.tierNetProfits,
                totals.tierAievNetProfits,
                20,
                mode7TelemetryOutput);
        printPhaseCConvergenceDiagnostics(
                "PHASE C COGNITIVE CONVERGENCE",
                totals,
                mode7TelemetryOutput);
        printPhaseCTransferIntegrity(
                "PHASE C TRANSFER INTEGRITY",
                totals,
                mode7TelemetryOutput);
        printPhaseCMode7SourceAttribution(
                "PHASE C SOURCE OF PROFIT (Baseline)",
                totals.godVsDumbNet,
                totals.godVsSmartNet,
                totals.godVsArcNet,
                totals.godOpponentNet,
                totals.playerLevelsByName,
                totals.totalHandsProcessed,
                20,
                mode7TelemetryOutput);
        printPhaseCShiftTimeline(
                "PHASE C SHIFT TIMELINE",
                totals.archetypeShiftCounts,
                totals.archetypeShiftHands,
                mode7TelemetryOutput);

        printAdvancedTierTelemetryDouble(
                "DUPLICATE PASS ADJUSTED TELEMETRY (Tier Overall)",
                pairedStats.duplicatePassNetProfits,
                pairedStats.duplicatePassProfitSquares,
                pairedStats.duplicatePassHands,
                20,
                mode7TelemetryOutput);
        printTierStdDevTelemetryDouble(
                "DUPLICATE PASS ADJUSTED STANDARD DEVIATION (Tier Overall)",
                pairedStats.duplicatePassNetProfits,
                pairedStats.duplicatePassProfitSquares,
                pairedStats.duplicatePassHands,
                20,
                mode7TelemetryOutput);

        printAdvancedTierTelemetryDouble(
                "COMBINED DUPLICATE PASS + AIEV ADJUSTED TELEMETRY (Tier Overall)",
                pairedStats.combinedNetProfits,
                pairedStats.combinedProfitSquares,
                pairedStats.combinedHands,
                20,
                mode7TelemetryOutput);
        printTierStdDevTelemetryDouble(
                "COMBINED DUPLICATE PASS + AIEV ADJUSTED STANDARD DEVIATION (Tier Overall)",
                pairedStats.combinedNetProfits,
                pairedStats.combinedProfitSquares,
                pairedStats.combinedHands,
                20,
                mode7TelemetryOutput);
        printPhaseCMode7SourceAttribution(
                "PHASE C SOURCE OF PROFIT (Duplicate Pass)",
                pairedStats.duplicateGodVsDumbNet,
                pairedStats.duplicateGodVsSmartNet,
                pairedStats.duplicateGodVsArcNet,
                pairedStats.duplicateGodOpponentNet,
                pairedStats.playerLevelsByName,
                pairedStats.pairDeals,
                20,
                mode7TelemetryOutput);
        printPhaseCMode7SourceAttribution(
                "PHASE C SOURCE OF PROFIT (Duplicate + AIEV Combined)",
                pairedStats.combinedGodVsDumbNet,
                pairedStats.combinedGodVsSmartNet,
                pairedStats.combinedGodVsArcNet,
                pairedStats.combinedGodOpponentNet,
                pairedStats.playerLevelsByName,
                pairedStats.pairDeals,
                20,
                mode7TelemetryOutput);
        printPhaseDMode7Overlay(
                "PHASE D OVERLAY (Baseline vs Paired)",
                totals.godVsDumbNet,
                totals.godVsSmartNet,
                totals.godVsArcNet,
                totals.totalHandsProcessed,
                pairedStats.duplicateGodVsDumbNet,
                pairedStats.duplicateGodVsSmartNet,
                pairedStats.duplicateGodVsArcNet,
                pairedStats.pairDeals,
                pairedStats.combinedGodVsDumbNet,
                pairedStats.combinedGodVsSmartNet,
                pairedStats.combinedGodVsArcNet,
                pairedStats.pairDeals,
                20,
                mode7TelemetryOutput);


        emitFormat(mode7TelemetryOutput, "Duplicate paired deals processed: %d\n", pairedStats.pairDeals);
        if (isProtectedMode) {
            emitLine(mode7TelemetryOutput, "Note: Simulation run in PROTECTED MODE (Exploits disabled).");
        }

        emitPhaseCRollupDiagnostics(totals, n);

        appendTelemetryOutputToFile(MODE7_TELEMETRY_OUTPUT_FILE, mode7TelemetryOutput.toString());
    }

    private static void runIndependentHandsSingleMode() {
        StringBuilder mode7TelemetryOutput = new StringBuilder();
        emitLine(mode7TelemetryOutput, "\n--- TRUE EV MODE: INDIVIDUAL HANDS (SINGLE MODE) ---");
        int maxHands = Player.getValidInt("Maximum independent hands to process?", 1, 10000000);

        SimulationTotals totals = new SimulationTotals();
        int processedHands = 0;
        boolean terminatedByUser = false;

        try {
            BotDiagnostics.setThreadForceEnabled(true);
            PokerBot.resetThreadCognitiveDB();
            try {
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, true, isProtectedMode,
                        nightmareIntensity, false);
                engine.setDiagnosticsContextLabels("MODE7", "BASELINE_SINGLE", 0);
                engine.setPerHandNeuralResetEnabled(false);
                engine.setAievEnabled(true);

                while (processedHands < maxHands) {
                    int handNumber = processedHands + 1;
                    emitLine(mode7TelemetryOutput, "\n=== MODE 7 SINGLE HAND #" + handNumber + " ===");
                    engine.runSingleTrueEvHand(true);
                    processedHands++;

                    System.out.println(
                            "Type \"t\" to terminate and return to the main menu, or press Enter to continue.");
                    String userInput = sc.nextLine().trim();
                    if (userInput.equalsIgnoreCase("t")) {
                        terminatedByUser = true;
                        break;
                    }
                }

                totals.absorbEngine(engine, null);
                totals.absorbCognitiveSnapshot(PokerBot.getCognitiveDB());
            } finally {
                PokerBot.clearThreadCognitiveDB();
            }
        } finally {
            BotDiagnostics.clearSimulatorContext();
            BotDiagnostics.clearThreadForceEnabled();
        }

        if (terminatedByUser) {
            emitLine(mode7TelemetryOutput, "Mode 7 single mode terminated by user after " + processedHands + " hands.");
        } else {
            emitLine(mode7TelemetryOutput, "Mode 7 single mode completed " + processedHands + " hands.");
        }

        printAggregatedTrueEvReport(processedHands, totals, mode7TelemetryOutput);
        printCognitiveTelemetry(
                "PHASE A COGNITIVE TELEMETRY",
                totals.cognitiveTelemetry,
                mode7TelemetryOutput);
        printPhaseBCognitiveDiagnostics(
                "PHASE B COGNITIVE DIAGNOSTICS",
                totals.cognitiveTelemetry,
                mode7TelemetryOutput);
        printSourceOfProfitTelemetry(
                "SOURCE OF PROFIT (Tier Overall, Mode 7)",
                totals.tierFoldoutNetProfits,
                totals.tierShowdownNetProfits,
                totals.tierHands,
                totals.tierNetProfits,
                totals.tierAievNetProfits,
                20,
                mode7TelemetryOutput);
        printPhaseBMode7EdgeAttribution(
                "PHASE B EDGE ATTRIBUTION (Mode 7)",
                totals.tierFoldoutNetProfits,
                totals.tierShowdownNetProfits,
                totals.tierHands,
                totals.tierNetProfits,
                totals.tierAievNetProfits,
                20,
                mode7TelemetryOutput);
        printPhaseCConvergenceDiagnostics(
                "PHASE C COGNITIVE CONVERGENCE",
                totals,
                mode7TelemetryOutput);
        printPhaseCTransferIntegrity(
                "PHASE C TRANSFER INTEGRITY",
                totals,
                mode7TelemetryOutput);
        printPhaseCMode7SourceAttribution(
                "PHASE C SOURCE OF PROFIT (Baseline)",
                totals.godVsDumbNet,
                totals.godVsSmartNet,
                totals.godVsArcNet,
                totals.godOpponentNet,
                totals.playerLevelsByName,
                totals.totalHandsProcessed,
                20,
                mode7TelemetryOutput);
        printPhaseCShiftTimeline(
                "PHASE C SHIFT TIMELINE",
                totals.archetypeShiftCounts,
                totals.archetypeShiftHands,
                mode7TelemetryOutput);

        if (isProtectedMode) {
            emitLine(mode7TelemetryOutput, "Note: Simulation run in PROTECTED MODE (Exploits disabled).");
        }

        emitPhaseCRollupDiagnostics(totals, processedHands);
        appendTelemetryOutputToFile(MODE7_TELEMETRY_OUTPUT_FILE, mode7TelemetryOutput.toString());
    }

    private static SimulationTotals runIndependentHandsBaseline(int n, int workers) {
        if (workers == 1) {
            SimulationTotals totals = new SimulationTotals();
            PokerBot.resetThreadCognitiveDB();
            try {
                SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                        nightmareIntensity, false);
                engine.setDiagnosticsContextLabels("MODE7", "BASELINE", 0);
                engine.setPerHandNeuralResetEnabled(false);
                engine.setAievEnabled(true);
                engine.runIndividualContinuous(n, false);
                totals.absorbEngine(engine, null);
                totals.absorbCognitiveSnapshot(PokerBot.getCognitiveDB());
            } finally {
                PokerBot.clearThreadCognitiveDB();
            }
            return totals;
        }

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<SimulationTotals>> futures = new ArrayList<>();
        int base = n / workers;
        int rem = n % workers;

        for (int w = 0; w < workers; w++) {
            int workerId = w;
            int handsForWorker = base + (w < rem ? 1 : 0);
            futures.add(executor.submit(() -> {
                SimulationTotals local = new SimulationTotals();
                PokerBot.resetThreadCognitiveDB();
                try {
                    SimEngine engine = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                            nightmareIntensity, false);
                    engine.setDiagnosticsContextLabels("MODE7", "BASELINE", workerId);
                    engine.setPerHandNeuralResetEnabled(false);
                    engine.setAievEnabled(true);
                    engine.runIndividualContinuous(handsForWorker, false);
                    local.absorbEngine(engine, null);
                    local.absorbCognitiveSnapshot(PokerBot.getCognitiveDB());
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

        return totals;
    }

    private static Mode7PairedStats runIndependentHandsPaired(int n, int workers) {
        if (workers == 1) {
            return runIndependentHandsPairedWorker(n, 0);
        }

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<Mode7PairedStats>> futures = new ArrayList<>();
        int base = n / workers;
        int rem = n % workers;

        for (int w = 0; w < workers; w++) {
            int workerId = w;
            int dealsForWorker = base + (w < rem ? 1 : 0);
            futures.add(executor.submit(() -> runIndependentHandsPairedWorker(dealsForWorker, workerId)));
        }

        Mode7PairedStats totals = new Mode7PairedStats();
        try {
            for (Future<Mode7PairedStats> f : futures) {
                totals.merge(f.get());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel duplicate-paired independent-hands simulation was interrupted", ie);
        } catch (ExecutionException ee) {
            throw new RuntimeException("Parallel duplicate-paired independent-hands simulation failed", ee.getCause());
        } finally {
            executor.shutdownNow();
        }

        return totals;
    }

    private static Mode7PairedStats runIndependentHandsPairedWorker(int pairDeals, int workerId) {
        Mode7PairedStats stats = new Mode7PairedStats();
        if (pairDeals <= 0) {
            return stats;
        }

        PokerBot.resetThreadCognitiveDB();
        try {
            SimEngine engineA = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                    nightmareIntensity, false);
            SimEngine engineB = new SimEngine(dumbCount, smartCount, godCount, false, isProtectedMode,
                    nightmareIntensity, false);
            engineA.setDiagnosticsContextLabels("MODE7", "PAIRED_PASS_A", workerId);
            engineB.setDiagnosticsContextLabels("MODE7", "PAIRED_PASS_B", workerId);
            engineA.setPerHandNeuralResetEnabled(false);
            engineB.setPerHandNeuralResetEnabled(false);
            engineA.setAievEnabled(true);
            engineB.setAievEnabled(true);

            // Mirror baseline seat order once; per-hand shared permutation is applied
            // afterward.
            Collections.reverse(engineB.bots);

            for (int hand = 1; hand <= pairDeals; hand++) {
                resetAllStacksTo20bb(engineA);
                resetAllStacksTo20bb(engineB);
                applySharedSeatPermutation(engineA, engineB);

                PokerDeck pairDeck = new PokerDeck();
                Card[][] holeCards = pairDeck.deal(engineA.bots.size());
                Card[][] swappedHoleCards = buildSwappedSeatHoleCards(holeCards);
                Card[] board = pairDeck.deal();

                engineA.runHand(hand, false, true, holeCards, board);
                engineB.runHand(hand, false, true, swappedHoleCards, board);

                stats.addPairSamples(
                        engineA.getLastHandTierRawProfits(),
                        engineB.getLastHandTierRawProfits(),
                        engineA.getLastHandTierAievProfits(),
                        engineB.getLastHandTierAievProfits(),
                        engineA.getLastHandGodVsDumbNet(),
                        engineB.getLastHandGodVsDumbNet(),
                        engineA.getLastHandGodVsSmartNet(),
                        engineB.getLastHandGodVsSmartNet(),
                        engineA.getLastHandGodVsArcNet(),
                        engineB.getLastHandGodVsArcNet(),
                        engineA.getLastHandGodOpponentNetSnapshot(),
                        engineB.getLastHandGodOpponentNetSnapshot(),
                        engineA.getPlayerLevelByNameSnapshot(),
                        engineB.getPlayerLevelByNameSnapshot());

            }
        } finally {
            PokerBot.clearThreadCognitiveDB();
        }

        return stats;
    }

    private static void resetAllStacksTo20bb(SimEngine engine) {
        for (PokerBot bot : engine.bots) {
            bot.removeChips(bot.getChips());
            // SHORT_STACKER bots get 499 chips (24.95 BB) so they stay under the
            // currentStackBB < 25.0 classifier threshold and their push-fold strategy is
            // appropriate; all others get 2000 (100 BB).
            int starting = (bot.simulatedArchetype == PokerBot.CognitiveArchetype.SHORT_STACKER)
                ? 499
                : 2000;
            bot.addChips(starting);
        }
    }

    private static void applySharedSeatPermutation(SimEngine engineA, SimEngine engineB) {
        int n = engineA.bots.size();
        if (n <= 1 || engineB.bots.size() != n) {
            return;
        }

        int[] perm = new int[n];
        for (int i = 0; i < n; i++)
            perm[i] = i;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }

        List<PokerBot> oldA = new ArrayList<>(engineA.bots);
        List<PokerBot> oldB = new ArrayList<>(engineB.bots);
        for (int seat = 0; seat < n; seat++) {
            engineA.bots.set(seat, oldA.get(perm[seat]));
            engineB.bots.set(seat, oldB.get(perm[seat]));
        }
    }

    private static Card[][] deepCopyHoleCards(Card[][] source) {
        if (source == null) {
            return null;
        }
        Card[][] copy = new Card[source.length][];
        for (int i = 0; i < source.length; i++) {
            if (source[i] == null) {
                copy[i] = null;
            } else {
                copy[i] = source[i].clone();
            }
        }
        return copy;
    }

    // Build pass-B hole cards so each seat gets a different seat's private cards.
    private static Card[][] buildSwappedSeatHoleCards(Card[][] passAHoleCards) {
        Card[][] swapped = deepCopyHoleCards(passAHoleCards);
        if (swapped == null || swapped.length <= 1) {
            return swapped;
        }

        int n = swapped.length;
        Card[][] result = new Card[n][];
        if (n == 2) {
            result[0] = swapped[1];
            result[1] = swapped[0];
            return result;
        }

        // Rotate by one seat for n>2 so no seat keeps the same hole cards.
        for (int seat = 0; seat < n; seat++) {
            int fromSeat = (seat + 1) % n;
            result[seat] = swapped[fromSeat];
        }
        return result;
    }

    // Explicit identity-based pass-B assignment for the two-bot duplicate duel.
    private static Card[][] buildDuelPass2HoleCards(Card[][] pass1HoleCards, List<PokerBot> seatOrder, PokerBot botA,
            PokerBot botB) {
        if (pass1HoleCards == null || pass1HoleCards.length != 2 || seatOrder == null || seatOrder.size() != 2) {
            return deepCopyHoleCards(pass1HoleCards);
        }

        Card[][] result = new Card[2][];
        for (int seat = 0; seat < 2; seat++) {
            PokerBot occupant = seatOrder.get(seat);
            if (occupant == botA) {
                result[seat] = pass1HoleCards[1].clone();
            } else if (occupant == botB) {
                result[seat] = pass1HoleCards[0].clone();
            } else {
                // Defensive fallback: preserve seat mapping if identity lookup is unavailable.
                result[seat] = pass1HoleCards[seat].clone();
            }
        }
        return result;
    }

    private static void printAggregatedTrueEvReport(int totalHands, SimulationTotals totals,
            StringBuilder telemetryOutput) {
        emitLine(telemetryOutput, "\n--- FINAL TELEMETRY REPORT ---");
        emitLine(telemetryOutput, "Total Hands Processed: " + totalHands);

        if (totalHands > 0) {
            emitFormat(telemetryOutput, "Bust Rates -> Dumb: %d (%.2f%%), Smart: %d (%.2f%%), God: %d (%.2f%%), Arc: %d (%.2f%%)\n",
                    totals.gameBusts[0], (totals.gameBusts[0] * 100.0 / totalHands),
                    totals.gameBusts[1], (totals.gameBusts[1] * 100.0 / totalHands),
                    totals.gameBusts[2], (totals.gameBusts[2] * 100.0 / totalHands),
                    totals.gameBusts[3], (totals.gameBusts[3] * 100.0 / totalHands));

            emitFormat(telemetryOutput, "Net Gold   -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d, Arc: %s✨%d\n",
                    (totals.tierNetProfits[0] >= 0 ? "+" : ""), totals.tierNetProfits[0],
                    (totals.tierNetProfits[1] >= 0 ? "+" : ""), totals.tierNetProfits[1],
                    (totals.tierNetProfits[2] >= 0 ? "+" : ""), totals.tierNetProfits[2],
                    (totals.tierNetProfits[3] >= 0 ? "+" : ""), totals.tierNetProfits[3]);

            if (totals.tierHands[0] > 0) {
                printAdvancedTierTelemetry("ADVANCED TELEMETRY (Tier Overall)", totals.tierNetProfits,
                        totals.tierProfitSquares, totals.tierHands, 20, telemetryOutput);
                printTierStdDevTelemetryLong(
                        "STANDARD DEVIATION (Tier Overall)",
                        totals.tierNetProfits,
                        totals.tierProfitSquares,
                        totals.tierHands,
                        20,
                        telemetryOutput);
            }

            if (totals.tierAievHands[0] > 0) {
                printAdvancedTierTelemetryDouble(
                        "AIEV ADJUSTED TELEMETRY (Tier Overall)",
                        totals.tierAievNetProfits,
                        totals.tierAievProfitSquares,
                        totals.tierAievHands,
                        20,
                        telemetryOutput);
                printTierStdDevTelemetryDouble(
                        "AIEV ADJUSTED STANDARD DEVIATION (Tier Overall)",
                        totals.tierAievNetProfits,
                        totals.tierAievProfitSquares,
                        totals.tierAievHands,
                        20,
                        telemetryOutput);
                emitFormat(telemetryOutput, "AIEV coverage: %d/%d all-in runout hands normalized\n",
                        totals.aievAdjustedHands, totalHands);
            }
        }

        int totalWins = totals.handWins[0] + totals.handWins[1] + totals.handWins[2];
        if (totalWins > 0) {
            emitFormat(telemetryOutput, "Win Rate %% (By Hands Won): God (%.1f%%), Smart (%.1f%%), Dumb (%.1f%%)\n",
                    (totals.handWins[2] * 100.0 / totalWins),
                    (totals.handWins[1] * 100.0 / totalWins),
                    (totals.handWins[0] * 100.0 / totalWins));
        }
    }

    private static void printCognitiveTelemetry(String title, CognitiveTelemetryAggregate telemetry,
            StringBuilder telemetryOutput) {
        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (telemetry == null || !telemetry.hasData()) {
            emitLine(telemetryOutput, "No tracked cognitive profiles available for this run.");
            return;
        }

        emitFormat(telemetryOutput, "Profiles tracked: %d | Weighted hands: %d\n", telemetry.profilesTracked,
                telemetry.weightedHands);
        emitFormat(
                telemetryOutput,
                "Archetype mix -> NIT: %d, STATION: %d, MANIAC: %d, TAG: %d, LAG: %d, WHALE: %d, FISH: %d, BULLY: %d, SS: %d, ELITE_REG: %d, UNKNOWN: %d\n",
                telemetry.nitCount,
                telemetry.stationCount,
                telemetry.maniacCount,
                telemetry.tagCount,
                telemetry.lagCount,
                telemetry.whaleCount,
                telemetry.fishCount,
                telemetry.bullyCount,
                telemetry.shortStackerCount,
                telemetry.eliteRegCount,
                telemetry.unknownCount);
        emitFormat(
                telemetryOutput,
                "EMA means -> VPIP: %.3f, PFR: %.3f, AFq(F/T/R): %.3f / %.3f / %.3f, WTSD: %.3f, FoldToCBet: %.3f\n",
                telemetry.mean(telemetry.vpipWeightedSum),
                telemetry.mean(telemetry.pfrWeightedSum),
                telemetry.mean(telemetry.afqFlopWeightedSum),
                telemetry.mean(telemetry.afqTurnWeightedSum),
                telemetry.mean(telemetry.afqRiverWeightedSum),
                telemetry.mean(telemetry.wtsdWeightedSum),
                telemetry.mean(telemetry.foldToCbetWeightedSum));
        emitFormat(
                telemetryOutput,
                "Stability signals -> VIndex: %.3f, StyleShiftEMA: %.3f\n",
                telemetry.mean(telemetry.vIndexWeightedSum),
                telemetry.mean(telemetry.styleShiftWeightedSum));

        // Per-bot individual profile breakdown
        Map<String, PokerBot.CognitiveProfile> profiles = PokerBot.getCognitiveDB();
        if (profiles != null && !profiles.isEmpty()) {
            emitLine(telemetryOutput, "--- Per-Bot Profiles ---");
            for (Map.Entry<String, PokerBot.CognitiveProfile> entry : profiles.entrySet()) {
                PokerBot.CognitiveProfile p = entry.getValue();
                if (p == null || p.handsPlayed <= 0) continue;
                emitFormat(telemetryOutput,
                        "  %s [%s] (%d hands) -> VPIP: %.3f, PFR: %.3f, AFq(F/T/R): %.3f/%.3f/%.3f, WTSD: %.3f\n",
                        entry.getKey(),
                        p.getArchetype().name(),
                        p.handsPlayed,
                        p.vpipEMA,
                        p.pfrEMA,
                        p.afqFlopEMA,
                        p.afqTurnEMA,
                        p.afqRiverEMA,
                        p.wtsdEMA);
                // Per-street EV breakdown — segments BB/100 by where the hand ended.
                long totalHands = 0;
                for (long n : p.handsEndedAtStreet) totalHands += n;
                if (totalHands > 0) {
                    String[] streetNames = {"preflop", "flop", "turn", "river"};
                    StringBuilder sb = new StringBuilder("    Per-street EV:");
                    for (int s = 0; s < 4; s++) {
                        double pct = 100.0 * p.handsEndedAtStreet[s] / totalHands;
                        double avgChips = p.handsEndedAtStreet[s] > 0
                            ? (double) p.netChipsAtStreet[s] / p.handsEndedAtStreet[s]
                            : 0.0;
                        sb.append(String.format(" %s=%d(%.1f%%, avg=%+.1f)",
                            streetNames[s], p.handsEndedAtStreet[s], pct, avgChips));
                    }
                    sb.append("\n");
                    emitLine(telemetryOutput, sb.toString().trim());
                }
            }
        }
    }

    private static void printSourceOfProfitTelemetry(
            String title,
            long[] tierFoldoutNetProfits,
            long[] tierShowdownNetProfits,
            long[] tierHands,
            long[] tierNetProfits,
            double[] tierAievNetProfits,
            int bigBlind,
            StringBuilder telemetryOutput) {

        if (tierHands == null || tierHands.length < 3 || (tierHands[0] + tierHands[1] + tierHands[2] + tierHands[3]) <= 0) {
            return;
        }

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        for (int lvl = 0; lvl < 4; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
            double hands = Math.max(1, tierHands[lvl]);
            double foldoutBb100 = (((double) tierFoldoutNetProfits[lvl] / hands) / bigBlind) * 100.0;
            double showdownBb100 = (((double) tierShowdownNetProfits[lvl] / hands) / bigBlind) * 100.0;
            double netBb100 = (((double) tierNetProfits[lvl] / hands) / bigBlind) * 100.0;
            double runoutLuckBb100 = (((double) tierNetProfits[lvl] - tierAievNetProfits[lvl]) / hands / bigBlind)
                    * 100.0;
            emitFormat(
                    telemetryOutput,
                    "%s Source Mix -> Fold-Out: %.2f BB/100 | Showdown: %.2f BB/100 | Runout Luck: %.2f BB/100 | Net: %.2f BB/100\n",
                    name,
                    foldoutBb100,
                    showdownBb100,
                    runoutLuckBb100,
                    netBb100);
        }
    }

    private static void printPhaseBCognitiveDiagnostics(String title, CognitiveTelemetryAggregate telemetry,
            StringBuilder telemetryOutput) {
        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (telemetry == null || !telemetry.hasData()) {
            emitLine(telemetryOutput, "Insufficient cognitive profile depth for Phase B diagnostics.");
            return;
        }

        double vpip = telemetry.mean(telemetry.vpipWeightedSum);
        double pfr = telemetry.mean(telemetry.pfrWeightedSum);
        double afqFlop = telemetry.mean(telemetry.afqFlopWeightedSum);
        double afqTurn = telemetry.mean(telemetry.afqTurnWeightedSum);
        double afqRiver = telemetry.mean(telemetry.afqRiverWeightedSum);
        double postflopAfq = (afqFlop + afqTurn + afqRiver) / 3.0;
        double wtsd = telemetry.mean(telemetry.wtsdWeightedSum);
        double foldToCbet = telemetry.mean(telemetry.foldToCbetWeightedSum);
        double vIndex = telemetry.mean(telemetry.vIndexWeightedSum);
        double styleShift = telemetry.mean(telemetry.styleShiftWeightedSum);

        long aggressiveProfiles = telemetry.maniacCount + telemetry.eliteRegCount;
        long passiveProfiles = telemetry.nitCount + telemetry.stationCount;
        double pressureBias = (double) (aggressiveProfiles - passiveProfiles) / Math.max(1L, telemetry.profilesTracked);

        double sampleDepth = Math.min(1.0, telemetry.weightedHands / 500.0);
        double profileBreadth = Math.min(1.0, telemetry.profilesTracked / 4.0);
        double confidenceScore = (0.70 * sampleDepth + 0.30 * profileBreadth) * 100.0;
        String confidenceBand;
        if (confidenceScore >= 75.0)
            confidenceBand = "HIGH";
        else if (confidenceScore >= 40.0)
            confidenceBand = "MEDIUM";
        else
            confidenceBand = "LOW";

        String pressureClass;
        if (pressureBias >= 0.20 && postflopAfq >= 0.35)
            pressureClass = "Aggressive Pool";
        else if (pressureBias <= -0.20 && foldToCbet <= 0.35)
            pressureClass = "Sticky Passive Pool";
        else if (pressureBias <= -0.20)
            pressureClass = "Passive Pool";
        else
            pressureClass = "Balanced Pool";

        double initiativeGap = pfr - vpip;
        double showdownStickiness = (wtsd + (1.0 - foldToCbet)) * 0.5;

        emitFormat(
                telemetryOutput,
                "Confidence -> %.1f%% (%s) | Pool Class: %s | Pressure Bias: %.3f\n",
                confidenceScore,
                confidenceBand,
                pressureClass,
                pressureBias);
        emitFormat(
                telemetryOutput,
                "Signals -> InitiativeGap(PFR-VPIP): %.3f, PostflopAFq: %.3f, ShowdownStickiness: %.3f\n",
                initiativeGap,
                postflopAfq,
                showdownStickiness);
        emitFormat(
                telemetryOutput,
                "Stability -> VIndex: %.3f, StyleShiftEMA: %.3f\n",
                vIndex,
                styleShift);
    }

    private static void printPhaseBMode7EdgeAttribution(
            String title,
            long[] tierFoldoutNetProfits,
            long[] tierShowdownNetProfits,
            long[] tierHands,
            long[] tierNetProfits,
            double[] tierAievNetProfits,
            int bigBlind,
            StringBuilder telemetryOutput) {

        if (tierHands == null || tierHands.length < 3 || (tierHands[0] + tierHands[1] + tierHands[2] + tierHands[3]) <= 0) {
            return;
        }

        long totalHands = 0;
        long rawNet = 0;
        double aievNet = 0.0;
        double[] foldoutBb100ByTier = new double[4];
        double[] showdownBb100ByTier = new double[4];
        double[] netBb100ByTier = new double[4];
        double[] runoutLuckBb100ByTier = new double[4];
        int leaderTier = 0;
        int laggingTier = 0;

        for (int lvl = 0; lvl < 4; lvl++) {
            long hands = Math.max(1L, tierHands[lvl]);
            foldoutBb100ByTier[lvl] = (((double) tierFoldoutNetProfits[lvl] / hands) / bigBlind) * 100.0;
            showdownBb100ByTier[lvl] = (((double) tierShowdownNetProfits[lvl] / hands) / bigBlind) * 100.0;
            netBb100ByTier[lvl] = (((double) tierNetProfits[lvl] / hands) / bigBlind) * 100.0;
            runoutLuckBb100ByTier[lvl] = ((((double) tierNetProfits[lvl] - tierAievNetProfits[lvl]) / hands) / bigBlind)
                    * 100.0;

            if (netBb100ByTier[lvl] > netBb100ByTier[leaderTier]) {
                leaderTier = lvl;
            }
            if (netBb100ByTier[lvl] < netBb100ByTier[laggingTier]) {
                laggingTier = lvl;
            }

            totalHands += tierHands[lvl];
            rawNet += tierNetProfits[lvl];
            aievNet += tierAievNetProfits[lvl];
        }

        if (totalHands <= 0) {
            return;
        }

        double hands = totalHands;
        double rawBb100 = (((double) rawNet / hands) / bigBlind) * 100.0;
        double aievBb100 = ((aievNet / hands) / bigBlind) * 100.0;

        double leaderFoldout = foldoutBb100ByTier[leaderTier];
        double leaderShowdown = showdownBb100ByTier[leaderTier];
        double leaderRunoutLuck = runoutLuckBb100ByTier[leaderTier];
        double leaderNet = netBb100ByTier[leaderTier];

        double gross = Math.abs(leaderFoldout) + Math.abs(leaderShowdown);
        double foldoutShare = (gross > 0.0) ? (Math.abs(leaderFoldout) / gross) * 100.0 : 0.0;
        double showdownShare = (gross > 0.0) ? (Math.abs(leaderShowdown) / gross) * 100.0 : 0.0;

        String leaderName = leaderTier == 0 ? "Dumb" : (leaderTier == 1 ? "Smart" : (leaderTier == 2 ? "God" : "Arc"));
        String laggingName = laggingTier == 0 ? "Dumb" : (laggingTier == 1 ? "Smart" : (laggingTier == 2 ? "God" : "Arc"));
        double spreadBb100 = netBb100ByTier[leaderTier] - netBb100ByTier[laggingTier];

        String edgeDriver;
        if (Math.abs(leaderFoldout) > Math.abs(leaderShowdown) * 1.15) {
            edgeDriver = "Pressure-Led Edge";
        } else if (Math.abs(leaderShowdown) > Math.abs(leaderFoldout) * 1.15) {
            edgeDriver = "Showdown-Led Edge";
        } else {
            edgeDriver = "Balanced Edge Mix";
        }

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        emitFormat(
                telemetryOutput,
                "Leader Lens [%s] -> Fold-Out: %.2f BB/100 (%.1f%%), Showdown: %.2f BB/100 (%.1f%%), Runout Luck: %.2f BB/100\n",
                leaderName,
                leaderFoldout,
                foldoutShare,
                leaderShowdown,
                showdownShare,
                leaderRunoutLuck);
        emitFormat(
                telemetryOutput,
                "Tier Spread -> Best: %s (%.2f BB/100), Worst: %s (%.2f BB/100), Gap: %.2f BB/100 | Driver: %s\n",
                leaderName,
                leaderNet,
                laggingName,
                netBb100ByTier[laggingTier],
                spreadBb100,
                edgeDriver);
        emitFormat(
                telemetryOutput,
                "Conservation Check -> Table Raw: %.2f BB/100, Table AIEV: %.2f BB/100 (expected near 0 in closed pool)\n",
                rawBb100,
                aievBb100);
    }

    private static void printPhaseCConvergenceDiagnostics(String title, SimulationTotals totals,
            StringBuilder telemetryOutput) {
        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (totals == null || totals.convergenceSamples <= 0) {
            emitLine(telemetryOutput, "No convergence samples were recorded for this run.");
            return;
        }

        double meanAbsVpip = totals.convergenceAbsVpipErrSum / totals.convergenceSamples;
        double meanAbsPfr = totals.convergenceAbsPfrErrSum / totals.convergenceSamples;
        emitFormat(
                telemetryOutput,
                "Convergence Samples: %d | Mean Abs Error VPIP: %.4f | Mean Abs Error PFR: %.4f\n",
                totals.convergenceSamples,
                meanAbsVpip,
                meanAbsPfr);
    }

    private static void printPhaseCTransferIntegrity(String title, SimulationTotals totals,
            StringBuilder telemetryOutput) {
        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (totals == null || totals.transferValidationHands <= 0) {
            emitLine(telemetryOutput, "No transfer integrity samples were recorded for this run.");
            return;
        }

        double seatErrorPerHand = totals.transferAbsSeatErrorSum / totals.transferValidationHands;
        double conservationPerHand = totals.transferAbsConservationErrorSum / totals.transferValidationHands;
        emitFormat(
                telemetryOutput,
                "Validation Hands: %d | Mean Seat Error: %.6f chips/hand | Max Seat Error: %.6f chips\n",
                totals.transferValidationHands,
                seatErrorPerHand,
                totals.transferMaxSeatError);
        emitFormat(
                telemetryOutput,
                "Mean Conservation Error: %.6f chips/hand | Max Conservation Error: %.6f chips\n",
                conservationPerHand,
                totals.transferMaxConservationError);
    }

    private static void printPhaseCMode7SourceAttribution(
            String title,
            double godVsDumbNet,
            double godVsSmartNet,
            double godVsArcNet,
            Map<String, Double> godOpponentNet,
            Map<String, Integer> playerLevelsByName,
            long handsProcessed,
            int bigBlind,
            StringBuilder telemetryOutput) {

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (handsProcessed <= 0) {
            emitLine(telemetryOutput, "No hands processed for source attribution.");
            return;
        }

        double hands = Math.max(1L, handsProcessed);
        double dumbBb100 = ((godVsDumbNet / hands) / bigBlind) * 100.0;
        double smartBb100 = ((godVsSmartNet / hands) / bigBlind) * 100.0;
        double arcBb100 = ((godVsArcNet / hands) / bigBlind) * 100.0;
        double totalNet = godVsDumbNet + godVsSmartNet + godVsArcNet;
        double totalBb100 = ((totalNet / hands) / bigBlind) * 100.0;

        emitFormat(
                telemetryOutput,
                "God Extraction -> vs Dumb: %s chips (%.2f BB/100) | vs Smart: %s chips (%.2f BB/100) | vs Arc: %s chips (%.2f BB/100) | Total: %s chips (%.2f BB/100)\n",
                signedDouble(godVsDumbNet),
                dumbBb100,
                signedDouble(godVsSmartNet),
                smartBb100,
                signedDouble(godVsArcNet),
                arcBb100,
                signedDouble(totalNet),
                totalBb100);


        if (godOpponentNet == null || godOpponentNet.isEmpty()) {
            emitLine(telemetryOutput, "No per-seat source map recorded.");
            return;
        }

        Map<String, List<Map.Entry<String, Double>>> byGod = new HashMap<>();
        for (Map.Entry<String, Double> entry : godOpponentNet.entrySet()) {
            String pairKey = entry.getKey();
            int arrow = pairKey.indexOf("->");
            if (arrow <= 0 || arrow >= pairKey.length() - 2) {
                continue;
            }
            String godName = pairKey.substring(0, arrow);
            String opponentName = pairKey.substring(arrow + 2);
            byGod.computeIfAbsent(godName, k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(opponentName, entry.getValue()));
        }

        if (byGod.isEmpty()) {
            emitLine(telemetryOutput, "No God-vs-opponent seat pairs found.");
            return;
        }

        List<String> gods = new ArrayList<>(byGod.keySet());
        Collections.sort(gods);
        for (String god : gods) {
            List<Map.Entry<String, Double>> pairs = byGod.get(god);
            pairs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            StringBuilder line = new StringBuilder();
            line.append(god).append(" top seats -> ");
            int emitted = 0;
            for (Map.Entry<String, Double> pair : pairs) {
                if (emitted >= 3) {
                    break;
                }
                String opponent = pair.getKey();
                int level = (playerLevelsByName == null)
                        ? inferTierFromName(opponent)
                        : playerLevelsByName.getOrDefault(opponent, inferTierFromName(opponent));
                String tier = tierName(level);
                if (emitted > 0) {
                    line.append(" | ");
                }
                line.append(opponent)
                        .append(" [")
                        .append(tier)
                        .append("]: ")
                        .append(signedDouble(pair.getValue()))
                        .append(" chips");
                emitted++;
            }
            if (emitted == 0) {
                line.append("no attributed extraction");
            }
            emitLine(telemetryOutput, line.toString());
        }
    }

    private static void printPhaseCShiftTimeline(
            String title,
            Map<String, Integer> shiftCounts,
            Map<String, List<Integer>> shiftHands,
            StringBuilder telemetryOutput) {

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (shiftCounts == null || shiftCounts.isEmpty()) {
            emitLine(telemetryOutput, "No archetype shifts recorded in this run.");
            return;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(shiftCounts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Integer> entry : entries) {
            String player = entry.getKey();
            int shifts = entry.getValue();
            List<Integer> hands = (shiftHands == null) ? null : shiftHands.get(player);
            StringBuilder line = new StringBuilder();
            line.append(player).append(" shifts: ").append(shifts);
            if (hands != null && !hands.isEmpty()) {
                line.append(" | sample hands: ");
                int cap = Math.min(10, hands.size());
                for (int i = 0; i < cap; i++) {
                    if (i > 0)
                        line.append(", ");
                    line.append(hands.get(i));
                }
            }
            emitLine(telemetryOutput, line.toString());
        }
    }

    private static void printPhaseDMode7Overlay(
            String title,
            double baselineVsDumb,
            double baselineVsSmart,
            double baselineVsArc,
            long baselineHands,
            double duplicateVsDumb,
            double duplicateVsSmart,
            double duplicateVsArc,
            long duplicateHands,
            double combinedVsDumb,
            double combinedVsSmart,
            double combinedVsArc,
            long combinedHands,

            int bigBlind,
            StringBuilder telemetryOutput) {

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        if (baselineHands <= 0 || duplicateHands <= 0 || combinedHands <= 0) {
            emitLine(telemetryOutput, "Insufficient paired-mode data for Phase D overlay.");
            return;
        }

        double baselineTotal = baselineVsDumb + baselineVsSmart + baselineVsArc;
        double duplicateTotal = duplicateVsDumb + duplicateVsSmart + duplicateVsArc;
        double combinedTotal = combinedVsDumb + combinedVsSmart + combinedVsArc;

        emitFormat(
                telemetryOutput,
                "Baseline -> vs Dumb: %s (%.2f BB/100) | vs Smart: %s (%.2f BB/100) | vs Arc: %s (%.2f BB/100) | Total: %s (%.2f BB/100)\n",
                signedDouble(baselineVsDumb),
                toBb100(baselineVsDumb, baselineHands, bigBlind),
                signedDouble(baselineVsSmart),
                toBb100(baselineVsSmart, baselineHands, bigBlind),
                signedDouble(baselineVsArc),
                toBb100(baselineVsArc, baselineHands, bigBlind),
                signedDouble(baselineTotal),
                toBb100(baselineTotal, baselineHands, bigBlind));
        emitFormat(
                telemetryOutput,
                "Duplicate -> vs Dumb: %s (%.2f BB/100) | vs Smart: %s (%.2f BB/100) | vs Arc: %s (%.2f BB/100) | Total: %s (%.2f BB/100)\n",
                signedDouble(duplicateVsDumb),
                toBb100(duplicateVsDumb, duplicateHands, bigBlind),
                signedDouble(duplicateVsSmart),
                toBb100(duplicateVsSmart, duplicateHands, bigBlind),
                signedDouble(duplicateVsArc),
                toBb100(duplicateVsArc, duplicateHands, bigBlind),
                signedDouble(duplicateTotal),
                toBb100(duplicateTotal, duplicateHands, bigBlind));
        emitFormat(
                telemetryOutput,
                "Combined -> vs Dumb: %s (%.2f BB/100) | vs Smart: %s (%.2f BB/100) | vs Arc: %s (%.2f BB/100) | Total: %s (%.2f BB/100)\n",
                signedDouble(combinedVsDumb),
                toBb100(combinedVsDumb, combinedHands, bigBlind),
                signedDouble(combinedVsSmart),
                toBb100(combinedVsSmart, combinedHands, bigBlind),
                signedDouble(combinedVsArc),
                toBb100(combinedVsArc, combinedHands, bigBlind),
                signedDouble(combinedTotal),
                toBb100(combinedTotal, combinedHands, bigBlind));


        double deltaDupDumb = duplicateVsDumb - baselineVsDumb;
        double deltaDupSmart = duplicateVsSmart - baselineVsSmart;
        double deltaDupArc = duplicateVsArc - baselineVsArc;
        double deltaDupTotal = duplicateTotal - baselineTotal;
        double deltaCombDumb = combinedVsDumb - baselineVsDumb;
        double deltaCombSmart = combinedVsSmart - baselineVsSmart;
        double deltaCombArc = combinedVsArc - baselineVsArc;
        double deltaCombTotal = combinedTotal - baselineTotal;

        emitFormat(
                telemetryOutput,
                "Delta (Duplicate - Baseline) -> vs Dumb: %s | vs Smart: %s | vs Arc: %s | Total: %s\n",
                signedDouble(deltaDupDumb),
                signedDouble(deltaDupSmart),
                signedDouble(deltaDupArc),
                signedDouble(deltaDupTotal));
        emitFormat(
                telemetryOutput,
                "Delta (Combined - Baseline) -> vs Dumb: %s | vs Smart: %s | vs Arc: %s | Total: %s\n",
                signedDouble(deltaCombDumb),
                signedDouble(deltaCombSmart),
                signedDouble(deltaCombArc),
                signedDouble(deltaCombTotal));


        double baselineDumbShare = sourceSharePercent(baselineVsDumb, baselineVsSmart);
        double duplicateDumbShare = sourceSharePercent(duplicateVsDumb, duplicateVsSmart);
        double combinedDumbShare = sourceSharePercent(combinedVsDumb, combinedVsSmart);
        emitFormat(
                telemetryOutput,
                "Mix Shift (Dumb share of |source|) -> Baseline: %.1f%% | Duplicate: %.1f%% | Combined: %.1f%%\n",
                baselineDumbShare,
                duplicateDumbShare,
                combinedDumbShare);

        String overlaySignal = classifyOverlaySignal(baselineTotal, duplicateTotal, combinedTotal);
        emitLine(telemetryOutput, "Overlay Signal -> " + overlaySignal);
    }

    private static double toBb100(double chips, long hands, int bigBlind) {
        if (hands <= 0 || bigBlind <= 0) {
            return 0.0;
        }
        return ((chips / hands) / bigBlind) * 100.0;
    }

    private static double sourceSharePercent(double vsDumb, double vsSmart) {
        double gross = Math.abs(vsDumb) + Math.abs(vsSmart);
        if (gross <= 1e-9) {
            return 0.0;
        }
        return (Math.abs(vsDumb) / gross) * 100.0;
    }

    private static String classifyOverlaySignal(double baselineTotal, double duplicateTotal, double combinedTotal) {
        boolean baselinePositive = baselineTotal > 0.0;
        boolean duplicatePositive = duplicateTotal > 0.0;
        boolean combinedPositive = combinedTotal > 0.0;

        if (baselinePositive && duplicatePositive && combinedPositive) {
            return "Stable edge under paired controls";
        }
        if (baselinePositive && (!duplicatePositive || !combinedPositive)) {
            return "Edge compresses under paired controls";
        }
        if (!baselinePositive && (duplicatePositive || combinedPositive)) {
            return "Paired controls reveal hidden edge";
        }
        return "No durable extraction edge detected";
    }

    private static void emitPhaseCRollupDiagnostics(SimulationTotals totals, long handsProcessed) {
        if (totals == null || !BotDiagnostics.enabled()) {
            return;
        }
        String payload = "hands=" + handsProcessed
                + ", godVsDumb=" + String.format("%.2f", totals.godVsDumbNet)
                + ", godVsSmart=" + String.format("%.2f", totals.godVsSmartNet)
                + ", godVsArc=" + String.format("%.2f", totals.godVsArcNet)
                + ", convergenceSamples=" + totals.convergenceSamples
                + ", topPairs=" + topPairSummary(totals.godOpponentNet, 5);
        BotDiagnostics.recordSourceTransferRollup(payload);
    }

    private static String topPairSummary(Map<String, Double> pairMap, int limit) {
        if (pairMap == null || pairMap.isEmpty() || limit <= 0) {
            return "none";
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(pairMap.entrySet());
        entries.sort((a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));
        StringBuilder sb = new StringBuilder();
        int cap = Math.min(limit, entries.size());
        for (int i = 0; i < cap; i++) {
            if (i > 0)
                sb.append(" | ");
            Map.Entry<String, Double> e = entries.get(i);
            sb.append(e.getKey()).append("=").append(String.format("%.2f", e.getValue()));
        }
        return sb.toString();
    }

    private static String signedDouble(double value) {
        return String.format("%s%.2f", value >= 0.0 ? "+" : "", value);
    }

    private static int inferTierFromName(String name) {
        if (name == null) {
            return -1;
        }
        if (name.contains("[D]"))
            return 0;
        if (name.contains("[S]"))
            return 1;
        if (name.contains("[G"))
            return 2;
        return -1;
    }

    private static String tierName(int tier) {
        if (tier == 0)
            return "Dumb";
        if (tier == 1)
            return "Smart";
        if (tier == 2)
            return "God";
        if (tier == 3)
            return "Arc";
        return "Unknown";
    }

    private static void printAdvancedTierTelemetry(String title, long[] tierNetProfits, double[] tierProfitSquares,
            long[] tierHands, int bigBlind) {
        printAdvancedTierTelemetry(title, tierNetProfits, tierProfitSquares, tierHands, bigBlind, null);
    }

    private static void printAdvancedTierTelemetry(String title, long[] tierNetProfits, double[] tierProfitSquares,
            long[] tierHands, int bigBlind, StringBuilder telemetryOutput) {
        if ((tierHands[0] + tierHands[1] + tierHands[2] + tierHands[3]) <= 0)
            return;

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        for (int lvl = 0; lvl < 4; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
            double meanProfit = (double) tierNetProfits[lvl] / tierHands[lvl];
            double bb100 = (meanProfit / bigBlind) * 100.0;
            double variance = (tierProfitSquares[lvl] / tierHands[lvl]) - (meanProfit * meanProfit);
            double stdev = Math.sqrt(Math.max(0, variance));
            double ciMargin = 1.96 * (stdev / Math.sqrt(tierHands[lvl]));
            double ciMarginBb100 = (ciMargin / bigBlind) * 100.0;
            emitFormat(telemetryOutput, "%s BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name, bb100,
                    bb100 - ciMarginBb100, bb100 + ciMarginBb100);
        }
    }

    private static void printAdvancedTierTelemetryDouble(String title, double[] tierNetProfits,
            double[] tierProfitSquares, long[] tierHands, int bigBlind, StringBuilder telemetryOutput) {
        if ((tierHands[0] + tierHands[1] + tierHands[2] + tierHands[3]) <= 0)
            return;


        emitLine(telemetryOutput, "\n--- " + title + " ---");
        for (int lvl = 0; lvl < 4; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
            double meanProfit = tierNetProfits[lvl] / tierHands[lvl];
            double bb100 = (meanProfit / bigBlind) * 100.0;
            double variance = (tierProfitSquares[lvl] / tierHands[lvl]) - (meanProfit * meanProfit);
            double stdev = Math.sqrt(Math.max(0, variance));
            double ciMargin = 1.96 * (stdev / Math.sqrt(tierHands[lvl]));
            double ciMarginBb100 = (ciMargin / bigBlind) * 100.0;
            emitFormat(telemetryOutput, "%s BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name, bb100,
                    bb100 - ciMarginBb100, bb100 + ciMarginBb100);
        }
    }

    private static void printTierStdDevTelemetryLong(String title, long[] tierNetProfits, double[] tierProfitSquares,
            long[] tierHands, int bigBlind, StringBuilder telemetryOutput) {
        if (tierHands[0] <= 0)
            return;

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        for (int lvl = 0; lvl < 4; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
            double meanProfit = (double) tierNetProfits[lvl] / tierHands[lvl];
            double variance = (tierProfitSquares[lvl] / tierHands[lvl]) - (meanProfit * meanProfit);
            double stdev = Math.sqrt(Math.max(0, variance));
            double stdevBb100 = (stdev / bigBlind) * 100.0;
            emitFormat(telemetryOutput, "%s StdDev -> %.2f chips/hand | %.2f BB/100\n", name, stdev, stdevBb100);
        }
    }

    private static void printTierStdDevTelemetryDouble(String title, double[] tierNetProfits,
            double[] tierProfitSquares, long[] tierHands, int bigBlind, StringBuilder telemetryOutput) {
        if (tierHands[0] <= 0)
            return;

        emitLine(telemetryOutput, "\n--- " + title + " ---");
        for (int lvl = 0; lvl < 4; lvl++) {
            String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
            double meanProfit = tierNetProfits[lvl] / tierHands[lvl];
            double variance = (tierProfitSquares[lvl] / tierHands[lvl]) - (meanProfit * meanProfit);
            double stdev = Math.sqrt(Math.max(0, variance));
            double stdevBb100 = (stdev / bigBlind) * 100.0;
            emitFormat(telemetryOutput, "%s StdDev -> %.2f chips/hand | %.2f BB/100\n", name, stdev, stdevBb100);
        }
    }

    // --- ENGINE CLASSES ---

    static class SimEngine {
        List<PokerBot> bots = new ArrayList<>();
        private int dumbCount, smartCount, godCount;
        private int handCount = 0;
        private int[] bustsByTier = new int[4];
        private int[] winsByTier = new int[4];
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

        private long[] tierNetProfits = new long[4];
        private long[] tierFoldoutNetProfits = new long[4];
        private long[] tierShowdownNetProfits = new long[4];
        private double[] tierSumSquaresOfProfits = new double[4];
        private long[] tierHandsPlayed = new long[4];
        private double[] tierAievNetProfits = new double[4];
        private double[] tierAievProfitSquares = new double[4];
        private long[] tierAievHandsPlayed = new long[4];
        private long aievAdjustedHands = 0;
        private long[] lastHandTierRawProfits = new long[4];
        private double[] lastHandTierAievProfits = new double[4];
        private boolean aievEnabled = false;
        private boolean isTrueEvMode = false;
        private static final double PREFLOP_EMA_ALPHA = 0.05;
        private static final double POSTFLOP_EMA_ALPHA = 0.05;
        private static final int AIEV_RUNOUT_SAMPLE_CAP = 384;
        private static final Card[] CANONICAL_DECK = new Deck().getCards();
        private boolean[] preflopVPIPFlags;
        private boolean[] preflopPFRFlags;
        private int[][] postflopAggressionActions;
        private int[][] postflopAggressionOpportunities;
        private boolean[] sawFlopThisHand;
        private boolean[] foldToCbetOpportunity;
        private boolean[] foldedToCbet;
        private boolean cbetFiredOnFlop = false;
        private int cbetAggressorIndex = -1;
        private long[] truePreflopHandsObserved;
        private long[] truePreflopVpipCount;
        private long[] truePreflopPfrCount;
        private static final int CONVERGENCE_LOG_INTERVAL = 100;
        private static final int SHIFT_TIMELINE_LIMIT = 24;

        private final Map<String, Integer> archetypeShiftCounts = new HashMap<>();
        private final Map<String, List<Integer>> archetypeShiftHands = new HashMap<>();
        private long convergenceSamples = 0;
        private double convergenceAbsVpipErrSum = 0.0;
        private double convergenceAbsPfrErrSum = 0.0;
        private long transferValidationHands = 0;
        private double transferAbsSeatErrorSum = 0.0;
        private double transferMaxSeatError = 0.0;
        private double transferAbsConservationErrorSum = 0.0;
        private double transferMaxConservationError = 0.0;

        private final Map<String, Double> godOpponentNet = new HashMap<>();
        private final Map<String, Integer> playerLevelByName = new HashMap<>();
        private double godVsDumbNet = 0.0;
        private double godVsSmartNet = 0.0;
        private double godVsArcNet = 0.0;
        private double lastHandGodVsDumbNet = 0.0;
        private double lastHandGodVsSmartNet = 0.0;
        private double lastHandGodVsArcNet = 0.0;

        private Map<String, Double> lastHandGodOpponentNet = new HashMap<>();

        private String diagnosticsModeLabel = "SIM";
        private String diagnosticsPassLabel = "BASE";
        private int diagnosticsWorkerId = -1;

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode) {
            this(d, s, g, interactive, protectedMode, 1, true);
        }

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode, int nightmareIntensity) {
            this(d, s, g, interactive, protectedMode, nightmareIntensity, true);
        }

        public SimEngine(int d, int s, int g, boolean interactive, boolean protectedMode, int nightmareIntensity,
                boolean shouldShuffle) {
            PokerBot.resetThreadCognitiveDB();
            this.dumbCount = d;
            this.smartCount = s;
            this.godCount = g;
            this.interactive = interactive;
            this.protectedMode = protectedMode;
            this.neuralProtectedMode = protectedMode && PokerSimulator.isNeuralProtectedMode;
            this.nightmareIntensity = nightmareIntensity;
            this.shouldShuffle = shouldShuffle;
            initializeBots();
        }

        private PokerBot.CognitiveArchetype[] duelArchetypes;


        public SimEngine(int[] levels, boolean interactive, boolean protectedMode, int nightmareIntensity) {
            this(levels, null, interactive, protectedMode, nightmareIntensity);
        }

        public SimEngine(int[] levels, PokerBot.CognitiveArchetype[] archetypes, boolean interactive, boolean protectedMode, int nightmareIntensity) {
            PokerBot.resetThreadCognitiveDB();
            this.duelLevels = levels;
            this.duelArchetypes = archetypes;
            this.dumbCount = 0;

            this.smartCount = 0;
            this.godCount = 0;
            for (int l : levels) {
                if (l == 0)
                    dumbCount++;
                else if (l == 1)
                    smartCount++;
                else if (l == 2)
                    godCount++;
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
                for (int i = 0; i < duelLevels.length; i++) {
                    int level = duelLevels[i];
                    if (level == 3 && duelArchetypes != null && i < duelArchetypes.length && duelArchetypes[i] != null) {
                        createArchetypeBot(duelArchetypes[i]);
                    } else {
                        createBot(level);
                    }
                }
            } else {

                for (int i = 0; i < dumbCount; i++) createBot(0);
                for (int i = 0; i < smartCount; i++) createBot(1);
                for (int i = 0; i < godCount; i++) createBot(2);
                // Phase 10: Archetype Bots
                for (int i = 0; i < PokerSimulator.arcNitCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.NIT);
                for (int i = 0; i < PokerSimulator.arcManiacCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.MANIAC);
                for (int i = 0; i < PokerSimulator.arcStationCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.STATION);
                for (int i = 0; i < PokerSimulator.arcTagCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.TAG);
                for (int i = 0; i < PokerSimulator.arcWhaleCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.WHALE);
                for (int i = 0; i < PokerSimulator.arcFishCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.FISH);
                for (int i = 0; i < PokerSimulator.arcBullyCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.BULLY);
                for (int i = 0; i < PokerSimulator.arcLagCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.LAG);
                for (int i = 0; i < PokerSimulator.arcShortStackerCount; i++)
                    createArchetypeBot(PokerBot.CognitiveArchetype.SHORT_STACKER);
                // ELITE_REG = God Bot in pureProtectedGtoMode
                for (int i = 0; i < PokerSimulator.arcEliteRegCount; i++)
                    createEliteRegBot();
            }
            if (shouldShuffle)
                Collections.shuffle(bots);
        }

        private void createBot(int level) {
            // Passing a dummy player named "edj" to the constructor to force-reveal tags
            // for the simulator
            PokerPlayer[] unlocker = { new PokerPlayer("edj") };
            PokerBot b = new PokerBot(unlocker);
            b.setBotLevel(level);
            b.setProtectedMode(protectedMode);
            b.setNeuralProtectedMode(neuralProtectedMode);
            b.setNightmareIntensity(nightmareIntensity);
            b.refreshNameTag(unlocker); // Force refresh with the unlocker
            b.removeChips(b.getChips());
            b.addChips(400);
            bots.add(b);
        }

        /** Phase 10: Create a botLevel 3 archetype bot with a specific simulated archetype. */
        private void createArchetypeBot(PokerBot.CognitiveArchetype archetype) {
            PokerPlayer[] unlocker = { new PokerPlayer("edj") };
            PokerBot b = new PokerBot(unlocker);
            b.setBotLevel(3);
            b.setSimulatedArchetype(archetype);
            b.setProtectedMode(false); // Archetype bots are always unprotected
            b.refreshNameTag(unlocker);
            b.removeChips(b.getChips());
            // SHORT_STACKER bots get 499 chips (24.95 BB), the maximum that still falls
            // under the currentStackBB < 25.0 classifier threshold. Their "always shove"
            // strategy is only sound at short stacks; full-stack SS plays a wildly −EV
            // strategy that defeats the archetype's purpose. All other archetypes get
            // 2000 chips (100 BB) — standard cash-game depth.
            int starting = (archetype == PokerBot.CognitiveArchetype.SHORT_STACKER)
                ? 499
                : 2000;
            b.addChips(starting);
            bots.add(b);
        }

        /** Phase 10: Create an ELITE_REG bot (God Bot in pureProtectedGtoMode). */
        private void createEliteRegBot() {
            PokerPlayer[] unlocker = { new PokerPlayer("edj") };
            PokerBot b = new PokerBot(unlocker);
            b.setBotLevel(2);
            b.setProtectedMode(true);  // Protected = no exploits
            b.setNeuralProtectedMode(false); // No matrix = pure GTO
            b.setNightmareIntensity(nightmareIntensity);
            b.refreshNameTag(unlocker);
            b.removeChips(b.getChips());
            b.addChips(400);
            bots.add(b);
        }

        public void setDiagnosticsContextLabels(String modeLabel, String passLabel, int workerId) {
            diagnosticsModeLabel = (modeLabel == null || modeLabel.isEmpty()) ? "SIM" : modeLabel;
            diagnosticsPassLabel = (passLabel == null || passLabel.isEmpty()) ? "BASE" : passLabel;
            diagnosticsWorkerId = workerId;
        }

        private void refreshPlayerLevelIndex() {
            for (PokerBot bot : bots) {
                playerLevelByName.put(bot.getName(), bot.getBotLevel());
            }
        }

        private void registerArchetypeShift(String playerName, int hand) {
            archetypeShiftCounts.merge(playerName, 1, Integer::sum);
            List<Integer> timeline = archetypeShiftHands.computeIfAbsent(playerName, k -> new ArrayList<>());
            if (timeline.size() < SHIFT_TIMELINE_LIMIT) {
                timeline.add(hand);
            }
        }

        private void trackSourceAttributionForHand(double[][] transfers, boolean reachedShowdown) {
            lastHandGodVsDumbNet = 0.0;
            lastHandGodVsSmartNet = 0.0;
            lastHandGodVsArcNet = 0.0;

            lastHandGodOpponentNet = new HashMap<>();

            if (transfers == null || transfers.length == 0) {
                return;
            }

            for (int godSeat = 0; godSeat < bots.size(); godSeat++) {
                PokerBot godBot = bots.get(godSeat);
                if (godBot.getBotLevel() != 2) {
                    continue;
                }
                String godName = godBot.getName();

                for (int oppSeat = 0; oppSeat < bots.size(); oppSeat++) {
                    if (oppSeat == godSeat) {
                        continue;
                    }

                    PokerBot oppBot = bots.get(oppSeat);
                    int oppLevel = oppBot.getBotLevel();
                    if (oppLevel == 2) {
                        continue;
                    }

                    double net = transfers[oppSeat][godSeat] - transfers[godSeat][oppSeat];
                    if (Math.abs(net) < 1e-9) {
                        continue;
                    }

                    String opponentName = oppBot.getName();
                    String key = godName + "->" + opponentName;
                    godOpponentNet.merge(key, net, Double::sum);
                    lastHandGodOpponentNet.merge(key, net, Double::sum);

                    if (oppLevel == 0) {
                        godVsDumbNet += net;
                        lastHandGodVsDumbNet += net;
                    } else if (oppLevel == 1) {
                        godVsSmartNet += net;
                        lastHandGodVsSmartNet += net;
                    } else if (oppLevel == 3) {
                        godVsArcNet += net;
                        lastHandGodVsArcNet += net;
                    }

                }
            }

            if (BotDiagnostics.enabled() && !lastHandGodOpponentNet.isEmpty()) {
                String topPairs = topPairSummary(lastHandGodOpponentNet, 4);
                BotDiagnostics.recordSourceTransferHand(
                        "path=" + (reachedShowdown ? "showdown" : "foldout")
                                + ", topPairs=" + topPairs
                                + ", godVsDumb=" + String.format("%.2f", lastHandGodVsDumbNet)
                                + ", godVsSmart=" + String.format("%.2f", lastHandGodVsSmartNet));
            }

            if (BotDiagnostics.enabled() && handCount > 0 && (handCount % CONVERGENCE_LOG_INTERVAL) == 0) {
                BotDiagnostics.recordSourceTransferRollup(
                        "trigger=periodic-" + CONVERGENCE_LOG_INTERVAL
                                + ", hands=" + handCount
                                + ", godVsDumb=" + String.format("%.2f", godVsDumbNet)
                                + ", godVsSmart=" + String.format("%.2f", godVsSmartNet)
                                + ", godVsArc=" + String.format("%.2f", godVsArcNet)
                                + ", topPairs=" + topPairSummary(godOpponentNet, 5));
            }
        }

        private void validateTransferMatrixAgainstHandProfits(double[][] transfers, long[] handProfitsBySeat) {
            if (transfers == null || handProfitsBySeat == null || transfers.length != handProfitsBySeat.length) {
                return;
            }

            transferValidationHands++;
            int n = handProfitsBySeat.length;
            double sumRowNet = 0.0;
            long sumProfit = 0L;

            for (int seat = 0; seat < n; seat++) {
                double rowNet = 0.0;
                for (int other = 0; other < n; other++) {
                    rowNet += transfers[other][seat] - transfers[seat][other];
                }

                sumRowNet += rowNet;
                sumProfit += handProfitsBySeat[seat];

                double delta = rowNet - handProfitsBySeat[seat];
                double absDelta = Math.abs(delta);
                transferAbsSeatErrorSum += absDelta;
                transferMaxSeatError = Math.max(transferMaxSeatError, absDelta);
            }

            double conservationError = Math.abs(sumRowNet - sumProfit);
            transferAbsConservationErrorSum += conservationError;
            transferMaxConservationError = Math.max(transferMaxConservationError, conservationError);
        }

        private boolean shouldTrackCognitiveBot(PokerBot bot) {
            if (bot == null)
                return false;
            return true; // PHASE 10: Track everyone
        }


        private int getStreetIndex(String street) {
            if ("preflop".equals(street))
                return 0;
            if ("flop".equals(street))
                return 1;
            if ("turn".equals(street))
                return 2;
            if ("river".equals(street))
                return 3;
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

            if (truePreflopHandsObserved == null || truePreflopHandsObserved.length != numPlayers) {
                truePreflopHandsObserved = new long[numPlayers];
                truePreflopVpipCount = new long[numPlayers];
                truePreflopPfrCount = new long[numPlayers];
            }

            cbetFiredOnFlop = false;
            cbetAggressorIndex = -1;
        }

        private void markPreflopActionForTelemetry(int playerIndex, int preActionTableBet, int preActionContribution,
                int paid, int actionCode, int postActionContribution) {
            if (preflopVPIPFlags == null || preflopPFRFlags == null)
                return;
            if (playerIndex < 0 || playerIndex >= bots.size())
                return;
            PokerBot actor = bots.get(playerIndex);
            if (!shouldTrackCognitiveBot(actor))
                return;

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
            if (preflopVPIPFlags == null || preflopPFRFlags == null)
                return;
            for (int i = 0; i < bots.size(); i++) {
                PokerBot actor = bots.get(i);
                if (!shouldTrackCognitiveBot(actor))
                    continue;

                truePreflopHandsObserved[i]++;
                if (preflopVPIPFlags[i])
                    truePreflopVpipCount[i]++;
                if (preflopPFRFlags[i])
                    truePreflopPfrCount[i]++;

                String playerName = actor.getName();
                PokerBot.CognitiveProfile beforeProfile = PokerBot.getOrCreateCognitiveProfile(playerName);
                PokerBot.CognitiveArchetype before = beforeProfile.getArchetype();

                PokerBot.updatePreflopTelemetryTracked(playerName, preflopVPIPFlags[i], preflopPFRFlags[i],
                        PREFLOP_EMA_ALPHA);

                PokerBot.CognitiveProfile afterProfile = PokerBot.getOrCreateCognitiveProfile(playerName);
                PokerBot.CognitiveArchetype after = afterProfile.getArchetype();
                boolean shifted = before != after;
                if (shifted) {
                    registerArchetypeShift(playerName, handCount);
                }

                long samples = Math.max(1L, truePreflopHandsObserved[i]);
                double trueVpip = (double) truePreflopVpipCount[i] / samples;
                double truePfr = (double) truePreflopPfrCount[i] / samples;
                double absVpipErr = Math.abs(afterProfile.vpipEMA - trueVpip);
                double absPfrErr = Math.abs(afterProfile.pfrEMA - truePfr);

                boolean periodic = (samples % CONVERGENCE_LOG_INTERVAL) == 0;
                if (periodic || shifted) {
                    convergenceSamples++;
                    convergenceAbsVpipErrSum += absVpipErr;
                    convergenceAbsPfrErrSum += absPfrErr;

                    String trigger = shifted
                            ? "archetype-shift"
                            : ("periodic-" + CONVERGENCE_LOG_INTERVAL);
                    BotDiagnostics.recordConvergence(
                            playerName,
                            i,
                            samples,
                            trueVpip,
                            truePfr,
                            afterProfile.vpipEMA,
                            afterProfile.pfrEMA,
                            trigger);
                }
            }
        }

        private void markPostflopActionForTelemetry(int streetIndex, int playerIndex, int preActionTableBet,
                int preActionContribution, int paid, int actionCode, int postActionContribution,
                int preflopAggressorIndex) {
            if (streetIndex < 1 || streetIndex > 3)
                return;
            if (postflopAggressionActions == null || postflopAggressionOpportunities == null)
                return;
            if (playerIndex < 0 || playerIndex >= bots.size())
                return;
            PokerBot actor = bots.get(playerIndex);
            if (!shouldTrackCognitiveBot(actor))
                return;

            boolean isAggressiveAction = (actionCode == 3 || actionCode == 4)
                    && postActionContribution > preActionTableBet;
            boolean isCall = (actionCode == 1 && paid > 0);
            boolean isFoldFacingBet = (actionCode == 2 && preActionTableBet > preActionContribution);

            if (isAggressiveAction || isCall || isFoldFacingBet) {
                postflopAggressionOpportunities[playerIndex][streetIndex]++;
                if (isAggressiveAction)
                    postflopAggressionActions[playerIndex][streetIndex]++;
            }

            if (streetIndex == 1) {
                if (!cbetFiredOnFlop && preActionTableBet == 0 && playerIndex == preflopAggressorIndex
                        && isAggressiveAction) {
                    cbetFiredOnFlop = true;
                    cbetAggressorIndex = playerIndex;
                }
                if (cbetFiredOnFlop && playerIndex != cbetAggressorIndex && preActionTableBet > preActionContribution) {
                    foldToCbetOpportunity[playerIndex] = true;
                    if (actionCode == 2)
                        foldedToCbet[playerIndex] = true;
                }
            }
        }

        private String getAFqStreetStatKey(int street) {
            if (street == 1)
                return "AFq_Flop";
            if (street == 2)
                return "AFq_Turn";
            if (street == 3)
                return "AFq_River";
            return "AFq_Preflop";
        }

        private void finalizePostflopTelemetry(boolean reachedShowdown, boolean[] folded) {
            if (postflopAggressionActions == null || postflopAggressionOpportunities == null)
                return;

            for (int i = 0; i < bots.size(); i++) {
                PokerBot actor = bots.get(i);
                if (!shouldTrackCognitiveBot(actor))
                    continue;
                String name = actor.getName();

                for (int street = 1; street <= 3; street++) {
                    int opps = postflopAggressionOpportunities[i][street];
                    if (opps > 0) {
                        double afqValue = (double) postflopAggressionActions[i][street] / opps;
                        PokerBot.updateCognitiveStatTracked(name, getAFqStreetStatKey(street), afqValue,
                                POSTFLOP_EMA_ALPHA);
                    }
                }

                if (foldToCbetOpportunity[i]) {
                    PokerBot.updateCognitiveStatTracked(name, "FoldToCBet", foldedToCbet[i] ? 1.0 : 0.0,
                            POSTFLOP_EMA_ALPHA);
                }

                if (sawFlopThisHand != null && sawFlopThisHand[i]) {
                    boolean survivedToShowdown = reachedShowdown && !folded[i];
                    PokerBot.updateCognitiveStatTracked(name, "WTSD", survivedToShowdown ? 1.0 : 0.0,
                            POSTFLOP_EMA_ALPHA);
                }
            }
        }

        public void setPerHandNeuralResetEnabled(boolean enabled) {
            this.resetNeuralMemoryEachHand = enabled;
        }

        public void setAievEnabled(boolean enabled) {
            this.aievEnabled = enabled;
        }

        public int[] getBusts() {
            return bustsByTier;
        }

        public long[] getTierNetProfits() {
            return tierNetProfits.clone();
        }

        public long[] getTierFoldoutNetProfits() {
            return tierFoldoutNetProfits.clone();
        }

        public long[] getTierShowdownNetProfits() {
            return tierShowdownNetProfits.clone();
        }

        public double[] getTierProfitSquares() {
            return tierSumSquaresOfProfits.clone();
        }

        public double[] getTierAievNetProfits() {
            return tierAievNetProfits.clone();
        }

        public double[] getTierAievProfitSquares() {
            return tierAievProfitSquares.clone();
        }

        public long[] getTierAievHandsPlayed() {
            return tierAievHandsPlayed.clone();
        }

        public long getAievAdjustedHands() {
            return aievAdjustedHands;
        }

        public long[] getLastHandTierRawProfits() {
            return lastHandTierRawProfits.clone();
        }

        public double[] getLastHandTierAievProfits() {
            return lastHandTierAievProfits.clone();
        }

        public double getGodVsDumbNet() {
            return godVsDumbNet;
        }

        public double getGodVsSmartNet() {
            return godVsSmartNet;
        }

        public double getGodVsArcNet() {
            return godVsArcNet;
        }


        public Map<String, Double> getGodOpponentNetSnapshot() {
            return new HashMap<>(godOpponentNet);
        }

        public Map<String, Integer> getPlayerLevelByNameSnapshot() {
            return new HashMap<>(playerLevelByName);
        }

        public double getLastHandGodVsDumbNet() {
            return lastHandGodVsDumbNet;
        }

        public double getLastHandGodVsSmartNet() {
            return lastHandGodVsSmartNet;
        }

        public double getLastHandGodVsArcNet() {
            return lastHandGodVsArcNet;
        }


        public Map<String, Double> getLastHandGodOpponentNetSnapshot() {
            return new HashMap<>(lastHandGodOpponentNet);
        }

        public Map<String, Integer> getArchetypeShiftCountsSnapshot() {
            return new HashMap<>(archetypeShiftCounts);
        }

        public Map<String, List<Integer>> getArchetypeShiftHandsSnapshot() {
            Map<String, List<Integer>> snapshot = new HashMap<>();
            for (Map.Entry<String, List<Integer>> entry : archetypeShiftHands.entrySet()) {
                snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return snapshot;
        }

        public long getConvergenceSamples() {
            return convergenceSamples;
        }

        public double getConvergenceAbsVpipErrSum() {
            return convergenceAbsVpipErrSum;
        }

        public double getConvergenceAbsPfrErrSum() {
            return convergenceAbsPfrErrSum;
        }

        public long getTransferValidationHands() {
            return transferValidationHands;
        }

        public double getTransferAbsSeatErrorSum() {
            return transferAbsSeatErrorSum;
        }

        public double getTransferMaxSeatError() {
            return transferMaxSeatError;
        }

        public double getTransferAbsConservationErrorSum() {
            return transferAbsConservationErrorSum;
        }

        public double getTransferMaxConservationError() {
            return transferMaxConservationError;
        }

        public long[] getTierHandsPlayed() {
            return tierHandsPlayed.clone();
        }

        public int[] getWinsByTier() {
            return winsByTier.clone();
        }

        public int getHandCount() {
            return handCount;
        }

        public long[] getTierNetGold() {
            if (isTrueEvMode) {
                return tierNetProfits.clone();
            }
            long[] netGold = new long[4];
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
            if (net[1] > net[0])
                best = 1;
            if (net[2] > net[best])
                best = 2;
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
                        System.out.println(
                                "\n🏆 WINNER: " + (winner != null ? winner.getName() : "None") + " takes the game!");
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
            if (verbose)
                printFinalReport(null, true);
        }

        public void runSingleTrueEvHand(boolean verbose) {
            this.isTrueEvMode = true;
            this.replacementMode = true;
            for (PokerBot b : bots) {
                b.removeChips(b.getChips());
                b.addChips(400);
            }
            Collections.shuffle(bots);
            handCount++;
            runHand(handCount, verbose, true);
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
            if (verbose)
                printFinalReport(null, true);
        }

        public void printFinalReport(PokerBot overallWinner, boolean showBusts) {
            System.out.println("\n--- FINAL TELEMETRY REPORT ---");
            if (overallWinner != null) {
                System.out.println(
                        "Winner: " + overallWinner.getName() + " [" + getTierCode(overallWinner.getBotLevel()) + "]");
            }
            System.out.println("Total Hands Processed: " + handCount);

            if (showBusts && handCount > 0) {
                System.out.printf("Bust Rates -> Dumb: %d (%.2f%%), Smart: %d (%.2f%%), God: %d (%.2f%%), Arc: %d (%.2f%%)\n",
                        bustsByTier[0], (bustsByTier[0] * 100.0 / handCount),
                        bustsByTier[1], (bustsByTier[1] * 100.0 / handCount),
                        bustsByTier[2], (bustsByTier[2] * 100.0 / handCount));

                long[] net = getTierNetGold();
                System.out.printf("Net Gold   -> Dumb: %s✨%d, Smart: %s✨%d, God: %s✨%d, Arc: %s✨%d\n",
                        (net[0] >= 0 ? "+" : ""), net[0], (net[1] >= 0 ? "+" : ""), net[1], (net[2] >= 0 ? "+" : ""),
                        net[2]);

                if (tierHandsPlayed[0] > 0) {
                    System.out.println("\n--- ADVANCED TELEMETRY (Tier Overall) ---");
                    for (int lvl = 0; lvl < 4; lvl++) {
                        String name = lvl == 0 ? "Dumb" : (lvl == 1 ? "Smart" : (lvl == 2 ? "God" : "Arc"));
                        double meanProfit = (double) tierNetProfits[lvl] / tierHandsPlayed[lvl];
                        double bb100 = (meanProfit / blinds) * 100.0;
                        double variance = (tierSumSquaresOfProfits[lvl] / tierHandsPlayed[lvl])
                                - (meanProfit * meanProfit);
                        double stdev = Math.sqrt(Math.max(0, variance));
                        double ciMargin = 1.96 * (stdev / Math.sqrt(tierHandsPlayed[lvl]));
                        double ciMarginBb100 = (ciMargin / blinds) * 100.0;
                        System.out.printf("%s BB/100 -> %.2f (95%% CI: %.2f to %.2f)\n", name, bb100,
                                bb100 - ciMarginBb100, bb100 + ciMarginBb100);
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
                        (winsByTier[2] * 100.0 / totalWins), (winsByTier[1] * 100.0 / totalWins),
                        (winsByTier[0] * 100.0 / totalWins));
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
            refreshPlayerLevelIndex();
            BotDiagnostics.setSimulatorContext(diagnosticsModeLabel, diagnosticsPassLabel, diagnosticsWorkerId, id,
                    "PREFLOP");
            boolean handInteractive = interactive;

            try {
                deck.reset();
                int numPlayers = bots.size();
                long[] initialChips = new long[numPlayers];
                for (int i = 0; i < numPlayers; i++)
                    initialChips[i] = bots.get(i).getChips();
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
                for (int i = 0; i < numPlayers; i++) {
                    PokerBot bot = bots.get(i);
                    bot.setHand(holeCards[i]);
                    if (shouldTrackCognitiveBot(bot)) {
                        PokerBot.CognitiveProfile profile = PokerBot.getOrCreateCognitiveProfile(bot.getName());
                        profile.handsPlayed++;
                        profile.currentStackBB = (blinds > 0) ? (double) bot.getChips() / blinds : 100.0;
                        profile.ltmAlpha = PokerSimulator.ltmAlpha;
                    }
                }

                int sbIdx = (id - 1) % numPlayers;
                int safety = 0;
                while (bots.get(sbIdx).getChips() <= 0 && safety++ < numPlayers)
                    sbIdx = (sbIdx + 1) % numPlayers;
                int bbIdx = (sbIdx + 1) % numPlayers;
                safety = 0;
                while (bots.get(bbIdx).getChips() <= 0 && safety++ < numPlayers)
                    bbIdx = (bbIdx + 1) % numPlayers;

                int preflopAggressorIndex = bbIdx;

                payBlind(sbIdx, blinds / 2, contributions, pot);
                payBlind(bbIdx, blinds, contributions, pot);

                String[] streetNames = { "preflop", "flop", "turn", "river" };
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
                boolean allInRunoutLocked = false;
                Card[] allInLockBoard = null;
                int[] allInLockContributions = null;
                boolean[] allInLockFolded = null;

                // Per-street EV instrumentation — track last street where betting had
                // ≥2 active players. Updated AFTER the early-break check so it reflects
                // the street where decisions actually occurred.
                int endedAtStreet = 0;
                int streetIdx = -1;

                for (String street : streetNames) {
                    streetIdx++;
                    BotDiagnostics.updateSimulatorStreet(street.toUpperCase());
                    if (!street.equals("preflop")) {
                        if (street.equals("flop")) {
                            if (fixedBoard == null)
                                deck.deal();
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
                        currentBet = 0;
                        lastRaise = blinds;
                        for (int i = 0; i < numPlayers; i++)
                            contributions[i] = 0;

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
                    if (countActive(folded) <= 1)
                        break;
                    endedAtStreet = streetIdx; // street where betting actually started with ≥2 active

                    if (handInteractive) {
                        System.out.println("\n--- " + street.toUpperCase() + " ---");
                        if (board.length > 0) {
                            System.out.print("Board: ");
                            for (Card c : board)
                                System.out.print(c.getValue() + " ");
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
                            int[] action = actor.action(street, contributions[i], currentBet, blinds, lastRaise, board,
                                    pot.getTotalPot(), participants, i, preflopAggressorIndex, sbIdx, bbIdx);

                            boolean actionIsFold = (action[0] == 2);
                            boolean actionIsCheck = false;
                            boolean actionIsAggressive = false;
                            boolean actorIsHuSmart = huSmartGod && i == huSmartIdx;
                            boolean actorIsHuGod = huSmartGod && i == huGodIdx;

                            if (actionIsFold) {
                                folded[i] = true;
                                if (handInteractive)
                                    System.out.println(actor.getName() + ": FOLDS");
                            } else {
                                int totalContribution = (action[0] == 1) ? (currentBet - contributions[i]) : action[1];
                                paid = Math.min(actor.getChips(), totalContribution);
                                contributions[i] += paid;
                                pot.addPlayerContribution(i, paid);

                                actionIsCheck = (action[0] == 1 && preActionCurrentBet == 0
                                        && contributions[i] == preActionContribution);
                                actionIsAggressive = (contributions[i] > preActionCurrentBet);

                                if (handInteractive) {
                                    String actionVerb = (action[0] == 4 || actor.getChips() == 0) ? "ALL-IN ✨"
                                            : (currentBet == 0 ? "BETS ✨" : "RAISES to ✨");
                                    if (action[0] == 1) {
                                        boolean isBigBlindCheck = (paid == 0 && street.equals("preflop") && i == bbIdx
                                                && currentBet == blinds);
                                        System.out.println(actor.getName() + ": "
                                                + ((paid == 0 && currentBet == 0) || isBigBlindCheck ? "CHECKS"
                                                        : "CALLS ✨" + contributions[i]));
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
                                    if (street.equals("preflop"))
                                        preflopAggressorIndex = i; // Track for C-bets
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
                                    if (actorIsHuGod && preflopAggressorIndex == huGodIdx && preActionCurrentBet == 0
                                            && actionIsAggressive) {
                                        huFlopCbetResponsePending = true;
                                        huGodCbetFlopSeen = true;
                                    }

                                    if (huFlopCbetResponsePending && actorIsHuSmart) {
                                        PokerBot.observeSmartFlopCbetResponseHU(huSmartName, actionIsFold,
                                                actionIsAggressive);
                                        huFlopCbetResponsePending = false;
                                    }
                                } else if (street.equals("turn")) {
                                    if (turnFirstActor == -1) {
                                        turnFirstActor = i;
                                        turnFirstActorChecked = actionIsCheck;
                                    } else if (!turnCheckBackObserved && actorIsHuSmart && turnFirstActor == huGodIdx
                                            && turnFirstActorChecked) {
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
                                markPreflopActionForTelemetry(i, preActionCurrentBet, preActionContribution, paid,
                                        action[0], contributions[i]);
                            } else {
                                markPostflopActionForTelemetry(streetIndex, i, preActionCurrentBet,
                                        preActionContribution, paid, action[0], contributions[i],
                                        preflopAggressorIndex);
                            }

                            if (aievEnabled && !allInRunoutLocked && shouldCaptureAllInRunoutLock(folded)) {
                                allInRunoutLocked = true;
                                allInLockBoard = Arrays.copyOf(board, board.length);
                                allInLockContributions = pot.getContributions().clone();
                                allInLockFolded = folded.clone();
                            }
                        }
                        playersActed++;
                        i = (i + 1) % numPlayers;

                        if (countActive(folded) <= 1)
                            break;

                        // Betting Round End Condition:
                        // 1. Everyone has had a chance to act (playersActed >= numPlayers)
                        // 2. The next player to act has already matched the current bet (or is
                        // out/all-in)
                        if (playersActed >= numPlayers) {
                            if (lastAggressor == -1 || contributions[i] == currentBet || bots.get(i).getChips() == 0
                                    || folded[i]) {
                                roundDone = true;
                            }
                        }
                    }
                    if (street.equals("preflop")) {
                        finalizePreflopTelemetry();
                    }
                    if (handInteractive) {
                        System.out.println("[Enter to continue, or 'skip' for showdown]");
                        String in = sc.nextLine();
                        if (in.equalsIgnoreCase("skip")) {
                            handInteractive = false;
                        }
                    }
                }

                int winnerIdx = -1;
                int winAmount = pot.getTotalPot();
                String winningHand = "N/A";
                int[] totalContributions = pot.getContributions().clone();
                double[][] handTransfers = new double[numPlayers][numPlayers];

                if (countActive(folded) == 1) {
                    for (int i = 0; i < numPlayers; i++)
                        if (!folded[i])
                            winnerIdx = i;
                    bots.get(winnerIdx).addChips(winAmount);
                    winsByTier[bots.get(winnerIdx).getBotLevel()]++;
                    winningHand = "FOLD-OUT";

                    for (int payer = 0; payer < numPlayers; payer++) {
                        int amount = totalContributions[payer];
                        if (amount > 0) {
                            handTransfers[payer][winnerIdx] += amount;
                        }
                    }
                } else {
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

                        int[] sidePayoutBySeat = new int[numPlayers];

                        if (pIdx == 0) {
                            mainPotWinners = new ArrayList<>(winnerIndices);
                            winningHand = getRankingName(outcome.bestRank);
                        }

                        int share = sidePot.amount / winnerIndices.size();
                        int remainder = sidePot.amount % winnerIndices.size();

                        // Randomize remainder distribution start to avoid seat-index bias.
                        int startWinner = (int) (Math.random() * winnerIndices.size());
                        for (int k = 0; k < winnerIndices.size(); k++) {
                            int winnerIdxInList = (startWinner + k) % winnerIndices.size();
                            int idx = winnerIndices.get(winnerIdxInList);

                            int amount = share + (remainder > 0 ? 1 : 0);
                            if (remainder > 0) {
                                remainder--;
                            }

                            bots.get(idx).addChips(amount);
                            sidePayoutBySeat[idx] += amount;
                            handWinners.add(idx);
                        }
                        paidOut += sidePot.amount;
                        applySidePotTransfers(sidePot, sidePayoutBySeat, handTransfers);
                    }

                    // Safety net: preserve chip conservation even if a rare edge case misses a
                    // slice.
                    int delta = winAmount - paidOut;
                    if (delta != 0) {
                        int deltaReceiver = -1;
                        if (!mainPotWinners.isEmpty()) {
                            deltaReceiver = mainPotWinners.get(0);
                            bots.get(deltaReceiver).addChips(delta);
                            handWinners.add(deltaReceiver);
                        } else {
                            for (int i = 0; i < numPlayers; i++) {
                                if (!folded[i]) {
                                    deltaReceiver = i;
                                    bots.get(i).addChips(delta);
                                    handWinners.add(i);
                                    break;
                                }
                            }
                        }
                        applyDeltaTransfers(totalContributions, deltaReceiver, delta, handTransfers);
                    }

                    for (int idx : handWinners) {
                        winsByTier[bots.get(idx).getBotLevel()]++;
                    }
                    winnerIdx = (mainPotWinners.size() == 1) ? mainPotWinners.get(0) : -1;
                }

                boolean reachedShowdown = (countActive(folded) > 1);
                trackSourceAttributionForHand(handTransfers, reachedShowdown);
                finalizePostflopTelemetry(reachedShowdown, folded);

                // Track hand-level profit for BB/100 and variance
                long[] handProfits = new long[4];
                long[] handProfitsBySeat = new long[numPlayers];
                for (int i = 0; i < numPlayers; i++) {
                    long profit = bots.get(i).getChips() - initialChips[i];
                    handProfitsBySeat[i] = profit;
                    int lvl = bots.get(i).getBotLevel();
                    handProfits[lvl] += profit;
                }

                // Per-street EV instrumentation: record where this hand ended + each bot's net delta.
                for (int i = 0; i < numPlayers; i++) {
                    PokerBot pb = bots.get(i);
                    if (shouldTrackCognitiveBot(pb)) {
                        PokerBot.CognitiveProfile profile = PokerBot.getOrCreateCognitiveProfile(pb.getName());
                        profile.recordHandEnd(endedAtStreet, handProfitsBySeat[i]);
                    }
                }

                validateTransferMatrixAgainstHandProfits(handTransfers, handProfitsBySeat);

                double[] aievHandProfits = new double[4];
                boolean aievApplied = false;
                if (aievEnabled
                        && reachedShowdown
                        && allInRunoutLocked
                        && allInLockBoard != null
                        && allInLockContributions != null
                        && allInLockFolded != null
                        && allInLockBoard.length < 5) {
                    double[] expectedPayouts = estimateExpectedPayoutsFromAllInLock(allInLockContributions,
                            allInLockFolded, allInLockBoard, holeCards);
                    if (expectedPayouts != null) {
                        for (int i = 0; i < numPlayers; i++) {
                            double expectedProfit = expectedPayouts[i] - allInLockContributions[i];
                            int lvl = bots.get(i).getBotLevel();
                            aievHandProfits[lvl] += expectedProfit;
                        }
                        aievApplied = true;
                        aievAdjustedHands++;
                    }
                }

                if (!aievApplied) {
                    for (int lvl = 0; lvl < 4; lvl++) {
                        aievHandProfits[lvl] = handProfits[lvl];
                    }
                }

                for (int lvl = 0; lvl < 4; lvl++) {
                    lastHandTierRawProfits[lvl] = handProfits[lvl];
                    lastHandTierAievProfits[lvl] = aievHandProfits[lvl];
                    tierNetProfits[lvl] += handProfits[lvl];
                    if (reachedShowdown) {
                        tierShowdownNetProfits[lvl] += handProfits[lvl];
                    } else {
                        tierFoldoutNetProfits[lvl] += handProfits[lvl];
                    }
                    tierSumSquaresOfProfits[lvl] += (double) handProfits[lvl] * handProfits[lvl];
                    tierAievNetProfits[lvl] += aievHandProfits[lvl];
                    tierAievProfitSquares[lvl] += aievHandProfits[lvl] * aievHandProfits[lvl];
                    tierAievHandsPlayed[lvl]++;
                    tierHandsPlayed[lvl]++;
                }

                // Always report if hand finished (including ties)
                String rep = "";
                for (int i = 0; i < numPlayers; i++) {
                    // PHASE 10: SHORT_STACKER note — stack is reset by the duel loop (mode 6) or
                    // busted-replacement logic (mode 7), so no mid-hand reset needed here.
                    PokerBot pb = bots.get(i);

                    if (pb.getChips() <= 0 && !folded[i]) { // Track eliminations
                        int lvl = pb.getBotLevel();
                        bustsByTier[lvl]++;
                        if (replacement) {
                            String oldName = bots.get(i).getName();
                            PokerPlayer[] unlocker = { new PokerPlayer("edj") };
                            PokerBot newBot = new PokerBot(unlocker);
                            newBot.setBotLevel(lvl);
                            if (lvl == 3) {
                                newBot.setSimulatedArchetype(bots.get(i).simulatedArchetype);
                            }
                            newBot.setProtectedMode(protectedMode);
                            newBot.setNeuralProtectedMode(neuralProtectedMode);
                            newBot.setNightmareIntensity(nightmareIntensity);
                            newBot.refreshNameTag(unlocker);
                            newBot.removeChips(newBot.getChips());
                            newBot.addChips(400);
                            bots.set(i, newBot);
                            if (verbose)
                                rep += " | [Bust: " + oldName + " -> New " + newBot.getName() + "]";
                        } else {
                            if (verbose)
                                rep += " | [Bust: " + bots.get(i).getName() + "]";
                            folded[i] = true; // Mark as permanently folded/out for this game
                        }
                    }
                }

                if (verbose) {
                    String winnerName = (winnerIdx == -1) ? "MULTIPLE PLAYERS" : bots.get(winnerIdx).getName();
                    System.out.println(
                            "Hand #" + id + ": " + winnerName + " won ✨" + winAmount + " with " + winningHand + rep);
                }
                return winnerIdx;
            } finally {
                BotDiagnostics.clearSimulatorContext();
            }
        }

        private int countActive(boolean[] folded) {
            int count = 0;
            for (boolean f : folded)
                if (!f)
                    count++;
            return count;
        }

        private boolean shouldCaptureAllInRunoutLock(boolean[] folded) {
            int activePlayers = 0;
            int activeWithChips = 0;
            for (int i = 0; i < folded.length; i++) {
                if (folded[i])
                    continue;
                activePlayers++;
                if (bots.get(i).getChips() > 0) {
                    activeWithChips++;
                }
            }
            return activePlayers > 1 && activeWithChips <= 1;
        }

        private static class SidePot {
            final int amount;
            final List<Integer> eligibleIndices;
            final int[] payerSlices;

            SidePot(int amount, List<Integer> eligibleIndices, int[] payerSlices) {
                this.amount = amount;
                this.eligibleIndices = eligibleIndices;
                this.payerSlices = payerSlices;
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
                int[] payerSlices = new int[totalContributions.length];

                for (int i = 0; i < totalContributions.length; i++) {
                    int currentSlice = Math.min(totalContributions[i], threshold)
                            - Math.min(totalContributions[i], previousThreshold);
                    if (currentSlice > 0) {
                        potAmount += currentSlice;
                        payerSlices[i] = currentSlice;
                    }
                    if (totalContributions[i] >= threshold && !folded[i]) {
                        eligibleIndices.add(i);
                    }
                }

                if (potAmount > 0 && !eligibleIndices.isEmpty()) {
                    sidePots.add(new SidePot(potAmount, eligibleIndices, payerSlices));
                }
                previousThreshold = threshold;
            }

            return sidePots;
        }

        private void applySidePotTransfers(SidePot sidePot, int[] sidePayoutBySeat, double[][] handTransfers) {
            if (sidePot == null || sidePayoutBySeat == null || handTransfers == null || sidePot.amount <= 0) {
                return;
            }

            for (int payer = 0; payer < sidePot.payerSlices.length; payer++) {
                int payerSlice = sidePot.payerSlices[payer];
                if (payerSlice <= 0) {
                    continue;
                }

                for (int receiver = 0; receiver < sidePayoutBySeat.length; receiver++) {
                    int payout = sidePayoutBySeat[receiver];
                    if (payout <= 0) {
                        continue;
                    }
                    handTransfers[payer][receiver] += ((double) payerSlice * payout) / sidePot.amount;
                }
            }
        }

        private void applyDeltaTransfers(int[] totalContributions, int receiverIdx, int delta,
                double[][] handTransfers) {
            if (totalContributions == null || handTransfers == null || receiverIdx < 0
                    || receiverIdx >= handTransfers.length || delta == 0) {
                return;
            }

            int denom = 0;
            for (int value : totalContributions) {
                if (value > 0) {
                    denom += value;
                }
            }

            if (denom <= 0) {
                handTransfers[receiverIdx][receiverIdx] += delta;
                return;
            }

            for (int payer = 0; payer < totalContributions.length; payer++) {
                int contribution = totalContributions[payer];
                if (contribution <= 0) {
                    continue;
                }
                handTransfers[payer][receiverIdx] += ((double) delta * contribution) / denom;
            }
        }

        private double[] estimateExpectedPayoutsFromAllInLock(int[] totalContributions, boolean[] folded,
                Card[] boardPrefix, Card[][] holeCards) {
            if (boardPrefix == null || boardPrefix.length >= 5) {
                return null;
            }

            List<SidePot> sidePots = buildSidePots(totalContributions, folded);
            if (sidePots.isEmpty()) {
                return null;
            }

            List<Card> available = buildAvailableRunoutCards(holeCards, boardPrefix);
            int cardsNeeded = 5 - boardPrefix.length;
            if (available.size() < cardsNeeded) {
                return null;
            }

            double[] expectedPayouts = new double[bots.size()];
            int samples = 0;

            if (cardsNeeded == 1) {
                for (Card c1 : available) {
                    Card[] board = new Card[5];
                    System.arraycopy(boardPrefix, 0, board, 0, boardPrefix.length);
                    board[boardPrefix.length] = c1;
                    addExpectedPayoutsForBoard(expectedPayouts, sidePots, board, holeCards);
                    samples++;
                }
            } else if (cardsNeeded == 2) {
                for (int i = 0; i < available.size(); i++) {
                    for (int j = i + 1; j < available.size(); j++) {
                        Card[] board = new Card[5];
                        System.arraycopy(boardPrefix, 0, board, 0, boardPrefix.length);
                        board[boardPrefix.length] = available.get(i);
                        board[boardPrefix.length + 1] = available.get(j);
                        addExpectedPayoutsForBoard(expectedPayouts, sidePots, board, holeCards);
                        samples++;
                    }
                }
            } else {
                for (int s = 0; s < AIEV_RUNOUT_SAMPLE_CAP; s++) {
                    int[] picks = sampleUniqueIndices(available.size(), cardsNeeded);
                    Card[] board = new Card[5];
                    System.arraycopy(boardPrefix, 0, board, 0, boardPrefix.length);
                    for (int k = 0; k < cardsNeeded; k++) {
                        board[boardPrefix.length + k] = available.get(picks[k]);
                    }
                    addExpectedPayoutsForBoard(expectedPayouts, sidePots, board, holeCards);
                    samples++;
                }
            }

            if (samples == 0) {
                return null;
            }

            for (int i = 0; i < expectedPayouts.length; i++) {
                expectedPayouts[i] /= samples;
            }
            return expectedPayouts;
        }

        private List<Card> buildAvailableRunoutCards(Card[][] holeCards, Card[] boardPrefix) {
            Set<String> used = new HashSet<>();
            if (holeCards != null) {
                for (Card[] hand : holeCards) {
                    if (hand == null)
                        continue;
                    for (Card c : hand) {
                        if (c != null)
                            used.add(c.getValue());
                    }
                }
            }
            if (boardPrefix != null) {
                for (Card c : boardPrefix) {
                    if (c != null)
                        used.add(c.getValue());
                }
            }

            List<Card> available = new ArrayList<>();
            for (Card c : CANONICAL_DECK) {
                if (!used.contains(c.getValue())) {
                    available.add(c);
                }
            }
            return available;
        }

        private int[] sampleUniqueIndices(int size, int needed) {
            int[] picks = new int[needed];
            boolean[] used = new boolean[size];
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int filled = 0;
            while (filled < needed) {
                int idx = rng.nextInt(size);
                if (!used[idx]) {
                    used[idx] = true;
                    picks[filled++] = idx;
                }
            }
            return picks;
        }

        private void addExpectedPayoutsForBoard(double[] expectedPayouts, List<SidePot> sidePots, Card[] board,
                Card[][] holeCards) {
            for (SidePot sidePot : sidePots) {
                ShowdownOutcome outcome = resolveShowdownForEligible(sidePot.eligibleIndices, board, holeCards);
                if (outcome.winnerIndices.isEmpty()) {
                    continue;
                }
                double split = (double) sidePot.amount / outcome.winnerIndices.size();
                for (int idx : outcome.winnerIndices) {
                    expectedPayouts[idx] += split;
                }
            }
        }

        private ShowdownOutcome resolveShowdownForEligible(List<Integer> eligibleIndices, Card[] board) {
            return resolveShowdownForEligible(eligibleIndices, board, null);
        }

        private ShowdownOutcome resolveShowdownForEligible(List<Integer> eligibleIndices, Card[] board,
                Card[][] holeCardsOverride) {
            List<Integer> winnerIndices = new ArrayList<>();
            int bestRank = 10;
            Card[] absoluteBest = null;

            for (int idx : eligibleIndices) {
                Card[] hole = (holeCardsOverride != null) ? holeCardsOverride[idx] : bots.get(idx).getHand();
                Card[] best = deck.getBestHand(hole, board);
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
                if (folded[i])
                    continue;
                if (first == -1)
                    first = i;
                else if (second == -1)
                    second = i;
                else
                    return new int[] { -1, -1 };
            }

            if (first == -1 || second == -1)
                return new int[] { -1, -1 };

            int firstLevel = bots.get(first).getBotLevel();
            int secondLevel = bots.get(second).getBotLevel();

            if (firstLevel == 1 && secondLevel == 2)
                return new int[] { first, second };
            if (firstLevel == 2 && secondLevel == 1)
                return new int[] { second, first };
            return new int[] { -1, -1 };
        }

        private String getRankingName(int rank) {
            String[] names = { "", "STRAIGHT FLUSH", "FOUR OF A KIND", "FULL HOUSE", "FLUSH", "STRAIGHT",
                    "THREE OF A KIND", "TWO PAIR", "ONE PAIR", "HIGH CARD" };
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
