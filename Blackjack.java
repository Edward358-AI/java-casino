import java.util.*;

public class Blackjack {
  Deck deck; // deal, reset

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
