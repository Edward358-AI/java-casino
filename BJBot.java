// import java.util.*;

public class BJBot extends BJPlayer {
  private static String[] names = { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie",
      "Ellie", "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena",
      "Joe", "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia",
      "Bryan", "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Aventurine" };

  public BJBot() { // <=16 hit >17 stand
    super(names[(int) (Math.random() * names.length)]);
  }

  public int[] action(int gS) {
    int[] out = new int[2];
    if (gS >= 17) {
      out[0] = 2;
    } else {
      out[0] = 1;
    }
    return out;
  }

  public void dispHand(boolean show) {
    super.dispHand();
  }

  public void dispHand() {
      System.out.println(); // start to display cards each person has
      System.out.println(getName() + ":");
      System.out.print("??? ");
      for (int i = 1; i < getHand().size(); i++) {
        System.out.print(getHand().get(i).getValue() + "  ");
      }
      System.out.println("\n");
    }
}