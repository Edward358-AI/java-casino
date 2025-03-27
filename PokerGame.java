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
  private Scanner sc = new Scanner(System.in);

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

  private void endGame() {
    System.out.println("Game Over! You ran out of chips!");
  }

  private void preflop() { // code to execute preflop
    lastPlayer = 1;
    Card[][] holeCards = p.deal(players.length);
    pot[0] = blinds[0];
    pot[1] = blinds[1];
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
          lastPlayer = (i == 0) ? players.length - 1 : i - 1;
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
      if (stillIn() < 2)
        break;
      if (i == players.length - 1)
        i = -1;
    }
    for (int i = 0; i < pot.length; i++)
      players[i].removeChips(pot[i]);
    pot = new int[players.length];
    if (stillIn() > 1)
      postflop();
    else
      showdown(0);
  }

  private void postflop() { // all code to execute postflop, including flop, turn and river
    Card[] b = p.deal();
    lastPlayer = players.length - 1;
    currAction = new int[2];
    currBet = 0;
    prevPot = p.getChips();
    int lastRound = 0;
    for (int j = 0; j < 3; j++) {
      for (int i = 0;; i++) { // code for the flop, turn and river
        if (players[i].inHand() && (pot[i] < currBet || currBet == 0)) {
          System.out.println(players[i].getName().toUpperCase() + "'s turn!");
          System.out.println("Board: " + b[0].getValue() + " - " + b[1].getValue() + " - " + b[2].getValue()
              + ((j > 0) ? (" - " + b[3].getValue()) : "") + ((j == 2) ? " - " + b[4].getValue() : ""));
          System.out.println("Current pot: $" + prevPot);
          currAction = players[i].action("postflop", pot[i], currBet, blinds[1]);
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

            if (players[i].status() == 2)
              System.out.println(players[i].getName() + " in big blind " + ((currBet == 0) ? "bets" : "raises to")
                  + " $" + currAction[1] + ".");
            else
              System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                  + ((currBet == 0) ? "bets" : "raises to") + " $" + currAction[1]);
            pot[i] += currAction[1];
            currBet = pot[i];
            lastPlayer = (i == 0) ? players.length - 1 : i - 1;
          }
          System.out.println();
          prevPot += sum(pot);
        }
        if (i == lastPlayer)
          break;
        if (i == players.length - 1)
          i = -1;
      }
      for (int i = 0; i < pot.length; i++)
        players[i].removeChips(pot[i]);
      p.addChips(prevPot - p.getChips());
      lastPlayer = players.length - 1;
      currAction = new int[2];
      currBet = 0;
      prevPot = 0;
      prevPot = p.getChips();
      pot = new int[players.length];
      lastRound = j;
      if (stillIn() < 2)
        break;
    }
    showdown((lastRound == 2) ? 1 : 0);
  }

  private void showdown(int complete) { // complete: 0 - incomplete, everyone folded, 1 - complete
    if (complete == 1) {
      ArrayList<Integer> currBest = new ArrayList<Integer>();
      Card[] bestHand = new Card[5];
      for (int i = 0; i < players.length; i++) {
        if (players[i].inHand()) {
          if (currBest.size() > 0) {
            Card[] currHand = p.getBestHand(players[i]);
            if (p.compareHands(bestHand, currHand) == 2) {
              currBest.clear();
              currBest.add(i);
              bestHand = currHand;
            } else if (p.compareHands(bestHand, currHand) == 0)
              currBest.add(i);
          } else {
            currBest.add(i);
            bestHand = p.getBestHand(players[i]);
          }
        }
      }
      Deck.sort(bestHand);
      for (int i = 0; i < currBest.size(); i++) {
        players[currBest.get(i)].addChips((int) (p.getChips() / currBest.size()));
        System.out.print(players[currBest.get(i)].getName() + " won $" + (int) (p.getChips() / currBest.size())
            + " this hand! Their hand was ");
        for (int f = 0; f < 5; f++) {
          System.out.print(bestHand[f].getValue() + ((f == 4) ? "\n" : " - "));
        }
      }
    } else {
      for (int i = 0; i < players.length; i++) {
        if (players[i].inHand()) {
          players[i].addChips(p.getChips());
          System.out.println(players[i].getName() + " won $" + p.getChips()
            + " this hand! Everyone else folded.");
        }
      }
    }
    System.out.println("Press Enter to continue:");
    sc.nextLine();
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
    Utils.clearScreen();
    preflop();
  }

  private int sum(int[] arr) {
    int sum = 0;
    for (int a : arr)
      sum += a;
    return sum;
  }

  private int stillIn() {
    int in = 0;
    for (int i = 0; i < players.length; i++)
      if (players[i].inHand())
        in++;
    return in;
  }
}