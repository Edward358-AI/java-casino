import java.util.*;

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

  public void checkFolds() {
    for (ArrayList<PokerPlayer> e : eligible) {
      for (int k = 0; k < e.size(); k++)
        if (!e.get(k).inHand())
          e.remove(k);
    }
  }

  // testing purposes
  public void setCont(int[] cont) {
    contributions = cont;
  }

  public void resetPot(PokerPlayer[] players) {
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

  private void updatePot() {
    pots.clear();
    eligible.clear();
    pots.add(0);
    eligible.add(new ArrayList<>());
    for (int i = 0; i < p.length; i++) if (contributions[i] > 0) eligible.get(0).add(p[i]);
    checkFolds();
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
    for (int c = thresholds.size() - 1; c > 0; c--) thresholds.set(c, thresholds.get(c) - thresholds.get(c-1));
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
              if (!eligible.get(i).contains(p[k]))
              eligible.get(i).add(p[k]);
            } else if (cont[k] > 0) {
              pots.set(i, pots.get(i) + cont[k]);
              if (!eligible.get(i).contains(p[k]))
              eligible.get(i).add(p[k]);
            }
            
          } catch (IndexOutOfBoundsException e) {
            if (cont[k] >= thresholds.get(i)) {
              pots.add(thresholds.get(i));
              eligible.add(new ArrayList<>());
              eligible.get(i).add(p[k]);
            } else if (cont[k] > 0) {
              pots.add(cont[k]);
              eligible.add(new ArrayList<>());
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
            eligible.get(eligible.size() - 1).add(p[i]);
          } catch (IndexOutOfBoundsException e) {
            pots.set(pots.size() - 1, pots.get(pots.size() - 1) + cont[i]);
            eligible.get(eligible.size() - 1).add(p[i]);
          }
        }
      }
    }
    checkFolds();
  }

  public String toString() {
    updatePot();
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

  public void assignWinner(PokerDeck d, int complete) {
    System.out.println("*** SHOWDOWN ***\n");
    updatePot();
    if (complete == 1) {
      System.out.println("Board: " + d.getBoard()[0].getValue() + " - " +
          d.getBoard()[1].getValue() + " - "
          + d.getBoard()[2].getValue() + " - " + d.getBoard()[3].getValue() + " - " +
          d.getBoard()[4].getValue());
      ArrayList<Integer> currBest = new ArrayList<Integer>();
      Card[] bestHand = new Card[5];
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      for (int k = 0; k < pots.size(); k++) {
        for (int i = 0; i < eligible.get(k).size(); i++) {
          if (eligible.get(k).get(i).inHand()) {
            Card[] currHand = d.getBestHand(eligible.get(k).get(i).getHand());
            if (eligible.get(k).get(i) instanceof PokerBot)
              System.out.println(eligible.get(k).get(i).getName() + "'s hand: " +
                  eligible.get(k).get(i).getHand()[0].getValue() + " "
                  + eligible.get(k).get(i).getHand()[1].getValue());
            else
              System.out.println("YOUR hand: " + eligible.get(k).get(i).getHand()[0].getValue()
                  + " "
                  + eligible.get(k).get(i).getHand()[1].getValue());
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (currBest.size() > 0) {
              if (d.compareHands(bestHand, currHand) == 2) {
                currBest.clear();
                currBest.add(i);
                bestHand = currHand;
              } else if (d.compareHands(bestHand, currHand) == 0)
                currBest.add(i);
            } else {
              currBest.add(i);
              bestHand = currHand;
            }
          }
        }
        Deck.sort(bestHand);
        System.out.println();
        for (int i = 0; i < currBest.size(); i++) {
          eligible.get(k).get(currBest.get(i)).addChips((int) (pots.get(k) / currBest.size()));
          System.out
              .print(eligible.get(k).get(currBest.get(i)).getName() + " won ✨" + (int) (pots.get(k) / currBest.size())
                  + " from the " + ((k == 0) ? "main" : "side") + " pot! Their hand was ");
          Card[] theHand = d.getBestHand(eligible.get(k).get(currBest.get(i)).getHand());
          Deck.sort(theHand);
          for (int f = 0; f < 5; f++) {
            System.out.print(theHand[f].getValue() + ((f == 4) ? "\n" : "  - "));
          }
        }
      }

    } else {
      for (int k = 0; k < pots.size(); k++) {
        for (int i = 0; i < eligible.get(k).size(); i++)
          if (eligible.get(k).get(i).inHand()) {
            eligible.get(k).get(i).addChips(eligible.get(k).get(i).getChips());
            System.out.println(eligible.get(k).get(i).getName() + " won ✨" + eligible.get(k).get(i).getChips()
                + " this hand! Everyone else folded.");
          }
      }
    }
  }
}