class PokerDeck extends Deck {
  private int pot;
  public PokerDeck() {
    super();
  }
  public void reset() {
    super.reset();
    pot = 0;
  }
  public int getPot() {
    return pot;
  }
  public void addChips(int c) {
    pot += c;
  }
  public Card[] deal() {
    Card[] board = new Card[5];
    for (int i = 0; i < 5; i++) {
      board[i] = super.deal()[0];
    }
    return board;
  }
  public Card[][] deal(int players) {
    Card[][] hands = new Card[players][2];
    for (int i = 0; i < players; i++) {
      for (int j = 0; j < 2; j++) {
        hands[i][j] = super.deal()[0];
      }
    }
    return hands;
  }
}