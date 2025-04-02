import java.util.*;

public class BJPlayer extends Player {
  private ArrayList<Card> hand = new ArrayList<Card>(); // getAction
  private Scanner s = new Scanner(System.in);

  public BJPlayer(String name) {
    super(name);
  }

  public void add(Card c) {
    hand.add(c);
  }
  public void clear() {
    hand.clear();
  }

  public int[] action(int prevBet, int c) {
    int[] out = new int[2];
    // 0 betting
    // 1 dealing
    // 2 player's turn // can only hit or stand, no double down or surrender so bet is not made into a method
    // 3 dealer's turn
    // external method to check if win
    switch(c) {
      case 0:
        out[0] = this.getValidInt("Place your bet: ", 0);
        //System.out.println(out[0]);
        return out;
    }
    //System.out.println("Your hand: " + hand[0] + " " + hand[1]);
    return null;
  }
  public int getValidInt(String message, int min) { // override (maybe overload?); dont need bounds sometimes
    int x;
    while (true) {
      System.out.println(message);
      try {
        String z = s.nextLine().trim();
        x = Integer.parseInt(z);
        if (x>=min) break;
      } catch (Exception e) {
        continue;
      }
    }
    return x;
  }
}
