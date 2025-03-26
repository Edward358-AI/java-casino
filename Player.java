import java.util.*;

public class Player {
  private String name;
  private int chips;
  private boolean in;
  public Player(String name) {
    this.name = name;
    chips = 1000;
    in = true;
  }
  public Object[] action()  {
    return null;
  }

  public int getChips() {
    return chips;
  }
  public String getName() {
    return name;
  }
  
  public boolean inHand() {
    return in;
  }
  public void addChips(int chips) {
    this.chips += chips;
  }
  public int removeChips(int chips) {
    this.chips -= chips;
    return chips;
  }
  public void setName(String name) {
    this.name = name;
  }
  public void setInHand(boolean in) {
    this.in = in;
  }
}