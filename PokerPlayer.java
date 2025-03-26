import java.util.*;

public class PokerPlayer extends Player {
  Card[] hand;
  int status; // 0 regular, 1 sb, 2 bb
  Scanner s = new Scanner(System.in);

  public PokerPlayer(String name) {
    super(name);
    hand = new Card[2];
    status = 0;
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

  public int[] action(String round, int prevBet, int bet, int blind) {
    int[] action = new int[2];
    switch (round) {
      case "preflop":
        int act;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("Current amount to call is $" + bet + "." + ((status == 1) ? " You are in small blind position."
            : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("Currently you have put in $" + prevBet + ". What is your action?\n");
        switch (status) {
          case 2:
            if (prevBet == bet) act = Player.getValidInt("[1] Check [2] Fold [3] Raise", 1, 3);
            else act = Player.getValidInt("[1] Call [2] Fold [3] Raise", 1, 3);
            break;
          default:
            act = Player.getValidInt("[1] Call [2] Fold [3] Raise", 1, 3);
            break;
        }
        action[0] = act;
        switch (act) {
          case 1:
            if (status == 2 && bet-prevBet == 0) action[1] = 0;
            else action[1] = bet-prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips = getValidInt(
                "How much would you like to raise the current bet by? Min - " + bet + ", Max - " + super.getChips(),
                bet,
                super.getChips());
            action[1] = chips + bet-prevBet;
            break;
        }
        break;
      case "flop":
        int flop;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("Current amount to call is $" + bet + "." + ((status == 1) ? " You are in small blind position."
            : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("Currently you have put in $" + prevBet + ". What is your action?\n");
        if (bet == 0)
          flop = Player.getValidInt("[1] Check [2] Fold [3] Bet", 1, 3);
        else
          flop = Player.getValidInt("[1] Call] [2] Fold [3] Raise", 1, 3);
        action[0] = flop;
        switch (flop) {
          case 1:
            action[1] = bet-prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips;
            if (bet != 0)
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + bet + ", Max - " + super.getChips(),
                  bet,
                  super.getChips());
            else
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + blind + ", Max - " + super.getChips(),
                  blind,
                  super.getChips());
            action[1] = chips + bet-prevBet;
            break;
        }
        break;
      case "turn":
        int turn;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("Current amount to call is $" + bet + "." + ((status == 1) ? " You are in small blind position."
            : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("Currently you have put in $" + prevBet + ". What is your action?\n");
        if (bet == 0)
          turn = Player.getValidInt("[1] Check [2] Fold [3] Bet", 1, 3);
        else
          turn = Player.getValidInt("[1] Call] [2] Fold [3] Raise", 1, 3);
        action[0] = turn;
        switch (turn) {
          case 1:
            action[1] = bet-prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips;
            if (bet != 0)
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + bet + ", Max - " + super.getChips(),
                  bet,
                  super.getChips());
            else
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + blind + ", Max - " + super.getChips(),
                  blind,
                  super.getChips());
            action[1] = chips + bet-prevBet;
            break;
        }
        break;
      case "river":
        int river;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("Current amount to call is $" + bet + "." + ((status == 1) ? " You are in small blind position."
            : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("Currently you have put in $" + prevBet + ". What is your action?\n");
        System.out.print("What is your action? ");
        if (bet == 0)
          river = Player.getValidInt("[1] Check [2] Fold [3] Bet", 1, 3);
        else
          river = Player.getValidInt("[1] Call] [2] Fold [3] Raise", 1, 3);
        action[0] = river;
        switch (river) {
          case 1:
            action[1] = bet-prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips;
            if (bet != 0)
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + bet + ", Max - " + super.getChips(),
                  bet,
                  super.getChips());
            else
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + blind + ", Max - " + super.getChips(),
                  blind,
                  super.getChips());
            action[1] = chips + bet-prevBet;
            break;
        }
        break;
    }
    System.out.println();
    return action;
  }
}
