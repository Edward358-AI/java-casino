import java.util.*;

public class PokerPot {
  private int chips;
  private ArrayList<PokerPlayer> p;
  private String name;
  private Map<PokerPlayer, Integer> contributions;

  public PokerPot(String name) {
    this.name = name;
    chips = 0;
    this.p = new ArrayList<>();
    contributions = new HashMap<>();
  }

  public String name() {
    return name;
  }

  public void addPlayerContribution(PokerPlayer player, int amount) {
    contributions.put(player, contributions.getOrDefault(player, 0) + amount);
    chips += amount;
    if (!p.contains(player)) {
      p.add(player);
    }
  }

  public int maxContribution() {
    int min = Integer.MAX_VALUE;
    for (Map.Entry<PokerPlayer, Integer> cont : contributions.entrySet()) {
      if (cont.getKey().inHand() && (cont.getValue() + cont.getKey().getChips() < min)) {
        min = cont.getValue() + cont.getKey().getChips();
      }
    }
    return min;
  }

  public Map<PokerPlayer, Integer> getContributions() {
    return new HashMap<PokerPlayer, Integer>(contributions);
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getChips() {
    return chips;
  }

  public String toString() {
    String n = name + " pot: $" + chips + ". Eligible to win: ";
    for (int i = 0; i < p.size(); i++)
      if (p.get(i).inHand())
        n += p.get(i).getName() + ", ";
    return n.substring(0, n.length() - 2);
  }

  public void assignWinner(PokerDeck d, int complete) {
    
    if (complete == 1) {
      System.out.println("Board: " + d.getBoard()[0].getValue() + " - " + d.getBoard()[1].getValue() + " - " + d.getBoard()[2].getValue() + " - " + d.getBoard()[3].getValue() + " - " + d.getBoard()[4].getValue());
      ArrayList<Integer> currBest = new ArrayList<Integer>();
      Card[] bestHand = new Card[5];
      for (int i = 0; i < p.size(); i++) {
        if (p.get(i).inHand()) {
          Card[] currHand = d.getBestHand(p.get(i).getHand());
          if (p.get(i) instanceof PokerBot) System.out.println(p.get(i).getName() + "'s hand: " + p.get(i).getHand()[0].getValue() + " " + p.get(i).getHand()[1].getValue());
          else System.out.println("YOUR hand: " + p.get(i).getHand()[0].getValue() + " " + p.get(i).getHand()[1].getValue());
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
      for (int i = 0; i < currBest.size(); i++) {
        p.get(currBest.get(i)).addChips((int) (getChips() / currBest.size()));
        System.out.print(p.get(currBest.get(i)).getName() + " won $" + (int) (getChips() / currBest.size())
            + " from the " + name + " pot! Their hand was ");
        for (int f = 0; f < 5; f++) {
          System.out.print(bestHand[f].getValue() + ((f == 4) ? "\n" : " - "));
        }
      }
    } else {
      for (int i = 0; i < p.size(); i++) {
        if (p.get(i).inHand()) {
          p.get(i).addChips(getChips());
          System.out.println(p.get(i).getName() + " won $" + getChips()
              + " this hand! Everyone else folded.");
        }
      }
    }
    chips = 0;
    this.p = new ArrayList<>();
    contributions = new HashMap<>();
  }
}
