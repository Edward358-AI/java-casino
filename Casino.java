import java.util.*;

public class Casino { // will operate blackjack/poker games
  private PokerGame p;
  private Blackjack bj;
  private Scanner sc = new Scanner(System.in);

  public void start() {
    Utils.clearScreen();
    System.out.println(
        "Welcome to ♠️ Aventurine's Adventures♠️ !\nA lifetime of opportunities awaits! We offer the highest quality blackjack and Texas Hold'em poker tables only.\n\nNOTE: Any and all currency used is completely fictional. We highly discourage underage gambling.\n\nWhat game would you like to play? Remember, type \"q\" anytime to quit the program!");
    int game = Player.getValidInt("[1] Poker [2] Blackjack", 1, 2);
    int buyIn = Player.getValidInt("What is your buy-in (in primogems ✨, 1000 recommended): [500-1000]", 500, 1000);
    String name = Player.getValidStr("What is your name? (3-20 char limit)", 3, 20);
    if (game == 1) {
      PokerPlayer mainPlayer = new PokerPlayer(name);
      mainPlayer.removeChips(mainPlayer.getChips());
      mainPlayer.addChips(buyIn);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PokerPlayer[] ps = new PokerPlayer[6];
      ps[0] = mainPlayer;
      for (int i = 1; i < 6; i++)
        ps[i] = new PokerBot();
      p = new PokerGame(ps);
      p.init();
    } else {
      BJPlayer m = new BJPlayer(name);
      m.removeChips(m.getChips());
      m.addChips(buyIn);
      bj = new Blackjack(m);
      bj.initialize();
    }
  }
}
