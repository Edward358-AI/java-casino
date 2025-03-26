import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int[] bbSize = {10, 20}; // current blinds size

  public PokerGame(PokerPlayer[] players) {
    this.players = players;
  }

  public void init() {
    preflop();
  }

  public void preflop() { // code to execute preflop
    Card[][] holeCards = p.deal(players.length);
    p.addChips(players[0].removeChips(bbSize[0]));
    p.addChips(players[1].removeChips(bbSize[1]));
    for (int i = 0; i < holeCards.length; i++) {
      players[i].setHand(holeCards[i]);
      // do some stuff based on action
    }
    postflop();
  }

  public void postflop() { // all code to execute postflop, including flop, turn and river
    Card[] b = p.deal();
    for (int i = 0; i < players.length; i++) { // code for the flop
      // do some stuff based on action
    }
    for (int i = 0; i < players.length; i++) { // code for the turn
      // do some stuff based on action
    }
    for (int i = 0; i < players.length; i++) { // code for the river
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
    PokerPlayer first = players[0];
    for (int i = 1; i < players.length; i++) {
      players[i-1] = players[i];
    }
    players[players.length-1] = first;
    for (int i = 0; i < players.length; i++) {
      players[i].setInHand(true);
    }
  }
}