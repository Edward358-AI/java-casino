import java.util.*;

// ♠♣♦♥ for testing purposes

public class Main {
  public static void main(String[] args) {
    PokerDeck h1 = new PokerDeck(); // testing purposes only, temporary
    Blackjack game = new Blackjack(new BJPlayer("player"));
    Deck d = new Deck();
    PokerPlayer[] p = {new PokerPlayer("edward"), new PokerBot(), new PokerBot(), new PokerBot(), new PokerBot()};
    PokerGame poker = new PokerGame(p);
    poker.init();
  }
}