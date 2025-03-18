public class Card {
  private String value;
  private int num;
  public Card(String value) {
    this.value = value;
    String temp = value.substring(0,1);
      switch(temp) {
        case "T":
          num = 10;
          break;
        case "J": 
          num = 11;
          break;
        case "Q":
          num = 12;
          break;
        case "K":
          num = 13;
          break;
        case "A":
          num = 14;
          break;
        default:
          num = Integer.parseInt(temp);
      }
    }
  public String getValue() {
    return value;
  }
  public int getNum() {
    return num;
  }
}