import java.util.*;

public class PokerBot extends PokerPlayer {
  private boolean opMode = false;

  public PokerBot() {
    super("temp");
    randomName();
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public void randomName() {
    super.setName(Names.getName());
  }

  // funny
  public void checkName() {
    if (super.getName().equals("Aventurine")) {
      opMode = true;
    }
  }

  public int[] action(String round, int prevBet, int bet, int blind, Card[] board) {
    int[] action = new int[2];
    double rand = Math.random();
    if (opMode && super.getChips() > 0) {
      action[0] = 4;
      action[1] = super.getChips();
    } else if (bet < super.getChips()) {
      if (rand >= 0 && rand < 0.75) {
        action[0] = 1;
        if (bet > 0) {
          action[1] = (bet >= super.getChips()) ? super.getChips() : bet - prevBet;
        } else
          action[1] = (bet == 0) ? 0 : bet - prevBet;
      } else if (rand >= 0.75 && rand < 0.85) {
        if (((bet == 0) ? blind : bet*2) + super.getChips()/10 < super.getChips()) {
          int max;
          int min;
          if (round.equals("preflop")) {
            max = super.getChips() / 10 + bet*2;
            min = bet*2;
          } else {
            if (bet == 0) {
              max = super.getChips() / 10 + blind;
              min = blind;
  
            } else {
              max = super.getChips() / 10 + bet*2;
              min = bet*2;
            }
          }
          action[0] = 3;
          action[1] = (int) (Math.random() * (max - min + 1) + min);
        } else {
          if (Math.random() > 0.85) {
            action[0] = 4;
            action[1] = super.getChips();
          } else {
            action[0] = 2;
            
          }
        }
      } else if (rand >= 0.85 && rand < 0.97 ) {
        action[0] = (bet == 0) ? 1 : 2;
      } else {
        action[0] = 4;
        action[1] = super.getChips();
      }
    } else {
      if (Math.random() > 0.8) {
        action[0] = 4;
        action[1] = super.getChips();
      } else {
        action[0] = 2;
        
      }
    }
    return action;
  }
}

class Names { // avoid dupe names
  private static ArrayList<String> names = new ArrayList<String>(
      Arrays.asList(new String[] { "Bob", "Rob", "Alice", "Aaron", "Sam", "Eddie", "Rachel", "Mike", "Charlie", "Ellie",
          "Colin", "Kevin", "Victor", "Robin", "Jean", "Katheryne", "Dan", "Mark", "Richard", "Dana", "Elena", "Joe",
          "Juan", "Tony", "Ella", "Sammy", "Edward", "Ethan", "Jonathan", "Jason", "Evelyn", "Josie", "Sophia", "Bryan",
          "Allen", "Alan", "Kim", "Chloe", "Claire", "Jerry", "Toby", "Scarlet", "Alex", "Leon", "Eric", "GuyWhoGoesAllInEveryTime", "Fei Yu-Ching", "Jay", "Daniel", "Evan", "Sean", "Selene", "James", "Jacques", "NoName", "Zoe", "Sarah", "Kyle", "Irene", "Sharolyn", "Ben", "Coco", "Cindy", "Megan", "Mia", "E-TEN", "Audrey", "Emily", "March 7th", "Stelle", "Cao Cao", "Liu", "Camellia", "Cameron", "Maddie", "Will", "Amy", "Aventurine" }));

  public static String getName() {
    return names.remove((int) (Math.random() * names.size()));
  }
}