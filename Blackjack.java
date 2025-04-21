import java.util.*;

public class Blackjack {
  private Deck deck; // deal, reset
  private ArrayList<BJPlayer> players = new ArrayList<BJPlayer>();
  private int prevBet;

  public Blackjack(BJPlayer p) {
    deck = new Deck();
    players.add(p);
    players.add(new BJBot());
    prevBet = 0;
  }

  private void main() {
    int[] action = new int[2];
    System.out.println();
    System.out.println("Welcome to the Blackjack table!");
    System.out.println("You will be playing against the dealer.");
    // maybe use fancy clear screen util
    // run action() function on each player before dealing cards to collect $$$
    // action to print info // or game print board-equivalent
    players.get(0).action(prevBet); // bet // why prevBet here
    for(BJPlayer player:players) { // give all players two cards
      player.add(deck.deal()[0]);
      player.add(deck.deal()[0]);
    }
    System.out.println(); // start to display cards each person has
    for(BJPlayer player:players) {
      player.dispHand(false);
    }
    Card[] arr = players.get(0).getHand().toArray(new Card[players.get(0).getHand().size()]);
    if(getSum(arr)==21) {
      System.out.println("You hit a blackjack!");
    }
    for(BJPlayer player:players) { // get action for each player
      action = player.action(prevBet);
    }
    //if()
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

}
