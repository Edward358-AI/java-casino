import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Verification harness: runs Mode 6 (Duplicate Duel) directly against a chosen
 * archetype with a chosen pair count, bypassing the interactive setup menu.
 *
 * Usage: java Mode6Verify <archetypeIndex 0-8> <pairs> [mode]
 *   archetypeIndex matches PokerSimulator.SELECTABLE_ARCHETYPES:
 *     0:NIT 1:STATION 2:MANIAC 3:TAG 4:WHALE 5:FISH 6:BULLY 7:SHORT_STACKER 8:LAG
 *   mode (optional, default = neural):
 *     pure       — Protected ON, Neural OFF (Pure GTO, ignores telemetry)
 *     neural     — Protected ON, Neural ON  (Neural Sandbox, archetype-aware) [DEFAULT]
 *     nightmare  — Protected OFF             (full exploitative — currently broken)
 *
 * Wraps System.in so Utils.flushInput() (which drains all "available" bytes)
 * cannot consume our scripted input.
 */
public class Mode6Verify {
    public static void main(String[] args) throws Exception {
        int arc = Integer.parseInt(args[0]);
        int pairs = Integer.parseInt(args[1]);
        String mode = args.length >= 3 ? args[2].toLowerCase() : "neural";

        boolean protectedOn;
        boolean neuralOn;
        switch (mode) {
            case "pure":      protectedOn = true;  neuralOn = false; break;
            case "neural":    protectedOn = true;  neuralOn = true;  break;
            case "nightmare": protectedOn = false; neuralOn = false; break;
            default: throw new IllegalArgumentException("mode must be pure|neural|nightmare, got: " + mode);
        }

        String input = "2\n3\n" + arc + "\n" + pairs + "\n";
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

        // Set Protected Mode and Neural Protected Mode per chosen mode.
        Field pmf = PokerSimulator.class.getDeclaredField("isProtectedMode");
        pmf.setAccessible(true);
        pmf.set(null, protectedOn);
        Field nmf = PokerSimulator.class.getDeclaredField("isNeuralProtectedMode");
        nmf.setAccessible(true);
        nmf.set(null, neuralOn);

        System.err.println("[Mode6Verify] arc=" + arc + " pairs=" + pairs + " mode=" + mode
            + " protected=" + protectedOn + " neural=" + neuralOn);

        Method m = PokerSimulator.class.getDeclaredMethod("runDuplicateDuel");
        m.setAccessible(true);
        m.invoke(null);
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
