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

  public static int getValidInt(String message) {
    Scanner sc = new Scanner(System.in);
    int x;
    while (true) {
      System.out.println(message);
      try {
        x = sc.nextInt();
        break;
      } catch (InputMismatchException e) {
        continue;
      }
    }
    sc.close();
    return x;
  }
  public static int getValidInt(String message, int min, int max) {
    Scanner sc = new Scanner(System.in);
    int x;
    while (true) {
      System.out.println(message);
      try {
        x = sc.nextInt();
        if (x >= min && x <= max) break;
        else continue;
      } catch (InputMismatchException e) {
        continue;
      }
    }
    sc.close();
    return x;
  }
}