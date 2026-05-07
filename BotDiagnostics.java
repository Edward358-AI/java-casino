import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public final class BotDiagnostics {
  private static final String ENABLE_PROP = "poker.diag";
  private static final String TRACE_ENABLE_PROP = "poker.trace";
  private static final String FILE_PATH = "bot_diagnostics.jsonl";

  private static final boolean ENABLED = isEnabledFromConfig();
  private static final boolean TRACE_ENABLED = isTraceEnabledFromConfig();
  private static final ThreadLocal<Boolean> THREAD_FORCE_ENABLED = ThreadLocal.withInitial(() -> false);

  private static final class SimulatorContext {
    final String mode;
    final String passLabel;
    final int workerId;
    final int handNumber;
    final String street;

    SimulatorContext(String mode, String passLabel, int workerId, int handNumber, String street) {
      this.mode = mode;
      this.passLabel = passLabel;
      this.workerId = workerId;
      this.handNumber = handNumber;
      this.street = street;
    }

    SimulatorContext withStreet(String nextStreet) {
      return new SimulatorContext(mode, passLabel, workerId, handNumber, nextStreet);
    }
  }

  private static final ThreadLocal<SimulatorContext> SIMULATOR_CONTEXT = new ThreadLocal<>();

  private BotDiagnostics() {}

  private static boolean isEnabledFromConfig() {
    String prop = System.getProperty(ENABLE_PROP);
    if (prop != null) {
      return "1".equals(prop) || "true".equalsIgnoreCase(prop) || "on".equalsIgnoreCase(prop);
    }

    String env = System.getenv("POKER_DIAG");
    if (env != null) {
      return "1".equals(env) || "true".equalsIgnoreCase(env) || "on".equalsIgnoreCase(env);
    }

    return false;
  }

  private static boolean isTraceEnabledFromConfig() {
    String prop = System.getProperty(TRACE_ENABLE_PROP);
    if (prop != null) {
      return "1".equals(prop) || "true".equalsIgnoreCase(prop) || "on".equalsIgnoreCase(prop);
    }

    String env = System.getenv("POKER_TRACE");
    if (env != null) {
      return "1".equals(env) || "true".equalsIgnoreCase(env) || "on".equalsIgnoreCase(env);
    }

    return false;
  }

  public static boolean enabled() {
    return ENABLED || THREAD_FORCE_ENABLED.get();
  }

  public static boolean traceConsoleEnabled() {
    return TRACE_ENABLED;
  }

  public static void setThreadForceEnabled(boolean forceEnabled) {
    THREAD_FORCE_ENABLED.set(forceEnabled);
  }

  public static void clearThreadForceEnabled() {
    THREAD_FORCE_ENABLED.remove();
  }

  public static void setSimulatorContext(String mode, String passLabel, int workerId, int handNumber, String street) {
    if (!enabled()) {
      return;
    }
    SIMULATOR_CONTEXT.set(new SimulatorContext(
        safeLabel(mode, "SIM"),
        safeLabel(passLabel, "BASE"),
        workerId,
        handNumber,
        safeLabel(street, "UNKNOWN")));
  }

  public static void updateSimulatorStreet(String street) {
    if (!enabled()) {
      return;
    }
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context == null) {
      return;
    }
    SIMULATOR_CONTEXT.set(context.withStreet(safeLabel(street, "UNKNOWN")));
  }

  public static void clearSimulatorContext() {
    SIMULATOR_CONTEXT.remove();
  }

  private static String safeLabel(String value, String fallback) {
    if (value == null || value.trim().isEmpty()) {
      return fallback;
    }
    return value.trim();
  }

  private static String jsonEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static int currentHandNumber() {
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context != null && context.handNumber >= 0) {
      return context.handNumber;
    }

    PokerGame game = PokerGame.getActiveGame();
    if (game == null) {
      return -1;
    }
    return game.getCurrentHandNumber();
  }

  private static String currentStreetLabel() {
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context != null && context.street != null && !context.street.isEmpty()) {
      return context.street;
    }

    PokerGame game = PokerGame.getActiveGame();
    if (game == null) {
      return "UNKNOWN";
    }

    switch (game.currentStreet) {
      case 0:
        return "PREFLOP";
      case 1:
        return "FLOP";
      case 2:
        return "TURN";
      case 3:
        return "RIVER";
      default:
        return "UNKNOWN";
    }
  }

  private static String currentModeLabel() {
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context == null || context.mode == null || context.mode.isEmpty()) {
      return "RUNTIME";
    }
    return context.mode;
  }

  private static String currentPassLabel() {
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context == null || context.passLabel == null || context.passLabel.isEmpty()) {
      return "DEFAULT";
    }
    return context.passLabel;
  }

  private static int currentWorkerId() {
    SimulatorContext context = SIMULATOR_CONTEXT.get();
    if (context == null) {
      return -1;
    }
    return context.workerId;
  }

  private static synchronized void writeJsonl(String category, String payload) {
    String safePayload = jsonEscape(payload);
    int hand = currentHandNumber();
    String street = currentStreetLabel();
    String mode = jsonEscape(currentModeLabel());
    String pass = jsonEscape(currentPassLabel());
    int worker = currentWorkerId();
    String json = "{\"ts\":\"" + Instant.now().toString() + "\",\"category\":\"" + category
        + "\",\"mode\":\"" + mode
        + "\",\"pass\":\"" + pass
        + "\",\"worker\":" + worker
        + ",\"hand\":" + hand
        + ",\"street\":\"" + jsonEscape(street) + "\""
        + ",\"payload\":\"" + safePayload + "\"}";
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
      bw.write(json);
      bw.newLine();
    } catch (IOException e) {
      // Intentionally silent: diagnostics should never disturb gameplay output.
    }
  }

  public static void recordGodDecision(String street, String botName, String decisionReason,
      String actionLabel, int amount, String details) {
    if (!enabled()) {
      return;
    }
    writeJsonl("decision",
        "street=" + street + ", bot=" + botName + ", action=" + actionLabel + ", amount=" + amount
            + ", reason=" + decisionReason + ", " + details);
  }

  public static void recordPlayerDecision(String street, String playerName, String actionLabel, int amount, String details) {
    if (!enabled()) {
      return;
    }
    writeJsonl("decision",
        "street=" + street + ", bot=" + playerName + " (HUMAN), action=" + actionLabel + ", amount=" + amount
            + ", reason=human input, " + details);
  }

  public static void recordArchetypeShift(String playerName,
      PokerBot.CognitiveArchetype before,
      PokerBot.CognitiveArchetype after,
      PokerBot.CognitiveProfile profile,
      String trigger) {
    if (!enabled() || before == after) {
      return;
    }

    String payload = "player=" + playerName
        + ", before=" + before
        + ", after=" + after
        + ", trigger=" + trigger
        + ", vpipEMA=" + String.format("%.3f", profile.vpipEMA)
        + ", pfrEMA=" + String.format("%.3f", profile.pfrEMA)
        + ", afqFlopEMA=" + String.format("%.3f", profile.afqFlopEMA)
        + ", afqTurnEMA=" + String.format("%.3f", profile.afqTurnEMA)
        + ", afqRiverEMA=" + String.format("%.3f", profile.afqRiverEMA)
        + ", foldToCbetEMA=" + String.format("%.3f", profile.foldToCbetEMA)
        + ", wtsdEMA=" + String.format("%.3f", profile.wtsdEMA)
        + ", vIndex=" + String.format("%.3f", profile.vIndex)
        + ", styleShiftEMA=" + String.format("%.3f", profile.styleShiftEMA);
      writeJsonl("archetype-shift", payload);
  }

  public static void recordProfileSnapshot(String playerName, PokerBot.CognitiveProfile profile, String trigger) {
    if (!enabled()) {
      return;
    }

    String payload = "player=" + playerName
        + ", trigger=" + trigger
        + ", archetype=" + profile.getArchetype()
        + ", handsPlayed=" + profile.handsPlayed
        + ", vpipEMA=" + String.format("%.3f", profile.vpipEMA)
        + ", pfrEMA=" + String.format("%.3f", profile.pfrEMA)
        + ", afqPreflopEMA=" + String.format("%.3f", profile.afqPreflopEMA)
        + ", afqFlopEMA=" + String.format("%.3f", profile.afqFlopEMA)
        + ", afqTurnEMA=" + String.format("%.3f", profile.afqTurnEMA)
        + ", afqRiverEMA=" + String.format("%.3f", profile.afqRiverEMA)
        + ", foldToCbetEMA=" + String.format("%.3f", profile.foldToCbetEMA)
        + ", wtsdEMA=" + String.format("%.3f", profile.wtsdEMA)
        + ", vIndex=" + String.format("%.3f", profile.vIndex)
        + ", styleShiftEMA=" + String.format("%.3f", profile.styleShiftEMA);
    writeJsonl("profile", payload);
  }

  public static void recordConvergence(String playerName,
      int seat,
      long samples,
      double trueVpip,
      double truePfr,
      double emaVpip,
      double emaPfr,
      String trigger) {
    if (!enabled()) {
      return;
    }

    double signedVpipErr = emaVpip - trueVpip;
    double signedPfrErr = emaPfr - truePfr;
    double absVpipErr = Math.abs(signedVpipErr);
    double absPfrErr = Math.abs(signedPfrErr);

    String payload = "player=" + playerName
        + ", seat=" + seat
        + ", trigger=" + trigger
        + ", samples=" + samples
        + ", trueVPIP=" + String.format("%.4f", trueVpip)
        + ", emaVPIP=" + String.format("%.4f", emaVpip)
        + ", absErrVPIP=" + String.format("%.4f", absVpipErr)
        + ", signedErrVPIP=" + String.format("%.4f", signedVpipErr)
        + ", truePFR=" + String.format("%.4f", truePfr)
        + ", emaPFR=" + String.format("%.4f", emaPfr)
        + ", absErrPFR=" + String.format("%.4f", absPfrErr)
        + ", signedErrPFR=" + String.format("%.4f", signedPfrErr);
    writeJsonl("convergence", payload);
  }

  public static void recordSourceTransferHand(String payload) {
    if (!enabled()) {
      return;
    }
    writeJsonl("source-transfer-hand", payload);
  }

  public static void recordSourceTransferRollup(String payload) {
    if (!enabled()) {
      return;
    }
    writeJsonl("source-transfer-rollup", payload);
  }
}
