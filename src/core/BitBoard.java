package core;

import java.util.Random;

public class BitBoard {
	private long[] bitboards = new long[14];
	private byte[] board = new byte[64];
	private long[] epTargetSquares = new long[2];
	private long[] castling = new long[] { 0L, 0L };
	public int toMove = CoreConstants.WHITE;
	int moveNumber = 0;

	// https://github.com/albertoruibal/carballo/blob/master/core/src/main/java/com/alonsoruibal/chess/Board.java
	public long[] moveHistory;
	public long[] whiteHistory;
	public long[] blackHistory;
	public long[][] pawnHistory;
	public long[][] rookHistory;
	public long[][] queenHistory;
	public long[][] bishopHistory;
	public long[][] knightHistory;
	public long[][] kingHistory;
	public byte[][] boardHistory;
	public long[][] castlingHistory;
	public long[][] epHistory;

	public BitBoard() {
		// Khởi tạo mảng lịch sử
		moveHistory = new long[CoreConstants.MAX_MOVES];
		whiteHistory = new long[CoreConstants.MAX_MOVES];
		blackHistory = new long[CoreConstants.MAX_MOVES];
		pawnHistory = new long[2][CoreConstants.MAX_MOVES];
		rookHistory = new long[2][CoreConstants.MAX_MOVES];
		queenHistory = new long[2][CoreConstants.MAX_MOVES];
		bishopHistory = new long[2][CoreConstants.MAX_MOVES];
		knightHistory = new long[2][CoreConstants.MAX_MOVES];
		kingHistory = new long[2][CoreConstants.MAX_MOVES];
		epHistory = new long[2][CoreConstants.MAX_MOVES];
		boardHistory = new byte[CoreConstants.MAX_MOVES][64];
		epHistory = new long[2][CoreConstants.MAX_MOVES];
		castlingHistory = new long[2][CoreConstants.MAX_MOVES];
	}

	public void loadFen(String board) {
		moveNumber = 0;
		// Bắt đầu đặt lại bảng
		reset();
		board = board.replace("/", "");
		board = board.replace("1", " ");
		board = board.replace("2", "  ");
		board = board.replace("3", "   ");
		board = board.replace("4", "    ");
		board = board.replace("5", "     ");
		board = board.replace("6", "      ");
		board = board.replace("7", "       ");
		board = board.replace("8", "        ");
		int index = 0;
		while (index <= 63) {
			int blackPiece = !Character.isUpperCase(board.charAt(index)) ? 1 : 0;
			int littleEndianIndex = 56 + (2 * (index % 8)) - index;
			switch (Character.toLowerCase(board.charAt(index))) {
			case 'p':
				addPiece((byte) (CoreConstants.WHITE_PAWN + blackPiece), littleEndianIndex);
				break;
			case 'n':
				addPiece((byte) (CoreConstants.WHITE_KNIGHT + blackPiece), littleEndianIndex);
				break;
			case 'b':
				addPiece((byte) (CoreConstants.WHITE_BISHOP + blackPiece), littleEndianIndex);
				break;
			case 'r':
				addPiece((byte) (CoreConstants.WHITE_ROOK + blackPiece), littleEndianIndex);
				break;
			case 'q':
				addPiece((byte) (CoreConstants.WHITE_QUEEN + blackPiece), littleEndianIndex);
				break;
			case 'k':
				addPiece((byte) (CoreConstants.WHITE_KING + blackPiece), littleEndianIndex);
				break;
			}
			index++;
		}
		// di chuyển tiếp
		if (index + 1 < board.length()) {
			char toMoveChar = board.charAt(index + 1);
			toMove = (toMoveChar == 'w') ? 0 : 1;
		}
	}

	public String exportFen() {
		String result = "";
		for (int row = 56; row >= 0; row -= 8) {
			for (int col = 0; col <= 7; col++) {
				int index = row + col;
				if (board[index] == CoreConstants.EMPTY) {
					result += ' ';
				} else {
					result += CoreConstants.pieceToLetterFen[board[index]];
				}
			}
			if (row != 0) {
				result += "/";
			}
		}
		result = result.replace("        ", "8");
		result = result.replace("       ", "7");
		result = result.replace("      ", "6");
		result = result.replace("     ", "5");
		result = result.replace("    ", "4");
		result = result.replace("   ", "3");
		result = result.replace("  ", "2");
		result = result.replace(" ", "1");
		return result;
	}

