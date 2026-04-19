import java.util.*;

public class PokerBot extends PokerPlayer {
  private boolean opMode = false;
  private PokerDeck p = new PokerDeck();
  private int[][] hands = { { 14, 14 }, { 14, 13 }, { 14, 12 }, { 14, 11 }, { 14, 10 }, { 14, 9 }, { 14, 8 }, { 14, 2 },
      { 13, 13 }, { 13, 12 }, { 13, 11 }, { 13, 10 }, { 13, 9 }, { 13, 8 }, { 12, 12 }, { 12, 11 }, { 12, 10 },
      { 12, 9 }, { 12, 8 }, { 11, 11 }, { 11, 10 }, { 11, 9 }, { 11, 8 }, { 10, 10 }, { 9, 9 }, { 8, 8 }, { 7, 7 },
      { 6, 6 }, { 5, 5 }, { 4, 4 }, { 3, 3 }, { 2, 2 } }; // preset hands for smart bot
  public int botLevel; // 0 = dumb, 1 = smart, 2 = god
  private boolean cbetFlop = false; // Persistent state for barrelling logic
  private boolean predatoryIntent = false; // "Two-Faced" nightmare personality
  private String baseName; // Store original name to allow tag refreshing

    public enum CognitiveArchetype {
    NIT,
    STATION,
    MANIAC,
    TAG,
    ELITE_REG,
    UNKNOWN
    }
  
  // PHASE 8: The Cognitive Matrix (Advanced Telemetry & Archetyping)
  public static class CognitiveProfile {
      private static final int STYLE_WINDOW = 10;

      public int handsPlayed = 0;

      // EMA Tracking variables
      public double vpipEMA = 0.0;
      public double pfrEMA = 0.0;

      // AFq by street
      public double afqPreflopEMA = 0.0;
      public double afqFlopEMA = 0.0;
      public double afqTurnEMA = 0.0;
      public double afqRiverEMA = 0.0;

      // Postflop action tracking
      public double wtsdEMA = 0.0;
      public double foldToCbetEMA = 0.0;

      // Volatility / gear-shift tracking
      public double vIndex = 0.0;
      public double styleShiftEMA = 0.0;

      // Archetype state
      public CognitiveArchetype archetype = CognitiveArchetype.UNKNOWN;

      // Backward compatibility for Phase 5/6
      public int aggressiveActions = 0;

      private final double[] styleHistory = new double[STYLE_WINDOW];
      private int styleSamples = 0;
      private int styleWriteIndex = 0;
      private double lastStylePoint = 0.0;
      private boolean hasLastStylePoint = false;

      public double getAggressionFactor() {
        if (handsPlayed == 0) {
          return 0.0;
        }
        // Backward-compatible aggression signal now sourced from live EMAs.
        double postflopBlend = getPostflopAFqBlend();
        double legacy = (double) aggressiveActions / Math.max(1, handsPlayed);
        return clamp01((pfrEMA * 0.35) + (postflopBlend * 0.50) + (legacy * 0.15));
      }

      private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
      }

      private double getPostflopAFqBlend() {
        return (afqFlopEMA + afqTurnEMA + afqRiverEMA) / 3.0;
      }

      private double getStylePoint() {
        double postAFq = getPostflopAFqBlend();
        double aggressionBlend = (pfrEMA + postAFq) / 2.0;
        double showdownStickiness = (wtsdEMA + (1.0 - foldToCbetEMA)) / 2.0;
        return clamp01((vpipEMA * 0.40) + (pfrEMA * 0.30) + (aggressionBlend * 0.20) + (showdownStickiness * 0.10));
      }

      private void observeStyleAndVolatility() {
        double stylePoint = getStylePoint();

        if (hasLastStylePoint) {
          double handShift = Math.abs(stylePoint - lastStylePoint);
          styleShiftEMA = (0.35 * handShift) + (0.65 * styleShiftEMA);
        }
        lastStylePoint = stylePoint;
        hasLastStylePoint = true;

        styleHistory[styleWriteIndex] = stylePoint;
        styleWriteIndex = (styleWriteIndex + 1) % STYLE_WINDOW;
        if (styleSamples < STYLE_WINDOW)
          styleSamples++;

        if (styleSamples <= 1) {
          vIndex = 0.0;
          return;
        }

        double mean = 0.0;
        for (int i = 0; i < styleSamples; i++) {
          mean += styleHistory[i];
        }
        mean /= styleSamples;

        double variance = 0.0;
        for (int i = 0; i < styleSamples; i++) {
          double diff = styleHistory[i] - mean;
          variance += diff * diff;
        }
        variance /= styleSamples;
        vIndex = Math.sqrt(Math.max(0.0, variance));
      }

      private void refreshArchetype() {
        double postAFq = getPostflopAFqBlend();
        double totalAFq = (pfrEMA + postAFq) / 2.0;

          boolean volatileStyle = (vIndex >= 0.04) || (styleShiftEMA >= 0.08);
        if (volatileStyle) {
          archetype = CognitiveArchetype.ELITE_REG;
          return;
        }

        if (vpipEMA <= 0.22 && pfrEMA <= 0.14 && totalAFq <= 0.30) {
          archetype = CognitiveArchetype.NIT;
        } else if (vpipEMA >= 0.37 && pfrEMA <= 0.16 && postAFq <= 0.28 && wtsdEMA >= 0.42) {
          archetype = CognitiveArchetype.STATION;
        } else if (vpipEMA >= 0.42 && pfrEMA >= 0.30 && totalAFq >= 0.45) {
          archetype = CognitiveArchetype.MANIAC;
        } else if (vpipEMA >= 0.20 && vpipEMA <= 0.34 && pfrEMA >= 0.16 && pfrEMA <= 0.28
          && totalAFq >= 0.26 && totalAFq <= 0.52) {
          archetype = CognitiveArchetype.TAG;
        } else {
          archetype = CognitiveArchetype.UNKNOWN;
        }
      }

      // EMA update function.
      public void updateEMA(String stat, double value, double alpha) {
        switch (stat) {
          case "VPIP":
            vpipEMA = (alpha * value) + ((1 - alpha) * vpipEMA);
            break;
          case "PFR":
            pfrEMA = (alpha * value) + ((1 - alpha) * pfrEMA);
            break;
          case "AFq_Preflop":
            afqPreflopEMA = (alpha * value) + ((1 - alpha) * afqPreflopEMA);
            break;
          case "AFq_Flop":
            afqFlopEMA = (alpha * value) + ((1 - alpha) * afqFlopEMA);
            break;
          case "AFq_Turn":
            afqTurnEMA = (alpha * value) + ((1 - alpha) * afqTurnEMA);
            break;
          case "AFq_River":
            afqRiverEMA = (alpha * value) + ((1 - alpha) * afqRiverEMA);
            break;
          case "WTSD":
            wtsdEMA = (alpha * value) + ((1 - alpha) * wtsdEMA);
            break;
          case "FoldToCBet":
            foldToCbetEMA = (alpha * value) + ((1 - alpha) * foldToCbetEMA);
            break;
          default:
            break;
        }

        observeStyleAndVolatility();
        refreshArchetype();
      }

      public void updatePreflopTelemetry(boolean vpip, boolean pfr, double alpha) {
        updateEMA("VPIP", vpip ? 1.0 : 0.0, alpha);
        updateEMA("PFR", pfr ? 1.0 : 0.0, alpha);
      }

      public CognitiveArchetype getArchetype() {
        return archetype;
      }

