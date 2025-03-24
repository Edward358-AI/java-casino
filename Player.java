import java.util.*;

public class Player {
  private String name;
  private int chips;
  private Card[] hand;
  public Player(String name) {
    this.name = name;
    chips = 1000;
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
  public void setChips(int chips) {
    this.chips = chips;
  }
  public void setHand(Card[] hand) {
    this.hand = hand;
  }
  public void setName(String name) {
    this.name = name;
  }
}