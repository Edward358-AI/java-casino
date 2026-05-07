import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralized RNG for all stochastic decisions in the simulator (mixed
 * strategies, frequency rolls, sizing jitter, side-pot chop remainders, seat
 * permutations). Replaces direct Math.random() / ThreadLocalRandom / new
 * Random() calls so that the entire run can be made reproducible by a single
 * Deck.setSeed(N).
 *
 * Reproducibility model:
 *   - When Deck.getSeed() < 0 (seed not set), every call falls through to
 *     ThreadLocalRandom — preserves the original unseeded throughput and
 *     parallel-safety with no synchronization cost.
 *   - When Deck.getSeed() >= 0, each thread maintains its own seeded Random.
 *     The simulation loop is responsible for calling SimRng.setHandSeed
 *     (workerId, handIdx) at the start of each hand it processes — that
 *     re-seeds the calling thread's Random from a deterministic mix of
 *     (baseSeed, workerId, handIdx). Same (workerId, handIdx) on a different
 *     run with the same baseSeed produces byte-identical RNG output, even
 *     under parallel scheduling.
 *
 * Bootstrap fallback: if a SimRng method is called on a thread that hasn't
 * had setHandSeed called yet (e.g., during bot construction before any hand
 * begins), we synthesize a deterministic per-thread seed from
 * (baseSeed, threadId) so even those calls are reproducible per-thread —
 * though the assignment of code paths to thread IDs is still scheduler-
 * dependent. Workers should call setHandSeed early to avoid this.
 */
public final class SimRng {
    private SimRng() {}

    private static volatile boolean seeded = false;
    private static final ThreadLocal<Random> LOCAL_RNG = new ThreadLocal<>();
    private static final ThreadLocal<Long> THREAD_HAND_SEED = new ThreadLocal<>();
    private static final ThreadLocal<Integer> THREAD_WORKER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> THREAD_HAND_IDX = new ThreadLocal<>();
    // Counter incremented on every Deck.nextRng() call within this thread.
    // Resets on each setHandSeed call. Lets Mode 7 baseline (which does many
    // shuffles between setHandSeed calls) get a unique stream per shuffle.
    private static final ThreadLocal<long[]> DRAW_COUNTER = ThreadLocal.withInitial(() -> new long[]{0L});

    // Splitmix-style multiplier used to derive independent streams from the
    // same parent seed without additional state.
    private static final long MIX = 6364136223846793005L;

    /**
     * Compute the deterministic hand seed for (workerId, handIdx) given the
     * current Deck.getSeed() base. Public so workers can pass the same value
     * to other reproducibility consumers if needed.
     */
    public static long deriveHandSeed(int workerId, long handIdx) {
        long base = Deck.getSeed();
        long h = base;
        h = h * MIX + workerId;
        h = h * MIX + handIdx;
        return h;
    }

    /**
     * Workers MUST call this at the start of every hand they process when the
     * deck is seeded. Re-seeds the calling thread's RNG and stores the seed
     * so Deck.nextRng() can derive the matching deck-shuffle stream.
     */
    public static void setHandSeed(int workerId, long handIdx) {
        long s = deriveHandSeed(workerId, handIdx);
        THREAD_HAND_SEED.set(s);
        THREAD_WORKER_ID.set(workerId);
        THREAD_HAND_IDX.set(handIdx);
        DRAW_COUNTER.get()[0] = 0L;
        Random r = LOCAL_RNG.get();
        if (r == null) {
            r = new Random(s);
            LOCAL_RNG.set(r);
        } else {
            r.setSeed(s);
        }
    }

    /** Returns and increments this thread's per-shuffle draw counter. Used by
     *  Deck.nextRng() so multiple deck constructions between setHandSeed calls
     *  (e.g., Mode 7 baseline's runIndividualContinuous loop) get distinct
     *  shuffles within a single worker run. */
    public static long nextDeckDrawIndex() {
        long[] c = DRAW_COUNTER.get();
        long v = c[0];
        c[0] = v + 1L;
        return v;
    }

    /** Reads the per-thread hand seed; null if none set on this thread. */
    public static Long getThreadHandSeed() {
        return THREAD_HAND_SEED.get();
    }

    /** Reads the worker ID this thread is currently processing; null if none set. */
    public static Integer getThreadWorkerId() {
        return THREAD_WORKER_ID.get();
    }

    /** Reads the hand index this thread is currently processing; null if none set. */
    public static Long getThreadHandIdx() {
        return THREAD_HAND_IDX.get();
    }

    /** Clears thread-local state — call when a worker is done. */
    public static void clearThreadState() {
        THREAD_HAND_SEED.remove();
        THREAD_WORKER_ID.remove();
        THREAD_HAND_IDX.remove();
        DRAW_COUNTER.remove();
        LOCAL_RNG.remove();
    }

    static void seed(long s) {
        seeded = (s >= 0L);
        // Don't touch thread-locals — workers re-seed per hand. But clear the
        // calling thread's state so a new run on a re-used JVM starts fresh.
        clearThreadState();
    }

    static void unseed() {
        seeded = false;
        clearThreadState();
    }

    public static boolean isSeeded() { return seeded; }

    @SuppressWarnings("deprecation") // Thread.getId() — still functional;
    // .threadId() is Java 19+ and we don't gate on JDK version.
    private static Random tlsRng() {
        Random r = LOCAL_RNG.get();
        if (r != null) return r;
        // No setHandSeed yet on this thread — synthesize a deterministic
        // bootstrap seed so the call is at least reproducible per-thread.
        long bootstrap = Deck.getSeed() ^ (Thread.currentThread().getId() * MIX);
        r = new Random(bootstrap);
        LOCAL_RNG.set(r);
        return r;
    }

    public static double nextDouble() {
        if (!seeded) return ThreadLocalRandom.current().nextDouble();
        return tlsRng().nextDouble();
    }

    public static int nextInt(int bound) {
        if (!seeded) return ThreadLocalRandom.current().nextInt(bound);
        return tlsRng().nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        if (!seeded) return ThreadLocalRandom.current().nextInt(origin, bound);
        return origin + tlsRng().nextInt(bound - origin);
    }

    public static long nextLong() {
        if (!seeded) return ThreadLocalRandom.current().nextLong();
        return tlsRng().nextLong();
    }

    /** Reproducible drop-in for Collections.shuffle(list). When seeded, derives
     *  a one-shot Random from this thread's stream so the shuffle is
     *  independent of subsequent bot-decision draws. When unseeded, falls
     *  through to Collections.shuffle's default behavior. */
    public static <T> void shuffle(java.util.List<T> list) {
        if (!seeded) {
            java.util.Collections.shuffle(list);
        } else {
            java.util.Collections.shuffle(list, new Random(tlsRng().nextLong()));
        }
    }
}
