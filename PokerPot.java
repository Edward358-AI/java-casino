import java.util.*;

/*
 * How the pot works:
 * keeps track of pot for a hand. the current player setup is stored in p.
 * each players TOTAL contributions are tracked through contributions array, with indices corresponding to PokerPlayer array p.
 * pots keeps track of each pot and the number of chips it holds. this includes main pot and all side pots.
 * eligible keeps track of number of players currently eligible to win the pot at the corresponding index in pots.
 * maxBet is the maximum contribution that a player has made for the hand.
 */

public class PokerPot {
  private PokerPlayer[] p;
  private int[] contributions;
  private ArrayList<Integer> pots;
  private ArrayList<ArrayList<PokerPlayer>> eligible;
  private int maxBet;

  public PokerPot(PokerPlayer[] players) {
    this.p = players;
    contributions = new int[players.length];
    pots = new ArrayList<>();
    pots.add(0);
    eligible = new ArrayList<>();
    eligible.add(new ArrayList<>());
    maxBet = 0;
  }

  // testing purposes
  public void setCont(int[] cont) {
    contributions = cont;
  }

  public void resetPot(PokerPlayer[] players) { // resets pot for a new hand
    p = players;
    contributions = new int[p.length];
    pots.clear();
    eligible.clear();
    pots.add(0);
    eligible.add(new ArrayList<>());
    maxBet = 0;
  }

  public void addPlayerContribution(int i, int amount) {
    contributions[i] += p[i].removeChips(amount);
  }

  public int[] getContributions() {
    return contributions;
  }

  public PokerPlayer[] getPlayers() {
    return p;
  }

  /*
   * this function below seems very complex, but heres a breakdown of what it
   * does:
   * it essentially uses the players and their total contributions so far for the
   * hand to construct the pots from scratch.
   * reason being this is in my opinion simpler to code than updating the pots
   * because say if a player with a lower contribution than the current lowest for
   * the main pot, then the pots need to be completely reworked for this new
   * player contribution.
   * if you only update the pots as you go, you'll have to backtrack and redo all
   * the pots anyways as a result of the above.
   * you could probably make this more efficient by only doing the pot
   * reconstruction if the amount bet is lower than the minimum contribution but
   * eh laziness exists and it required a lot of effort to make the below code
   * work in the first place lol
   */
  private void updatePot() {
    pots.clear();
    eligible.clear();
    pots.add(0);
    eligible.add(new ArrayList<>());
    for (int i = 0; i < p.length; i++)
      if (contributions[i] > 0 && p[i].inHand())
        eligible.get(0).add(p[i]);
    int[] cont = contributions.clone();
    for (int c : contributions)
      if (c > maxBet)
        maxBet = c;
    ArrayList<Integer> thresholds = new ArrayList<>();
    for (int c = 0; c < cont.length; c++) {
      if (p[c].getChips() == 0 && !thresholds.contains(cont[c])) {
        thresholds.add(cont[c]);
      }
    }
    Collections.sort(thresholds);
    for (int c = thresholds.size() - 1; c > 0; c--)
      thresholds.set(c, thresholds.get(c) - thresholds.get(c - 1));
    // System.out.println(thresholds + "\n");
    if (thresholds.size() == 0) {
      for (int i = 0; i < cont.length; i++) {
        pots.set(0, pots.get(0) + cont[i]);
      }
    } else {
      for (int i = 0; i < thresholds.size(); i++) {
        // System.out.println(thresholds.get(i));
        for (int k = 0; k < cont.length; k++) {
          try {
            if (cont[k] >= thresholds.get(i)) {
              pots.set(i, pots.get(i) + thresholds.get(i));
              if (!eligible.get(i).contains(p[k]) && p[k].inHand())
                eligible.get(i).add(p[k]);
            } else if (cont[k] > 0) {
              pots.set(i, pots.get(i) + cont[k]);
              if (!eligible.get(i).contains(p[k]) && p[k].inHand())
                eligible.get(i).add(p[k]);
            }

          } catch (IndexOutOfBoundsException e) {
            if (cont[k] >= thresholds.get(i)) {
              pots.add(thresholds.get(i));
              eligible.add(new ArrayList<>());
              if (p[k].inHand())
                eligible.get(i).add(p[k]);
            } else if (cont[k] > 0) {
              pots.add(cont[k]);
              eligible.add(new ArrayList<>());
              if (p[k].inHand())
                eligible.get(i).add(p[k]);
            }
          }
          if (cont[k] >= thresholds.get(i))
            cont[k] -= thresholds.get(i);
          else if (cont[k] > 0)
            cont[k] = 0;
        }
        // System.out.println(Arrays.toString(cont));
      }
      int orgLen = pots.size();
      for (int i = 0; i < cont.length; i++) {
        if (cont[i] > 0) {
          if (pots.size() == orgLen) {
            pots.add(0);
            eligible.add(new ArrayList<>());
          }
          try {
            pots.set(pots.size() - 1, pots.get(pots.size() - 1) + cont[i]);
            if (p[i].inHand())
              eligible.get(eligible.size() - 1).add(p[i]);
          } catch (IndexOutOfBoundsException e) {
            pots.set(pots.size() - 1, pots.get(pots.size() - 1) + cont[i]);
            if (p[i].inHand())
              eligible.get(eligible.size() - 1).add(p[i]);
          }
        }
      }
    }
  }

