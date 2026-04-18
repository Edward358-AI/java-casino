import java.util.*;

public class BustRateSim_15_v2 {
    public static void main(String[] args) {
        System.out.println("Starting 10,000 Games Simulation (15 Hands each)...");
        
        int godBusts = 0;
        int smartBusts = 0;
        int dumbBusts = 0;
        int totalSimulations = 10000;
        int handsPerGame = 15;
        
        String[] suits = {"♠️", "♣️", "♦️", "♥️"};
        String[] vals = {"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
        
        for (int i = 0; i < totalSimulations; i++) {
            PokerPlayer[] players = new PokerPlayer[6];
            for (int k = 0; k < 6; k++) {
                players[k] = new PokerBot(null);
                players[k].addChips(1000);
            }
            
            // Assign Levels: 1 God, 2 Smart, 3 Dumb
            setBotLevel((PokerBot)players[0], 2);
            setBotLevel((PokerBot)players[1], 1);
            setBotLevel((PokerBot)players[2], 1);
            setBotLevel((PokerBot)players[3], 0);
            setBotLevel((PokerBot)players[4], 0);
            setBotLevel((PokerBot)players[5], 0);

            for (int h = 0; h < handsPerGame; h++) {
                int activeCount = 0;
                for(PokerPlayer p : players) if(p.getChips() > 0) activeCount++;
                if(activeCount < 2) break;

                List<Card> deck = new ArrayList<>();
                for(String s : suits) for(String v : vals) deck.add(new Card(v+s));
                Collections.shuffle(deck);
                
                for (PokerPlayer p : players) p.setInHand(p.getChips() > 0);
                for (PokerPlayer p : players) if(p.inHand()) p.setHand(new Card[]{deck.remove(0), deck.remove(0)});

                int pot = 40; 
                int currentBet = 20;
                int preflopAggressor = -1;

                // Preflop
                for (int pIndex = 0; pIndex < 6; pIndex++) {
                    PokerPlayer p = players[pIndex];
                    if (p.inHand() && p.getChips() > 0) {
                        int[] act = ((PokerBot)p).action("preflop", 0, currentBet, 20, null, pot, players, pIndex, -1);
                        if (act[0] == 2) p.setInHand(false);
                        else {
                            int cost = Math.min(p.getChips(), act[1]);
                            p.removeChips(cost);
                            pot += cost;
                            if (act[1] > currentBet) {
                                currentBet = act[1];
                                preflopAggressor = pIndex;
                            }
                        }
                    }
                }

                // Postflop
                if (deck.size() > 5) {
                    Card[] b = {deck.remove(0), deck.remove(0), deck.remove(0), deck.remove(0), deck.remove(0)};
                    // Simulating simple showdown for bust tracking
                    int winner = -1;
                    int bestRank = 10;
                    for (int k = 0; k < 6; k++) {
                        if (players[k].inHand()) {
                            PokerDeck d = new PokerDeck();
                            Card[] best = d.getBestHand(players[k].getHand(), new Card[]{b[0], b[1], b[2], b[3], b[4]});
                            int rank = d.getRanking(best);
                            if (rank < bestRank) {
                                bestRank = rank;
                                winner = k;
                            }
                        }
                    }
                    if (winner != -1) players[winner].addChips(pot);
                }
            }
            
            if (players[0].getChips() == 0) godBusts++;
            if (players[1].getChips() == 0) smartBusts++;
            if (players[2].getChips() == 0) smartBusts++;
            if (players[3].getChips() == 0) dumbBusts++;
            if (players[4].getChips() == 0) dumbBusts++;
            if (players[5].getChips() == 0) dumbBusts++;
        }

        System.out.println("--- BUST RATE (15 Hands) ---");
        System.out.println("God Bot:   " + (godBusts * 100.0 / totalSimulations) + "%");
        System.out.println("Smart Bot: " + ((smartBusts / 2.0) * 100.0 / totalSimulations) + "%");
        System.out.println("Dumb Bot:  " + ((dumbBusts / 3.0) * 100.0 / totalSimulations) + "%");
    }

    private static void setBotLevel(PokerBot bot, int level) {
        try {
            java.lang.reflect.Field f = PokerBot.class.getDeclaredField("botLevel");
            f.setAccessible(true);
            f.set(bot, level);
        } catch (Exception e) {}
    }
}
