public class Utils {
  public static void clearScreen() {
    try {
      final String os = System.getProperty("os.name");
      if (os.contains("Windows")) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
      } else {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
      }
    } catch (final Exception e) {
      System.out.println("Exception: " + e);
    }
  }
  public static void sleep(int time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
