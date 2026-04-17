import java.util.*;

class PokerGame {
  private PokerDeck p = new PokerDeck();
  private PokerPlayer[] players;
  private int blinds = 20; // current blinds size
  private int hands; // original first player, used for blind increasing
  private int lastPlayer; // keeps track of the last player to act
  private int[] currAction; // keeps track of current players action
  private Scanner sc = Player.sc;
  private PokerPot pot; // a pokerpot object which keeps track of player contributions and the pot
  private int currBet; // current bet for round
  private int[] currConts; // current contributions this round
  private PlayerStat mp; // keeps track of players stats for the game
  private PokerPlayer mainPlayer; // original player
  private boolean skipMode = false; // keeps track of whether we are fast-forwarding

  public PokerGame(PokerPlayer[] players) { // initialiaze a poker game
    this.players = players;
    mainPlayer = players[0];
    mp = new PlayerStat(0);
    for (int i = 0; i < players.length; i++) { // randomizes bot chip amount
      if (players[i] instanceof PokerBot) {
        if (Math.random() >= 0.5) {
          players[i].addChips((int) (Math.random() * 150));
        } else {
          players[i].removeChips((int) (Math.random() * 150));
        }
      }
    }
    // shuffles player positions
    List<PokerPlayer> temp = Arrays.asList(players);
    Collections.shuffle(temp);
    this.players = temp.toArray(new PokerPlayer[players.length]);
    pot = new PokerPot(this.players);
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
      if (players[0] == mainPlayer)
        mp.addBet(blinds / 2);
      pot.addPlayerContribution(0, blinds / 2);
      currConts[0] += blinds / 2;
    } else {
      if (players[0] == mainPlayer) {
        mp.addBet(players[0].getChips());
        mp.allIn();
      }
      pot.addPlayerContribution(0, players[0].getChips());
      currConts[0] += players[0].getChips();
    }

    if (blinds < players[1].getChips()) {
      if (players[1] == mainPlayer)
        mp.addBet(blinds);
      pot.addPlayerContribution(1, blinds);
      currConts[1] += blinds;
    } else {
      if (players[1] == mainPlayer) {
        mp.addBet(players[1].getChips());
        mp.allIn();
      }
      pot.addPlayerContribution(1, players[1].getChips());
      currConts[1] += players[1].getChips();
    }

    int i = 2;
    String roundName = "*** PREFLOP ***\n";
    String realPlayerOrder = "Real player order: ";
    int count = 0;
    for (int k = 0; k < players.length; k++) {
      int idx = (i + k) % players.length;
      if (!(players[idx] instanceof PokerBot) && players[idx].inHand()) {
        realPlayerOrder += players[idx].getName() + " -> ";
        count++;
      }
    }
    if (count > 0) {
      realPlayerOrder = realPlayerOrder.substring(0, realPlayerOrder.length() - 4) + "\n";
      roundName += realPlayerOrder;
    }