	// http://peterellisjones.com/post/41238723473/chess-engine-part-ii-move-encoding-and-move
	// Corresponding github repo: https://github.com/peterellisjones/Checkmate
	public void addPiece(byte piece, int square) {
		// Thêm quân cờ vào mảng bàn cờ
		board[square] = piece;
		long bitboard = 1L << square;

		// Thêm bảng vào bitboard cho quân cờ và màu của nó
		bitboards[piece & 1] |= bitboard;
		bitboards[piece] |= bitboard;
	}

	public void removePiece(int square) {
		// Di chuyển quân cơ trong mảng, bảng màu và loại mảng
		byte piece = board[square];
		board[square] = CoreConstants.EMPTY;
		long bitboard = ~(1L << square);
		bitboards[piece & 1] &= bitboard;
		bitboards[piece] &= bitboard;
	}

	void reset() {
		for (int i = 0; i < 64; i++) {
			board[i] = CoreConstants.EMPTY;
		}
		for (int i = 0; i < 14; i++) {
			bitboards[i] = 0;
		}
		toMove = CoreConstants.WHITE;
	}

	public void move(Move move) {
		// Lưu trữ bảng hiện tại
		storeHistory();
		moveNumber++;
		// chuyển người chơi
		toMove = (toMove == 0) ? 1 : 0;
		int finalIndex = move.getFinalPos();
		int oldIndex = move.getOldPos();
		byte piece = board[oldIndex];
		int side = piece % 2;
		int enemy = (piece == 0) ? 1 : 0;
		// If the piece is moving to a non-empty square then it is a capture
		// Nếu quân cờ di chuyển đến 1 hình vuông thì nó sẽ đc bắt lại
		// Di chuyển
		boolean capture = board[finalIndex] != CoreConstants.EMPTY;
		// Cập nhật cờ nhập thành cho quân xe
		// Nếu quân di chuyển thì không được nhập thành
		// Đảm bảo chỉ được nhập thành vào đúng thời điểm
		if (piece == CoreConstants.WHITE_ROOK) {
			if (oldIndex == 0) {
				castling[side] |= 0b010;
			}
			if (oldIndex == 7) {
				castling[side] |= 0b001;
			}
		} else if (piece == CoreConstants.BLACK_ROOK) {
			if (oldIndex == 56) {
				castling[side] |= 0b010;
			}
			if (oldIndex == 63) {
				castling[side] |= 0b001;
			}
			// Nếu vua đang di chuyển, chỉnh sửa, chỉnh sửa cờ nhập thành cho phù hợp
		} else if (piece == CoreConstants.WHITE_KING) {
			castling[side] |= 0b100;
		} else if (piece == CoreConstants.BLACK_KING) {
			castling[side] |= 0b100;
		}
		byte capturedPiece = board[finalIndex];
		if (capture && capturedPiece != CoreConstants.WHITE_KING
				&& capturedPiece != CoreConstants.BLACK_KING) {
			removePiece(finalIndex);
		}
		if (move.isEnPassant()) {
			int offset = (side == 0) ? -8 : 8;
			removePiece(finalIndex + offset);
		}
		if (finalIndex >= 0 && finalIndex < 64) {
			removePiece(oldIndex);
			addPiece(piece, finalIndex);
		}
		// Cập nhật cờ nhập thành cho cả hai người chơi
		updateCastlingFlags(side);
		updateCastlingFlags(enemy);
		epTargetSquares[enemy] = 0;
		if (piece == CoreConstants.WHITE_PAWN) {
			if (finalIndex - oldIndex == 16) {
				int square = finalIndex - 8;
				epTargetSquares[1] = 1L << square;
			}
		} else if (piece == CoreConstants.BLACK_PAWN) {
			if (finalIndex - oldIndex == -16) {
				int square = finalIndex + 8;
				epTargetSquares[0] = 1L << square;
			}
		}
		// Kiểm tra xem hành động hiện tại có phải là di chuyển nhập thành không
		byte castle = move.getCastlingFlag();
		if (castle != 0) {
			int rookOldIndex = 0;
			int rookFinalIndex = 0;
			switch (castle) {
			// Quân hậu trắng
			case CoreConstants.wQSide:
				rookOldIndex = 0;
				rookFinalIndex = 3;
				break;
			// Quân vua trắng
			case CoreConstants.wKSide:
				rookOldIndex = 7;
				rookFinalIndex = 5;
				break;
			// Quân hậu đen
			case CoreConstants.bQSide:
				rookOldIndex = 56;
				rookFinalIndex = 59;
				break;
			// Quân vua đen
			case CoreConstants.bKSide:
				rookOldIndex = 63;
				rookFinalIndex = 61;
				break;
			}
			// Remove the rook being moved
			// Xóa quân xe đã di chuyển
			removePiece(rookOldIndex);
			addPiece((castle <= 2) ? CoreConstants.WHITE_ROOK : CoreConstants.BLACK_ROOK,
					rookFinalIndex);
		}
	}

