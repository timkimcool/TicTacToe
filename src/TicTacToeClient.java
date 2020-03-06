import java.io.*;
import java.net.*;
import java.util.Date;
import javafx.stage.Stage;
import java.util.Date;
import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;

public class TicTacToeClient extends Application implements TicTacToeConstants {
	private boolean myTurn = false; 			// turn tracker
	private char myToken = ' '; 				// player token
	private char otherToken = ' ';				// other player token
	private Cell[][] cell = new Cell[3][3];		// initialize cells
	private Label lblTitle = new Label();		// create + initialize title label
	private Label lblStatus = new Label();		// create + initialize status label
	private int rowSelected;					// indicate selected row + column by current move
	private int columnSelected;
	private DataInputStream fromServer;			// input and output stream from/to server
	private DataOutputStream toServer;
	private boolean continueToPlay = true;		// continue?
	private boolean waiting = true;				// wait for player to mark cell
	private String host = "localhost";			// host name or ip
	
	
	@Override // override start method in application class
	public void start(Stage primaryStage) throws Exception {
		// Create UI: pane to hold cell
		GridPane pane = new GridPane();
		for (int i = 0; i < 3; i ++)
			for (int j = 0; j <3; j++)
				pane.add(cell[i][j] = new Cell(i, j), j, i);
		
		BorderPane borderPane = new BorderPane();
		borderPane.setTop(lblTitle);;
		borderPane.setCenter(pane);
		borderPane.setBottom(lblStatus);
		
		// create a scene and place it in stage
		Scene scene = new Scene(borderPane, 320, 350);
		primaryStage.setTitle("TicTacToeClient");;
		primaryStage.setScene(scene);
		primaryStage.show();
		
		connectToServer();
	}
	
	private void connectToServer() {
		try {
			// create socket to connect to server
			Socket socket = new Socket(host, 8000);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		// Control game on separate thread
		new Thread(() -> {
			try {
				int player = fromServer.readInt(); // get notification from server
				if (player == PLAYER1) {
					myToken = 'X';
					otherToken = 'O';
					Platform.runLater(() -> {
						lblTitle.setText("Player 1 with token 'X'");
						lblStatus.setText("Waiting for player 2 to join");
					});
					
					fromServer.readInt(); //receive startup notification from server and ignore
					
					Platform.runLater(() -> lblStatus.setText("Player 2 has joined. I start first"));
					myTurn = true;
				} else if (player == PLAYER2) {
					myToken = 'O';
					otherToken = 'X';
					Platform.runLater(() -> {
						lblTitle.setText("Player 2 with token 'X'");
						lblStatus.setText("Waiting for player 1 to move");
					});
				}
				
				// Continue play
				while (continueToPlay) {
					if (player == PLAYER1) {
						waitForPlayerAction();
						sendMove();
						receiveInfoFromServer();
					} else if (player == PLAYER2) {
						receiveInfoFromServer();
						waitForPlayerAction();
						sendMove();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
	}
	
	private void waitForPlayerAction() throws InterruptedException {
		while (waiting) {
			Thread.sleep(100);
		}
		waiting = true;
	}
	
	private void sendMove() throws IOException {
		toServer.writeInt(rowSelected);;
		toServer.writeInt(columnSelected);
	
	}
	
	private void receiveInfoFromServer() throws IOException {
		// Receive game status
		int status = fromServer.readInt();
		
		if (status == PLAYER1_WON) {
			continueToPlay = false;
			if (myToken == 'X') {
				Platform.runLater(() -> lblStatus.setText("I won! (X)"));
			} else if (myToken == 'O') {
			Platform.runLater(() -> lblStatus.setText("Player 1 (X); has won!"));
			receiveMove();
			} 
		} else if (status == PLAYER2_WON) {
			continueToPlay = false;
			if (myToken == 'O') {
				Platform.runLater(() -> lblStatus.setText("I won! (O)"));
			} else if (myToken == 'O') {
			Platform.runLater(() -> lblStatus.setText("Player 2 (O); has won!"));
			receiveMove();
			} 
		} else if (status == DRAW) {
			continueToPlay = false;
			Platform.runLater(() -> lblStatus.setText("Game is over, no winner!"));
			if (myToken == 'O') {
				receiveMove();
			} 
		} else {
			receiveMove();
			Platform.runLater(() -> lblStatus.setText("My turn)"));
			myTurn = true;
		}
	}
	
	private void receiveMove() throws IOException {
		int row = fromServer.readInt();
		int column = fromServer.readInt();
		Platform.runLater(() -> cell[row][column].setToken(otherToken));
	}
	
	//inner class for cell
	public class Cell extends Pane {
		private int row;
		private int column;
		private char token = ' ';
		
		public Cell(int row, int column) {
			this.row = row;
			this.column = column;
			this.setPrefSize(2000,  2000);
			setStyle("-fx-border-color: black");
			this.setOnMouseClicked(e -> handleMouseClick());
		}

		public char getToken() {
			return token;
		}

		public void setToken(char c) {
			token = c;
			repaint();
		}
		
		protected void repaint() {
			if (token == 'X') {
				Line line1 = new Line(10, 10, this.getWidth() - 10, this.getHeight() - 10);
				line1.endXProperty().bind(this.widthProperty().subtract(10));
				line1.endYProperty().bind(this.heightProperty().subtract(10));
				Line line2 = new Line(10, this.getHeight() - 10, this.getWidth() - 10, 10);
				line2.startYProperty().bind(this.heightProperty().subtract(10));
				line2.endXProperty().bind(this.widthProperty().subtract(10));
				this.getChildren().addAll(line1, line2);
			} else if (token == 'O') {
				Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2, this.getWidth() / 2-10, this.getHeight() / 2 - 10);
				ellipse.centerXProperty().bind(this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
				ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));
				ellipse.setStroke(Color.BLACK);
				ellipse.setFill(Color.WHITE);
				getChildren().add(ellipse);
			}
		}
		
		private void handleMouseClick() {
			if (token == ' ' && myTurn) {
				setToken(myToken);
				myTurn = false;
				rowSelected = row;
				columnSelected = column;
				lblStatus.setText("Waiting for the other player to move");;
				waiting = false;
			}
		}
		
		
	}

}
