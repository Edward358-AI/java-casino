import java.util.*;

public class PokerBot extends PokerPlayer {
  private boolean opMode = false;
  private PokerDeck p = new PokerDeck();
  private int[][] hands = { { 14, 14 }, { 14, 13 }, { 14, 12 }, { 14, 11 }, { 14, 10 }, { 14, 9 }, { 14, 8 }, { 14, 2 },
      { 13, 13 }, { 13, 12 }, { 13, 11 }, { 13, 10 }, { 13, 9 }, { 13, 8 }, { 12, 12 }, { 12, 11 }, { 12, 10 },
      { 12, 9 }, { 12, 8 }, { 11, 11 }, { 11, 10 }, { 11, 9 }, { 11, 8 }, { 10, 10 }, { 9, 9 }, { 8, 8 }, { 7, 7 },
      { 6, 6 }, { 5, 5 }, { 4, 4 }, { 3, 3 }, { 2, 2 } }; // preset hands for smart bot
  public int botLevel; // 0 = dumb, 1 = smart, 2 = god, 3 = archetype bot
  public CognitiveArchetype simulatedArchetype = null; // Phase 10: archetype bot identity
  private boolean cbetFlop = false; // Persistent state for barrelling logic
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_TOTAL = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_RAISE = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_CALL = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_FOLD = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_BET = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_BULLY_CHECK = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_FACING_BET_RAISES = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_FACING_BET_CALLS = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_FACING_BET_FOLDS = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_PRE_OPEN = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_PRE_FOLD = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_PRE_4BET = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_PRE_CALL3B = new java.util.concurrent.atomic.AtomicInteger(0);
  public static java.util.concurrent.atomic.AtomicInteger __DBG_PRE_FOLD3B = new java.util.concurrent.atomic.AtomicInteger(0);
  private boolean predatoryIntent = false; // "Two-Faced" nightmare personality
  private String baseName; // Store original name to allow tag refreshing

  public enum CognitiveArchetype {
    NIT,
    STATION,
    MANIAC,
    TAG,
    LAG,
    ELITE_REG,
    WHALE,
    FISH,
    BULLY,
    SHORT_STACKER,
    UNKNOWN
  }

  // PHASE 8/9: The Dual-Matrix Cognitive System (STM + LTM)
  public static class CognitiveProfile {
    private static final int STYLE_WINDOW = 10;

    public int handsPlayed = 0;
    public double ltmAlpha = 0.01; // Configurable via simulator setup
    public double currentStackBB = 100.0; // Updated each hand for SHORT_STACKER detection

    // STM EMA variables (alpha ~0.35, fast adaptation)
    public double vpipEMA = 0.0;
    public double pfrEMA = 0.0;
    public double afqPreflopEMA = 0.0;
    public double afqFlopEMA = 0.0;
    public double afqTurnEMA = 0.0;
    public double afqRiverEMA = 0.0;
    public double wtsdEMA = 0.0;
    public double foldToCbetEMA = 0.0;

    // LTM EMA variables (alpha ~0.01, slow deep-trend)
    public double ltmVpipEMA = 0.0;
    public double ltmPfrEMA = 0.0;
    public double ltmAfqFlopEMA = 0.0;
    public double ltmAfqTurnEMA = 0.0;
    public double ltmAfqRiverEMA = 0.0;
    public double ltmWtsdEMA = 0.0;

    // Volatility tracking
    public double vIndex = 0.0;
    public double styleShiftEMA = 0.0;

    // Archetype state
    public CognitiveArchetype archetype = CognitiveArchetype.UNKNOWN; // legacy alias
    public CognitiveArchetype stmArchetype = CognitiveArchetype.UNKNOWN;
    public CognitiveArchetype ltmArchetype = CognitiveArchetype.UNKNOWN;
    public CognitiveArchetype finalArchetype = CognitiveArchetype.UNKNOWN;
    public boolean isGearShifted = false;

    // Hysteresis state — smooths classification across hands so per-hand STM jitter
    // doesn't flip finalArchetype. Once an archetype is established, switching requires
    // the new candidate to persist for HYSTERESIS_PENDING_HANDS consecutive calls.
    private CognitiveArchetype establishedArchetype = CognitiveArchetype.UNKNOWN;
    private CognitiveArchetype pendingArchetype = CognitiveArchetype.UNKNOWN;
    private int pendingRunLength = 0;
    private static final int HYSTERESIS_PENDING_HANDS = 3;

    // Backward compatibility for Phase 5/6
    public int aggressiveActions = 0;

    // Per-street EV instrumentation. Tracks how many hands ended at each street
    // (0=preflop, 1=flop, 2=turn, 3=river-or-showdown) and the cumulative net chips
    // delta for those hands. Used by Mode 6 telemetry to identify which streets are
    // leaking BB/100 — essential scaffolding for future per-archetype exploit tuning.
    public final long[] handsEndedAtStreet = new long[4];
    public final long[] netChipsAtStreet = new long[4];

    public void recordHandEnd(int endStreet, long chipDelta) {
      if (endStreet < 0 || endStreet > 3) return;
      handsEndedAtStreet[endStreet]++;
      netChipsAtStreet[endStreet] += chipDelta;
    }

    private final double[] styleHistory = new double[STYLE_WINDOW];
    private int styleSamples = 0;
    private int styleWriteIndex = 0;
    private double lastStylePoint = 0.0;
    private boolean hasLastStylePoint = false;

