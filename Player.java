import java.util.*;

public class Player {
  private String name;
  private int chips;
  private boolean in;
  public static Scanner sc = new Scanner(System.in);

  public Player(String name) {
    if (name.length() > 0)
      this.name = name.substring(0, 1).toUpperCase() + name.substring(1);
    else
      this.name = Names.getUniqueName(null);
    chips = 1000;
    in = true;
  }

  public int[] action() { // length 2
    return null;
  }

  public int getChips() {
    return chips;
  }

  public void setChips(int chips) {
    this.chips = chips;
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
    if (chips > this.chips) {
      int taken = this.chips;
      this.chips = 0;
      return taken;
    }
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
    return getValidInt(message, min, max, false, false);
  }

  public static int getValidInt(String message, int min, int max, boolean allowBack) {
    return getValidInt(message, min, max, allowBack, false);
  }

  public static int getValidInt(String message, int min, int max, boolean allowBack, boolean lastManStanding) { // continuously
                                                                                                                // prompt
                                                                                                                // user
                                                                                                                // for
                                                                                                                // valid
                                                                                                                // int
                                                                                                                // given
                                                                                                                // range
                                                                                                                // and
                                                                                                                // message
                                                                                                                // to
                                                                                                                // keep
                                                                                                                // prompting
                                                                                                                // with
    int x;
    if (!allowBack)
      Utils.flushInput();
    while (true) {
      System.out.println(message + (allowBack ? " [B] to go back" : ""));
      try {
        String z = sc.nextLine().trim();
        if (z.toLowerCase().trim().equals("q"))
          System.exit(0);
        if (allowBack && z.toLowerCase().trim().equals("b"))
          return -1;
        if (z.isEmpty() || !z.matches("-?\\d+")) {
          if (lastManStanding)
            return 0;
          throw new NumberFormatException();
        }
        x = Integer.parseInt(z);
        if (x >= min && x <= max)
          break;
        else
          System.out.print("Not within specified bounds! ");
      } catch (Exception e) {
        System.out.print("Not an integer! ");
        continue;
      }
    }
    return x;
  }

  public static String getValidStr(String message, int min, int max) { // continusoly prompt user for valid string
                                                                       // between certain length
    int x;
    String r;
    while (true) {
      System.out.println(message);
      try {
        String z = sc.nextLine().trim();
        if (z.toLowerCase().trim().equals("q"))
          System.exit(0);
        x = z.length();
        if (x >= min && x <= max) {
          r = z;
          break;
        } else
          System.out.print("Not within specified length! ");
      } catch (Exception e) {
        continue;
      }
    }
    return r;
  }

  public static double getValidDouble(String message, double min, double max, double defaultVal) {
    while (true) {
      System.out.println(message + " [default: " + defaultVal + "]");
      try {
        String z = sc.nextLine().trim();
        if (z.toLowerCase().equals("q"))
          System.exit(0);
        if (z.isEmpty())
          return defaultVal;
        double x = Double.parseDouble(z);
        if (x >= min && x <= max)
          return x;
        else
          System.out.print("Not within specified bounds (" + min + " - " + max + ")! ");
      } catch (Exception e) {
        System.out.print("Not a valid number! ");
      }
    }
  }

  public String toString() {
    return name + ": ✨" + chips;
  }
}