    String positionMap = "Action order: ";
    for (int k = 0; k < players.length; k++) {
      int idx = (i + k) % players.length;
      if (players[idx].inHand()) {
        positionMap += players[idx].getName();
        if (idx == 0)
          positionMap += " (SB)";
        else if (idx == 1)
          positionMap += " (BB)";
        else if (idx == players.length - 1)
          positionMap += " (D)";
        positionMap += " -> ";
      }
    }
    if (positionMap.length() > 14) {
      positionMap = positionMap.substring(0, positionMap.length() - 4) + "\n\n";
      roundName += positionMap;
    } else {
      roundName += "\n";
    }
    String roundHistory = "";
    do {
      if (players[i].inHand() && players[i].getChips() > 0) {
        Utils.clearScreen();
        System.out.print(roundName + pot.toString() + "\n\n" + roundHistory);

        String turnHeader = players[i].getName().toUpperCase() + "'s turn!\n";

        if (players[i] instanceof PokerBot) {
          turnHeader += "Their stack: ✨" + players[i].getChips() + "\n";
          System.out.print(turnHeader);
          if (!skipMode) Utils.sleep(1000);
        } else {
          System.out.print(turnHeader);
        }

        if (players[i] instanceof PokerBot) {
          PokerBot temp = (PokerBot) players[i];
          currAction = temp.action("preflop", currConts[i], currBet, blinds, null);
        } else {
          System.out.println("Are you " + players[i].getName() + "? Press Enter to confirm and show your hand...");
          Utils.flushInput();
          sc.nextLine();
          Utils.clearScreen();
          System.out.print(roundName + pot.toString() + "\n\n" + roundHistory);
          System.out.print(turnHeader);
          currAction = players[i].action("preflop", currConts[i], currBet, blinds);
        }

        String actionLog = handleAction(i);
        if (!(players[i] instanceof PokerBot))
          actionLog += " (real)";
        // Reprint with updated stack after action
        turnHeader = players[i].getName().toUpperCase() + "'s turn!\n";
        turnHeader += "Their stack: ✨" + players[i].getChips() + "\n";
        Utils.clearScreen();
        System.out.print(roundName + pot.toString() + "\n\n" + roundHistory);
        System.out.print(turnHeader);
        System.out.println(actionLog + "\n");

        roundHistory += turnHeader + actionLog + "\n\n";
        if (!skipMode) Utils.sleep(1000);
      }
      if (i == players.length - 1)
        i = 0;
      else
        i++;
    } while (i != lastPlayer && stillIn() > 1);
    Utils.clearScreen();
    System.out.print(roundName + pot.toString() + "\n\n" + roundHistory);
    if (!skipMode && realPlayersIn() == 0) {
      System.out.println("Type \"skip\" to fast forward the hand, or hit enter to continue:");
      Utils.flushInput();
      Player.sc = new Scanner(System.in);
      sc = Player.sc;
      String input = sc.nextLine().strip().toLowerCase();
      if (input.equals("skip"))
        skipMode = true;
    } else if (!skipMode) {
      System.out.println("Press Enter to continue:");
      Utils.flushInput();
      Player.sc = new Scanner(System.in);
      sc = Player.sc;
      sc.nextLine();
    }
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
      String roundName = ((j == 0) ? "*** THE FLOP ***\n"
          : ((j == 1) ? "*** THE TURN (4th Street) ***\n" : "*** THE RIVER (5th Street) ***\n"));
      String realPlayerOrder = "Real player order: ";
      int count = 0;
      for (int k = 0; k < players.length; k++) {
        int idx = (i + k) % players.length;
        if (!(players[idx] instanceof PokerBot) && players[idx].inHand()) {
          realPlayerOrder += players[idx].getName() + " -> ";
          count++;
        }
      }
      if (count > 0) {
        realPlayerOrder = realPlayerOrder.substring(0, realPlayerOrder.length() - 4) + "\n";
        roundName += realPlayerOrder;
      }

