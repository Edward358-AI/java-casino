import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public final class BotDiagnostics {
  private static final String ENABLE_PROP = "poker.diag";
  private static final String FILE_PATH = "bot_diagnostics.jsonl";

  private static final boolean ENABLED = isEnabledFromConfig();

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

  public static boolean enabled() {
    return ENABLED;
  }

  private static int currentHandNumber() {
    PokerGame game = PokerGame.getActiveGame();
    if (game == null) {
      return -1;
    }
    return game.getCurrentHandNumber();
  }

  private static String currentStreetLabel() {
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

  private static synchronized void writeJsonl(String category, String payload) {
    String safePayload = payload.replace("\\", "\\\\").replace("\"", "\\\"");
    int hand = currentHandNumber();
    String street = currentStreetLabel();
    String json = "{\"ts\":\"" + Instant.now().toString() + "\",\"category\":\"" + category
        + "\",\"hand\":" + hand + ",\"street\":\"" + street + "\",\"payload\":\"" + safePayload + "\"}";
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
      bw.write(json);
      bw.newLine();
    } catch (IOException e) {
      // Intentionally silent: diagnostics should never disturb gameplay output.
    }
  }

  public static void recordGodDecision(String street, String botName, String decisionReason,
      String actionLabel, int amount, String details) {
    if (!ENABLED) {
      return;
    }
    writeJsonl("decision",
        "street=" + street + ", bot=" + botName + ", action=" + actionLabel + ", amount=" + amount
            + ", reason=" + decisionReason + ", " + details);
  }

  public static void recordPlayerDecision(String street, String playerName, String actionLabel, int amount, String details) {
    if (!ENABLED) {
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
    if (!ENABLED || before == after) {
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
    if (!ENABLED) {
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
}
