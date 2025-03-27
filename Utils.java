public class Utils {
  public static void clearScreen() {
    System.out.print("\033[H\033[2J"); // fancy code to clear terminal nicely
    System.out.flush();
  }
}