      String positionMap = "Action order: ";
      for (int k = 0; k < players.length; k++) {
        int idx = (i + k) % players.length;
        if (players[idx].inHand()) {
          positionMap += players[idx].getName();
          if (idx == 0)
            positionMap += " (SB)";
          else if (idx == 1)
            positionMap += " (BB)";
          else if (idx == players.length - 1)
            positionMap += " (D)";
          positionMap += " -> ";
        }
      }
      if (positionMap.length() > 14) {
        positionMap = positionMap.substring(0, positionMap.length() - 4) + "\n\n";
        roundName += positionMap;
      } else {
        roundName += "\n";
      }
      String roundHistory = "";
      do {
        if (players[i].inHand() && players[i].getChips() > 0) {
          String boardStr = "Board: " + b[0].getValue() + "  - " + b[1].getValue() + "  - " + b[2].getValue()
              + ((j > 0) ? ("  - " + b[3].getValue()) : "") + ((j == 2) ? "  - " + b[4].getValue() : "");

          Utils.clearScreen();
          System.out.print(roundName + boardStr + "\n\n" + pot.toString() + "\n\n" + roundHistory);

          String turnHeader = players[i].getName().toUpperCase() + "'s turn!\n";

          if (players[i] instanceof PokerBot) {
            turnHeader += "Their stack: ✨" + players[i].getChips() + "\n";
            System.out.print(turnHeader);
            if (!skipMode) Utils.sleep(1000);
          } else {
            System.out.print(turnHeader);
          }

          if (players[i] instanceof PokerBot) {
            PokerBot temp = (PokerBot) players[i];
            currAction = temp.action("postflop", currConts[i], currBet, blinds, boardForBot.toArray(new Card[j + 3]));
          } else {
            System.out.println("Are you " + players[i].getName() + "? Press Enter to confirm and show your hand...");
            Utils.flushInput();
            sc.nextLine();
            Utils.clearScreen();
            System.out.print(roundName + boardStr + "\n\n" + pot.toString() + "\n\n" + roundHistory);
            System.out.print(turnHeader);
            currAction = players[i].action("postflop", currConts[i], currBet, blinds);
          }

          String actionLog = handleAction(i);
          if (!(players[i] instanceof PokerBot))
            actionLog += " (real)";
          // Reprint with updated stack after action
          turnHeader = players[i].getName().toUpperCase() + "'s turn!\n";
          turnHeader += "Their stack: ✨" + players[i].getChips() + "\n";
          Utils.clearScreen();
          System.out.print(roundName + boardStr + "\n\n" + pot.toString() + "\n\n" + roundHistory);
          System.out.print(turnHeader);
          System.out.println(actionLog + "\n");

          roundHistory += turnHeader + actionLog + "\n\n";
          if (!skipMode) Utils.sleep(1000);
        }

        if (i == players.length - 1)
          i = 0;
        else
          i++;
      } while (i != lastPlayer && stillIn() > 1);
      Utils.clearScreen();
      String boardStr = "Board: " + b[0].getValue() + "  - " + b[1].getValue() + "  - " + b[2].getValue()
          + ((j > 0) ? ("  - " + b[3].getValue()) : "") + ((j == 2) ? "  - " + b[4].getValue() : "");
      System.out.print(roundName + boardStr + "\n\n" + pot.toString() + "\n\n" + roundHistory);
      if (!skipMode && realPlayersIn() == 0 && j < 2) {
        System.out.println("Type \"skip\" to fast forward the hand, or hit enter to continue:");
        Utils.flushInput();
        Player.sc = new Scanner(System.in);
        sc = Player.sc;
        String input = sc.nextLine().strip().toLowerCase();
        if (input.equals("skip"))
          skipMode = true;
      } else if (!skipMode) {
        System.out.println("Press Enter to continue:");
        Utils.flushInput();
        Player.sc = new Scanner(System.in);
        sc = Player.sc;
        sc.nextLine();
      }
      Utils.clearScreen();
      i = 0;
      lastPlayer = 0;
      currConts = new int[players.length];
      currBet = 0;
      if (j < 2)
        boardForBot.add(b[j + 3]);
      if (stillIn() < 2)
        break;
    }
    if (stillIn() < 2)
      showdown(0);
    else
      showdown(1);
  }

  private void showdown(int c) { // assign winnner at end of hand
    int[] stats = pot.assignWinner(p, c, mainPlayer);
    if (stats[0] == 1) {
      mp.addWin(stats[1]);
      mp.setGain(stats[1]);
    } else {
      for (int i = 0; i < players.length; i++) {
        if (players[i] == mainPlayer) {
          mp.addLoss(pot.getContributions()[i]);
        }
      }
    }
    System.out.println();
    newHand();
  }

  private void newHand() { // setup for new hand
    skipMode = false;
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
    if (hands >= players.length) {
      if (blinds < 320) {
        blinds *= 2;
      } else {
        System.out.println("Round finished");
      }
      hands = 0;
    }
    for (int i = 0; i < players.length; i++) {
      if (players[i] == mainPlayer)
        mp.setChips(players[i].getChips());
      if (players[i].getChips() > 0)
        players[i].setInHand(true);
      else if (players[i] != mainPlayer) {
        String ogName = players[i].getName();
        players[i] = new PokerBot(players);
        System.out.println(
            ogName + " has run out of primogems ✨, they have been replaced by newcomer " + players[i].getName());
      } else
        gameOver = true;
    }
    // Random player join/leave (7% chance per hand)
    if (!gameOver && Math.random() < 0.07) {
      boolean tryRemove = Math.random() < 0.5;
      if (tryRemove && players.length > 6) {
        // Find all bot indices eligible to leave
        ArrayList<Integer> botIndices = new ArrayList<>();
        for (int k = 0; k < players.length; k++) {
          if (players[k] instanceof PokerBot)
            botIndices.add(k);
        }
        if (!botIndices.isEmpty()) {
          int removeIdx = botIndices.get((int) (Math.random() * botIndices.size()));
          String leavingName = players[removeIdx].getName();
          PokerPlayer[] newPlayers = new PokerPlayer[players.length - 1];
          int idx = 0;
          for (int k = 0; k < players.length; k++) {
            if (k != removeIdx)
              newPlayers[idx++] = players[k];
          }
          players = newPlayers;
          System.out.println(leavingName + " has left the table.");
        }
      } else if (players.length < 12) {
        // Add a new bot
        PokerPlayer[] newPlayers = new PokerPlayer[players.length + 1];
        for (int k = 0; k < players.length; k++)
          newPlayers[k] = players[k];
        PokerBot newBot = new PokerBot(players);
        newPlayers[players.length] = newBot;
        players = newPlayers;
        System.out.println("A new player has joined the table: " + newBot.getName() + "!");
      }
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

  private String handleAction(int i) { // do certain things based on a player's action
    String log = "";
    switch (currAction[0]) {
      case 1:
        if (players[i] == mainPlayer)
          mp.addBet(currAction[1]);
        pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += currAction[1];
        if (players[i].status() == 2) {
          if (currAction[1] != 0)
            log = players[i].getName() + " in big blind CALLS FOR ✨" + currBet + ".";
          else
            log = players[i].getName() + " in big blind CHECKS.";
        } else {
          if (currAction[1] != 0)
            log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "CALLS FOR ✨" + currBet + ".";
          else
            log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
                + "CHECKS.";
        }
        break;
      case 2:
        players[i].setInHand(false);
        if (players[i].status() == 2)
          log = players[i].getName() + " in big blind FOLDS.";
        else
          log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "FOLDS.";
        break;
      default:
        if (players[i] == mainPlayer && currAction[0] == 4)
          mp.allIn();
        if (players[i] == mainPlayer)
          mp.addBet(currAction[1]);
        if (players[i].status() == 2)
          log = players[i].getName() + " in big blind "
              + ((currAction[0] == 4) ? "GOES ALL IN FOR" : ((currBet == 0) ? "BETS" : "RAISES TO"))
              + " ✨" + (currConts[i] + currAction[1]) + ".";
        else
          log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ")
              + ((currAction[0] == 4) ? "GOES ALL IN FOR" : ((currBet == 0) ? "BETS" : "RAISES TO")) + " ✨"
              + (currConts[i] + currAction[1]) + ".";

        pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += currAction[1];
        if (currConts[i] > currBet) {
          currBet = currConts[i];
          lastPlayer = i;
        }
        break;
    }
    return log;
  }

  private int stillIn() { // gets # of players still in
    int in = 0;
    for (int i = 0; i < players.length; i++)
      if (players[i].inHand())
        in++;
    return in;
  }

  private int realPlayersIn() {
    int count = 0;
    for (PokerPlayer p : players) {
      if (!(p instanceof PokerBot) && p.inHand())
        count++;
    }
    return count;
  }
}