  public String toString() { // returns a string of the pot(s) formatted in a nice, friendly and readable way
    updatePot(); // runs a pot update before it prints—don't want to print the wrong stuff!
    String result = "";
    result += "Main pot: ✨" + pots.get(0) + " | Eligible: ";
    for (PokerPlayer p : eligible.get(0))
      result += p.getName() + ", ";
    result = result.substring(0, result.length() - 2);
    for (int i = 1; i < pots.size(); i++) {
      String temp = "\n";
      temp += "Side pot: ✨" + pots.get(i) + " | Eligible: ";
      for (PokerPlayer p : eligible.get(i))
        temp += p.getName() + ", ";
      temp = temp.substring(0, temp.length() - 2);
      result += temp;
    }
    return result;
  }

  public int[] assignWinner(PokerDeck d, int complete) { // assigns winners across all the pots.
    int[] stats = new int[2]; // 0 is loss, 1 is win for the player
    System.out.println("*** SHOWDOWN ***");
    updatePot();
    if (complete == 1) { // only runs this if the game has been through a full round
      System.out.println("\nBoard: " + d.getBoard()[0].getValue() + " - " +
          d.getBoard()[1].getValue() + " - "
          + d.getBoard()[2].getValue() + " - " + d.getBoard()[3].getValue() + " - " +
          d.getBoard()[4].getValue());
      System.out.println(toString() + "\n");
      Utils.sleep(1000);
      for (int k = 0; k < pots.size(); k++) {
        ArrayList<Integer> currBest = new ArrayList<Integer>();
        Card[] bestHand = new Card[5];
        for (int i = 0; i < eligible.get(k).size(); i++) {
          if (eligible.get(k).get(i).inHand()) {
            Card[] currHand = d.getBestHand(eligible.get(k).get(i).getHand());
            if (k == 0) {
              if (eligible.get(k).get(i) instanceof PokerBot)
                System.out.println(eligible.get(k).get(i).getName() + "'s hand: " +
                    eligible.get(k).get(i).getHand()[0].getValue() + " "
                    + eligible.get(k).get(i).getHand()[1].getValue());
              else
                System.out.println("YOUR hand: " + eligible.get(k).get(i).getHand()[0].getValue()
                    + " "
                    + eligible.get(k).get(i).getHand()[1].getValue());
              Utils.sleep(1000);
            }

            if (currBest.size() > 0) {
              if (d.compareHands(bestHand, currHand) == 2) {
                currBest.clear();
                currBest.add(i);
                bestHand = currHand;
              } else if (d.compareHands(bestHand, currHand) == 0 && !currBest.contains(i))
                currBest.add(i);
            } else {
              currBest.add(i);
              bestHand = currHand;
            }
          }
        }
        Deck.sort(bestHand);
        System.out.println();
        if (currBest.size() > 1) {
          for (int i = 0; i < currBest.size(); i++) {
            eligible.get(k).get(currBest.get(i)).addChips((int) (pots.get(k) / currBest.size()));
            System.out
                .print(eligible.get(k).get(currBest.get(i)).getName() + " won ✨" + (int) (pots.get(k) / currBest.size())
                    + " from the " + ((k == 0) ? "main" : "side") + " pot! Their hand was ");
            Card[] theHand = d.getBestHand(eligible.get(k).get(currBest.get(i)).getHand());
            Deck.sort(theHand);
            for (int f = 0; f < 5; f++) {
              System.out.print(theHand[f].getValue() + ((f == 4) ? "\n\n" : "  - "));
            }
            if (!(eligible.get(k).get(currBest.get(i)) instanceof PokerBot)) {
              stats[0] = 1;
              stats[1] += pots.get(k) / currBest.size();
            }
          }
        } else {
          eligible.get(k).get(currBest.get(0)).addChips(pots.get(k));
          Utils.sleep(1000);
          System.out.print(eligible.get(k).get(currBest.get(0)).getName() + " won ✨" + pots.get(k) + " from the "
              + ((k == 0) ? "main" : "side") + " pot! Their hand was a ");
          Card[] theHand = d.getBestHand(eligible.get(k).get(currBest.get(0)).getHand());
          Deck.sort(theHand);
          switch (d.getRanking(theHand)) {
            case 1:
              System.out.print("STRAIGHT FLUSH: ");
              break;
            case 2:
              System.out.print("FOUR of a KIND: ");
              break;
            case 3:
              System.out.print("FULL HOUSE: ");
              break;
            case 4:
              System.out.print("FLUSH");
              break;
            case 5:
              System.out.print("STRAIGHT");
              break;
            case 6:
              System.out.print("THREE of a KIND");
              break;
            case 7:
              System.out.print("TWO PAIR");
              break;
            case 8:
              System.out.print("ONE PAIR");
              break;
            case 9:
              System.out.print("HIGH Card");
              break;
          }
          System.out.print(": ");
          for (int f = 0; f < 5; f++)
            System.out.print(theHand[f].getValue() + ((f == 4) ? "\n\n" : "  - "));
          if (!(eligible.get(k).get(currBest.get(0)) instanceof PokerBot)) {
            stats[0] = 1;
            stats[1] += pots.get(k);
          }
        }
      }
    } else { // executes if the hand was abruptly stopped, i.e. everyone folded except one
             // person
      for (int k = 0; k < pots.size(); k++) {
        for (int i = 0; i < eligible.get(k).size(); i++)
          if (eligible.get(k).get(i).inHand()) {
            Utils.sleep(1000);
            eligible.get(k).get(i).addChips(pots.get(k));
            System.out.println(eligible.get(k).get(i).getName() + " won ✨" + pots.get(k)
                + " this hand! Everyone else folded.");
            if (!(eligible.get(k).get(i) instanceof PokerBot)) {
              stats[0] = 1;
              stats[1] += pots.get(k);
            }
          }
      }
    }
    return stats;
  }
}