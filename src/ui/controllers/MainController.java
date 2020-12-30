package ui.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import core.BitBoard;
import core.CoreConstants;
import core.Main;
import core.Move;
import core.MoveGen;
import eval.EvalConstants;
import eval.Search;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {
	public Canvas chessPane;
	public TextArea pgnTextField;
	public Slider moveSpeedSlider;

	private LinkedList<Integer> blueSquares = new LinkedList<>();
	private int oldPos;
	private Search search = new Search();
	private BitBoard board;
	private String[] pgnHistory = new String[CoreConstants.MAX_MOVES];
	private LinkedList<Move> moveList = new LinkedList<>();

	public void initialize() {
		MoveGen.initialiseKnightLookupTable();
		MoveGen.initialiseKingLookupTable();
		MoveGen.initialisePawnLookupTable();
		MoveGen.generateMoveDatabase(true);
		MoveGen.generateMoveDatabase(false);
		BitBoard.initialiseZobrist();
		moveSpeedSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {
				double value = moveSpeedSlider.getValue();
				EvalConstants.THINKING_TIME = EvalConstants.MAX_THINKING_TIME - (value / 100
						* (EvalConstants.MAX_THINKING_TIME - EvalConstants.MIN_THINKING_TIME));
			}
		});

	}

	private double paintChessBoard(BitBoard board) {
		GraphicsContext g = chessPane.getGraphicsContext2D();
		double width = chessPane.getWidth();
		double height = chessPane.getHeight();
		double cellSize = Math.ceil(Math.min(width / 8.0, height / 8.0));
		int squareNo = 0;
		// Xem xét từng ô vuông
		while (squareNo <= 63) {
			int col = squareNo % 8;
			int row = squareNo / 8;
			// Chọn màu của hình vuông dựa trên hàng và cột
			if ((col % 2 == 0 && row % 2 == 0) || (col % 2 == 1 && row % 2 == 1)) {
				g.setFill(UIConstants.BOARD_COLOUR.getColourPrimary());
			} else {
				g.setFill(UIConstants.BOARD_COLOUR.getColourSecondary());
			}
			// Vẽ hình vuông
			g.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
			squareNo++;
		}

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				byte piece = board.getBoardArray()[(row * 8) + col];
				// Nếu hình vuông không trống, nó sẽ vẽ hình ảnh dựa trên quân cờ
				if (piece != CoreConstants.EMPTY) {
					Image image = new Image(MainController.class
							.getResource("/images/" + CoreConstants.FILE_NAMES[piece] + ".png")
							.toExternalForm());
					g.drawImage(image, col * cellSize, (7 - row) * cellSize, cellSize, cellSize);
				}
			}

		}
		return cellSize;
	}

	public void playGame() {
		if (UIConstants.AI_COLOUR == CoreConstants.WHITE) {
			moveAI(board);
		}
	}

	public void setupGame() {
		board = new BitBoard();
		board.resetToInitialSetup();
		pgnTextField.setText("");
		moveList = MoveGen.generateMoves(board, true);
		double cellSize = paintChessBoard(board);
		// Đảm bảo rằng có một trình nghe hành động đang hoạt động
		chessPane.setOnMouseClicked(evt -> clickListener(board, evt, cellSize));
	}

	private void clearCanvas() {
		GraphicsContext g = chessPane.getGraphicsContext2D();
		g.clearRect(0, 0, chessPane.getWidth(), chessPane.getHeight());
	}

	private void clickListener(BitBoard board, MouseEvent evt, double cellSize) {
		// Lấy vị trí được nhấp theo bảng
		double x = evt.getX();
		double y = evt.getY();
		int column = (int) Math.floor(x / cellSize);
		int row = 7 - (int) Math.floor(y / cellSize);
		boolean pieceMoved = false;
		// Lấy phần đã được nhấp
		int index = (8 * row) + column;
		if (index < 64 && x <= (8 * cellSize) && y <= (8 * cellSize)) {
			byte piece = board.getBoardArray()[index];
			for (int square : blueSquares) {
				if (square == index) {
					move(board, getMove(moveList, board.getBoardArray()[oldPos], oldPos, index), true);
					blueSquares.clear();
					pieceMoved = true;
					break;
				}
			}
			if (piece != CoreConstants.EMPTY && !pieceMoved) {
				// Nhận các bước di chuyển có sẵn của nó
				LinkedList<Move> moves = getMovesPiece(index, moveList);
				oldPos = index;
				// Xóa
				clearCanvas();
				paintChessBoard(board);
				GraphicsContext g = chessPane.getGraphicsContext2D();
				// Hiển thị các bước di chuyển có sẵn bằng cách tô một vòng tròn màu xanh lam trong các ô
				blueSquares.clear();
				for (Move move : moves) {
					int rowMove = Math.floorDiv(move.getFinalPos(), 8);
					int colMove = move.getFinalPos() % 8;
					if (board.getBoardArray()[move.getFinalPos()] == CoreConstants.EMPTY) {
						g.setFill(Color.BLUE);
						g.fillOval(colMove * cellSize, (7 - rowMove) * cellSize, cellSize,
								cellSize);
					} else {
						g.setFill(Color.RED);
						g.fillOval(colMove * cellSize, (7 - rowMove) * cellSize, cellSize / 5,
								cellSize / 5);
					}
					blueSquares.add(move.getFinalPos());
				}

			}
		}
	}

	public Move getMove(LinkedList<Move> moves, int piece, int oldIndex, int finalIndex) {
		for (Move move : moves) {
			if (move.getPieceType() == piece && move.getOldPos() == oldIndex
					&& move.getFinalPos() == finalIndex) {
				return move;
			}
		}
		return null;
	}

	private LinkedList<Move> getMovesPiece(int oldPos, LinkedList<Move> moveList) {
		LinkedList<Move> result = new LinkedList<>();
		for (Move move : moveList) {
			if (move.getOldPos() == oldPos) {
				result.add(move);
			}
		}
		return result;
	}

	public void move(BitBoard board, Move move, boolean repaint) {
		boolean capture = board.getBoardArray()[move.getFinalPos()] != CoreConstants.EMPTY;
		board.move(move);
		int side = move.getPieceType() % 2;
		if (move.isPromotion() && side == UIConstants.PLAYER_COLOUR) {
			pawnPromotion(move.getOldPos(), move.getFinalPos(), side, board, true);
		} else if (move.isPromotion() && side == UIConstants.AI_COLOUR) {
			pawnPromotion(move.getOldPos(), move.getFinalPos(), side, board, false);
		}
		updatePGNTextField(board, move, capture);
		if (repaint) {
			clearCanvas();
			paintChessBoard(board);
			moveList = MoveGen.generateMoves(board, true);

		}
		boolean aiLost = board.checkmate(UIConstants.AI_COLOUR);
		displayEndGameMessage(board);
		// If it is the AI's turn
		if (side == UIConstants.PLAYER_COLOUR && UIConstants.PLAYING_AI && !aiLost) {
			moveAI(board);
		}

	}

	private void displayEndGameMessage(BitBoard board) {
		boolean aiLost = board.checkmate(UIConstants.AI_COLOUR);
		boolean playerLost = board.checkmate(UIConstants.PLAYER_COLOUR);
		boolean stalemate = board.stalemate(board.toMove);
		if (aiLost || playerLost || stalemate) {
			String result = "";
			String sPlayerColour = (UIConstants.PLAYER_COLOUR == 0) ? "WHITE" : "BLACK";
			String sEnemyColour = (UIConstants.PLAYER_COLOUR == 0) ? "BLACK" : "WHITE";
			if (aiLost) {
				// kết quả kkhi chiến thắng
				result = "WIN, playing with " + sPlayerColour;
			} else if (playerLost && UIConstants.PLAYING_AI) {
				result = "YOU LOSE.";
			} else if (playerLost && !UIConstants.PLAYING_AI) {
				result = "The player playing with " + sPlayerColour
						+ " has lost. Congratulations to the player playing with " + sEnemyColour;
			} else if (stalemate) {
				result = "It is a stalemate!";
			}
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Game Over");
			alert.setHeaderText(null);
			alert.setContentText(result);
			alert.showAndWait();
		}
	}

	private void pawnPromotion(int pawnOldPos, int newPos, int side, BitBoard board,
			boolean display) {
		board.removePiece(pawnOldPos);
		if (display) {
			// Lựa chọn hiển thị
			String choice = "";
			List<String> choices = new LinkedList<>();
			choices.add("Queen");
			choices.add("Rook");
			choices.add("Bishop");
			choices.add("Knight");

			ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", choices);
			dialog.setTitle("Pawn Promotion");
			dialog.setHeaderText("Choose the piece to switch your pawn to");
			dialog.setContentText("Choose piece:");
			Optional<String> result = dialog.showAndWait();

			if (result.isPresent()) {
				choice = result.get();
			} else {
				pawnPromotion(pawnOldPos, newPos, side, board, display);
			}
			switch (choice) {
			case "Queen":
				board.addPiece((side == 0) ? CoreConstants.WHITE_QUEEN : CoreConstants.BLACK_QUEEN,
						newPos);
				break;
			case "Rook":
				board.addPiece((side == 0) ? CoreConstants.WHITE_ROOK : CoreConstants.BLACK_ROOK,
						newPos);
				break;
			case "Bishop":
				board.addPiece(
						(side == 0) ? CoreConstants.WHITE_BISHOP : CoreConstants.BLACK_BISHOP,
						newPos);
				break;
			case "Knight":
				board.addPiece(
						(side == 0) ? CoreConstants.WHITE_KNIGHT : CoreConstants.BLACK_KNIGHT,
						newPos);
				break;
			}
		} else {
			board.addPiece((side == 0) ? CoreConstants.WHITE_QUEEN : CoreConstants.BLACK_QUEEN,
					newPos);
		}
	}

	private void moveAI(BitBoard board) {
		int colorFactor = (UIConstants.AI_COLOUR == 0) ? EvalConstants.WHITE : EvalConstants.BLACK;
		Move move = search.rootNegamax(board, colorFactor);
		if (move != null) {
			move(board, move, true);
		}
	}

	private void updatePGNTextField(BitBoard board, Move move, boolean capture) {
		String result = "";
		if (board.getMoveNumber() % 2 == 0) {
			result = " ";
		}
		if (board.getMoveNumber() % 2 == 1 && board.getMoveNumber() != 1) {
			result += "\n";
		}
		pgnTextField.setWrapText(true);
		int side = move.getPieceType() % 2;
		int enemy = (side == 0) ? 1 : 0;
		if (side == 0) {
			result += String.valueOf((board.getMoveNumber() / 2) + 1) + ". ";
		}
		result += CoreConstants.pieceToLetterCapital[move.getPieceType() / 2];
		if (capture) {
			result += "x";
		}
		result += CoreConstants.indexToAlgebraic[move.getFinalPos()];

		if (move.getCastlingFlag() != 0) {
			if (move.getCastlingFlag() == CoreConstants.wQSide
					|| move.getCastlingFlag() == CoreConstants.bQSide) {
				result = String.valueOf(
						((board.getMoveNumber() % 2 == 1 && board.getMoveNumber() != 1) ? "\n" : "")
								+ String.valueOf((board.getMoveNumber() / 2) + 1))
						+ "." + " O-O";
			} else {
				result = String.valueOf(
						((board.getMoveNumber() % 2 == 1 && board.getMoveNumber() != 1) ? "\n" : "")
								+ String.valueOf((board.getMoveNumber() / 2) + 1))
						+ "." + " O-O-O";
			}
		}
		if (board.checkmate(enemy)) {
			result += "#";
		} else if (board.check(enemy)) {
			result += "+";
		}

		pgnTextField
				.setText((pgnTextField.getText() == null ? "" : pgnTextField.getText()) + result);
		// Lưu trữ lịch sử của trường để hoàn tác hoạt động
		pgnHistory[board.getMoveNumber()] = pgnTextField.getText();
	}

	@FXML
	private void handleLoadFileAction(ActionEvent event) {
		System.out.println("LOADING GAME");
		Stage stage = Main.primaryStage;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Game");
		File file = fileChooser.showOpenDialog(stage);
		BufferedReader reader = null;
		if (file != null) {
			try {
				reader = new BufferedReader(new FileReader(file));
				board.toMove = Integer.valueOf(reader.readLine());
				int noOfMoves = Integer.valueOf(reader.readLine());
				board.setMoveNumber(noOfMoves);
				for (int i = 0; i <= 63; i++) {
					board.getBoardArray()[i] = (byte) ((int) Integer.valueOf(reader.readLine()));
				}
				for (int i = 0; i <= 13; i++) {
					board.getBitBoards()[i] = Long.valueOf(reader.readLine());
				}

				for (int i = 0; i < noOfMoves; i++) {
					board.moveHistory[i] = Long.valueOf(reader.readLine());
					board.whiteHistory[i] = Long.valueOf(reader.readLine());
					board.blackHistory[i] = Long.valueOf(reader.readLine());
					board.pawnHistory[0][i] = Long.valueOf(reader.readLine());
					board.pawnHistory[1][i] = Long.valueOf(reader.readLine());
					board.rookHistory[0][i] = Long.valueOf(reader.readLine());
					board.rookHistory[1][i] = Long.valueOf(reader.readLine());
					board.queenHistory[0][i] = Long.valueOf(reader.readLine());
					board.queenHistory[1][i] = Long.valueOf(reader.readLine());
					board.bishopHistory[0][i] = Long.valueOf(reader.readLine());
					board.bishopHistory[1][i] = Long.valueOf(reader.readLine());
					board.knightHistory[0][i] = Long.valueOf(reader.readLine());
					board.knightHistory[1][i] = Long.valueOf(reader.readLine());
					board.kingHistory[0][i] = Long.valueOf(reader.readLine());
					board.kingHistory[1][i] = Long.valueOf(reader.readLine());
					board.boardHistory[0][i] = Byte.valueOf(reader.readLine());
					board.boardHistory[1][i] = Byte.valueOf(reader.readLine());
					board.castlingHistory[0][i] = Long.valueOf(reader.readLine());
					board.castlingHistory[1][i] = Long.valueOf(reader.readLine());
					board.epHistory[0][i] = Long.valueOf(reader.readLine());
					board.epHistory[1][i] = Long.valueOf(reader.readLine());
				}
				board.getCastlingFlags()[0] = Integer.valueOf(reader.readLine());
				board.getCastlingFlags()[1] = Integer.valueOf(reader.readLine());
				int pgnNoOfLines = Integer.valueOf(reader.readLine());
				String pgnText = "";
				for (int i = 0; i < pgnNoOfLines; i++) {
					if (i != pgnNoOfLines - 1) {
						pgnText += reader.readLine() + "\n";
					} else {
						pgnText += reader.readLine();
					}
				}
				pgnTextField.setText(pgnText);
				UIConstants.PLAYER_COLOUR = Integer.valueOf(reader.readLine());
				UIConstants.AI_COLOUR = Integer.valueOf(reader.readLine());
				clearCanvas();
				paintChessBoard(board);
				moveList = MoveGen.generateMoves(board, true);
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error Dialog");
				alert.setContentText(
						"Ooops, there was an error whilst loading the save game file!");
				alert.showAndWait();
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	@FXML
	private void handleSaveFileAction(ActionEvent event) {
		String result = "";
		int noOfMoves = board.getMoveNumber();
		result += board.toMove + "\n";
		result += String.valueOf(noOfMoves) + "\n";
		for (int i = 0; i <= 63; i++) {
			result += board.getBoardArray()[i] + "\n";
		}
		for (int i = 0; i <= 13; i++) {
			result += String.valueOf(board.getBitBoards()[i]) + "\n";
		}
		for (int i = 0; i < noOfMoves; i++) {
			result += String.valueOf(board.moveHistory[i]) + "\n";
			result += String.valueOf(board.whiteHistory[i]) + "\n";
			result += String.valueOf(board.blackHistory[i]) + "\n";
			result += String.valueOf(board.pawnHistory[0][i]) + "\n";
			result += String.valueOf(board.pawnHistory[1][i]) + "\n";
			result += String.valueOf(board.rookHistory[0][i]) + "\n";
			result += String.valueOf(board.rookHistory[1][i]) + "\n";
			result += String.valueOf(board.queenHistory[0][i]) + "\n";
			result += String.valueOf(board.queenHistory[1][i]) + "\n";
			result += String.valueOf(board.bishopHistory[0][i]) + "\n";
			result += String.valueOf(board.bishopHistory[1][i]) + "\n";
			result += String.valueOf(board.knightHistory[0][i]) + "\n";
			result += String.valueOf(board.knightHistory[1][i]) + "\n";
			result += String.valueOf(board.kingHistory[0][i]) + "\n";
			result += String.valueOf(board.kingHistory[1][i]) + "\n";
			result += String.valueOf(board.boardHistory[0][i]) + "\n";
			result += String.valueOf(board.boardHistory[1][i]) + "\n";
			result += String.valueOf(board.castlingHistory[0][i]) + "\n";
			result += String.valueOf(board.castlingHistory[1][i]) + "\n";
			result += String.valueOf(board.epHistory[0][i]) + "\n";
			result += String.valueOf(board.epHistory[1][i]) + "\n";
		}
		result += board.getCastlingFlags()[0] + "\n";
		result += board.getCastlingFlags()[1] + "\n";
		result += String.valueOf(countLines(pgnTextField.getText())) + "\n";
		result += pgnTextField.getText() + "\n";
		result += String.valueOf(UIConstants.PLAYER_COLOUR) + "\n";
		result += String.valueOf(UIConstants.AI_COLOUR) + "\n";
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Game");
		Stage stage = Main.primaryStage;
		File file = fileChooser.showSaveDialog(stage);

		BufferedWriter writer = null;
		if (file != null) {
			try {
				writer = new BufferedWriter(
						new FileWriter(file + (!file.getName().endsWith(".txt") ? ".txt" : "")));
				writer.write(result);
			} catch (IOException ie) {
				ie.printStackTrace();
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Phương thức được gọi khi người dùng nhấn nút khởi động lại trò chơi
	@FXML
	private void restartGame(ActionEvent event) {
		setupGame();
		playGame();
	}

	// Chỉ cần hoàn tác trò chơi hai lần để quay lại bước di chuyển của người chơi
	@FXML
	private void undoAction(ActionEvent event) {
		if (board.getMoveNumber() >= 2 && UIConstants.PLAYING_AI) {
			board.undo();
			board.undo();
			pgnTextField.setText(pgnHistory[board.getMoveNumber()]);
			clearCanvas();
			paintChessBoard(board);
			moveList = MoveGen.generateMoves(board, true);
		} else if (board.getMoveNumber() >= 1 && !UIConstants.PLAYING_AI) {
			board.undo();
			pgnTextField.setText(pgnHistory[board.getMoveNumber()]);
			clearCanvas();
			paintChessBoard(board);
			moveList = MoveGen.generateMoves(board, true);
		}
	}

	@FXML
	private void loadFenMenuItem(ActionEvent event) {
		TextInputDialog dialog = new TextInputDialog("");
		dialog.setTitle("Input FEN Notation");
		dialog.setContentText("Please enter FEN of board to be loaded");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			try {
				// Thay đổi bảng
				board.loadFen(result.get());
				moveList = MoveGen.generateMoves(board, true);
				pgnTextField.setText("");
				clearCanvas();
				paintChessBoard(board);
				if (UIConstants.AI_COLOUR == CoreConstants.WHITE) {
					moveAI(board);
				}
				displayEndGameMessage(board);
			} catch (Exception e) {
				// Nếu có lỗi, thông báo cho người dùng
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error Dialog");
				alert.setContentText("Ooops, there was an error whilst loading the FEN notation. "
						+ "Are you sure the notation is valid?");
				alert.showAndWait();
				e.printStackTrace();
			}
		}

	}

	@FXML
	private void exportFenMenuItem(ActionEvent event) {
		String fen = board.exportFen();
		TextInputDialog alert = new TextInputDialog(fen);
		alert.setResizable(true);
		alert.setTitle("Export FEN");
		alert.setHeaderText("The FEN notation for the current board");
		alert.getEditor().setEditable(false);
		alert.showAndWait();
	}

	@FXML
	private void boardColourMenuItem(ActionEvent event) {
		List<String> choices = new LinkedList<>();
		choices.add("Classic");
		choices.add("Moss Green");
		choices.add("Grey");

		ChoiceDialog<String> dialog = new ChoiceDialog<>(UIConstants.BOARD_COLOUR.getColourName(),
				choices);
		dialog.setHeaderText("Choose a Colour Theme");
		dialog.setTitle("Choose Board Colour");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			switch (result.get()) {
			case "Classic":
				UIConstants.BOARD_COLOUR = BoardColour.CLASSIC;
				break;
			case "Moss Green":
				UIConstants.BOARD_COLOUR = BoardColour.MOSS_GREEN;
				break;
			case "Grey":
				UIConstants.BOARD_COLOUR = BoardColour.GREY;
				break;
			}
		}
		clearCanvas();
		paintChessBoard(board);
	}

	// http://stackoverflow.com/questions/2850203/count-the-number-of-lines-in-a-java-string#2850259
	private static int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}
}