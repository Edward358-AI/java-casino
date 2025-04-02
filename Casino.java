import java.util.*;

public class Casino { // will operate blackjack/poker games
  private PokerGame p;
  private Blackjack bj;
  public Casino() {
    
  }

  public void start() {
    System.out.println("Welcome to ♠Aventurine's Adventures♠!\nA lifetime of gambling opportunities awaits! We offer the highest quality blackjack and Texas Hold'em poker tables only. What game would you like to play?");
    int game = Player.getValidInt("[1] Poker [2] Blackjack", 1, 2);
  }
}
