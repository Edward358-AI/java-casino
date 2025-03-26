import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int[] blinds = {0, 1, 2}; // current blinds positions
  private int[] bbSize = {10, 20}; // current blinds size

  public PokerGame(PokerPlayer[] players) {
    this.players = players;
  }

  public void init() {
    preflop();
  }

  public void preflop() { // code to execute preflop
    Card[][] holeCards = p.deal(players.length);
    p.addChips(players[blinds[1]].removeChips(bbSize[0]));
    p.addChips(players[blinds[2]].removeChips(bbSize[1]));
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