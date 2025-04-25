import java.util.*;

public class Blackjack {
  private Deck deck; // deal, reset
  private ArrayList<BJPlayer> players = new ArrayList<BJPlayer>();
  private int prevBet;
  private Scanner sc = new Scanner(System.in);

  public Blackjack(BJPlayer p) {
    deck = new Deck();
    players.add(p);
    players.add(new BJBot());
    prevBet = 0;
  }

  private void main() {
    int[] action = new int[2];
    Utils.clearScreen();
    System.out.println("Welcome to the Blackjack table!");
    System.out.println("You will be playing against the dealer, " + players.get(1).getName() + ".");
    // maybe use fancy clear screen util
    // run action() function on each player before dealing cards to collect $$$
    // action to print info // or game print board-equivalent
    action = players.get(0).action(prevBet); // get bet
    prevBet = action[1];
    for (BJPlayer player : players) { // give all players two cards
      player.add(deck.deal()[0]);
      player.add(deck.deal()[0]);
    }
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Utils.clearScreen();
    for (int i = players.size() - 1; i >= 0; i--) {
      players.get(i).dispHand();
    }

    int dealerTemp = getSum(players.get(1).getHand().toArray(new Card[2]));
    int playerTemp = getSum(players.get(0).getHand().toArray(new Card[2]));
    if (dealerTemp == 21 || playerTemp == 21) {
      if (dealerTemp == 21 && playerTemp == 21) {
        System.out.println("Both you and the dealer got a blackjack; tie");
        System.out.println("You are returned your original bet.");
      } else {
        if (dealerTemp == 21) {
          System.out.println(players.get(1).getName() + " hit a blackjack!");
          System.out.println("You immediately lost " + prevBet + "✨!");
          players.get(0).removeChips(prevBet);
        } else {
          System.out.println(players.get(0).getName() + " hit a blackjack!");
          System.out.println("You won " + (int) (prevBet * 1.5) + "✨!");
          players.get(0).addChips((int) (prevBet * 1.5));
        }
      }
    } else {
      boolean surrendered = false;
      boolean busted = false;
      do {
        action = players.get(0).action(prevBet);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (action[0] == 1) {
          players.get(0).add(deck.deal()[0]);
          Utils.clearScreen();
          for (int j = players.size() - 1; j >= 0; j--) {
            players.get(j).dispHand();
          }
        }
        if (action[0] == 3) {
          System.out.println("You surrendered and got back " + prevBet / 2 + "✨!");
          players.get(0).removeChips(prevBet / 2);
          surrendered = true;
          break;
        }
        if (getSum(players.get(0).getHand().toArray(new Card[players.get(0).getHand().size()])) > 21) {
          System.out.println("You busted! You lost " + prevBet + "✨!");
          players.get(0).removeChips(prevBet);
          busted = true;
          break;
        }

      } while (action[0] == 1);
      if (!surrendered && !busted) {
        do {
          Card[] arr = players.get(1).getHand().toArray(new Card[players.get(1).getHand().size()]);
          int gS = getSum(arr);
          action = players.get(1).action(gS);
          if (action[0] == 1) {
            players.get(1).add(deck.deal()[0]);
          }
        } while (action[0] == 1);
        Utils.clearScreen();
        System.out.print("SHOWDOWN:\n");
        BJBot b = (BJBot) players.get(1);
        b.dispHand(true);
        players.get(0).dispHand();
        int dealer = getSum(players.get(1).getHand().toArray(new Card[players.get(1).getHand().size()]));
        int player = getSum(players.get(0).getHand().toArray(new Card[players.get(0).getHand().size()]));
        if (player > dealer || dealer > 21) {
          players.get(0).addChips(prevBet);
          System.out.println("You beat the dealer! You won " + prevBet + "✨");
        } else {
          players.get(0).removeChips(prevBet);
          System.out.println(
              "Dealer " + ((player == dealer) ? "tied, house rules, y" : "won! Y") + "ou lost " + prevBet + "✨");
        }
      }
    }
    System.out.println("Continue playing? [y/n]:");
    String s = sc.nextLine().strip().toLowerCase();
    if (s.equals("n") || s.equals("no") || s.equals("nah")) {
    } else {
      deck.reset();
      action = new int[2];
      players.get(0).clear();
      players.get(1).clear();
      if (players.get(0).getChips() > 0)
        main();
      else {
        System.out.println("Game over! You ran out of primogems.");
        System.out.println("Press Enter to continue:");
        sc.nextLine();
      }
    }
  }

  public void initialize() {
    main();
  }

  private int getNumber(Card c) {
    int num = -1;
    String temp = c.getValue().substring(0, 1);
    switch (temp) { // this gets the numerical value of the card
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
    for (Card ca : c) {
      if (getNumber(ca) < 11) {
        runningSum += getNumber(ca);
      } else {
        numAces++;
      }
    }
    for (int i = 0; i < numAces; i++) {
      if (runningSum + 11 > 21) {
        runningSum++;
      } else {
        runningSum += 11;
      }
    }
    return runningSum;
  }

}