	public void storeHistory() {
		whiteHistory[moveNumber] = bitboards[0];
		blackHistory[moveNumber] = bitboards[1];
		pawnHistory[0][moveNumber] = bitboards[2];
		pawnHistory[1][moveNumber] = bitboards[3];
		knightHistory[0][moveNumber] = bitboards[4];
		knightHistory[1][moveNumber] = bitboards[5];
		rookHistory[0][moveNumber] = bitboards[6];
		rookHistory[1][moveNumber] = bitboards[7];
		bishopHistory[0][moveNumber] = bitboards[8];
		bishopHistory[1][moveNumber] = bitboards[9];
		queenHistory[0][moveNumber] = bitboards[10];
		queenHistory[1][moveNumber] = bitboards[11];
		kingHistory[0][moveNumber] = bitboards[12];
		kingHistory[1][moveNumber] = bitboards[13];
		for(int i = 0; i < 64; i++){
			boardHistory[moveNumber][i] = board[i];
		}
		epHistory[0][moveNumber] = epTargetSquares[0];
		epHistory[1][moveNumber] = epTargetSquares[1];
		castlingHistory[0][moveNumber] = castling[0];
		castlingHistory[1][moveNumber] = castling[1];
	}

	public void undo() {
		moveNumber--;
		bitboards[0] = whiteHistory[moveNumber];
		bitboards[1] = blackHistory[moveNumber];
		bitboards[2] = pawnHistory[0][moveNumber];
		bitboards[3] = pawnHistory[1][moveNumber];
		bitboards[4] = knightHistory[0][moveNumber];
		bitboards[5] = knightHistory[1][moveNumber];
		bitboards[6] = rookHistory[0][moveNumber];
		bitboards[7] = rookHistory[1][moveNumber];
		bitboards[8] = bishopHistory[0][moveNumber];
		bitboards[9] = bishopHistory[1][moveNumber];
		bitboards[10] = queenHistory[0][moveNumber];
		bitboards[11] = queenHistory[1][moveNumber];
		bitboards[12] = kingHistory[0][moveNumber];
		bitboards[13] = kingHistory[1][moveNumber];
		for(int i = 0; i < 64; i++){
			board[i] = boardHistory[moveNumber][i];
		}
		epTargetSquares[0] = epHistory[0][moveNumber];
		epTargetSquares[1] = epHistory[1][moveNumber];
		castling[0] = castlingHistory[0][moveNumber];
		castling[1] = castlingHistory[1][moveNumber];
		// Change who's next to move
		toMove = (toMove == 0) ? 1 : 0;

	}


