import java.util.*;

public class Deck {
  private ArrayList<Card> deck = new ArrayList<Card>();
  private ArrayList<Card> removed = new ArrayList<Card>();
  public Deck() {
    String[] temp = { "A♠", "A♣", "A♦", "A♥", "2♠", "2♣", "2♦", "2♥", "3♠", "3♣", "3♦", "3♥", "4♠",
        "4♣", "4♦", "4♥", "5♠", "5♣", "5♦", "5♥", "6♠", "6♣", "6♦", "6♥", "7♠", "7♣",
        "7♦", "7♥", "8♠", "8♣", "8♦", "8♥", "9♠", "9♣", "9♦", "9♥", "T♠", "T♣",
        "T♦", "T♥", "J♠", "J♣", "J♦", "J♥", "Q♠", "Q♣", "Q♦", "Q♥", "K♠", "K♣",
        "K♦", "K♥" };
    for (int i = 0; i < temp.length; i++) {
      deck.add(new Card(temp[i]));
      Collections.shuffle(deck);
    }
  }
  public void shuffle() {
    Collections.shuffle(deck);
    Collections.shuffle(deck);
    Card[] t = deck.toArray(new Card[deck.size()]);
    Card[] topHalf = Arrays.copyOfRange(t, 0, deck.size()/2);
    Card[] bottomHalf = Arrays.copyOfRange(t, deck.size()/2, deck.size());
    deck = new ArrayList<Card>();
    Collections.addAll(deck, bottomHalf);
    Collections.addAll(deck, topHalf);
    Collections.shuffle(deck);
  }

  public Card[] getDeck() {
    return deck.toArray(new Card[deck.size()]);
  }

  public Card[] deal() {
    Card r = deck.remove(0);
    removed.add(r);
    return new Card[]{r};
  }

  public void reset() {
    deck.addAll(removed);
    removed = new ArrayList<Card>();
  }
}