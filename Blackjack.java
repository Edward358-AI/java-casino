import java.util.*;

public class Blackjack {
  private Deck deck; // deal, reset
  private ArrayList<BJPlayer> players = new ArrayList<BJPlayer>();
  private int prevBet;
  private Scanner sc = new Scanner(System.in);
  private PlayerStat mp; // player stats for the game

  public Blackjack(BJPlayer p) {
    deck = new Deck();
    players.add(p);
    players.add(new BJBot());
    prevBet = 0;
    mp = new PlayerStat(0);
  }

  public PlayerStat getStat() {
    return mp;
  }

  private void main() {
    int[] action = new int[2];
    Utils.clearScreen();
    System.out.println("Welcome to the Blackjack table!");
    System.out.println("You will be playing against the dealer, " + players.get(1).getName() + ".");
    // run action() function on each player before dealing cards to collect $$$
    // action to print info // or game print board-equivalent
    action = players.get(0).action(prevBet); // get bet
    prevBet = action[1];
    mp.addBet(prevBet);
    if (prevBet == players.get(0).getChips())
      mp.allIn();
    for (BJPlayer player : players) { // give all players two cards
      player.add(deck.deal()[0]);
      player.add(deck.deal()[0]);
    }
    Utils.sleep(1000);
    Utils.clearScreen();
    for (int i = players.size() - 1; i >= 0; i--) {
      players.get(i).dispHand(); // displays hand
    }

    int dealerTemp = getSum(players.get(1).getHand().toArray(new Card[2]));
    int playerTemp = getSum(players.get(0).getHand().toArray(new Card[2]));
    if (dealerTemp == 21 || playerTemp == 21) { // checks for blackjacks
      if (dealerTemp == 21 && playerTemp == 21) {
        System.out.println("Both you and the dealer got a blackjack; tie");
        System.out.println("You are returned your original bet.");
      } else {
        if (dealerTemp == 21) {
          System.out.println(players.get(1).getName() + " hit a blackjack!");
          System.out.println("You immediately lost " + prevBet + "✨!");
          players.get(0).removeChips(prevBet);
          mp.addLoss(prevBet);
        } else {
          System.out.println(players.get(0).getName() + " hit a blackjack!");
          System.out.println("You won " + (int) (prevBet * 1.5) + "✨!");
          players.get(0).addChips((int) (prevBet * 1.5));
          mp.addWin((int) (prevBet * 1.5));
          mp.setGain((int) (prevBet * 1.5));
        }
      }
    } else {
      boolean surrendered = false;
      boolean busted = false;
      do { // continues prompting user until they stand/surrender/bust
        action = players.get(0).action(prevBet);
        Utils.sleep(1000);
        if (action[0] == 1) {
          players.get(0).add(deck.deal()[0]);
          Utils.clearScreen();
          for (int j = players.size() - 1; j >= 0; j--) {
            players.get(j).dispHand();
          }
        }
        if (action[0] == 3) {
          System.out.println("You surrendered and got back " + prevBet / 2 + "✨!"); // surrender
          players.get(0).removeChips(prevBet / 2);
          mp.addLoss(prevBet / 2);
          surrendered = true;
          break;
        }
        if (getSum(players.get(0).getHand().toArray(new Card[players.get(0).getHand().size()])) > 21) {
          System.out.println("You busted! You lost " + prevBet + "✨!"); // busted
          players.get(0).removeChips(prevBet);
          mp.addLoss(prevBet);
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
        System.out.print("SHOWDOWN:\n"); // assign winners
        BJBot b = (BJBot) players.get(1);
        b.dispHand(true);
        players.get(0).dispHand();
        int dealer = getSum(players.get(1).getHand().toArray(new Card[players.get(1).getHand().size()]));
        int player = getSum(players.get(0).getHand().toArray(new Card[players.get(0).getHand().size()]));
        if (player > dealer || dealer > 21) {
          players.get(0).addChips(prevBet);
          mp.addWin(prevBet);
          mp.setGain(prevBet);
          System.out.println("You beat the dealer! You won " + prevBet + "✨");
        } else {
          players.get(0).removeChips(prevBet);
          mp.addLoss(prevBet);
          System.out.println(
              "Dealer " + ((player == dealer) ? "tied, house rules, y" : "won! Y") + "ou lost " + prevBet + "✨");
        }
      }
    }
    mp.hands();
    System.out.println("Continue playing? [y/n]:");
    String s = sc.nextLine().strip().toLowerCase();
    if (s.equals("n") || s.equals("no") || s.equals("nah")) {
      System.out.println("Game over! You ended the game early. Here are your stats:\n");
      System.out.println(mp);
    } else {
      deck.reset();
      action = new int[2];
      players.get(0).clear();
      players.get(1).clear();
      if (players.get(0).getChips() > 0)
        main();
      else {
        System.out.println("Game over! You ran out of primogems. Here are your stats:\n");
        System.out.println(mp);
      }
    }
    mp.setChips(players.get(0).getChips());
  }

  public void initialize() {
    main();
  }

  private int getNumber(Card c) { // gets the numerical value of a card based on blackjack rules
    int num = -1;
    String temp = c.getValue().substring(0, 1);
    switch (temp) { 
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

  public int getSum(Card[] c) { // sums a blackjack hand and incorporates the varying value of aces properly
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
