import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int[] blinds = { 10, 20 }; // current blinds size
  private PokerPlayer og; // original first player, used for blind increasing
  private int[] pot; // keeps track of total contribution for each player in current round
  private int lastPlayer; // keeps track of the last player to act
  private int currBet; // current chips to call
  private int[] currAction; // keeps track of current players action
  private Scanner sc = new Scanner(System.in);
  private ArrayList<PokerPot> pots; // all the pots of chips
  private PokerPlayer main;

  public PokerGame(PokerPlayer[] players) { // initialiaze a poker game
    this.players = players;
    main = players[0];
    // shuffles player positions
    List<PokerPlayer> temp = Arrays.asList(players);
    Collections.shuffle(temp);
    players = temp.toArray(new PokerPlayer[players.length]);

    og = this.players[0];
    pot = new int[players.length];
    pots = new ArrayList<>();
    pots.add(new PokerPot("Main"));
  }

  public void init() { // start the main loop
    System.out
        .println("\n\nWelcome to the Texas Hold'em table!\nHere are the players in today and their current buy-ins!");
    for (PokerPlayer p : players) {
      System.out.println(p.toString());
    }
    System.out.println("Press enter to continue...");
    sc.nextLine();
    Utils.clearScreen();
    players[0].setStatus(1);
    players[1].setStatus(2);
    preflop();
  }

  private void endGame() {
    System.out.print("Game Over!");
    if (main.getChips() > 0)
      System.out.println("You ended the game early.");
    else
      System.out.println("You ran out of chips");
  }

  private void preflop() { // code to execute preflop
    lastPlayer = 2;
    Card[][] holeCards = p.deal(players.length);
    for (int i = 0; i < players.length; i++) {
      players[i].setHand(holeCards[i]);
      pots.get(0).addPlayerContribution(players[i], 0);
    }
    currAction = new int[2];
    currAction[1] = blinds[0];
    distributeBet(0);
    pot[0] = players[0].removeChips(blinds[0]);
    currAction[1] = blinds[1];
    distributeBet(1);
    pot[1] = players[1].removeChips(blinds[1]);
    currBet = blinds[1];
    int i = 2;
    do {
      if (players[i].inHand() && players[i].getChips() > 0) {
        if (players[i].inHand()) {
          System.out.println(players[i].getName().toUpperCase() + "'s turn!");
          if (players[i] instanceof PokerBot) {
            System.out.println("Their current stack: ✨" + players[i].getChips());
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          for (PokerPot k : pots)
            System.out.println(k.toString());
          if (players[i].getChips() > 0) {
            if (players[i] instanceof PokerBot) {
              PokerBot temp = (PokerBot) players[i];
              currAction = temp.action("preflop", pot[i], currBet, blinds[1], new Card[5]);
            } else
              currAction = players[i].action("preflop", pot[i], currBet, blinds[1]);
            handleAction(i);
          } else if (!(players[i] instanceof PokerBot)) {
            System.out
                .println("Your hand: " + players[i].getHand()[0].getValue() + " " + players[i].getHand()[1].getValue());
          }
          System.out.println();
          try {
            Thread.sleep(1500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        if (i == players.length - 1)
          i = 0;
        else
          i++;
      }
    } while (i != lastPlayer && stillIn() > 1);
    System.out.println();
    for (PokerPot k : pots)
      System.out.println(k.toString());
    System.out.println("Press Enter to continue:");
    sc.nextLine();
    Utils.clearScreen();
    if (stillIn() < 2)
      showdown(0);
    else
      postflop();
  }

  private void postflop() { // all code to execute postflop, including flop, turn and river
    Card[] b = p.deal();
    pot = new int[players.length];
    lastPlayer = 0;
    currBet = 0;
    int i = 0;
    for (int j = 0; j < 3; j++) {
      do {
        if (players[i].inHand()) {
          System.out.println(players[i].getName().toUpperCase() + "'s turn!");
          if (players[i] instanceof PokerBot) {
            System.out.println("Their current stack: ✨" + players[i].getChips());
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          System.out.println("Board: " + b[0].getValue() + "  - " + b[1].getValue() + "  - " + b[2].getValue()
              + ((j > 0) ? ("  - " + b[3].getValue()) : "") + ((j == 2) ? "  - " + b[4].getValue() : ""));
          for (PokerPot k : pots)
            System.out.println(k.toString());

          if (players[i].getChips() > 0) {
            if (players[i] instanceof PokerBot) {
              PokerBot temp = (PokerBot) players[i];
              currAction = temp.action("postflop", pot[i], currBet, blinds[1], b);
            } else
              currAction = players[i].action("postflop", pot[i], currBet, blinds[1]);
            handleAction(i);
          } else if (!(players[i] instanceof PokerBot)) {
            System.out
                .println("Your hand: " + players[i].getHand()[0].getValue() + " " + players[i].getHand()[1].getValue());
          }
          System.out.println();
          try {
            Thread.sleep(1500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        if (i == players.length - 1)
          i = 0;
        else
          i++;
      } while (i != lastPlayer && stillIn() > 1);
      pot = new int[players.length];
      i = 0;
      lastPlayer = 0;
      currBet = 0;
      System.out.println();
      for (PokerPot k : pots)
        System.out.println(k.toString());
      System.out.println("Press Enter to continue:");
      sc.nextLine();
      Utils.clearScreen();
    }
    if (stillIn() < 2)
      showdown(0);
    else
      showdown(1);
  }

  private void showdown(int c) { // assign winnner
    for (PokerPot b : pots) {
      b.assignWinner(p, c);
      System.out.println();
    }
    newHand();
  }

  private void newHand() { // setup for new hand
    boolean gameOver = false;
    p.reset();
    if (pots.size() > 1)
      for (int i = pots.size() - 1; i > 0; i++)
        pots.remove(i);
    players[0].setStatus(0);
    players[1].setStatus(0);
    PokerPlayer first = players[players.length - 1];
    for (int i = players.length - 1; i > 0; i--)
      players[i] = players[i - 1];
    players[0] = first;
    if (players[0].equals(og)) {
      blinds[0] *= 2;
      blinds[1] *= 2;
      System.out.println("Round finished! Increasing blind sizes...");
    }
    for (int i = 0; i < players.length; i++) {
      if (players[i].getChips() > 0)
        players[i].setInHand(true);
      else if (players[i] instanceof PokerBot) {
        String ogName = players[i].getName();
        players[i] = new PokerBot();
        System.out
            .println(ogName + " has run out of chips, they has been replaced by newcomer " + players[i].getName());
      } else
        gameOver = true;
    }
    if (!gameOver) {
      players[0].setStatus(1);
      players[1].setStatus(2);
      pot = new int[players.length];
      System.out.println("Press Enter to continue:");
      sc.nextLine();
      Utils.clearScreen();
      preflop();
    } else
      endGame();
  }

  private void handleAction(int i) { // do certain things based on a player's action
    switch (currAction[0]) {
      case 1:
        distributeBet(i);
        pot[i] += players[i].removeChips(currAction[1]);
        if (players[i].status() == 2) {
          if (currAction[1] != 0)
            System.out.println(players[i].getName() + " in big blind calls for ✨" + currBet + ".");
          else
            System.out.println(players[i].getName() + " in big blind checks.");
        } else {
          if (currAction[1] != 0)
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "calls for ✨" + currBet + ".");
          else
            System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "checks.");
        }
        break;
      case 2:
        if (players[i].status() == 2)
          System.out.println(players[i].getName() + " in big blind folds.");
        else
          System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        break;
      default:
        distributeBet(i);
        pot[i] += players[i].removeChips(currAction[1]);
        currBet = pot[i];
        lastPlayer = i;
        if (players[i].status() == 2)
          System.out.println(players[i].getName() + " in big blind " + ((currBet == 0) ? "bets" : "raises to")
              + " ✨" + pot[i] + ".");
        else
          System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
              + ((currBet == 0) ? "bets" : "raises to") + " ✨" + pot[i] + ".");
        break;
    }

  }

  private void distributeBet(int i) { // distributes a bet that has been made
    int remain = currAction[1];
    for (PokerPot p : pots) {
      if (remain < p.maxContribution()) {
        p.addPlayerContribution(players[i], remain);
        remain = 0;
        break;
      } else {
        p.addPlayerContribution(players[i], p.maxContribution());
        remain -= p.maxContribution();
      }
    }
    if (remain > 0) {
      PokerPot side = new PokerPot("Side");
      side.addPlayerContribution(players[i], remain);
      pots.add(side);
    }
  }

  private int stillIn() {
    int in = 0;
    for (int i = 0; i < players.length; i++)
      if (players[i].inHand())
        in++;
    return in;
  }
}