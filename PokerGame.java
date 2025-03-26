import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int[] blinds = { 10, 20 }; // current blinds size
  private PokerPlayer og;
  private int[] pot;
  private int lastPlayer;
  private int currBet;
  private int[] currAction;
  private int prevPot;

  public PokerGame(PokerPlayer[] players) {
    this.players = players;
    og = this.players[0];
    pot = new int[players.length];
  }

  public void init() {
    players[0].setStatus(1);
    players[1].setStatus(2);
    preflop();
  }

  private void preflop() { // code to execute preflop
    lastPlayer = 1;
    Card[][] holeCards = p.deal(players.length);
    pot[0] = players[0].removeChips(blinds[0]);
    pot[1] = players[1].removeChips(blinds[1]);
    currAction = new int[2];
    currBet = blinds[1];
    p.addChips(sum(pot));
    for (int i = 0; i < holeCards.length; i++)
      players[i].setHand(holeCards[i]);
    for (int i = 2;; i++) {
      if (players[i].inHand() && (pot[i] < currBet || (players[i].status() == 2 && currBet == blinds[1]))) {
        System.out.println(players[i].getName().toUpperCase() + "'s turn!");
        System.out.println("Current pot: $" + p.getChips());
        currAction = players[i].action("preflop", pot[i], currBet, blinds[1]);
        if (currAction[0] == 1) {
          pot[i] += currAction[1];
          if (currAction[1] == 0)
            System.out.println(players[i].getName() + " in big blind checks.");
          else if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind calls for $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "calls for $" + currBet + ".");
        } else if (currAction[0] == 2) {
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind folds.");
          else
            System.out
                .println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        } else {
          pot[i] += currAction[1];
          currBet = pot[i];
          lastPlayer = i - 1;
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind raises to $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "raises to $" + currBet);
        }
        System.out.println();
        p.addChips(0 - p.getChips());
        p.addChips(sum(pot));
      }
      if (i == lastPlayer)
        break;
      if (i == players.length - 1) {
        i = -1;
      }
    }
    pot = new int[players.length];
    postflop();
  }

  private void postflop() { // all code to execute postflop, including flop, turn and river
    Card[] b = p.deal();
    lastPlayer = players.length - 1;
    currAction = new int[2];
    currBet = 0;
    prevPot = p.getChips();
    for (int i = 0;; i++) { // code for the flop
      if (players[i].inHand() && (pot[i] < currBet || currBet == 0)) {
        System.out.println(players[i].getName().toUpperCase() + "'s turn!");
        System.out.println("Board: " + b[0].getValue() + " - " + b[1].getValue() + " - " + b[2].getValue());
        System.out.println("Current pot: $" + prevPot);
        currAction = players[i].action("flop", pot[i], currBet, blinds[1]);
        if (currAction[0] == 1) {
          pot[i] += currAction[1];
          if (currAction[1] == 0)
            System.out.println(players[i].getName() + " checks.");
          else if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind calls for $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "calls for $" + currBet + ".");
        } else if (currAction[0] == 2) {
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind folds.");
          else
            System.out
                .println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        } else {
          pot[i] += currAction[1];
          currBet = pot[i];
          lastPlayer = i - 1;
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind raises to $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "raises to $" + currBet);
        }
        System.out.println();
        prevPot += sum(pot);
      }
      if (i == lastPlayer)
        break;
      if (i == players.length - 1) {
        i = -1;
      }
    }
    p.addChips(prevPot - p.getChips());
    lastPlayer = players.length - 1;
    currAction = new int[2];
    currBet = 0;
    prevPot = 0;
    prevPot = p.getChips();
    pot = new int[players.length];
    for (int i = 0;; i++) { // code for the turn
      if (players[i].inHand() && (pot[i] < currBet || currBet == 0)) {
        System.out.println(players[i].getName().toUpperCase() + "'s turn!");
        System.out.println("Board: " + b[0].getValue() + " - " + b[1].getValue() + " - " + b[2].getValue() + " - " + b[3].getValue());
        System.out.println("Current pot: $" + prevPot);
        currAction = players[i].action("turn", pot[i], currBet, blinds[1]);
        if (currAction[0] == 1) {
          pot[i] += currAction[1];
          if (currAction[1] == 0)
            System.out.println(players[i].getName() + " checks.");
          else if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind calls for $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "calls for $" + currBet + ".");
        } else if (currAction[0] == 2) {
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind folds.");
          else
            System.out
                .println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        } else {
          pot[i] += currAction[1];
          currBet = pot[i];
          lastPlayer = i - 1;
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind raises to $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "raises to $" + currBet);
        }
        System.out.println();
        prevPot += sum(pot);
      }
      if (i == lastPlayer)
        break;
      if (i == players.length - 1) {
        i = -1;
      }
    }
    p.addChips(prevPot - p.getChips());
    lastPlayer = players.length - 1;
    currAction = new int[2];
    currBet = 0;
    prevPot = p.getChips();
    pot = new int[players.length];
    for (int i = 0;; i++) { // code for the river
      if (players[i].inHand() && (pot[i] < currBet || currBet == 0)) {
        System.out.println(players[i].getName().toUpperCase() + "'s turn!");
        System.out.println("Board: " + b[0].getValue() + " - " + b[1].getValue() + " - " + b[2].getValue() + " - " + b[3].getValue() + " - " + b[4].getValue());
        System.out.println("Current pot: $" + prevPot);
        currAction = players[i].action("river", pot[i], currBet, blinds[1]);
        if (currAction[0] == 1) {
          pot[i] += currAction[1];
          if (currAction[1] == 0)
            System.out.println(players[i].getName() + " checks.");
          else if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind calls for $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "calls for $" + currBet + ".");
        } else if (currAction[0] == 2) {
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind folds.");
          else
            System.out
                .println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        } else {
          pot[i] += currAction[1];
          currBet = pot[i];
          lastPlayer = i - 1;
          if (players[i].status() == 2)
            System.out.println(players[i].getName() + " in big blind raises to $" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "raises to $" + currBet);
        }
        System.out.println();
        prevPot += sum(pot);
      }
      if (i == lastPlayer)
        break;
      if (i == players.length - 1) {
        i = -1;
      }
    }
    p.addChips(prevPot - p.getChips());
    lastPlayer = players.length - 1;
    currAction = new int[2];
    currBet = 0;
    prevPot = 0;
    pot = new int[players.length];
    showdown();
  }

  private void showdown() {
    // award chips and designate winner
    newHand();
  }

  private void newHand() {
    p.reset();
    players[0].setStatus(0);
    players[1].setStatus(0);
    PokerPlayer first = players[0];
    for (int i = 1; i < players.length; i++) {
      players[i - 1] = players[i];
    }
    if (players[0].equals(og)) {
      blinds[0] *= 2;
      blinds[1] *= 2;
    }
    players[players.length - 1] = first;
    for (int i = 0; i < players.length; i++) {
      players[i].setInHand(true);
    }
    players[0].setStatus(1);
    players[1].setStatus(2);
    pot = new int[players.length];
  }

  private int sum(int[] arr) {
    int sum = 0;
    for (int a : arr)
      sum += a;
    return sum;
  }
}