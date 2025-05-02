import java.util.*;

public class Player {
  private String name;
  private int chips;
  private boolean in;
  private static Scanner sc = new Scanner(System.in);
  public Player(String name) {
    if (name.length() > 0)
      this.name = name.substring(0,1).toUpperCase() + name.substring(1);
    else
      this.name = Names.getName();
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

  public static int getValidInt(String message, int min, int max) { // continuously prompt user for valid int given range and message to keep prompting with
    int x;
    while (true) {
      System.out.println(message);
      try {
        String z = sc.nextLine().trim();
        if (z.toLowerCase().trim().equals("q")) System.exit(0);
        x = Integer.parseInt(z);
        if (x >= min && x <= max) break;
        else System.out.print("Not within specified bounds! ");
      } catch (Exception e) {
        System.out.print("Not an integer! ");
        continue;
      }
    }
    return x;
  }
  public static String getValidStr(String message, int min, int max) { // continusoly prompt user for valid string between certain length
    int x;
    String r;
    while (true) {
      System.out.println(message);
      try {
        String z = sc.nextLine().trim();
        if (z.toLowerCase().trim().equals("q")) System.exit(0);
        x = z.length();
        if (x >= min && x <= max) {
          r = z;
          break;
        } else System.out.print("Not within specified length! ");
      } catch (Exception e) {
        continue;
      }
    }
    return r;
  }
  public String toString() {
    return name + ": ✨" + chips;
  }
}