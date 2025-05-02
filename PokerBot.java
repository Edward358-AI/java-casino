import java.util.*;

public class PokerBot extends PokerPlayer {
  private boolean opMode = false;
  private PokerDeck p = new PokerDeck();
  private int[][] hands = { { 14, 14 }, { 14, 13 }, { 14, 12 }, { 14, 11 }, { 14, 10 }, { 14, 9 }, { 14, 8 }, { 14, 2 },
      { 13, 13 }, { 13, 12 }, { 13, 11 }, { 13, 10 }, { 13, 9 }, { 13, 8 }, { 12, 12 }, { 12, 11 }, { 12, 10 },
      { 12, 9 }, { 12, 8 }, { 11, 11 }, { 11, 10 }, { 11, 9 }, { 11, 8 }, { 10, 10 }, { 9, 9 }, { 8, 8 }, { 7, 7 },
      { 6, 6 }, { 5, 5 }, { 4, 4 }, { 3, 3 }, { 2, 2 } }; // preset hands for smart bot
  private boolean dumb; // dumb is basically if the bot should js do random stuff or have some decency

  public PokerBot() {
    super("temp");
    randomName();
    dumb = true;
    if (Math.random() > 0.5)
      dumb = false;
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public void randomName() {
    super.setName(Names.getName());
  }

  // funny
  public void checkName() {
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public int[] action(String round, int prevBet, int bet, int blind, Card[] board) {
    if (dumb) { // idiot bot code, fixed percentages for all situations no matter what
      int[] action = new int[2];
      double rand = Math.random();
      if (opMode && super.getChips() > 0) {
        action[0] = 4;
        action[1] = super.getChips();
      } else if (bet < super.getChips()) {
        if (rand >= 0 && rand < 0.75) {
          action[0] = 1;
          if (bet > 0) {
            action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
          } else
            action[1] = (bet == 0) ? 0 : bet - prevBet;
        } else if (rand >= 0.75 && rand < 0.85) {
          if (((bet == 0) ? blind : bet * 2) + super.getChips() / 10 < super.getChips()) {
            int max;
            int min;
            if (round.equals("preflop")) {
              max = super.getChips() / 10 + bet * 2;
              min = bet * 2;
            } else {
              if (bet == 0) {
                max = super.getChips() / 10 + blind;
                min = blind;

              } else {
                max = super.getChips() / 10 + bet * 2;
                min = bet * 2;
              }
            }
            action[0] = 3;
            action[1] = (int) (Math.random() * (max - min + 1) + min);
          } else {
            if (Math.random() > 0.85) {
              action[0] = 4;
              action[1] = super.getChips();
            } else {
              action[0] = 2;

            }
          }
        } else if (rand >= 0.85 && rand < 0.97) {
          action[0] = (bet == 0) ? 1 : 2;
        } else {
          action[0] = 4;
          action[1] = super.getChips();
        }
      } else {
        if (Math.random() > 0.8) {
          action[0] = 4;
          action[1] = super.getChips();
        } else {
          action[0] = 2;

        }
      }
      return action;
    } else { // intelligent bot code, varying percentages based on current siutation
      int[] action = new int[2];
      if (round.equals("preflop")) {
        /*
         * if hand is in the range, plays it only if bet is lower than half of current
         * stack, otherwise, has a 80% chance to call and 20% chance of folding.
         * for the former, 20% chance to raise, 80% chance to call.
         * if hand is out of range, has a 25% chance to call and 75% chance of folding.
         */
        boolean isHand = false;
        for (int[] h : hands) {
          if (Arrays.equals(Deck.cardToInt(super.getHand()), h)) {
            isHand = true;
            if (bet < super.getChips() / 2) {
              if (Math.random() < 0.2) {
                action[0] = 3;
                action[1] = (bet == 0) ? (int) (blind * (Math.random() + 2)) : (int) (bet * (Math.random() + 2));
              } else {
                action[0] = 1;
                if (bet > 0) {
                  action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
                } else
                  action[1] = (bet == 0) ? 0 : bet - prevBet;

              }
            } else {
              if (Math.random() < 0.8) {
                action[0] = 1;
                if (bet > 0) {
                  action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
                } else
                  action[1] = (bet == 0) ? 0 : bet - prevBet;
              } else {
                action[0] = 2;
              }
            }
          }
        }
        if (!isHand) {
          if (bet < super.getChips() / 2 && Math.random() < 0.25) {
            action[0] = 1;
            action[1] = (bet == 0) ? 0 : bet - prevBet;
          } else {
            action[0] = 2;
          }
        }
      } else {
        // three betting settings: 0 - call 1 - bets/raise to 100-150% of current bet, 2
        // - 150-300% of current bet, 3 - 300% to all in. 4 - fold
        // if this has straight or flush: 0 - 25%, 1 - 55%, 2 - 15%, 3 - 5%, 4 - 0%.
        // if full house: 0 - 5%, 1 - 35%, 2 - 45%, 3 - 15%, 4 - 0%.
        // if four of kind or straight flush: 0 - 0%, 1 - 15%, 2 - 25%, 3 - 60%, 4 - 0%.
        // drawing hand (2 pair, 3 o kind, straight/flush draw): 0 - 50%, 1 - 30%, 2 -
        // 5%, 3 - 5%, 4 - 10% [given bet < half of curr stack], otherwise: 0 - 30%, 1 -
        // 15%, 2 - 5%, 3 - 0%, 4 - 50%. replace fold chance with check chance
        // all other hands: 0 - 65%, 1 - 10%, 2 - 5%, 3 - 0%, 4 - 20% [given bet < third
        // of curr stack], otherwise: 0 - 25%, 1 - 5%, 2 - 0%, 3 - 0%, 4 - 70%, replace
        // fold chance with check chance if applicable
        // bot's behavior in a nutshell every time on its turn
        int subAction = -1;
        Card[] hand = new Card[5];
        if (board.length == 3) {
          hand[0] = super.getHand()[0];
          hand[1] = super.getHand()[1];
          for (int i = 0; i < 3; i++)
            hand[i + 2] = board[i];
        } else {
          hand = p.getBestHand(super.getHand(), board);
        }
        int rank = p.getRanking(hand);
        int rand = (int) (Math.random() * 101 + 1);
        if (rank == 5 || rank == 4) {
          if (rand <= 25) {
            subAction = 0;
          } else if (rand > 25 && rand <= 80) {
            subAction = 1;
          } else if (rand > 80 && rand <= 95) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else if (rank == 3) {
          if (rand <= 5) {
            subAction = 0;
          } else if (rand > 5 && rand <= 40) {
            subAction = 1;
          } else if (rand > 40 && rand <= 85) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else if (rank < 3) {
          if (rand <= 15) {
            subAction = 1;
          } else if (rand > 15 && rand <= 40) {
            subAction = 2;
          } else {
            subAction = 3;
          }
        } else {
          Card[] temp = new Card[2 + board.length];
          temp[0] = super.getHand()[0];
          temp[1] = super.getHand()[1];
          for (int i = 0; i < board.length; i++)
            temp[i + 2] = board[i];
          boolean[] draw = draw(temp);
          if (rank == 6 || rank == 7 || ((draw[0] || draw[1]) && board.length < 3)) {
            if (bet < super.getChips() / 2) {
              if (rand <= 50) {
                subAction = 0;
              } else if (rand > 50 && rand <= 80) {
                subAction = 1;
              } else if (rand > 80 && rand <= 85) {
                subAction = 2;
              } else if (rand > 85 && rand <= 90) {
                subAction = 3;
              } else {
                subAction = 4;
              }
            } else {
              if (rand <= 30) {
                subAction = 0;
              } else if (rand > 30 && rand <= 45) {
                subAction = 1;
              } else if (rand > 45 && rand <= 50) {
                subAction = 2;
              } else {
                subAction = 4;
              }
            }
          } else {
            if (bet < super.getChips() / 3) {
              if (rand <= 65) {
                subAction = 0;
              } else if (rand > 65 && rand <= 75) {
                subAction = 1;
              } else if (rand > 75 && rand <= 80) {
                subAction = 2;
              } else {
                subAction = 4;
              }
            } else {
              if (rand <= 25) {
                subAction = 0;
              } else if (rand > 25 && rand <= 30) {
                subAction = 1;
              } else {
                subAction = 4;
              }
            }
          }
        }
        boolean zeroBet = false;
        if (bet == 0) {
          bet = blind;
          zeroBet = true;
        }
        switch (subAction) {
          case 0:
            action[0] = 1;
            if (!zeroBet) {
              action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
            } else
              action[1] = (zeroBet) ? 0 : bet - prevBet;
            break;
          case 1:
            action[0] = 3;
            action[1] = (int) ((Math.random() * .6 + 2) * bet);
            break;
          case 2:
            action[0] = 3;
            action[1] = (int) ((Math.random() * 1.6 + 2.5) * bet);
            break;
          case 3:
            double upper = (double) super.getChips() / bet;
            action[0] = 3;
            action[1] = (int) ((Math.random() * (upper - 2 + 0.1) + 4) * bet);
            break;
          case 4:
            if (!zeroBet)
              action[0] = 2;
            else
              action[0] = 1;
            break;
        }
      }
      if (action[1] >= super.getChips()) {
        action[0] = 4;
        action[1] = super.getChips();
      }
      return action;
    }
  }

  public boolean[] draw(Card[] total) {
    // checks to see if the combined hand and board contains a draw, i.e. 4 cards in
    // a straight or 4 cards in a flush (draw for full house is js two pair or three
    // of a kind)
    Deck.sort(total);
    int straightCount = 0;
    for (int i = 1; i < total.length; i++) {
      if (straightCount < 4) {
        if (total[i].getNum() == total[i - 1].getNum() + 1)
          straightCount++;
        else
          straightCount = 0;
      } else {
        break;
      }
    }
    int[] flushCount = new int[4]; // indexing: 0 - spades, 1 - clubs, 2 - diamonds, 3 - hearts
    boolean flushDraw = false;
    for (Card d : total) {
      switch (d.getValue().substring(1, 2)) {
        case "♠️":
          flushCount[0]++;
          break;
        case "♣️":
          flushCount[1]++;
          break;
        case "♦️":
          flushCount[2]++;
          break;
        case "♥️":
          flushCount[3]++;
          break;
      }
    }
    for (int i : flushCount)
      if (i >= 4)
        flushDraw = true;
    return new boolean[] { straightCount > 3, flushDraw };
  }
}

class Names { // avoid dupe names
  private static ArrayList<String> names = new ArrayList<String>(
      Arrays.asList(new String[] { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie",
          "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe",
          "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan",
          "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Toby", "Scarlet", "Alex", "Leon", "Eric",
          "GuyWhoGoesAllInEveryTime", "Fei Yu-Ching", "Jay", "Daniel", "Evan", "Sean", "Selene", "James", "Jacques",
          "NoName", "Zoe", "Sarah", "Kyle", "Irene", "Sharolyn", "Ben", "Coco", "Cindy", "Megan", "Mia", "E10WINS",
          "Audrey", "Emily", "March 7th", "Stelle", "Cao Cao", "Liu", "Camellia", "Cameron", "Maddie", "Will", "Amy",
          "Kelly", "Aventurine" }));

  public static String getName() {
    return names.remove((int) (Math.random() * names.size()));
  }
}