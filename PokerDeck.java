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
    return Arrays.copyOf(board, 5);
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
      } else {
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
          } else
            rank = 6;
          break;
        case 2:
          if (modes[3] == 2) {
            rank = 7;
          } else
            rank = 8;
          break;
      }
    } else {
      boolean flush = true;
      boolean straight = true;
      for (int i = 1; i < 5; i++) {
        if (numhand[i] != numhand[i - 1] + 1)
          straight = false;
        if (hand[i].getValue().charAt(1) != hand[i - 1].getValue().charAt(1))
          flush = false;
      }
      rank = (flush && straight) ? 1 : ((flush || straight) ? ((flush) ? 5 : 6) : 9);
    }
    return rank;
  }

  public int compareHands(Card[] h1, Card[] h2) {
    int H1 = this.getRanking(h1);
    int H2 = this.getRanking(h2);
    int[] num1 = new int[5];
    int[] num2 = new int[5];
    for (int i = 0; i < 5; i++) {
      num1[i] = h1[i].getNum();
      num2[i] = h2[i].getNum();
    }
    Arrays.sort(num1);
    Arrays.sort(num2);
    Integer[] useMode = { 2, 3, 6, 7, 8 };

    if (H1 < H2)
      return 1;
    else if (H2 < H1)
      return 2;
    else {
      if (!Arrays.equals(num1, num2)) {
        if (Arrays.asList(useMode).contains((Integer) H1)) {
          if (H1 != 7 && H1 != 8) {
            if (mode(num1) != mode(num2))
              return (mode(num1) > mode(num2)) ? 1 : 2;
            else
              return 0;
          } else if (H1 == 7) {
            int[] mode1 = new int[2];
            int[] mode2 = new int[2];
            int[] sNum1 = new int[3];
            int[] sNum2 = new int[3];
            int i1 = 0;
            int i2 = 0;
            int m1 = mode(num1);
            int m2 = mode(num2);
            mode1[0] = m1;
            mode2[0] = m2;
            for (int i = 0; i < 5; i++) {
              if (num1[i] != m1) {
                sNum1[i1] = num1[i];
                i1++;
              }
              if (num2[i] != m2) {
                sNum2[i2] = num2[i];
                i2++;
              }
            }
            mode1[1] = mode(sNum1);
            mode2[1] = mode(sNum2);
            Arrays.sort(mode1);
            Arrays.sort(mode2);
            if (!Arrays.equals(mode1, mode2)) {
              return (mode1[0] > mode2[0] || mode1[1] > mode2[1]) ? 1 : 2;
            } else {
              int last1 = 0;
              int last2 = 0;
              for (int i = 0; i < 3; i++) {
                if (sNum1[i] != mode1[0] || sNum1[i] != mode1[1])
                  last1 = sNum1[i];
                if (sNum2[i] != mode2[0] || sNum2[i] != mode2[1])
                  last2 = sNum2[i];
              }
              return (last1 != last2) ? ((last1 > last2) ? 1 : 2) : 0;
            }
          } else {
            int m1 = mode(num1);
            int m2 = mode(num2);
            if (m1 == m2) {
              int r = 0;
              for (int i = 4; i >= 0; i--) {
                if (num1[i] > num2[i]) {
                  r = 1;
                  break;
                } else if (num1[i] < num2[i]) {
                  r = 2;
                  break;
                }
              }
              return r;
            } else {
              return (m1 > m2) ? 1 : 2;
            }
          }
        } else {
          int r = 0;
          for (int i = 4; i >= 0; i--) {
            if (num1[i] > num2[i]) {
              r = 1;
              break;
            } else if (num1[i] < num2[i]) {
              r = 2;
              break;
            }
          }
          return r;
        }
      } else
        return 0;
    }
  }

  private int mode(int[] j) { // returns the mode of int list, assuming j is already sorted
    int mode = j[0];
    int[] k = new int[j.length];
    k[0] = 1;
    int currIndex = 0;
    for (int i = 1; i < j.length; i++) {
      if (j[i - 1] == j[i]) {
        k[currIndex]++;
        if (currIndex > 0) {
          boolean greater = true;
          for (int l = 0; l < currIndex; l++) {
            if (k[currIndex] < k[l])
              greater = false;
          }
          mode = greater ? j[i] : mode;
        }
      } else {
        currIndex++;
      }
    }
    return mode;
  }
}