import java.util.*;

public class Player {
  private String name;
  private int chips;
  private Card[] hand;
  private boolean in;
  public Player(String name) {
    this.name = name;
    chips = 1000;
    in = true;
    hand = new Card[2];
  }
  public String action() {
    return "";
  }

  public int getChips() {
    return chips;
  }
  public String getName() {
    return name;
  }
  public Card[] getHand() {
    return hand;
  }
  public boolean inHand() {
    return in;
  }
  public void setChips(int chips) {
    this.chips = chips;
  }
  public void setHand(Card[] hand) {
    this.hand = hand;
  }
  public void setName(String name) {
    this.name = name;
  }
  public void setInHand(boolean in) {
    this.in = in;
  }
}