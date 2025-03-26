import java.util.*;

public class BJPlayer extends Player {
  private ArrayList<Card> hand = new ArrayList<Card>(); // getAction
  
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
