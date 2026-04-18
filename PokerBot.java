import java.util.*;

public class PokerBot extends PokerPlayer {
  private boolean opMode = false;
  private PokerDeck p = new PokerDeck();
  private int[][] hands = { { 14, 14 }, { 14, 13 }, { 14, 12 }, { 14, 11 }, { 14, 10 }, { 14, 9 }, { 14, 8 }, { 14, 2 },
      { 13, 13 }, { 13, 12 }, { 13, 11 }, { 13, 10 }, { 13, 9 }, { 13, 8 }, { 12, 12 }, { 12, 11 }, { 12, 10 },
      { 12, 9 }, { 12, 8 }, { 11, 11 }, { 11, 10 }, { 11, 9 }, { 11, 8 }, { 10, 10 }, { 9, 9 }, { 8, 8 }, { 7, 7 },
      { 6, 6 }, { 5, 5 }, { 4, 4 }, { 3, 3 }, { 2, 2 } }; // preset hands for smart bot
  public int botLevel; // 0 = dumb, 1 = smart, 2 = god
  private boolean cbetFlop = false; // Persistent state for barrelling logic
  private boolean predatoryIntent = false; // "Two-Faced" nightmare personality
  private String baseName; // Store original name to allow tag refreshing

  private boolean revealTags = false; // Persistent memory for trigger detection
  private boolean nightmareActive = false; // Persistent memory for nightmare mode
  private boolean protectedMode = false; // "Protected Mode": Strips exploits for pure GTO testing

  public PokerBot(PokerPlayer[] currentPlayers) {
    super("temp");
    randomName(currentPlayers);
    double r = Math.random();
    if (r < 0.44)
      botLevel = 0;
    else if (r < 0.88)
      botLevel = 1;
    else
      botLevel = 2;

    this.baseName = super.getName();
    refreshNameTag(currentPlayers);

    if (super.getName().contains("Aventurine")) {
      opMode = true;
    }
  }

  public void refreshNameTag(PokerPlayer[] playersForNightmareCheck) {
    // Trigger Check: Only update global state if a player list is provided
    if (playersForNightmareCheck != null) {
      this.revealTags = false;
      this.nightmareActive = false;
      for (PokerPlayer p : playersForNightmareCheck) {
        if (p != null) {
          if ("edj".equalsIgnoreCase(p.getName()) || "edjiang1234".equalsIgnoreCase(p.getName())) {
            this.revealTags = true;
          }
          if ("edjiang1234".equalsIgnoreCase(p.getName())) {
            this.nightmareActive = true;
            if (botLevel != 2) {
              botLevel = 2; // Sync level if nightmare triggered
              predatoryIntent = (Math.random() < 0.5);
            }
          }
        }
      }
    }

    String tag = "";
    if (this.revealTags) {
      if (botLevel == 0)
        tag = " [D]";
      else if (botLevel == 1)
        tag = " [S]";
      else {
        if (this.nightmareActive) {
          tag = predatoryIntent ? " [G-B]" : " [G-S]";
        } else {
          tag = " [G]";
        }
      }
    }
    super.setName(this.baseName + tag);
  }

  public PokerBot() {
    this(null);
  }

  public void randomName(PokerPlayer[] currentPlayers) {
    super.setName(Names.getUniqueName(currentPlayers));
  }

  public void randomName() {
    randomName(null);
  }

  public int getBotLevel() {
    return botLevel;
  }


  public void setBotLevel(int level) {
    this.botLevel = level;
    if (level == 2 && !protectedMode) predatoryIntent = (Math.random() < 0.5); // Re-roll intent for promoted gods (disable if protected)
    refreshNameTag(null); // Simple refresh (Nightmare mode handled in constructor or re-verified here if needed)
  }

  public void setProtectedMode(boolean val) {
    this.protectedMode = val;
    if (val) predatoryIntent = false; // Immediately disable aggression
    refreshNameTag(null);
  }

