import java.util.*;

class PokerDeck extends Deck { // a deck specifically designed to facilitate a game of poker
  private Card[] board;

  public PokerDeck() {
    super();
    board = new Card[5];
  }

  public Card[] getBoard() {
    return board.clone();
  }

  public void reset() { // resets for new hand
    super.reset();
    board = new Card[5];
  }

  public Card[] deal() { // deals out the cards required for the board
    for (int i = 0; i < 5; i++) {
      board[i] = super.deal()[0];
    }
    return board.clone();
  }

  public Card[][] deal(int players) { // deals out cards to int players players
    Card[][] hands = new Card[players][2];
    for (int i = 0; i < players; i++) {
      for (int j = 0; j < 2; j++) {
        hands[i][j] = super.deal()[0];
      }
    }
    return hands;
  }

  public int getRanking(Card[] hand) { // gets the ranking of a 5 card poker hand
    /* 
     * Rankings:
     * 1 - straight flush
     * 2 - 4 of a kind
     * 3 - full house
     * 4 - flush
     * 5 - straight
     * 6 - 3 of a kind
     * 7 - 2 pair
     * 8 - 1 pair
     * 9 - high card
     */
    int[] numhand = Deck.cardToInt(hand);
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
      rank = (flush && straight) ? 1 : ((flush || straight) ? ((flush) ? 4 : 5) : 9);
    }
    return rank;
  }

  public Card[] getBestHand(Card[] h) { // gets the best possible hand given a players hand and the community cards
    // use board and player's hand
    ArrayList<Card> hand = new ArrayList<Card>(Arrays.asList(h));
    hand.addAll(Arrays.asList(board));
    int currRank = Integer.MAX_VALUE;
    ArrayList<ArrayList<Card>> currHand = new ArrayList<ArrayList<Card>>();
    // We need to choose 5 cards out of 7 (C(7,5) = 21 combinations) (brute force XD)
    for (int i = 0; i < hand.size(); i++) {
      for (int j = i + 1; j < hand.size(); j++) {
        for (int k = j + 1; k < hand.size(); k++) {
          for (int l = k + 1; l < hand.size(); l++) {
            for (int m = l + 1; m < hand.size(); m++) {
              ArrayList<Card> combo = new ArrayList<Card>();
              combo.add(hand.get(i));
              combo.add(hand.get(j));
              combo.add(hand.get(k));
              combo.add(hand.get(l));
              combo.add(hand.get(m));
              if (getRanking(combo.toArray(new Card[5])) < currRank) {
                currHand = new ArrayList<ArrayList<Card>>();
                currHand.add(combo);
                currRank = getRanking(combo.toArray(new Card[5]));
              } else if (getRanking(combo.toArray(new Card[5])) == currRank) currHand.add(combo);
            }
          }
        }
      }
    }
    // the above code finds hands only by rank, but does not necessarily compare within the same rank, that's why we have this to compare them within the same rank because there can only be one possible best hand

    // i.e. the above code might return all the possible full houses if that's the best, but we need to find the best full house out of all of the full houses, thats what this does
    Card[] bestHand = currHand.get(0).toArray(new Card[5]);
    for (int i = 1; i < currHand.size(); i++) {
      if (compareHands(bestHand, currHand.get(i).toArray(new Card[5])) == 2) bestHand = currHand.get(i).toArray(new Card[5]);
    }
    return bestHand;
  }
  
  public Card[] getBestHand(Card[] h, Card[] b) { // gets the best possible hand given a players hand and custom board
    ArrayList<Card> hand = new ArrayList<Card>(Arrays.asList(h));
    hand.addAll(Arrays.asList(b));
    int currRank = Integer.MAX_VALUE;
    ArrayList<ArrayList<Card>> currHand = new ArrayList<ArrayList<Card>>();
    for (int i = 0; i < hand.size(); i++) {
      for (int j = i + 1; j < hand.size(); j++) {
        for (int k = j + 1; k < hand.size(); k++) {
          for (int l = k + 1; l < hand.size(); l++) {
            for (int m = l + 1; m < hand.size(); m++) {
              ArrayList<Card> combo = new ArrayList<Card>();
              combo.add(hand.get(i));
              combo.add(hand.get(j));
              combo.add(hand.get(k));
              combo.add(hand.get(l));
              combo.add(hand.get(m));
              if (getRanking(combo.toArray(new Card[5])) < currRank) {
                currHand = new ArrayList<ArrayList<Card>>();
                currHand.add(combo);
                currRank = getRanking(combo.toArray(new Card[5]));
              } else if (getRanking(combo.toArray(new Card[5])) == currRank) currHand.add(combo);
            }
          }
        }
      }
    }
    Card[] bestHand = currHand.get(0).toArray(new Card[5]);
    for (int i = 1; i < currHand.size(); i++) {
      if (compareHands(bestHand, currHand.get(i).toArray(new Card[5])) == 2) bestHand = currHand.get(i).toArray(new Card[5]);
    }
    return bestHand;
  }

  public int compareHands(Card[] h1, Card[] h2) { // compares two poker hands
    // returns 1 if the first one is better, 2 if the second one better, 0 if theyre the are the exact same
    int H1 = this.getRanking(h1);
    int H2 = this.getRanking(h2);
    int[] num1 = Deck.cardToInt(h1);
    int[] num2 = Deck.cardToInt(h2);
    Arrays.sort(num1);
    Arrays.sort(num2);
    Integer[] useMode = { 2, 3, 6, 7, 8 }; // see the rankings for getRankings
    // ^ these are the types of hands that require checking the most reoccuring card

    if (H1 < H2) // these are just simple comparisons by rank
      return 1;
    else if (H2 < H1)
      return 2;
    else {
      if (!Arrays.equals(num1, num2)) { // checks if the arrays are equal to immediately determine if needs to return 0
        if (Arrays.asList(useMode).contains((Integer) H1)) { 
            if (mode(num1) != mode(num2))
              return (mode(num1) > mode(num2)) ? 1 : 2;
            else {
              if (H1 == 7) { // compares two 2-pair hands
                int[] mode1 = new int[2]; // stores the pairs for hand 1
                int[] mode2 = new int[2]; // stores the pairs for hand 2
                int[] sNum1 = new int[3]; // stores hand 1 after removing the first pair, temporary
                int[] sNum2 = new int[3]; // stores hand 2 after removing the first pair, temporary
                // below code acquires the 2 pairs in each hand
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
                //sorts mode arrays to check if theyre equal
                Arrays.sort(mode1);
                Arrays.sort(mode2);
                if (!Arrays.equals(mode1, mode2)) {
                  return (mode1[0] > mode2[0] || mode1[1] > mode2[1]) ? 1 : 2; // if the pairs are not same, compare them directly
                } else { // else, check the "kicker" card, or the last card to determine equality
                  int last1 = 0;
                  int last2 = 0;
                  for (int i = 0; i < 3; i++) {
                    if (sNum1[i] != mode1[0] && sNum1[i] != mode1[1])
                      last1 = sNum1[i];
                    if (sNum2[i] != mode2[0] && sNum2[i] != mode2[1])
                      last2 = sNum2[i];
                  }
                  return (last1 != last2) ? ((last1 > last2) ? 1 : 2) : 0;
                }
              } else { // compares any other pair/set hands (1 pair, 3 of a kind, 4 of a kind, full house)
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
            }
        } else { // compares any non pair/set related hands, like flushes, straights, straight flush, high card
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