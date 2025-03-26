import java.util.*;

public class PokerPlayer extends Player {
  Card[] hand;
  int status; // 0 - regular
  public PokerPlayer(String name) {
    super(name);
    hand = new Card[2];
    status = 0;
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
}
