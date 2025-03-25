import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private Player[] players;
  private int[] blinds = {0, 1, 2};
  private int[] bbSize = {10, 20};

  public PokerGame(Player[] players) {
    this.players = new Player[players.length];
    this.players = players;
  }

  public void init() {
    preflop();
  }

  public void preflop() {
    Card[][] holeCards = p.deal(players.length);
    p.addChips(players[blinds[1]].removeChips(bbSize[0]));
    p.addChips(players[blinds[2]].removeChips(bbSize[1]));
    for (int i = 0; i < holeCards.length; i++) {
      players[i].setHand(holeCards[i]);
      String action = players[i].action("preflop");
      // do some stuff based on action
    }
    postflop();
  }

  public void postflop() {
    Card[] b = p.deal();
    for (int i = 0; i < players.length; i++) {
      String action;
      if (players[i].inHand()) action = players[i].action("flop");
      // do some stuff based on action
    }
    for (int i = 0; i < players.length; i++) {
      String action;
      if (players[i].inHand()) action = players[i].action("turn");
      // do some stuff based on action
    }
    for (int i = 0; i < players.length; i++) {
      String action;
      if (players[i].inHand()) action = players[i].action("river");
      // do some stuff based on action
    }
    showdown();
  }

  public void showdown() {
    // award chips and designate winner 
    newHand();
  }

  public void newHand() {
    p.reset();
    for (int i = 0; i < players.length; i++) {
      players[i].setInHand(true);
    }
    players[blinds[0]].setStatus(0);
    players[blinds[1]].setStatus(0);
    players[blinds[2]].setStatus(0);
    for (int i = 0; i < 3; i++) {
      if (blinds[i] == players.length - 1) {
        blinds[i] = 0;
        if (i == 0) {
          bbSize[0] *= 2;
          bbSize[1] *= 2;
        }
      }
      else blinds[i]++;
    }
    players[blinds[0]].setStatus(1);
    players[blinds[1]].setStatus(2);
    players[blinds[2]].setStatus(3);
  }
}