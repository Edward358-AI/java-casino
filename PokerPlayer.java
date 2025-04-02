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
        System.out.println("Your hand: " + hand[0].getValue() + "  " + hand[1].getValue());
        System.out.println("Current amount to call is ✨" + bet + "." + " You have ✨" + super.getChips()
            + "." + ((status == 1) ? " You are in small blind position."
                : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("You have contributed ✨" + prevBet + " for this round. What is your action?\n");
        switch (status) {
          case 2:
            if (prevBet == bet)
              act = Player.getValidInt("[1] Check [2] Fold [3] Raise [4] All In", 1, 4);
            else
              act = Player.getValidInt("[1] Call [2] Fold [3] Raise [4] All In", 1, 4);
            break;
          default:
            act = Player.getValidInt("[1] Call [2] Fold [3] Raise [4] All In", 1, 4);
            break;
        }
        action[0] = act;
        switch (act) {
          case 1:
            if (status == 2 && bet - prevBet == 0)
              action[1] = 0;
            else
              action[1] = bet - prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips = getValidInt(
                "How much would you like to raise the current bet by? Min - " + bet + ", Max - "
                    + (super.getChips() - bet),
                bet,
                super.getChips() - bet);
            if (chips == super.getChips() - bet) {
              action[0] = 4;
              action[1] = super.getChips();
            } else
              action[1] = chips + bet - prevBet;
            break;
          case 4:
            action[1] = super.getChips();
        }
        break;
      default:
        int flop;
        System.out.println("Your hand is " + hand[0].getValue() + " " + hand[1].getValue());
        System.out.println("Current amount to call is ✨" + bet + "." + " You have ✨" + super.getChips()
            + "." + ((status == 1) ? " You are in small blind position."
                : ((status == 2) ? "You are in big blind position." : "")));
        System.out.print("You have contributed ✨" + prevBet + " for this round. What is your action?\n");
        if (bet == 0)
          flop = Player.getValidInt("[1] Check [2] Fold [3] Bet [4] All In", 1, 4);
        else
          flop = Player.getValidInt("[1] Call [2] Fold [3] Raise [4] All In", 1, 4);
        action[0] = flop;
        switch (flop) {
          case 1:
            action[1] = (bet == 0) ? 0 : bet - prevBet;
            break;
          case 2:
            super.setInHand(false);
            break;
          case 3:
            int chips;
            if (bet != 0) {
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + bet + ", Max - "
                      + (super.getChips() - bet),
                  bet,
                  super.getChips() - bet);
              if (chips == super.getChips() - bet)
                action[0] = 4;
            } else {
              chips = getValidInt(
                  "How much would you like to raise the current bet by? Min - " + blind + ", Max - "
                      + super.getChips(),
                  blind,
                  super.getChips());
              if (chips == super.getChips())
                action[0] = 4;
            }
            action[1] = chips + bet - prevBet;
            break;
          case 4:
            action[1] = super.getChips();
        }
        break;
    }
    return action;
  }
}
