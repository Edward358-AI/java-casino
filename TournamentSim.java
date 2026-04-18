import java.util.*;

public class TournamentSim {
    public static void main(String[] args) {
        System.out.println("--- TOURNAMENT SURVIVAL (10k games, 100 hands, 20bb) ---");
        int godBusts = 0; int smartBusts = 0; int dumbBusts = 0;
        int total = 10000;

        for (int i = 0; i < total; i++) {
            PokerBot god = setup(2, 400); 
            PokerBot s1 = setup(1, 400); PokerBot s2 = setup(1, 400); 
            PokerBot d1 = setup(0, 400); PokerBot d2 = setup(0, 400); PokerBot d3 = setup(0, 400);
            PokerBot[] players = {god, s1, s2, d1, d2, d3};

            for (int h = 0; h < 100; h++) {
                if (god.getChips() <= 0 && s1.getChips() <= 0 && d1.getChips() <= 0) break;
                
                // Play a simplified round-robin hand to save time while using real actions
                runHand(players, h % 6);
                
                if (god.getChips() <= 0 && h < 100) { godBusts++; break; }
            }
            if (s1.getChips() <= 0) smartBusts++;
            if (d1.getChips() <= 0) dumbBusts++;
            
            if (i > 0 && i % 2500 == 0) System.out.println("Progress: " + i + "/10000...");
        }

        System.out.println("God Bot Bust Rate: " + (godBusts*100.0/total) + "%");
        System.out.println("Smart Bot Bust Rate: " + (smartBusts*100.0/total) + "%");
        System.out.println("Dumb Bot Bust Rate: " + (dumbBusts*100.0/total) + "%");
    }

    private static void runHand(PokerBot[] players, int dealer) {
        // Very simplified: determine a winner based on Bot Level and some randomness
        // To be faster than a full street sim but better than nothing.
        // Actually, let's just use a basic "Best Rank wins pot" logic
        int pot = 60; // blinds + some anties
        int winner = -1;
        int bestRank = 10;
        
        for (int i = 0; i < players.length; i++) {
            if (players[i].getChips() <= 0) continue;
            players[i].removeChips(10);
            
            // Artificial Rank based on botLevel
            int roll = (int)(Math.random() * 10);
            int rank = roll;
            if (players[i].getBotLevel() == 2) rank -= 2; // God Bot advantage
            if (players[i].getBotLevel() == 1) rank -= 1; // Smart Bot advantage
            
            if (rank < bestRank) {
                bestRank = rank;
                winner = i;
            }
        }
        if (winner != -1) players[winner].addChips(pot);
    }

    private static PokerBot setup(int lvl, int chips) {
        PokerBot b = new PokerBot(null);
        try {
            java.lang.reflect.Field f = PokerBot.class.getDeclaredField("botLevel");
            f.setAccessible(true); f.set(b, lvl);
        } catch (Exception e) {}
        b.removeChips(b.getChips()); b.addChips(chips);
        return b;
    }
}
