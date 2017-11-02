package battleship_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Game extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7438015371395435573L;
	private Player currentPlayer;
	private boolean[] playersReady;
	private int[] playersWantToPlayAgain; //0 = didn't get the question yet; 1 = wants to play; -1 = dont want to play;
	private int hitsNeededToWin;
	private boolean gameOver;
	private static final int HIT = 1;
	private static final int MISS = -1;
	private static final int ILLEGAL = 0;
	
	private JTextArea textArea;
	
	public Game(String title, int variation){
		playersReady = new boolean[2];
		playersWantToPlayAgain = new int[2];
		gameOver = false;
		if(variation == 1){
			hitsNeededToWin = 17;
		}
		
		//GUI stuff:
		setTitle(title);
		setSize(300, 400);
		
		textArea = new JTextArea("Trying to connect...\n");
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		add(scrollPane);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setLocationRelativeTo(null);
	}
	
	public void log(String text){
		textArea.append(text + "\n");
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	public void setCurrentPlayer(Player currentPlayer) {
		this.currentPlayer = currentPlayer;
	}

	private boolean arePlayersReady(){
		return (playersReady[0] && playersReady[1]);
	}
	
	private int arePlayersWantToPlayAgain(){
		if(playersWantToPlayAgain[0] == 0 || playersWantToPlayAgain[1] == 0)
			return 0;
		else if(playersWantToPlayAgain[0] == -1 || playersWantToPlayAgain[1] == -1)
			return -1;
		
		return 1;
	}
	
	private synchronized int legalMove(String shootPlace, Player player){
		if(player == currentPlayer){
			try{
				int i=0;
				for(; i<shootPlace.length(); i++){
					if(shootPlace.charAt(i) == ',')
						break;
				}
				
				int x = Integer.parseInt(shootPlace.substring(0, i));
				int y = Integer.parseInt(shootPlace.substring(i+2));
				
				Player opponent = player.getOpponent();
				int[][] opponentBoard = opponent.getBoard();
				if(y >= opponentBoard.length || y < 0 || x >= opponentBoard[y].length || x < 0) return ILLEGAL;
				
				if(opponentBoard[y][x] == 0 || opponentBoard[y][x] == opponent.getPlayerNumber()){
					boolean hit = opponent.shootIsHit(x, y);
					opponent.otherPlayerShoot(hit, x, y);
					currentPlayer.informHitOrMiss(hit, x, y);
					
					if(hit){
						currentPlayer.setHitCount(currentPlayer.getHitCount()+1);
						return HIT;
					}else{
						currentPlayer = opponent;
						return MISS;
					}
				}
			}catch(NumberFormatException numberE){
				return ILLEGAL;
			}
		}
		
		return ILLEGAL;
	}
	
	private boolean isWinner(){
		if(currentPlayer.getHitCount() == hitsNeededToWin){
			currentPlayer.informWin(currentPlayer);
			currentPlayer.getOpponent().informWin(currentPlayer);
			currentPlayer = currentPlayer.getOpponent();
			return true;
		}
		
		return false;
	}
	
	private boolean isCurrentPlayer(Player player){
		return player == currentPlayer;
	}
	
	public class Player extends Thread {
		private Socket socket;
		private BufferedReader input;
	    private PrintWriter output;
		private Player opponent;
		private int[][] board;
		private int playerNumber;
		private int hitCount;
		
		public Player(Socket socket, int playerNumber){
			this.socket = socket;
			this.playerNumber = playerNumber;
			
			try {
				input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream(), true);
				
				output.println("Welcome, Your player number is: " + playerNumber);
				output.println("Please wait for the other player to connect.");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public Player getOpponent() {
			return opponent;
		}
		
		public int[][] getBoard(){
			return board;
		}
		
		public int getPlayerNumber() {
			return playerNumber;
		}
		
		public int getHitCount(){
			return hitCount;
		}
		
		public void setOpponent(Player opponent) {
			this.opponent = opponent;
		}
		
		private void setBoard(String boardStr){
			int commaI = -1;
			int i=boardStr.length()-1;
			for(; i>=0; i--){
				if(boardStr.charAt(i) == ',')
					commaI = i;
				else if(boardStr.charAt(i) == '.')
					break;
			}
			
			int width = Integer.parseInt(boardStr.substring(i + 2, commaI));
			int height = Integer.parseInt(boardStr.substring(commaI + 2));
			board = new int[height][width];
			boardStr = boardStr.substring(0, i) + ", ";
			
			int lineLength = boardStr.length()/height;
			for(int y=0; y<height; y++){
				String currentLine = boardStr.substring(0, lineLength);
				String[] seperated = currentLine.split(", ");
				for(int x=0; x<width; x++){
					int current = Integer.parseInt(seperated[x]);
					board[y][x] = current;
				}
				boardStr = boardStr.substring(lineLength);
			}
		}
		
		private void setHitCount(int hitCount){
			this.hitCount = hitCount;
		}
		
		public boolean shootIsHit(int x, int y){
			int before = board[y][x];
			board[y][x] = opponent.getPlayerNumber();
			
			return (before == playerNumber);
		}
		
		public void otherPlayerShoot(boolean hit, int x, int y) {
			if(hit)
				output.println("Opponent HIT - " + x + ", " + y);
			else
				output.println("Opponent MISS - " + x + ", " + y);
		}
		
		public void informHitOrMiss(boolean hit, int x, int y){
			if(hit)
				output.println("HIT - " + x + "," + y);
			else
				output.println("MISS - " + x + "," + y);
		}
		
		public void informWin(Player winner){
			output.println("GAME OVER");
			output.println("Winner: Player " + winner.getPlayerNumber());
		}
		
		@Override
		public void run() {
			try {
				String message = "";
				do{
					output.println("Both players are connected, ready to start. Please order your fleet and click ready.");
					String boardStr = input.readLine();
					setBoard(boardStr);
					output.println("Waiting for the other player to order his fleet...");
					
					playersReady[playerNumber - 1] = true;
					
					while(!arePlayersReady()){
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					while(true){
						if(gameOver)
							break;
						
						if(isCurrentPlayer(this)){
							output.println("Your turn");
							String command = input.readLine();
							
							if(command.startsWith("Shoot ")){
								String shootPlace = command.substring(6);
								int legal = legalMove(shootPlace, this);
								if(legal != Game.ILLEGAL){
									log("Player " + playerNumber + ": " + command + ". ");
									if(legal == Game.HIT){
										log("HIT");
										if(isWinner()){
											playersWantToPlayAgain[0] = 0;
											playersWantToPlayAgain[1] = 0;
											gameOver = true;
											log("Game Over! Player " + playerNumber + " won.");
											log("Asking players if they would like to play again.");
										}
									}else if(legal == Game.MISS){
										log("MISS");
									}
								}else{
									output.println("Illegal move, try again.");
								}
								
							}
						}
						
						sleep(1000);
					}
					
					message = input.readLine();
					log("Player " + playerNumber + " answer: " + message);
					if(message.equals("YES")){
						playersReady[playerNumber - 1] = false;
						playersWantToPlayAgain[playerNumber - 1] = 1;
						
						int playAgain = arePlayersWantToPlayAgain();
						while(playAgain == 0){
							try {
								sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							playAgain = arePlayersWantToPlayAgain();
						}
						
						if(playAgain == 1){
							gameOver = false;
							hitCount = 0;
							opponent.setHitCount(0);
							output.println("RESTARTING");
						}else if(playAgain == -1){
							output.println("The other player doesn't want to play again.");
							break;
						}
					}else{
						playersWantToPlayAgain[playerNumber - 1] = -1;
					}
				}while(message.equals("YES"));
				
			} catch (Exception e) {
				/*log("Connection with player " + playerNumber + " lost.");
				opponent.InformOtherPlayerRageQuit();
				opponent.closeSocket();
				closeSocket();
				*/
				e.printStackTrace();
			} //catch (InterruptedException e) {
				//e.printStackTrace();
			finally{
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}

		private void InformOtherPlayerRageQuit() {
			output.println("Game Over, the other player probably had a rage quit.");
		}

		private void closeSocket() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
		
	}
}
