import java.util.*;

// ♠♣♦♥ for testing purposes

public class Main {
  public static void main(String[] args) {
    Casino casino = new Casino();
    casino.start();

    // testing for pots, do not delete
    // PokerPlayer[] d = {new PokerPlayer("zuowen"), new PokerBot(), new PokerBot(), new PokerBot(), new PokerBot()};
    // for (int i = 0; i < d.length-1; i++) d[i].removeChips(d[i].getChips());
    // System.out.println(Arrays.toString(d));
    // PokerPot p = new PokerPot(d);
    // p.setCont(new int[]{50, 50, 100, 100, 200}); 
    // System.out.println(p.toString());
  }
}