import java.util.*;

public class BJPlayer extends Player {
  private ArrayList<Card> hand = new ArrayList<Card>(); // getAction

  public BJPlayer(String name) {
    super(name);
  }

  public ArrayList<Card> getHand() {
    return hand;
  }

  public void add(Card c) {
    hand.add(c);
  }

  public void clear() {
    hand.clear();
  }

  public int[] action(int prevBet) { // if hand.size() == 0 prompt for bet
    // else ask him for regular action and display his hand
    int[] out = new int[2]; // {a, b}
    /*
     * a: 0 - bet, 1 - hit, 2 - stand, 3 - surrender
     * b: bet (if applicable), otherwise js 0
     */
    // 0 betting
    // 1 dealing
    // 2 player's turn // can only hit, stand, or surrender (lose half bet continue
    // to next round), no double down so bet is not made into a method
    // 3 dealer's turn
    // external method to check if win
    if (hand.size() == 0) {
      out[0] = 0;
      out[1] = BJPlayer.getValidInt("Current stack: " + getChips() + "✨\nPlace your bet: (50-" + getChips() + "✨)", 50, getChips());
      // System.out.println(out[0]);
      return out;
    } else {
      System.out.println("What will you do?");
      out[0] = BJPlayer.getValidInt("[1] Hit [2] Stand [3] Surrender(-50% bet)", 1, 3);
      return out;
    }
    // System.out.println("Your hand: " + hand[0] + " " + hand[1]);
  }

  public void dispHand() {
    System.out.println(); // start to display cards each person has
    System.out.println(getName() + ":");
    for (Card card : hand) {
      System.out.print(card.getValue() + "  ");
    }
    System.out.println("\n");
  }
}
