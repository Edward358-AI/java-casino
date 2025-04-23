public class PlayerStat {
  private int hands;
  private int totalBet;
  private int totalWin;
  private int totalLoss;
  private int biggestGain;
  private int allInTimes;

  public PlayerStat() {
    hands = 0;
    totalBet = 0;
    totalWin = 0;
    totalLoss = 0;
    biggestGain = 0;
    allInTimes = 0;
  }

  public void hands() {
    hands++;
  }

  public void addBet(int bet) {
    totalBet += bet;
  }

  public void addWin(int win) {
    totalWin += win;
  }

  public void addLoss(int loss) {
    totalLoss += loss;
  }

  public void setGain(int gain) {
    if (gain > biggestGain) biggestGain = gain;
  }

  public void allIn() {
    allInTimes++;
  }

  public String toString() {
    return String.format("""
        Hands played: %1$d
        Total amount bet: %2$d
        Total amount won: %3$d
        Total amount lost: %4$d
        Biggest win: %5$d
        No. of times all in: %6$d
        """, hands, totalBet, totalWin, totalLoss, biggestGain, allInTimes);
  }
}