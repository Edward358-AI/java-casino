import java.util.*;

public class PokerPlayer extends Player {
  Card[] hand;
  int status; // 0 regular, 1 sb, 2 bb
  Scanner s = Player.sc;

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

  public int[] action(String round, int prevBet, int bet, int blind, int lastRaise) {
    int[] action = new int[2];
    switch (round) {
      case "preflop":
        int act;
        while (true) {
          System.out.println(
              "Your hand: " + hand[0].getValue() + "  " + hand[1].getValue() + "  |  Your stack: ✨" + super.getChips());
          System.out.println("To call: ✨" + bet + "  |  Your contribution: ✨" + prevBet
              + ((status == 1) ? " (SB)" : ((status == 2) ? " (BB)" : "")));
          if (bet < super.getChips()) {
            boolean canRaise = (super.getChips() >= bet + lastRaise);
            String raiseLabel = canRaise ? "Raise" : "All In";
            switch (status) {
              case 2:
                if (prevBet == bet)
                  act = Player.getValidInt("[1] Check [2] Fold [3] " + raiseLabel + " [4] All In", 1, 4);
                else
                  act = Player.getValidInt("[1] Call (" + (bet - prevBet) + ") [2] Fold [3] " + raiseLabel + " [4] All In", 1, 4);
                break;
              default:
                act = Player.getValidInt("[1] Call (" + (bet - prevBet) + ") [2] Fold [3] " + raiseLabel + " [4] All In", 1, 4);
                break;
            }
          } else {
            // Stack <= Bet: Only Fold or All-In (Call) remains
            act = Player.getValidInt("[1] Fold [2] All In (" + super.getChips() + ")", 1, 2);
            act = (act == 1) ? 2 : 4;
          }
          action[0] = act;
          boolean back = false;
          switch (act) {
            case 1:
              if (status == 2 && bet - prevBet == 0)
                action[1] = 0;
              else
                action[1] = bet - prevBet;
              break;
            case 2:
              break;
            case 3:
              int chips;
              if (bet + lastRaise < super.getChips()) {
                chips = Player.getValidInt(
                    "What would you like to raise the current bet to? Min - " + (bet + lastRaise) + ", Max - "
                        + super.getChips(),
                    bet + lastRaise,
                    super.getChips(), true);
              } else {
                chips = Player.getValidInt(
                    "What would you like to raise the current bet to? Min - " + super.getChips() + ", Max - "
                        + super.getChips(),
                    super.getChips(),
                    super.getChips(), true);
              }
              if (chips == -1) {
                back = true;
                break;
              }
              if (chips == super.getChips()) {
                action[0] = 4;
                action[1] = super.getChips();
              } else
                action[1] = chips - prevBet;
              break;
            case 4:
              action[1] = super.getChips();
          }
          if (back)
            continue;
          break;
        }
        break;
      default:
        int flop;
        while (true) {
          System.out.println(
              "Your hand: " + hand[0].getValue() + "  " + hand[1].getValue() + " | Your stack: ✨" + super.getChips());
          System.out.println("To call: ✨" + bet + " | Your contribution: ✨" + prevBet
              + ((status == 1) ? " (SB)" : ((status == 2) ? " (BB)" : "")));
          if (bet < super.getChips()) {
            boolean canRaise = (bet == 0) ? (super.getChips() >= lastRaise) : (super.getChips() >= bet + lastRaise);
            String raiseLabel = canRaise ? "Raise" : "All In";
            if (bet == 0)
              flop = Player.getValidInt("[1] Check [2] Fold [3] " + raiseLabel + " [4] All In", 1, 4);
            else
              flop = Player.getValidInt("[1] Call (" + (bet - prevBet) + ") [2] Fold [3] " + raiseLabel + " [4] All In", 1, 4);
          } else {
            // Stack <= Bet: Only Fold or All-In (Call) remains
            flop = Player.getValidInt("[1] Fold [2] All In (" + super.getChips() + ")", 1, 2);
            flop = (flop == 1) ? 2 : 4;
          }
          action[0] = flop;
          boolean back = false;
          switch (flop) {
            case 1:
              action[1] = (bet == 0) ? 0 : bet - prevBet;
              break;
            case 2:
              break;
            case 3:
              int chips;
              if (bet != 0) {
                if (bet + lastRaise < super.getChips()) {
                  chips = Player.getValidInt(
                      "What would you like to raise the current bet to? Min - " + (bet + lastRaise) + ", Max - "
                          + super.getChips(),
                      bet + lastRaise,
                      super.getChips(), true);
                } else {
                  chips = Player.getValidInt(
                      "What would you like to raise the current bet to? Min - " + super.getChips() + ", Max - "
                          + super.getChips(),
                      super.getChips(),
                      super.getChips(), true);
                }
              } else {
                chips = Player.getValidInt(
                    "What would you like to raise the current bet to? Min - " + lastRaise + ", Max - "
                        + super.getChips(),
                    lastRaise,
                    super.getChips(), true);

              }
              if (chips == -1) {
                back = true;
                break;
              }
              if (chips == super.getChips()) {
                action[0] = 4;
                action[1] = super.getChips();
              } else
                action[1] = chips - prevBet;
              break;
            case 4:
              action[1] = super.getChips();
          }
          if (back)
            continue;
          break;
        }
        break;
    }
    return action;
  }
}
