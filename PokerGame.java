import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int blinds = 20; // current blinds size
  private int hands; // original first player, used for blind increasing
  private int lastPlayer; // keeps track of the last player to act
  private int[] currAction; // keeps track of current players action
  private Scanner sc = new Scanner(System.in);
  private PokerPot pot; // a pokerpot object which keeps track of player contributions and the pot
  private int currBet; // current bet for round
  private int[] currConts; // current contributions this round
  private PlayerStat mp; // keeps track of players stats for the game

  public PokerGame(PokerPlayer[] players) { // initialiaze a poker game
    this.players = players;
    mp = new PlayerStat(0);
    for (int i = 1; i < players.length; i++) { // randomizes bot chip amount
      if (Math.random() >= 0.5) {
        players[i].addChips((int) (Math.random() * 150));
      } else {
        players[i].removeChips((int) (Math.random() * 150));
      }
    }
    // shuffles player positions
    List<PokerPlayer> temp = Arrays.asList(players);
    Collections.shuffle(temp);
    players = temp.toArray(new PokerPlayer[players.length]);
    pot = new PokerPot(players);
  }

  public PlayerStat getStats() {
    return mp;
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

  private void endGame(boolean t) {
    Utils.clearScreen();
    System.out.println("\nGame Over! " + ((t) ? " You ran out of primogems ✨ :(" : "You ended the game early."));
    System.out.println(mp);
  }

  private void preflop() { // code to execute preflop
    currBet = blinds;
    currConts = new int[players.length];
    lastPlayer = 2;
    Card[][] holeCards = p.deal(players.length);
    for (int i = 0; i < players.length; i++) {
      players[i].setHand(holeCards[i]);
    }
    currAction = new int[2];

    if (blinds / 2 < players[0].getChips()) {
      if (!(players[0] instanceof PokerBot))
        mp.addBet(blinds / 2);
      pot.addPlayerContribution(0, blinds / 2);
      currConts[0] += blinds / 2;
    } else {
      if (!(players[0] instanceof PokerBot)) {
        mp.addBet(players[0].getChips());
        mp.allIn();
      }
      pot.addPlayerContribution(0, players[0].getChips());
      currConts[0] += players[0].getChips();
    }

    if (blinds < players[1].getChips()) {
      if (!(players[1] instanceof PokerBot))
        mp.addBet(blinds);
      pot.addPlayerContribution(1, blinds);
      currConts[1] += blinds;
    } else {
      if (!(players[1] instanceof PokerBot)) {
        mp.addBet(players[1].getChips());
        mp.allIn();
      }
      pot.addPlayerContribution(1, players[1].getChips());
      currConts[1] += players[1].getChips();
    }

    int i = 2;
    System.out.println("*** PREFLOP ***\n");
    do {
      if (players[i].inHand() && players[i].getChips() > 0) {
        if (players[i].inHand()) {
          System.out.println(players[i].getName().toUpperCase() + "'s turn!");
          System.out.println(pot.toString());
          if (players[i] instanceof PokerBot) {
            System.out.println("Their current stack: ✨" + players[i].getChips());
            Utils.sleep(1000);
          }
          if (players[i].getChips() > 0) {
            if (players[i] instanceof PokerBot) {
              PokerBot temp = (PokerBot) players[i];
              currAction = temp.action("preflop", currConts[i], currBet, blinds, null);
            } else
              currAction = players[i].action("preflop", currConts[i], currBet, blinds);
            handleAction(i);
          } else if (!(players[i] instanceof PokerBot)) {
            System.out
                .println("Your hand: " + players[i].getHand()[0].getValue() + " " + players[i].getHand()[1].getValue());
          }
          // System.out.println(Arrays.toString(players));
          // System.out.println(Arrays.toString(pot.getPlayers()));
          // System.out.println(stillIn());
          // System.out.println(Arrays.toString(currAction));
          System.out.println();
          Utils.sleep(1000);
        }
      }
      if (i == players.length - 1)
        i = 0;
      else
        i++;
    } while (i != lastPlayer && stillIn() > 1);
    System.out.println();
    System.out.println(pot.toString());
    System.out.println("Press Enter to continue:");
    sc.nextLine();
    Utils.clearScreen();
    if (stillIn() < 2)
      showdown(0);
    else
      postflop();
  }

  private void postflop() { // all code to execute postflop, including flop, turn and river
    currConts = new int[players.length];
    Card[] b = p.deal();
    ArrayList<Card> boardForBot = new ArrayList<>();
    boardForBot.add(b[0]);
    boardForBot.add(b[1]);
    boardForBot.add(b[2]);
    lastPlayer = 0;
    currBet = 0;
    int i = 0;
    for (int j = 0; j < 3; j++) {
      System.out.println(((j == 0) ? "*** THE FLOP ***\n"
          : ((j == 1) ? "*** THE TURN (4th Street) ***\n" : "*** THE RIVER (5th Street) ***\n")));
      do {
        if (players[i].inHand()) {
          System.out.println(
              ((players[i] instanceof PokerBot) ? players[i].getName().toUpperCase() + "'s" : "YOUR") + " turn!");
          System.out.println(pot.toString());
          System.out.println("Board: " + b[0].getValue() + "  - " + b[1].getValue() + "  - " + b[2].getValue()
              + ((j > 0) ? ("  - " + b[3].getValue()) : "") + ((j == 2) ? "  - " + b[4].getValue() : ""));
          if (players[i] instanceof PokerBot) {
            System.out.println("Their current stack: ✨" + players[i].getChips());
            Utils.sleep(1000);
          }
          if (players[i].getChips() > 0) {
            if (players[i] instanceof PokerBot) {
              PokerBot temp = (PokerBot) players[i];
              currAction = temp.action("postflop", currConts[i], currBet, blinds, boardForBot.toArray(new Card[j + 3]));
            } else
              currAction = players[i].action("postflop", currConts[i], currBet, blinds);

            handleAction(i);
          } else if (!(players[i] instanceof PokerBot)) {
            System.out
                .println("Your hand: " + players[i].getHand()[0].getValue() + " " + players[i].getHand()[1].getValue());
          }
          // System.out.println(Arrays.toString(players));
          // System.out.println(Arrays.toString(pot.getPlayers()));
          // System.out.println(stillIn());
          // System.out.println(Arrays.toString(currAction));
          System.out.println();
          Utils.sleep(1000);
        }

        if (i == players.length - 1)
          i = 0;
        else
          i++;
      } while (i != lastPlayer && stillIn() > 1);
      i = 0;
      lastPlayer = 0;
      currConts = new int[players.length];
      currBet = 0;
      if (j < 2)
        boardForBot.add(b[j + 3]);
      System.out.println();
      System.out.println(pot.toString());
      System.out.println("Press Enter to continue:");
      sc.nextLine();
      Utils.clearScreen();
      if (stillIn() < 2)
        break;
    }
    if (stillIn() < 2)
      showdown(0);
    else
      showdown(1);
  }

  private void showdown(int c) { // assign winnner at end of hand
    int[] stats = pot.assignWinner(p, c);
    if (stats[0] == 1) {
      mp.addWin(stats[1]);
      mp.setGain(stats[1]);
    } else {
      for (int i = 0; i < players.length; i++) {
        if (!(players[i] instanceof PokerBot)) {
          mp.addLoss(pot.getContributions()[i]);
        }
      }
    }
    System.out.println();
    newHand();
  }

  private void newHand() { // setup for new hand
    hands++;
    mp.hands();
    boolean gameOver = false;
    p.reset();
    players[0].setStatus(0);
    players[1].setStatus(0);
    PokerPlayer first = players[players.length - 1];
    for (int i = players.length - 1; i > 0; i--)
      players[i] = players[i - 1];
    players[0] = first;
    if (hands == players.length) {
      if (blinds < 1280)
        blinds *= 2;
      System.out.println("Round finished! Increasing blind sizes...");
      hands = 0;
    }
    for (int i = 0; i < players.length; i++) {
      if (!(players[i] instanceof PokerBot))
        mp.setChips(players[i].getChips());
      if (players[i].getChips() > 0)
        players[i].setInHand(true);
      else if (players[i] instanceof PokerBot) {
        String ogName = players[i].getName();
        players[i] = new PokerBot();
        System.out.println(
            ogName + " has run out of primogems ✨, they has been replaced by newcomer " + players[i].getName());
      } else
        gameOver = true;
    }
    if (!gameOver) {
      players[0].setStatus(1);
      players[1].setStatus(2);
      pot.resetPot(players);
      System.out.println("Continue to next hand? [y/n]");
      String s = sc.nextLine().strip().toLowerCase();
      if (s.equals("n") || s.equals("no") || s.equals("nah")) {
        endGame(false);
      } else {
        Utils.clearScreen();
        preflop();
      }
    } else
      endGame(true);
  }

  private void handleAction(int i) { // do certain things based on a player's action
    switch (currAction[0]) {
      case 1:
        if (!(players[i] instanceof PokerBot))
          mp.addBet(currAction[1]);
        pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += currAction[1];
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
        players[i].setInHand(false);
        if (players[i].status() == 2)
          System.out.println(players[i].getName() + " in big blind folds.");
        else
          System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "folds.");
        break;
      default:
        if (!(players[i] instanceof PokerBot) && currAction[0] == 4)
          mp.allIn();
        if (!(players[i] instanceof PokerBot))
          mp.addBet(currAction[1]);
        if (players[i].status() == 2)
          System.out.println(players[i].getName() + " in big blind "
              + ((currAction[0] == 4) ? "goes all in for" : ((currBet == 0) ? "bets" : "raises by"))
              + " ✨" + (currAction[1]) + ".");
        else
          System.out.println(players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
              + ((currAction[0] == 4) ? "goes all in for" : ((currBet == 0) ? "bets" : "raises by")) + " ✨"
              + (currAction[1]) + ".");

        pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += currAction[1];
        if (currConts[i] > currBet) {
          currBet = currConts[i];
          lastPlayer = i;
        }
        break;
    }
  }

  private int stillIn() { // gets # of players still in
    int in = 0;
    for (int i = 0; i < players.length; i++)
      if (players[i].inHand())
        in++;
    return in;
  }
}