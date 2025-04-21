import java.util.*;

public class BJBot extends BJPlayer {
  private static String[] names = {"Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie", "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe", "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan", "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Aventurine"};
  private boolean opMode = false;
  public BJBot() { // >=16 hit >=17 stand
    super(names[(int) (Math.random() * names.length)]);
    if (super.getName().equals("Aventurine")) { // see next card?
      opMode = true;
    }
  }

  public int[] action(int prevBet) {
    int[] out = new int[2];
    out[0]+=Math.random()*2+1;
    return out;
  }

  public void dispHand(boolean isFirst) {
    System.out.println(); // start to display cards each person has
    System.out.println(getName());
    if (isFirst) {
      System.out.print(getHand().get(0).getValue() + "  ");
    } else {
      for(Card card : getHand()) {
        System.out.print(card.getValue() + "  ");
      }
    }
    System.out.println("\n");
  }
}
