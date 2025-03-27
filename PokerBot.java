import java.util.*;

public class PokerBot extends PokerPlayer {
  private String[] names = {"Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie", "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe", "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan", "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Aventurine"};
  private boolean opMode = false;
  public PokerBot() {
    super("temp");
    randomName();
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }
  public void randomName () {
    super.setName(names[(int) (Math.random() * names.length)]);
  }
  public int[] action(String round, int prevBet, int bet, int blind) {
    int[] action = new int[2];
    int rand =  (int) (Math.random() * 100);
    if (rand > 70) {
      action[0] = 2;
      super.setInHand(false);
    } else {
      action[0] = 1;
      action[1] = bet - prevBet;
    }
    return action;
  }
  public int[] action(String round, int prevBet, int bet, int blind, Card[] board) {
    int[] action = new int[2];
    int rand =  (int) (Math.random() * 100);
    if (rand > 70) {
      action[0] = 2;
      super.setInHand(false);
    } else {
      action[0] = 1;
      action[1] = bet - prevBet;
    }
    return action;
  }
}