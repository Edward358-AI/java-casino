import java.util.*;

public class Blackjack {
  private Deck deck; // deal, reset
  private ArrayList<BJPlayer> players = new ArrayList<BJPlayer>();
  private static Scanner sc = new Scanner(System.in);

  public Blackjack(BJPlayer p) {
    deck = new Deck();
    players.add(p);
    players.add(new BJBot());
  }

  private void main() {
    // run action() function on each player before dealing cards to collect $$$
    // action to print info // or game print board-equivalent
    players.get(0).action(0, 0); // bet
    for(BJPlayer player:players) { // give all players two cards
      player.add(deck.deal()[0]);
      player.add(deck.deal()[0]);
    }
    
  }

  public void initialize() {
    main();
  }

  private int getNumber(Card c) {
    int num = -1;
    String temp = c.getValue().substring(0,1);
      switch(temp) { // this gets the numerical value of the card
        case "T":
          num = 10;
          break;
        case "J": 
          num = 10;
          break;
        case "Q":
          num = 10;
          break;
        case "K":
          num = 10;
          break;
        case "A":
          num = 11; // over 21 becomes 1
          break;
        default:
          num = Integer.parseInt(temp);
      }
      return num;
  }

  public int getSum(Card[] c) {
    int runningSum = 0;
    int numAces = 0;
    for(Card ca:c) {
      if(getNumber(ca)<11) {
        runningSum+=getNumber(ca);
      } else {
        numAces++;
      }
    }
    for(int i = 0;i<numAces;i++) {  
      if(runningSum+11>21) {
        runningSum++;
      } else {
        runningSum+=11;
      }
    }
    return runningSum;
  }

  public static int getValidInt(String message, int min) { // override (maybe overload); dont need bounds sometimes
    int x;
    while (true) {
      System.out.println(message);
      try {
        String z = sc.nextLine().trim();
        x = Integer.parseInt(z);
        if (x>=min) break;
      } catch (Exception e) {
        continue;
      }
    }
    return x;
  }
}
