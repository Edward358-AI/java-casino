import java.util.*;

public class Player {
  private String name;
  private int chips;
  private Card[] hand;
  private boolean in;
  private int status; // 0 - nothing, 1 - dealer, 2 - sb, 3 - bb
  public Player(String name) {
    this.name = name;
    chips = 1000;
    in = true;
    hand = new Card[2];
    status = 0;
  }
  public String action(String round) {
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
  public int status() {
    return status;
  }
  public void addChips(int chips) {
    this.chips += chips;
  }
  public int removeChips(int chips) {
    this.chips -= chips;
    return chips;
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
  public void setStatus(int i) {
    status = i;
  }
}