import java.util.*;

class PokerGame {
  PokerDeck p = new PokerDeck();
  Player[] players;

  public PokerGame(Player[] players) {
    this.players = new Player[players.length];
    this.players = players;

  }
}