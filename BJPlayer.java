import java.util.*;

public class BJPlayer extends Player {
  private ArrayList<Card> hand = new ArrayList<Card>(); // getAction
  Scanner s = new Scanner(System.in);

  public BJPlayer(String name) {
    super(name);
  }

  public void add(Card c) {
    hand.add(c);
  }
  public void clear() {
    hand.clear();
  }

  
}
