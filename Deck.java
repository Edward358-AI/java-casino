import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Deck { // deck of Cards
  // Reproducibility: when DECK_SEED >= 0, every shuffle is derived from
  // DECK_SEED + HAND_COUNTER (atomic, so parallel runs still increment
  // monotonically — but exact replay requires single-threaded execution).
  // Set via Deck.setSeed(long). Default (-1) preserves the original
  // unseeded JVM-default behaviour.
  private static volatile long DECK_SEED = -1L;
  private static final AtomicLong HAND_COUNTER = new AtomicLong(0L);

  public static void setSeed(long s) {
    DECK_SEED = s;
    HAND_COUNTER.set(0L);
    if (s >= 0L) SimRng.seed(s);
    else SimRng.unseed();
  }

  public static long getSeed() { return DECK_SEED; }
  public static long getHandCounter() { return HAND_COUNTER.get(); }

  // Deterministic unshuffled card list — used for static-init constants like
  // CANONICAL_DECK in SimEngine where iteration order must not depend on which
  // thread first triggered class loading. Returns a fresh array each call.
  public static Card[] canonicalCards() {
    String[] temp = { "A♠️", "A♣️", "A♦️", "A♥️", "2♠️", "2♣️", "2♦️", "2♥️", "3♠️", "3♣️", "3♦️", "3♥️", "4♠️",
        "4♣️", "4♦️", "4♥️", "5♠️", "5♣️", "5♦️", "5♥️", "6♠️", "6♣️", "6♦️", "6♥️", "7♠️", "7♣️",
        "7♦️", "7♥️", "8♠️", "8♣️", "8♦️", "8♥️", "9♠️", "9♣️", "9♦️", "9♥️", "T♠️", "T♣️",
        "T♦️", "T♥️", "J♠️", "J♣️", "J♦️", "J♥️", "Q♠️", "Q♣️", "Q♦️", "Q♥️", "K♠️", "K♣️",
        "K♦️", "K♥️" };
    Card[] cards = new Card[temp.length];
    for (int i = 0; i < temp.length; i++) cards[i] = new Card(temp[i]);
    return cards;
  }

  private ArrayList<Card> deck = new ArrayList<Card>();
  private ArrayList<Card> removed = new ArrayList<Card>(); // keeps track of cards dealt away to be more efficient when returning them to deck
  public Deck() {
    String[] temp = { "A♠️", "A♣️", "A♦️", "A♥️", "2♠️", "2♣️", "2♦️", "2♥️", "3♠️", "3♣️", "3♦️", "3♥️", "4♠️",
        "4♣️", "4♦️", "4♥️", "5♠️", "5♣️", "5♦️", "5♥️", "6♠️", "6♣️", "6♦️", "6♥️", "7♠️", "7♣️",
        "7♦️", "7♥️", "8♠️", "8♣️", "8♦️", "8♥️", "9♠️", "9♣️", "9♦️", "9♥️", "T♠️", "T♣️",
        "T♦️", "T♥️", "J♠️", "J♣️", "J♦️", "J♥️", "Q♠️", "Q♣️", "Q♦️", "Q♥️", "K♠️", "K♣️",
        "K♦️", "K♥️" }; // temporary array of all string values for card
    for (int i = 0; i < temp.length; i++) {
      deck.add(new Card(temp[i]));
    }
    shuffleOnce(nextRng()); // shuffles deck once upon construction
  }

  // One Random per shuffle event — keeps the three RRCR shuffles mutually
  // correlated through the same Random instance, so a single seed-derivation
  // gives one fully reproducible deal sequence.
  //
  // Two paths:
  //   1. Per-thread hand seed set (multi-thread reproducibility):
  //      SimRng.setHandSeed(workerId, handIdx) was called on this thread, so
  //      we derive the deck stream from that hand seed. Different runs with
  //      the same baseSeed produce identical decks per (workerId, handIdx)
  //      regardless of thread scheduling.
  //   2. No per-thread hand seed (single-thread back-compat): fall back to
  //      the global atomic counter — works for runDuplicateDuel and any
  //      caller that hasn't migrated to setHandSeed.
  //
  // The deck stream is derived as `handSeed * MIX + 1` so it's independent
  // of the SimRng stream that uses `handSeed` directly — bots and dealer
  // don't share a Random instance.
  private static final long DECK_STREAM_MIX = 6364136223846793005L;
  private static Random nextRng() {
    if (DECK_SEED < 0L) return null;
    Long handSeed = SimRng.getThreadHandSeed();
    if (handSeed != null) {
      // Per-thread draw counter so multiple shuffles between setHandSeed calls
      // (Mode 7 baseline) each get a distinct stream while staying reproducible
      // per (workerId, handIdx, drawIdx).
      long draw = SimRng.nextDeckDrawIndex();
      return new Random(handSeed * DECK_STREAM_MIX + draw + 1L);
    }
    return new Random(DECK_SEED + HAND_COUNTER.getAndIncrement());
  }

  private void shuffleOnce(Random rng) {
    if (rng == null) Collections.shuffle(deck);
    else Collections.shuffle(deck, rng);
  }

  public void shuffle() { // an RRCR sequence, which riffle shuffles x2 in a row, cuts the deck, and riffle shuffles again
    Random rng = nextRng();
    shuffleOnce(rng);
    shuffleOnce(rng);
    // cutting procedure start
    Card[] t = deck.toArray(new Card[deck.size()]);
    Card[] topHalf = Arrays.copyOfRange(t, 0, deck.size()/2);
    Card[] bottomHalf = Arrays.copyOfRange(t, deck.size()/2, deck.size());
    deck = new ArrayList<Card>();
    Collections.addAll(deck, bottomHalf);
    Collections.addAll(deck, topHalf);
    // cutting procedure end
    shuffleOnce(rng);
  }

  public Card[] getCards() { // returns array of the deck
    return deck.toArray(new Card[deck.size()]);
  }

  public Card[] deal() { // deals one Card
    Card r = deck.remove(0);
    removed.add(r);
    return new Card[]{r};
  }

  public void reset() { // resets the deck, adds any dealt Cards back
    deck.addAll(removed);
    removed = new ArrayList<Card>();
    shuffle();
  }

  public static void sort(Card[] d) { // utility sort function that sorts an array of Cards
    Arrays.sort(d, new cardSort());
  }

  public static String[] cardToString(Card[] d) { // utility Cards to string array
    String[] a = new String[d.length];
    for (int i = 0; i < d.length; i++) a[i] = d[i].getValue();
    return a;
  }

  public static int[] cardToInt(Card[] d) {// utility Cards to int array
    int[] a = new int[d.length];
    for (int i = 0; i < d.length; i++) a[i] = d[i].getNum();
    return a;
  }
}


class cardSort implements Comparator<Card> { // comparator interface for sorting an array of Cards
  public int compare(Card a, Card b) {
    return a.getNum() - b.getNum();
  }
}