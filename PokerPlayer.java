import java.util.*;

public class PokerPlayer extends Player {
  Card[] hand;
  int status; // -1 regular, 0 dealer, 1 sb, 2 bb
  Scanner s = new Scanner(System.in);

  public PokerPlayer(String name) {
    super(name);
    hand = new Card[2];
    status = -1;
  }

  public Card[] getHand() {
    return hand;
  }

  public int status() {
    return status;
  }

  public void setHand(Card[] hand) {
    this.hand = hand;
  }

  public void setStatus(int i) {
    status = i;
  }

  public int[] action(String round, int bet, int blind) {
    int[] action = new int[2];
    switch (round) {
      case "preflop":
        int act;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("The current bet is $" + bet + ".");
        System.out.print("What is your action? ");
        switch (status) {
          case 2:
            act = Player.getValidInt("[1] Check [2] Fold [3] Raise", 1, 3);
            break;
          default:
            act = Player.getValidInt("[1] Call] [2] Fold [3] Raise", 1, 3);
            break;
        }
        action[0] = act;
        switch (act) {
          case 1:
            if (status != 2) action[1] = bet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3: 
            int chips = getValidInt("How much would you like to raise by? Min - " + blind + ", Max - " + super.getChips(), blind, super.getChips());
            action[1] = chips;
            break;
        }
        break;
      case "flop":
        break;
      case "turn":
        break;
      case "river":
        break;
    }
    return action;
  }
}
