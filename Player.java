import java.util.*;

public class Player {
  private String name;
  private int chips;
  private boolean in;
  private static Scanner sc = new Scanner(System.in);
  public Player(String name) {
    this.name = name.substring(0,1).toUpperCase() + name.substring(1);
    chips = 1000;
    in = true;
  }
  public int[] action() { // length 2
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

  public static int getValidInt(String message, int min, int max) {
    int x;
    while (true) {
      System.out.println(message);
      try {
        String z = sc.nextLine().trim();
        x = Integer.parseInt(z);
        if (x >= min && x <= max) break;
      } catch (Exception e) {
        continue;
      }
    }
    return x;
  }
}