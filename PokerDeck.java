import java.util.*;

class PokerDeck extends Deck {
  private int pot;
  private Card[] board;
  public PokerDeck() {
    super();
    board = new Card[5];
  }
  public void reset() {
    super.reset();
    pot = 0;
    board = new Card[5];
  }
  public int getPot() {
    return pot;
  }
  public void addChips(int c) {
    pot += c;
  }
  public Card[] deal() {
    for (int i = 0; i < 5; i++) {
      board[i] = super.deal()[0];
    }
    return board;
  }
  public Card[][] deal(int players) {
    Card[][] hands = new Card[players][2];
    for (int i = 0; i < players; i++) {
      for (int j = 0; j < 2; j++) {
        hands[i][j] = super.deal()[0];
      }
    }
    return hands;
  }
  public int getRanking(Card[] hand) {
    int[] numhand = new int[5];
    int rank = 0;
    for (int i = 0; i < 5; i++) {
      numhand[i] = hand[i].getNum();
    }
    Arrays.sort(numhand);
    int[] modes = new int[5];
    int currMode = 0;
    modes[0]++;
    boolean containsDupes = false;
    for (int i = 1; i < 5; i++) { // check duplicates
      if (numhand[i] == numhand[i - 1]) {
        modes[currMode]++;
        containsDupes = true;
      }
      else {
        currMode++;
        modes[currMode]++;
      }
    }
    Arrays.sort(modes);
    if (containsDupes) {
      switch (modes[4]) {
        case 4:
          rank = 2;
          break;
        case 3:
          if (modes[3] == 2) {
            rank = 3;
          } else rank = 6;
          break;
        case 2:
          if (modes[3] == 2) {
            rank = 7;
          } else rank = 8;
          break;
      }
    } else {
      boolean flush = true;
      boolean straight = true;
      for (int i = 1; i < 5; i++) {
        if (numhand[i] != numhand[i - 1] + 1) straight = false;
        if (hand[i].getValue().charAt(1) != hand[i - 1].getValue().charAt(1)) flush = false;
      }
      rank = (flush && straight) ? 1 : ((flush || straight) ? ((flush) ? 5 : 6) : 9);
    }
    return rank; 
  }
}