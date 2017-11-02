package battleship_server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Random;

import battleship_server.Game.Player;

public class Server{

	
	public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
        	int counter = 1;
        	Game game = new Game("BattleShip server", 1);
        	serverSocket = new ServerSocket(9090, 2, InetAddress.getLocalHost());//InetAddress.getByName("25.70.115.243"));
        	game.log("Connected to: " + InetAddress.getLocalHost().getHostAddress() + 
        		"\nBattleship server is Running.");
            while (true) {
            	
            	Player[] player = new Player[2];
            	player[0] = game.new Player(serverSocket.accept(), 1);
            	game.log("Game #" + counter);
            	game.log("Player 1 connected.");
                player[1] = game.new Player(serverSocket.accept(), 2);
                game.log("Player 2 connected.");
                
                player[0].setOpponent(player[1]);
                player[1].setOpponent(player[0]);
                
                Random r = new Random();
                int random = r.nextInt(2);
                
                game.log("Starting player: " + player[random].getPlayerNumber());
                game.setCurrentPlayer(player[random]);
                
                player[0].start();
                player[1].start();
                
                counter++;
                game.log("------------------------------------------");
            }
        } catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	
}
