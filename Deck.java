import java.util.*;

public class Deck { // deck of Cards
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
    Collections.shuffle(deck); // shuffles deck once uponn
  }
  public void shuffle() { // an RRCR sequence, which riffle shuffles x2 in a row, cuts the deck, and riffle shuffles again
    Collections.shuffle(deck);
    Collections.shuffle(deck);
    // cutting procedure start
    Card[] t = deck.toArray(new Card[deck.size()]);
    Card[] topHalf = Arrays.copyOfRange(t, 0, deck.size()/2);
    Card[] bottomHalf = Arrays.copyOfRange(t, deck.size()/2, deck.size());
    deck = new ArrayList<Card>();
    Collections.addAll(deck, bottomHalf);
    Collections.addAll(deck, topHalf);
    // cutting procedure end
    Collections.shuffle(deck);
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