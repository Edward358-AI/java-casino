public class PlayerStat {
  private int hands;
  private int totalBet;
  private int totalWin;
  private int totalLoss;
  private int biggestGain;
  private int allInTimes;
  private int totalChips;

  public PlayerStat(int chips) {
    hands = 0;
    totalBet = 0;
    totalWin = 0;
    totalLoss = 0;
    biggestGain = 0;
    allInTimes = 0;
    totalChips = chips;
  }

  public int getHands() {
    return hands;
  }

  public int getBets() {
    return totalBet;
  }

  public int getWins() {
    return totalWin;
  }

  public int getLoss() {
    return totalLoss;
  }

  public int getGain() {
    return biggestGain;
  }

  public int allIns() {
    return allInTimes;
  }

  public void merge(PlayerStat p) { // merges two PlayerStat objects
    if (p != null) {
      hands += p.getHands();
      totalBet += p.getBets();
      totalWin += p.getWins();
      totalLoss += p.getLoss();
      biggestGain += p.getGain();
      allInTimes += p.allIns();
    }
  }

  public int getChips() {
    return totalChips;
  }
  public void setChips(int chips) {
    totalChips = chips;
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
    if (gain > biggestGain)
      biggestGain = gain;
  }

  public void allIn() {
    allInTimes++;
  }

  public String toString() {
    return String.format(
        "Hands played: %1$d\nTotal amount bet: %2$d\nTotal amount won: %3$d\nTotal amount lost: %4$d\nBiggest win: %5$d\nNo. of times all in: %6$d\n",
        hands, totalBet, totalWin, totalLoss, biggestGain, allInTimes);
  }
}