    public double getAggressionFactor() {
      if (handsPlayed == 0)
        return 0.0;
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

    private double getPostflopAFqBlendLTM() {
      return (ltmAfqFlopEMA + ltmAfqTurnEMA + ltmAfqRiverEMA) / 3.0;
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
      for (int i = 0; i < styleSamples; i++)
        mean += styleHistory[i];
      mean /= styleSamples;
      double variance = 0.0;
      for (int i = 0; i < styleSamples; i++) {
        double d = styleHistory[i] - mean;
        variance += d * d;
      }
      variance /= styleSamples;
      vIndex = Math.sqrt(Math.max(0.0, variance));
    }

    // Archetype classifier — thresholds calibrated to actual Mode 6 telemetry
    // measurements (5000-pair runs, exploits enabled).
    //
    // Reference Arc-bot stat ranges (measured):
    //   NIT:    VPIP 0.12, PFR 0.03, flopAFq 0.00       (never bets postflop)
    //   TAG:    VPIP 0.31, PFR 0.25, flopAFq 0.45-0.51  (bets only paired hands)
    //   LAG:    VPIP 0.31, PFR 0.22, flopAFq 0.84-0.89  (bets ~67%, barrels wide)
    //   BULLY:  VPIP 0.25, PFR 0.25, flopAFq 0.99       (bets 100%, overbets)
    //   MANIAC: VPIP 0.40, PFR 0.40, flopAFq 0.95       (bets every street)
    //
    // Disambiguation:
    //   MANIAC vs BULLY: both have flopAFq ≥ 0.95. MANIAC has VPIP ≥ 0.40 (loose).
    //   BULLY vs LAG: BULLY flopAFq ≥ 0.95, LAG flopAFq 0.65-0.95.
    //   LAG vs TAG: LAG flopAFq ≥ 0.65, TAG flopAFq < 0.65.
    private CognitiveArchetype classifyArchetype(double vpip, double pfr, double totalAFq,
        double postflopAFq, double wtsd, boolean useStack) {
      double flopAFq = useStack ? afqFlopEMA : ltmAfqFlopEMA;
      return classifyArchetype(vpip, pfr, totalAFq, postflopAFq, flopAFq, wtsd, useStack);
    }

    private CognitiveArchetype classifyArchetype(double vpip, double pfr, double totalAFq,
        double postflopAFq, double flopAFq, double wtsd, boolean useStack) {
      // Thresholds calibrated against measured Arc-bot stats at 200k+ hands (50k-pair Mode 6).
      // See classifierTrace output for ground truth measurements.

      // SHORT_STACKER (behavioral): enters pot ONLY by raising (VPIP ≈ PFR), zero postflop play.
      // This is the all-in-or-fold fingerprint and works regardless of current stack depth
      // (Mode 6 duels reset stacks each pair, so the stack-based detector below rarely fires).
      if (handsPlayed > 10 && Math.abs(vpip - pfr) < 0.05 && vpip >= 0.10 && postflopAFq <= 0.05)
        return CognitiveArchetype.SHORT_STACKER;
      // SHORT_STACKER (stack-based): preserved for live games where stack actually drops.
      if (useStack && handsPlayed > 10 && currentStackBB < 25.0)
        return CognitiveArchetype.SHORT_STACKER;

      // NIT: very tight, never bets postflop.
      if (vpip <= 0.18 && pfr <= 0.10 && postflopAFq <= 0.10)
        return CognitiveArchetype.NIT;

      // MANIAC: very loose AND raises every hand they play AND rarely reaches showdown.
      // WTSD is the cleanest MANIAC↔BULLY separator: MANIAC WTSD < 0.10 (always blasts off),
      // BULLY WTSD ≥ 0.15 (gets called and reaches showdown).
      if (vpip >= 0.30 && pfr >= 0.30 && flopAFq >= 0.85 && wtsd <= 0.10)
        return CognitiveArchetype.MANIAC;

      // BULLY: barrels every street (postflop AFq blend ≥ 0.80). Sub-MANIAC behavior
      // (gated out by MANIAC's WTSD ≤ 0.10 check above).
      if (postflopAFq >= 0.80 && vpip >= 0.18 && pfr >= 0.20 && pfr < 0.45)
        return CognitiveArchetype.BULLY;

      // LAG: significant flop barreling (≥ 0.50), lower combined postflop AFq than BULLY.
      // Range overlaps slightly with BULLY at postAFq 0.80-0.85; pfr threshold disambiguates.
      if (flopAFq >= 0.50 && postflopAFq < 0.85 && vpip >= 0.20 && pfr >= 0.10)
        return CognitiveArchetype.LAG;

      // STATION: loose-passive (covers FISH/WHALE — distinct exploit not built per-subtype).
      if (vpip >= 0.20 && pfr <= 0.18 && postflopAFq < 0.40)
        return CognitiveArchetype.STATION;

      // TAG: moderate stats, mid aggression, < 50% flopAFq. Wider vpip/pfr bounds to handle
      // the variance ARC-TAG actually exhibits across runs (vpip 0.17–0.45, pfr 0.10–0.33).
      if (vpip >= 0.15 && vpip < 0.50 && pfr >= 0.05 && pfr < 0.40 && flopAFq < 0.50)
        return CognitiveArchetype.TAG;

      return CognitiveArchetype.UNKNOWN;
    }

    private void refreshArchetype() {
      double stmPostAFq = getPostflopAFqBlend();
      double stmTotalAFq = (pfrEMA + stmPostAFq) / 2.0;
      double ltmPostAFq = getPostflopAFqBlendLTM();
      double ltmTotalAFq = (ltmPfrEMA + ltmPostAFq) / 2.0;

      stmArchetype = classifyArchetype(vpipEMA, pfrEMA, stmTotalAFq, stmPostAFq, wtsdEMA, true);
      ltmArchetype = classifyArchetype(ltmVpipEMA, ltmPfrEMA, ltmTotalAFq, ltmPostAFq, ltmWtsdEMA, false);

      // ELITE_REG: only after 500 hands with stable TAG LTM + elevated volatility
      if (handsPlayed > 500 && ltmArchetype == CognitiveArchetype.TAG && vIndex >= 0.04) {
        stmArchetype = CognitiveArchetype.ELITE_REG;
        ltmArchetype = CognitiveArchetype.ELITE_REG;
      }

      // Normalized Euclidean Distance (3-stat) — measures STM's deviation from LTM trend
      double dVpip = vpipEMA - ltmVpipEMA;
      double dPfr = pfrEMA - ltmPfrEMA;
      double dAFq = stmTotalAFq - ltmTotalAFq;
      double distance = Math.sqrt((dVpip * dVpip + dPfr * dPfr + dAFq * dAFq) / 3.0);

      double dynamicThreshold = 0.15 + (vIndex * 1.5);

      // Candidate archetype: prefer LTM after warmup (LTM is more stable). Before
      // warmup, fall back to STM (LTM hasn't accumulated enough updates).
      // Note: empirically LTM AFq under-converges in Mode 6 duels (only ~1200 updates
      // for a bot in 10k hands), so STM is often more accurate. Use STM after 30 hands.
      CognitiveArchetype candidate;
      if (handsPlayed >= 30) {
        // STM is well-converged (alpha 0.35 → 99% in ~12 updates). Trust STM unless
        // it's UNKNOWN, in which case fall back to LTM.
        candidate = (stmArchetype != CognitiveArchetype.UNKNOWN) ? stmArchetype : ltmArchetype;
        isGearShifted = (candidate == stmArchetype && distance >= dynamicThreshold);
      } else if (distance < dynamicThreshold) {
        candidate = ltmArchetype;
        isGearShifted = false;
      } else {
        candidate = stmArchetype;
        isGearShifted = true;
      }

      // HYSTERESIS: prevent per-hand classification flips.
      //   - If candidate matches established → keep established.
      //   - If candidate matches pending → increment pendingRunLength; promote when threshold hit.
      //   - If candidate is something new → reset pending to candidate (length 1).
      if (candidate == establishedArchetype) {
        pendingArchetype = CognitiveArchetype.UNKNOWN;
        pendingRunLength = 0;
      } else if (candidate == pendingArchetype) {
        pendingRunLength++;
        if (pendingRunLength >= HYSTERESIS_PENDING_HANDS) {
          establishedArchetype = pendingArchetype;
          pendingArchetype = CognitiveArchetype.UNKNOWN;
          pendingRunLength = 0;
        }
      } else {
        pendingArchetype = candidate;
        pendingRunLength = 1;
      }

      // First-time establishment: if no established yet, accept candidate immediately.
      if (establishedArchetype == CognitiveArchetype.UNKNOWN) {
        establishedArchetype = candidate;
      }

      finalArchetype = establishedArchetype;
      archetype = finalArchetype; // keep legacy field in sync
    }

    // EMA update — maintains both STM (fast alpha) and LTM (slow ltmAlpha).
    public void updateEMA(String stat, double value, double alpha) {
      switch (stat) {
        case "VPIP":
          vpipEMA = (alpha * value) + ((1 - alpha) * vpipEMA);
          ltmVpipEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmVpipEMA);
          break;
        case "PFR":
          pfrEMA = (alpha * value) + ((1 - alpha) * pfrEMA);
          ltmPfrEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmPfrEMA);
          break;
        case "AFq_Preflop":
          afqPreflopEMA = (alpha * value) + ((1 - alpha) * afqPreflopEMA);
          break;
        case "AFq_Flop":
          afqFlopEMA = (alpha * value) + ((1 - alpha) * afqFlopEMA);
          ltmAfqFlopEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmAfqFlopEMA);
          break;
        case "AFq_Turn":
          afqTurnEMA = (alpha * value) + ((1 - alpha) * afqTurnEMA);
          ltmAfqTurnEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmAfqTurnEMA);
          break;
        case "AFq_River":
          afqRiverEMA = (alpha * value) + ((1 - alpha) * afqRiverEMA);
          ltmAfqRiverEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmAfqRiverEMA);
          break;
        case "WTSD":
          wtsdEMA = (alpha * value) + ((1 - alpha) * wtsdEMA);
          ltmWtsdEMA = (ltmAlpha * value) + ((1 - ltmAlpha) * ltmWtsdEMA);
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
      return finalArchetype;
    }

    public String getArchetypeLabel() {
      return finalArchetype.name() + (isGearShifted ? "*" : "");
    }

    /** Diagnostic: returns a single-line trace of this profile's classifier inputs and outputs. */
    public String classifierTrace(String name) {
      double stmAfqMean = (afqFlopEMA + afqTurnEMA + afqRiverEMA) / 3.0;
      double ltmAfqMean = (ltmAfqFlopEMA + ltmAfqTurnEMA + ltmAfqRiverEMA) / 3.0;
      return String.format(
        "[CLASSIFIER] name=%s hands=%d ltmAlpha=%.5f stm{vpip=%.3f pfr=%.3f afq=%.3f flop=%.3f turn=%.3f river=%.3f}=%s ltm{vpip=%.3f pfr=%.3f afq=%.3f flop=%.3f turn=%.3f river=%.3f}=%s final=%s gearShift=%s",
        name, handsPlayed, ltmAlpha,
        vpipEMA, pfrEMA, stmAfqMean, afqFlopEMA, afqTurnEMA, afqRiverEMA, stmArchetype.name(),
        ltmVpipEMA, ltmPfrEMA, ltmAfqMean, ltmAfqFlopEMA, ltmAfqTurnEMA, ltmAfqRiverEMA, ltmArchetype.name(),
        finalArchetype.name(), isGearShifted);
    }
  }

  /** Diagnostic: dump classifier traces for all tracked profiles to stderr. */
  public static void dumpClassifierTrace() {
    java.util.Map<String, CognitiveProfile> db = getCognitiveDB();
    if (db == null || db.isEmpty()) return;
    System.err.println("=== CLASSIFIER TRACE (" + db.size() + " profiles) ===");
    for (java.util.Map.Entry<String, CognitiveProfile> e : db.entrySet()) {
      System.err.println(e.getValue().classifierTrace(e.getKey()));
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

  private static final ThreadLocal<Map<String, CognitiveProfile>> cognitiveDBLocal = ThreadLocal
      .withInitial(HashMap::new);
  private static final ThreadLocal<Map<String, SmartLeakProfile>> smartLeakDBLocal = ThreadLocal
      .withInitial(HashMap::new);
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

  // ============================================================================
  // PER-ARCHETYPE EXPLOIT KILL-SWITCHES (Neural Sandbox only)
  // Each branch can be toggled independently for A/B verification at 50k pairs.
  // Flip to false to revert that specific exploit; recompile to apply.
  // ============================================================================
  // TAG (tells: bets 65% pot only with pair, never bluffs, folds air to overbet)
  private static final boolean EXPLOIT_TAG_FOLD_TO_BET            = true;
  private static final boolean EXPLOIT_TAG_BLUFF_OVERBET_VS_CHECK = true;
  private static final boolean EXPLOIT_TAG_NO_THIN_VALUE          = true;
  // BULLY (tells: always overbets 120% pot, re-raises 50% facing bets)
  private static final boolean EXPLOIT_BULLY_NEVER_CBET           = false;
  private static final boolean EXPLOIT_BULLY_CHECK_RAISE_TOP_PAIR = true;
  private static final boolean EXPLOIT_BULLY_FOLD_AIR             = true;
  // TIGHTEN_PRE disabled: BULLY-counter preflop hard counter (4-bet QQ+, fold marginal vs
  // 3-bet) handles defense. Tightening open in SB just costs blinds when BULLY folds.
  private static final boolean EXPLOIT_BULLY_TIGHTEN_PRE          = false;
  // LAG (tells: 50%-pot=bluff, 70%-pot=value, check-raises 45% with strong)
  // 3BET_SMALL_BET / BIG_BET_RESPONSE: disabled — narrow defensive overlays both regressed
  //   at 50k pairs (Forcing fold air to LAG value bets removes Pure GTO's calling overlay
  //   on backdoor equity; raising small bets builds pots that resolve worse on later streets.)
  // TIGHTEN_PRE: enabled — Neural Sandbox's spicy preflop opens (wheelAce, faceCards, mixed)
  //   open ~50% width vs LAG's heavy aggression. Tighten to ~25% baseline to stop the bleed.
  private static final boolean EXPLOIT_LAG_3BET_SMALL_BET         = false;
  private static final boolean EXPLOIT_LAG_BIG_BET_RESPONSE       = false;
  // LAG_TIGHTEN_PRE disabled: same logic as BULLY — preflop hard counter handles defense
  // vs 3-bet, tightening opens just bleeds blinds when LAG folds (~65% of hands).
  private static final boolean EXPLOIT_LAG_TIGHTEN_PRE            = false;
  // SHORT_STACKER (tell: shoves every postflop decision)
  private static final boolean EXPLOIT_SS_TIGHT_OPEN              = false;
  // TAG: TAG only 3-bets ~5% (premiums). We can OPEN WIDER vs TAG since they fold 75%
  //   to opens AND rarely punish marginal opens. Gain blinds via fold equity.
  private static final boolean EXPLOIT_TAG_WIDE_OPEN              = true;

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
      else if (botLevel == 3) {
        tag = (simulatedArchetype != null) ? " [ARC-" + simulatedArchetype.name() + "]" : " [ARC]";
      } else {
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

  /** Phase 10: Centralized live game spawning tree (God bots and 7-Archetypes) */
  public static PokerBot createLiveGameBot(PokerPlayer[] currentPlayers) {
    PokerBot newBot = new PokerBot(currentPlayers);
    double spawnRoll = Math.random();

    if (spawnRoll < 0.40) {
      // 40% chance: spawn a God Bot (via the randomized spawning tree)
      newBot.setBotLevel(2);
      double godRoll = Math.random();
      if (godRoll < 0.50) {
        // 50% Protected
        newBot.setProtectedMode(true);
        if (Math.random() < 0.50) {
          newBot.setNeuralProtectedMode(false); // Protected + No Cognition = ELITE_REG-like
        } else {
          newBot.setNeuralProtectedMode(true);  // Protected + Cognitive Matrix
        }
      } else {
        // 50% Unprotected
        newBot.setProtectedMode(false);
        if (Math.random() >= 0.33) {
          // Nightmare Mode (Bold or Sneaky)
          newBot.setNightmareIntensity(2);
          newBot.setNightmareActive(true);
          newBot.setPredatoryIntent(Math.random() < 0.50);
        }
      }
    } else {
      // 60% chance: spawn a random archetype bot (7 archetypes, excluding SHORT_STACKER)
      CognitiveArchetype[] archetypePool = {
        CognitiveArchetype.NIT, CognitiveArchetype.MANIAC, CognitiveArchetype.STATION,
        CognitiveArchetype.TAG, CognitiveArchetype.WHALE, CognitiveArchetype.FISH, CognitiveArchetype.BULLY,
        CognitiveArchetype.LAG
      };
      CognitiveArchetype chosen = archetypePool[(int)(Math.random() * archetypePool.length)];
      newBot.setBotLevel(3);
      newBot.setSimulatedArchetype(chosen);
      newBot.setProtectedMode(false);
    }

    newBot.refreshNameTag(currentPlayers);
    return newBot;
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
    if (level == 2 && !protectedMode)
      predatoryIntent = (Math.random() < 0.5); // Re-roll intent for promoted gods (disable if protected)
    refreshNameTag(null);
  }

  /** Phase 10: Set the simulated archetype for a botLevel 3 archetype bot. */
  public void setSimulatedArchetype(CognitiveArchetype archetype) {
    this.simulatedArchetype = archetype;
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

  /** Phase 10: Directly set predatory intent for God Bot spawning tree. */
  public void setPredatoryIntent(boolean val) {
    this.predatoryIntent = val;
  }

  /** Phase 10: Directly activate nightmare mode for God Bot spawning tree. */
  public void setNightmareActive(boolean val) {
    this.nightmareActive = val;
    if (val && botLevel != 2) botLevel = 2;
  }

  public int getNightmareIntensity() {
    return nightmareIntensity;
  }

  private void trace(String stage, String message) {
    if (!BotDiagnostics.traceConsoleEnabled()) {
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

  public int[] action(String round, int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize,
      PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int tablePlayers = 0;
    for (PokerPlayer p : players)
      if (p.getChips() > 0)
        tablePlayers++;
    boolean headsUpTable = (tablePlayers == 2);

    int activeCount = 0;
    for (PokerPlayer p : players)
      if (p.inHand())
        activeCount++;
    boolean headsUpHand = (activeCount == 2);

    String stage = "ACTION-" + round.toUpperCase();
    if (botLevel == 1 || botLevel == 2) {
      trace(stage,
          "ENTER | level=" + botLevel + ", hole=" + cardsToString(super.getHand()) + ", prevBet=" + prevBet
              + ", tableBet=" + bet + ", blind=" + blind + ", lastRaise=" + lastRaise + ", pot=" + potSize
              + ", headsUpTable=" + headsUpTable + ", headsUpHand=" + headsUpHand);
      traceTableState(stage, players, board, seatIndex);
    }

    if (botLevel == 3) {
      if (round.equals("preflop")) {
        return archetypePreflop(prevBet, bet, blind, lastRaise, players, seatIndex);
      } else {
        return archetypePostflop(prevBet, bet, blind, potSize, board, players, seatIndex);
      }
    }

    if (botLevel == 2) {
      if (round.equals("preflop")) {
        return godPreflop(prevBet, bet, blind, lastRaise, players, seatIndex, sbIdx, bbIdx);
      } else {
        return godPostflop(prevBet, bet, blind, lastRaise, board, potSize, players, seatIndex, preflopAggressorIndex,
            sbIdx, bbIdx);
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
            // if those "conditions" are not met, then has 15% to continue and all in the
            // current bet, otherwise folds.
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
          if (headsUpTable && (cardInts[0] >= 12 || cardInts[1] >= 12))
            callAirFreq = 1.0; // Play any Q+ preflop 1v1

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
                if (Math.random() < 0.50)
                  subAction = 0;
                else
                  subAction = 4; // Gamble on draws 50%
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

  private int[] godPreflop(int prevBet, int bet, int blind, int lastRaise, PokerPlayer[] players, int seatIndex,
      int sbIdx, int bbIdx) {
    int numPlayers = players.length;
    int relativePos = (seatIndex - sbIdx + numPlayers) % numPlayers;

    int[] action = new int[2];
    String decisionReason = "unresolved";
    int[] numhand = Deck.cardToInt(super.getHand());
    Arrays.sort(numhand);
    boolean paired = numhand[0] == numhand[1];
    boolean suited = super.getHand()[0].getValue().charAt(1) == super.getHand()[1].getValue().charAt(1);
    boolean premium = (paired && numhand[0] >= 10) || (numhand[0] >= 13 && numhand[1] >= 13)
        || (numhand[1] == 14 && numhand[0] >= 11);

    boolean earlyPos = (relativePos >= 2 && relativePos <= 3);
    boolean latePos = (relativePos >= numPlayers - 2);
    boolean inBlinds = (seatIndex == sbIdx || seatIndex == bbIdx);
    boolean unraised = (bet == blind || bet == 0);

    boolean chipleader = true;
    int tablePlayers = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
        tablePlayers++;
        if (p != this && p.getChips() > super.getChips())
          chipleader = false;
      }
    }
    boolean headsUpTable = (tablePlayers == 2);
    boolean shortStacks = true;
    int dumbBotCount = 0;
    int smartBotCount = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
        if (p.inHand() && p != this && p.getChips() > blind * 20)
          shortStacks = false;

        if (p instanceof PokerBot && p != this) {
          int level = ((PokerBot) p).botLevel;
          if (level == 0)
            dumbBotCount++;
          else if (level == 1)
            smartBotCount++;
        }
      }
    }

    boolean neuralSandbox = neuralSandboxEnabled();
    boolean pureProtectedGtoMode = protectedMode && !neuralSandbox;
    // Protected-only baseline intentionally masks bot-tier tracking for pure GTO
    // behavior.
    int scriptedDumbBotCount = pureProtectedGtoMode ? 0 : dumbBotCount;
    int scriptedSmartBotCount = pureProtectedGtoMode ? 0 : smartBotCount;
    // Aggressive Arc-bots (TAG/LAG/BULLY/SS/NIT) count as scripted opponents — the
    // GodBot bluff lanes (mixedStrategy, nutBlocker jam, trapMode, semiBluff) bleed
    // against deterministic aggressive/value-only ranges. Passive Arc-bots
    // (STATION/MANIAC/WHALE/FISH) do NOT count — those bluff lanes print against
    // opponents that fold too much or call inappropriately.
    int scriptedArchetypeBotCount = 0;
    if (!pureProtectedGtoMode) {
      for (PokerPlayer pr : players) {
        if (pr != null && pr != this && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3) {
          CognitiveArchetype sa = ((PokerBot) pr).simulatedArchetype;
          if (sa == CognitiveArchetype.TAG || sa == CognitiveArchetype.LAG
              || sa == CognitiveArchetype.BULLY || sa == CognitiveArchetype.SHORT_STACKER
              || sa == CognitiveArchetype.NIT) {
            scriptedArchetypeBotCount++;
          }
        }
      }
    }

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

    // Split-Brain Trigger: Only be "Spicy" if only Humans or other God Bots are in
    // the pot. Archetype bots also disqualify (deterministic, not "thinking").
    boolean isThinkingOpponentOnly = (!pureProtectedGtoMode && scriptedDumbBotCount == 0 && scriptedSmartBotCount == 0
                                       && scriptedArchetypeBotCount == 0);

    // PHASE 4: Universal Split-Personality Parameters (Active across all table
    // sizes if in Nightmare Mode)
    boolean isNightmareMode = false;
    for (PokerPlayer pr : players)
      if (pr != null && "edjiang1234".equalsIgnoreCase(pr.getName()))
        isNightmareMode = true;
    boolean isGB = (isNightmareMode && predatoryIntent && !protectedMode);
    boolean isGS = (isNightmareMode && !predatoryIntent && !protectedMode);

    // PHASE 5/8: Opponent Aggression Profiling (Cognitive Database).
    // Track every non-self opponent — bot or human. In Mode 6 / bot-vs-bot duels the
    // opponent IS a PokerBot, so excluding bots leaves PHASE 5 bluff-defense dormant
    // and we get barrelled off our pairs and busted draws.
    double maxHumanAggression = 0.0;
    for (PokerPlayer pr : players) {
      if (pr != null && pr != this && pr.inHand()) {
        CognitiveProfile np = getCognitiveDB().get(pr.getName());
        if (np != null && np.handsPlayed >= 1 && np.getAggressionFactor() > maxHumanAggression) {
          maxHumanAggression = np.getAggressionFactor();
        }
      }
    }

    // PHASE 9: Archetype-targeted preflop execution matrix.
    // Multiway Suppressor + Homogeneous Override + Worst-Case Targeting + Phased Strength.
    int activeCount = 0;
    for (PokerPlayer pr : players) if (pr != null && pr.inHand()) activeCount++;

    CognitiveArchetype focusArchetype = CognitiveArchetype.UNKNOWN;
    int focusStack = -1;
    boolean foundHumanFocus = false;
    boolean xrayConfirmed = false; // true when focusArchetype came from an Arc-bot's simulatedArchetype field (perfect read)
    java.util.List<CognitiveArchetype> opponentArchetypes = new java.util.ArrayList<>();
    // Pure GTO mode (Protected ON, Neural OFF) MUST ignore telemetry per architecture
    // (see pokerbot.md). Skip cognitive profile reads entirely so all exploit booleans
    // stay false and PHASE 12 / huFoldChance archetype offsets do not fire.
    if (!pureProtectedGtoMode) {
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand()) continue;
      CognitiveArchetype ca;
      // PHASE 10 OMNISCIENT X-RAY: if unprotected and opponent is an archetype bot, read directly
      if (!protectedMode && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3
          && ((PokerBot) pr).simulatedArchetype != null) {
        ca = ((PokerBot) pr).simulatedArchetype;
        opponentArchetypes.add(ca);
        if (foundHumanFocus) continue;
        if (headsUpTable || pr.getChips() > focusStack) {
          focusStack = pr.getChips(); focusArchetype = ca; xrayConfirmed = true;
        }
        continue;
      }
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp == null || cp.handsPlayed < 3) continue;
      ca = cp.getArchetype();
      opponentArchetypes.add(ca);
      if (!(pr instanceof PokerBot)) {
        if (!foundHumanFocus || headsUpTable || pr.getChips() > focusStack) {
          focusStack = pr.getChips(); focusArchetype = ca; foundHumanFocus = true;
        }
        continue;
      }
      // In Protected Mode, X-Ray is already blocked above (line 1070 checks !protectedMode).
      // The cognitive DB is fair game — it's just observing behavior patterns.
      if (foundHumanFocus) continue;
      if (headsUpTable || pr.getChips() > focusStack) { focusStack = pr.getChips(); focusArchetype = ca; }
    }
    }

    // Danger hierarchy for worst-case targeting in mixed multiway pots
    final java.util.List<CognitiveArchetype> DANGER_HIERARCHY = java.util.Arrays.asList(
        CognitiveArchetype.NIT, CognitiveArchetype.ELITE_REG, CognitiveArchetype.TAG,
        CognitiveArchetype.STATION, CognitiveArchetype.MANIAC, CognitiveArchetype.SHORT_STACKER);

    boolean isHomogeneous = false;
    if (activeCount >= 3 && opponentArchetypes.size() >= 2) {
      isHomogeneous = opponentArchetypes.stream().distinct().count() == 1;
      if (!isHomogeneous) {
        for (CognitiveArchetype danger : DANGER_HIERARCHY)
          if (opponentArchetypes.contains(danger)) { focusArchetype = danger; break; }
      }
    }

    // Multiway Suppressor: scale exploit aggression based on table size
    double multiwayStrength = (activeCount == 2) ? 1.0 : (isHomogeneous ? 1.0 : (activeCount == 3 ? 0.25 : 0.0));

    // Hand-count Phase Strength: 0-24 = GTO baseline, 25-49 = cautious, 50+ = full MES
    // PHASE 10: X-Ray vision — archetype bots get phaseStrength = 1.0 immediately
    CognitiveProfile focusOpponentProfile = null;
    boolean xRayActive = false;
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand()) continue;
      if (!protectedMode && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3
          && ((PokerBot) pr).simulatedArchetype == focusArchetype) { xRayActive = true; break; }
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp != null && cp.getArchetype() == focusArchetype) { focusOpponentProfile = cp; break; }
    }
    double phaseStrength;
    if (xRayActive) {
      phaseStrength = 1.0; // Perfect knowledge — skip the EMA warmup entirely
    } else if (focusOpponentProfile == null || focusOpponentProfile.handsPlayed < 25) {
      phaseStrength = 0.0;
    } else if (focusOpponentProfile.handsPlayed < 50) {
      phaseStrength = (!focusOpponentProfile.isGearShifted
          && focusOpponentProfile.stmArchetype == focusOpponentProfile.ltmArchetype) ? 0.5 : 0.0;
    } else {
      phaseStrength = 1.0;
    }
    double exploitStrength = phaseStrength * multiwayStrength;

    boolean exploitNit          = (focusArchetype == CognitiveArchetype.NIT);
    boolean exploitManiac       = (focusArchetype == CognitiveArchetype.MANIAC);
    boolean exploitStation      = (focusArchetype == CognitiveArchetype.STATION
                                    || focusArchetype == CognitiveArchetype.WHALE
                                    || focusArchetype == CognitiveArchetype.FISH);
    boolean gtoFallback         = (focusArchetype == CognitiveArchetype.ELITE_REG);
    // PER-ARCHETYPE REBUILD (TAG/BULLY/LAG/SS): fire when classifier has converged
    // (Neural Sandbox + phaseStrength >= 0.5 from EMA observation) OR when X-Ray
    // gives a perfect read (Unprotected vs Arc-bot — botLevel==3 simulatedArchetype).
    // Pure GTO still skips entirely (classifier reads are blocked above).
    boolean archetypeCounterReady = (neuralSandbox && phaseStrength >= 0.5) || xrayConfirmed;
    boolean exploitBully        = archetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.BULLY);
    boolean exploitTag          = archetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.TAG);
    boolean exploitLag          = archetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.LAG);
    boolean exploitShortStacker = archetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.SHORT_STACKER);

    trace("GOD-PREFLOP",
        "START | hole=" + cardsToString(super.getHand()) + ", prevBet=" + prevBet + ", tableBet=" + bet
            + ", blind=" + blind + ", lastRaise=" + lastRaise + ", seatIndex=" + seatIndex + ", relativePos="
            + relativePos + ", paired=" + paired + ", suited=" + suited + ", premium=" + premium
            + ", headsUpTable=" + headsUpTable + ", smartHU=" + smartHuExploitMode + ", focusArchetype="
            + focusArchetype + ", maxHumanAgg=" + maxHumanAggression + ", isGB=" + isGB + ", isGS=" + isGS
            + ", neuralSandbox=" + neuralSandbox);

    boolean stealRange = paired || numhand[1] >= 14 || (suited && numhand[1] - numhand[0] <= 4);
    boolean smartPressureRange = smartHuExploitMode
        && (paired || numhand[1] >= 12 || (suited && numhand[1] - numhand[0] <= 5));

    // GTO Hardening: Balanced Early Range (Matching Smart Bot + Suited Wheel Aces)
    // - SPICY MODE ONLY
    // Spicy preflop opens (wheelAce, faceCards, mixedStrategy) widen to ~50% range.
    // These were designed for unknown opponents but bleed vs aggressive reads.
    // Suppress when we have a confident archetype read (anything but UNKNOWN/STATION
    // family) — TAG/LAG/BULLY/MANIAC punish wide opens; NIT/STATION/WHALE/FISH/SS
    // don't (they fold or call passively).
    boolean focusIsAggressive = (focusArchetype == CognitiveArchetype.TAG
                                 || focusArchetype == CognitiveArchetype.LAG
                                 || focusArchetype == CognitiveArchetype.BULLY
                                 || focusArchetype == CognitiveArchetype.MANIAC);
    boolean spicyOpenAllowed = !focusIsAggressive;
    boolean wheelAce = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && spicyOpenAllowed
        && (suited && numhand[1] == 14 && numhand[0] >= 2 && numhand[0] <= 9) && !isGS;
    boolean faceCards = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && spicyOpenAllowed
        && ((numhand[1] >= 13 && numhand[0] >= 10) || (numhand[1] == 12 && numhand[0] >= 11));
    boolean earlyRange = premium || paired || faceCards || wheelAce;

    // Mixed Strategy Anomaly (15%): Playing GTO Gappers/Trash from any position -
    // SPICY MODE ONLY
    double mixedFreq = isGB ? 0.35 : (isGS ? 0.05 : 0.15);
    if (smartHuExploitMode) {
      mixedFreq = Math.max(0.0, mixedFreq - 0.10);
    }
    if (gtoFallback)
      mixedFreq = 0.0;
    else if (exploitNit)
      mixedFreq = Math.min(0.50, mixedFreq + (0.10 * exploitStrength));
    else if (exploitStation)
      mixedFreq = Math.max(0.0, mixedFreq - (0.10 * exploitStrength));
    boolean mixedStrategy = (!protectedMode || neuralSandbox) && isThinkingOpponentOnly && spicyOpenAllowed
        && (Math.random() < mixedFreq && (suited && numhand[1] - numhand[0] <= 5));

    // Heads-Up Tournament Protocol: Any Ace, King, Queen or Pair becomes Premium
    if (headsUpTable) {
      if (numhand[1] >= 12 || paired)
        premium = true;
    }


    // PHASE 9: MANIAC exploit — Trap AA/KK with flat; light 3-bet suited blockers
    if (exploitManiac && exploitStrength > 0 && bet > blind) {
      boolean ultraPremium = paired && numhand[0] >= 13; // AA or KK
      boolean lightBlocker = suited && numhand[1] == 14 && numhand[0] <= 9; // A2s-A9s
      if (ultraPremium) {
        decisionReason = "maniac exploit: trap AA/KK with flat call";
        action[0] = 1; action[1] = bet - prevBet;
        BotDiagnostics.recordGodDecision("PREFLOP", super.getName(), decisionReason,
            actionLabel(action[0]), action[1], "focusArchetype=" + focusArchetype);
        cbetFlop = false;
        return action;
      }
      if (lightBlocker && Math.random() < (0.45 * exploitStrength)) {
        decisionReason = "maniac exploit: light 3-bet with suited blocker";
        action[0] = 3;
        int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
        action[1] = raiseTo - prevBet;
        BotDiagnostics.recordGodDecision("PREFLOP", super.getName(), decisionReason,
            actionLabel(action[0]), action[1], "focusArchetype=" + focusArchetype);
        cbetFlop = false;
        return action;
      }
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
    if (exploitBully && EXPLOIT_BULLY_TIGHTEN_PRE) {
      // BULLY 3-bets ~70% of playable. Don't WIDEN defense — narrow it. We override below
      // for the open-only case; here we just refuse to expand defense range vs BULLY.
      // (Intentional no-op: do not add to defenseRange.)
    }
    if (gtoFallback) {
      // Tighter baseline defense: top pairs, strong broadways, suited connectors
      defenseRange = paired || (numhand[1] >= 14) || (numhand[1] >= 12 && numhand[0] >= 10)
          || (suited && numhand[1] - numhand[0] <= 2 && numhand[1] >= 9);
    }

    if (premium || (chipleader && shortStacks && stealRange) || mixedStrategy) {
      decisionReason = "open/3bet value lane: premium or pressure steal or mixed strategy";
      action[0] = 3;
      int intended = (bet > blind) ? (bet * 3) : (blind * 3);
      // STATION (includes former WHALE/FISH): inflate value sizing by up to 50% — inelastic to size
      if (exploitStation && premium && exploitStrength > 0) {
        double sizeMult = 1.0 + (0.5 * exploitStrength);
        intended = (int) (intended * sizeMult);
        decisionReason = "station value oversize: " + (int)(sizeMult * 100) + "%";
      }
      int raiseTo = Math.min(Math.max(intended, bet + lastRaise), super.getChips() + prevBet);
      action[1] = raiseTo - prevBet;
    } else if (bet > blind && (isThinkingOpponentOnly || maxHumanAggression > 0.0 || smartHuExploitMode
        || protectedMode || xrayConfirmed)) {
      // SPICY MODE: Active Defense Logic (also entered in protectedMode to avoid HU fallback punt,
      // and on xrayConfirmed so unprotected vs Arc-bots routes through the archetype hard counters
      // instead of the HU fallback that 4-bets non-premium hands vs 3-bets).
      // In Protected Mode (Pure GTO OR Neural Sandbox), ALWAYS fall back to GTO baseline.
      // Exploit branches act as overlays on top of GTO, not replacements. Without this,
      // turning on an exploit DISABLES the baseline and we get worse than Pure GTO.
      if (protectedMode) gtoFallback = true;
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

      // vs NITs: scale up to 100% 3-bet frequency in defended range based on exploitStrength
      if (exploitNit && defenseRange) {
        threeBetChance = Math.min(1.0, 0.60 + (0.40 * exploitStrength));
      }

      // vs MANIACs: lower 3-bet frequency and bias to flats to induce postflop punts
      if (exploitManiac) {
        threeBetChance = Math.max(0.10, threeBetChance - (0.25 * exploitStrength));
      }

      // BULLY/LAG: 3-bet adjustments handled by GTO baseline (gtoFallback gate above
      // already caps threeBetChance ≤ 0.30 in protected mode). Per-archetype 3-bet
      // tuning was net-negative at 50k pairs; reverted.

      // vs STATION: never bluff preflop with junk; fold non-premium and extract value postflop
      if (exploitStation && exploitStrength > 0 && !premium && !defenseRange) {
        decisionReason = "station exploit: fold non-premium preflop, extract value postflop";
        action[0] = 2; action[1] = 0;
      }

      // PHASE 5: Nemesis VPIP/PFR Exploitation (Tightened - Stop punting garbage)
      boolean hasConfirmedExploit = (exploitNit || exploitManiac || exploitBully || exploitStation || exploitTag || exploitLag || exploitShortStacker) && exploitStrength > 0;
      if (maxHumanAggression > 0.40 && !gtoFallback && !hasConfirmedExploit) {
        // Instead of 100% defenseRange, only expand to reasonably playable hands (any
        // Ace, any broadway, any pair, connected suites)
        defenseRange = earlyRange || stealRange || (numhand[1] >= 14) || faceCards || paired
            || (suited && numhand[1] - numhand[0] <= 3);
        threeBetChance = Math.min(0.80, threeBetChance + 0.20); // Cap 3-bet frequency so it's not literally punting
                                                                // every hand
      }

      // PREFLOP HARD COUNTERS — facing TAG/LAG/BULLY/SS raises, exact analytical defense.
      // These derive from each archetype's preflop source ranges (PokerBot.java:2929+).
      if (exploitShortStacker && bet > blind) {
        // SS analytical counter — SS shoves only postflop AND with top ~32% preflop.
        // Vs SS's wide preflop shove range, JJ+/AK have ~52-86% equity. Call those, fold rest.
        // (The 5% variance in BB/100 across runs reflects all-in showdown noise; structural
        // floor for SS is ~−15 to −25 BB/100 due to forced-fold preflop investment math.)
        boolean callShove = (paired && numhand[0] >= 11)                        // JJ-AA
            || (numhand[1] == 14 && numhand[0] == 13);                          // AK
        if (callShove) {
          action[0] = 1; action[1] = bet - prevBet;
          decisionReason = "SS counter: call shove w/ JJ+/AK";
        } else {
          action[0] = 2; action[1] = 0;
          decisionReason = "SS counter: fold to SS shove";
        }
      } else if (exploitTag && bet > blind) {
        // TAG opens 25% (3x), 3-bets only AA-TT/AK/AQ/KQ at ~9x (3-bet sizing).
        // Differentiate by sizing: bet ≤ 6 BB = open, bet > 6 BB = 3-bet.
        boolean tagThreeBet = bet > blind * 6;
        boolean fourBetHand = (paired && numhand[0] >= 13);                     // KK+
        if (fourBetHand) {
          action[0] = 3;
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
          action[1] = raiseTo - prevBet;
          decisionReason = "TAG counter: 4-bet KK+ (vs TAG's tight 3-bet range)";
        } else if (tagThreeBet) {
          // Vs TAG's 3-bet (AA-TT, AK, AQ, KQ): only continue with hands that beat TAG's range.
          // QQ-JJ have ~50% equity vs AA-TT,AK,AQ,KQ. Marginal — call.
          boolean continueVs3bet = (paired && numhand[0] >= 11);                // JJ+
          if (continueVs3bet) {
            action[0] = 1; action[1] = bet - prevBet;
            decisionReason = "TAG counter: call 3-bet w/ JJ+ (50%+ equity vs TAG 3-bet range)";
          } else {
            action[0] = 2; action[1] = 0;
            decisionReason = "TAG counter: fold to TAG 3-bet (their range crushes ours)";
          }
        } else {
          // Vs TAG's open (~25% TAG range: paired, A6+, K9+, QT+, suited connectors).
          // Defending wide vs that range loses postflop because TAG's range crushes
          // our marginal hands (e.g., QJ vs TAG's KQ/AK). Defend only hands with edge:
          // pairs 77+ (beat TAG's middle pairs), AT+ (dominate broadway range),
          // KQ/KJs (block TAG's premium combos), suited connectors 76s+ (postflop equity).
          boolean openCallHand = (paired && numhand[0] >= 7)                     // 77+
              || (numhand[1] == 14 && numhand[0] >= 10)                          // AT+
              || (numhand[1] == 13 && numhand[0] >= 11)                          // KJ+
              || (suited && numhand[1] - numhand[0] <= 3 && numhand[1] >= 7);    // suited connectors 76s+
          if (openCallHand) {
            action[0] = 1; action[1] = bet - prevBet;
            decisionReason = "TAG counter: defend tight vs TAG open (avoid dominated range)";
          } else {
            action[0] = 2; action[1] = 0;
            decisionReason = "TAG counter: fold trash vs TAG open";
          }
        }
      } else if (exploitLag && bet > blind) {
        // LAG opens 35% (3x), 3-bets ~17% w/ wider range. Differentiate by sizing.
        boolean lagThreeBet = bet > blind * 6;
        boolean fourBetHand = (paired && numhand[0] >= 11)                      // JJ+
            || (numhand[1] == 14 && numhand[0] >= 12);                          // AQ+
        if (fourBetHand) {
          action[0] = 3;
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
          action[1] = raiseTo - prevBet;
          decisionReason = "LAG counter: 4-bet JJ+/AQ+ (vs LAG's wide 3-bet range)";
        } else if (lagThreeBet) {
          // Vs LAG's 3-bet: tighter call range. LAG 3-bet w/ pairs+, AT+, KQ+, KQs.
          boolean continueVs3bet = (paired && numhand[0] >= 9)                  // 99+
              || (numhand[1] == 14 && numhand[0] >= 11);                        // AJ+
          if (continueVs3bet) {
            action[0] = 1; action[1] = bet - prevBet;
            decisionReason = "LAG counter: call 3-bet w/ 99+/AJ+";
          } else {
            action[0] = 2; action[1] = 0;
            decisionReason = "LAG counter: fold to LAG 3-bet";
          }
        } else {
          // Vs LAG's open (~35% range). Defend wide — LAG opens loose, we hold range advantage
          // by defending wider. Postflop hard counter handles aggression.
          boolean openCallHand = paired                                          // any pair
              || (numhand[1] >= 10)                                              // T+ high card
              || (numhand[1] >= 8 && numhand[0] >= 6)                            // mid-strength
              || (suited && numhand[1] - numhand[0] <= 3);                       // suited connectors
          if (openCallHand) {
            action[0] = 1; action[1] = bet - prevBet;
            decisionReason = "LAG counter: defend wide vs LAG open (range advantage)";
          } else {
            action[0] = 2; action[1] = 0;
            decisionReason = "LAG counter: fold trash vs LAG open";
          }
        }
      } else if (exploitBully && bet > blind) {
        // BULLY opens 35-40% w/ 4x sizing, 3-bets 70% of playable when facing raise.
        // We only continue w/ hands that can stand BULLY's overbets postflop.
        boolean fourBetHand = (paired && numhand[0] >= 12);                     // QQ+
        boolean callHand = (paired && numhand[0] >= 8)                          // 88-JJ
            || (numhand[1] == 14 && numhand[0] >= 11)                           // AJ+
            || (numhand[1] >= 12 && numhand[0] >= 10);                          // strong broadway
        if (fourBetHand) {
          action[0] = 3;
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
          action[1] = raiseTo - prevBet;
          __DBG_PRE_4BET.incrementAndGet();
          decisionReason = "BULLY counter: 4-bet QQ+ (BULLY 5-bet range narrow)";
        } else if (bet <= blind * 6) {
          // Vs BULLY's open (~37% range). BULLY 4x opens but we can defend wider — postflop
          // hard counter folds air to overbets, calls pair down for showdown value.
          boolean openCallHand = paired                                          // any pair
              || (numhand[1] >= 11)                                              // J+ high card
              || (numhand[1] >= 9 && numhand[0] >= 7)                            // mid strength
              || (suited && numhand[1] - numhand[0] <= 3);                       // suited connectors
          if (openCallHand) {
            action[0] = 1; action[1] = bet - prevBet;
            __DBG_PRE_CALL3B.incrementAndGet();
            decisionReason = "BULLY counter: defend wide vs BULLY open (postflop CR plan)";
          } else {
            action[0] = 2; action[1] = 0;
            __DBG_PRE_FOLD3B.incrementAndGet();
            decisionReason = "BULLY counter: fold trash vs BULLY open";
          }
        } else {
          action[0] = 2; action[1] = 0;
          __DBG_PRE_FOLD3B.incrementAndGet();
          decisionReason = "BULLY counter: fold to oversized 3-bet";
        }
      } else if (smartHuExploitMode && bet >= blind * 8 && !premium
          && !(paired && numhand[0] >= 9)
          && !(numhand[1] >= 13 && numhand[0] >= 11)) {
        // Against Smart 4-bet/5-bet pressure in HU, avoid spewing marginal opens.
        decisionReason = "smart-HU anti-spew fold vs oversized 4/5-bet pressure";
        action[0] = 2;
        action[1] = 0;
      } else if (gtoFallback && !premium) {
        // Pure baseline fallback against elite/volatile opponents.
        decisionReason = "elite-reg fallback: tighten to baseline defense";
        if (defenseRange && bet <= blind * 4.5) {
          action[0] = 1;
          action[1] = bet - prevBet;
        } else {
          action[0] = 2;
          action[1] = 0;
        }
      } else if (exploitNit && !premium) {
        decisionReason = "nit exploit: respect nit preflop aggression and fold non-premium";
        action[0] = 2;
        action[1] = 0;
      } else if (exploitNit && defenseRange) {
        decisionReason = "nit exploit: high-frequency preflop pressure";
        action[0] = 3;
        int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
        action[1] = raiseTo - prevBet;
      // exploitManiac BB defense tightening REMOVED. The override claimed "induce punts
      // with flatter defend range" but MANIAC opens wide AND barrels — folding marginal
      // hands gives up +EV equity realization. Bisect (50k pairs vs Arc-MANIAC):
      //   override on:  +301 BB/100 unprotected
      //   override off: +358 BB/100 unprotected (matches Pure GTO +355)
      // Net leak: ~57 BB/100. Neural unaffected (within CI noise).
      } else if (defenseRange && Math.random() < threeBetChance) {
        decisionReason = "defense range RNG passed threeBetChance=" + threeBetChance;
        action[0] = 3;
        int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
        action[1] = raiseTo - prevBet;
      } else if (seatIndex == bbIdx && bet <= blind * 4.5 && defenseRange) {
        decisionReason = "big-blind defense flat";
        action[0] = 1;
        action[1] = bet - prevBet; // Big Blind Defense (Sticky Call)
      } else if (latePos && bet <= blind * 3.5 && (paired || wheelAce)) {
        decisionReason = "late-position flat with paired/wheel-ace defense";
        action[0] = 1;
        action[1] = bet - prevBet; // Positional Flatting (Call in position)
      } else if (maxHumanAggression > 0.40 && ((paired && numhand[0] >= 6) || (numhand[1] >= 13 && numhand[0] >= 10))) {
        // PHASE 5: Snap-call manic 5-bet preflop jams with playable stuff (mid-pairs or
        // strong broadways), not arbitrary J-highs
        decisionReason = "high-aggression opponent exploit: snap-call playable blocker hands";
        action[0] = 1;
        action[1] = bet - prevBet;
      } else if (headsUpTable) {
        decisionReason = "heads-up pressure fallback 3-bet";
        action[0] = 3;
        int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips() + prevBet);
        action[1] = raiseTo - prevBet;
      } else {
        decisionReason = "trash fold after defense tree exhausted";
        action[0] = 2;
        action[1] = 0; // Still fold trash
      }
    } else if (unraised && latePos && stealRange) {
      decisionReason = "late-position open steal";
      action[0] = 3;
      action[1] = Math.min(blind * 3, super.getChips());
    } else if (unraised && inBlinds && !headsUpTable) {
      if (bet <= super.getChips() / 4) {
        decisionReason = "blind defense call/check in unraised pot";
        action[0] = 1;
        action[1] = bet > 0 ? bet - prevBet : 0;
      } else {
        decisionReason = "blind defense fold to oversized open";
        action[0] = 2;
        action[1] = 0;
      }
    } else if (earlyPos && earlyRange) {
      decisionReason = "early-position value open";
      action[0] = 3;
      action[1] = Math.min(blind * 3, super.getChips());
    } else {
      if (headsUpTable) {
        // HU SB open: raise hands with equity, fold pure trash
        boolean huOpenRange;
        // Tighten open ONLY vs aggressive archetypes (TAG/LAG/BULLY/SS) where wider opens
        // get punished by 3-bet barrels. Vs passive opponents (STATION/WHALE/FISH/MANIAC/NIT/
        // Smart/Dumb/human), wider opens capture more dead blinds — keep ~85% range.
        boolean tightenVsAggressive = xrayConfirmed && (
            focusArchetype == CognitiveArchetype.TAG
         || focusArchetype == CognitiveArchetype.LAG
         || focusArchetype == CognitiveArchetype.BULLY
         || focusArchetype == CognitiveArchetype.SHORT_STACKER);
        if (protectedMode || tightenVsAggressive) {
          // Tightened GTO baseline: ~45% open.
          huOpenRange = paired || numhand[1] >= 11 || (suited && numhand[1] >= 9) || (numhand[0] >= 9 && numhand[1] >= 10);
        } else {
          // Wider ~85% open vs passive archetypes / Smart / Dumb / human.
          huOpenRange = paired || numhand[1] >= 9 || suited || numhand[0] >= 5;
        }
        if (exploitNit) {
          // NIT folds 95% of BB defenses — open 100% to print blinds.
          huOpenRange = true;
        }
        if (exploitShortStacker) {
          // SS folds preflop ~85% (push-or-fold) — open 100% to capture all that fold equity.
          // Risk: 15% of hands SS shoves over our open and we lose 3 BB. Math: +0.40 BB per
          // open (0.85×1 - 0.15×3) >> -0.5 BB per fold. Open 100%.
          huOpenRange = true;
        }
        if (exploitTag && EXPLOIT_TAG_WIDE_OPEN) {
          // TAG only 3-bets ~5% (AA-TT, AK, AQ, KQ). 95% of the time TAG calls or folds
          // our open. With wide open we capture fold equity on TAG's 75% pre-fold rate.
          // Open ~70% of hands.
          huOpenRange = paired || numhand[1] >= 9 || (suited && numhand[1] >= 7) || (numhand[0] >= 7 && numhand[1] >= 9);
        }
        if (exploitBully && EXPLOIT_BULLY_TIGHTEN_PRE) {
          // BULLY 3-bets ~70% of playable. Wide opens get blasted. Tighten to ~25%
          // (premium + strong broadways) so opens have showdown value vs BULLY's overbets.
          huOpenRange = paired || numhand[1] >= 12 || (suited && numhand[1] >= 11) || (numhand[0] >= 10 && numhand[1] >= 12);
        }
        if (exploitLag && EXPLOIT_LAG_TIGHTEN_PRE) {
          // LAG aggressively 3-bets and barrels. Neural Sandbox's wheelAce/faceCards/mixed
          // opens widen to ~50% which bleeds vs LAG. Tighten to ~25% baseline.
          huOpenRange = paired || numhand[1] >= 12 || (suited && numhand[1] >= 11) || (numhand[0] >= 10 && numhand[1] >= 12);
        }
        if (exploitShortStacker && EXPLOIT_SS_TIGHT_OPEN) {
          // SS shoves 100% postflop and is wide preflop. Open only hands that have ≥40%
          // equity vs a wide jam-or-call range so we're not stuck calling marginal preflop.
          huOpenRange = paired || numhand[1] >= 12 || (suited && numhand[1] >= 11);
        }
        // exploitManiac preflop tightening REMOVED. The override claimed "MANIAC barrels
        // postflop so open premium-only," but MANIAC also CALLS everything — wider opens
        // capture max value from MANIAC's loose defense. Bisect (50k pairs):
        //   override on:  +165 BB/100 unprotected
        //   override off: +291 BB/100 unprotected (matches Pure GTO ~baseline)
        // Net leak from this single branch: ~126 BB/100.
        if (huOpenRange) {
          decisionReason = "heads-up open raise";
          action[0] = 3;
          action[1] = Math.max(bet * 3, blind * 3);
        } else {
          decisionReason = "heads-up fold trash from SB";
          action[0] = 2;
          action[1] = 0;
        }
      } else {
        decisionReason = "multiway fallback fold/check";
        action[0] = 2;
        action[1] = 0;
        if (bet == 0) {
          action[0] = 1;
          action[1] = 0;
        }
      }
    }

    // Humanoid Noise: ±5 chip jitter on preflop raises to look less robotic vs humans.
    // Gated to NOT fire vs X-Ray-confirmed Arc-bots — bisect found this noise causes
    // catastrophic regressions in deterministic Arc-bot play (e.g., Arc-BULLY: -63 → +52
    // BB/100 when disabled vs Arc-bots). Likely cause: noise sometimes pushes bet below
    // legal-min, triggering the all-in clamp at unintended spots in big-pot hands.
    if (action[0] == 3 && !protectedMode && !xrayConfirmed) {
      int noise = (int) (Math.random() * 11) - 5;
      int raiseTo = (prevBet + action[1]) + noise;
      int legalMin = bet + lastRaise;
      if (raiseTo < legalMin)
        raiseTo = Math.min(super.getChips() + prevBet, legalMin + (int) (Math.random() * 5));
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

  private int[] godPostflop(int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize,
      PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int[] action = new int[2];
    String decisionReason = "unresolved";
    Card[] fullHand = new Card[5];
    if (board.length == 3) {
      fullHand[0] = super.getHand()[0];
      fullHand[1] = super.getHand()[1];
      for (int i = 0; i < 3; i++)
        fullHand[i + 2] = board[i];
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
    for (int i = 0; i < board.length; i++)
      total[i + 2] = board[i];
    boolean[] draws = draw(total);
    int outs = 0;
    if (draws[0])
      outs += 8;
    if (draws[1])
      outs += 9;
    double equity = (board.length == 3) ? (outs * 4) / 100.0 : (board.length == 4 ? (outs * 2) / 100.0 : 0);

    boolean zeroBet = (bet == 0);
    double costToCall = zeroBet ? 0 : bet - prevBet;
    double potOdds = costToCall / Math.max(1, (double) (potSize + costToCall));

    int act = 1;
    int actAmount = zeroBet ? 0 : bet - prevBet;

    boolean cbet = (board.length == 3 && preflopAggressorIndex == seatIndex);

    boolean flushScare = false;
    String scareSuit = "";
    int[] flushC = new int[4];
    for (Card d : board) {
      switch (d.getValue().substring(1)) {
        case "♠️":
          flushC[0]++;
          if (flushC[0] >= 3)
            scareSuit = "♠️";
          break;
        case "♣️":
          flushC[1]++;
          if (flushC[1] >= 3)
            scareSuit = "♣️";
          break;
        case "♦️":
          flushC[2]++;
          if (flushC[2] >= 3)
            scareSuit = "♦️";
          break;
        case "♥️":
          flushC[3]++;
          if (flushC[3] >= 3)
            scareSuit = "♥️";
          break;
      }
    }
    for (int i : flushC)
      if (i >= 3)
        flushScare = true;

    // Elite Awareness: Straight Scares and Paired Boards
    boolean straightScare = false;
    if (board.length >= 4) {
      Card[] bSorted = board.clone();
      Deck.sort(bSorted);
      int con = 0;
      for (int i = 1; i < bSorted.length; i++) {
        if (bSorted[i].getNum() == bSorted[i - 1].getNum() + 1)
          con++;
        else if (bSorted[i].getNum() != bSorted[i - 1].getNum())
          con = 0;
        if (con >= 3)
          straightScare = true;
      }
    }

    boolean aceHighBoard = false;
    for (Card d : board)
      if (d.getNum() == 14)
        aceHighBoard = true;

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
      if (pr.inHand())
        activeCount++;
      if (pr.inHand() && pr != this) {
        if (pr.getChips() > largestOpponentStack)
          largestOpponentStack = pr.getChips();
        if (pr instanceof PokerBot) {
          int level = ((PokerBot) pr).botLevel;
          if (level == 1) {
            smartBotCount++;
            if (pr.getChips() > largestSmartStack)
              largestSmartStack = pr.getChips();
          } else if (level == 0) {
            dumbBotCount++;
            if (pr.getChips() > largestDumbStack)
              largestDumbStack = pr.getChips();
          } else if (level == 2)
            godBotCount++;
        }
        // PHASE 5/8: Postflop Aggression Checking — track every opponent (bot or human),
        // otherwise scripted barrel-bots (TAG/LAG/MANIAC) bypass our bluff-defense.
        CognitiveProfile np = getCognitiveDB().get(pr.getName());
        if (np != null && np.handsPlayed >= 1 && np.getAggressionFactor() > maxHumanAggression) {
          maxHumanAggression = np.getAggressionFactor();
        }
      }
    }
    boolean neuralSandbox = neuralSandboxEnabled();
    boolean pureProtectedGtoMode = protectedMode && !neuralSandbox;
    // Protected-only baseline intentionally masks bot-tier tracking for pure GTO
    // behavior.
    int scriptedDumbBotCount = pureProtectedGtoMode ? 0 : dumbBotCount;
    int scriptedSmartBotCount = pureProtectedGtoMode ? 0 : smartBotCount;
    // Aggressive Arc-bots count as scripted; passive ones don't (see preflop comment).
    int scriptedArchetypeBotCount = 0;
    if (!pureProtectedGtoMode) {
      for (PokerPlayer pr : players) {
        if (pr != null && pr != this && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3) {
          CognitiveArchetype sa = ((PokerBot) pr).simulatedArchetype;
          if (sa == CognitiveArchetype.TAG || sa == CognitiveArchetype.LAG
              || sa == CognitiveArchetype.BULLY || sa == CognitiveArchetype.SHORT_STACKER
              || sa == CognitiveArchetype.NIT) {
            scriptedArchetypeBotCount++;
          }
        }
      }
    }

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

    // PHASE 9: Postflop archetype matrix — Multiway Suppressor + Worst-Case Targeting + Phased Strength.
    CognitiveArchetype focusArchetype = CognitiveArchetype.UNKNOWN;
    int focusStack = -1;
    boolean foundHumanFocus = false;
    java.util.List<CognitiveArchetype> postOpponentArchetypes = new java.util.ArrayList<>();
    // Pure GTO mode (Protected ON, Neural OFF) MUST ignore telemetry per architecture
    // (see pokerbot.md). Skip cognitive profile reads so PHASE 12 / huFoldChance offsets do not fire.
    if (!pureProtectedGtoMode) {
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand()) continue;
      CognitiveArchetype ca;
      // PHASE 10 OMNISCIENT X-RAY: if unprotected and opponent is an archetype bot, read directly
      if (!protectedMode && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3
          && ((PokerBot) pr).simulatedArchetype != null) {
        ca = ((PokerBot) pr).simulatedArchetype;
        postOpponentArchetypes.add(ca);
        if (foundHumanFocus) continue;
        if (headsUpHand || pr.getChips() > focusStack) { focusStack = pr.getChips(); focusArchetype = ca; }
        continue;
      }
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp == null || cp.handsPlayed < 3) continue;
      ca = cp.getArchetype();
      postOpponentArchetypes.add(ca);
      if (!(pr instanceof PokerBot)) {
        if (!foundHumanFocus || headsUpHand || pr.getChips() > focusStack) {
          focusStack = pr.getChips(); focusArchetype = ca; foundHumanFocus = true;
        }
        continue;
      }
      // In Protected Mode, X-Ray is already blocked above. Cognitive DB is fair.
      if (foundHumanFocus) continue;
      if (headsUpHand || pr.getChips() > focusStack) { focusStack = pr.getChips(); focusArchetype = ca; }
    }
    }

    final java.util.List<CognitiveArchetype> POST_DANGER_HIERARCHY = java.util.Arrays.asList(
        CognitiveArchetype.NIT, CognitiveArchetype.ELITE_REG, CognitiveArchetype.TAG,
        CognitiveArchetype.BULLY, CognitiveArchetype.STATION, CognitiveArchetype.MANIAC, CognitiveArchetype.SHORT_STACKER);

    boolean postIsHomogeneous = false;
    if (activeCount >= 3 && postOpponentArchetypes.size() >= 2) {
      postIsHomogeneous = postOpponentArchetypes.stream().distinct().count() == 1;
      if (!postIsHomogeneous) {
        for (CognitiveArchetype danger : POST_DANGER_HIERARCHY)
          if (postOpponentArchetypes.contains(danger)) { focusArchetype = danger; break; }
      }
    }

    double postMultiwayStrength = (activeCount == 2) ? 1.0 : (postIsHomogeneous ? 1.0 : (activeCount == 3 ? 0.25 : 0.0));

    CognitiveProfile focusPostProfile = null;
    boolean postXRayActive = false;
    for (PokerPlayer pr : players) {
      if (pr == null || pr == this || !pr.inHand()) continue;
      if (!protectedMode && pr instanceof PokerBot && ((PokerBot) pr).botLevel == 3
          && ((PokerBot) pr).simulatedArchetype == focusArchetype) { postXRayActive = true; break; }
      CognitiveProfile cp = getCognitiveDB().get(pr.getName());
      if (cp != null && cp.getArchetype() == focusArchetype) { focusPostProfile = cp; break; }
    }
    double postPhaseStrength;
    if (postXRayActive) {
      postPhaseStrength = 1.0; // X-Ray: perfect knowledge, skip EMA warmup
    } else if (focusPostProfile == null || focusPostProfile.handsPlayed < 25) {
      postPhaseStrength = 0.0;
    } else if (focusPostProfile.handsPlayed < 50) {
      postPhaseStrength = (!focusPostProfile.isGearShifted
          && focusPostProfile.stmArchetype == focusPostProfile.ltmArchetype) ? 0.5 : 0.0;
    } else {
      postPhaseStrength = 1.0;
    }
    double postExploitStrength = postPhaseStrength * postMultiwayStrength;

    boolean exploitNit          = (focusArchetype == CognitiveArchetype.NIT);
    boolean exploitStation      = (focusArchetype == CognitiveArchetype.STATION
                                    || focusArchetype == CognitiveArchetype.WHALE
                                    || focusArchetype == CognitiveArchetype.FISH);
    boolean exploitManiac       = (focusArchetype == CognitiveArchetype.MANIAC);
    boolean gtoFallback         = (focusArchetype == CognitiveArchetype.ELITE_REG);
    // PER-ARCHETYPE REBUILD: fire when Neural Sandbox classifier has converged
    // (postPhaseStrength >= 0.5) OR when X-Ray confirms the read in Unprotected.
    boolean postArchetypeCounterReady = ((neuralSandbox && postPhaseStrength >= 0.5) || postXRayActive);
    boolean exploitBully        = postArchetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.BULLY);
    boolean exploitTag          = postArchetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.TAG);
    boolean exploitLag          = postArchetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.LAG);
    boolean exploitShortStacker = postArchetypeCounterReady
                                    && (focusArchetype == CognitiveArchetype.SHORT_STACKER);
    // GTO baseline gate: in protected mode, ALWAYS fall back to GTO baseline.
    // Exploit branches act as overlays on top of GTO, not replacements.
    if (protectedMode) gtoFallback = true;

    // PHASE 11: Tactical Range Exploit — modest bluff frequency bump on dry boards
    // when a TAG/LAG/Smart opponent shows weakness (checks).
    // Activation rules:
    //   Protected Mode:   HU only, vs confirmed TAG, LAG, or ELITE_REG
    //   Unprotected Mode: HU only, vs confirmed TAG/LAG/ELITE_REG OR Smart bot
    // TAG/LAG removed from tactical leak exploit — direct archetype exploits handle them now.
    // ELITE_REG kept (no archetype-specific exploit; this leak-based bump is the only tool).
    boolean tacticalTagLagElite = (focusArchetype == CognitiveArchetype.ELITE_REG);
    boolean tacticalExploitActive = false;
    SmartLeakProfile tacticalLeak = null;
    if (headsUpHand) {
      if (protectedMode && tacticalTagLagElite) {
        // Protected: Only vs confirmed TAGs/LAGs in HU
        tacticalExploitActive = true;
        for (PokerPlayer pr : players) {
          if (pr == null || pr == this || !pr.inHand()) continue;
          tacticalLeak = getSmartLeakDB().get(pr.getName());
          break;
        }
      } else if (!protectedMode && (tacticalTagLagElite || smartHuExploitMode)) {
        // Unprotected: vs TAG/LAGs or Smart Bots
        tacticalExploitActive = true;
        tacticalLeak = (smartHuLeak != null) ? smartHuLeak : null;
        if (tacticalLeak == null) {
          for (PokerPlayer pr : players) {
            if (pr == null || pr == this || !pr.inHand()) continue;
            tacticalLeak = getSmartLeakDB().get(pr.getName());
            break;
          }
        }
      }
    }
    boolean dryBoard = isDryBoard(board);

    boolean isNightmareMode = false;
    for (PokerPlayer pr : players)
      if (pr != null && "edjiang1234".equalsIgnoreCase(pr.getName()))
        isNightmareMode = true;
    boolean isGB = (isNightmareMode && predatoryIntent && !protectedMode);
    boolean isGS = (isNightmareMode && !predatoryIntent && !protectedMode);

    boolean isThinkingOpponentOnly = (!pureProtectedGtoMode && scriptedDumbBotCount == 0 && scriptedSmartBotCount == 0
                                       && scriptedArchetypeBotCount == 0);
    boolean isBeingBullied = (isThinkingOpponentOnly && depthRatio < 0.5 && (!protectedMode || neuralSandbox));
    if (smartDeficitRecovery) {
      isBeingBullied = true;
    }
    boolean fishPredatoryMode = (headsUpHand && scriptedDumbBotCount > 0 && !protectedMode);
    boolean baselineNightmarePredatoryMode = (headsUpHand && isNightmareMode && predatoryIntent && !protectedMode);
    // Intensity 2+ unlocks adaptive pressure in God-only small fields (2-4 active
    // players).
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

    // ============================================================================
    // ARCHETYPE HARD COUNTERS — analytical responses derived from Arc-bot source.
    // Arc-bots are deterministic state machines (archetypePostflop, lines ~3120-3282).
    // For each archetype we compute the exact +EV response analytically rather than
    // approximating via cascade heuristics. Bypasses the GTO cascade entirely.
    //
    //   TAG    → bets ⇔ hitPair, calls any size w/ pair, folds w/o pair
    //   LAG    → 70% pot ⇔ pair OR Q+ in hole, 50% pot ⇔ neither;
    //            45% check-raise w/ strongHand (top pair hi≥10 or overpair)
    //   BULLY  → always overbets 120% pot when checked; 50/50 re-raise vs call
    //   SS     → ALWAYS shoves entire stack postflop
    //   NIT    → fit-or-fold (kept from prior offensive overrides)
    //   MANIAC → always bets, check-raises 30%; (kept from prior offensive overrides)
    //
    // myRank: 1=straight flush ... 5=straight, 6=trips, 7=two pair, 8=one pair, 9=high card
    // HU postflop: BB acts first, SB acts last. zeroBet & SB ⇒ BB checked.
    // ============================================================================
    boolean archetypeFinal  = false;
    boolean iAmSb           = (seatIndex == sbIdx);
    boolean iAmBb           = (seatIndex == bbIdx);
    boolean opponentChecked = zeroBet && iAmSb;
    boolean iActFirst       = zeroBet && iAmBb;
    boolean isFlop          = (board.length == 3);
    boolean isRiver         = (board.length == 5);

    if (headsUpHand && exploitTag) {
      // TAG bets ⇔ pair, checks ⇔ no pair. Never bluffs.
      if (!zeroBet) {
        if (myRank <= 7) {
          // Set/two-pair+ beats TAG's pair. TAG calls any size → raise big.
          act = 3; actAmount = bet * 3;
          decisionReason = "TAG counter: raise vs TAG bet (we beat top pair)";
        } else if (myRank == 8) {
          // Pair vs pair. Marginal. Call for showdown.
          act = 1; actAmount = bet - prevBet;
          decisionReason = "TAG counter: call w/ pair (showdown)";
        } else {
          // Air vs pair. TAG never bluffs. FOLD.
          act = 2; actAmount = 0;
          decisionReason = "TAG counter: fold air (TAG never bluffs)";
        }
      } else if (opponentChecked) {
        // TAG checked → no pair. Bet → TAG folds 100%.
        act = 3; actAmount = Math.max(blind, (int)(potSize * 0.40));
        if (isFlop) cbetFlop = true;
        decisionReason = "TAG counter: bet vs check (TAG no pair, auto-folds)";
      } else {
        // We act first (BB). Bet two-pair+ for value; check otherwise.
        if (myRank <= 7) {
          act = 3; actAmount = (int)(potSize * 0.55);
          decisionReason = "TAG counter: BB lead value (two-pair+)";
        } else {
          act = 1; actAmount = 0;
          decisionReason = "TAG counter: BB check (let TAG reveal pair/no-pair)";
        }
      }
      archetypeFinal = true;

    } else if (headsUpHand && exploitLag) {
      // LAG sizing tells: 50% pot=weak, 70% pot=strong. CR 45% w/ strongHand.
      // Raise wide (two-pair+) — LAG calls light, gets max value. Raise gets check-raised
      // ~5% (45% × ~10% strongHand range) — acceptable cost for value extraction.
      if (!zeroBet) {
        double betRatio = (potSize > 0) ? (double)(bet - prevBet) / potSize : 0.0;
        if (betRatio <= 0.55) {
          if (myRank <= 7) {
            act = 3; actAmount = bet * 3;
            decisionReason = "LAG counter: raise vs small bet (LAG range weak)";
          } else if (myRank == 8 || (draws[0] || draws[1])) {
            act = 1; actAmount = bet - prevBet;
            decisionReason = "LAG counter: call vs small bet (pair/draw)";
          } else {
            act = 2; actAmount = 0;
            decisionReason = "LAG counter: fold air vs small bet";
          }
        } else {
          if (myRank <= 7) {
            act = 3; actAmount = bet * 3;
            decisionReason = "LAG counter: raise two-pair+ vs big bet";
          } else if (myRank == 8) {
            act = 1; actAmount = bet - prevBet;
            decisionReason = "LAG counter: call pair vs big bet";
          } else if ((draws[0] || draws[1]) && equity > potOdds * 1.20) {
            act = 1; actAmount = bet - prevBet;
            decisionReason = "LAG counter: call draw w/ overlay";
          } else {
            act = 2; actAmount = 0;
            decisionReason = "LAG counter: fold air vs big bet";
          }
        }
      } else if (opponentChecked) {
        // LAG checked despite high bet freq → range capped weak.
        if (myRank <= 7) {
          act = 3; actAmount = (int)(potSize * 0.60);
          decisionReason = "LAG counter: value bet vs check (two-pair+)";
        } else if (myRank == 8) {
          act = 3; actAmount = (int)(potSize * 0.40);
          decisionReason = "LAG counter: thin value bet pair vs check";
        } else {
          // LAG check range mostly weak. Bluff small.
          act = 3; actAmount = Math.max(blind, (int)(potSize * 0.33));
          if (isFlop) cbetFlop = true;
          decisionReason = "LAG counter: bluff vs check (LAG check ⇒ weak)";
        }
      } else {
        // We act first (BB). Lead w/ monster, check otherwise.
        if (myRank <= 5) {
          act = 3; actAmount = (int)(potSize * 0.65);
          decisionReason = "LAG counter: BB lead w/ strong (LAG calls wide)";
        } else {
          act = 1; actAmount = 0;
          decisionReason = "LAG counter: BB check (let LAG c-bet, react to sizing)";
        }
      }
      archetypeFinal = true;

    } else if (headsUpHand && exploitBully) {
      // BULLY auto-bets 120% pot. 50% re-raise vs our bet. Range = unfiltered.
      __DBG_BULLY_TOTAL.incrementAndGet();
      if (!zeroBet) {
        __DBG_FACING_BET_RAISES.incrementAndGet(); // count facing-bet entries (rename later)
        // Pot odds for 120% pot bet: 1.20/3.20 = 37.5%. Pair has ~55%, air ~30%.
        if (myRank <= 7) {
          // Two-pair+. Raise. BULLY 50% re-raises = builds pot for our value.
          act = 3; actAmount = bet * 3;
          __DBG_BULLY_RAISE.incrementAndGet();
          decisionReason = "BULLY counter: raise w/ two-pair+ (BULLY 50% re-raises)";
        } else if (myRank == 8) {
          // Pair: just call (showdown vs BULLY's random range).
          act = 1; actAmount = bet - prevBet;
          __DBG_BULLY_CALL.incrementAndGet();
          decisionReason = "BULLY counter: call w/ pair (showdown vs random)";
        } else if ((draws[0] || draws[1]) && equity >= 0.30) {
          act = 1; actAmount = bet - prevBet;
          __DBG_BULLY_CALL.incrementAndGet();
          decisionReason = "BULLY counter: call w/ draw (pot odds)";
        } else {
          act = 2; actAmount = 0;
          __DBG_BULLY_FOLD.incrementAndGet();
          decisionReason = "BULLY counter: fold air (need 37.5%, have <30%)";
        }
      } else if (opponentChecked) {
        // BULLY almost never checks. When they do, take it.
        act = 3; actAmount = Math.max(blind, (int)(potSize * 0.70));
        if (isFlop) cbetFlop = true;
        decisionReason = "BULLY counter: bet vs rare check";
      } else {
        // We act first (BB). BULLY auto-bets next.
        if (myRank <= 5) {
          // Monster: lead big (BULLY 50% re-raises).
          act = 3; actAmount = (int)(potSize * 0.75);
          decisionReason = "BULLY counter: BB lead monster (induce re-raise)";
        } else {
          // Check: let BULLY auto-bet, then we react.
          act = 1; actAmount = 0;
          decisionReason = "BULLY counter: BB check (let BULLY auto-bet)";
        }
      }
      archetypeFinal = true;

    } else if (headsUpHand && exploitShortStacker) {
      // SS always shoves any postflop decision. Range = any 2 cards.
      if (!zeroBet) {
        double potOddsRequired = (potSize + costToCall > 0)
            ? (double)costToCall / (potSize + costToCall) : 0.5;
        if (myRank <= 8) {
          // Pair+ has +EV vs random hand at typical pot odds.
          act = 1; actAmount = bet - prevBet;
          decisionReason = "SS counter: call shove (pair+ vs random range)";
        } else if ((draws[0] || draws[1]) && equity > potOddsRequired) {
          act = 1; actAmount = bet - prevBet;
          decisionReason = "SS counter: call draw (pot odds)";
        } else {
          act = 2; actAmount = 0;
          decisionReason = "SS counter: fold air vs shove";
        }
      } else {
        // SS won't check postflop (always shoves). zeroBet ⇒ first action of street.
        if (myRank <= 5) {
          // Monster: lead — SS will shove over for max value.
          act = 3; actAmount = (int)(potSize * 0.50);
          decisionReason = "SS counter: lead value (induce shove)";
        } else {
          // Check: avoid building pot vs imminent SS shove.
          act = 1; actAmount = 0;
          decisionReason = "SS counter: check (avoid building pot)";
        }
      }
      archetypeFinal = true;

    } else if (headsUpHand && (exploitNit || exploitManiac)) {
      // NIT/MANIAC kept from prior offensive overrides — already verified +EV.
      boolean twoPairPlus = (myRank <= 7);
      boolean canStealBet = isFlop && !cbetFlop;
      if (zeroBet) {
        if (exploitNit) {
          if (canStealBet) {
            act = 3; actAmount = Math.max(blind, (int)(potSize * 0.50));
            cbetFlop = true;
            decisionReason = "vs NIT: flop c-bet steal";
            archetypeFinal = true;
          } else if (twoPairPlus) {
            act = 3; actAmount = Math.max(blind, (int)(potSize * 0.55));
            decisionReason = "vs NIT: value bet turn/river";
            archetypeFinal = true;
          } else {
            act = 1; actAmount = 0;
            decisionReason = "vs NIT: check turn/river (NIT called flop = has pair)";
            archetypeFinal = true;
          }
        } else if (exploitManiac) {
          if (opponentChecked) {
            act = 3; actAmount = Math.max(blind, (int)(potSize * 0.60));
            if (isFlop) cbetFlop = true;
            decisionReason = "vs MANIAC: SB bet vs rare check";
            archetypeFinal = true;
          } else if (iActFirst) {
            act = 1; actAmount = 0;
            decisionReason = "vs MANIAC: BB check to induce";
            archetypeFinal = true;
          }
        }
      }
    }

    // Stack-clip safety on hard-counter overbets.
    if (archetypeFinal && act == 3) {
      if (actAmount > super.getChips()) actAmount = super.getChips();
      if (actAmount < 0) actAmount = 0;
    }

    if (!archetypeFinal) {
    if (soulReadSmartOverbet && myRank > 7) {
      decisionReason = "soul-read overbet from smart bot implies value-heavy range => fold bluff-catchers";
      act = 2; // Preserve strong made hands (straight+) while folding medium bluff-catchers.
    } else if (soulReadSmartMixed && headsUpHand && myRank >= 6 && myRank <= 8 && !zeroBet) {
      // SubAction-1 sizing is mixed value/bluff; in HU this lane should be
      // call-first, not auto-raise.
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
    } else if (nutBlocker && isThinkingOpponentOnly && !(exploitTag || exploitLag || exploitBully)) {
      // Nut-flush-blocker jam: don't bluff-jam vs barrel-heavy/calling archetypes.
      // TAG calls pair+ regardless of size; LAG calls wide; BULLY 50% re-raises.
      // In Pure GTO this branch is gated off by isThinkingOpponentOnly=false anyway.
      decisionReason = "nut-blocker bluff lane on scare texture";
      act = 3;
      actAmount = exploitingSmartBot ? (int) bluffSizeVsSmart : Math.max(potSize, super.getChips());
    } else if (smartHuExploitMode && board.length == 4 && zeroBet && smartHuLeak.checkBackTurnHUEMA >= 0.55
        && myRank <= 8) {
      // Smart check-back tendency on turn gets punished by proactive probing.
      decisionReason = "smart-HU turn probe punish after high check-back tendency";
      act = 3;
      actAmount = (int) Math.max(blind, potSize * (smartDeficitRecovery ? 0.70 : 0.45));
    } else if (board.length >= 5 && myRank >= boardRank && p.compareHands(fullHand, bestBoard) == 0) {
      decisionReason = "board-lock scenario: protect against chop disasters";
      // Defensive Awareness: If board is terrifying and we just have a pair, fold to
      // big pressure
      // SPICY OVERRIDE: If being bullied, stay suspicious and don't fold Top Pairs or
      // better
      if (!zeroBet && (flushScare || straightScare) && costToCall > potSize * 0.5 && myRank > 5 && !isBeingBullied) {
        if (maxHumanAggression > 0.40 && (myRank <= 7 || isGB)) {
          // PHASE 5: Maniac exploit - Snap call their scare card bluff with Top/Middle
          // pair
          decisionReason = "board-lock + maniac exploit => bluff-catch call";
          act = 1;
          actAmount = bet - prevBet;
        } else {
          decisionReason = "board-lock + heavy pressure => fold";
          act = 2;
        }
      } else {
        if (zeroBet) {
          decisionReason = "board-lock with no bet => check";
          act = 1;
        } else {
          if (isBeingBullied && myRank <= 8) {
            act = 1;
            actAmount = bet - prevBet;
          } else
            act = 2;
          if (isBeingBullied && myRank <= 8)
            decisionReason = "board-lock + bullied => hero-call";
          else
            decisionReason = "board-lock fallback fold";
        }
      }
    } else if (myRank <= 3 || (isBeingBullied && myRank <= 8) || (maxHumanAggression > 0.40 && myRank <= 6)) {
      decisionReason = "value/trap engine for strong made hands";
      // Trapping Logic (Slow-play): 20% chance to check/call monster hands to bait
      // bluffs - SPICY MODE ONLY
      double trapFreq = isGB ? 0.10 : (isGS ? 0.40 : 0.20);
      if (exploitManiac)
        trapFreq = 0.0; // vs MANIAC: NEVER trap. MANIAC pays off bets — trapping forfeits 2+ streets of value
                        // and Neural mode (protected) can't trap at all (gate at 2512), so matching that.
      else if (exploitBully && EXPLOIT_BULLY_CHECK_RAISE_TOP_PAIR)
        trapFreq = Math.min(0.80, trapFreq + (0.40 * postExploitStrength)); // vs BULLY: maximize trapping → CR plan: BULLY auto-bets, we raise from check
      else if (exploitStation)
        trapFreq = Math.max(0.02, trapFreq - (0.15 * postExploitStrength)); // max value, no trapping
      else if (exploitNit)
        trapFreq = Math.max(0.02, trapFreq - (0.05 * postExploitStrength));
      if (gtoFallback)
        trapFreq = 0.20;
      if (smartHuExploitMode)
        trapFreq = smartDeficitRecovery ? 0.03 : Math.min(trapFreq, 0.08);
      boolean trapMode = (!protectedMode && isThinkingOpponentOnly && Math.random() < trapFreq
          && (board.length == 3 || board.length == 4));

      if (scriptedDumbBotCount > 0 && scriptedDumbBotCount == (activeCount - 1) && largestDumbStack > 0 && !protectedMode) {
        decisionReason = "minus-one exploit: homogeneous dumb bot pot";
        act = 3;
        actAmount = Math.max(potSize, largestDumbStack - 1);
        minusOneActive = true;
      } else if (trapMode) {
        decisionReason = "trap mode selected (slow-play)";
        if (zeroBet)
          act = 1;
        else {
          act = 1;
          actAmount = bet - prevBet;
        }
      } else {
        // Iron Chin: Snap-calling bullies with Hero Ranges (Pairs or better)
        if (isBeingBullied && !zeroBet) {
          decisionReason = "bullied defense call";
          act = 1;
          actAmount = bet - prevBet;
        } else {
          double valueMult = (depthRatio > 1.5 && !protectedMode) ? 1.0 : 0.75;
          if (exploitStation) valueMult += (0.15 * postExploitStrength);
          else if (exploitNit) valueMult -= (0.10 * postExploitStrength);
          else if (exploitManiac) valueMult += (0.05 * postExploitStrength);
          else if (exploitStation && postExploitStrength > 0) {
            // STATION (includes former WHALE/FISH): inflate value sizing — inelastic to size
            double stationMult = 1.0 + (0.5 * postExploitStrength);
            if (postIsHomogeneous && activeCount > 2) stationMult += (0.25 * (activeCount - 2));
            valueMult = Math.min(2.0, stationMult);
            decisionReason = "station overbet value: " + (int)(valueMult * 100) + "% pot";
          }
          if (smartHuExploitMode) {
            if (smartDeficitRecovery) valueMult += 0.15;
            if (smartLockdown) valueMult -= 0.10;
          }
          if (gtoFallback) valueMult = Math.min(valueMult, 0.85);
          valueMult = Math.max(0.40, Math.min(2.0, valueMult));
          if (zeroBet) {
            decisionReason = "strong hand value lead with dynamic multiplier=" + valueMult;
            act = 3;
            actAmount = (int) (potSize * valueMult);
          } else {
            decisionReason = "strong hand value raise versus bet";
            act = 3;
            actAmount = bet * 3;
          }
        }
      }
    } else if (myRank <= (headsUpHand ? 8 : 7) && scriptedDumbBotCount > 0 && !draws[0] && !draws[1]) {
      decisionReason = "thin value versus dumb bot range";
      // Hyper-Thin Value Betting vs Dumb Bots (Isolated 1v1 expands threshold to Any
      // Pair)
      if (zeroBet) {
        act = 3;
        actAmount = potSize;
      } else {
        act = 3;
        actAmount = Math.max(potSize, bet * 2);
      }
    } else if (myRank <= 5) {
      decisionReason = "standard value extraction lane";
      // PHASE 7: Multiway Value Extraction - Charge a premium against multiple
      // players to protect strong hands
      double valueSize = (activeCount >= 3) ? 0.85 : 0.5;
      if (exploitStation) valueSize *= (1.0 + (0.20 * postExploitStrength));
      else if (exploitNit) valueSize *= (1.0 - (0.10 * postExploitStrength));
      else if (exploitManiac) valueSize *= (1.0 + (0.10 * postExploitStrength));
      else if (exploitStation && postExploitStrength > 0)
        valueSize = (1.0 + (0.5 * postExploitStrength)) + (activeCount > 2 ? 0.25 * (activeCount - 2) : 0);
      if (smartHuExploitMode) {
        if (smartDeficitRecovery) valueSize += 0.20;
        if (smartLockdown) valueSize += 0.05;
      }
      if (gtoFallback) valueSize = (activeCount >= 3) ? 0.80 : 0.50;
      valueSize = Math.max(0.35, Math.min(2.0, valueSize));
      if (zeroBet) {
        act = 3;
        actAmount = (int) (potSize * valueSize);
      } else {
        if (costToCall > super.getChips() * 0.5) {
          act = 1;
          actAmount = bet - prevBet;
        } else {
          act = 3;
          actAmount = bet * 3;
        }
        if (costToCall > super.getChips() * 0.5)
          decisionReason = "value hand but high call cost => control via call";
        else
          decisionReason = "value hand raise for protection";
      }
    } else if (myRank <= 8 || draws[0] || draws[1]) {
      decisionReason = "draw/marginal hand semi-bluff and c-bet engine";
      // GTO Hardening: Semi-Bluffing (40% chance to lead draws aggressively) - SPICY
      // MODE ONLY
      boolean semiBluff = (isThinkingOpponentOnly && (draws[0] || draws[1]) && Math.random() < 0.40);

      // Elite Range Advantage: Ace-high boards C-bet 90% of the time
      double cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
      double barrelFreq = 0.70;

      if (exploitNit) {
        cbetFreq = postExploitStrength > 0 ? 1.0 : cbetFreq; // Infinite steal vs NITs
        barrelFreq = postExploitStrength > 0 ? Math.max(barrelFreq, 0.90) : barrelFreq;
      } else if (exploitManiac) {
        cbetFreq *= Math.max(0.40, 1.0 - (0.35 * postExploitStrength)); // MANIAC: check and let them barrel
        barrelFreq *= Math.max(0.30, 1.0 - (0.40 * postExploitStrength));
      } else if (exploitBully && EXPLOIT_BULLY_NEVER_CBET) {
        // BULLY auto-bets every flop. Never c-bet — let BULLY barrel into us.
        cbetFreq = 0.0;
        barrelFreq = 0.0;
      } else if (exploitStation && postExploitStrength > 0) {
        cbetFreq = 0.0; // No bluffing stations (former WHALE/FISH/STATION)
        barrelFreq = 0.0;
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

      // PHASE 11: Tactical Range Exploit — modest bluff bump on dry boards when TAG/Smart checks
      if (tacticalExploitActive && dryBoard && zeroBet && tacticalLeak != null) {
        double flopFoldRate = tacticalLeak.foldToFlopCbetHUEMA;
        double turnFoldRate = tacticalLeak.foldToTurnBarrelHUEMA;
        // Only exploit if leak data confirms they over-fold (above neutral 0.50 baseline)
        if (board.length == 3 && flopFoldRate >= 0.52) {
          double bump = Math.min(0.15, (flopFoldRate - 0.50) * 0.30); // modest 0-15% bump
          cbetFreq += bump;
          decisionReason += " | tactical dry-board exploit +" + String.format("%.0f", bump * 100) + "% cbet";
          trace("GOD-POSTFLOP", "PHASE11 tactical exploit: dryBoard=true, flopFoldRate=" + flopFoldRate
              + ", cbetBump=" + bump + ", tacticalActive=true");
        } else if (board.length == 4 && turnFoldRate >= 0.52) {
          double bump = Math.min(0.15, (turnFoldRate - 0.50) * 0.30); // modest 0-15% bump
          barrelFreq += bump;
          decisionReason += " | tactical dry-board barrel exploit +" + String.format("%.0f", bump * 100) + "%";
          trace("GOD-POSTFLOP", "PHASE11 tactical exploit: dryBoard=true, turnFoldRate=" + turnFoldRate
              + ", barrelBump=" + bump + ", tacticalActive=true");
        }
      }

      // PHASE 7: Multiway Respect - Slash bluffing/barreling frequencies into
      // multiple opponents
      if (activeCount >= 3) {
        cbetFreq *= 0.50; // Cut bluffs in half vs 3+ players
        barrelFreq *= 0.40; // Double barrel is even rarer
      }

      // PHASE 4: G-B vs G-S Baseline Aggression Skew
      if (isGB) {
        cbetFreq += 0.15;
        barrelFreq += 0.15;
      }
      if (isGS) {
        cbetFreq -= 0.15;
        barrelFreq -= 0.15;
      }

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

      if (exploitNit && cbet) {
        cbetFreq = 1.0;
        if (postExploitStrength <= 0) barrelFreq = 0.0; // NEVER double barrel air against a tight player who called flop!
      }
      // LEAK PLUG attempt (reverted): "suppress flop c-bet with air vs LAG/BULLY"
      // tested at 10k pairs — Neural -62 vs Pure -36 for LAG (regression −26 BB/100).
      // The mid-street fold avg is real but represents the NATURAL cost of +EV c-bet
      // strategy, not a fixable leak. Suppressing c-bets removes the wins from fold-outs.
      if (exploitStation && cbet)
        cbetFreq = 0.0;
      if (gtoFallback && cbet)
        cbetFreq = Math.max(0.45, Math.min(0.80, cbetFreq));

      // PHASE 7: Multiway C-Bet Sizing (Down-size C-bets in multiway pots to risk
      // less capital per bluff)
      double cbetSize = (activeCount >= 3) ? 0.30 : 0.40;
      if (exploitNit)
        cbetSize *= 1.10;
      else if (exploitStation)
        cbetSize *= 0.75;
      else if (exploitManiac)
        cbetSize *= 0.85;
      if (gtoFallback)
        cbetSize = (activeCount >= 3) ? 0.30 : 0.40;
      if (smartHuExploitMode) {
        if (smartDeficitRecovery)
          cbetSize *= 1.25;
        else if (smartLockdown)
          cbetSize *= 0.90;
      }
      cbetSize = Math.max(0.20, Math.min(0.60, cbetSize));

      if (zeroBet && cbet && Math.random() < cbetFreq) {
        decisionReason = "c-bet fired with tuned frequency=" + cbetFreq;
        act = 3;
        actAmount = (int) (Math.max(potSize * cbetSize, blind));
        if (board.length == 3)
          cbetFlop = true;
      } else if (zeroBet && semiBluff) {
        decisionReason = "semi-bluff lead on draw";
        act = 3;
        actAmount = (int) (potSize * 0.75); // Lead aggressively on semi-bluff
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < barrelFreq) {
        // Triple Barreling: Turn is a face card and we C-bet flop
        decisionReason = "turn barrel after flop c-bet on favorable overcard";
        act = 3;
        actAmount = (int) (potSize * 0.6);
      } else if (!zeroBet && semiBluff && !(exploitTag || exploitLag)) {
        decisionReason = "semi-bluff raise facing bet";
        act = 3;
        actAmount = bet * 3; // Aggressive Raise on semi-bluff (suppressed vs TAG/LAG: they call)
      } else if (!zeroBet && (draws[0] || draws[1])
          && equity > potOdds * ((exploitTag || exploitLag) ? 1.30 : 1.00)) {
        // Vs barrel-heavy archetypes (TAG/LAG), require a 30% equity overlay before
        // calling draws — they will fire turn AND river ~80% so weak draws bleed.
        decisionReason = "draw call justified by equity>potOdds";
        act = 1;
        actAmount = bet - prevBet;
      } else {
        // PHASE 6: Scare-Board Float & Bluff-Catch Logic (REVISED)
        // If you're aggressively bluffing scare cards, they will float/call to induce
        // you
        // BUT ONLY if conditions are mathematically sound (hand equity, bet size,
        // frequency cap)
        double floatFreq = 0.50; // Increased to 50% frequency cap
        double requiredEquity = (costToCall > 0) ? costToCall / (double) (potSize + costToCall) : 0.0;
        boolean isScareBoardBluff = (maxHumanAggression > 0.40 && !zeroBet && (flushScare || straightScare)
            && myRank > 7 && myRank <= 9 && Math.random() < floatFreq && equity > requiredEquity * 0.9);
        if (exploitNit) isScareBoardBluff = false; // Never bluff catch a NIT

        // GTO BASELINE — hand-strength-graded Minimum Defense Frequency.
        //
        // Rationale: real GTO defense isn't a single fold rate. It's a continuum keyed
        // to how strong our hand is relative to opponent's range. Strong hands always
        // call. Pair calls near MDF (the bluff-breakeven). Air folds heavy.
        //
        //   MDF = costToCall / potSize  (the bluff-breakeven fold rate)
        //
        // Tier behavior (HU):
        //   myRank ≤ 5 (two-pair+):  0% fold — never give up value
        //   myRank ≤ 7 (set-ish):    half-MDF, capped 20% — bluff-catch most spots
        //   myRank = 8 (one pair):   MDF, capped 50% — call near breakeven
        //   draws (≥ 15% equity):   pot-odds gated (call if equity > potOdds)
        //   myRank = 9 (high card):  max(MDF, 0.85) — fold heavy, no showdown value
        //
        // Multiway: keep the conservative legacy rate (true MDF requires modeling N opps).
        double huFoldChance;
        if (headsUpHand) {
          double mdf = (costToCall > 0 && potSize > 0)
              ? Math.min(0.95, costToCall / (double) potSize)
              : 0.0;
          boolean hasDraw = draws[0] || draws[1];
          if (myRank <= 5) {
            huFoldChance = 0.0;
          } else if (myRank <= 7) {
            huFoldChance = Math.min(0.15, mdf * 0.4);
          } else if (myRank == 8) {
            huFoldChance = Math.min(0.40, mdf);
          } else if (hasDraw && equity > 0.15) {
            huFoldChance = (equity > potOdds) ? 0.0 : Math.min(0.80, mdf);
          } else {
            // High card no draw — let MDF handle it. Calling some bluffs is +EV vs
            // bluff-heavy opponents; folding at MDF is breakeven vs balanced range.
            huFoldChance = mdf;
          }
        } else {
          huFoldChance = gtoFallback ? 0.82 : 0.85;
        }
        if (exploitNit)
          huFoldChance = Math.min(1.0, huFoldChance + 0.15);
        if (exploitStation)
          huFoldChance = Math.min(1.0, huFoldChance + 0.05);
        if (exploitManiac)
          huFoldChance = Math.max(0.0, huFoldChance - 0.20);
        if (exploitBully && EXPLOIT_BULLY_FOLD_AIR) {
          // BULLY auto-bets 120% with everything. Air = 0% equity, fold instant.
          // Pair+ = call down (BULLY's bet range is uniform; pair has solid showdown).
          if (myRank > 8) {
            huFoldChance = 1.00;
          } else {
            huFoldChance = 0.00;
          }
        }
        if (exploitTag && EXPLOIT_TAG_FOLD_TO_BET) {
          // TAG bets only with pair+ (never bluffs). Air folds 100%.
          // Pair vs TAG bet: usually behind (TAG's pair likely beats ours since TAG bets only
          // top-pair-good-kicker). Tight call only with two-pair+.
          if (myRank > 8) huFoldChance = 1.00;
          else if (myRank == 8) huFoldChance = Math.min(1.0, huFoldChance + 0.30);
        }
        // LAG huFoldChance block removed — both branches found to regress at 50k pairs.
        // LAG resists narrow exploits; Pure GTO baseline outperforms.
        if (smartHuExploitMode) {
          huFoldChance = smartDeficitRecovery ? 0.12 : 0.22;
          huFoldChance -= Math.max(0.0, smartHuLeak.raiseVsCbetHUEMA - 0.35) * 0.10;
          huFoldChance = Math.max(0.05, Math.min(0.80, huFoldChance));
        }
        if (isGB && headsUpHand)
          huFoldChance = 0.00; // G-B never folds 1v1 to generic pressure here
        if (isGS && headsUpHand)
          huFoldChance = 0.50; // G-S respects pressure more

        // PHASE 5: Float pairs/draws into aggression on flop/turn.
        // TAG/LAG/BULLY exploits above already set huFoldChance correctly per archetype;
        // skip float overrides when an archetype exploit is active (they take priority).
        if (maxHumanAggression > 0.40 && !exploitTag && !exploitLag && !exploitBully) {
          if (myRank <= 7)
            huFoldChance = 0.00; // Two pair+ never fold
          else if (myRank <= 9 && board.length < 5)
            huFoldChance = 0.00; // Float pairs pre-river
          else if ((draws[0] || draws[1]) && board.length < 5 && equity > 0.15)
            huFoldChance = 0.00; // Float draws
        }

        // HARD FLOOR: Do not punt with Ace-high (rank 9) on the river against any archetype.
        if (board.length == 5 && myRank >= 9 && !isGB) {
          huFoldChance = Math.max(0.95, huFoldChance);
        }

        // PHASE 6: Bluff-Catching on Scare Boards - Float instead of fold (Tightened)
        if (isScareBoardBluff) {
          // Dynamic Response: If bet is small (<50% pot), occasionally
          // check-raise/re-raise to punish bluffs
          double betRatio = (double) (bet - prevBet) / Math.max(1, potSize);
          if (betRatio < 0.50 && Math.random() < 0.60) {
            decisionReason = "scare-board bluff catch converted to punish raise";
            act = 3;
            actAmount = Math.max(bet * 3, potSize); // Re-raise the small bluff
          } else {
            decisionReason = "scare-board bluff catch via controlled call";
            act = 1;
            actAmount = bet - prevBet; // Float/call against bigger bets to control the pot
          }
        } else {
          double riskRatio = (super.getChips() > 0) ? costToCall / (double) super.getChips() : 1.0;
          double callProb = (!zeroBet && riskRatio < 0.20) ? (0.20 - riskRatio) * 5.0 : 0.0; // Phase 3: Smooth 0-100%
                                                                                             // call scale for micro
                                                                                             // bets

          double stabFreq = 0.0;
          if (exploitNit) stabFreq = 1.0; // Always stab NITs who check (they missed and never bluff)
          // TAG/LAG/BULLY stabs: removed during rebuild. TAG handled in PHASE 12 offensive
          // overrides (overbet bluff vs check). LAG check-raises 45% so stabs are net-negative.
          // BULLY auto-bets so stabs trigger immediate re-raise.

          // LAG sizing-tell raise: when LAG bets ≤55% pot (bluff frequency tell), raise to 3x.
          double lagBetRatio = (potSize > 0 && !zeroBet) ? (double)(bet - prevBet) / potSize : 0.0;
          boolean lagRaiseSpot = exploitLag && EXPLOIT_LAG_3BET_SMALL_BET
              && lagBetRatio > 0 && lagBetRatio <= 0.55;
          // BULLY check-raise: when facing BULLY's auto-c-bet with top-pair+, raise 3x.
          boolean bullyCRSpot = exploitBully && EXPLOIT_BULLY_CHECK_RAISE_TOP_PAIR
              && !zeroBet && myRank <= 8;

          if (zeroBet && Math.random() < stabFreq && postExploitStrength <= 0) {
            decisionReason = "archetype exploit: stab when checked to";
            act = 3;
            actAmount = Math.max(blind, potSize / 2);
          } else if (lagRaiseSpot) {
            decisionReason = "vs LAG: 3-bet small bet (sizing tell = bluff)";
            act = 3;
            actAmount = Math.max(bet * 3, (int)(potSize * 1.0));
          } else if (bullyCRSpot && myRank <= 7) {
            // Two-pair+ vs BULLY's c-bet: raise. Pair we just call (top pair shines on showdown,
            // raising puts us in BULLY's 50% re-raise range which is value-heavy).
            decisionReason = "vs BULLY: check-raise top-set+ on c-bet";
            act = 3;
            actAmount = bet * 3;
          } else if (zeroBet && headsUpHand && !cbet && (seatIndex == bbIdx)
                     && board.length == 3 && dryBoard
                     && (myRank <= 7 || (myRank == 9 && Math.random() < 0.15))) {
            // GTO Day-1 — Donk-bet on dry flops as BB defender.
            // BB has range advantage on dry low boards (SB folded its trash preflop, leaving
            // a balanced range; BB's defending range is similar but with more middle pairs).
            // Donk with value (myRank ≤ 7: trips/two-pair/pair) and a small bluff frequency
            // (15% high-card with backdoor potential — keeps range balanced).
            // Sizing: 33% pot — small enough to fold rarely-called, big enough to deny equity.
            decisionReason = "GTO donk-bet dry flop as BB (myRank=" + myRank + ")";
            act = 3;
            actAmount = (int) Math.max(blind, potSize * 0.33);
            cbetFlop = true; // claim initiative for turn barrel logic
          } else if (zeroBet) {
            decisionReason = "no-bet fallback check";
            act = 1;
          } else if (exploitNit && costToCall > 0 && myRank > 5 && equity < requiredEquity * 1.5) {
            decisionReason = "nit exploit: respect nit postflop bet and fold non-monster";
            act = 2;
          } else if (!zeroBet && costToCall > 0 && Math.random() < callProb) {
            decisionReason = "micro-bet exploit defense call";
            act = 1;
            actAmount = bet - prevBet;
          } else if (headsUpHand && Math.random() > huFoldChance) {
            decisionReason = "heads-up stickiness call";
            act = 1;
            actAmount = bet - prevBet;
          } else {
            decisionReason = "draw/marginal fallback fold";
            act = 2;
          }
        }
      }
    } else if (predatoryMode && myRank <= 10) {
      decisionReason = "predatory thin-value line (ace-high capable)";
      // Ultra-Thin Value: Betting Ace-High against Dumb Bots 1v1
      if (zeroBet) {
        act = 3;
        actAmount = (int) (potSize * 0.5);
      } else {
        act = 1;
        actAmount = bet - prevBet;
      }
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

      if (scriptedDumbBotCount > 0)
        cbetFreq = (headsUpHand) ? 0.60 : 0.0; // PREDATORY BLUFFING 1v1 vs Dumb Bots

      if (smartHuExploitMode) {
        cbetFreq += (smartHuLeak.foldToFlopCbetHUEMA - 0.50) * 0.35;
        if (smartDeficitRecovery)
          cbetFreq += 0.10;
        if (smartLockdown)
          cbetFreq -= 0.08;
      }

      if (exploitNit && cbet)
        cbetFreq = 1.0;
      else if (exploitStation && cbet)
        cbetFreq = 0.0;
      else if (exploitManiac && cbet)
        cbetFreq *= 0.80;
      else if (exploitBully && cbet)
        cbetFreq *= 1.0; // BULLY: standard air c-bet — they'll overbet if we check
      else if (gtoFallback && cbet)
        cbetFreq = Math.max(0.45, Math.min(0.80, cbetFreq));

      // PHASE 11: Tactical Range Exploit — air fallback dry-board bump
      if (tacticalExploitActive && dryBoard && zeroBet && cbet && tacticalLeak != null) {
        double flopFoldRate = tacticalLeak.foldToFlopCbetHUEMA;
        if (board.length == 3 && flopFoldRate >= 0.52) {
          double bump = Math.min(0.15, (flopFoldRate - 0.50) * 0.30);
          cbetFreq += bump;
          decisionReason = "tactical dry-board air exploit +" + String.format("%.0f", bump * 100) + "% cbet";
        }
      }

      if (zeroBet && cbet && Math.random() < cbetFreq) {
        decisionReason = "fallback c-bet fired with frequency=" + cbetFreq;
        act = 3;
        actAmount = (int) (Math.max(potSize * 0.4, blind));
        if (board.length == 3)
          cbetFlop = true;
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < 0.70) {
        // Triple Barreling
        decisionReason = "fallback turn barrel after flop c-bet";
        act = 3;
        actAmount = (int) (potSize * 0.6);
      } else {
        decisionReason = "fallback check/fold";
        act = zeroBet ? 1 : 2;
      }
    }
    } // end if (!archetypeFinal) — full GTO cascade is bypassed when archetype override fired

    // Meta-Bluffing Level 3: Overbetting to exploit other God Bots who respect math
    if (!protectedMode && godBotCount > 0 && scriptedDumbBotCount == 0 && Math.random() < 0.12 && act == 2) {
      decisionReason += " | meta-bluff override triggered";
      act = 3;
      actAmount = (int) (potSize * (1.5 + Math.random()));
    }

    if (smartHuExploitMode) {
      boolean weakOrAir = (myRank > 8 && !draws[0] && !draws[1]);
      if (!zeroBet && weakOrAir) {
        double callRisk = costToCall / Math.max(1.0, super.getChips());
        if (callRisk > 0.20) {
          // Smart bots earn most EV from trapping oversized hero calls; tighten this
          // lane.
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

    // Humanoid Noise: ±5 chips on postflop bets to look less robotic vs humans.
    // Skip vs X-Ray-confirmed Arc-bots — same reasoning as preflop noise gate.
    if (act == 3 && !minusOneActive && !postXRayActive) {
      actAmount += (int) (Math.random() * 11) - 5;
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
    if (act == 3 && actAmount <= bet)
      actAmount = bet + Math.max(lastRaise, blind); // Ensure legal raise (Official Increment Rule)
    if (act == 3 && actAmount == 0)
      actAmount = Math.max(lastRaise, blind);

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
    if (act == 4) {
      action[0] = 4;
      action[1] = super.getChips();
    } else if (act == 3) {
      action[0] = 3;
      action[1] = actAmount - prevBet;
    } else if (act == 2) {
      action[0] = zeroBet ? 1 : 2;
      action[1] = 0;
    } else {
      action[0] = 1;
      action[1] = zeroBet ? 0 : bet - prevBet;
    } // Standardized increment

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

  // ============================================================
  // PHASE 11: TACTICAL RANGE EXPLOIT HELPERS
  // ============================================================

  /**
   * Determines if a board texture is "dry" (rainbow, unpaired, disconnected).
   * A dry board favors range exploitation because strong draws are unlikely
   * and an opponent's check is more likely to indicate a missed hand.
   */
  private boolean isDryBoard(Card[] board) {
    if (board == null || board.length < 3) return false;

    // Check for paired board (any two cards share rank)
    for (int i = 0; i < board.length; i++) {
      for (int j = i + 1; j < board.length; j++) {
        if (board[i].getNum() == board[j].getNum()) return false; // paired = not dry
      }
    }

    // Check for flush potential (2+ cards of same suit = not dry)
    int[] suitCounts = new int[4];
    for (Card c : board) {
      String suit = c.getValue().substring(1);
      switch (suit) {
        case "♠️": suitCounts[0]++; break;
        case "♣️": suitCounts[1]++; break;
        case "♦️": suitCounts[2]++; break;
        case "♥️": suitCounts[3]++; break;
      }
    }
    for (int s : suitCounts) {
      if (s >= 2) return false; // flush draw possible = not dry
    }

    // Check for straight connectivity (3+ connected = not dry)
    int[] ranks = new int[board.length];
    for (int i = 0; i < board.length; i++) ranks[i] = board[i].getNum();
    java.util.Arrays.sort(ranks);
    int connected = 1;
    for (int i = 1; i < ranks.length; i++) {
      int gap = ranks[i] - ranks[i - 1];
      if (gap <= 2) { // within 2 ranks = connected
        connected++;
        if (connected >= 3) return false; // straight draw possible = not dry
      } else {
        connected = 1;
      }
    }

    return true; // rainbow, unpaired, disconnected = dry
  }

  // ============================================================
  // PHASE 10: ARCHETYPE BOT SCRIPTS (botLevel = 3)
  // ============================================================

  /**
   * Preflop decision engine for archetype bots.
   * Targets: NIT (~15% VPIP, 8% PFR), MANIAC (~60% VPIP, 35% PFR),
   *          STATION (~40% VPIP, 12% PFR), TAG (~22% VPIP, 18% PFR),
   *          WHALE (~55% VPIP, 10% PFR), FISH (~38% VPIP, 14% PFR),
   *          BULLY (~28% VPIP, 24% PFR), SHORT_STACKER (push/fold).
   */
  private int[] archetypePreflop(int prevBet, int bet, int blind, int lastRaise,
      PokerPlayer[] players, int seatIndex) {
    int[] action = new int[2];
    Card[] hole = super.getHand();
    int[] numhand = (hole != null) ? Deck.cardToInt(hole) : new int[]{2, 2};
    java.util.Arrays.sort(numhand);
    int hi = numhand[1];
    int lo = numhand[0];
    boolean paired = (hi == lo);
    boolean suited = (hole != null && hole[0].getValue().charAt(1) == hole[1].getValue().charAt(1));
    int gap        = hi - lo;
    boolean facing = (bet > blind);

    if (simulatedArchetype == null) {
      action[0] = 2; action[1] = 0; return action; // fold if unassigned
    }

    switch (simulatedArchetype) {

      case NIT: {
        // Premium-only: AA-99, AK, AQ, AJs+, KQs. 3-bet only AA/KK.
        boolean premium = paired && hi >= 9;
        boolean premiumSuited = suited && hi == 14 && lo >= 11;
        boolean premiumOff    = hi == 14 && lo >= 12;
        if (!premium && !premiumSuited && !premiumOff) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          if (paired && hi >= 13) { // AA or KK — 3-bet
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = bet - prevBet; } // flat call
        } else {
          int raiseTo = blind * 3;
          action[0] = 3; action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet; // open raise 3x
        }
        return action;
      }

      case MANIAC: {
        // Plays ~60% of hands. Raises or 3-bets constantly.
        double playFreq = 0.60;
        if (Math.random() > playFreq) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        action[0] = 3;
        int raiseTo = facing ? bet * 3 : blind * 3;
        action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
        return action;
      }

      case STATION: {
        // Limps ~40%+ of hands. Rarely raises; calls almost any raise.
        double limpFreq = 0.42;
        if (Math.random() > limpFreq) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          // Call any raise — stations don't fold to bets
          if (bet - prevBet <= super.getChips()) { action[0] = 1; action[1] = bet - prevBet; }
          else { action[0] = 4; action[1] = super.getChips(); }
        } else {
          // Limp (just call the big blind)
          action[0] = 1; action[1] = blind - prevBet;
        }
        return action;
      }

      case TAG: {
        // Broaden range to ensure VPIP remains > 18% so it avoids NIT classification
        boolean playable = (paired) || (hi == 14 && lo >= 6) || (hi >= 12 && lo >= 9) 
            || (suited && hi >= 8 && gap <= 4);
        if (!playable) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          boolean strongHand = (paired && hi >= 10) || (hi == 14 && lo >= 12) || (hi == 13 && lo >= 12);
          if (strongHand) {
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = bet - prevBet; }
        } else {
          int raiseTo = blind * 3;
          action[0] = 3; action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
        }
        return action;
      }

      case WHALE: {
        // Limps almost everything (~55%), calls any raise.
        if (Math.random() > 0.55) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          // Call any raise — inelastic to sizing
          if (bet - prevBet <= super.getChips()) { action[0] = 1; action[1] = bet - prevBet; }
          else { action[0] = 4; action[1] = super.getChips(); }
        } else {
          action[0] = 1; action[1] = blind - prevBet; // limp
        }
        return action;
      }

      case FISH: {
        // Limps wide (~38%), folds to re-raises.
        if (Math.random() > 0.38) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          // Fish occasionally call one raise, fold to large 3-bets
          boolean bigRaise = (bet > blind * 4);
          if (bigRaise) { action[0] = 2; action[1] = 0; }
          else { action[0] = 1; action[1] = bet - prevBet; }
        } else {
          action[0] = 1; action[1] = blind - prevBet; // limp
        }
        return action;
      }

      case BULLY: {
        // Wide and aggressive (~35-40%) to ensure it's classified properly and plays aggressively
        boolean playable = (paired) || (hi == 14) || (hi >= 12 && lo >= 8) 
            || (hi == 11 && lo >= 9) || (suited && gap <= 4);
        if (!playable) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; } // free check
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          // 3-bet aggressively with most playable hands
          double threeBet = 0.70;
          if (Math.random() < threeBet) {
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = bet - prevBet; }
        } else {
          int raiseTo = blind * 4;
          action[0] = 3; action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet; // open-raise 4x
        }
        return action;
      }

      case SHORT_STACKER: {
        // Push or fold: top 15% shoves, everything else folds.
        boolean shoveHand = paired || hi == 14 || (hi >= 12 && lo >= 9)
            || (suited && hi >= 13 && gap <= 1) || (hi >= 11 && lo >= 11);
        if (shoveHand) {
          action[0] = 4; action[1] = super.getChips();
        } else {
          action[0] = 2; action[1] = 0;
        }
        return action;
      }

      case LAG: {
        // Plays ~30-35% of hands. Raises aggressively, 3-bets balanced range.
        boolean playable = (paired) || (hi >= 13 && lo >= 6) || (hi >= 12 && lo >= 8)
            || (suited && hi >= 9 && gap <= 4) || (hi >= 11 && lo >= 9);
        if (!playable) {
          if (bet == prevBet) { action[0] = 1; action[1] = 0; return action; }
          action[0] = 2; action[1] = 0; return action;
        }
        if (facing) {
          boolean threeBetHand = (paired && hi >= 8) || (hi == 14 && lo >= 10) || (hi >= 13 && lo >= 11)
              || (suited && hi >= 12 && gap <= 2);
          double threeBetFreq = threeBetHand ? 0.65 : 0.20;
          if (Math.random() < threeBetFreq) {
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = bet - prevBet; }
        } else {
          int raiseTo = blind * 3;
          action[0] = 3; action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
        }
        return action;
      }

      default:
        action[0] = 2; action[1] = 0; return action;
    }
  }

  /**
   * Postflop decision engine for archetype bots.
   * Behaviorally accurate: targets the statistical profiles described in the Phase 10 plan.
   */
  private int[] archetypePostflop(int prevBet, int bet, int blind, int potSize,
      Card[] board, PokerPlayer[] players, int seatIndex) {
    int[] action = new int[2];
    if (simulatedArchetype == null) { action[0] = 2; action[1] = 0; return action; }

    // Basic hand strength heuristic using Card API
    Card[] hole = super.getHand();
    int[] numhand = (hole != null) ? Deck.cardToInt(hole) : new int[]{2, 2};
    java.util.Arrays.sort(numhand);
    int hi = numhand[1];
    int lo = numhand[0];
    boolean paired = (hi == lo);
    int topBoard = 0;
    for (Card c : board) if (c.getNum() > topBoard) topBoard = c.getNum();
    boolean hitTopPair = (hi == topBoard || lo == topBoard);
    boolean hitPair    = paired || hitTopPair;
    boolean facingBet  = (bet > prevBet);
    int callAmt        = bet - prevBet;

    switch (simulatedArchetype) {

      case NIT: {
        // Fit-or-fold. Fold to any bet if missed. Call if hit (rarely raise).
        if (facingBet) {
          if (!hitPair) { action[0] = 2; action[1] = 0; } // fold if missed
          else { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); } // call if hit
        } else {
          action[0] = 0; action[1] = 0; // check
        }
        return action;
      }

      case MANIAC: {
        // C-bet 100%, barrel every street. Check-raise ~30% when facing a bet.
        if (facingBet) {
          double checkRaiseFreq = 0.30;
          if (Math.random() < checkRaiseFreq) {
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); }
        } else {
          // Always bet (c-bet / barrel)
          action[0] = 3;
          int betAmt = Math.max(blind, potSize / 2);
          action[1] = Math.min(betAmt, super.getChips());
        }
        return action;
      }

      case STATION: {
        // Never folds pairs or draws. Never raises.
        if (facingBet) {
          action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); // always call
        } else {
          action[0] = 0; action[1] = 0; // check
        }
        return action;
      }

      case TAG: {
        // Solid play. Bet for value if hit, fold to massive overbets if missed.
        if (facingBet) {
          boolean massiveOverbet = (callAmt > potSize);
          if (!hitPair && massiveOverbet) { action[0] = 2; action[1] = 0; }
          else if (hitPair) { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); }
          else { action[0] = 2; action[1] = 0; }
        } else {
          if (hitPair) {
            action[0] = 3;
            int betAmt = Math.max(blind, (int) (potSize * 0.65));
            action[1] = Math.min(betAmt, super.getChips());
          } else { action[0] = 0; action[1] = 0; }
        }
        return action;
      }

      case WHALE: {
        // Calls massive bets even with bottom pair. Inelastic to sizing.
        if (facingBet) {
          // Only fold absolute air (no pair, no draw heuristic)
          if (!hitPair && Math.random() < 0.20) { action[0] = 2; action[1] = 0; }
          else { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); }
        } else { action[0] = 0; action[1] = 0; } // check
        return action;
      }

      case FISH: {
        // Fit-or-fold. Calls one small bet on flop. Folds to heavy turn aggression if missed.
        if (facingBet) {
          boolean smallBet = (callAmt <= potSize / 3);
          boolean turnOrRiver = (board.length >= 4);
          if (!hitPair && (turnOrRiver || !smallBet)) { action[0] = 2; action[1] = 0; }
          else { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); }
        } else { action[0] = 0; action[1] = 0; }
        return action;
      }

      case BULLY: {
        // Hyper-aggressive. Overbet the pot to force folds. Very rarely checks.
        if (facingBet) {
          // Re-raise with significant frequency
          if (Math.random() < 0.50) {
            action[0] = 3;
            int raiseTo = bet * 3;
            action[1] = Math.min(raiseTo, super.getChips() + prevBet) - prevBet;
          } else { action[0] = 1; action[1] = Math.min(callAmt, super.getChips()); }
        } else {
          // Overbet pot
          action[0] = 3;
          int betAmt = (int) (potSize * 1.20);
          action[1] = Math.min(Math.max(betAmt, blind), super.getChips());
        }
        return action;
      }

      case SHORT_STACKER: {
        // Always shove remaining chips if in a postflop situation
        action[0] = 4; action[1] = super.getChips();
        return action;
      }

      case LAG: {
        // Aggressive postflop: high c-bet frequency, frequent double barrels, check-raise bluffs
        if (!facingBet) {
          // IP or checked to: c-bet/probe ~70% of the time
          boolean hasSomething = hitPair || (hi >= 12);
          double betFreq = hasSomething ? 0.80 : 0.55;
          if (Math.random() < betFreq) {
            int betSize = (int)(potSize * (hasSomething ? 0.70 : 0.50));
            betSize = Math.max(blind, Math.min(betSize, super.getChips()));
            action[0] = 3; action[1] = betSize;
          } else {
            action[0] = 1; action[1] = 0; // check
          }
        } else {
          // Facing a bet: check-raise with strong hands + some bluffs
          boolean strongHand = (hitTopPair && hi >= 10) || (paired && hi >= topBoard);
          if (strongHand && Math.random() < 0.45) {
            // Check-raise
            int raiseAmt = Math.min(bet * 3, super.getChips() + prevBet) - prevBet;
            action[0] = 3; action[1] = Math.max(raiseAmt, callAmt);
          } else if (hitPair || hi >= 12) {
            // Call with pair or overcards
            action[0] = 1; action[1] = callAmt;
          } else if (Math.random() < 0.20) {
            // Float bluff ~20% with air
            action[0] = 1; action[1] = callAmt;
          } else {
            action[0] = 2; action[1] = 0; // fold
          }
        }
        return action;
      }

      default:
        action[0] = 2; action[1] = 0; return action;
    }
  }
}

class Names { // avoid dupe names
  private static final String[] names = new String[] { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike",
      "Charlie", "Ellie",
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
      if (!used)
        return candidate;
    }
  }
}