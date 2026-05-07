import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Verification harness: runs Mode 6 (Duplicate Duel) directly against a chosen
 * archetype with a chosen pair count, bypassing the interactive setup menu.
 *
 * Usage:
 *   java Mode6Verify <archetypeIndex 0-8> <pairs> [mode] [flags...]
 *
 *   archetypeIndex matches PokerSimulator.SELECTABLE_ARCHETYPES:
 *     0:NIT 1:STATION 2:MANIAC 3:TAG 4:WHALE 5:FISH 6:BULLY 7:SHORT_STACKER 8:LAG
 *
 *   mode (optional, default = neural):
 *     pure         — Protected ON,  Neural OFF (Pure GTO, ignores telemetry)
 *     neural       — Protected ON,  Neural ON  (Neural Sandbox, archetype-aware)
 *     unprotected  — Protected OFF             (full exploit suite in batch sim)
 *     nightmare    — alias for unprotected (the nightmareIntensity branch is
 *                    gated by player name in PokerBot, so in batch sim this
 *                    reduces to the unprotected exploit baseline)
 *
 *   flags:
 *     --seed N         Set Deck.setSeed(N) for reproducible deals.
 *     --out-csv PATH   Write per-hand CSV to PATH (Mode 6 schema).
 *     --run-id ID      Tag every CSV row with this run identifier.
 *     --no-debug       Skip writing dbg_count.txt (BULLY debug instrumentation).
 *
 * Wraps System.in so Utils.flushInput() (which drains all "available" bytes)
 * cannot consume our scripted input.
 */
public class Mode6Verify {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Mode6Verify <archetypeIndex 0-8> <pairs> [mode] [--seed N] [--out-csv PATH] [--run-id ID] [--no-debug]");
            System.exit(2);
        }

        int arc = Integer.parseInt(args[0]);
        int pairs = Integer.parseInt(args[1]);
        String mode = "neural";
        long seed = -1L;
        String outCsv = null;
        String runId = "";
        boolean noDebug = false;
        // Default Mode 6 setup is "God Bot vs Archetype Bot" — the menu input
        // "2\n3\n" + arc selects tier 2 (God) for Bot A and tier 3 (Archetype)
        // for Bot B. --null-test overrides this to "God vs God" so both bots
        // share strategy, which is what you want for the pure-vs-pure null
        // test (BB/100 should be ~0 with CI containing 0).
        boolean nullTest = false;

        int idx = 2;
        if (idx < args.length && !args[idx].startsWith("--")) {
            mode = args[idx].toLowerCase();
            idx++;
        }
        while (idx < args.length) {
            String a = args[idx];
            switch (a) {
                case "--seed":
                    seed = Long.parseLong(args[++idx]);
                    break;
                case "--out-csv":
                    outCsv = args[++idx];
                    break;
                case "--run-id":
                    runId = args[++idx];
                    break;
                case "--no-debug":
                    noDebug = true;
                    break;
                case "--null-test":
                    nullTest = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown flag: " + a);
            }
            idx++;
        }

        boolean protectedOn;
        boolean neuralOn;
        switch (mode) {
            case "pure":        protectedOn = true;  neuralOn = false; break;
            case "neural":      protectedOn = true;  neuralOn = true;  break;
            case "unprotected": protectedOn = false; neuralOn = false; break;
            case "nightmare":   protectedOn = false; neuralOn = false; break;
            default: throw new IllegalArgumentException("mode must be pure|neural|unprotected, got: " + mode);
        }

        // Tier codes: 0=Dumb, 1=Smart, 2=God, 3=Archetype.
        // Default: Bot A = God (2), Bot B = Archetype (3, with index `arc`).
        // --null-test: both bots = God (2), arc index ignored. Same strategy,
        // same deck → expected BB/100 = 0 ± CI. Used to detect spurious edges
        // introduced by code changes.
        String input = nullTest
            ? "2\n2\n" + pairs + "\n"
            : "2\n3\n" + arc + "\n" + pairs + "\n";
        final ByteArrayInputStream src = new ByteArrayInputStream(input.getBytes());
        InputStream wrapped = new InputStream() {
            @Override public int read() throws IOException { return src.read(); }
            @Override public int read(byte[] b, int off, int len) throws IOException { return src.read(b, off, len); }
            @Override public int available() { return 0; }
        };
        System.setIn(wrapped);

        // Re-init the static Scanner inside Player to read from the wrapped stream
        Field sf = Player.class.getDeclaredField("sc");
        sf.setAccessible(true);
        sf.set(null, new java.util.Scanner(System.in));

        // Re-init PokerSimulator's private sc reference too
        Field psf = PokerSimulator.class.getDeclaredField("sc");
        psf.setAccessible(true);
        psf.set(null, sf.get(null));

        // Save current static config so we can restore it on exit (defensive —
        // the JVM exits at end of main, but if this is invoked twice in the
        // same JVM via reflection the second call would otherwise inherit the
        // first call's mode flags).
        Field pmf = PokerSimulator.class.getDeclaredField("isProtectedMode");
        pmf.setAccessible(true);
        Field nmf = PokerSimulator.class.getDeclaredField("isNeuralProtectedMode");
        nmf.setAccessible(true);
        boolean savedProtected = pmf.getBoolean(null);
        boolean savedNeural = nmf.getBoolean(null);
        long savedDeckSeed = Deck.getSeed();

        String archetypeFocus = archetypeName(arc);

        // Reset debug counters so dbg_count.txt reflects only this run.
        PokerBot.resetDebugCounters();

        System.err.println("[Mode6Verify] arc=" + arc + " (" + archetypeFocus + ") pairs=" + pairs
            + " mode=" + mode + " protected=" + protectedOn + " neural=" + neuralOn
            + " seed=" + seed + " out-csv=" + outCsv + " run-id=" + runId);

        try {
            pmf.set(null, protectedOn);
            nmf.set(null, neuralOn);
            if (seed >= 0L) Deck.setSeed(seed);
            if (outCsv != null) {
                PokerSimulator.openHandCsv(outCsv, runId, archetypeFocus);
            }

            Method m = PokerSimulator.class.getDeclaredMethod("runDuplicateDuel");
            m.setAccessible(true);
            m.invoke(null);
        } finally {
            if (outCsv != null) PokerSimulator.closeHandCsv();
            pmf.set(null, savedProtected);
            nmf.set(null, savedNeural);
            Deck.setSeed(savedDeckSeed);
        }

        if (!noDebug) {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("dbg_count.txt"),
                "BULLY postflop total=" + PokerBot.__DBG_BULLY_TOTAL.get()
                + " | facing-bet=" + PokerBot.__DBG_FACING_BET_RAISES.get()
                + " R=" + PokerBot.__DBG_BULLY_RAISE.get()
                + " C=" + PokerBot.__DBG_BULLY_CALL.get()
                + " F=" + PokerBot.__DBG_BULLY_FOLD.get()
                + "\nPREFLOP counter: 4bet=" + PokerBot.__DBG_PRE_4BET.get()
                + " call3b=" + PokerBot.__DBG_PRE_CALL3B.get()
                + " fold3b=" + PokerBot.__DBG_PRE_FOLD3B.get()
                + "\n");
        }
    }

    private static String archetypeName(int arc) {
        switch (arc) {
            case 0: return "NIT";
            case 1: return "STATION";
            case 2: return "MANIAC";
            case 3: return "TAG";
            case 4: return "WHALE";
            case 5: return "FISH";
            case 6: return "BULLY";
            case 7: return "SHORT_STACKER";
            case 8: return "LAG";
            default: return "ARC_" + arc;
        }
    }
}
