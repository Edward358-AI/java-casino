import java.util.*;

public class Casino { // will operate blackjack/poker games
  private PokerGame p;
  private Blackjack bj;
  private Scanner sc = new Scanner(System.in);

  public void start() {
    System.out.println("Welcome to ♠️ Aventurine's Adventures♠️ !\nA lifetime of opportunities awaits! We offer the highest quality blackjack and Texas Hold'em poker tables only.\nNote: Any and all currency used is completely fictional. We highly discourage underage gambling.\nWhat game would you like to play?");
    int game = Player.getValidInt("[1] Poker [2] Blackjack", 1, 2);
    int buyIn = Player.getValidInt("What is your buy-in (in primogems ✨): [100-1000]", 100, 1000);
    System.out.println("What is your name?");
    String name = sc.nextLine();
    if (name.length() == 0) System.out.println("You have not entered anything, a random name will be chosen for you!");
    if (game == 1) {
      PokerPlayer mainPlayer = new PokerPlayer(name);
      if (name.length() == 0) System.out.println("Your name will be " + mainPlayer.getName().toUpperCase() + " for this game!");
      mainPlayer.removeChips(mainPlayer.getChips());
      mainPlayer.addChips(buyIn);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      p = new PokerGame(new PokerPlayer[]{mainPlayer, new PokerBot(), new PokerBot(), new PokerBot(), new PokerBot()});
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
