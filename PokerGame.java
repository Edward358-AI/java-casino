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
  private int preflopAggressorIndex = -1;
  private int lastRaise = 0; // Tracks the minimum legal increment for the current round
  private static final double PREFLOP_EMA_ALPHA = 0.35; // >80% weight in roughly last 5 actions
  private static final double POSTFLOP_EMA_ALPHA = 0.35; // >80% weight in roughly last 5 actions
  private boolean[] preflopVPIPFlags;
  private boolean[] preflopPFRFlags;
  private int[][] postflopAggressionActions;
  private int[][] postflopAggressionOpportunities;
  private boolean[] sawFlopThisHand;
  private boolean[] foldToCbetOpportunity;
  private boolean[] foldedToCbet;
  private boolean cbetFiredOnFlop = false;
  private int cbetAggressorIndex = -1;
  
  // PHASE 8 COGNITIVE MATRIX: Street Tracking State
  // 0 = Preflop, 1 = Flop, 2 = Turn, 3 = River
  public int currentStreet = 0;
  public boolean isShowdown = false;

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
    currentStreet = 0;
    isShowdown = false; // PHASE 8: Reset showdown state
    currBet = blinds;
    lastRaise = blinds; // Pre-flop min raise is 1BB
    currConts = new int[players.length];
    preflopVPIPFlags = new boolean[players.length];
    preflopPFRFlags = new boolean[players.length];
    postflopAggressionActions = new int[players.length][4];
    postflopAggressionOpportunities = new int[players.length][4];
    sawFlopThisHand = new boolean[players.length];
    foldToCbetOpportunity = new boolean[players.length];
    foldedToCbet = new boolean[players.length];
    cbetFiredOnFlop = false;
    cbetAggressorIndex = -1;
    lastPlayer = 2;
    Card[][] holeCards = p.deal(players.length);
    for (int i = 0; i < players.length; i++) {
      players[i].setHand(holeCards[i]);
      // PHASE 8 COGNITIVE MATRIX: Increment hands played for tracked profiles
      if (shouldTrackCognitivePlayer(players[i])) {
        String pName = players[i].getName();
        PokerBot.getOrCreateCognitiveProfile(pName).handsPlayed++;
      }
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
          int livePot = pot.getTotalPot();
          for(int c : currConts) livePot += c;
          currAction = temp.action("preflop", currConts[i], currBet, blinds, lastRaise, null, livePot, players, i, preflopAggressorIndex, 0, 1);
        } else {
          System.out.println("Are you " + players[i].getName() + "? Press Enter to confirm and show your hand...");
          Utils.flushInput();
          sc.nextLine();
          Utils.clearScreen();
          System.out.print(roundName + pot.toString() + "\n\n" + roundHistory);
          System.out.print(turnHeader);
          currAction = players[i].action("preflop", currConts[i], currBet, blinds, lastRaise);
        }

        String actionLog = handleAction(i);
        if (currAction[0] == 3 || currAction[0] == 4) {
          if (currAction[1] > 0) preflopAggressorIndex = i;
        }
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
    finalizePreflopTelemetry();
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
    for (int idx = 0; idx < players.length; idx++) {
      sawFlopThisHand[idx] = players[idx].inHand();
    }
    cbetFiredOnFlop = false;
    cbetAggressorIndex = -1;
    Card[] b = p.deal();
    ArrayList<Card> boardForBot = new ArrayList<>();
    boardForBot.add(b[0]);
    boardForBot.add(b[1]);
    boardForBot.add(b[2]);
    lastPlayer = 0;
    currBet = 0;
    lastRaise = blinds; // Post-flop min-lead is 1BB
    int i = 0;
    for (int j = 0; j < 3; j++) {
      currentStreet = j + 1; // PHASE 8: 1=Flop, 2=Turn, 3=River
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
            int livePot = pot.getTotalPot();
            for(int c : currConts) livePot += c;
            currAction = temp.action("postflop", currConts[i], currBet, blinds, lastRaise, boardForBot.toArray(new Card[j + 3]), livePot, players, i, preflopAggressorIndex, 0, 1);
          } else {
            System.out.println("Are you " + players[i].getName() + "? Press Enter to confirm and show your hand...");
            Utils.flushInput();
            sc.nextLine();
            Utils.clearScreen();
            System.out.print(roundName + boardStr + "\n\n" + pot.toString() + "\n\n" + roundHistory);
            System.out.print(turnHeader);
            currAction = players[i].action("postflop", currConts[i], currBet, blinds, lastRaise);
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
      lastRaise = blinds; // Reset min-lead to 1BB for the next street
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
    isShowdown = (c == 1); // PHASE 8: Flag if went to showdown
    finalizePostflopTelemetry(c);
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
    preflopAggressorIndex = -1;
    players[0].setStatus(0);
    players[1].setStatus(0);
    PokerPlayer first = players[players.length - 1];
    for (int i = players.length - 1; i > 0; i--)
      players[i] = players[i - 1];
    players[0] = first;
    int handsPerRound = (players.length <= 8) ? (players.length * 3) : (players.length * 2);
    if (hands >= handsPerRound) {
      if (blinds < 320) {
        blinds *= 2;
        System.out.println("Blinds increasing to ✨" + blinds);
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
        int godBotCount = 0;
        for (PokerPlayer p : players) {
          if (p instanceof PokerBot && ((PokerBot) p).getBotLevel() == 2)
            godBotCount++;
        }
        ArrayList<Integer> botIndices = new ArrayList<>();
        for (int k = 0; k < players.length; k++) {
          if (players[k] instanceof PokerBot) {
            if (godBotCount > 1 || ((PokerBot) players[k]).getBotLevel() != 2)
              botIndices.add(k);
          }
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

  private boolean shouldTrackCognitivePlayer(PokerPlayer player) {
    if (player == null)
      return false;
    if (!(player instanceof PokerBot))
      return true; // Always track humans
    if (PokerBot.testLearnFromBots)
      return true; // Test mode: include Dumb/Smart bots too
    return ((PokerBot) player).getBotLevel() == 2; // Production mode: only God bots
  }

  private void markPreflopActionForTelemetry(int playerIndex, int preActionTableBet, int preActionContribution, int paid) {
    if (currentStreet != 0 || preflopVPIPFlags == null || preflopPFRFlags == null)
      return;
    if (playerIndex < 0 || playerIndex >= players.length)
      return;
    if (!shouldTrackCognitivePlayer(players[playerIndex]))
      return;

    boolean contributed = paid > 0 && currConts[playerIndex] > preActionContribution;
    boolean isAggressiveAction = (currAction[0] == 3 || currAction[0] == 4);
    boolean isRaise = isAggressiveAction && currConts[playerIndex] > preActionTableBet;

    if (contributed && (currAction[0] == 1 || isAggressiveAction)) {
      preflopVPIPFlags[playerIndex] = true;
    }
    if (isRaise) {
      preflopPFRFlags[playerIndex] = true;
      preflopVPIPFlags[playerIndex] = true;
    }
  }

  private void finalizePreflopTelemetry() {
    if (preflopVPIPFlags == null || preflopPFRFlags == null)
      return;

    for (int i = 0; i < players.length; i++) {
      if (!shouldTrackCognitivePlayer(players[i]))
        continue;
      String pName = players[i].getName();
      PokerBot.CognitiveProfile profile = PokerBot.getOrCreateCognitiveProfile(pName);
      profile.updatePreflopTelemetry(preflopVPIPFlags[i], preflopPFRFlags[i], PREFLOP_EMA_ALPHA);
    }

    preflopVPIPFlags = null;
    preflopPFRFlags = null;
  }

  private String getAFqStreetStatKey(int street) {
    if (street == 1)
      return "AFq_Flop";
    if (street == 2)
      return "AFq_Turn";
    if (street == 3)
      return "AFq_River";
    return "AFq_Preflop";
  }

  private void markPostflopActionForTelemetry(int playerIndex, int preActionTableBet, int preActionContribution, int paid) {
    if (currentStreet < 1 || currentStreet > 3)
      return;
    if (postflopAggressionActions == null || postflopAggressionOpportunities == null)
      return;
    if (playerIndex < 0 || playerIndex >= players.length)
      return;
    if (!shouldTrackCognitivePlayer(players[playerIndex]))
      return;

    boolean isAggressiveAction = (currAction[0] == 3 || currAction[0] == 4) && currConts[playerIndex] > preActionTableBet;
    boolean isCall = (currAction[0] == 1 && paid > 0);
    boolean isFoldFacingBet = (currAction[0] == 2 && preActionTableBet > preActionContribution);

    if (isAggressiveAction || isCall || isFoldFacingBet) {
      postflopAggressionOpportunities[playerIndex][currentStreet]++;
      if (isAggressiveAction)
        postflopAggressionActions[playerIndex][currentStreet]++;
    }

    if (currentStreet == 1) {
      if (!cbetFiredOnFlop && preActionTableBet == 0 && playerIndex == preflopAggressorIndex && isAggressiveAction) {
        cbetFiredOnFlop = true;
        cbetAggressorIndex = playerIndex;
      }
      if (cbetFiredOnFlop && playerIndex != cbetAggressorIndex && preActionTableBet > preActionContribution) {
        foldToCbetOpportunity[playerIndex] = true;
        if (currAction[0] == 2)
          foldedToCbet[playerIndex] = true;
      }
    }
  }

  private void finalizePostflopTelemetry(int showdownMode) {
    if (postflopAggressionActions == null || postflopAggressionOpportunities == null)
      return;

    for (int i = 0; i < players.length; i++) {
      if (!shouldTrackCognitivePlayer(players[i]))
        continue;
      String pName = players[i].getName();
      PokerBot.CognitiveProfile profile = PokerBot.getOrCreateCognitiveProfile(pName);

      for (int street = 1; street <= 3; street++) {
        int opps = postflopAggressionOpportunities[i][street];
        if (opps > 0) {
          double afqValue = (double) postflopAggressionActions[i][street] / opps;
          profile.updateEMA(getAFqStreetStatKey(street), afqValue, POSTFLOP_EMA_ALPHA);
        }
      }

      if (foldToCbetOpportunity[i]) {
        profile.updateEMA("FoldToCBet", foldedToCbet[i] ? 1.0 : 0.0, POSTFLOP_EMA_ALPHA);
      }

      if (sawFlopThisHand != null && sawFlopThisHand[i]) {
        boolean reachedShowdown = (showdownMode == 1 && players[i].inHand());
        profile.updateEMA("WTSD", reachedShowdown ? 1.0 : 0.0, POSTFLOP_EMA_ALPHA);
      }
    }

    postflopAggressionActions = null;
    postflopAggressionOpportunities = null;
    sawFlopThisHand = null;
    foldToCbetOpportunity = null;
    foldedToCbet = null;
    cbetFiredOnFlop = false;
    cbetAggressorIndex = -1;
  }

  private String handleAction(int i) { // do certain things based on a player's action
    String log = "";
    int paid = 0;
    int preActionTableBet = currBet;
    int preActionContribution = currConts[i];
    
    switch (currAction[0]) {
      case 1: // CALL/CHECK
        paid = pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += paid;
        if (players[i] == mainPlayer) mp.addBet(paid);
        
        if (players[i].status() == 2) {
          if (paid > 0) log = players[i].getName() + " in big blind CALLS FOR ✨" + currConts[i] + ".";
          else log = players[i].getName() + " in big blind CHECKS.";
        } else {
          if (paid > 0) log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "CALLS FOR ✨" + currConts[i] + ".";
          else log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "CHECKS.";
        }
        break;
        
      case 2: // FOLD
        players[i].setInHand(false);
        if (players[i].status() == 2) log = players[i].getName() + " in big blind FOLDS.";
        else log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : " ") + "FOLDS.";
        break;
        
      default: // BET/RAISE/ALL-IN
        if (players[i] == mainPlayer && currAction[0] == 4) mp.allIn();
        
        // Actually move the chips
        paid = pot.addPlayerContribution(i, currAction[1]);
        currConts[i] += paid;
        if (players[i] == mainPlayer) mp.addBet(paid);

        // Determine action name for log
        String actionVerb = (currAction[0] == 4) ? "GOES ALL IN FOR" : (currBet == 0 ? "BETS" : "RAISES TO");
        
        // Log based on ACTUAL contribution
        log = players[i].getName() + ((players[i].status() == 1) ? " in small blind " : (players[i].status() == 2 ? " in big blind " : " "))
            + actionVerb + " ✨" + currConts[i] + ".";

        // Update table bet state
        if (currConts[i] > currBet) {
          int increment = currConts[i] - currBet;
          lastPlayer = i; // Ensure everyone responds to the new bet amount
          // Only change the minimum raise size if it's a FULL raise
          if (increment >= lastRaise) {
              lastRaise = increment;
          }

          // PHASE 8 COGNITIVE MATRIX TRACKING (Only run for Humans)
          if (!(players[i] instanceof PokerBot)) {
             String pName = players[i].getName();
             PokerBot.CognitiveProfile profile = PokerBot.getOrCreateCognitiveProfile(pName);
             // Massive overbets or jams
             if (currAction[0] == 4 || increment > currBet * 1.5) {
               profile.aggressiveActions += 2;
             } else {
               profile.aggressiveActions += 1;
             }
          }
          currBet = currConts[i];
        }
        break;
    }
    markPreflopActionForTelemetry(i, preActionTableBet, preActionContribution, paid);
    markPostflopActionForTelemetry(i, preActionTableBet, preActionContribution, paid);
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