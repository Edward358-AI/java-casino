import java.util.*;

public class Casino { // will operate blackjack/poker games
  private PokerGame p;
  private Blackjack bj;
  private Scanner sc = new Scanner(System.in);
  private PlayerStat poker;
  private PlayerStat black;
  private int chips = -1;

  public void start() {
    poker = new PlayerStat(0);
    black = new PlayerStat(0);
    while (chips != 0) {
      Utils.clearScreen();
      System.out.println(
          "Welcome to ♠️ Aventurine's Adventures♠️ !\nA lifetime of opportunities awaits! We offer the highest quality blackjack and Texas Hold'em poker tables only.\n\nNOTE: Any and all currency used is completely fictional. We highly discourage underage gambling.\n\nYou have "
              + (chips == -1 ? 0 : chips)
              + "✨ primogems.\nWhat game would you like to play? Remember, type \"q\" anytime to quit the program!");
      int game = Player.getValidInt("[1] Poker [2] Blackjack [3] Exit Casino", 1, 3);
      if (game == 3)
        break;
      if (chips == -1)
        chips = Player.getValidInt("What is your buy-in (in primogems ✨, 1000 recommended): [500-1000]", 500, 1000);
      String name = Player.getValidStr("What is your name? (3-20 char limit)", 3, 20);
      if (game == 1) {
        PokerPlayer mainPlayer = new PokerPlayer(name);
        mainPlayer.removeChips(mainPlayer.getChips());
        mainPlayer.addChips(chips);
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
        poker.merge(p.getStats());
        chips = p.getStats().getChips();
        if (chips != 0) {
          System.out.println("We hope you enjoyed our blackjack tables! Would you like to play anything else? [y/n]");
          String s = sc.nextLine().strip().toLowerCase();
          if (s.equals("n") || s.equals("no") || s.equals("nah")) {
            break;
          }
        }
      } else {
        BJPlayer m = new BJPlayer(name);
        m.removeChips(m.getChips());
        m.addChips(chips);
        bj = new Blackjack(m);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        bj.initialize();
        black.merge(bj.getStat());
        chips = bj.getStat().getChips();
        if (chips != 0) {
          System.out.println("We hope you enjoyed our blackjack tables! Would you like to play anything else? [y/n]");
          String s = sc.nextLine().strip().toLowerCase();
          if (s.equals("n") || s.equals("no") || s.equals("nah")) {
            break;
          }
        }
      }
    }
    Utils.clearScreen();
    System.out.println("Thanks for playing at ♠️ Aventurine's Adventures♠️ ! Here are your final statistics:");
    Utils.sleep(1000);
    System.out.println("\nPoker Stats:");
    System.out.println(poker);
    Utils.sleep(1000);
    System.out.println("\nBlackjack Stats:");
    System.out.println(black);
  }
}