	void updateCastlingFlags(int side) {
		if (side == 0) {
			castling[side] &= 0b01111;
			if (board[0] == CoreConstants.WHITE_ROOK) {
				if (board[1] == CoreConstants.EMPTY) {
					if (board[2] == CoreConstants.EMPTY) {
						if (board[3] == CoreConstants.EMPTY) {
							if (board[4] == CoreConstants.WHITE_KING) {
								if (!((castling[side] & 0b00100) == 4)) {
									if (!((castling[side] & 0b00010) == 2)) {
										if (!isSquareAttacked(1, side)) {
											if (!isSquareAttacked(2, side)) {
												if (!isSquareAttacked(3, side)) {
													if (!check(side)) {
														castling[side] |= 0b10000;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Xem xét màu trắng của quân vua
			castling[side] &= 0b10111;
			if (board[7] == CoreConstants.WHITE_ROOK) {
				if (board[6] == CoreConstants.EMPTY) {
					if (board[5] == CoreConstants.EMPTY) {
						if (board[4] == CoreConstants.WHITE_KING) {
							if (!((castling[side] & 0b00100) == 4)) {
								if (!((castling[side] & 0b00001) == 1)) {
									if (!check(side)) {
										if (!isSquareAttacked(5, side)) {
											if (!isSquareAttacked(6, side)) {
												castling[side] |= 0b01000;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} else {
			// Xem xét màu đen của quân hậu
			castling[side] &= 0b01111;
			if (board[56] == CoreConstants.BLACK_ROOK) {
				if (board[57] == CoreConstants.EMPTY) {
					if (board[58] == CoreConstants.EMPTY) {
						if (board[59] == CoreConstants.EMPTY) {
							if (board[60] == CoreConstants.BLACK_KING) {
								if (!((castling[side] & 0b00100) == 4)) {
									if (!((castling[side] & 0b00010) == 2)) {
										if (!isSquareAttacked(57, side)) {
											if (!isSquareAttacked(58, side)) {
												if (!isSquareAttacked(59, side)) {
													if (!check(side)) {
														castling[side] |= 0b10000;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Xem xét màu đen của quân vua
			castling[side] &= 0b10111;
			if (board[63] == CoreConstants.BLACK_ROOK) {
				if (board[62] == CoreConstants.EMPTY) {
					if (board[61] == CoreConstants.EMPTY) {
						if (board[60] == CoreConstants.BLACK_KING) {
							if (!((castling[side] & 0b00100) == 4)) {
								if (!((castling[side] & 0b00001) == 1)) {
									if (!check(side)) {
										if (!isSquareAttacked(61, side)) {
											if (!isSquareAttacked(62, side)) {
												castling[side] |= 0b01000;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	// https://chessprogramming.wikispaces.com/Checks+and+Pinned+Pieces+(Bitboards)
	public boolean check(int side) {
		int kingIndex = (side == CoreConstants.WHITE)
				? bitScanForward(bitboards[CoreConstants.WHITE_KING])
				: bitScanForward(bitboards[CoreConstants.BLACK_KING]);
		long enemyPawns = bitboards[3 - side];
		long enemyKnights = bitboards[5 - side];
		long enemyRookQueen = bitboards[11 - side];
		long enemyBishopQueen = enemyRookQueen;
		long occupiedBoard = bitboards[CoreConstants.WHITE] | bitboards[CoreConstants.BLACK];
		enemyRookQueen |= bitboards[7 - side];
		enemyBishopQueen |= bitboards[9 - side];
		long result = (CoreConstants.PAWN_ATTACKS_TABLE[side][kingIndex] & enemyPawns)
				| (CoreConstants.KNIGHT_TABLE[kingIndex] & enemyKnights)
				| (bishopAttacks(occupiedBoard, kingIndex, side) & enemyBishopQueen)
				| (rookAttacks(occupiedBoard, kingIndex, side) & enemyRookQueen);
		return (BitBoard.bitScanForward(result) != -1);
	}

	public boolean checkmate(int side) {
		if (check(side) && MoveGen.generateMoves(this, true).size() == 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkmate(int side, int sideToMove) {
		int origToMove = toMove;
		toMove = sideToMove;
		if (check(side) && MoveGen.generateMoves(this, true).size() == 0) {
			toMove = origToMove;
			return true;
		} else {
			toMove = origToMove;
			return false;
		}
	}

	public boolean stalemate(int sideToMove) {
		int origToMove = toMove;
		toMove = sideToMove;
		if (!check(sideToMove) && MoveGen.generateMoves(this, true).size() == 0) {
			toMove = origToMove;
			return true;
		} else {
			toMove = origToMove;
			return false;
		}

	}

	public boolean isSquareAttacked(int index, int side) {
		long enemyPawns = bitboards[3 - side];
		long enemyKnights = bitboards[5 - side];
		long enemyRookQueen = bitboards[11 - side];
		long enemyBishopQueen = enemyRookQueen;
		long occupiedBoard = bitboards[CoreConstants.WHITE] | bitboards[CoreConstants.BLACK];
		enemyRookQueen |= bitboards[7 - side];
		enemyBishopQueen |= bitboards[9 - side];
		long result = (CoreConstants.PAWN_ATTACKS_TABLE[side][index] & enemyPawns)
				| (CoreConstants.KNIGHT_TABLE[index] & enemyKnights)
				| (bishopAttacks(occupiedBoard, index, side) & enemyBishopQueen)
				| (rookAttacks(occupiedBoard, index, side) & enemyRookQueen);
		return (BitBoard.bitScanForward(result) != -1);
	}

	long bishopAttacks(long occupiedBoard, int index, int side) {
		long bishopBlockers = (occupiedBoard) & CoreConstants.occupancyMaskBishop[index];
		int lookupIndex = (int) ((bishopBlockers
				* CoreConstants.magicNumbersBishop[index]) >>> CoreConstants.magicShiftBishop[index]);
		long moveSquares = CoreConstants.magicMovesBishop[index][lookupIndex] & ~bitboards[side];
		return moveSquares;
	}

	long rookAttacks(long occupiedBoard, int index, int side) {
		long rookBlockers = (occupiedBoard) & CoreConstants.occupancyMaskRook[index];
		int lookupIndex = (int) ((rookBlockers
				* CoreConstants.magicNumbersRook[index]) >>> CoreConstants.magicShiftRook[index]);
		long moveSquares = CoreConstants.magicMovesRook[index][lookupIndex] & ~bitboards[side];
		return moveSquares;
	}

	public void resetToInitialSetup() {
		// Begin by setting all indexes to empty
		for (int index = 0; index < 64; index++) {
			board[index] = CoreConstants.EMPTY;
		}

		for (int col = 0; col <= 7; col++) {
			board[8 + col] = CoreConstants.WHITE_PAWN;
			board[48 + col] = CoreConstants.BLACK_PAWN;
		}
		for (int col = 0; col <= 7; col += 7) {
			board[0 + col] = CoreConstants.WHITE_ROOK;
			board[56 + col] = CoreConstants.BLACK_ROOK;
		}
		for (int col = 1; col <= 6; col += 5) {
			board[0 + col] = CoreConstants.WHITE_KNIGHT;
			board[56 + col] = CoreConstants.BLACK_KNIGHT;
		}
		for (int col = 2; col <= 5; col += 3) {
			board[0 + col] = CoreConstants.WHITE_BISHOP;
			board[56 + col] = CoreConstants.BLACK_BISHOP;
		}
		// Adding queens
		board[0 + 3] = CoreConstants.WHITE_QUEEN;
		board[56 + 3] = CoreConstants.BLACK_QUEEN;
		// Adding kings
		board[0 + 4] = CoreConstants.WHITE_KING;
		board[56 + 4] = CoreConstants.BLACK_KING;

		// Bitboards in hexadecimal for each of the piece types
		bitboards[CoreConstants.WHITE] = 0x000000000000FFFFL;
		bitboards[CoreConstants.BLACK] = 0xFFFF000000000000L;
		bitboards[CoreConstants.WHITE_PAWN] = 0x000000000000FF00L;
		bitboards[CoreConstants.WHITE_KNIGHT] = 0x0000000000000042L;
		bitboards[CoreConstants.WHITE_BISHOP] = 0x0000000000000024L;
		bitboards[CoreConstants.WHITE_ROOK] = 0x0000000000000081L;
		bitboards[CoreConstants.WHITE_QUEEN] = 0x0000000000000008L;
		bitboards[CoreConstants.WHITE_KING] = 0x0000000000000010L;
		bitboards[CoreConstants.BLACK_PAWN] = 0x00FF000000000000L;
		bitboards[CoreConstants.BLACK_KNIGHT] = 0x4200000000000000L;
		bitboards[CoreConstants.BLACK_BISHOP] = 0x2400000000000000L;
		bitboards[CoreConstants.BLACK_ROOK] = 0x8100000000000000L;
		bitboards[CoreConstants.BLACK_QUEEN] = 0x0800_0000_0000_0000L;
		bitboards[CoreConstants.BLACK_KING] = 0x1000000000000000L;
		toMove = CoreConstants.WHITE;
		castling[0] = 0L;
		castling[1] = 0L;
	}

	public static int bitScanForward(long bb) {
		int pos = Long.numberOfTrailingZeros(bb);
		return pos == 64 ? -1 : pos;
	}

	public static int bitScanBackward(long bb) {
		int pos = Long.numberOfLeadingZeros(bb);

		return pos == 64 ? -1 : 63 - pos;
	}

	public static void initialiseZobrist() {
		for (int x = 0; x <= 63; x++) {
			for (int y = 0; y <= 11; y++) {
				CoreConstants.zobristTable[x][y] = new Random().nextInt((int) Math.pow(2, 64) - 1);
			}
		}
	}

	public int hash() {
		int hash = 0;
		for (int index = 0; index < 64; index++) {
			if (board[index] != CoreConstants.EMPTY) {
				int j = board[index] - 2;
				hash = hash ^ CoreConstants.zobristTable[index][j];
			}
		}
		return hash;
	}

	// Based on algorithm at https://en.wikipedia.org/wiki/Hamming_weight
	public static int hammingWeight(long x) {
		x -= (x >> 1) & CoreConstants.m1;
		x = (x & CoreConstants.m2) + ((x >> 2) & CoreConstants.m2);
		x = (x + (x >> 4)) & CoreConstants.m4;
		return (int) ((x * CoreConstants.h01) >> 56);
	}

	public int getMoveNumber() {
		return moveNumber;
	}

	public byte[] cloneBoard(byte[] origArray, byte[] newArray) {
		for (int i = 0; i < 64; i++) {
			newArray[i] = origArray[i];
		}
		return newArray;
	}

	public void setMoveNumber(int n) {
		moveNumber = n;
	}

	public byte[] getBoardArray() {
		return board;
	}

	public long[] getBitBoards() {
		return bitboards;
	}

	public long[] getEpTargetSquares() {
		return epTargetSquares;
	}

	public long[] getCastlingFlags() {
		return castling;
	}
}
