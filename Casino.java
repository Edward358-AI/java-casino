import java.util.*;

public class Casino { // will operate blackjack/poker games
  private PokerGame p; // poker game object
  private Blackjack bj; // blackjack game object
  private Scanner sc = new Scanner(System.in);
  private PlayerStat poker; // universal poker stats tracking
  private PlayerStat black; // universal blackjack statistics tracking
  private int chips = -1; // keeps track of universal chips
  private String name; // keeps track of name

  public void start() {
    poker = new PlayerStat(0);
    black = new PlayerStat(0);
    while (chips != 0) {
      Utils.clearScreen();
      System.out.println(
          "Welcome to ♠️ Aventurine's Adventures♠️ , " + name + " !\nA lifetime of opportunities awaits! We offer the highest quality blackjack and Texas Hold'em poker tables only.\n\nNOTE: Any and all currency used is completely fictional. We highly discourage underage gambling.\nIf you have any questions, please contact the casino owners, Sammie Z and Edward J.\n\nYou have "
              + (chips == -1 ? 0 : chips)
              + "✨ primogems.");
      if (chips == -1)
        chips = Player.getValidInt("What is your buy-in (in primogems ✨, 1000 recommended): [500-1000]", 500, 1000);
      if (name == null)
        name = Player.getValidStr("What is your name? (3-20 char limit)", 3, 20);
      System.out.println("\nWhat would you like to do? Remember, type \"q\" anytime to quit the program!");
      int game = Player.getValidInt("[1] Poker [2] Blackjack [3] Help [4] Exit Casino", 1, 4);
      if (game == 4)
        break;
      if (game == 3) {
        System.out.println(
            "\nOur casino offers two games where you can play to your heart's content, Blackjack and No Limit Hold'em!\n\nOur rules of blackjack are the same as the tried and true classic favorite, blackjack, but with some modifications. Firstly, we don't have any special rules like double down, split, or insurance. We do have the option to surrender the hand though. Everything else you can expect to be the same! Ties will go to the dealer because those are house rules, except in the case of a blackjack tie in which the bet is returned to both players.\n\nOur No Limit Hold'em is practically the exact same as the official rules. We don't play with jokers, for your information. Our tables are extremely lively and popular, and your table may have as low as 5 people but maybe as high as 10 people playing.\n\nNow that you know our house rules, it's time to get out there and play! What do you say?\nPress enter to return to casino:");
        sc.nextLine();
        continue;
      }
      if (game == 1) {
        PokerPlayer mainPlayer = new PokerPlayer(name);
        mainPlayer.removeChips(mainPlayer.getChips());
        mainPlayer.addChips(chips);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        int x = (int) (Math.random() * (10 - 4 + 1) + 4);
        PokerPlayer[] ps = new PokerPlayer[x];
        ps[0] = mainPlayer;
        for (int i = 1; i < x; i++)
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
    System.out.println("Thank you " + name
        + ", for playing at ♠️ Aventurine's Adventures♠️ ! Here are your final statistics:\nFinal primogems: " + chips
        + "✨");
    Utils.sleep(1000);
    System.out.println("\nPoker Stats:");
    System.out.println(poker);
    Utils.sleep(1000);
    System.out.println("\nBlackjack Stats:");
    System.out.println(black);
  }
}