  // funny
  public void checkName() {
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public int[] action(String round, int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize, PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int numPlayers = players.length;

    int tablePlayers = 0;
    for (PokerPlayer p : players) if (p.getChips() > 0) tablePlayers++;
    boolean headsUpTable = (tablePlayers == 2);
    
    int activeCount = 0;
    for (PokerPlayer p : players) if (p.inHand()) activeCount++;
    boolean headsUpHand = (activeCount == 2);

    if (botLevel == 2) {
      if (round.equals("preflop")) {
        return godPreflop(prevBet, bet, blind, lastRaise, players, seatIndex, sbIdx, bbIdx);
      } else {
        return godPostflop(prevBet, bet, blind, lastRaise, board, potSize, players, seatIndex, preflopAggressorIndex, sbIdx, bbIdx);
      }
    } else if (botLevel == 0) { // idiot bot code, fixed percentages for all situations no matter what
      int[] action = new int[2];
      double rand = Math.random();
      if (opMode && super.getChips() > 0) {
        action[0] = 4;
        action[1] = super.getChips();
      } else if (bet < super.getChips()) {
        if (rand >= 0 && rand < 0.75) { // 75% chance to call
          action[0] = 1;
          if (bet > 0) {
            action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
          } else
            action[1] = (bet == 0) ? 0 : bet - prevBet;
        } else if (rand >= 0.75 && rand < 0.85) { // 10% chance to raise
          if (((bet == 0) ? blind : bet + blind) + super.getChips() / 10 < super.getChips()) {
            // however, only raises if the bet meets certain conditions
            int max;
            int min;
            if (round.equals("preflop")) {
              max = super.getChips() / 10 + bet + lastRaise;
              min = bet + lastRaise;
            } else {
              if (bet == 0) {
                max = super.getChips() / 10 + lastRaise;
                min = lastRaise;
              } else {
                max = super.getChips() / 10 + bet + lastRaise;
                min = bet + lastRaise;
              }
            }
            action[0] = 3;
            int raiseTo = (int) (Math.random() * (max - min + 1) + min);
            action[1] = raiseTo - prevBet;
          } else {
            // if those "conditions" are not met, then has 15% to continue and all in the current bet, otherwise folds.
            if (Math.random() > 0.85) {
              action[0] = 4;
              action[1] = super.getChips();
            } else {
              action[0] = 2;

            }
          }
        } else if (rand >= 0.85 && rand < 0.97) { // 12% chance to call/fold
          action[0] = (bet == 0) ? 1 : 2;
        } else { // 3% chance to all in
          action[0] = 4;
          action[1] = super.getChips();
        }
      } else {
        // Dumb Bot Extinction Rule: 50% Call, 50% Fold
        if (Math.random() < 0.5) {
          action[0] = 4;
          action[1] = super.getChips();
        } else {
          action[0] = 2;
          action[1] = 0;
        }
      }
      return action;
    } else { // intelligent bot code, varying percentages based on current siutation
      int[] action = new int[2];
      if (round.equals("preflop")) {
        /*
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
                int raiseTo = (bet == 0) ? (int) (blind * (Math.random() + 2)) : (int) (bet * (Math.random() + 2));
                raiseTo = Math.max(raiseTo, bet + lastRaise); // Standardized Floor
                action[1] = Math.min(raiseTo - prevBet, super.getChips());
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
            break;
          }
        }
        if (!isHand) {
          double callAirFreq = (headsUpTable) ? 0.70 : 0.25;
          int[] cardInts = Deck.cardToInt(super.getHand());
          if (headsUpTable && (cardInts[0] >= 12 || cardInts[1] >= 12)) callAirFreq = 1.0; // Play any Q+ preflop 1v1

          if (bet < super.getChips() / 2 && Math.random() < callAirFreq) {
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
          if (rank == 6 || rank == 7 || ((draw[0] || draw[1]) && board.length < 5)) {
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
              if (draw[0] || draw[1]) {
                 if (Math.random() < 0.50) subAction = 0; else subAction = 4; // Gamble on draws 50%
              } else {
                  double foldRate = (headsUpHand) ? 0.25 : 0.50; // Reduce panic fold 1v1
                  if (rand <= 30) {
                    subAction = 0;
                  } else if (rand > 30 && rand <= 45) {
                    subAction = 1;
                  } else if (rand > 45 && rand <= 50) {
                    subAction = 2;
                  } else if (Math.random() < (1.0 - foldRate)) {
                    subAction = 0; // Call instead of fold
                  } else {
                    subAction = 4;
                  }
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
        
        // Override removed to bring bust rate up to 20-30%
        
        int raiseTo;
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
            raiseTo = (int) ((Math.random() * .6 + 2) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
            break;
          case 2:
            action[0] = 3;
            raiseTo = (int) ((Math.random() * 1.6 + 2.5) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
            break;
          case 3:
            double upper = (double) super.getChips() / bet;
            action[0] = 3;
            raiseTo = (int) ((Math.random() * (upper - 2 + 0.1) + 4) * bet);
            raiseTo = Math.max(raiseTo, bet + lastRaise);
            action[1] = raiseTo - prevBet;
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

  private int[] godPreflop(int prevBet, int bet, int blind, int lastRaise, PokerPlayer[] players, int seatIndex, int sbIdx, int bbIdx) {
    int numPlayers = players.length;
    int relativePos = (seatIndex - sbIdx + numPlayers) % numPlayers;
    
    int[] action = new int[2];
    int[] numhand = Deck.cardToInt(super.getHand());
    Arrays.sort(numhand);
    boolean paired = numhand[0] == numhand[1];
    boolean suited = super.getHand()[0].getValue().charAt(1) == super.getHand()[1].getValue().charAt(1);
    boolean premium = (paired && numhand[0] >= 10) || (numhand[0] >= 13 && numhand[1] >= 13) || (numhand[1] == 14 && numhand[0] >= 11);
    
    boolean earlyPos = (relativePos >= 2 && relativePos <= 3);
    boolean latePos = (relativePos >= numPlayers - 2);
    boolean inBlinds = (seatIndex == sbIdx || seatIndex == bbIdx);
    boolean unraised = (bet == blind || bet == 0);
    
    boolean chipleader = true;
    int tablePlayers = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
          tablePlayers++;
          if (p != this && p.getChips() > super.getChips()) chipleader = false;
      }
    }
    boolean headsUpTable = (tablePlayers == 2);
    boolean shortStacks = true;
    int dumbBotCount = 0;
    int smartBotCount = 0;
    for (PokerPlayer p : players) {
      if (p.getChips() > 0) {
        if (p.inHand() && p != this && p.getChips() > blind * 20) shortStacks = false;
        
        if (p instanceof PokerBot && p != this) {
            int level = ((PokerBot)p).botLevel;
            if (level == 0) dumbBotCount++;
            else if (level == 1) smartBotCount++;
        }
      }
    }
    
    // Split-Brain Trigger: Only be "Spicy" if only Humans or other God Bots are in the pot
    boolean isThinkingOpponentOnly = (dumbBotCount == 0 && smartBotCount == 0);
    
    boolean stealRange = paired || numhand[1] >= 14 || (suited && numhand[1] - numhand[0] <= 4);
    
    // GTO Hardening: Balanced Early Range (Matching Smart Bot + Suited Wheel Aces) - SPICY MODE ONLY
    boolean wheelAce = !protectedMode && isThinkingOpponentOnly && (suited && numhand[1] == 14 && numhand[0] >= 2 && numhand[0] <= 9);
    boolean faceCards = !protectedMode && isThinkingOpponentOnly && ((numhand[1] >= 13 && numhand[0] >= 10) || (numhand[1] == 12 && numhand[0] >= 11));
    boolean earlyRange = premium || paired || faceCards || wheelAce;
    
    // Mixed Strategy Anomaly (15%): Playing GTO Gappers/Trash from any position - SPICY MODE ONLY
    boolean mixedStrategy = !protectedMode && isThinkingOpponentOnly && (Math.random() < 0.15 && (suited && numhand[1] - numhand[0] <= 5));
    
    // Heads-Up Tournament Protocol: Any Ace, King, Queen or Pair becomes Premium
    if (headsUpTable) {
        if (numhand[1] >= 12 || paired) premium = true;
    }

    // Fortress Defense: Active Defensive Ranges
    boolean defenseRange = earlyRange || stealRange;
    
    if (premium || (chipleader && shortStacks && stealRange) || mixedStrategy) {
      action[0] = 3;
      int intended = (bet > blind) ? (bet * 3) : (blind * 3);
      int raiseTo = Math.min(Math.max(intended, bet + lastRaise), super.getChips());
      action[1] = raiseTo - prevBet;
    } else if (bet > blind && isThinkingOpponentOnly && !protectedMode) {
      // SPICY MODE: Active Defense Logic
      double threeBetChance = 0.35; // 35% chance to re-raise bluffs
      
      if (defenseRange && Math.random() < threeBetChance) {
          action[0] = 3; 
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips());
          action[1] = raiseTo - prevBet;
      } else if (seatIndex == bbIdx && bet <= blind * 4.5 && defenseRange) {
          action[0] = 1; action[1] = bet - prevBet; // Big Blind Defense (Sticky Call)
      } else if (latePos && bet <= blind * 3.5 && (paired || wheelAce)) {
          action[0] = 1; action[1] = bet - prevBet; // Positional Flatting (Call in position)
      } else if (headsUpTable) {
          action[0] = 3; 
          int raiseTo = Math.min(Math.max(bet * 3, bet + lastRaise), super.getChips());
          action[1] = raiseTo - prevBet;
      } else {
          action[0] = 2; action[1] = 0; // Still fold trash
      }
    } else if (unraised && latePos && stealRange) {
      action[0] = 3;
      action[1] = Math.min(blind * 3, super.getChips());
    } else if (unraised && inBlinds) {
      if (bet <= super.getChips() / 4) {
        action[0] = 1;
        action[1] = bet > 0 ? bet - prevBet : 0;
      } else {
         action[0] = 2; action[1] = 0;
      }
    } else if (earlyPos && earlyRange) {
      action[0] = 3; action[1] = Math.min(blind * 3, super.getChips());
    } else {
      if (headsUpTable) {
          action[0] = 3; action[1] = Math.max(bet * 3, blind * 3);
      } else {
          action[0] = 2; action[1] = 0;
          if (bet == 0) { action[0] = 1; action[1] = 0; }
      }
    }
    
    if (action[0] == 3 && !protectedMode) {
      int noise = (int)(Math.random() * 11) - 5; // Humanoid Noise: +/- 5 chips
      int raiseTo = (prevBet + action[1]) + noise;
      // FINAL LEGAL GUARD: Ensure the noisy bet still respects the increment floor
      int legalMin = bet + lastRaise;
      if (raiseTo < legalMin) raiseTo = Math.min(super.getChips(), legalMin + (int)(Math.random() * 5));
      action[1] = raiseTo - prevBet;
    }
    
    if (action[1] >= super.getChips()) { action[0] = 4; action[1] = super.getChips(); }
    cbetFlop = false; // Reset barrelling state for new hand
    return action;
  }

  private int[] godPostflop(int prevBet, int bet, int blind, int lastRaise, Card[] board, int potSize, PokerPlayer[] players, int seatIndex, int preflopAggressorIndex, int sbIdx, int bbIdx) {
    int[] action = new int[2];
    Card[] fullHand = new Card[5];
    if (board.length == 3) {
      fullHand[0] = super.getHand()[0];
      fullHand[1] = super.getHand()[1];
      for (int i = 0; i < 3; i++) fullHand[i + 2] = board[i];
    } else {
      fullHand = p.getBestHand(super.getHand(), board);
    }
    
    int myRank = p.getRanking(fullHand);
    
    Card[] bestBoard = null;
    int boardRank = 9;
    if (board.length >= 5) {
      bestBoard = p.getBestHand(new Card[0], board);
      boardRank = p.getRanking(bestBoard);
    }
    
    Card[] total = new Card[2 + board.length];
    total[0] = super.getHand()[0];
    total[1] = super.getHand()[1];
    for (int i = 0; i < board.length; i++) total[i + 2] = board[i];
    boolean[] draws = draw(total);
    int outs = 0;
    if (draws[0]) outs += 8;
    if (draws[1]) outs += 9;
    double equity = (board.length == 3) ? (outs * 4) / 100.0 : (board.length == 4 ? (outs * 2) / 100.0 : 0);
    
    boolean zeroBet = (bet == 0);
    double costToCall = zeroBet ? 0 : bet - prevBet;
    double potOdds = costToCall / Math.max(1, (double)(potSize + costToCall));
    
    int act = 1; 
    int actAmount = zeroBet ? 0 : bet - prevBet;
    
    boolean cbet = (board.length == 3 && preflopAggressorIndex == seatIndex);
    
    boolean flushScare = false;
    String scareSuit = "";
    int[] flushC = new int[4];
    for (Card d : board) {
       switch(d.getValue().substring(1)) {
          case "♠️": flushC[0]++; if(flushC[0]>=3) scareSuit="♠️"; break;
          case "♣️": flushC[1]++; if(flushC[1]>=3) scareSuit="♣️"; break;
          case "♦️": flushC[2]++; if(flushC[2]>=3) scareSuit="♦️"; break;
          case "♥️": flushC[3]++; if(flushC[3]>=3) scareSuit="♥️"; break;
       }
    }
    for (int i : flushC) if (i >= 3) flushScare = true;
    
    // Elite Awareness: Straight Scares and Paired Boards
    boolean straightScare = false;
    if (board.length >= 4) {
      Card[] bSorted = board.clone();
      Deck.sort(bSorted);
      int con = 0;
      for (int i = 1; i < bSorted.length; i++) {
        if (bSorted[i].getNum() == bSorted[i-1].getNum() + 1) con++;
        else if (bSorted[i].getNum() != bSorted[i-1].getNum()) con = 0;
        if (con >= 3) straightScare = true;
      }
    }
    
    
    boolean aceHighBoard = false;
    for (Card d : board) if (d.getNum() == 14) aceHighBoard = true;
    
    boolean nutBlocker = false;
    if (flushScare && myRank > 4) { 
       if ((super.getHand()[0].getValue().charAt(1) == scareSuit.charAt(0) && super.getHand()[0].getNum() == 14) ||
           (super.getHand()[1].getValue().charAt(1) == scareSuit.charAt(0) && super.getHand()[1].getNum() == 14)) {
           nutBlocker = true;
       }
    }
    
    int smartBotCount = 0;
    int dumbBotCount = 0;
    int godBotCount = 0;
    int largestSmartStack = 0;
    int largestDumbStack = 0;
    int largestOpponentStack = 0;
    int activeCount = 0;
    for (PokerPlayer pr : players) {
      if (pr.inHand()) activeCount++;
      if (pr.inHand() && pr != this) {
         if (pr.getChips() > largestOpponentStack) largestOpponentStack = pr.getChips();
         if (pr instanceof PokerBot) {
                int level = ((PokerBot)pr).botLevel;
                if (level == 1) { smartBotCount++; if (pr.getChips() > largestSmartStack) largestSmartStack = pr.getChips(); }
                else if (level == 0) { dumbBotCount++; if (pr.getChips() > largestDumbStack) largestDumbStack = pr.getChips(); }
                else if (level == 2) godBotCount++;
         }
      }
    }
    boolean headsUpHand = (activeCount == 2);
    double depthRatio = (largestOpponentStack > 0) ? (double) super.getChips() / largestOpponentStack : 1.0;

    boolean isNightmareMode = false;
    for (PokerPlayer pr : players) if (pr != null && "edjiang1234".equalsIgnoreCase(pr.getName())) isNightmareMode = true;

    boolean isThinkingOpponentOnly = (dumbBotCount == 0 && smartBotCount == 0);
    boolean isBeingBullied = (isThinkingOpponentOnly && depthRatio < 0.5 && !protectedMode);
    boolean predatoryMode = (headsUpHand && (dumbBotCount > 0 || (isNightmareMode && predatoryIntent)) && !protectedMode);
    boolean exploitingSmartBot = (smartBotCount > 0 && dumbBotCount == 0 && !protectedMode); 
    boolean minusOneActive = false;
    double bluffSizeVsSmart = (largestSmartStack > 0) ? (largestSmartStack * 0.5) + 1 : potSize;

    // Heuristic Sizing Scanner (Soul Reading) - Restricted to Multi-Player Hands
    boolean soulReadSmartBot = false;
    if (!zeroBet && smartBotCount > 0 && !headsUpHand && !protectedMode) {
       double baseBet = (prevBet > 0) ? prevBet : blind;
       if (bet >= baseBet * 2.6) soulReadSmartBot = true;
    }

    if (soulReadSmartBot && myRank > 3) {
       act = 2; // Mathematically deduced Smart Bot has Full House+
    } else if (nutBlocker && isThinkingOpponentOnly) {
       act = 3; actAmount = exploitingSmartBot ? (int)bluffSizeVsSmart : Math.max(potSize, super.getChips());
    } else if (board.length >= 5 && myRank >= boardRank && p.compareHands(fullHand, bestBoard) == 0) {
      // Defensive Awareness: If board is terrifying and we just have a pair, fold to big pressure
      // SPICY OVERRIDE: If being bullied, stay suspicious and don't fold Top Pairs or better
      if (!zeroBet && (flushScare || straightScare) && costToCall > potSize * 0.5 && myRank > 5 && !isBeingBullied) {
         act = 2;
      } else {
         if (zeroBet) act = 1; else {
            if (isBeingBullied && myRank <= 8) { act = 1; actAmount = bet - prevBet; }
            else act = 2;
         }
      }
    } else if (myRank <= 3 || (isBeingBullied && myRank <= 8)) { 
      // Trapping Logic (Slow-play): 20% chance to check/call monster hands to bait bluffs - SPICY MODE ONLY
      boolean trapMode = (!protectedMode && isThinkingOpponentOnly && Math.random() < 0.20 && (board.length == 3 || board.length == 4));
      
      if (dumbBotCount > 0 && largestDumbStack > 0 && !protectedMode) {
          act = 3; actAmount = Math.max(potSize, largestDumbStack - 1); // MINUS ONE EXPLOIT
          minusOneActive = true;
      } else if (trapMode) {
          if (zeroBet) act = 1; else { act = 1; actAmount = bet - prevBet; }
      } else {
          // Iron Chin: Snap-calling bullies with Hero Ranges (Pairs or better)
          if (isBeingBullied && !zeroBet) {
             act = 1; actAmount = bet - prevBet;
          } else {
             double valueMult = (depthRatio > 1.5 && !protectedMode) ? 1.0 : 0.75; // Big Stack Bully widening (disable if protected)
             if (zeroBet) { act = 3; actAmount = (int)(potSize * valueMult); } 
             else { act = 3; actAmount = bet * 3; }
          }
      }
    } else if (myRank <= (headsUpHand ? 8 : 7) && dumbBotCount > 0 && !draws[0] && !draws[1]) {
      // Hyper-Thin Value Betting vs Dumb Bots (Isolated 1v1 expands threshold to Any Pair)
      if (zeroBet) { act = 3; actAmount = potSize; }
      else { act = 3; actAmount = Math.max(potSize, bet * 2); }
    } else if (myRank <= 5) { 
      if (zeroBet) { act = 3; actAmount = (int)(potSize * 0.5); }
      else { 
        if (costToCall > super.getChips() * 0.5) { act = 1; actAmount = bet - prevBet; } 
        else { act = 3; actAmount = bet * 2; }
      }
    } else if (myRank <= 7 || draws[0] || draws[1]) { 
      // GTO Hardening: Semi-Bluffing (40% chance to lead draws aggressively) - SPICY MODE ONLY
      boolean semiBluff = (isThinkingOpponentOnly && (draws[0] || draws[1]) && Math.random() < 0.40);
      
      // Elite Range Advantage: Ace-high boards C-bet 90% of the time
      double cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
      double barrelFreq = 0.70;
      
      // Dynamic Stack-Depth Aggression Scaling
      if (depthRatio > 1.5) { cbetFreq = Math.min(1.0, cbetFreq + 0.15); barrelFreq = Math.min(1.0, barrelFreq + 0.15); }
      else if (depthRatio <= 0.5 && !predatoryMode) { cbetFreq = 0.0; barrelFreq = 0.0; } // Survival mode (disabled in predatory)
      
      if (dumbBotCount > 0) {
          cbetFreq = (headsUpHand) ? 0.60 : 0.0; // PREDATORY BLUFFING 1v1 vs Dumb Bots
      }

      if (zeroBet && cbet && Math.random() < cbetFreq) {
         act = 3; actAmount = (int)(Math.max(potSize * 0.4, blind));
         if (board.length == 3) cbetFlop = true;
      } else if (zeroBet && semiBluff) {
         act = 3; actAmount = (int)(potSize * 0.75); // Lead aggressively on semi-bluff
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < barrelFreq) {
         // Triple Barreling: Turn is a face card and we C-bet flop
         act = 3; actAmount = (int)(potSize * 0.6);
      } else if (!zeroBet && semiBluff) {
         act = 3; actAmount = bet * 3; // Aggressive Raise on semi-bluff
      } else if (!zeroBet && (draws[0] || draws[1]) && equity > potOdds) {
         act = 1; actAmount = bet - prevBet;
      } else if (!zeroBet && costToCall <= super.getChips() * 0.20 && costToCall > 0) { 
         act = 1; actAmount = bet - prevBet;
      } else {
         double huFoldChance = (headsUpHand) ? 0.25 : 0.85; // Intelligent Stickiness 1v1
         if (zeroBet) act = 1; 
         else if (headsUpHand && Math.random() > huFoldChance) { act = 1; actAmount = bet - prevBet; }
         else act = 2;
      }
    } else if (predatoryMode && myRank <= 10) {
       // Ultra-Thin Value: Betting Ace-High against Dumb Bots 1v1
       if (zeroBet) { act = 3; actAmount = (int)(potSize * 0.5); }
       else { act = 1; actAmount = bet - prevBet; }
    } else { 
      double cbetFreq = (aceHighBoard) ? 0.90 : 0.65;
      
      // Dynamic Stack-Depth Aggression Scaling
      if (depthRatio > 1.5) { cbetFreq = Math.min(1.0, cbetFreq + 0.15); }
      else if (depthRatio <= 0.5 && !predatoryMode) { cbetFreq = 0.0; } // Survival mode (disabled in predatory)
      
      if (dumbBotCount > 0) cbetFreq = (headsUpHand) ? 0.60 : 0.0; // PREDATORY BLUFFING 1v1 vs Dumb Bots

      if (zeroBet && cbet && Math.random() < cbetFreq) {
         act = 3; actAmount = (int)(Math.max(potSize * 0.4, blind)); 
         if (board.length == 3) cbetFlop = true;
      } else if (board.length == 4 && cbetFlop && board[3].getNum() >= 11 && Math.random() < 0.70) {
         // Triple Barreling
         act = 3; actAmount = (int)(potSize * 0.6);
      } else {
         act = zeroBet ? 1 : 2; 
      }
    }
    
    // Meta-Bluffing Level 3: Overbetting to exploit other God Bots who respect math
    if (!protectedMode && godBotCount > 0 && dumbBotCount == 0 && Math.random() < 0.12 && act == 2) {
       act = 3;
       actAmount = (int)(potSize * (1.5 + Math.random()));
    }
    
    // Humanoid Noise: Adding +/- 5 chips to break predictable denominations
    if (act == 3 && !minusOneActive) {
       actAmount += (int)(Math.random() * 11) - 5;
    }
    
    // FINAL POST-FLOP LEGAL GUARD: Ensure noise didn't violate the increment rule
    if (act == 3) {
       int legalMin = bet + lastRaise;
       if (actAmount < legalMin) actAmount = Math.min(super.getChips() + prevBet, legalMin);
    }
    
    if (dumbBotCount > 0 && act == 3 && myRank > 7 && !headsUpHand) act = 1; // Only eradicate bluffs in multi-player pots
    if (act == 3 && actAmount <= bet) actAmount = bet + Math.max(lastRaise, blind); // Ensure legal raise (Official Increment Rule)
    if (act == 3 && actAmount == 0) actAmount = Math.max(lastRaise, blind);
    
    if (act == 3 && actAmount >= super.getChips() * 0.9 && !minusOneActive) { act = 4; } 

    // NUCLEAR PREDATOR OVERRIDE: 1v1 against fish, NEVER fold top-pair+
    if (predatoryMode && act == 2 && myRank <= 8) act = 1; 

    if (act == 3 && actAmount > super.getChips() + prevBet) { act = 4; actAmount = super.getChips() + prevBet; }
    if (act == 4) { action[0] = 4; action[1] = super.getChips(); }
    else if (act == 3) { action[0] = 3; action[1] = actAmount - prevBet; }
    else if (act == 2) { action[0] = zeroBet ? 1 : 2; action[1] = 0; }
    else { action[0] = 1; action[1] = zeroBet ? 0 : bet - prevBet; } // Standardized increment
    
    if (action[1] >= super.getChips()) { action[0] = 4; action[1] = super.getChips(); }
    return action;
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
      String suit = d.getValue().substring(1);
      switch (suit) {
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
  private static final String[] names = new String[] { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie",
          "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe",
          "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan",
          "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Toby", "Scarlet", "Alex", "Leon", "Eric",
          "GuyWhoGoesAllInEveryTime", "Fei Yu-Ching", "Jay", "Daniel", "Evan", "Sean", "Selene", "James", "Jacques",
          "NoName", "Zoe", "Sarah", "Kyle", "Irene", "Sharolyn", "Ben", "Coco", "Cindy", "Megan", "Mia", "E10WINS",
          "Audrey", "Emily", "March 7th", "Stelle", "Cao Cao", "Liu", "Camellia", "Cameron", "Maddie", "Will", "Amy",
          "Kelly", "Aventurine" };

  public static String getUniqueName(PokerPlayer[] currentPlayers) {
    while (true) {
      String candidate = names[(int) (Math.random() * names.length)];
      boolean used = false;
      if (currentPlayers != null) {
        for (PokerPlayer p : currentPlayers) {
          if (p != null && candidate.equals(p.getName())) {
            used = true;
            break;
          }
        }
      }
      if (!used) return candidate;
    }
  }
}