      public String getArchetypeLabel() {
        return archetype.name();
      }
  }

  // Smart-bot specific leak profile for God's heads-up exploitation.
  public static class SmartLeakProfile {
      private static final double ALPHA = 0.30;

      public int huSamples = 0;
      public double foldTo3BetHUEMA = 0.50;
      public double foldToFlopCbetHUEMA = 0.50;
      public double foldToTurnBarrelHUEMA = 0.50;
      public double foldToRiverLargeBetHUEMA = 0.50;
      public double checkBackTurnHUEMA = 0.50;
      public double raiseVsCbetHUEMA = 0.30;
      public double onePairCallDownHUEMA = 0.50;

      private double ema(double current, double observation) {
        return (ALPHA * observation) + ((1.0 - ALPHA) * current);
      }

      public void observeFoldTo3Bet(boolean folded) {
        huSamples++;
        foldTo3BetHUEMA = ema(foldTo3BetHUEMA, folded ? 1.0 : 0.0);
      }

      public void observeFlopCbetResponse(boolean folded, boolean raised) {
        huSamples++;
        foldToFlopCbetHUEMA = ema(foldToFlopCbetHUEMA, folded ? 1.0 : 0.0);
        raiseVsCbetHUEMA = ema(raiseVsCbetHUEMA, raised ? 1.0 : 0.0);
      }

      public void observeTurnBarrelResponse(boolean folded) {
        huSamples++;
        foldToTurnBarrelHUEMA = ema(foldToTurnBarrelHUEMA, folded ? 1.0 : 0.0);
      }

      public void observeRiverLargeBetResponse(boolean folded) {
        huSamples++;
        foldToRiverLargeBetHUEMA = ema(foldToRiverLargeBetHUEMA, folded ? 1.0 : 0.0);
      }

      public void observeTurnCheckBack(boolean checkedBack) {
        huSamples++;
        checkBackTurnHUEMA = ema(checkBackTurnHUEMA, checkedBack ? 1.0 : 0.0);
      }
  }

  private static final ThreadLocal<Map<String, CognitiveProfile>> cognitiveDBLocal = ThreadLocal.withInitial(HashMap::new);
  private static final ThreadLocal<Map<String, SmartLeakProfile>> smartLeakDBLocal = ThreadLocal.withInitial(HashMap::new);
    public static boolean testLearnFromBots = false; // Enables telemetry learning from Dumb/Smart bots for testing.

  public static Map<String, CognitiveProfile> getCognitiveDB() {
    return cognitiveDBLocal.get();
  }

  public static void resetThreadCognitiveDB() {
    cognitiveDBLocal.set(new HashMap<>());
    smartLeakDBLocal.set(new HashMap<>());
  }

  public static void clearThreadCognitiveDB() {
    cognitiveDBLocal.remove();
    smartLeakDBLocal.remove();
  }

  public static Map<String, SmartLeakProfile> getSmartLeakDB() {
    return smartLeakDBLocal.get();
  }

  public static SmartLeakProfile getOrCreateSmartLeakProfile(String playerName) {
    Map<String, SmartLeakProfile> db = getSmartLeakDB();
    db.putIfAbsent(playerName, new SmartLeakProfile());
    return db.get(playerName);
  }

  public static void observeSmartFoldTo3BetHU(String playerName, boolean folded) {
    getOrCreateSmartLeakProfile(playerName).observeFoldTo3Bet(folded);
  }

  public static void observeSmartFlopCbetResponseHU(String playerName, boolean folded, boolean raised) {
    getOrCreateSmartLeakProfile(playerName).observeFlopCbetResponse(folded, raised);
  }

  public static void observeSmartTurnBarrelResponseHU(String playerName, boolean folded) {
    getOrCreateSmartLeakProfile(playerName).observeTurnBarrelResponse(folded);
  }

  public static void observeSmartRiverLargeBetResponseHU(String playerName, boolean folded) {
    getOrCreateSmartLeakProfile(playerName).observeRiverLargeBetResponse(folded);
  }

  public static void observeSmartTurnCheckBackHU(String playerName, boolean checkedBack) {
    getOrCreateSmartLeakProfile(playerName).observeTurnCheckBack(checkedBack);
  }

  public static CognitiveProfile getOrCreateCognitiveProfile(String playerName) {
    Map<String, CognitiveProfile> db = getCognitiveDB();
    db.putIfAbsent(playerName, new CognitiveProfile());
    return db.get(playerName);
  }

  public static void updatePreflopTelemetryTracked(String playerName, boolean vpip, boolean pfr, double alpha) {
    CognitiveProfile profile = getOrCreateCognitiveProfile(playerName);
    CognitiveArchetype before = profile.getArchetype();
    profile.handsPlayed++;
    profile.updatePreflopTelemetry(vpip, pfr, alpha);
    CognitiveArchetype after = profile.getArchetype();
    BotDiagnostics.recordArchetypeShift(playerName, before, after, profile, "preflopTelemetry");
    BotDiagnostics.recordProfileSnapshot(playerName, profile, "preflopTelemetry");
  }

  public static void updateCognitiveStatTracked(String playerName, String stat, double value, double alpha) {
    CognitiveProfile profile = getOrCreateCognitiveProfile(playerName);
    CognitiveArchetype before = profile.getArchetype();
    profile.updateEMA(stat, value, alpha);
    CognitiveArchetype after = profile.getArchetype();
    BotDiagnostics.recordArchetypeShift(playerName, before, after, profile, stat);
    BotDiagnostics.recordProfileSnapshot(playerName, profile, stat);
  }

  public static CognitiveArchetype getTrackedArchetype(String playerName) {
    CognitiveProfile profile = getCognitiveDB().get(playerName);
    if (profile == null)
      return CognitiveArchetype.UNKNOWN;
    return profile.getArchetype();
  }

  private boolean revealTags = false; // Persistent memory for trigger detection
  private boolean nightmareActive = false; // Persistent memory for nightmare mode
  private boolean protectedMode = false; // "Protected Mode": Strips exploits for pure GTO testing
  private boolean neuralProtectedMode = false; // Simulator-only neural sandbox (valid only with Protected Mode)
  private int nightmareIntensity = 2; // Fixed adaptive nightmare mode

  public PokerBot(PokerPlayer[] currentPlayers) {
    super("temp");
    randomName(currentPlayers);
    double r = Math.random();
    if (r < 0.44)
      botLevel = 0;
    else if (r < 0.88)
      botLevel = 1;
    else
      botLevel = 2;

    this.baseName = super.getName();
    refreshNameTag(currentPlayers);

    if (super.getName().contains("Aventurine")) {
      opMode = true;
    }
  }

  public void refreshNameTag(PokerPlayer[] playersForNightmareCheck) {
    // Trigger Check: Only update global state if a player list is provided
    if (playersForNightmareCheck != null) {
      this.revealTags = false;
      this.nightmareActive = false;
      for (PokerPlayer p : playersForNightmareCheck) {
        if (p != null) {
          if ("edj".equalsIgnoreCase(p.getName()) || "edjiang1234".equalsIgnoreCase(p.getName())) {
            this.revealTags = true;
          }
          if ("edjiang1234".equalsIgnoreCase(p.getName())) {
            this.nightmareActive = true;
            if (botLevel != 2) {
              botLevel = 2; // Sync level if nightmare triggered
              predatoryIntent = (Math.random() < 0.5);
            }
          }
        }
      }
    }

    String tag = "";
    if (this.revealTags) {
      if (botLevel == 0)
        tag = " [D]";
      else if (botLevel == 1)
        tag = " [S]";
      else {
        if (this.nightmareActive) {
          tag = predatoryIntent ? " [G-B]" : " [G-S]";
        } else {
          tag = " [G]";
        }
      }
    }
    super.setName(this.baseName + tag);
  }

  public PokerBot() {
    this(null);
  }

  public void randomName(PokerPlayer[] currentPlayers) {
    super.setName(Names.getUniqueName(currentPlayers));
  }

  public void randomName() {
    randomName(null);
  }

  public int getBotLevel() {
    return botLevel;
  }


  public void setBotLevel(int level) {
    this.botLevel = level;
    if (level == 2 && !protectedMode) predatoryIntent = (Math.random() < 0.5); // Re-roll intent for promoted gods (disable if protected)
    refreshNameTag(null); // Simple refresh (Nightmare mode handled in constructor or re-verified here if needed)
  }

  public void setProtectedMode(boolean val) {
    this.protectedMode = val;
    if (val) {
      predatoryIntent = false; // Immediately disable aggression
    } else {
      neuralProtectedMode = false; // Hard guard: neural sandbox cannot survive outside protected mode.
    }
    refreshNameTag(null);
  }

  public void setNeuralProtectedMode(boolean enabled) {
    // Neural sandbox is constrained to Protected Mode by design.
    this.neuralProtectedMode = enabled && this.protectedMode;
  }

  public boolean isNeuralProtectedMode() {
    return protectedMode && neuralProtectedMode;
  }

  private boolean neuralSandboxEnabled() {
    return protectedMode && neuralProtectedMode;
  }

  public void setNightmareIntensity(int val) {
    this.nightmareIntensity = 2;
  }

  public int getNightmareIntensity() {
    return nightmareIntensity;
  }

  private void trace(String stage, String message) {
    if (!BotDiagnostics.enabled()) {
      return;
    }
    System.out.println("[BOT TRACE][" + super.getName() + "][" + stage + "] " + message);
  }

  private String cardsToString(Card[] cards) {
    if (cards == null || cards.length == 0)
      return "(none)";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cards.length; i++) {
      if (cards[i] == null)
        continue;
      if (sb.length() > 0)
        sb.append(" ");
      sb.append(cards[i].getValue());
    }
    return sb.length() == 0 ? "(none)" : sb.toString();
  }

  private String actionLabel(int actionCode) {
    switch (actionCode) {
      case 1:
        return "CALL/CHECK";
      case 2:
        return "FOLD";
      case 3:
        return "BET/RAISE";
      case 4:
        return "ALL-IN";
      default:
        return "UNKNOWN";
    }
  }

  private void traceTableState(String stage, PokerPlayer[] players, Card[] board, int seatIndex) {
    if (players == null)
      return;
    String boardText = (board == null) ? "(preflop)" : cardsToString(board);
    trace(stage, "TABLE SNAPSHOT | board=" + boardText + ", seatIndex=" + seatIndex);
    for (int i = 0; i < players.length; i++) {
      PokerPlayer pl = players[i];
      if (pl == null)
        continue;
      String marker = (i == seatIndex) ? " <- ACTING" : "";
      trace(stage,
          "seat=" + i + ", name=" + pl.getName() + ", inHand=" + pl.inHand() + ", chips=" + pl.getChips()
              + ", hole=" + cardsToString(pl.getHand()) + marker);
    }
  }

  // funny
  public void checkName() {
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public int[] action(String round, int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize, PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int tablePlayers = 0;
    for (PokerPlayer p : players) if (p.getChips() > 0) tablePlayers++;
    boolean headsUpTable = (tablePlayers == 2);
    
    int activeCount = 0;
    for (PokerPlayer p : players) if (p.inHand()) activeCount++;
    boolean headsUpHand = (activeCount == 2);

    String stage = "ACTION-" + round.toUpperCase();
    if (botLevel == 1 || botLevel == 2) {
      trace(stage,
          "ENTER | level=" + botLevel + ", hole=" + cardsToString(super.getHand()) + ", prevBet=" + prevBet
              + ", tableBet=" + bet + ", blind=" + blind + ", lastRaise=" + lastRaise + ", pot=" + potSize
              + ", headsUpTable=" + headsUpTable + ", headsUpHand=" + headsUpHand);
      traceTableState(stage, players, board, seatIndex);
    }

    if (botLevel == 2) {
      if (round.equals("preflop")) {
        return godPreflop(prevBet, bet, blind, lastRaise, players, seatIndex, sbIdx, bbIdx);
      } else {
        return godPostflop(prevBet, bet, blind, lastRaise, board, potSize, players, seatIndex, preflopAggressorIndex, sbIdx, bbIdx);
      }
    } else if (botLevel == 0) { // idiot bot code, fixed percentages for all situations no matter what
      int[] action = new int[2];
      double rand = Math.random();
      if (opMode && super.getChips() > 0) {
        action[0] = 4;
        action[1] = super.getChips();
      } else if (bet < super.getChips()) {
        if (rand >= 0 && rand < 0.75) { // 75% chance to call
          action[0] = 1;
          if (bet > 0) {
            action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
          } else
            action[1] = (bet == 0) ? 0 : bet - prevBet;
        } else if (rand >= 0.75 && rand < 0.85) { // 10% chance to raise
          if (((bet == 0) ? blind : bet + blind) + super.getChips() / 10 < super.getChips()) {
            // however, only raises if the bet meets certain conditions
            int max;
            int min;
            if (round.equals("preflop")) {
              max = super.getChips() / 10 + bet + lastRaise;
              min = bet + lastRaise;
            } else {
              if (bet == 0) {
                max = super.getChips() / 10 + lastRaise;
                min = lastRaise;
              } else {
                max = super.getChips() / 10 + bet + lastRaise;
                min = bet + lastRaise;
              }
            }
            action[0] = 3;
            int raiseTo = (int) (Math.random() * (max - min + 1) + min);
            action[1] = raiseTo - prevBet;
          } else {
            // if those "conditions" are not met, then has 15% to continue and all in the current bet, otherwise folds.
            if (Math.random() > 0.85) {
              action[0] = 4;
              action[1] = super.getChips();
            } else {
              action[0] = 2;

            }
          }
        } else if (rand >= 0.85 && rand < 0.97) { // 12% chance to call/fold
          action[0] = (bet == 0) ? 1 : 2;
        } else { // 3% chance to all in
          action[0] = 4;
          action[1] = super.getChips();
        }
      } else {
        // Dumb Bot Extinction Rule: 50% Call, 50% Fold
        if (Math.random() < 0.5) {
          action[0] = 4;
          action[1] = super.getChips();
        } else {
          action[0] = 2;
          action[1] = 0;
        }
      }
      return action;
    } else { // intelligent bot code, varying percentages based on current siutation
      int[] action = new int[2];
      if (round.equals("preflop")) {
        /*
         * stack, otherwise, has a 80% chance to call and 20% chance of folding.
         * for the former, 20% chance to raise, 80% chance to call.
         * if hand is out of range, has a 25% chance to call and 75% chance of folding.
         */
        boolean isHand = false;
        String smartReason = "preflop-default";
        trace("SMART-PREFLOP",
            "START | hole=" + cardsToString(super.getHand()) + ", prevBet=" + prevBet + ", tableBet=" + bet
                + ", lastRaise=" + lastRaise + ", stack=" + super.getChips() + ", headsUpTable=" + headsUpTable);
        for (int[] h : hands) {
          if (Arrays.equals(Deck.cardToInt(super.getHand()), h)) {
            isHand = true;
            if (bet < super.getChips() / 2) {
              if (Math.random() < 0.2) {
                smartReason = "in-range hand + affordable pressure lane => raise";
                action[0] = 3;
                int raiseTo = (bet == 0) ? (int) (blind * (Math.random() + 2)) : (int) (bet * (Math.random() + 2));
                raiseTo = Math.max(raiseTo, bet + lastRaise); // Standardized Floor
                action[1] = Math.min(raiseTo - prevBet, super.getChips());
              } else {
                smartReason = "in-range hand + affordable pressure lane => call";
                action[0] = 1;
                if (bet > 0) {
                  action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
                } else
                  action[1] = (bet == 0) ? 0 : bet - prevBet;
              }
            } else {
              if (Math.random() < 0.8) {
                smartReason = "in-range hand + expensive bet => defensive call";
                action[0] = 1;
                if (bet > 0) {
                  action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
                } else
                  action[1] = (bet == 0) ? 0 : bet - prevBet;
              } else {
                smartReason = "in-range hand + expensive bet => disciplined fold";
                action[0] = 2;
              }
            }
            break;
          }
        }
        if (!isHand) {
          double callAirFreq = (headsUpTable) ? 0.70 : 0.25;
          int[] cardInts = Deck.cardToInt(super.getHand());
          if (headsUpTable && (cardInts[0] >= 12 || cardInts[1] >= 12)) callAirFreq = 1.0; // Play any Q+ preflop 1v1

          if (bet < super.getChips() / 2 && Math.random() < callAirFreq) {
            smartReason = "out-of-range hand + affordable bet + RNG under callAirFreq=" + callAirFreq + " => call";
            action[0] = 1;
            action[1] = (bet == 0) ? 0 : bet - prevBet;
          } else {
            smartReason = "out-of-range hand => fold";
            action[0] = 2;
          }
        }
        trace("SMART-PREFLOP",
            "RESOLVE | inPresetRange=" + isHand + ", reason=" + smartReason + ", action="
                + actionLabel(action[0]) + ", amount=" + action[1]);
      } else {
        // three betting settings: 0 - call 1 - bets/raise to 100-150% of current bet, 2
        // - 150-300% of current bet, 3 - 300% to all in. 4 - fold
        // if this has straight or flush: 0 - 25%, 1 - 55%, 2 - 15%, 3 - 5%, 4 - 0%.
        // if full house: 0 - 5%, 1 - 35%, 2 - 45%, 3 - 15%, 4 - 0%.
        // if four of kind or straight flush: 0 - 0%, 1 - 15%, 2 - 25%, 3 - 60%, 4 - 0%.
        // drawing hand (2 pair, 3 o kind, straight/flush draw): 0 - 50%, 1 - 30%, 2 -
        // 5%, 3 - 5%, 4 - 10% [given bet < half of curr stack], otherwise: 0 - 30%, 1 -
        // 15%, 2 - 5%, 3 - 0%, 4 - 50%. replace fold chance with check chance
        // all other hands: 0 - 65%, 1 - 10%, 2 - 5%, 3 - 0%, 4 - 20% [given bet < third
        // of curr stack], otherwise: 0 - 25%, 1 - 5%, 2 - 0%, 3 - 0%, 4 - 70%, replace
        // fold chance with check chance if applicable
        // bot's behavior in a nutshell every time on its turn
        int subAction = -1;
        String smartReason = "postflop-default";
        Card[] hand = new Card[5];
        if (board.length == 3) {
          hand[0] = super.getHand()[0];
          hand[1] = super.getHand()[1];
          for (int i = 0; i < 3; i++)
            hand[i + 2] = board[i];
        } else {
          hand = p.getBestHand(super.getHand(), board);
        }
        int rank = p.getRanking(hand);
        int rand = (int) (Math.random() * 101 + 1);
        trace("SMART-POSTFLOP",
            "START | hole=" + cardsToString(super.getHand()) + ", board=" + cardsToString(board) + ", best5="
                + cardsToString(hand) + ", rank=" + rank + ", randRoll=" + rand + ", prevBet=" + prevBet
                + ", tableBet=" + bet + ", lastRaise=" + lastRaise + ", stack=" + super.getChips());
        if (rank == 5 || rank == 4) {
          smartReason = "strong made hand bucket (straight/flush)";
          if (rand <= 25) {
            subAction = 0;
          } else if (rand > 25 && rand <= 80) {
            subAction = 1;
          } else if (rand > 80 && rand <= 95) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else if (rank == 3) {
          smartReason = "monster bucket (full house)";
          if (rand <= 5) {
            subAction = 0;
          } else if (rand > 5 && rand <= 40) {
            subAction = 1;
          } else if (rand > 40 && rand <= 85) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else if (rank < 3) {
          smartReason = "ultra-premium bucket (quads/straight flush)";
          if (rand <= 15) {
            subAction = 1;
          } else if (rand > 15 && rand <= 40) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else {
          Card[] temp = new Card[2 + board.length];
          temp[0] = super.getHand()[0];
          temp[1] = super.getHand()[1];
          for (int i = 0; i < board.length; i++)
            temp[i + 2] = board[i];
          boolean[] draw = draw(temp);
          if (rank == 6 || rank == 7 || ((draw[0] || draw[1]) && board.length < 5)) {
            smartReason = "medium-strength/draw bucket";
            if (bet < super.getChips() / 2) {
              if (rand <= 50) {
                subAction = 0;
              } else if (rand > 50 && rand <= 80) {
                subAction = 1;
              } else if (rand > 80 && rand <= 85) {
                subAction = 2;
              } else if (rand > 85 && rand <= 90) {
                subAction = 3;
              } else {
                subAction = 4;
              }
            } else {
              if (draw[0] || draw[1]) {
                 smartReason = "draw under pressure: coin-flip continue/fold";
                 if (Math.random() < 0.50) subAction = 0; else subAction = 4; // Gamble on draws 50%
              } else {
                  smartReason = "medium made hand under pressure with fold-rate balancing";
                  double foldRate = (headsUpHand) ? 0.25 : 0.50; // Reduce panic fold 1v1
                  if (rand <= 30) {
                    subAction = 0;
                  } else if (rand > 30 && rand <= 45) {
                    subAction = 1;
                  } else if (rand > 45 && rand <= 50) {
                    subAction = 2;
                  } else if (Math.random() < (1.0 - foldRate)) {
                    subAction = 0; // Call instead of fold
                  } else {
                    subAction = 4;
                  }
              }
            }
          } else {
            smartReason = "weak/no-draw bucket";
            if (bet < super.getChips() / 3) {
              if (rand <= 65) {
                subAction = 0;
              } else if (rand > 65 && rand <= 75) {
                subAction = 1;
              } else if (rand > 75 && rand <= 80) {
                subAction = 2;
              } else {
                subAction = 4;
              }
            } else {
              if (rand <= 25) {
                subAction = 0;
              } else if (rand > 25 && rand <= 30) {
                subAction = 1;
              } else {
                subAction = 4;
              }
            }
          }
        }
        boolean zeroBet = false;
        if (bet == 0) {
          bet = blind;
          zeroBet = true;
        }

        trace("SMART-POSTFLOP",
            "SUBACTION PICKED | subAction=" + subAction + ", zeroBet=" + zeroBet + ", reason=" + smartReason);
        
        // Override removed to bring bust rate up to 20-30%
        
        int raiseTo;
        switch (subAction) {
          case 0:
            action[0] = 1;
            if (!zeroBet) {
              action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
            } else
              action[1] = (zeroBet) ? 0 : bet - prevBet;
            break;
          case 1:
            action[0] = 3;
            raiseTo = (int) ((Math.random() * .6 + 2) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
            break;
          case 2:
            action[0] = 3;
            raiseTo = (int) ((Math.random() * 1.6 + 2.5) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
            break;
          case 3:
            double upper = (double) super.getChips() / bet;
            action[0] = 3;
            raiseTo = (int) ((Math.random() * (upper - 2 + 0.1) + 4) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
            break;
          case 4:
            if (!zeroBet)
              action[0] = 2;
            else
              action[0] = 1;
            break;
        }
        trace("SMART-POSTFLOP",
            "MAP SUBACTION | action=" + actionLabel(action[0]) + ", amount=" + action[1]);
      }
      if (action[1] >= super.getChips()) {
        action[0] = 4;
        action[1] = super.getChips();
      }
      trace("SMART-" + round.toUpperCase(),
          "FINAL | action=" + actionLabel(action[0]) + ", amount=" + action[1] + ", chips=" + super.getChips());
      return action;
    }
  }

  private int[] godPreflop(int prevBet, int bet, int blind, int lastRaise, PokerPlayer[] players, int seatIndex, int sbIdx, int bbIdx) {
    int numPlayers = players.length;
    int relativePos = (seatIndex - sbIdx + numPlayers) % numPlayers;
    
    int[] action = new int[2];
    String decisionReason = "unresolved";
    int[] numhand = Deck.cardToInt(super.getHand());
    Arrays.sort(numhand);
    boolean paired = numhand[0] == numhand[1];
    boolean suited = super.getHand()[0].getValue().charAt(1) == super.getHand()[1].getValue().charAt(1);
    boolean premium = (paired && numhand[0] >= 10) || (numhand[0] >= 13 && numhand[1] >= 13) || (numhand[1] == 14 && numhand[0] >= 11);
    
    boolean earlyPos = (relativePos >= 2 && relativePos <= 3);
    boolean latePos = (relativePos >= numPlayers - 2);
    boolean inBlinds = (seatIndex == sbIdx || seatIndex == bbIdx);
    boolean unraised = (bet == blind || bet == 0);
    
    boolean chipleader = true;
    int tablePlayers = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
          tablePlayers++;
          if (p != this && p.getChips() > super.getChips()) chipleader = false;
      }
    }
    boolean headsUpTable = (tablePlayers == 2);
    boolean shortStacks = true;
    int dumbBotCount = 0;
    int smartBotCount = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
        if (p.inHand() && p != this && p.getChips() > blind * 20) shortStacks = false;
        
        if (p instanceof PokerBot && p != this) {
            int level = ((PokerBot)p).botLevel;
            if (level == 0) dumbBotCount++;
            else if (level == 1) smartBotCount++;
        }
      }
    }

    boolean neuralSandbox = neuralSandboxEnabled();
    boolean pureProtectedGtoMode = protectedMode && !neuralSandbox;
    // Protected-only baseline intentionally masks bot-tier tracking for pure GTO behavior.
    int scriptedDumbBotCount = pureProtectedGtoMode ? 0 : dumbBotCount;
    int scriptedSmartBotCount = pureProtectedGtoMode ? 0 : smartBotCount;

    String smartHuOpponent = null;
    int smartHuStack = 0;
    if (headsUpTable && scriptedSmartBotCount == 1 && scriptedDumbBotCount == 0) {
      for (PokerPlayer p : players) {
        if (p == null || p == this || !p.inHand())
          continue;
        if (p instanceof PokerBot && ((PokerBot) p).botLevel == 1) {
          smartHuOpponent = p.getName();
          smartHuStack = Math.max(1, p.getChips());
          break;
        }
      }
    }
    SmartLeakProfile smartHuLeak = (smartHuOpponent != null) ? getOrCreateSmartLeakProfile(smartHuOpponent) : null;
    boolean smartHuExploitMode = (smartHuLeak != null && !protectedMode);
    double smartHuDepthRatio = smartHuExploitMode ? ((double) super.getChips() / smartHuStack) : 1.0;
    
    // Split-Brain Trigger: Only be "Spicy" if only Humans or other God Bots are in the pot
    boolean isThinkingOpponentOnly = (!pureProtectedGtoMode && scriptedDumbBotCount == 0 && scriptedSmartBotCount == 0);
    
    // PHASE 4: Universal Split-Personality Parameters (Active across all table sizes if in Nightmare Mode)
    boolean isNightmareMode = false;
    for (PokerPlayer pr : players) if (pr != null && "edjiang1234".equalsIgnoreCase(pr.getName())) isNightmareMode = true;
    boolean isGB = (isNightmareMode && predatoryIntent && !protectedMode);
    boolean isGS = (isNightmareMode && !predatoryIntent && !protectedMode);
    
    // PHASE 5/8: Human Aggression Profiling (Cognitive Database)
    double maxHumanAggression = 0.0;
    for (PokerPlayer pr : players) {
      if (pr != null && !(pr instanceof PokerBot) && pr.inHand()) {
          CognitiveProfile np = getCognitiveDB().get(pr.getName());
          if (np != null && np.handsPlayed >= 1 && np.getAggressionFactor() > maxHumanAggression) {
              maxHumanAggression = np.getAggressionFactor();
          }
      }
    }

    // CHUNK 5: Archetype-targeted preflop execution matrix.
    // We isolate one opponent profile (heads-up target or largest active stack) and exploit accordingly.
    CognitiveArchetype focusArchetype = CognitiveArchetype.UNKNOWN;
    int focusStack = -1;
    boolean foundHumanFocus = false;
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand())
        continue;
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp == null || cp.handsPlayed < 3)
        continue;

      if (!(pr instanceof PokerBot)) {
        if (!foundHumanFocus || headsUpTable || pr.getChips() > focusStack) {
          focusStack = pr.getChips();
          focusArchetype = cp.getArchetype();
          foundHumanFocus = true;
        }
        continue;
      }

      if (pureProtectedGtoMode)
        continue;

      if (foundHumanFocus)
        continue;

      if (headsUpTable || pr.getChips() > focusStack) {
        focusStack = pr.getChips();
        focusArchetype = cp.getArchetype();
      }
    }
    boolean exploitNit = (focusArchetype == CognitiveArchetype.NIT);
    boolean exploitManiac = (focusArchetype == CognitiveArchetype.MANIAC);
    boolean exploitStation = (focusArchetype == CognitiveArchetype.STATION);
    boolean gtoFallback = (focusArchetype == CognitiveArchetype.ELITE_REG);

    trace("GOD-PREFLOP",
      "START | hole=" + cardsToString(super.getHand()) + ", prevBet=" + prevBet + ", tableBet=" + bet
        + ", blind=" + blind + ", lastRaise=" + lastRaise + ", seatIndex=" + seatIndex + ", relativePos="
        + relativePos + ", paired=" + paired + ", suited=" + suited + ", premium=" + premium
        + ", headsUpTable=" + headsUpTable + ", smartHU=" + smartHuExploitMode + ", focusArchetype="
        + focusArchetype + ", maxHumanAgg=" + maxHumanAggression + ", isGB=" + isGB + ", isGS=" + isGS
        + ", neuralSandbox=" + neuralSandbox);
    
    boolean stealRange = paired || numhand[1] >= 14 || (suited && numhand[1] - numhand[0] <= 4);
    boolean smartPressureRange = smartHuExploitMode && (paired || numhand[1] >= 12 || (suited && numhand[1] - numhand[0] <= 5));
    
    // GTO Hardening: Balanced Early Range (Matching Smart Bot + Suited Wheel Aces) - SPICY MODE ONLY
    boolean wheelAce = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && (suited && numhand[1] == 14 && numhand[0] >= 2 && numhand[0] <= 9) && !isGS;
    boolean faceCards = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && ((numhand[1] >= 13 && numhand[0] >= 10) || (numhand[1] == 12 && numhand[0] >= 11));
    boolean earlyRange = premium || paired || faceCards || wheelAce;
    
    // Mixed Strategy Anomaly (15%): Playing GTO Gappers/Trash from any position - SPICY MODE ONLY
    double mixedFreq = isGB ? 0.35 : (isGS ? 0.05 : 0.15);
    if (smartHuExploitMode) {
      mixedFreq = Math.max(0.0, mixedFreq - 0.10);
    }
    if (gtoFallback) mixedFreq = 0.0; // Pure baseline vs volatile/elite profiles.
    else if (exploitNit) mixedFreq = Math.min(0.50, mixedFreq + 0.10); // Attack over-folding ranges.
    else if (exploitStation) mixedFreq = Math.max(0.0, mixedFreq - 0.10); // Reduce low-EV air vs sticky callers.
    boolean mixedStrategy = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && (Math.random() < mixedFreq && (suited && numhand[1] - numhand[0] <= 5));
    
    // Heads-Up Tournament Protocol: Any Ace, King, Queen or Pair becomes Premium
    if (headsUpTable) {
        if (numhand[1] >= 12 || paired) premium = true;
    }

    // Fortress Defense: Active Defensive Ranges
    boolean defenseRange = earlyRange || stealRange;
    if (smartHuExploitMode) {
      defenseRange = defenseRange || smartPressureRange;
    }
    if (exploitNit && (latePos || headsUpTable)) {
      defenseRange = defenseRange || numhand[1] >= 12 || (suited && numhand[1] - numhand[0] <= 6);
    }
    if (exploitManiac) {
      defenseRange = defenseRange || paired || faceCards || (suited && numhand[1] - numhand[0] <= 3);
    }
    if (gtoFallback) {
      defenseRange = earlyRange || paired || (suited && numhand[1] - numhand[0] <= 2);
    }
    
    if (premium || (chipleader && shortStacks && stealRange) || mixedStrategy) {
      decisionReason = "open/3bet value lane: premium or pressure steal or mixed strategy";
      action[0] = 3;
      int intended = (bet > blind) ? (bet * 3) : (blind * 3);
      int raiseTo = Math.min(Math.max(intended, bet + lastRaise), super.getChips() + prevBet);
      action[1] = raiseTo - prevBet;
    } else if (bet > blind && (isThinkingOpponentOnly || maxHumanAggression > 0.0 || smartHuExploitMode) && (!protectedMode || neuralSandbox)) {
      // SPICY MODE: Active Defense Logic
      double threeBetChance = isGB ? 0.50 : (isGS ? 0.20 : 0.35); // 35% chance to re-raise bluffs

      if (smartHuExploitMode) {
        // Drive Smart-HU extraction with bounded adaptive pressure.
        threeBetChance = 0.38;
        if (smartHuLeak.foldTo3BetHUEMA >= 0.56) {
          threeBetChance += 0.18;
        } else if (smartHuLeak.foldTo3BetHUEMA <= 0.40) {
          threeBetChance -= 0.12;
        }

        if (smartHuDepthRatio < 0.75) {
          threeBetChance += 0.12; // Deficit recovery: reclaim initiative.
        } else if (smartHuDepthRatio > 1.25) {
          threeBetChance -= 0.10; // Lockdown: avoid giving back EV.
        }
      }

      if (gtoFallback) {
        threeBetChance = Math.min(threeBetChance, 0.30);
      }

      threeBetChance = Math.max(0.08, Math.min(0.90, threeBetChance));

      // CHUNK 5: vs Nits, aggressively pressure opens with 100% 3-bet frequency in defended range.
      if (exploitNit && defenseRange) {
        threeBetChance = 1.0;
      }

      // CHUNK 5: vs Maniacs, lower 3-bet frequency and bias to flats to induce postflop punts.
      if (exploitManiac) {
        threeBetChance = Math.max(0.10, threeBetChance - 0.25);
      }
      
      // PHASE 5: Nemesis VPIP/PFR Exploitation (Tightened - Stop punting garbage)
      if (maxHumanAggression > 0.40 && !gtoFallback) {
          // Instead of 100% defenseRange, only expand to reasonably playable hands (any Ace, any broadway, any pair, connected suites)
          defenseRange = earlyRange || stealRange || (numhand[1] >= 14) || faceCards || paired || (suited && numhand[1] - numhand[0] <= 3);
          threeBetChance = Math.min(0.80, threeBetChance + 0.20); // Cap 3-bet frequency so it's not literally punting every hand
      }

      if (smartHuExploitMode && bet >= blind * 8 && !premium
          && !(paired && numhand[0] >= 9)
          && !(numhand[1] >= 13 && numhand[0] >= 11)) {
        // Against Smart 4-bet/5-bet pressure in HU, avoid spewing marginal opens.
        decisionReason = "smart-HU anti-spew fold vs oversized 4/5-bet pressure";
        action[0] = 2; action[1] = 0;
      } else if (gtoFallback && !premium) {
        // Pure baseline fallback against elite/volatile opponents.
        decisionReason = "elite-reg fallback: tighten to baseline defense";
        if (defenseRange && bet <= blind * 3.5) {
          action[0] = 1; action[1] = bet - prevBet;
        } else {
          action[0] = 2; action[1] = 0;
        }
      } else if (exploitNit && defenseRange) {
        decisionReason = "nit exploit: high-frequency preflop pressure";
        action[0] = 3;
        int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
        action[1] = raiseTo - prevBet;
      } else if (exploitManiac && defenseRange && !premium) {
        decisionReason = "maniac exploit: induce punts with flatter defend range";
        if (bet <= blind * 6.0 || (paired && numhand[0] >= 7) || faceCards) {
          action[0] = 1; action[1] = bet - prevBet;
        } else {
          action[0] = 2; action[1] = 0;
        }
      } else if (defenseRange && Math.random() < threeBetChance) {
          decisionReason = "defense range RNG passed threeBetChance=" + threeBetChance;
          action[0] = 3; 
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
          action[1] = raiseTo - prevBet;
      } else if (seatIndex == bbIdx && bet <= blind * 4.5 && defenseRange) {
          decisionReason = "big-blind defense flat";
          action[0] = 1; action[1] = bet - prevBet; // Big Blind Defense (Sticky Call)
      } else if (latePos && bet <= blind * 3.5 && (paired || wheelAce)) {
          decisionReason = "late-position flat with paired/wheel-ace defense";
          action[0] = 1; action[1] = bet - prevBet; // Positional Flatting (Call in position)
      } else if (maxHumanAggression > 0.40 && ((paired && numhand[0] >= 6) || (numhand[1] >= 13 && numhand[0] >= 10))) {
          // PHASE 5: Snap-call manic 5-bet preflop jams with playable stuff (mid-pairs or strong broadways), not arbitrary J-highs
          decisionReason = "high-aggression opponent exploit: snap-call playable blocker hands";
          action[0] = 1; action[1] = bet - prevBet;
      } else if (headsUpTable) {
          decisionReason = "heads-up pressure fallback 3-bet";
          action[0] = 3; 
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
          action[1] = raiseTo - prevBet;
      } else {
          decisionReason = "trash fold after defense tree exhausted";
          action[0] = 2; action[1] = 0; // Still fold trash
      }
    } else if (unraised && latePos && stealRange) {
      decisionReason = "late-position open steal";
      action[0] = 3;
      action[1] = Math.min(blind * 3, super.getChips());
    } else if (unraised && inBlinds) {
      if (bet <= super.getChips() / 4) {
        decisionReason = "blind defense call/check in unraised pot";
        action[0] = 1;
        action[1] = bet > 0 ? bet - prevBet : 0;
      } else {
         decisionReason = "blind defense fold to oversized open";
         action[0] = 2; action[1] = 0;
      }
    } else if (earlyPos && earlyRange) {
      decisionReason = "early-position value open";
      action[0] = 3; action[1] = Math.min(blind * 3, super.getChips());
    } else {
      if (headsUpTable) {
          decisionReason = "heads-up fallback aggression";
          action[0] = 3; action[1] = Math.max(bet * 3, blind * 3);
      } else {
          decisionReason = "multiway fallback fold/check";
          action[0] = 2; action[1] = 0;
          if (bet == 0) { action[0] = 1; action[1] = 0; }
      }
    }
    
    if (action[0] == 3 && !protectedMode) {
      int noise = (int)(Math.random() * 11) - 5; // Humanoid Noise: +/- 5 chips
      int raiseTo = (prevBet + action[1]) + noise;
      // FINAL LEGAL GUARD: Ensure the noisy bet still respects the increment floor
      int legalMin = bet + lastRaise;
      if (raiseTo < legalMin) raiseTo = Math.min(super.getChips() + prevBet, legalMin + (int)(Math.random() * 5));
      action[1] = raiseTo - prevBet;
      decisionReason += " | sizing-noise-adjusted";
    }
    
    if (action[1] >= super.getChips()) {
      action[0] = 4;
      action[1] = super.getChips();
      decisionReason += " | clipped-to-all-in";
    }
    trace("GOD-PREFLOP",
        "FINAL | reason=" + decisionReason + ", action=" + actionLabel(action[0]) + ", amount=" + action[1]
            + ", defenseRange=" + defenseRange + ", mixedStrategy=" + mixedStrategy + ", smartHU="
            + smartHuExploitMode + ", depthRatio=" + smartHuDepthRatio);
    BotDiagnostics.recordGodDecision(
      "PREFLOP",
      super.getName(),
      decisionReason,
      actionLabel(action[0]),
      action[1],
      "tableBet=" + bet + ", prevBet=" + prevBet + ", blind=" + blind + ", seatIndex=" + seatIndex
        + ", defenseRange=" + defenseRange + ", mixedStrategy=" + mixedStrategy + ", smartHU="
        + smartHuExploitMode + ", depthRatio=" + smartHuDepthRatio + ", focusArchetype="
        + focusArchetype + ", hole=" + cardsToString(super.getHand()));
    cbetFlop = false; // Reset barrelling state for new hand
    return action;
  }

  private int[] godPostflop(int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize, PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int[] action = new int[2];
    String decisionReason = "unresolved";
    Card[] fullHand = new Card[5];
    if (board.length == 3) {
      fullHand[0] = super.getHand()[0];
      fullHand[1] = super.getHand()[1];
      for (int i = 0; i < 3; i++) fullHand[i + 2] = board[i];
    } else {
      fullHand = p.getBestHand(super.getHand(), board);
    }
    
    int myRank = p.getRanking(fullHand);
    
    Card[] bestBoard = null;
    int boardRank = 9;
    if (board.length >= 5) {
      bestBoard = p.getBestHand(new Card[0], board);
      boardRank = p.getRanking(bestBoard);
    }
    
    Card[] total = new Card[2 + board.length];
    total[0] = super.getHand()[0];
    total[1] = super.getHand()[1];
    for (int i = 0; i < board.length; i++) total[i + 2] = board[i];
    boolean[] draws = draw(total);
    int outs = 0;
    if (draws[0]) outs += 8;
    if (draws[1]) outs += 9;
    double equity = (board.length == 3) ? (outs * 4) / 100.0 : (board.length == 4 ? (outs * 2) / 100.0 : 0);
    
    boolean zeroBet = (bet == 0);
    double costToCall = zeroBet ? 0 : bet - prevBet;
    double potOdds = costToCall / Math.max(1, (double)(potSize + costToCall));
    
    int act = 1; 
    int actAmount = zeroBet ? 0 : bet - prevBet;
    
    boolean cbet = (board.length == 3 && preflopAggressorIndex == seatIndex);
    
    boolean flushScare = false;
    String scareSuit = "";
    int[] flushC = new int[4];
    for (Card d : board) {
       switch(d.getValue().substring(1)) {
          case "♠️": flushC[0]++; if(flushC[0]>=3) scareSuit="♠️"; break;
          case "♣️": flushC[1]++; if(flushC[1]>=3) scareSuit="♣️"; break;
          case "♦️": flushC[2]++; if(flushC[2]>=3) scareSuit="♦️"; break;
          case "♥️": flushC[3]++; if(flushC[3]>=3) scareSuit="♥️"; break;
       }
    }
    for (int i : flushC) if (i >= 3) flushScare = true;
    
    // Elite Awareness: Straight Scares and Paired Boards
    boolean straightScare = false;
    if (board.length >= 4) {
      Card[] bSorted = board.clone();
      Deck.sort(bSorted);
      int con = 0;
      for (int i = 1; i < bSorted.length; i++) {
        if (bSorted[i].getNum() == bSorted[i-1].getNum() + 1) con++;
        else if (bSorted[i].getNum() != bSorted[i-1].getNum()) con = 0;
        if (con >= 3) straightScare = true;
      }
    }
    
    
    boolean aceHighBoard = false;
    for (Card d : board) if (d.getNum() == 14) aceHighBoard = true;
    
    boolean nutBlocker = false;
    if (flushScare && myRank > 4) { 
       if ((super.getHand()[0].getValue().charAt(1) == scareSuit.charAt(0) && super.getHand()[0].getNum() == 14) ||
           (super.getHand()[1].getValue().charAt(1) == scareSuit.charAt(0) && super.getHand()[1].getNum() == 14)) {
           nutBlocker = true;
       }
    }
    
    int smartBotCount = 0;
    int dumbBotCount = 0;
    int godBotCount = 0;
    int largestSmartStack = 0;
    int largestDumbStack = 0;
    int largestOpponentStack = 0;
    int activeCount = 0;
    double maxHumanAggression = 0.0;
    for (PokerPlayer pr : players) {
      if (pr.inHand()) activeCount++;
      if (pr.inHand() && pr != this) {
         if (pr.getChips() > largestOpponentStack) largestOpponentStack = pr.getChips();
         if (pr instanceof PokerBot) {
                int level = ((PokerBot)pr).botLevel;
                if (level == 1) { smartBotCount++; if (pr.getChips() > largestSmartStack) largestSmartStack = pr.getChips(); }
                else if (level == 0) { dumbBotCount++; if (pr.getChips() > largestDumbStack) largestDumbStack = pr.getChips(); }
                else if (level == 2) godBotCount++;
         } else {
            // PHASE 5/8: Postflop Aggression Checking
            CognitiveProfile np = getCognitiveDB().get(pr.getName());
            if (np != null && np.handsPlayed >= 1 && np.getAggressionFactor() > maxHumanAggression) {
                maxHumanAggression = np.getAggressionFactor();
            }
         }
      }
    }
    boolean neuralSandbox = neuralSandboxEnabled();
    boolean pureProtectedGtoMode = protectedMode && !neuralSandbox;
    // Protected-only baseline intentionally masks bot-tier tracking for pure GTO behavior.
    int scriptedDumbBotCount = pureProtectedGtoMode ? 0 : dumbBotCount;
    int scriptedSmartBotCount = pureProtectedGtoMode ? 0 : smartBotCount;

    boolean headsUpHand = (activeCount == 2);
    double depthRatio = (largestOpponentStack > 0) ? (double) super.getChips() / largestOpponentStack : 1.0;

    String smartHuOpponent = null;
    if (headsUpHand && scriptedSmartBotCount == 1 && scriptedDumbBotCount == 0) {
      for (PokerPlayer pr : players) {
        if (pr == null || pr == this || !pr.inHand())
          continue;
        if (pr instanceof PokerBot && ((PokerBot) pr).botLevel == 1) {
          smartHuOpponent = pr.getName();
          break;
        }
      }
    }
    SmartLeakProfile smartHuLeak = (smartHuOpponent != null) ? getOrCreateSmartLeakProfile(smartHuOpponent) : null;
    boolean smartHuExploitMode = (smartHuLeak != null && !protectedMode);
    boolean smartDeficitRecovery = smartHuExploitMode && depthRatio < 0.72;
    boolean smartLockdown = smartHuExploitMode && depthRatio > 1.20;
    boolean smartRecaptureMode = (!protectedMode && scriptedSmartBotCount > 0 && depthRatio < 0.85);

    // CHUNK 6: Postflop archetype-focused execution matrix target.
    CognitiveArchetype focusArchetype = CognitiveArchetype.UNKNOWN;
    int focusStack = -1;
    boolean foundHumanFocus = false;
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand())
        continue;
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp == null || cp.handsPlayed < 3)
        continue;

      if (!(pr instanceof PokerBot)) {
        if (!foundHumanFocus || headsUpHand || pr.getChips() > focusStack) {
          focusStack = pr.getChips();
          focusArchetype = cp.getArchetype();
          foundHumanFocus = true;
        }
        continue;
      }

      if (pureProtectedGtoMode)
        continue;

      if (foundHumanFocus)
        continue;

      if (headsUpHand || pr.getChips() > focusStack) {
        focusStack = pr.getChips();
        focusArchetype = cp.getArchetype();
      }
    }
    boolean exploitNit = (focusArchetype == CognitiveArchetype.NIT);
    boolean exploitStation = (focusArchetype == CognitiveArchetype.STATION);
    boolean exploitManiac = (focusArchetype == CognitiveArchetype.MANIAC);
    boolean gtoFallback = (focusArchetype == CognitiveArchetype.ELITE_REG);

    boolean isNightmareMode = false;
    for (PokerPlayer pr : players) if (pr != null && "edjiang1234".equalsIgnoreCase(pr.getName())) isNightmareMode = true;
    boolean isGB = (isNightmareMode && predatoryIntent && !protectedMode);
    boolean isGS = (isNightmareMode && !predatoryIntent && !protectedMode);

    boolean isThinkingOpponentOnly = (!pureProtectedGtoMode && scriptedDumbBotCount == 0 && scriptedSmartBotCount == 0);
    boolean isBeingBullied = (isThinkingOpponentOnly && depthRatio < 0.5 && (!protectedMode || neuralSandbox));
    if (smartDeficitRecovery) {
      isBeingBullied = true;
    }
    boolean fishPredatoryMode = (headsUpHand && scriptedDumbBotCount > 0 && !protectedMode);
    boolean baselineNightmarePredatoryMode = (headsUpHand && isNightmareMode && predatoryIntent && !protectedMode);
    // Intensity 2+ unlocks adaptive pressure in God-only small fields (2-4 active players).
    boolean adaptiveNightmarePredatoryMode = (nightmareIntensity >= 2 && activeCount >= 2 && activeCount <= 4
      && isThinkingOpponentOnly && isNightmareMode && predatoryIntent && !protectedMode);
    boolean predatoryMode = fishPredatoryMode || baselineNightmarePredatoryMode || adaptiveNightmarePredatoryMode;
    boolean exploitingSmartBot = (scriptedSmartBotCount > 0 && scriptedDumbBotCount == 0 && !protectedMode); 
    boolean minusOneActive = false;
    double bluffSizeVsSmart = (largestSmartStack > 0) ? (largestSmartStack * 0.5) + 1 : potSize;

    trace("GOD-POSTFLOP",
      "START | hole=" + cardsToString(super.getHand()) + ", board=" + cardsToString(board) + ", best5="
        + cardsToString(fullHand) + ", myRank=" + myRank + ", boardRank=" + boardRank + ", draws=["
        + draws[0] + "," + draws[1] + "], equity=" + equity + ", potOdds=" + potOdds + ", prevBet="
        + prevBet + ", tableBet=" + bet + ", pot=" + potSize + ", smartHU=" + smartHuExploitMode
        + ", depthRatio=" + depthRatio + ", focusArchetype=" + focusArchetype + ", predatoryMode="
          + predatoryMode + ", cbetEligible=" + cbet + ", neuralSandbox=" + neuralSandbox);
    if (smartHuExploitMode) {
      double pressureBias = (smartHuLeak.foldToFlopCbetHUEMA - 0.5) * 0.25;
      bluffSizeVsSmart *= (1.0 + pressureBias);
      if (smartDeficitRecovery) {
        bluffSizeVsSmart *= 1.10;
      } else if (smartLockdown) {
        bluffSizeVsSmart *= 0.90;
      }
      bluffSizeVsSmart = Math.max(blind * 2.0, Math.min(super.getChips(), bluffSizeVsSmart));
    }

     // Heuristic Sizing Scanner (Soul Reading)
     boolean soulReadSmartOverbet = false;
     boolean soulReadSmartMixed = false;
     double smartSizingRatio = 0.0;
    if (!zeroBet && scriptedSmartBotCount > 0 && !protectedMode) {
       double baseBet = (prevBet > 0) ? prevBet : blind;
       smartSizingRatio = (double) bet / Math.max(1.0, baseBet);
       soulReadSmartOverbet = smartSizingRatio >= 2.65;
       soulReadSmartMixed = smartSizingRatio > 1.0 && smartSizingRatio < 2.65;
     }

     if (soulReadSmartOverbet && myRank > 7) {
       decisionReason = "soul-read overbet from smart bot implies value-heavy range => fold bluff-catchers";
       act = 2; // Preserve strong made hands (straight+) while folding medium bluff-catchers.
     } else if (soulReadSmartMixed && headsUpHand && myRank >= 6 && myRank <= 8 && !zeroBet) {
       // SubAction-1 sizing is mixed value/bluff; in HU this lane should be call-first, not auto-raise.
       boolean scaryPairSpot = (myRank >= 8) && (flushScare || straightScare);
       double callCap = smartDeficitRecovery ? 0.95 : (smartLockdown ? 0.55 : 0.75);
       if (!scaryPairSpot && costToCall <= potSize * callCap) {
        decisionReason = "soul-read mixed sizing in HU => controlled bluff-catch call";
        act = 1;
        actAmount = bet - prevBet;
       } else {
        decisionReason = "soul-read mixed sizing in HU but risk too high => disciplined fold";
        act = 2;
       }
     } else if (nutBlocker && isThinkingOpponentOnly) {
       decisionReason = "nut-blocker bluff lane on scare texture";
       act = 3; actAmount = exploitingSmartBot ? (int)bluffSizeVsSmart : Math.max(potSize, super.getChips());
    } else if (smartHuExploitMode && board.length == 4 && zeroBet && smartHuLeak.checkBackTurnHUEMA >= 0.55 && myRank <= 8) {
      // Smart check-back tendency on turn gets punished by proactive probing.
      decisionReason = "smart-HU turn probe punish after high check-back tendency";
      act = 3;
      actAmount = (int) Math.max(blind, potSize * (smartDeficitRecovery ? 0.70 : 0.45));
    } else if (board.length >= 5 && myRank >= boardRank && p.compareHands(fullHand, bestBoard) == 0) {
      decisionReason = "board-lock scenario: protect against chop disasters";
      // Defensive Awareness: If board is terrifying and we just have a pair, fold to big pressure
      // SPICY OVERRIDE: If being bullied, stay suspicious and don't fold Top Pairs or better
      if (!zeroBet && (flushScare || straightScare) && costToCall > potSize * 0.5 && myRank > 5 && !isBeingBullied) {
         if (maxHumanAggression > 0.40 && (myRank <= 7 || isGB)) {
             // PHASE 5: Maniac exploit - Snap call their scare card bluff with Top/Middle pair
           decisionReason = "board-lock + maniac exploit => bluff-catch call";
             act = 1; actAmount = bet - prevBet;
         } else {
           decisionReason = "board-lock + heavy pressure => fold";
             act = 2;
         }
      } else {
        if (zeroBet) {
          decisionReason = "board-lock with no bet => check";
          act = 1;
        } else {
            if (isBeingBullied && myRank <= 8) { act = 1; actAmount = bet - prevBet; }
          else act = 2;
          if (isBeingBullied && myRank <= 8) decisionReason = "board-lock + bullied => hero-call";
          else decisionReason = "board-lock fallback fold";
         }
      }
    } else if (myRank <= 3 || (isBeingBullied && myRank <= 8) || (maxHumanAggression > 0.40 && myRank <= 6)) { 
      decisionReason = "value/trap engine for strong made hands";
      // Trapping Logic (Slow-play): 20% chance to check/call monster hands to bait bluffs - SPICY MODE ONLY
      double trapFreq = isGB ? 0.10 : (isGS ? 0.40 : 0.20);
      if (exploitManiac) trapFreq = Math.min(0.70, trapFreq + 0.25);
      if (exploitStation) trapFreq = Math.max(0.05, trapFreq - 0.10);
      if (exploitNit) trapFreq = Math.max(0.02, trapFreq - 0.05);
      if (gtoFallback) trapFreq = 0.20;
      if (smartHuExploitMode) {
        trapFreq = smartDeficitRecovery ? 0.03 : Math.min(trapFreq, 0.08);
      }
      boolean trapMode = (!protectedMode && isThinkingOpponentOnly && Math.random() < trapFreq && (board.length == 3 || board.length == 4));
      
        if (scriptedDumbBotCount > 0 && largestDumbStack > 0 && !protectedMode) {
          decisionReason = "minus-one exploit active versus dumb bot";
          act = 3; actAmount = Math.max(potSize, largestDumbStack - 1); // MINUS ONE EXPLOIT
          minusOneActive = true;
      } else if (trapMode) {
          decisionReason = "trap mode selected (slow-play)";
          if (zeroBet) act = 1; else { act = 1; actAmount = bet - prevBet; }
      } else {
          // Iron Chin: Snap-calling bullies with Hero Ranges (Pairs or better)
          if (isBeingBullied && !zeroBet) {
           decisionReason = "bullied defense call";
             act = 1; actAmount = bet - prevBet;
          } else {
             double valueMult = (depthRatio > 1.5 && !protectedMode) ? 1.0 : 0.75; // Big Stack Bully widening (disable if protected)
             if (exploitStation) valueMult += 0.15;
             else if (exploitNit) valueMult -= 0.10;
             else if (exploitManiac) valueMult += 0.05;
             if (smartHuExploitMode) {
               if (smartDeficitRecovery) valueMult += 0.15;
               if (smartLockdown) valueMult -= 0.10;
             }
             if (gtoFallback) valueMult = Math.min(valueMult, 0.85);
             valueMult = Math.max(0.40, Math.min(1.20, valueMult));
             if (zeroBet) {
               decisionReason = "strong hand value lead with dynamic multiplier=" + valueMult;
               act = 3; actAmount = (int)(potSize * valueMult);
             } else {
               decisionReason = "strong hand value raise versus bet";
               act = 3; actAmount = bet * 3;
             }
          }
      }
    } else if (myRank <= (headsUpHand ? 8 : 7) && scriptedDumbBotCount > 0 && !draws[0] && !draws[1]) {
      decisionReason = "thin value versus dumb bot range";
      // Hyper-Thin Value Betting vs Dumb Bots (Isolated 1v1 expands threshold to Any Pair)
      if (zeroBet) { act = 3; actAmount = potSize; }
      else { act = 3; actAmount = Math.max(potSize, bet * 2); }
    } else if (myRank <= 5) { 
      decisionReason = "standard value extraction lane";
      // PHASE 7: Multiway Value Extraction - Charge a premium against multiple players to protect strong hands
      double valueSize = (activeCount >= 3) ? 0.85 : 0.5;
      if (exploitStation) valueSize *= 1.20;
      else if (exploitNit) valueSize *= 0.90;
      else if (exploitManiac) valueSize *= 1.10;
      if (smartHuExploitMode) {
        if (smartDeficitRecovery) valueSize += 0.20;
        if (smartLockdown) valueSize += 0.05;
      }
      if (gtoFallback) valueSize = (activeCount >= 3) ? 0.80 : 0.50;
      valueSize = Math.max(0.35, Math.min(1.30, valueSize));
      if (zeroBet) { act = 3; actAmount = (int)(potSize * valueSize); }
      else { 
        if (costToCall > super.getChips() * 0.5) { act = 1; actAmount = bet - prevBet; } 
        else { act = 3; actAmount = bet * 3; }
        if (costToCall > super.getChips() * 0.5) decisionReason = "value hand but high call cost => control via call";
        else decisionReason = "value hand raise for protection";
      }
    } else if (myRank <= 8 || draws[0] || draws[1]) { 
      decisionReason = "draw/marginal hand semi-bluff and c-bet engine";
      // GTO Hardening: Semi-Bluffing (40% chance to lead draws aggressively) - SPICY MODE ONLY
      boolean semiBluff = (isThinkingOpponentOnly && (draws[0] || draws[1]) && Math.random() < 0.40);
      
      // Elite Range Advantage: Ace-high boards C-bet 90% of the time
      double cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
      double barrelFreq = 0.70;

        if (exploitNit) {
          cbetFreq = 1.0;
          barrelFreq = Math.max(barrelFreq, 0.90);
        } else if (exploitStation) {
          cbetFreq = 0.0;
          barrelFreq *= 0.25;
        } else if (exploitManiac) {
          cbetFreq *= 0.75;
          barrelFreq *= 0.60;
        } else if (gtoFallback) {
          cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
          barrelFreq = 0.70;
        }

      if (smartHuExploitMode) {
        cbetFreq += (smartHuLeak.foldToFlopCbetHUEMA - 0.50) * 0.40;
        cbetFreq -= Math.max(0.0, smartHuLeak.raiseVsCbetHUEMA - 0.35) * 0.25;
        barrelFreq += (smartHuLeak.foldToTurnBarrelHUEMA - 0.50) * 0.45;
        if (smartDeficitRecovery) {
          cbetFreq += 0.12;
          barrelFreq += 0.10;
        }
        if (smartLockdown) {
          cbetFreq -= 0.08;
          barrelFreq -= 0.10;
        }
      } else if (smartRecaptureMode && activeCount <= 3) {
        cbetFreq += 0.06;
      }
      
      // PHASE 7: Multiway Respect - Slash bluffing/barreling frequencies into multiple opponents
      if (activeCount >= 3) {
          cbetFreq *= 0.50;  // Cut bluffs in half vs 3+ players
          barrelFreq *= 0.40; // Double barrel is even rarer
      }
      
      // PHASE 4: G-B vs G-S Baseline Aggression Skew
      if (isGB) { cbetFreq += 0.15; barrelFreq += 0.15; }
      if (isGS) { cbetFreq -= 0.15; barrelFreq -= 0.15; }
      
      // PHASE 3: Smooth Dynamic Stack-Depth Aggression Scaling
      double depthShift = (depthRatio - 1.0) * 0.15;
      depthShift = Math.max(-0.4, Math.min(0.2, depthShift)); // Cap at sensible boundaries
      if (!predatoryMode && depthRatio < 0.6) {
          // Continuous survival scaling rather than simple binary drop to 0
          cbetFreq *= (depthRatio / 0.6);
          barrelFreq *= (depthRatio / 0.6);
      } else {
          cbetFreq = Math.max(0.0, Math.min(1.0, cbetFreq + depthShift));
          barrelFreq = Math.max(0.0, Math.min(1.0, barrelFreq + depthShift));
      }
      
        if (scriptedDumbBotCount > 0) {
          cbetFreq = (headsUpHand) ? 0.60 : 0.0; // PREDATORY BLUFFING 1v1 vs Dumb Bots
      }

        if (exploitNit && cbet) cbetFreq = 1.0;
        if (exploitStation && cbet) cbetFreq = 0.0;
        if (gtoFallback && cbet) cbetFreq = Math.max(0.45, Math.min(0.80, cbetFreq));

      // PHASE 7: Multiway C-Bet Sizing (Down-size C-bets in multiway pots to risk less capital per bluff)
      double cbetSize = (activeCount >= 3) ? 0.30 : 0.40;
        if (exploitNit) cbetSize *= 1.10;
        else if (exploitStation) cbetSize *= 0.75;
        else if (exploitManiac) cbetSize *= 0.85;
        if (gtoFallback) cbetSize = (activeCount >= 3) ? 0.30 : 0.40;
        if (smartHuExploitMode) {
          if (smartDeficitRecovery) cbetSize *= 1.25;
          else if (smartLockdown) cbetSize *= 0.90;
        }
        cbetSize = Math.max(0.20, Math.min(0.60, cbetSize));

      if (zeroBet && cbet && Math.random() < cbetFreq) {
        decisionReason = "c-bet fired with tuned frequency=" + cbetFreq;
         act = 3; actAmount = (int)(Math.max(potSize * cbetSize, blind));
         if (board.length == 3) cbetFlop = true;
      } else if (zeroBet && semiBluff) {
        decisionReason = "semi-bluff lead on draw";
         act = 3; actAmount = (int)(potSize * 0.75); // Lead aggressively on semi-bluff
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < barrelFreq) {
         // Triple Barreling: Turn is a face card and we C-bet flop
        decisionReason = "turn barrel after flop c-bet on favorable overcard";
         act = 3; actAmount = (int)(potSize * 0.6);
      } else if (!zeroBet && semiBluff) {
        decisionReason = "semi-bluff raise facing bet";
         act = 3; actAmount = bet * 3; // Aggressive Raise on semi-bluff
      } else if (!zeroBet && (draws[0] || draws[1]) && equity > potOdds) {
        decisionReason = "draw call justified by equity>potOdds";
         act = 1; actAmount = bet - prevBet;
      } else {
         // PHASE 6: Scare-Board Float & Bluff-Catch Logic (REVISED)
         // If you're aggressively bluffing scare cards, they will float/call to induce you
         // BUT ONLY if conditions are mathematically sound (hand equity, bet size, frequency cap)
         double floatFreq = 0.50; // Increased to 50% frequency cap
         double requiredEquity = (costToCall > 0) ? costToCall / (double)(potSize + costToCall) : 0.0;
         boolean isScareBoardBluff = (maxHumanAggression > 0.40 && !zeroBet && (flushScare || straightScare) 
             && myRank > 7 && myRank <= 9 && Math.random() < floatFreq && equity > requiredEquity * 0.9);
         
         double huFoldChance = (headsUpHand) ? 0.25 : 0.85; // Intelligent Stickiness 1v1
         if (exploitNit) huFoldChance = Math.min(1.0, huFoldChance + 0.15);
         if (exploitStation) huFoldChance = Math.min(1.0, huFoldChance + 0.05);
         if (exploitManiac) huFoldChance = Math.max(0.0, huFoldChance - 0.20);
         if (gtoFallback) huFoldChance = headsUpHand ? 0.30 : 0.82;
         if (smartHuExploitMode) {
           huFoldChance = smartDeficitRecovery ? 0.12 : 0.22;
           huFoldChance -= Math.max(0.0, smartHuLeak.raiseVsCbetHUEMA - 0.35) * 0.10;
           huFoldChance = Math.max(0.05, Math.min(0.80, huFoldChance));
         }
         if (isGB && headsUpHand) huFoldChance = 0.00; // G-B never folds 1v1 to generic pressure here
         if (isGS && headsUpHand) huFoldChance = 0.50; // G-S respects pressure more
         
         // PHASE 5: Leak Fix - Require mathematical backing before sticking to draws or dropping huFoldChance to 0
         if (maxHumanAggression > 0.40) {
             if (myRank <= 7) huFoldChance = 0.00; // Two pair+ never fold to mania
             else if (myRank <= 9 && board.length < 5) huFoldChance = 0.00; // Float pairs pre-river
             else if ((draws[0] || draws[1]) && board.length < 5 && equity > 0.15) huFoldChance = 0.00; // Float decent draws, but DO NOT call busted draws on River
         }
         
         // PHASE 6: Bluff-Catching on Scare Boards - Float instead of fold (Tightened)
         if (isScareBoardBluff) {
             // Dynamic Response: If bet is small (<50% pot), occasionally check-raise/re-raise to punish bluffs
             double betRatio = (double)(bet - prevBet) / Math.max(1, potSize);
             if (betRatio < 0.50 && Math.random() < 0.60) {
             decisionReason = "scare-board bluff catch converted to punish raise";
                 act = 3; actAmount = Math.max(bet * 3, potSize); // Re-raise the small bluff
             } else {
             decisionReason = "scare-board bluff catch via controlled call";
                 act = 1; actAmount = bet - prevBet; // Float/call against bigger bets to control the pot
             }
         } else {
             double riskRatio = (super.getChips() > 0) ? costToCall / (double)super.getChips() : 1.0;
             double callProb = (!zeroBet && riskRatio < 0.20) ? (0.20 - riskRatio) * 5.0 : 0.0; // Phase 3: Smooth 0-100% call scale for micro bets
             
             if (zeroBet) {
               decisionReason = "no-bet fallback check";
               act = 1;
             } else if (!zeroBet && costToCall > 0 && Math.random() < callProb) {
               decisionReason = "micro-bet exploit defense call";
               act = 1; actAmount = bet - prevBet;
             } else if (headsUpHand && Math.random() > huFoldChance) {
               decisionReason = "heads-up stickiness call";
               act = 1; actAmount = bet - prevBet;
             } else {
               decisionReason = "draw/marginal fallback fold";
               act = 2;
             }
         }
      }
    } else if (predatoryMode && myRank <= 10) {
       decisionReason = "predatory thin-value line (ace-high capable)";
       // Ultra-Thin Value: Betting Ace-High against Dumb Bots 1v1
       if (zeroBet) { act = 3; actAmount = (int)(potSize * 0.5); }
       else { act = 1; actAmount = bet - prevBet; }
    } else { 
      decisionReason = "air fallback c-bet/check-fold policy";
      double cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
      
      // PHASE 3: Smooth Dynamic Stack-Depth Aggression Scaling
      double depthShift = (depthRatio - 1.0) * 0.15;
      depthShift = Math.max(-0.4, Math.min(0.2, depthShift));
      if (!predatoryMode && depthRatio < 0.6) {
          cbetFreq *= (depthRatio / 0.6);
      } else {
          cbetFreq = Math.max(0.0, Math.min(1.0, cbetFreq + depthShift));
      }

      if (scriptedDumbBotCount > 0) cbetFreq = (headsUpHand) ? 0.60 : 0.0; // PREDATORY BLUFFING 1v1 vs Dumb Bots

      if (smartHuExploitMode) {
        cbetFreq += (smartHuLeak.foldToFlopCbetHUEMA - 0.50) * 0.35;
        if (smartDeficitRecovery) cbetFreq += 0.10;
        if (smartLockdown) cbetFreq -= 0.08;
      }

      if (exploitNit && cbet) cbetFreq = 1.0;
      else if (exploitStation && cbet) cbetFreq = 0.0;
      else if (exploitManiac && cbet) cbetFreq *= 0.80;
      else if (gtoFallback && cbet) cbetFreq = Math.max(0.45, Math.min(0.80, cbetFreq));

      if (zeroBet && cbet && Math.random() < cbetFreq) {
        decisionReason = "fallback c-bet fired with frequency=" + cbetFreq;
         act = 3; actAmount = (int)(Math.max(potSize * 0.4, blind)); 
         if (board.length == 3) cbetFlop = true;
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < 0.70) {
         // Triple Barreling
        decisionReason = "fallback turn barrel after flop c-bet";
         act = 3; actAmount = (int)(potSize * 0.6);
      } else {
        decisionReason = "fallback check/fold";
         act = zeroBet ? 1 : 2; 
      }
    }
    
    // Meta-Bluffing Level 3: Overbetting to exploit other God Bots who respect math
    if (!protectedMode && godBotCount > 0 && scriptedDumbBotCount == 0 && Math.random() < 0.12 && act == 2) {
       decisionReason += " | meta-bluff override triggered";
       act = 3;
       actAmount = (int)(potSize * (1.5 + Math.random()));
    }

    if (smartHuExploitMode) {
      boolean weakOrAir = (myRank > 8 && !draws[0] && !draws[1]);
      if (!zeroBet && weakOrAir) {
        double callRisk = costToCall / Math.max(1.0, super.getChips());
        if (callRisk > 0.20) {
          // Smart bots earn most EV from trapping oversized hero calls; tighten this lane.
          decisionReason += " | smart-HU weak-air clamp forced fold";
          act = 2;
          actAmount = 0;
        }
      }

      if (act == 3 && weakOrAir) {
        int bluffCap = (int) (potSize * (smartDeficitRecovery ? 0.55 : 0.40));
        actAmount = Math.max(blind, Math.min(actAmount, bluffCap));
        decisionReason += " | weak-air bluff cap=" + bluffCap;
      }

      if (act == 3 && smartLockdown && myRank >= 7) {
        int lockdownCap = (int) (potSize * 0.45);
        actAmount = Math.max(blind, Math.min(actAmount, lockdownCap));
        decisionReason += " | smart lockdown cap=" + lockdownCap;
      }
    }

    if (act == 3 && board.length == 5 && smartHuExploitMode) {
      if (smartDeficitRecovery && smartHuLeak.foldToRiverLargeBetHUEMA >= 0.45) {
        actAmount = Math.max(actAmount, (int) (potSize * 0.95));
        decisionReason += " | river pressure boost (deficit recovery)";
      } else if (smartLockdown && myRank > 6) {
        actAmount = Math.max(blind, (int) (potSize * 0.45));
        decisionReason += " | river lockdown sizing";
      } else if (smartHuLeak.foldToRiverLargeBetHUEMA >= 0.55 && myRank > 6) {
        actAmount = Math.max(actAmount, (int) (potSize * 0.80));
        decisionReason += " | river exploit overfold sizing";
      }
    }
    
    // Humanoid Noise: Adding +/- 5 chips to break predictable denominations
    if (act == 3 && !minusOneActive) {
       actAmount += (int)(Math.random() * 11) - 5;
       decisionReason += " | humanoid noise applied";
    }
    
    // FINAL POST-FLOP LEGAL GUARD: Ensure noise didn't violate the increment rule
    if (act == 3) {
       int legalMin = bet + lastRaise;
       if (actAmount < legalMin) {
        actAmount = Math.min(super.getChips() + prevBet, legalMin);
        decisionReason += " | legal-min clamp=" + legalMin;
       }
    }
    
    if (scriptedDumbBotCount > 0 && act == 3 && myRank > 7 && !headsUpHand) {
      decisionReason += " | multiway dumb-bot bluff suppression";
      act = 1;
    } // Only eradicate bluffs in multi-player pots
    if (act == 3 && actAmount <= bet) actAmount = bet + Math.max(lastRaise, blind); // Ensure legal raise (Official Increment Rule)
    if (act == 3 && actAmount == 0) actAmount = Math.max(lastRaise, blind);
    
    if (act == 3 && actAmount >= super.getChips() * 0.9 && !minusOneActive) {
      decisionReason += " | converted to all-in threshold";
      act = 4;
    }

    // NUCLEAR PREDATOR OVERRIDE: 1v1 against fish, NEVER fold top-pair+
    if (predatoryMode && act == 2 && myRank <= 8) {
      decisionReason += " | predator no-fold override";
      act = 1;
    }

    if (act == 3 && actAmount > super.getChips() + prevBet) {
      decisionReason += " | raise exceeds stack converted to all-in";
      act = 4;
      actAmount = super.getChips() + prevBet;
    }
    if (act == 4) { action[0] = 4; action[1] = super.getChips(); }
    else if (act == 3) { action[0] = 3; action[1] = actAmount - prevBet; }
    else if (act == 2) { action[0] = zeroBet ? 1 : 2; action[1] = 0; }
    else { action[0] = 1; action[1] = zeroBet ? 0 : bet - prevBet; } // Standardized increment
    
    if (action[1] >= super.getChips()) {
      decisionReason += " | final amount clipped to stack";
      action[0] = 4;
      action[1] = super.getChips();
    }
    trace("GOD-POSTFLOP",
        "FINAL | reason=" + decisionReason + ", action=" + actionLabel(action[0]) + ", amount=" + action[1]
            + ", actCode=" + act + ", rawActAmount=" + actAmount + ", zeroBet=" + zeroBet + ", cbetFlop="
            + cbetFlop + ", smartHU=" + smartHuExploitMode + ", minusOne=" + minusOneActive);
    BotDiagnostics.recordGodDecision(
      "POSTFLOP",
      super.getName(),
      decisionReason,
      actionLabel(action[0]),
      action[1],
      "tableBet=" + bet + ", prevBet=" + prevBet + ", pot=" + potSize + ", rank=" + myRank
        + ", boardRank=" + boardRank + ", equity=" + String.format("%.3f", equity) + ", potOdds="
        + String.format("%.3f", potOdds) + ", smartHU=" + smartHuExploitMode + ", depthRatio="
        + String.format("%.3f", depthRatio) + ", focusArchetype=" + focusArchetype + ", board="
        + cardsToString(board) + ", hole=" + cardsToString(super.getHand()));
    return action;
  }

  public boolean[] draw(Card[] total) {
    // checks to see if the combined hand and board contains a draw, i.e. 4 cards in
    // a straight or 4 cards in a flush (draw for full house is js two pair or three
    // of a kind)
    Deck.sort(total);
    int straightCount = 0;
    for (int i = 1; i < total.length; i++) {
      if (straightCount < 4) {
        if (total[i].getNum() == total[i - 1].getNum() + 1)
          straightCount++;
        else
          straightCount = 0;
      } else {
        break;
      }
    }
    int[] flushCount = new int[4]; // indexing: 0 - spades, 1 - clubs, 2 - diamonds, 3 - hearts
    boolean flushDraw = false;
    for (Card d : total) {
      String suit = d.getValue().substring(1);
      switch (suit) {
        case "♠️":
          flushCount[0]++;
          break;
        case "♣️":
          flushCount[1]++;
          break;
        case "♦️":
          flushCount[2]++;
          break;
        case "♥️":
          flushCount[3]++;
          break;
      }
    }
    for (int i : flushCount)
      if (i >= 4)
        flushDraw = true;
    return new boolean[] { straightCount > 3, flushDraw };
  }
}

class Names { // avoid dupe names
  private static final String[] names = new String[] { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie",
          "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe",
          "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan",
          "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Toby", "Scarlet", "Alex", "Leon", "Eric",
          "GuyWhoGoesAllInEveryTime", "Fei Yu-Ching", "Jay", "Daniel", "Evan", "Sean", "Selene", "James", "Jacques",
          "NoName", "Zoe", "Sarah", "Kyle", "Irene", "Sharolyn", "Ben", "Coco", "Cindy", "Megan", "Mia", "E10WINS",
          "Audrey", "Emily", "March 7th", "Stelle", "Cao Cao", "Liu", "Camellia", "Cameron", "Maddie", "Will", "Amy",
          "Kelly", "Aventurine" };

  public static String getUniqueName(PokerPlayer[] currentPlayers) {
    while (true) {
      String candidate = names[(int) (Math.random() * names.length)];
      boolean used = false;
      if (currentPlayers != null) {
        for (PokerPlayer p : currentPlayers) {
          if (p != null && candidate.equals(p.getName())) {
            used = true;
            break;
          }
        }
      }
      if (!used) return candidate;
    }
  }
}