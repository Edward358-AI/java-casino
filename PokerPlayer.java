import java.util.*;

public class PokerPlayer extends Player {
  Card[] hand;
  int status; // -1 regular, 0 dealer, 1 sb, 2 bb
  Scanner s = new Scanner(System.in);
  public PokerPlayer(String name) {
    super(name);
    hand = new Card[2];
    status = -1;
  }

  public Card[] getHand() {
    return hand;
  }

  public int status() {
    return status;
  }

  public void setHand(Card[] hand) {
    this.hand = hand;
  }

  public void setStatus(int i) {
    status = i;
  }

  public Object[] action(String round, int bet, int blind) {
    Object[] action = new Object[2];
    switch(round) {
      case "preflop":
        break;
      case "flop":
        break;
      case "turn":
        break;
      case "river":
        break;
    }
    return null;
  }
}
