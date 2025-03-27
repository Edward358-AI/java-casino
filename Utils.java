public class Utils {
  public static void clearScreen() {
    try {
      final String os = System.getProperty("os.name");
      if (os.contains("Windows")) {
          new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
      } else {
          new ProcessBuilder("clear").inheritIO().start().waitFor();
      }
  } catch (final Exception e) {
      System.out.println("Exception"+e);
  }
  }
}
