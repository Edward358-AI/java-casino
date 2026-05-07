import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Verification harness: runs Mode 7 (True EV / Independent Hands) directly
 * with a chosen opponent archetype and hand count, bypassing the interactive
 * setup menu.
 *
 * Bot table is set via reflection: 1 God Bot (the arm under test) + N copies
 * of the chosen opponent archetype (default N=1; override with --opponents).
 * All other tier counts are zeroed out for the run, then restored after.
 *
 * Hand-level CSV (when --out-csv is set) captures the paired-pass deals only.
 * Mode 7's baseline pass routes through SimEngine.runIndividualContinuous and
 * is not currently instrumented for per-hand export — its data lives in the
 * aggregate mode7_telemetry_output.txt block.
 *
 * Usage:
 *   java Mode7Verify <archetypeIndex 0-8> <hands> [mode] [flags...]
 *
 *   archetypeIndex: 0:NIT 1:STATION 2:MANIAC 3:TAG 4:WHALE 5:FISH 6:BULLY 7:SHORT_STACKER 8:LAG
 *
 *   mode (optional, default = neural): pure | neural | unprotected (alias: nightmare)
 *
 *   flags:
 *     --seed N           Set Deck.setSeed(N) for reproducible deals.
 *     --out-csv PATH     Write per-hand CSV (paired pass only) to PATH.
 *     --run-id ID        Tag every CSV row with this run identifier.
 *     --opponents K      Number of archetype opponents at the table (default 1).
 *     --workers N        Enable parallel mode with N worker threads (default 1).
 *                        Reproducibility joint key is (seed, workers); same
 *                        seed with different worker counts produces different
 *                        partitions. Lock both in your reproducibility manifest.
 */
public class Mode7Verify {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Mode7Verify <archetypeIndex 0-8> <hands> [mode] [--seed N] [--out-csv PATH] [--run-id ID] [--opponents K]");
            System.exit(2);
        }

        int arc = Integer.parseInt(args[0]);
        int hands = Integer.parseInt(args[1]);
        String mode = "neural";
        long seed = -1L;
        String outCsv = null;
        String runId = "";
        int opponents = 1;
        int workers = 1;

        int idx = 2;
        if (idx < args.length && !args[idx].startsWith("--")) {
            mode = args[idx].toLowerCase();
            idx++;
        }
        while (idx < args.length) {
            String a = args[idx];
            switch (a) {
                case "--seed":      seed = Long.parseLong(args[++idx]); break;
                case "--out-csv":   outCsv = args[++idx]; break;
                case "--run-id":    runId = args[++idx]; break;
                case "--opponents": opponents = Integer.parseInt(args[++idx]); break;
                case "--workers":   workers = Integer.parseInt(args[++idx]); break;
                default: throw new IllegalArgumentException("Unknown flag: " + a);
            }
            idx++;
        }
        if (workers < 1) {
            throw new IllegalArgumentException("--workers must be >= 1, got: " + workers);
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

        // Scripted input: "n" to skip single-hand mode, then hand count.
        String input = "n\n" + hands + "\n";
        final ByteArrayInputStream src = new ByteArrayInputStream(input.getBytes());
        InputStream wrapped = new InputStream() {
            @Override public int read() throws IOException { return src.read(); }
            @Override public int read(byte[] b, int off, int len) throws IOException { return src.read(b, off, len); }
            @Override public int available() { return 0; }
        };
        System.setIn(wrapped);

        Field sf = Player.class.getDeclaredField("sc");
        sf.setAccessible(true);
        sf.set(null, new java.util.Scanner(System.in));

        Field psf = PokerSimulator.class.getDeclaredField("sc");
        psf.setAccessible(true);
        psf.set(null, sf.get(null));

        Field pmf = PokerSimulator.class.getDeclaredField("isProtectedMode");
        pmf.setAccessible(true);
        Field nmf = PokerSimulator.class.getDeclaredField("isNeuralProtectedMode");
        nmf.setAccessible(true);
        Field pef = PokerSimulator.class.getDeclaredField("parallelEnabled");
        pef.setAccessible(true);
        Field ptf = PokerSimulator.class.getDeclaredField("parallelThreads");
        ptf.setAccessible(true);
        boolean savedProtected = pmf.getBoolean(null);
        boolean savedNeural = nmf.getBoolean(null);
        boolean savedParallel = pef.getBoolean(null);
        int savedThreads = ptf.getInt(null);
        long savedDeckSeed = Deck.getSeed();

        // Save current bot-count config so we can restore it on exit. All
        // mutations happen inside the try block so a failure during
        // openHandCsv (or anywhere else) does not leak modified static state.
        String[] tierFields = {"dumbCount", "smartCount", "godCount", "arcEliteRegCount"};
        String[] arcFields  = {"arcNitCount", "arcStationCount", "arcManiacCount", "arcTagCount",
                               "arcWhaleCount", "arcFishCount", "arcBullyCount",
                               "arcShortStackerCount", "arcLagCount"};
        if (arc < 0 || arc >= arcFields.length) {
            throw new IllegalArgumentException("archetypeIndex must be 0..8, got: " + arc);
        }
        java.util.Map<String, Integer> saved = new java.util.HashMap<>();
        for (String f : tierFields) saved.put(f, getInt(f));
        for (String f : arcFields)  saved.put(f, getInt(f));

        String archetypeFocus = arcFields[arc].replace("arc", "").replace("Count", "").toUpperCase();

        PokerBot.resetDebugCounters();

        System.err.println("[Mode7Verify] arc=" + arc + " (" + archetypeFocus + ") hands=" + hands
            + " mode=" + mode + " protected=" + protectedOn + " neural=" + neuralOn
            + " seed=" + seed + " out-csv=" + outCsv + " run-id=" + runId
            + " opponents=" + opponents + " workers=" + workers);

        try {
            pmf.set(null, protectedOn);
            nmf.set(null, neuralOn);
            pef.set(null, workers > 1);
            ptf.setInt(null, workers);
            for (String f : tierFields) setInt(f, 0);
            for (String f : arcFields)  setInt(f, 0);
            setInt("godCount", 1);
            setInt(arcFields[arc], opponents);
            if (seed >= 0L) Deck.setSeed(seed);
            if (outCsv != null) {
                PokerSimulator.openHandCsv(outCsv, runId, archetypeFocus);
            }

            Method m = PokerSimulator.class.getDeclaredMethod("runIndependentHands");
            m.setAccessible(true);
            m.invoke(null);
        } finally {
            if (outCsv != null) PokerSimulator.closeHandCsv();
            pmf.set(null, savedProtected);
            nmf.set(null, savedNeural);
            pef.set(null, savedParallel);
            ptf.setInt(null, savedThreads);
            Deck.setSeed(savedDeckSeed);
            for (java.util.Map.Entry<String, Integer> e : saved.entrySet()) {
                setInt(e.getKey(), e.getValue());
            }
        }
    }

    private static int getInt(String name) throws Exception {
        Field f = PokerSimulator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(null);
    }

    private static void setInt(String name, int value) throws Exception {
        Field f = PokerSimulator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(null, value);
    }
}
