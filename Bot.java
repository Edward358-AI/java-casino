import java.util.*;

public class Bot extends Player {
  private String[] names = {"Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie", "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe", "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan", "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Aventurine"};
  private boolean opMode = false;
  public Bot() {
    super("");
    randomName();
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }
  public void randomName () {
    super.setName(names[(int) (Math.random() * names.length)]);
  }
  public String action(String round) {
    return "";
  }
}